package com.fiuu.xdkandroid.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLErrorCancelled
import platform.Foundation.create
import platform.Foundation.setHTTPBody
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import platform.Foundation.stringByRemovingPercentEncoding
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationOpenURLOptionUniversalLinksOnly
import platform.UIKit.UIBarButtonItem
import platform.UIKit.UIModalPresentationFullScreen
import platform.UIKit.navigationItem
import platform.UIKit.UINavigationController
import platform.UIKit.UIViewController
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val APP_NOT_INSTALLED_MESSAGE = "The app needed for this payment isn't installed"
private const val RESULT_PAGE_MARKER = "MOLPay/result.php?"

private val INTERNAL_SCHEMES = setOf(
    "http", "https", "about", "blob", "data", "javascript", "file"
)

private data class EwalletUniversalLink(
    val host: String,
    val pathPrefix: String = "",
)

private val EWALLET_UNIVERSAL_LINKS = listOf(
    EwalletUniversalLink("atome-my.onelink.me"),
    EwalletUniversalLink("myboost.app"),
    EwalletUniversalLink("app.shopback.com", pathPrefix = "/pay"),
    EwalletUniversalLink("m.tngdigital.com.my", pathPrefix = ""),
)

private fun isEwalletUniversalLink(url: NSURL): Boolean {
    if (url.scheme?.lowercase() != "https") return false
    val host = url.host?.lowercase() ?: return false
    val path = url.path ?: ""
    return EWALLET_UNIVERSAL_LINKS.any { link ->
        (host == link.host || host.endsWith(".${link.host}")) && path.startsWith(link.pathPrefix)
    }
}

private fun extractBrowserFallbackUrl(intentUrl: String): String? {
    val marker = "browser_fallback_url="
    val index = intentUrl.indexOf(marker)
    if (index == -1) return null
    val raw = intentUrl.substring(index + marker.length).substringBefore(";").substringBefore("&")
    val decoded = (raw as NSString).stringByRemovingPercentEncoding ?: raw
    return decoded.takeIf { it.startsWith("https://") || it.startsWith("http://") }
}

class IosWebViewBridge : WebViewBridge {

    // Set after the ComposeUIViewController is created (can't reference the VC inside its own lambda)
    internal var controller: UIViewController? = null

    private var presentedNavigationController: UINavigationController? = null
    // UIBarButtonItem holds its target weakly, so the handler backing the Cancel button must be
    // retained here for the lifetime of the presentation, not just inside createWebView.
    private var cancelButtonHandler: CancelButtonHandler? = null
    // WKWebView.navigationDelegate is weak too. An anonymous delegate with no other reference can
    // be collected by Kotlin/Native's GC mid-payment, after which no load failure or success
    // callback ever fires. Held here for the presentation's lifetime, like cancelButtonHandler.
    private var navigationDelegate: RedirectNavigationDelegate? = null
    private var lifecycleObserver: LifecycleObserver? = null
    private var merchantId: String = ""
    private var sessionId: String = ""

    // Mirrors AndroidWebViewBridge's system-back handling: the user must always have a way out of
    // the payment page, and that exit must run onClose so the ViewModel confirms the transaction
    // status through /inquire instead of leaving it pending. modalPresentationStyle =
    // UIModalPresentationFullScreen (below) disables the interactive swipe-to-dismiss gesture, so
    // without this the gateway page's own cancel link (if any) is the only way out.
    @OptIn(BetaInteropApi::class)
    private class CancelButtonHandler(private val onTap: () -> Unit) : NSObject() {
        @ObjCAction
        fun invoke() {
            onTap()
        }
    }

    @OptIn(BetaInteropApi::class)
    private class LifecycleObserver(
        private val onPause: () -> Unit,
        private val onResume: () -> Unit
    ) : NSObject() {
        @ObjCAction
        fun appDidEnterBackground() {
            onPause()
        }

        @ObjCAction
        fun appDidBecomeActive() {
            onResume()
        }
    }

    private fun showAppNotInstalledAlert() {
        dispatch_async(dispatch_get_main_queue()) {
            val presenter = presentedNavigationController ?: controller ?: return@dispatch_async
            if (presenter.presentedViewController != null) return@dispatch_async
            val alert = UIAlertController.alertControllerWithTitle(
                title = null,
                message = APP_NOT_INSTALLED_MESSAGE,
                preferredStyle = UIAlertControllerStyleAlert
            )
            alert.addAction(UIAlertAction.actionWithTitle("OK", UIAlertActionStyleDefault, null))
            presenter.presentViewController(alert, animated = true, completion = null)
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private class RedirectNavigationDelegate(
        private val onFinished: (url: String?) -> Unit,
        private val onFailed: (message: String) -> Unit,
        private val onResultPage: ((url: String, loaded: Boolean) -> Unit)? = null,
        private val onShowAppNotInstalledAlert: () -> Unit
    ) : NSObject(), WKNavigationDelegateProtocol {

        private var ewalletWebFallbackUrl: String? = null

        @ObjCSignatureOverride
        override fun webView(
            webView: WKWebView,
            decidePolicyForNavigationAction: WKNavigationAction,
            decisionHandler: (WKNavigationActionPolicy) -> Unit
        ) {
            val requestUrl = decidePolicyForNavigationAction.request.URL
            val requestPath = requestUrl?.absoluteString ?: ""
            val scheme = requestUrl?.scheme?.lowercase() ?: ""

            // 1. Result page navigation signal
            if (requestPath.contains(RESULT_PAGE_MARKER)) {
                onResultPage?.invoke(requestPath, false)
            }

            // 2. SCB Easy App Link
            // KIV: SCB Easy is disabled for this publish and comes back in the next one (Android's
            // handleScbEasyLink is commented out too). Uncomment this block to re-enable it.
            // if (requestPath.contains("scbeasy/easy_app_link.html")) {
            //     decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            //     if (requestUrl != null) {
            //         UIApplication.sharedApplication.openURL(requestUrl, emptyMap<Any?, Any?>(), null)
            //     }
            //     webView.evaluateJavaScript("document.getElementById('ref_no').value") { result, _ ->
            //         val refNo = (result as? String)?.replace("\"", "") ?: ""
            //         if (refNo.isNotEmpty()) {
            //             val scbLoadingUrl = "https://pay.merchant.razer.com/RMS/intermediate_app/loading.php?tranID=$refNo"
            //             val req = NSURLRequest(uRL = NSURL(string = scbLoadingUrl))
            //             webView.loadRequest(req)
            //         }
            //     }
            //     return
            // }

            // 3. External App Schemes (e.g. tngdwallet://, boostapp://, intent://)
            val isExternalScheme = scheme.isNotEmpty() && scheme !in INTERNAL_SCHEMES
            if (requestUrl != null && isExternalScheme) {
                decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
                if (scheme == "intent") {
                    val fallback = extractBrowserFallbackUrl(requestPath)
                    if (fallback != null) {
                        webView.loadRequest(NSURLRequest(uRL = NSURL(string = fallback)))
                        return
                    }
                }
                openExternalAppScheme(requestUrl)
                return
            }

            // 4. E-Wallet Universal Links (e.g. atome-my.onelink.me, myboost.app, etc.)
            if (requestUrl != null && isEwalletUniversalLink(requestUrl)) {
                if (requestPath == ewalletWebFallbackUrl) {
                    // Fallback web cashier inside WebView when native app is not installed
                    ewalletWebFallbackUrl = null
                } else {
                    decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
                    openEwalletUniversalLink(webView, requestUrl)
                    return
                }
            }

            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
        }

        private fun openExternalAppScheme(url: NSURL) {
            UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>()) { opened ->
                if (!opened) {
                    onShowAppNotInstalledAlert()
                }
            }
        }

        private fun openEwalletUniversalLink(webView: WKWebView, url: NSURL) {
            UIApplication.sharedApplication.openURL(
                url,
                options = mapOf<Any?, Any?>(UIApplicationOpenURLOptionUniversalLinksOnly to true)
            ) { opened ->
                if (!opened) {
                    ewalletWebFallbackUrl = url.absoluteString
                    webView.loadRequest(NSURLRequest(uRL = url))
                }
            }
        }

        override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
            val currentUrl = webView.URL?.absoluteString
            if (currentUrl != null && currentUrl.contains(RESULT_PAGE_MARKER)) {
                onResultPage?.invoke(currentUrl, true)
            }
            onFinished(currentUrl)
        }

        // Without these, a load failure (unreachable host, TLS error) previously left the
        // user stuck on a blank WKWebView with no feedback — surface it instead. -999
        // (NSURLErrorCancelled) only means a navigation was superseded by another, e.g. a
        // 3DS/bank redirect chain or a JS navigation before the load finished — not a failure.
        @ObjCSignatureOverride
        override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) {
            if (withError.code == NSURLErrorCancelled) return
            onFailed("WebView navigation failed: ${withError.localizedDescription}")
        }

        @ObjCSignatureOverride
        override fun webView(webView: WKWebView, didFailProvisionalNavigation: WKNavigation?, withError: NSError) {
            if (withError.code == NSURLErrorCancelled) return
            onFailed("WebView failed to load: ${withError.localizedDescription}")
        }
    }

    override fun initialize(merchantId: String, sessionId: String) {
        this.merchantId = merchantId
        this.sessionId = sessionId
    }

    override fun startCheckOut(onComplete: (String) -> Unit) {
        // Minimal cross-platform default: call completion immediately.
        // If your checkout flow is URL-based, prefer calling `openGetUrl(...)` from the caller
        // with the URL returned by your backend.
        onComplete("")
    }

    // Every dismissal path must release both fields together — cancelButtonHandler's closure
    // captures navigationController, so leaving it set keeps the dismissed WKWebView (and
    // whatever payment-page session state it holds) reachable until the next createWebView() call
    // overwrites it.
    private fun releasePresentedController() {
        lifecycleObserver?.let {
            NSNotificationCenter.defaultCenter.removeObserver(it)
            lifecycleObserver = null
        }
        presentedNavigationController = null
        cancelButtonHandler = null
        navigationDelegate = null
    }

    // Builds the presented, full-screen WKWebView + its navigation delegate — shared by
    // openGetUrl and openPostUrl, which only differ in what gets loaded into it.
    @OptIn(ExperimentalForeignApi::class)
    private fun createWebView(
        onSuccess: () -> Unit,
        onFailure: (String?) -> Unit,
        onClose: () -> Unit,
        closeButtonDisplay: Boolean,
        onResultPage: ((url: String, loaded: Boolean) -> Unit)? = null,
        onAppPaused: (() -> Unit)? = null,
        onAppResume: (() -> Unit)? = null
    ): Pair<UINavigationController, WKWebView> {
        val webViewController = UIViewController()
        val webView = WKWebView(frame = webViewController.view.bounds, configuration = WKWebViewConfiguration())
        webView.autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight

        webViewController.view.addSubview(webView)

        val navigationController = UINavigationController(rootViewController = webViewController)
        navigationController.modalPresentationStyle = UIModalPresentationFullScreen
        presentedNavigationController = navigationController

        if (onAppPaused != null || onAppResume != null) {
            val observer = LifecycleObserver(
                onPause = { onAppPaused?.invoke() },
                onResume = { onAppResume?.invoke() }
            )
            lifecycleObserver = observer
            NSNotificationCenter.defaultCenter.addObserver(
                observer = observer,
                selector = NSSelectorFromString("appDidEnterBackground"),
                name = UIApplicationDidEnterBackgroundNotification,
                `object` = null
            )
            NSNotificationCenter.defaultCenter.addObserver(
                observer = observer,
                selector = NSSelectorFromString("appDidBecomeActive"),
                name = UIApplicationDidBecomeActiveNotification,
                `object` = null
            )
        }

        // closeButtonDisplay mirrors mp_closebutton_display's meaning on the legacy Webcore engine
        // (see ProvideFiuuPaymentSDK.kt) — false omits this button entirely, same as Android's
        // AndroidWebViewBridge skipping its close bar. The interactive swipe-to-dismiss gesture is
        // already disabled (modalPresentationStyle above), so with it false the only way out is
        // the gateway page's own cancel link (if any).
        if (closeButtonDisplay) {
            val handler = CancelButtonHandler {
                navigationController.dismissViewControllerAnimated(true) { onClose() }
                releasePresentedController()
            }
            cancelButtonHandler = handler
            webViewController.navigationItem.setLeftBarButtonItem(
                UIBarButtonItem(
                    title = "Cancel",
                    style = platform.UIKit.UIBarButtonItemStyle.UIBarButtonItemStylePlain,
                    target = handler,
                    action = NSSelectorFromString("invoke")
                )
            )
        }

        val delegate = RedirectNavigationDelegate(
            onFinished = { currentUrl ->
                if (currentUrl?.contains("success=true") == true) {
                    onSuccess()
                    navigationController.dismissViewControllerAnimated(true) { onClose() }
                    releasePresentedController()
                } else if (currentUrl?.contains("success=false") == true) {
                    onFailure("Payment failed")
                    navigationController.dismissViewControllerAnimated(true) { onClose() }
                    releasePresentedController()
                }
            },
            onFailed = { message ->
                onFailure(message)
                navigationController.dismissViewControllerAnimated(true) { onClose() }
                releasePresentedController()
            },
            onResultPage = onResultPage,
            onShowAppNotInstalledAlert = {
                showAppNotInstalledAlert()
            }
        )
        navigationDelegate = delegate
        webView.navigationDelegate = delegate

        return navigationController to webView
    }

    @OptIn(ExperimentalForeignApi::class, ExperimentalEncodingApi::class)
    override fun openGetUrl(
        url: String,
        onSuccess: () -> Unit,
        onFailure: (String?) -> Unit,
        onClose: () -> Unit,
        urlType: String?,
        headers: Map<String, String>?,
        closeButtonDisplay: Boolean,
        onResultPage: ((url: String, loaded: Boolean) -> Unit)?,
        onAppPaused: (() -> Unit)?,
        onAppResume: (() -> Unit)?
    ) {
        val presenter = controller ?: return onFailure("WebViewBridge controller not set")
        if (com.fiuu.xdkandroid.diagnostics.DiagnosticChaosEngine.activeWebViewLoadFail) {
            return onFailure("Simulated WebView TLS / Connection Failure (Chaos Mode)")
        }
        val (navigationController, webView) = createWebView(onSuccess, onFailure, onClose, closeButtonDisplay, onResultPage, onAppPaused, onAppResume)

        if (urlType == "data") {
            // The gateway returned inline base64-encoded HTML (urlType == "data") rather than
            // a redirect URL — render it directly instead of navigating anywhere. The base URL is
            // the form action's origin (see inlineHtmlBaseUrl), so the page's auto-submitted form
            // doesn't POST from an opaque origin.
            val html = try {
                Base64.decode(url).decodeToString()
            } catch (e: Exception) {
                return onFailure("Failed to decode HTML payload: ${e.message}")
            }
            webView.loadHTMLString(html, baseURL = inlineHtmlBaseUrl(html)?.let { NSURL(string = it) })
        } else {
            val nsUrl = NSURL(string = url)
            val request = NSMutableURLRequest(uRL = nsUrl)
            headers?.forEach { (key, value) -> request.setValue(value, forHTTPHeaderField = key) }
            webView.loadRequest(request)
        }

        presenter.presentViewController(navigationController, animated = true, completion = null)
        onAppResume?.invoke()
    }

    // For gateways (e.g. FPX) that require their own form fields submitted as a real POST body —
    // see PaymentCreateResponse.requestData. No headers parameter (see WebViewBridge.openPostUrl).
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun openPostUrl(
        url: String,
        postData: Map<String, String>,
        onSuccess: () -> Unit,
        onFailure: (String?) -> Unit,
        onClose: () -> Unit,
        urlType: String?,
        closeButtonDisplay: Boolean,
        onResultPage: ((url: String, loaded: Boolean) -> Unit)?,
        onAppPaused: (() -> Unit)?,
        onAppResume: (() -> Unit)?
    ) {
        if (urlType == "data") {
            // Inline base64-encoded HTML payload — there's no URL to POST postData to, so
            // render it exactly like openGetUrl's "data" branch does.
            openGetUrl(url = url, onSuccess = onSuccess, onFailure = onFailure, onClose = onClose, urlType = urlType, headers = null, closeButtonDisplay = closeButtonDisplay, onResultPage = onResultPage, onAppPaused = onAppPaused, onAppResume = onAppResume)
            return
        }

        val presenter = controller ?: return onFailure("WebViewBridge controller not set")
        if (com.fiuu.xdkandroid.diagnostics.DiagnosticChaosEngine.activeWebViewLoadFail) {
            return onFailure("Simulated WebView TLS / Connection Failure (Chaos Mode)")
        }
        val (navigationController, webView) = createWebView(onSuccess, onFailure, onClose, closeButtonDisplay, onResultPage, onAppPaused, onAppResume)

        val nsUrl = NSURL(string = url)
        val request = NSMutableURLRequest(uRL = nsUrl)
        request.setHTTPMethod("POST")
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField = "Content-Type")

        val encodedBody = postData.entries.joinToString("&") { (k, v) -> "${percentEncode(k)}=${percentEncode(v)}" }
        val bodyBytes = encodedBody.encodeToByteArray()
        // addressOf(0) on an empty ByteArray is out-of-bounds — only pin/encode when there's
        // actually a body (postData could legitimately be empty for some channels).
        if (bodyBytes.isNotEmpty()) {
            val bodyData = bodyBytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bodyBytes.size.toULong())
            }
            request.setHTTPBody(bodyData)
        }

        webView.loadRequest(request)
        presenter.presentViewController(navigationController, animated = true, completion = null)
        onAppResume?.invoke()
    }

    // Plain RFC 3986 percent-encoding over raw UTF-8 bytes — avoids relying on NSString/
    // NSCharacterSet Foundation interop for something this simple.
    private fun percentEncode(value: String): String {
        val unreserved = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
        val builder = StringBuilder()
        for (byte in value.encodeToByteArray()) {
            val char = byte.toInt().toChar()
            if (byte >= 0 && unreserved.contains(char)) {
                builder.append(char)
            } else {
                builder.append('%')
                builder.append((byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0'))
            }
        }
        return builder.toString()
    }

    override fun close() {
        presentedNavigationController?.dismissViewControllerAnimated(true, completion = null)
        releasePresentedController()
    }
}
