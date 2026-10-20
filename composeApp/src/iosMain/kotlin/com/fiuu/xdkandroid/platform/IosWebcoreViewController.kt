package com.fiuu.xdkandroid.platform

import com.fiuu.xdkandroid.applepay.ApplePayExpressResult
import com.fiuu.xdkandroid.applepay.applePayErrorResponse
import com.fiuu.xdkandroid.applepay.presentApplePayExpressCheckout
import com.fiuu.xdkandroid.data.mapper.WebcoreResponseMapper
import com.fiuu.xdkandroid.data.remote.dto.PaymentResponseDto
import com.fiuu.xdkandroid.utils.FiuuLogger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.ObjCSignatureOverride
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import platform.Foundation.NSError
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplicationOpenURLOptionUniversalLinksOnly
import platform.UIKit.UIBarButtonItem
import platform.UIKit.UIBarButtonItemStyle
import platform.UIKit.UIBarStyleBlack
import platform.UIKit.UIColor
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.UIDevice
import platform.UIKit.UIModalPresentationFullScreen
import platform.UIKit.UINavigationBarAppearance
import platform.UIKit.UINavigationController
import platform.UIKit.UIViewController
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIApplication
import platform.UIKit.navigationItem
import platform.UIKit.navigationController
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKUIDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebpagePreferences
import platform.WebKit.WKWebsiteDataStore
import platform.WebKit.WKWindowFeatures
import platform.Foundation.NSData
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private const val APP_NOT_INSTALLED_MESSAGE = "The app needed for this payment isn't installed"

private val RECEIPT_CAPTURE_ENHANCEMENT_SCRIPT = """
(function() {
    var SQUARE_711_BASE64 = '$SQUARE_SEVEN_ELEVEN_PNG_BASE64';

    // Preload verified square PNG into browser memory cache
    try {
        var preloadImg = new Image();
        preloadImg.src = SQUARE_711_BASE64;
    } catch(e) {}

    function ensureReceiptElements() {
        // 1. Live logo inlining: ensure img#receipt-logo or .receipt-logo has a valid data URI
        // html2canvas drops cross-origin http/https URLs unless they are data URIs
        var liveImg = document.getElementById('receipt-logo') || document.querySelector('.receipt-logo');
        if (liveImg) {
            if (!liveImg.src || !liveImg.src.startsWith('data:image/')) {
                liveImg.src = SQUARE_711_BASE64;
                liveImg.setAttribute('src', SQUARE_711_BASE64);
            }
            liveImg.style.maxWidth = '80px';
            liveImg.style.maxHeight = '80px';
            liveImg.style.objectFit = 'contain';
            liveImg.style.display = 'block';
            liveImg.style.margin = '0 auto';
        }

        // 2. Fallback protection: if Angular ever created .header-fallback, replace with 80x80 square logo
        var fallback = document.querySelector('.header-fallback');
        if (fallback) {
            var existingImg = fallback.querySelector('img.receipt-fallback-logo');
            if (!existingImg) {
                fallback.innerHTML = '';
                var img = document.createElement('img');
                img.id = 'receipt-logo';
                img.className = 'receipt-logo receipt-fallback-logo';
                img.alt = '7-Eleven';
                img.src = SQUARE_711_BASE64;
                img.style.maxWidth = '80px';
                img.style.maxHeight = '80px';
                img.style.objectFit = 'contain';
                img.style.display = 'block';
                img.style.margin = '0 auto';
                fallback.appendChild(img);
            }
        }

        // 3. Mark receipt-footer to be ignored by html2canvas
        var footer = document.querySelector('.receipt-footer');
        if (footer && !footer.hasAttribute('data-html2canvas-ignore')) {
            footer.setAttribute('data-html2canvas-ignore', 'true');
        }
    }

    window.ensureReceiptElements = ensureReceiptElements;
    ensureReceiptElements();

    if (window._receiptCaptureEnhanced) return;
    window._receiptCaptureEnhanced = true;

    var observer = new MutationObserver(function() {
        ensureReceiptElements();
    });
    if (document.body) {
        observer.observe(document.body, { childList: true, subtree: true, attributes: true, attributeFilter: ['src'] });
    }

    var polls = 0;
    var timer = setInterval(function() {
        ensureReceiptElements();
        polls++;
        if (polls > 20) clearInterval(timer);
    }, 150);

    document.addEventListener('click', function(e) {
        var el = e.target;
        var isSave = false;
        while (el && el !== document.body) {
            var cls = (el.className && typeof el.className === 'string') ? el.className : '';
            var text = (el.innerText || el.textContent || '').trim().toLowerCase();
            if (cls.indexOf('save-button') !== -1 || text.indexOf('save image') !== -1) {
                isSave = true;
                break;
            }
            el = el.parentElement;
        }

        if (isSave) {
            // 1. Reset scroll so html2canvas doesn't clip top content on iOS WebKit
            window.scrollTo(0, 0);
            if (document.documentElement) document.documentElement.scrollTop = 0;
            if (document.body) document.body.scrollTop = 0;

            // 2. Synchronously ensure logo has data URI right before html2canvas clones the DOM
            ensureReceiptElements();
            var liveImg = document.getElementById('receipt-logo') || document.querySelector('.receipt-logo');
            if (liveImg) {
                liveImg.src = SQUARE_711_BASE64;
                liveImg.setAttribute('src', SQUARE_711_BASE64);
            }

            // 3. Mark and hide footer during capture
            var footer = document.querySelector('.receipt-footer');
            if (footer) {
                footer.setAttribute('data-html2canvas-ignore', 'true');
                setTimeout(function() {
                    footer.style.setProperty('display', 'none', 'important');
                }, 10);
                setTimeout(function() {
                    footer.style.removeProperty('display');
                }, 2500);
            }
        }
    }, true);
})();
""".trimIndent()

internal fun evaluateJs(webView: WKWebView, script: String) {
    webView.evaluateJavaScript(script) { _, error ->
        if (error != null) {
            FiuuLogger.log("IosWebcore: evaluateJavaScript error: ${error.localizedDescription}")
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class WebcoreCloseHandler(private val onAction: () -> Unit) : NSObject() {
    @OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @ObjCAction
    fun invoke() {
        onAction()
    }
}

@OptIn(ExperimentalForeignApi::class, ExperimentalEncodingApi::class)
private class WebcoreNavDelegate(
    private val controller: FiuuWebcoreViewController
) : NSObject(), WKNavigationDelegateProtocol, WKUIDelegateProtocol {

    private fun trimPath(path: String, prefix: String): String {
        val stripped = path.removePrefix(prefix)
            .replace("_", "=")
            .replace("-", "+")
        val pad = (4 - stripped.length % 4) % 4
        return if (pad in 1..3) stripped + "=".repeat(pad) else stripped
    }

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

    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        val urlString = webView.URL?.absoluteString ?: ""
        FiuuLogger.log("IosWebcore: didFinishNavigation url=$urlString")

        val mainWebView = controller.mainWebView
        if (webView == mainWebView) {
            if (urlString.isNotEmpty() && !urlString.startsWith("about:")) {
                // Scheduled single-shot updateSdkData with 1000ms delay to allow Angular/JS to register updateSdkData
                val paramsJson = controller.paymentParamsJson
                val script = "setTimeout(function() { if (typeof updateSdkData === 'function') { updateSdkData($paramsJson); } }, 1000);"
                evaluateJs(mainWebView, script)

                val applePayClickScript = """
                    (function() {
                        if (window._applePayBridgeInjected) return;
                        window._applePayBridgeInjected = true;
                        document.addEventListener('click', function(e) {
                            var el = e.target;
                            while (el && el !== document.body) {
                                var text = (el.innerText || el.textContent || '').trim().toLowerCase();
                                var title = (el.getAttribute('title') || '').toLowerCase();
                                var mask = (el.getAttribute('maskname') || '').toLowerCase();
                                if (text === 'apple pay' || title.indexOf('apple pay') !== -1 || mask.indexOf('apple pay') !== -1) {
                                    window.location.href = 'mpclickapbutton://';
                                    e.preventDefault();
                                    e.stopPropagation();
                                    return;
                                }
                                el = el.parentElement;
                            }
                        }, true);
                    })();
                """.trimIndent()
                evaluateJs(mainWebView, applePayClickScript)

                if (controller.isShowingReceipt) {
                    controller.setupReceiptCaptureEnhancements()
                }
            }
        } else {
            val openOverride = """
                (function() {
                    window.open = function (url) { window.location.href = url; };
                    window.close = function () { window.location.href = window.location.href; };
                })();
            """.trimIndent()
            evaluateJs(webView, openOverride)
        }

        // TNG-EWALLET intermediate redirect extraction
        if (urlString.contains("intermediate_appTNG-EWALLET.php") ||
            urlString.contains("intermediate_app/processing.php") ||
            urlString.contains("intermediate_app/process.php")
        ) {
            webView.evaluateJavaScript("document.getElementById('systembrowserurl').innerHTML") { result, _ ->
                val resultString = result as? String ?: ""
                if (resultString.isNotEmpty()) {
                    val decodedData = try {
                        Base64.decode(resultString).decodeToString()
                    } catch (e: Exception) {
                        resultString
                    }
                    if (decodedData.isNotEmpty()) {
                        val appUrl = NSURL.URLWithString(decodedData)
                        if (appUrl != null) {
                            UIApplication.sharedApplication.openURL(
                                appUrl,
                                emptyMap<Any?, Any>()
                            ) { success ->
                                FiuuLogger.log("IosWebcore: TNG intermediate openURL result: $success")
                            }
                        }
                    }
                }
            }
        }
    }

    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit
    ) {
        val requestUrl = decidePolicyForNavigationAction.request.URL
        val requestPath = requestUrl?.absoluteString ?: ""
        val scheme = requestUrl?.scheme?.lowercase() ?: ""
        val mainWebView = controller.mainWebView ?: webView

        FiuuLogger.log("IosWebcore: decidePolicyForNavigationAction -> $requestPath (scheme=$scheme)")

        // 1. Only notify Webcore of child-view navigations with 1000ms debounce
        if (webView != controller.mainWebView && requestPath.isNotEmpty() && !requestPath.startsWith("about:")) {
            val escapedPath = requestPath.replace("\\", "\\\\").replace("\"", "\\\"")
            val urlUpdateScript = "setTimeout(function() { if (typeof nativeWebRequestUrlUpdates === 'function') { nativeWebRequestUrlUpdates({\"requestPath\": \"$escapedPath\"}); } }, 1000);"
            evaluateJs(mainWebView, urlUpdateScript)
        }

        // 2. SCB Easy App Link
        if (requestPath.contains("scbeasy/easy_app_link.html")) {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            if (requestUrl != null) {
                UIApplication.sharedApplication.openURL(requestUrl, emptyMap<Any?, Any>(), null)
            }
            webView.evaluateJavaScript("document.getElementById('ref_no').value") { result, _ ->
                val refNo = (result as? String)?.replace("\"", "") ?: ""
                if (refNo.isNotEmpty()) {
                    val scbLoadingUrl = "https://pay.merchant.razer.com/RMS/intermediate_app/loading.php?tranID=$refNo"
                    val req = NSURLRequest(uRL = NSURL(string = scbLoadingUrl))
                    webView.loadRequest(req)
                }
            }
            return
        }

        // 3. External Legal URLs (Terms & Privacy Policy)
        if (requestUrl != null &&
            requestUrl.scheme?.lowercase() == "https" &&
            requestUrl.host?.lowercase() == "fiuu.com" &&
            (requestUrl.path == "/privacy-policy/" || requestUrl.path == "/terms-of-services/my")
        ) {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            UIApplication.sharedApplication.openURL(requestUrl, emptyMap<Any?, Any>(), null)
            return
        }

        // 4. Transaction Results Callback
        if (scheme == "mptransactionresults") {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            if (webView != controller.mainWebView) {
                FiuuLogger.log("IosWebcore: Ignoring transaction result from non-main webview")
                return
            }
            val trimmed = trimPath(requestPath, "mptransactionresults://")
            val decodedJson = try {
                Base64.decode(trimmed).decodeToString()
            } catch (e: Exception) {
                FiuuLogger.log("IosWebcore: Base64 decode error for results: ${e.message}")
                trimmed
            }
            FiuuLogger.log("IosWebcore: Decoded transaction result: $decodedJson")
            val responseDto = WebcoreResponseMapper.mapJsonToDto(decodedJson)

            // Check if mp_request_type is "Receipt" (e.g., Cash 7-Eleven / OTC payment slip)
            val jsonElement = try {
                Json.parseToJsonElement(decodedJson).jsonObject
            } catch (_: Exception) {
                null
            }
            val mpRequestType = jsonElement?.get("mp_request_type")?.jsonPrimitive?.contentOrNull
            val hasErrorCode = jsonElement?.get("error_code")?.jsonPrimitive?.contentOrNull != null ||
                jsonElement?.get("Error")?.jsonPrimitive?.contentOrNull != null ||
                jsonElement?.get("error")?.jsonPrimitive?.contentOrNull != null
            val isReceipt = mpRequestType.equals("Receipt", ignoreCase = true) && !hasErrorCode

            if (isReceipt) {
                FiuuLogger.log("IosWebcore: Payment receipt detected (status=${responseDto.status}) — presenting receipt slip in WebView")
                controller.handleReceiptResult(responseDto)
            } else {
                controller.finishWithResult(responseDto)
            }
            return
        }

        // 5. Open Popup Window Callback
        if (scheme == "mpopenmolpaywindow") {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            if (webView != controller.mainWebView) {
                FiuuLogger.log("IosWebcore: Ignoring popup request from non-main webview")
                return
            }
            val trimmed = trimPath(requestPath, "mpopenmolpaywindow://")
            val decodedHtml = try {
                Base64.decode(trimmed).decodeToString()
            } catch (e: Exception) {
                ""
            }
            if (decodedHtml.isNotEmpty()) {
                controller.openPopupWindow(decodedHtml)
            }
            return
        }

        // 6. Close All Windows Callback
        if (scheme == "mpcloseallwindows") {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            controller.closeAllPopupWindows()
            return
        }

        // 7. Close popup and run script on main window
        if (scheme == "mpclosepopupandrunscriptonmolpaywindow") {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            if (webView != controller.mainWebView) {
                FiuuLogger.log("IosWebcore: Ignoring close-and-run-script from non-main webview")
                return
            }
            val trimmed = trimPath(requestPath, "mpclosepopupandrunscriptonmolpaywindow://")
            val script = try {
                Base64.decode(trimmed).decodeToString()
            } catch (e: Exception) {
                ""
            }
            if (script.isNotEmpty()) {
                evaluateJs(mainWebView, script)
            }
            return
        }

        // 8. Run script on popup window
        if (scheme == "mprunscriptonpopup") {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            if (webView != controller.mainWebView) {
                FiuuLogger.log("IosWebcore: Ignoring run-script-on-popup from non-main webview")
                return
            }
            val trimmed = trimPath(requestPath, "mprunscriptonpopup://")
            val script = try {
                Base64.decode(trimmed).decodeToString()
            } catch (e: Exception) {
                ""
            }
            val popup = controller.activePopupWebView
            if (script.isNotEmpty() && popup != null) {
                evaluateJs(popup, script)
            }
            return
        }

        // 9. Apple Pay Scheme Interception
        if (scheme == "mpclickapbutton") {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            if (webView != controller.mainWebView) {
                FiuuLogger.log("IosWebcore: Ignoring Apple Pay trigger from non-main webview")
                return
            }
            val trimmed = trimPath(requestPath, "mpclickapbutton://")
            val normalized = trimmed.replace("-", "+").replace("_", "=")
            val pad = (4 - normalized.length % 4) % 4
            val padded = normalized + "=".repeat(pad)
            val decodedJson = try {
                Base64.decode(padded).decodeToString()
            } catch (e: Exception) {
                FiuuLogger.log("IosWebcore: Base64 decode error for Apple Pay: ${e.message}")
                ""
            }
            FiuuLogger.log("IosWebcore: Decoded Apple Pay payload: $decodedJson")
            controller.launchModernApplePay(decodedJson)
            return
        }

        // 10. Instruction Image Capture Callback (Save Slip / Barcode)
        if (scheme == "mppinstructioncapture" || scheme == "mpinstructioncapture") {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            val prefix = if (scheme == "mppinstructioncapture") "mppinstructioncapture://" else "mpinstructioncapture://"
            val trimmed = trimPath(requestPath, prefix)
            try {
                // Restore receipt-footer visibility in WebView
                evaluateJs(controller.mainWebView ?: webView, "var f = document.querySelector('.receipt-footer'); if (f) f.style.removeProperty('display');")

                val decodedJson = Base64.decode(trimmed).decodeToString()
                val jsonObject = Json.parseToJsonElement(decodedJson).jsonObject
                val base64Img = jsonObject["base64ImageUrlData"]?.jsonPrimitive?.contentOrNull
                    ?: jsonObject["base64Image"]?.jsonPrimitive?.contentOrNull
                    ?: jsonObject["image"]?.jsonPrimitive?.contentOrNull
                val filename = jsonObject["filename"]?.jsonPrimitive?.contentOrNull
                    ?: "fiuu_payment_slip"

                if (!base64Img.isNullOrEmpty()) {
                    val raw = if (base64Img.contains(",")) base64Img.substringAfter(",") else base64Img
                    val clean = raw.trim().replace("\n", "").replace("\r", "")
                    val pad = (4 - clean.length % 4) % 4
                    val padded = if (pad in 1..3) clean + "=".repeat(pad) else clean
                    val imgBytes = Base64.decode(padded)
                    controller.saveInstructionImage(imgBytes, filename)
                } else {
                    FiuuLogger.log("IosWebcore: No image data found in instruction capture payload")
                }
            } catch (e: Exception) {
                FiuuLogger.log("IosWebcore: Failed to decode instruction capture payload: ${e.message}")
            }
            return
        }

        // 11. External E-Wallet / Bank App hand-offs
        val internalSchemes = setOf(
            "http", "https", "about", "blob", "data", "javascript", "file",
            "mpopenmolpaywindow", "mpcloseallwindows", "mpclosepopupandrunscriptonmolpaywindow",
            "mptransactionresults", "mprunscriptonpopup", "mpinstructioncapture", "mppinstructioncapture",
            "mpclickapbutton"
        )
        val isExternalScheme = scheme.isNotEmpty() && scheme !in internalSchemes

        if (requestUrl != null && isExternalScheme) {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            openExternalAppScheme(requestUrl)
            return
        }

        if (requestUrl != null && isEwalletUniversalLink(requestUrl)) {
            if (requestPath == ewalletWebFallbackUrl) {
                // Our own in-WebView fallback for a wallet app that isn't installed — let it load.
                ewalletWebFallbackUrl = null
            } else {
                decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
                openEwalletUniversalLink(webView, requestUrl)
                return
            }
        }

        decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
    }

    // Wallet cashier links that must open the wallet app (its approval step doesn't work in a
    // WebView). Matched on the parsed host and path, not a substring of the URL — a gateway page
    // carrying "myboost.app" in a query parameter must not trigger a hand-off. Subdomains match,
    // as the old substring check did.
    private val ewalletUniversalLinks = listOf(
        "atome-my.onelink.me" to "",
        "myboost.app" to "",
        "app.shopback.com" to "/pay",
        "m.tngdigital.com.my" to "",
    )

    // Set while the WebView loads a wallet link itself because the app isn't installed, so that
    // navigation isn't intercepted and handed off again.
    private var ewalletWebFallbackUrl: String? = null

    private fun isEwalletUniversalLink(url: NSURL): Boolean {
        if (url.scheme?.lowercase() != "https") return false
        val host = url.host?.lowercase() ?: return false
        val path = url.path ?: ""
        return ewalletUniversalLinks.any { (linkHost, pathPrefix) ->
            (host == linkHost || host.endsWith(".$linkHost")) && path.startsWith(pathPrefix)
        }
    }

    // universalLinksOnly: open the wallet app or nothing — without it iOS falls back to Safari,
    // taking the user out of the SDK. When the app isn't installed, load the wallet's web
    // cashier in this WebView instead.
    private fun openEwalletUniversalLink(webView: WKWebView, url: NSURL) {
        FiuuLogger.log("IosWebcore: E-wallet universal link, handing off to app: ${url.host}${url.path}")
        UIApplication.sharedApplication.openURL(
            url,
            options = mapOf<Any?, Any?>(UIApplicationOpenURLOptionUniversalLinksOnly to true)
        ) { opened ->
            if (!opened) {
                FiuuLogger.log("IosWebcore: E-wallet app not installed, loading web cashier in WebView")
                ewalletWebFallbackUrl = url.absoluteString
                webView.loadRequest(NSURLRequest(uRL = url))
            }
        }
    }

    // Custom schemes (wallet / bank apps). If no installed app handles the scheme, openURL
    // reports false — tell the user instead of leaving them on a page that did nothing.
    private fun openExternalAppScheme(url: NSURL) {
        FiuuLogger.log("IosWebcore: External scheme, handing off to app: ${url.scheme}")
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>()) { opened ->
            if (!opened) {
                FiuuLogger.log("IosWebcore: No app installed for scheme ${url.scheme}")
                showAppNotInstalledAlert()
            }
        }
    }

    private fun showAppNotInstalledAlert() {
        if (controller.presentedViewController != null) return
        val alert = UIAlertController.alertControllerWithTitle(
            title = null,
            message = APP_NOT_INSTALLED_MESSAGE,
            preferredStyle = UIAlertControllerStyleAlert
        )
        alert.addAction(UIAlertAction.actionWithTitle("OK", UIAlertActionStyleDefault, null))
        controller.presentViewController(alert, animated = true, completion = null)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) {
        FiuuLogger.log("IosWebcore: didFailNavigation: ${withError.localizedDescription}")
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFailProvisionalNavigation: WKNavigation?, withError: NSError) {
        FiuuLogger.log("IosWebcore: didFailProvisionalNavigation: ${withError.localizedDescription}")
    }
}

@OptIn(ExperimentalForeignApi::class)
class FiuuWebcoreViewController(
    private val targetUrl: String,
    val paymentParamsJson: String,
    private val isShowCloseButton: Boolean,
    val rawParams: Map<String, Any?> = emptyMap(),
    private val onResult: (PaymentResponseDto) -> Unit,
    private val onClose: () -> Unit
) : UIViewController(nibName = null, bundle = null) {

    var mainWebView: WKWebView? = null
    var activePopupWebView: WKWebView? = null
    private var navDelegate: WebcoreNavDelegate? = null
    private var closeHandler: WebcoreCloseHandler? = null
    private var hasDispatchedResult: Boolean = false
    internal var isShowingReceipt: Boolean = false
    private var pendingReceiptResponse: PaymentResponseDto? = null
    private val webConfig = WKWebViewConfiguration().apply {
        websiteDataStore = WKWebsiteDataStore.nonPersistentDataStore()
        defaultWebpagePreferences = WKWebpagePreferences().apply {
            allowsContentJavaScript = true
        }
    }

    fun launchModernApplePay(decodedJson: String) {
        val mergedParams = rawParams.toMutableMap()
        if (decodedJson.isNotEmpty()) {
            try {
                val jsonElement = Json.parseToJsonElement(decodedJson).jsonObject
                jsonElement.forEach { (k, v) ->
                    val str = v.jsonPrimitive.contentOrNull
                    if (!str.isNullOrEmpty()) {
                        mergedParams[k] = str
                    }
                }
            } catch (e: Exception) {
                FiuuLogger.log("IosWebcore: Failed to parse Apple Pay JSON payload: ${e.message}")
            }
        }

        val isExpressMode = when (val exp = mergedParams["mp_express_mode"]) {
            is Boolean -> exp
            is String -> exp.equals("true", ignoreCase = true)
            else -> false
        }

        dispatch_async(dispatch_get_main_queue()) {
            presentApplePayExpressCheckout(mergedParams) { result ->
                when (result) {
                    is ApplePayExpressResult.PaymentReceived -> {
                        finishWithResult(result.response)
                    }
                    ApplePayExpressResult.Cancelled -> {
                        if (isExpressMode) {
                            finishWithClose()
                        }
                    }
                    is ApplePayExpressResult.SheetError -> {
                        if (isExpressMode) {
                            finishWithResult(applePayErrorResponse("APPLE_PAY_ERROR", result.message))
                        } else {
                            FiuuLogger.log("IosWebcore: Apple Pay sheet error: ${result.message}")
                        }
                    }
                    ApplePayExpressResult.InProgress -> {
                        FiuuLogger.log("IosWebcore: Apple Pay express payment in progress")
                    }
                }
            }
        }
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.whiteColor

        // Navigation Bar Configuration — same colours as the native PaymentScaffold app bar:
        // AppColors.Primary background, white title, AppColors.Secondary (green) Cancel button.
        val appearance = UINavigationBarAppearance().apply {
            configureWithOpaqueBackground()
            backgroundColor = UIColor.colorWithRed(0x00 / 255.0, 0x3B / 255.0, 0xA0 / 255.0, 1.0)
            titleTextAttributes = mapOf<Any?, Any?>(NSForegroundColorAttributeName to UIColor.whiteColor)
        }
        navigationController?.navigationBar?.standardAppearance = appearance
        navigationController?.navigationBar?.scrollEdgeAppearance = appearance
        navigationController?.navigationBar?.compactAppearance = appearance
        // Dark bar style makes the navigation controller ask for a light (white) status bar.
        navigationController?.navigationBar?.barStyle = UIBarStyleBlack
        navigationController?.navigationBar?.tintColor =
            UIColor.colorWithRed(0x44 / 255.0, 0xD6 / 255.0, 0x2C / 255.0, 1.0)

        if (isShowCloseButton) {
            val handler = WebcoreCloseHandler { finishWithClose() }
            closeHandler = handler
            val closeBtn = UIBarButtonItem(
                title = "Cancel",
                style = UIBarButtonItemStyle.UIBarButtonItemStylePlain,
                target = handler,
                action = NSSelectorFromString("invoke")
            )
            navigationItem.leftBarButtonItem = closeBtn
        }

        val delegate = WebcoreNavDelegate(this)
        navDelegate = delegate

        val webView = WKWebView(frame = view.bounds, configuration = webConfig).apply {
            autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
            scrollView.bounces = false
            allowsLinkPreview = true
            navigationDelegate = delegate
            UIDelegate = delegate
        }
        mainWebView = webView
        view.addSubview(webView)

        val request = NSURLRequest(uRL = NSURL(string = targetUrl))
        webView.loadRequest(request)
    }

    fun openPopupWindow(htmlString: String) {
        closeAllPopupWindows()
        val popup = WKWebView(frame = view.bounds, configuration = webConfig).apply {
            autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
            navigationDelegate = navDelegate
            UIDelegate = navDelegate
        }
        activePopupWebView = popup
        view.addSubview(popup)
        popup.loadHTMLString(htmlString, baseURL = null)
    }

    fun closeAllPopupWindows() {
        activePopupWebView?.let { popup ->
            popup.navigationDelegate = null
            popup.UIDelegate = null
            popup.removeFromSuperview()
        }
        activePopupWebView = null
    }

    fun handleReceiptResult(response: PaymentResponseDto) {
        closeAllPopupWindows()
        isShowingReceipt = true
        pendingReceiptResponse = response

        dispatch_async(dispatch_get_main_queue()) {
            navigationItem.title = "Payment Slip"

            val handler = WebcoreCloseHandler {
                pendingReceiptResponse?.let { finishWithResult(it) } ?: finishWithClose()
            }
            closeHandler = handler
            val closeBtn = UIBarButtonItem(
                title = "Close",
                style = UIBarButtonItemStyle.UIBarButtonItemStyleDone,
                target = handler,
                action = NSSelectorFromString("invoke")
            )
            navigationItem.leftBarButtonItem = closeBtn

            // Inject receipt capture enhancements
            setupReceiptCaptureEnhancements()
        }
    }

    fun setupReceiptCaptureEnhancements() {
        val webView = mainWebView ?: return
        evaluateJs(webView, RECEIPT_CAPTURE_ENHANCEMENT_SCRIPT)
    }

    fun saveInstructionImage(bytes: ByteArray, filename: String) {
        val imageSaver = IosImageSaver(viewControllerProvider = { this })
        imageSaver.saveImage(bytes, filename) { success, error ->
            if (success) {
                FiuuLogger.log("IosWebcore: Instruction image saved/shared successfully ($filename)")
            } else {
                FiuuLogger.log("IosWebcore: Failed to save instruction image: $error")
            }
        }
    }

    fun finishWithResult(response: PaymentResponseDto) {
        if (!hasDispatchedResult) {
            hasDispatchedResult = true
            if (response.status == "00" || response.statusCheck) {
                com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.logHostApproval(
                    process = com.fiuu.xdkandroid.telemetry.model.TelemetryProcess.ONLINE_PAYMENT_OUTCOME,
                    statusCode = response.status
                )
            } else if (response.status == "22") {
                // Pending result (e.g. issued OTC/Cash voucher awaiting payment at counter) is NOT a decline
                com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.logTerminalOutcome(
                    status = response.status,
                    message = response.message,
                    process = com.fiuu.xdkandroid.telemetry.model.TelemetryProcess.ONLINE_PAYMENT_OUTCOME,
                    source = com.fiuu.xdkandroid.telemetry.model.TelemetryOrigin.EXTERNAL_HOST_BANK,
                    referenceNumber = response.referenceNo ?: ""
                )
            } else {
                com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.logHostDecline(
                    process = com.fiuu.xdkandroid.telemetry.model.TelemetryProcess.ONLINE_PAYMENT_OUTCOME,
                    statusCode = response.status,
                    statusMessage = response.message,
                    source = com.fiuu.xdkandroid.telemetry.model.TelemetryOrigin.EXTERNAL_HOST_BANK,
                    referenceNumber = response.referenceNo ?: ""
                )
            }
            dispatch_async(dispatch_get_main_queue()) {
                onResult(response)
                dismissViewControllerAnimated(true) {
                    cleanup()
                    onClose()
                }
            }
        }
    }

    fun finishWithClose() {
        val receipt = pendingReceiptResponse
        if (isShowingReceipt && receipt != null) {
            finishWithResult(receipt)
            return
        }
        if (!hasDispatchedResult) {
            hasDispatchedResult = true
            com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.recordBreadcrumb(
                area = "WEBCORE_IOS",
                process = "finishWithClose",
                message = "User closed webview"
            )
            dispatch_async(dispatch_get_main_queue()) {
                val cancelDto = WebcoreResponseMapper.mapJsonToDto(null)
                onResult(cancelDto)
                dismissViewControllerAnimated(true) {
                    cleanup()
                    onClose()
                }
            }
        }
    }

    private fun cleanup() {
        closeAllPopupWindows()
        mainWebView?.navigationDelegate = null
        mainWebView?.UIDelegate = null
        mainWebView = null
        navDelegate = null
        closeHandler = null
        pendingReceiptResponse = null
    }
}

@OptIn(ExperimentalForeignApi::class)
fun createIosWebcoreViewController(
    params: Map<String, Any?>,
    onResult: (PaymentResponseDto) -> Unit,
    onClose: () -> Unit
): UIViewController {
    val coreEnv = params["mp_core_env"]?.toString() ?: "2"
    val targetUrl = when (coreEnv) {
        "2" -> "https://xdk.fiuu.com/"
        "3" -> "https://uat-xdk.fiuu.com/"
        "4" -> "https://sandbox-xdk.fiuu.com/"
        else -> "https://xdk.fiuu.com/"
    }

    val isShowCloseButton = when (val btnDisplay = params["mp_closebutton_display"]) {
        is Boolean -> btnDisplay
        is String -> !btnDisplay.equals("false", ignoreCase = true)
        else -> true
    }

    FiuuLogger.log("IosWebcore: Initializing Webcore UI with targetUrl=$targetUrl (coreEnv=$coreEnv)")

    // Generate fallback device info JSON if not provided as valid JSON
    val defaultDeviceInfoJson = buildJsonObject {
        put("platform", "iOS")
        put("os", UIDevice.currentDevice.systemVersion)
        put("brand", "Apple")
        put("model", UIDevice.currentDevice.model)
        put("modelNo", UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "iPhone")
        val sdkMajor = UIDevice.currentDevice.systemVersion.split(".").firstOrNull()?.toIntOrNull() ?: 0
        put("sdkVersion", sdkMajor)
        put("carrierName", "--")
    }.toString()

    val rawDeviceInfo = params["device_info"]?.toString()?.trim()
    val sanitizedDeviceInfo = if (rawDeviceInfo != null && rawDeviceInfo.startsWith("{") && rawDeviceInfo.endsWith("}")) {
        rawDeviceInfo
    } else {
        defaultDeviceInfoJson
    }

    // Build clean JSON payload for Webcore JS bridge with strict boolean type coercion
    // Exclude internal wrapper parameters (mp_core_env, mp_closebutton_display)
    val paymentParamsJson = buildJsonObject {
        val booleanKeys = setOf(
            "mp_sandbox_mode", "mp_dev_mode", "mp_express_mode", "mp_editing_enabled",
            "mp_bill_name_edit_disabled", "mp_bill_email_edit_disabled",
            "mp_bill_mobile_edit_disabled", "mp_bill_description_edit_disabled", "is_submodule"
        )
        val excludedKeys = setOf("mp_core_env", "mp_closebutton_display", "device_info")

        params.forEach { (key, value) ->
            if (value != null && key !in excludedKeys) {
                val strVal = value.toString().trim()
                when {
                    key in booleanKeys -> {
                        when {
                            value is Boolean -> put(key, value)
                            strVal.equals("true", ignoreCase = true) -> put(key, true)
                            strVal.equals("false", ignoreCase = true) -> put(key, false)
                            else -> put(key, false)
                        }
                    }
                    value is Boolean -> put(key, value)
                    value is Number -> put(key, value)
                    else -> put(key, strVal)
                }
            }
        }
        put("device_info", sanitizedDeviceInfo)
        put("module_id", "molpay-mobile-xdk-ios")
        put("wrapper_version", "1.0.10")
    }.toString()

    val rootVC = FiuuWebcoreViewController(
        targetUrl = targetUrl,
        paymentParamsJson = paymentParamsJson,
        isShowCloseButton = isShowCloseButton,
        rawParams = params,
        onResult = onResult,
        onClose = onClose
    )

    val navigationController = UINavigationController(rootViewController = rootVC)
    navigationController.modalPresentationStyle = UIModalPresentationFullScreen
    return navigationController
}
