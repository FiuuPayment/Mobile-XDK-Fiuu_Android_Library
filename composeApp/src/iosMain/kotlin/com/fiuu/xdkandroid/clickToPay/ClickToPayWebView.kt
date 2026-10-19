package com.fiuu.xdkandroid.clickToPay

import FullScreenLoadingOverlay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.fiuu.xdkandroid.components.PaymentScaffold
import com.fiuu.xdkandroid.ui.theme.AppColors
import com.fiuu.xdkandroid.utils.FiuuLogger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSError
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLErrorCancelled
import platform.Foundation.NSURLRequestReloadIgnoringLocalCacheData
import platform.Foundation.setValue
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUIDelegateProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebpagePreferences
import platform.WebKit.WKWindowFeatures
import platform.darwin.NSObject

// iOS counterpart of androidMain's ClickToPayWebView: hosts the same hosted mastercardc2p widget
// page in a WKWebView embedded in the Compose tree (UIKitView), so the surrounding FiuuPaymentSDK
// composition — and its paymentError/paymentSuccess LaunchedEffects — stays alive for the whole
// flow. Bridge messages and the "Add New Card" fallback screen are shared with Android
// (ClickToPayShared.kt); only the WebView host differs.

// Message handler name for the dev-mode console forwarder below.
private const val C2P_CONSOLE_HANDLER = "FiuuC2PConsole"

// The hosted page calls AndroidBridge.postMessage(json) — an object Android injects with
// addJavascriptInterface. WKWebView has no equivalent, so define a same-named shim at document
// start that forwards to the WKScriptMessageHandler registered under C2P_BRIDGE_NAME. Injected
// into every frame, like addJavascriptInterface, in case the call comes from a child frame.
private val bridgeShimScript = """
    (function() {
        if (window.$C2P_BRIDGE_NAME) return;
        window.$C2P_BRIDGE_NAME = {
            postMessage: function(message) {
                window.webkit.messageHandlers.$C2P_BRIDGE_NAME.postMessage(
                    typeof message === 'string' ? message : JSON.stringify(message)
                );
            }
        };
    })();
""".trimIndent()

// Dev-mode only (FiuuLogger.enabled): mirrors Android's onConsoleMessage, the only visibility
// into the widget's JS (including the cross-origin SRC iframes) when diagnosing on a device.
private val consoleForwarderScript = """
    (function() {
        if (window.__fiuuConsoleForwarded) return;
        window.__fiuuConsoleForwarded = true;
        ['log', 'warn', 'error'].forEach(function(level) {
            var original = console[level];
            console[level] = function() {
                try {
                    var parts = Array.prototype.map.call(arguments, function(a) {
                        if (typeof a === 'string') return a;
                        try { return JSON.stringify(a); } catch (e) { return String(a); }
                    });
                    window.webkit.messageHandlers.$C2P_CONSOLE_HANDLER.postMessage(
                        '[' + level + '] ' + location.host + ': ' + parts.join(' ')
                    );
                } catch (e) {}
                if (original) original.apply(console, arguments);
            };
        });
    })();
""".trimIndent()

@Composable
internal fun ClickToPayWebView(
    url: String,
    headers: Map<String, String>,
    amount: Long,
    currency: String,
    country: String? = null,
    closeButtonDisplay: Boolean = true,
    secondsRemaining: (() -> Int)? = null,
    onSuccess: (JsonObject) -> Unit,
    onCancel: (String?) -> Unit,
    onFullCancel: (String?) -> Unit,
    onNavigateToManualCard: () -> Unit
) {
    var showRegistrationRequired by remember { mutableStateOf(false) }
    var registrationErrorMessage by remember { mutableStateOf<String?>(null) }
    // Kept across the registration-placeholder detour so its JS session (SRC SDK init, OTP
    // validation already done, etc.) survives — "Pay Now" injects the new-card details back into
    // this same WebView rather than reloading a fresh, unauthenticated page.
    var widgetWebView by remember { mutableStateOf<WKWebView?>(null) }

    // Both views stay composed (and the widget's WebView stays alive) for the whole flow; the
    // placeholder simply overlays on top when shown.
    Box(modifier = Modifier.fillMaxSize()) {
        ClickToPayWidgetView(
            url = url,
            headers = headers,
            closeButtonDisplay = closeButtonDisplay,
            secondsRemaining = secondsRemaining,
            onSuccess = onSuccess,
            onNotRegistered = {
                registrationErrorMessage = null
                showRegistrationRequired = true
            },
            onSubmitError = { message ->
                registrationErrorMessage = message
                showRegistrationRequired = true
            },
            onNavigateToManualCard = onNavigateToManualCard,
            onWebViewReady = { widgetWebView = it },
            // iOS has no system back button (Android's BackHandler returns to the channel list),
            // so the widget screen gets an explicit back arrow for the same action.
            onNavigateBack = onCancel,
            // The widget screen's own Close button — this IS the payment page as far as the user
            // is concerned, so closing it ends the whole attempt (see onClickToPayFullCancel).
            onClose = onFullCancel
        )
        if (showRegistrationRequired) {
            ClickToPayRegistrationRequiredView(
                amount = amount,
                currency = currency,
                country = country,
                closeButtonDisplay = closeButtonDisplay,
                secondsRemaining = secondsRemaining,
                errorMessage = registrationErrorMessage,
                onPayNow = { card, expiry, cvv ->
                    showRegistrationRequired = false
                    submitNewCardToWidget(widgetWebView, card, expiry, cvv)
                },
                onNavigateBack = onCancel,
                onClose = onFullCancel
            )
        }
    }
}

private fun submitNewCardToWidget(webView: WKWebView?, card: String, expiry: String, cvv: String) {
    if (webView == null) {
        FiuuLogger.log("C2P submitNewCardToWidget: widget WebView is null, cannot inject card")
        return
    }
    webView.evaluateJavaScript(c2pNewCardScript(card, expiry, cvv)) { _, error ->
        if (error != null) {
            FiuuLogger.log("C2P submitNewCardToWidget: evaluateJavaScript error: ${error.localizedDescription}")
        }
    }
}

// Receives AndroidBridge.postMessage calls (via the shim) and dev-mode console lines. WebKit
// delivers script messages on the main thread, so dispatching straight into Compose state is safe.
private class C2PScriptMessageHandler(
    private val onBridgeMessage: (String) -> Unit
) : NSObject(), WKScriptMessageHandlerProtocol {
    override fun userContentController(
        userContentController: WKUserContentController,
        didReceiveScriptMessage: WKScriptMessage
    ) {
        val body = didReceiveScriptMessage.body as? String ?: run {
            FiuuLogger.log("C2P bridge: non-string message body from ${didReceiveScriptMessage.name}")
            return
        }
        when (didReceiveScriptMessage.name) {
            C2P_BRIDGE_NAME -> onBridgeMessage(body)
            C2P_CONSOLE_HANDLER -> FiuuLogger.log("C2P console $body")
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class C2PNavigationDelegate(
    private val onPageFinished: () -> Unit,
    private val onLoadFailed: () -> Unit,
    private val onContentProcessTerminated: () -> Unit
) : NSObject(), WKNavigationDelegateProtocol, WKUIDelegateProtocol {

    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        FiuuLogger.log("C2P didFinishNavigation: ${webView.URL?.absoluteString}")
        onPageFinished()
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) {
        if (withError.code == NSURLErrorCancelled) return
        FiuuLogger.log("C2P didFailNavigation ${withError.code}: ${withError.localizedDescription}")
        onLoadFailed()
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFailProvisionalNavigation: WKNavigation?, withError: NSError) {
        // -999 fires when a navigation is superseded by another (e.g. a redirect) — not a failure.
        if (withError.code == NSURLErrorCancelled) return
        FiuuLogger.log("C2P didFailProvisionalNavigation ${withError.code}: ${withError.localizedDescription}")
        onLoadFailed()
    }

    // iOS equivalent of Android's onRenderProcessGone: the heavy Mastercard/Visa widget's web
    // content process was killed, which otherwise leaves a blank page with no callback at all.
    override fun webViewWebContentProcessDidTerminate(webView: WKWebView) {
        FiuuLogger.log("C2P web content process terminated")
        onContentProcessTerminated()
    }

    // target=_blank / window.open: load in this same WebView, matching Android's default
    // (setSupportMultipleWindows(false)) instead of silently dropping the navigation.
    override fun webView(
        webView: WKWebView,
        createWebViewWithConfiguration: WKWebViewConfiguration,
        forNavigationAction: WKNavigationAction,
        windowFeatures: WKWindowFeatures
    ): WKWebView? {
        if (forNavigationAction.targetFrame == null) {
            webView.loadRequest(forNavigationAction.request)
        }
        return null
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun ClickToPayWidgetView(
    url: String,
    headers: Map<String, String>,
    closeButtonDisplay: Boolean,
    secondsRemaining: (() -> Int)?,
    onSuccess: (JsonObject) -> Unit,
    onNotRegistered: () -> Unit,
    onSubmitError: (message: String) -> Unit,
    onNavigateToManualCard: () -> Unit,
    onWebViewReady: (WKWebView) -> Unit,
    onNavigateBack: (String?) -> Unit,
    onClose: (String?) -> Unit
) {
    // Same loading/timeout handling as Android: the shell page finishes fast, but the widget
    // stays blank for several more seconds while Mastercard/Visa's cross-domain scripts load.
    var isPageLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(C2P_LOAD_TIMEOUT_MS)
        if (isPageLoading) {
            FiuuLogger.log("C2P widget still loading after ${C2P_LOAD_TIMEOUT_MS}ms timeout")
            isPageLoading = false
            onSubmitError("Click to Pay is taking too long to respond. Please try again.")
        }
    }

    // WebKit holds the delegate weakly and the message handler strongly; both are created once
    // per composition and read the latest callbacks through these.
    val currentOnSuccess by rememberUpdatedState(onSuccess)
    val currentOnNotRegistered by rememberUpdatedState(onNotRegistered)
    val currentOnSubmitError by rememberUpdatedState(onSubmitError)
    val currentOnNavigateToManualCard by rememberUpdatedState(onNavigateToManualCard)

    val messageHandler = remember {
        C2PScriptMessageHandler { message ->
            dispatchC2PBridgeMessage(
                message = message,
                onSuccess = { currentOnSuccess(it) },
                onNotRegistered = { currentOnNotRegistered() },
                onSubmitError = { currentOnSubmitError(it) },
                onNavigateToManualCard = { currentOnNavigateToManualCard() }
            )
        }
    }
    val c2pDelegate = remember {
        C2PNavigationDelegate(
            onPageFinished = { isPageLoading = false },
            onLoadFailed = { isPageLoading = false },
            onContentProcessTerminated = {
                isPageLoading = false
                currentOnSubmitError("The payment page stopped responding. Please try again.")
            }
        )
    }

    PaymentScaffold(
        showAppBar = closeButtonDisplay,
        secondsRemaining = secondsRemaining,
        timerBackground = AppColors.White,
        // Same look as FiuuPaymentSDK's own FullScreenLoadingOverlay, but over the body only —
        // CLOSE stays usable while the SRC scripts take their ~10-15s to load.
        bodyOverlay = if (isPageLoading) { { FullScreenLoadingOverlay("Loading...") } } else null,
        // Blank — the embedded widget's own page already identifies itself.
        title = "",
        onNavigateBack = { onNavigateBack("User cancel payment") },
        onClose = { onClose("User cancel payment") }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            UIKitView(
                factory = {
                    val config = WKWebViewConfiguration().apply {
                        defaultWebpagePreferences = WKWebpagePreferences().apply {
                            allowsContentJavaScript = true
                        }
                        preferences.javaScriptCanOpenWindowsAutomatically = true
                        // Default (persistent) website data store: the Mastercard SRC widget relies
                        // on its own cookies/storage to recognise a returning device.
                        userContentController.addUserScript(
                            WKUserScript(
                                source = bridgeShimScript,
                                injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentStart,
                                forMainFrameOnly = false
                            )
                        )
                        userContentController.addScriptMessageHandler(messageHandler, name = C2P_BRIDGE_NAME)
                        if (FiuuLogger.enabled) {
                            userContentController.addUserScript(
                                WKUserScript(
                                    source = consoleForwarderScript,
                                    injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentStart,
                                    forMainFrameOnly = false
                                )
                            )
                            userContentController.addScriptMessageHandler(messageHandler, name = C2P_CONSOLE_HANDLER)
                        }
                    }
                    WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = config).apply {
                        navigationDelegate = c2pDelegate
                        UIDelegate = c2pDelegate
                        scrollView.bounces = false
                        val request = NSMutableURLRequest(uRL = NSURL(string = url)).apply {
                            // A payment widget must never be served from cache — see Android's
                            // LOAD_NO_CACHE for the retry-after-cancel failure this avoids.
                            setCachePolicy(NSURLRequestReloadIgnoringLocalCacheData)
                            headers.forEach { (key, value) -> setValue(value, forHTTPHeaderField = key) }
                        }
                        loadRequest(request)
                    }.also(onWebViewReady)
                },
                modifier = Modifier.fillMaxSize(),
                // Not reusable — also pins this call to the current UIKitView overload.
                onReset = null,
                onRelease = { webView ->
                    // The user content controller retains the message handler (and through it this
                    // composition's callbacks) — unregister before tearing the WebView down so a
                    // late postMessage can't fire into a disposed composable.
                    val contentController = webView.configuration.userContentController
                    contentController.removeScriptMessageHandlerForName(C2P_BRIDGE_NAME)
                    contentController.removeScriptMessageHandlerForName(C2P_CONSOLE_HANDLER)
                    contentController.removeAllUserScripts()
                    webView.stopLoading()
                    webView.navigationDelegate = null
                    webView.UIDelegate = null
                }
            )
        }
    }
}
