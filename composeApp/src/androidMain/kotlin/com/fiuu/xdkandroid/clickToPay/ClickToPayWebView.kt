package com.fiuu.xdkandroid.clickToPay

import FullScreenLoadingOverlay
import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.fiuu.xdkandroid.utils.FiuuLogger
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fiuu.xdkandroid.components.PaymentScaffold
import com.fiuu.xdkandroid.ui.theme.AppColors
import kotlinx.serialization.json.JsonObject
import kotlinx.coroutines.delay

// Renders Click to Pay inside the existing Compose tree (via AndroidView) instead of replacing
// the Activity's content view, so the surrounding FiuuPaymentSDK composition — and the
// paymentError/paymentSuccess LaunchedEffects it owns — stays alive for the whole flow.
@Composable
fun ClickToPayWebView(
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
    // this same WebView via onAndroidMessage rather than reloading a fresh, unauthenticated page.
    var widgetWebView by remember { mutableStateOf<WebView?>(null) }

    // System back had nothing to intercept it here (no OnBackPressedCallback like the
    // redirect-overlay WebView has), so it fell through to the Activity's default back handling
    // and closed the app instead of ending the C2P attempt. Back always returns to the channel
    // list — same as every other screen's back arrow in the SDK — regardless of which C2P
    // sub-screen is showing; only the explicit Close button (see ClickToPayWidgetView/
    // ClickToPayRegistrationRequiredView below) ends the whole attempt via onFullCancel.
    BackHandler {
        onCancel("User cancel payment")
    }

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
            // The widget screen's own Close button — this IS the payment page as far as the user
            // is concerned, so closing it ends the whole attempt (see onClickToPayFullCancel).
            onCancel = onFullCancel
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
                // Back arrow returns to the channel list; the explicit Close button ends the
                // whole attempt — same split as everywhere else in this file.
                onNavigateBack = onCancel,
                onClose = onFullCancel
            )
        }
    }
}

// Feeds natively-collected card details back into the live widget's SRC SDK session — see
// c2pNewCardScript (commonMain) for the payload the page's onAndroidMessage expects.
private fun submitNewCardToWidget(webView: WebView?, card: String, expiry: String, cvv: String) {
    if (webView == null) {
        Log.e("FIUU_WEBVIEW", "submitNewCardToWidget: widget WebView is null, cannot inject card")
        return
    }
    webView.evaluateJavascript(c2pNewCardScript(card, expiry, cvv), null)
}

// JS bridge for the Mastercard SRC ("Click to Pay") widget: the page calls
// AndroidBridge.postMessage(json) — see dispatchC2PBridgeMessage (commonMain, shared with iOS)
// for what each `type` means. @JavascriptInterface methods run on a background thread, so the
// dispatch is posted to the main thread before it touches any Compose state.
private class C2PJsBridge(
    private val onSuccess: (JsonObject) -> Unit,
    private val onNotRegistered: () -> Unit,
    private val onSubmitError: (message: String) -> Unit,
    private val onNavigateToManualCard: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun postMessage(message: String) {
        mainHandler.post {
            dispatchC2PBridgeMessage(
                message = message,
                onSuccess = onSuccess,
                onNotRegistered = onNotRegistered,
                onSubmitError = onSubmitError,
                onNavigateToManualCard = onNavigateToManualCard
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
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
    onWebViewReady: (WebView) -> Unit,
    onCancel: (String?) -> Unit
) {
    // Compose does not dispose an AndroidView on Activity stop/resume (task switch, Home
    // button) — only when the composable leaves composition — so the WebView instance survives
    // backgrounding but its Chromium-rendered surface is never told to resume without this,
    // leaving it permanently blank on return even though the underlying page/JS session is alive.
    var lifecycleWebView by remember { mutableStateOf<WebView?>(null) }
    // The SRC widget's shell page finishes fast, but the actual UI stays blank for several more
    // seconds while it loads Mastercard/Visa's own cross-domain scripts (src.mastercard.com,
    // secure.checkout.visa.com, thm.visa.com fraud fingerprinting, etc. — confirmed on-device:
    // ~14s between onPageFinished and the widget doing anything visible). Without an explicit
    // loading state that whole window looks identical to a broken/stuck WebView. Cleared on
    // onPageFinished (or a load error) — the same "reveal once the shell is ready" signal
    // AndroidWebViewBridge's overlay WebView already uses for the redirect flow.
    var isPageLoading by remember { mutableStateOf(true) }
    // Belt-and-braces beyond onRenderProcessGone below: a renderer crash is the one failure
    // mode we've actually observed getting stuck (silent — no callback fires at all without that
    // override), but there's no guarantee it's the only way this can hang, and a payment screen
    // must never be able to strand the user on a spinner with no way out. ~14s was the slowest
    // normal load observed on-device (Mastercard/Visa's own cross-domain scripts); 30s gives that
    // real headroom before treating it as failed.
    LaunchedEffect(Unit) {
        delay(C2P_LOAD_TIMEOUT_MS)
        if (isPageLoading) {
            Log.e("FIUU_WEBVIEW", "C2P widget still loading after 30s timeout")
            isPageLoading = false
            onSubmitError("Click to Pay is taking too long to respond. Please try again.")
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    lifecycleWebView?.onResume()
                    lifecycleWebView?.resumeTimers()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    lifecycleWebView?.onPause()
                    lifecycleWebView?.pauseTimers()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Same PaymentScaffold app bar every other screen in the SDK uses (ClickToPayRegistrationRequiredView,
    // CardManualEntryScreen, etc.) rather than a bespoke floating close button — it also hands the
    // WebView its system-bar insets for free (see ClickToPayRegistrationRequiredView's comment),
    // so the manual windowInsetsPadding this used to need is gone too. showAppBar mirrors
    // mp_closebutton_display's meaning on the legacy Webcore engine (see ProvideFiuuPaymentSDK.kt):
    // false hides the bar entirely rather than just the close action within it.
    PaymentScaffold(
        showAppBar = closeButtonDisplay,
        secondsRemaining = secondsRemaining,
        timerBackground = AppColors.White,
        // Same look as FiuuPaymentSDK's own FullScreenLoadingOverlay, but over the body only —
        // CLOSE stays usable while the SRC scripts take their ~10-15s to load.
        bodyOverlay = if (isPageLoading) { { FullScreenLoadingOverlay("Loading...") } } else null,
        // Blank instead of PaymentScaffold's "Payment" default — the embedded widget's own page
        // already identifies itself ("Click to Pay is finding your cards", etc.), so a title here
        // would just be redundant.
        title = "",
        onClose = { onCancel("User cancel payment") }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val webView = WebView(ctx)
                    webView.apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            javaScriptCanOpenWindowsAutomatically = true
                            // This page handles card data and exposes AndroidBridge to every script
                            // it loads (Mastercard, Visa, ThreatMetrix). An http sub-resource or a
                            // file:/content: URL must never be able to reach that bridge (SEC-02).
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            allowFileAccess = false
                            allowContentAccess = false
                            // Disable media autoplay and block any prompts for camera/mic
                            mediaPlaybackRequiresUserGesture = true
                            // A cancelled attempt tears its WebView down mid-load (stopLoading() in
                            // onRelease below) — Chromium's HTTP cache is shared across every WebView
                            // in the process, so without this a retry's brand-new WebView can be
                            // served back that same interrupted/partial response for this URL instead
                            // of re-fetching, rendering blank. A payment widget must never read from
                            // cache anyway.
                            cacheMode = WebSettings.LOAD_NO_CACHE
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                // Payment flows rely on redirects (302) — let the WebView follow them itself.
                                if (FiuuLogger.enabled) Log.d("FIUU_WEBVIEW", "C2P shouldOverrideUrlLoading: ${request?.url}, isForMainFrame=${request?.isForMainFrame}")
                                return false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                if (FiuuLogger.enabled) Log.d("FIUU_WEBVIEW", "onPageFinished: $url")
                                CookieManager.getInstance().flush()
                                isPageLoading = false
                                // init.js's own console.log("notYouRequested", event) only reaches
                                // onConsoleMessage as the JS engine's default toString of the event
                                // ("[object CustomEvent]") — there's no way to recover the original
                                // object from that string. Attaching our own capture-phase listener
                                // lets us read event.detail directly and JSON-serialize it instead.
                                // Debug-only (FiuuLogger.enabled), and guarded so repeat
                                // onPageFinished calls on the same document don't double-attach.
                                if (FiuuLogger.enabled) {
                                    view?.evaluateJavascript(
                                        """
                                        (function() {
                                            if (window.__fiuuDebugListenersAttached) return;
                                            window.__fiuuDebugListenersAttached = true;
                                            ['notYouRequested'].forEach(function(name) {
                                                document.addEventListener(name, function(e) {
                                                    var detail;
                                                    try { detail = JSON.stringify(e.detail); } catch (err) { detail = String(e.detail); }
                                                    console.log('[FiuuDebug] ' + name + ' detail=' + detail);
                                                }, true);
                                            });
                                        })();
                                        """.trimIndent(),
                                        null
                                    )
                                }
                            }

                            // Unlike AndroidWebViewBridge's redirect overlay, this WebViewClient had no
                            // error visibility at all — a failed load (e.g. a stale/expired session on
                            // retry) rendered as a silent blank page with nothing in Logcat to explain
                            // why. Always logged (not gated behind FiuuLogger.enabled) since this is a
                            // genuine failure, not routine tracing.
                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: android.webkit.WebResourceError?
                            ) {
                                if (request?.isForMainFrame != true) return
                                Log.e("FIUU_WEBVIEW", "C2P WebView error ${error?.errorCode}: ${error?.description}")
                                isPageLoading = false
                            }

                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                errorResponse: android.webkit.WebResourceResponse?
                            ) {
                                if (request?.isForMainFrame != true) return
                                Log.e("FIUU_WEBVIEW", "C2P WebView HTTP error ${errorResponse?.statusCode}")
                                isPageLoading = false
                            }

                            // The Mastercard/Visa widget is heavy (multiple WebGL contexts for
                            // device fingerprinting, ThreatMetrix fraud scripts, etc. — confirmed
                            // on-device: "Too many active WebGL contexts" warnings even on a
                            // single attempt) and none of the callbacks above fire when the
                            // renderer process itself crashes/is killed rather than the page
                            // failing to load — that left a retry after a cancelled first attempt
                            // stuck on the loading overlay forever with nothing in Logcat to
                            // explain why. Returning true tells Android this WebView handled the
                            // crash itself (default behavior without an override is to crash the
                            // whole app), and routes the user back through the same fallback the
                            // "01" JS bridge error already uses instead of a dead spinner.
                            override fun onRenderProcessGone(
                                view: WebView?,
                                detail: android.webkit.RenderProcessGoneDetail?
                            ): Boolean {
                                Log.e(
                                    "FIUU_WEBVIEW",
                                    "C2P WebView renderer process gone: didCrash=${detail?.didCrash()} " +
                                        "priority=${detail?.rendererPriorityAtExit()}"
                                )
                                isPageLoading = false
                                onSubmitError("The payment page stopped responding. Please try again.")
                                return true
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onPermissionRequest(request: PermissionRequest) {
                                if (FiuuLogger.enabled) Log.d("FIUU_WEBVIEW", "Permission denied for: ${request.resources.joinToString()}")
                                request.deny()
                            }

                            // Surfaces console.log/error from every frame (including cross-origin iframes
                            // the SRC widget itself runs in) — addJavascriptInterface only reaches the
                            // top-level frame, so this is the only visibility we have into iframe JS.
                            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage): Boolean {
                                if (FiuuLogger.enabled) Log.d(
                                    "FIUU_WEBVIEW",
                                    "console [${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}] ${consoleMessage.message()}"
                                )
                                return true
                            }
                        }
                        // Mastercard SRC widget depends on cross-domain (third-party) cookies.
                        CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                            setAcceptThirdPartyCookies(webView, true)
                        }
                        addJavascriptInterface(
                            C2PJsBridge(
                                onSuccess = onSuccess,
                                onNotRegistered = onNotRegistered,
                                onSubmitError = onSubmitError,
                                onNavigateToManualCard = onNavigateToManualCard
                            ),
                            C2P_BRIDGE_NAME
                        )
                        loadUrl(url, headers)
                    }.also(onWebViewReady).also { lifecycleWebView = it }
                },
                onRelease = { webView ->
                    lifecycleWebView = null
                    // C2PJsBridge's closure captures onSuccess/onNotRegistered/onSubmitError, which are
                    // tied to this composition — stop any in-flight load/JS first so a pending postMessage
                    // callback can't fire into a torn-down composable after destroy().
                    webView.removeJavascriptInterface(C2P_BRIDGE_NAME)
                    webView.stopLoading()
                    webView.loadUrl("about:blank")
                    webView.clearHistory()
                    // NOT webView.pauseTimers(): despite being called on a single WebView instance,
                    // Android docs are explicit that it "pauses all layout, parsing, and JavaScript
                    // timers for all WebViews... a global request, not restricted to just this
                    // WebView" — and nothing ever paired it with resumeTimers() here, since this
                    // WebView is being destroyed, not backgrounded. Confirmed on-device: after the
                    // first Click to Pay cancel, every retry's WebView loaded its shell page but
                    // never produced a single console log or onPageFinished again for the rest of
                    // the app process — the heavily setTimeout/Promise-driven Visa/Mastercard SDK
                    // bootstrap never got to run because JS timers stayed frozen process-wide.
                    // onPause() dropped too — it's meaningless on a WebView about to be destroy()ed.
                    webView.webChromeClient = null
                    webView.destroy()
                }
            )
        }
    }
}
