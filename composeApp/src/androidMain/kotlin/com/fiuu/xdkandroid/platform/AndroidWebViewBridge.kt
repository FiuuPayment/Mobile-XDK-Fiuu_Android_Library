package com.fiuu.xdkandroid.platform

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.graphics.Bitmap
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.fiuu.xdkandroid.utils.FiuuLogger
import java.net.URLEncoder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// Gateway redirect/deep-link URLs (bank pages, 3DS challenge pages, MOLPay result pages) can carry
// a transient session reference or token in their query string — log only scheme/host/path so
// navigation can still be traced without writing that query string to Logcat. The fragment goes
// too: an intent:// URI carries its extras there (e.g. a browser_fallback_url with its own query).
private fun String.withoutQuery(): String = substringBefore("?").substringBefore("#")

// The gateway's final result page — see handleTerminalRedirect/reportResultPageLoaded.
private const val RESULT_PAGE_MARKER = "MOLPay/result.php?"

private const val CLOSE_DISABLED_WHILE_LOADING_MESSAGE = "Please wait, this can't be closed while loading"

private const val APP_NOT_INSTALLED_MESSAGE = "The app needed for this payment isn't installed"

class AndroidWebViewBridge(private val activity: ComponentActivity) : WebViewBridge {

    private var webView: WebView? = null
    // The overlay's actual root when a close bar is showing (webView becomes its child rather
    // than android.R.id.content's direct child) — tracked separately so destroyWebView() can tear
    // down the whole overlay, not just the WebView.
    private var overlayContainer: ViewGroup? = null
    // The close bar's own "CLOSE" label (see createCloseBar) — kept so revealWebView() can
    // restore it once the page actually finishes loading, mirroring PaymentScaffold's dimmed-
    // while-loading Close button on the Compose side. null whenever closeButtonDisplay is off.
    private var closeBarText: TextView? = null
    // True from the moment addOverlayWebView adds the (still-invisible) overlay until the page
    // reaches any terminal state (loaded, error, SSL error, HTTP error) — i.e. exactly the window
    // webView stays View.INVISIBLE. Gates the close bar's tap handler so it can't tear down an
    // in-flight WebView the same way PaymentScaffold's Close is gated by uiState.loading.
    private var pageStillLoading: Boolean = false
    private var backPressedCallback: OnBackPressedCallback? = null
    // Host of the page the WebView is showing or navigating to (initial load, followed redirects,
    // committed pages) — lets handleSslError() tell the payment page's own certificate failing
    // apart from a sub-resource's. Reset by destroyWebView().
    private var mainFrameHost: String? = null
    private var merchantId: String = ""
    private var sessionId: String = ""

    override fun initialize(merchantId: String, sessionId: String) {
        this.merchantId = merchantId
        this.sessionId = sessionId
        // Initial setup can happen here if needed before the WebView is created
    }

    override fun startCheckOut(onComplete: (String) -> Unit) {}

    // Gateway pages (e.g. e-wallet OTP/approval steps) redirect to custom URI schemes
    // (`boostappdeeplink://...`, `intent://...`) to hand off to a native app rather than
    // continuing in-browser. WebView has no protocol handler for these — left unintercepted,
    // Chromium fails the navigation with net::ERR_UNKNOWN_URL_SCHEME, which onReceivedError
    // then reports as a fatal payment error. Hand them to the OS via an explicit Intent instead,
    // and fall back (see handleMissingApp) when the target app isn't installed — the underlying
    // checkout page keeps running either way.
    //
    // `intent://` URIs must go through Intent.parseUri: as a plain ACTION_VIEW data URI nothing
    // resolves the "intent" scheme (confirmed on device: "unable to resolve Intent"), so the
    // hand-off silently did nothing. A gateway page controls the parsed intent's fields, so it's
    // restricted to what a browser would allow: BROWSABLE, no explicit component or selector.
    private fun handleNonHttpScheme(url: String): Boolean {
        val scheme = url.substringBefore("://", missingDelimiterValue = "").lowercase()
        if (scheme.isEmpty() || scheme == "http" || scheme == "https") return false
        if (FiuuLogger.enabled) Log.d("FIUU_WEBVIEW", "Non-http(s) scheme, handing off to external app: ${url.withoutQuery()}")
        val intent = if (scheme == "intent") {
            try {
                Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    component = null
                    selector = null
                }
            } catch (e: Exception) {
                Log.w("FIUU_WEBVIEW", "Malformed intent:// URI: ${url.withoutQuery()}")
                return true
            }
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        }
        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            handleMissingApp(intent)
        }
        return true
    }

    // Target app not installed: prefer the page's own browser_fallback_url (an intent:// extra),
    // then the app's Play Store listing, and otherwise tell the user — previously this was
    // swallowed, leaving them on a page that had already tried to hand off.
    private fun handleMissingApp(intent: Intent) {
        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
            ?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
        if (fallbackUrl != null) {
            if (FiuuLogger.enabled) Log.d("FIUU_WEBVIEW", "App not installed, loading browser_fallback_url: ${fallbackUrl.withoutQuery()}")
            webView?.loadUrl(fallbackUrl)
            return
        }
        val packageName = intent.`package`
        if (packageName != null) {
            Log.w("FIUU_WEBVIEW", "App not installed, opening Play Store listing: $packageName")
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
                return
            } catch (e: ActivityNotFoundException) {
                // No Play Store on this device — fall through to the message.
            }
        }
        Log.w("FIUU_WEBVIEW", "No app found to handle deep link: ${intent.data?.toString()?.withoutQuery()}")
        Toast.makeText(activity, APP_NOT_INSTALLED_MESSAGE, Toast.LENGTH_LONG).show()
    }

    // A handful of e-wallet gateway domains return a page a WebView *could* technically render
    // (valid http/https, so handleNonHttpScheme above lets them through), but they're actually
    // universal-link landing pages meant to hand off to the native wallet app for approval —
    // rendering them in-WebView instead leaves that approval step stuck. Ported from the legacy
    // native XDK's molPay WebViewClient, which force-launches these via Intent.ACTION_VIEW
    // regardless of scheme.
    //
    // packageName (optional) is passed to Intent.setPackage() to pin resolution to that app rather
    // than relying on its App Links verification. None is set today: TNG's cashier link
    // (/s/cashier/...) opened TNG directly on both test devices (SUNMI V3 / Android 13, Pixel 8
    // Pro), including one with TNG's "Open supported links" turned off (2026-10-02). Pinning
    // my.com.tngdigital.ewallet would make the hand-off fail outright if TNG ever moved the
    // cashier to a path its app doesn't claim, instead of falling back to the browser.
    //
    // Matched on the parsed host and path, not a substring of the URL — a gateway page carrying
    // "myboost.app" in a query parameter must not trigger a hand-off.
    private data class EwalletUniversalLink(
        val host: String,
        val pathPrefix: String = "",
        val packageName: String? = null,
    ) {
        fun matches(uri: Uri): Boolean {
            val uriHost = uri.host?.lowercase() ?: return false
            return uri.scheme.equals("https", ignoreCase = true) &&
                (uriHost == host || uriHost.endsWith(".$host")) &&
                (uri.path ?: "").startsWith(pathPrefix)
        }
    }

    private val EWALLET_UNIVERSAL_LINKS = listOf(
        EwalletUniversalLink("atome-my.onelink.me"),
        EwalletUniversalLink("myboost.app"),
        EwalletUniversalLink("app.shopback.com", pathPrefix = "/pay"),
        EwalletUniversalLink("m.tngdigital.com.my", pathPrefix = "/s/"),
    )

    private fun handleEwalletUniversalLink(url: String): Boolean {
        val uri = Uri.parse(url)
        val link = EWALLET_UNIVERSAL_LINKS.firstOrNull { it.matches(uri) } ?: return false
        if (FiuuLogger.enabled) Log.d("FIUU_WEBVIEW", "E-wallet universal link detected, handing off to external app: ${url.withoutQuery()}")
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            link.packageName?.let { intent.setPackage(it) }
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w("FIUU_WEBVIEW", "No app found to handle e-wallet link: ${url.withoutQuery()}")
        }
        return true
    }

    // SCB Easy's app-link page is a plain https URL, so neither handler above catches it. Ported
    // from the legacy native XDK (PaymentActivity.java) and mirrors IosWebcoreViewController: hand
    // the link to the SCB Easy app, then swap the stuck app-link page for the gateway's
    // intermediate loading page, keyed by the page's own ref_no field.
    // KIV: SCB Easy is disabled for this publish and comes back in the next one. Uncomment this
    // function and its three call sites in the shouldOverrideUrlLoading overrides below.
    // private fun handleScbEasyLink(url: String, webView: WebView): Boolean {
    //     if (!url.contains("scbeasy/easy_app_link.html")) return false
    //     if (FiuuLogger.enabled) Log.d("FIUU_WEBVIEW", "SCB Easy app link detected, handing off to external app: ${url.withoutQuery()}")
    //     try {
    //         activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    //     } catch (e: ActivityNotFoundException) {
    //         Log.w("FIUU_WEBVIEW", "No app found to handle SCB Easy link: ${url.withoutQuery()}")
    //     }
    //     webView.evaluateJavascript("document.getElementById(\"ref_no\").value") { raw ->
    //         val refNo = raw?.replace("\"", "")?.takeIf { it.isNotEmpty() && it != "null" } ?: return@evaluateJavascript
    //         if (this.webView !== webView) return@evaluateJavascript
    //         webView.loadUrl("https://pay.merchant.razer.com/RMS/intermediate_app/loading.php?tranID=${URLEncoder.encode(refNo, "UTF-8")}")
    //     }
    //     return true
    // }

    // Recognizes the gateway's own terminal redirect URLs (result page, explicit failure/cancel
    // status query params) so the WebView can resolve immediately instead of sitting on a blank
    // "redirecting to merchant's site" stub until the /subscribe SSE stream (or the 60s timeout)
    // eventually catches up. Previously only wired into the inline-HTML ("data") flow — POST-based
    // channels like FPX/online banking (openPostUrl) and GET redirects (openGetUrl's non-data
    // branch) never checked for this, so a bank-side cancel there had no WebView-level signal at
    // all and relied solely on the stream/back-press/timeout.
    // Second half of onResultPage: the gateway's result page has finished loading. PLUGIN payments
    // (see PaymentViewModel.handleResultPageReached) treat the loaded result.php?k=… page as the
    // completion signal — for /express sessions it stays on "Redirecting…" and never reaches
    // /pay/callback, so no stream event would ever arrive.
    private fun reportResultPageLoaded(url: String?, onResultPage: ((String, Boolean) -> Unit)?) {
        if (url != null && url.contains(RESULT_PAGE_MARKER)) onResultPage?.invoke(url, true)
    }

    private fun handleTerminalRedirect(
        redirectUrl: String,
        webView: WebView,
        onFailure: (String?) -> Unit,
        onClose: () -> Unit,
        onResultPage: ((url: String, loaded: Boolean) -> Unit)?,
    ): Boolean = when {
        redirectUrl.contains(RESULT_PAGE_MARKER) -> {
            // Confirmed via device logs (FPX/Maybank2u cancel): reaching this URL is where the
            // WebView used to get stuck showing the gateway's "redirecting to merchant's site..."
            // stub forever. onSuccess() alone doesn't close anything — per openPayment()'s own
            // comment it's deliberately just a loading-flag flip that fires on every ordinary page
            // load. Route through the same destroy-then-onClose() sequence the back-press path
            // uses (AndroidWebViewBridge's OnBackPressedCallback) so the ViewModel confirms the
            // real outcome via /inquire instead of leaving the frozen page on screen. Deferred one
            // tick via post{} since we're still inside this WebView's own navigation-decision
            // callback — destroying it synchronously here would tear down the view while it's
            // mid-dispatch.
            if (onResultPage != null) {
                // result.php must be allowed to load: it redirects to the backend's
                // /pay/callback/<id>, which is what triggers the /subscribe result event
                // (confirmed via device logs — blocking it meant the stream never delivered).
                // The ViewModel closes the WebView once the stream delivers, or after a bounded
                // wait, so the "redirecting to merchant's site..." stub can't be left on screen.
                if (FiuuLogger.enabled) Log.d("FIUU_WEBVIEW", "Payment result URL detected, letting it load for the stream callback")
                onResultPage(redirectUrl, false)
                false
            } else {
                if (FiuuLogger.enabled) Log.d("FIUU_WEBVIEW", "Payment result URL detected, closing WebView")
                webView.post {
                    destroyWebView()
                    onClose()
                }
                true
            }
        }
        redirectUrl.contains("payment_status=11") || redirectUrl.contains("status=failed") -> {
            if (FiuuLogger.enabled) Log.d("FIUU_WEBVIEW", "Payment FAILURE detected: ${redirectUrl.withoutQuery()}")
            revealWebView(webView)
            onFailure(redirectUrl)
            onClose()
            true
        }
        else -> false
    }

    private fun trackMainFrame(url: String?) {
        val host = url?.let { Uri.parse(it).host } ?: return
        mainFrameHost = host
    }

    // onReceivedSslError, unlike onReceivedError/onReceivedHttpError, has no isForMainFrame — it
    // fires for sub-resources too. Failing the whole payment on those was wrong: a 3DS page's
    // ThreatMetrix script (h.online-metrix.net, chained to GlobalSign Root R46, which older
    // devices' trust stores lack) failed while 3DS itself carried on to result.php (confirmed on
    // device, 2026-10-01). The certificate is always rejected either way — never proceed() in a
    // payment flow — but only an error on the page's own host fails the payment. When either
    // host is unknown, it's treated as the page's own (the previous, fail-safe behaviour).
    private fun handleSslError(
        handler: SslErrorHandler?,
        error: SslError?,
        webView: WebView,
        onFailure: (String?) -> Unit,
    ) {
        handler?.cancel()
        val failedHost = error?.url?.let { Uri.parse(it).host }
        val pageHost = mainFrameHost
        if (failedHost != null && pageHost != null && !failedHost.equals(pageHost, ignoreCase = true)) {
            Log.w("FIUU_WEBVIEW", "SSL error on a sub-resource (page host $pageHost), blocked and ignored: $error")
            return
        }
        Log.e("FIUU_WEBVIEW", "SSL error: $error")
        revealWebView(webView)
        onFailure("SSL error loading payment page: ${error?.primaryError}")
    }

    // Some gateway/bank result pages (e.g. the generic "You are now being redirected to the
    // merchant's site..." stub) end the flow by calling JS window.close() instead of navigating
    // anywhere else — expecting to close a popup/tab, which is meaningless for an in-app WebView.
    // Without a WebChromeClient, WebView drops that call silently and the overlay is left stuck
    // on that page forever, since shouldOverrideUrlLoading/onPageFinished never fire again. Treat
    // it the same as the user pressing back: tear down our WebView and let the ViewModel resolve
    // the outcome via /inquire (see handleWebViewClosedByUser).
    private fun createChromeClient(onClose: () -> Unit): WebChromeClient = object : WebChromeClient() {
        override fun onCloseWindow(window: WebView?) {
            if (FiuuLogger.enabled) Log.d("FIUU_WEBVIEW", "onCloseWindow: JS window.close() received, closing WebView")
            destroyWebView()
            onClose()
        }
    }

    // Shared by every payment WebView (inline HTML, GET redirect, POST). Gateway and bank pages
    // never need the device's files or content providers, and the defaults for both depend on the
    // host's targetSdk, so they're switched off explicitly (MASVS-PLATFORM).
    @SuppressLint("SetJavaScriptEnabled")
    private fun configurePaymentSettings(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // Without these, WebView ignores the page's <meta name="viewport"> tag and renders
            // it at a fixed desktop-style width (~980px) before scaling down to fit the screen —
            // full-bleed/absolutely-positioned decorative elements then land far from where the
            // page's own mobile layout intends them.
            useWideViewPort = true
            loadWithOverviewMode = true
            allowFileAccess = false
            allowContentAccess = false
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    @SuppressLint("SetJavaScriptEnabled")
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
        // Without this, a WebView already attached from a prior openGetUrl call (e.g. a retry)
        // would leak: this.webView gets reassigned below and the stale one is never removed from
        // android.R.id.content or destroyed.
        destroyWebView()
        if (com.fiuu.xdkandroid.diagnostics.DiagnosticChaosEngine.activeWebViewLoadFail) {
            onFailure("Simulated WebView TLS / Connection Failure (Chaos Mode)")
            return
        }
        if (urlType == "data") {
            // The gateway returned inline base64-encoded HTML (urlType == "data") rather than
            // a redirect URL — render it directly instead of navigating anywhere.
            //
            // Decode to a real HTML string and use loadDataWithBaseURL rather than handing the raw
            // base64 to loadData(url, "text/html", "base64"): loadData's own base64 decoding path
            // is a long-known-unreliable legacy WebView API — confirmed on-device for a Maybank2u
            // FPX redirect stub (~55KB decoded HTML with an embedded New Relic bundle) where
            // loadData never fired onPageFinished *or* any error callback, leaving the WebView
            // permanently invisible and the loading overlay stuck forever. loadDataWithBaseURL
            // takes the HTML directly and reliably reaches onPageFinished/onReceivedError. Matches
            // IosWebViewBridge.openGetUrl, which also decodes and calls loadHTMLString. The base
            // URL is the form action's origin — see inlineHtmlBaseUrl.
            val html = try {
                Base64.decode(url).decodeToString()
            } catch (e: Exception) {
                onFailure("Failed to decode HTML payload: ${e.message}")
                return
            }
            val baseUrl = inlineHtmlBaseUrl(html)
            if (FiuuLogger.enabled) Log.d("FIUU_WEBVIEW", "openGetUrl: urlType=data, loading inline HTML, baseUrl=$baseUrl")
            val webView = WebView(activity)
            this.webView = webView
            configurePaymentSettings(webView)
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val redirectUrl = request?.url?.toString() ?: return false
                    if (FiuuLogger.enabled) Log.d("FIUU_WEBVIEW", "shouldOverrideUrlLoading: ${redirectUrl.withoutQuery()}")
                    if (handleNonHttpScheme(redirectUrl)) return true
                    // KIV SCB Easy: if (handleScbEasyLink(redirectUrl, webView)) return true
                    if (handleEwalletUniversalLink(redirectUrl)) return true
                    if (handleTerminalRedirect(redirectUrl, webView, onFailure, onClose, onResultPage)) return true
                    if (FiuuLogger.enabled) Log.d("FIUU_WEBVIEW", "Unhandled redirect, letting WebView navigate: ${redirectUrl.withoutQuery()}")
                    if (request?.isForMainFrame == true) trackMainFrame(redirectUrl)
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    trackMainFrame(url)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    if (FiuuLogger.enabled) Log.d("FIUU_WEBVIEW", "onPageFinished: ${url?.withoutQuery()}")
                    revealWebView(webView)
                    onSuccess()
                    reportResultPageLoaded(url, onResultPage)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame != true) return
                    val message = "WebView error ${error?.errorCode}: ${error?.description}"
                    Log.e("FIUU_WEBVIEW", message)
                    revealWebView(webView)
                    onFailure(message)
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    handleSslError(handler, error, webView, onFailure)
                }
            }
            webView.webChromeClient = createChromeClient(onClose)
            trackMainFrame(baseUrl)
            webView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
            addOverlayWebView(webView, closeButtonDisplay, onClose)
        } else {
            // Render the bank/channel redirect in an in-app WebView (overlaid on top of the
            // existing Compose content, not replacing it) instead of launching an external
            // Chrome Custom Tab — Custom Tabs is a separate Activity/Task that close() has no
            // reliable, non-destructive way to dismiss once the payment result stream completes.
            if (FiuuLogger.enabled) Log.d("FIUU_WEBVIEW", "openGetUrl: urlType=$urlType, loading redirect URL ${url.withoutQuery()}")
            val webView = WebView(activity)
            this.webView = webView
            configurePaymentSettings(webView)
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val redirectUrl = request?.url?.toString() ?: return false
                    if (handleNonHttpScheme(redirectUrl)) return true
                    // KIV SCB Easy: if (handleScbEasyLink(redirectUrl, webView)) return true
                    if (handleEwalletUniversalLink(redirectUrl)) return true
                    val handled = handleTerminalRedirect(redirectUrl, webView, onFailure, onClose, onResultPage)
                    if (!handled && request?.isForMainFrame == true) trackMainFrame(redirectUrl)
                    return handled
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    trackMainFrame(url)
                }

                override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                    if (FiuuLogger.enabled) Log.d("FIUU_WEBVIEW", "onPageFinished: ${finishedUrl?.withoutQuery()}")
                    revealWebView(webView)
                    onSuccess()
                    reportResultPageLoaded(finishedUrl, onResultPage)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame != true) return
                    val message = "WebView error ${error?.errorCode}: ${error?.description}"
                    Log.e("FIUU_WEBVIEW", message)
                    revealWebView(webView)
                    onFailure(message)
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    if (request?.isForMainFrame != true) return
                    val statusCode = errorResponse?.statusCode
                    if (statusCode == null || statusCode < 400) return
                    val message = "Payment page returned HTTP $statusCode"
                    Log.e("FIUU_WEBVIEW", message)
                    revealWebView(webView)
                    onFailure(message)
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    handleSslError(handler, error, webView, onFailure)
                }
            }
            webView.webChromeClient = createChromeClient(onClose)
            trackMainFrame(url)
            // shouldOverrideUrlLoading above only fires for navigations that happen *within* an
            // already-loading WebView (redirects, in-page links, JS navigation) — Android never
            // calls it for the very first loadUrl() that kicks a load off. So a channel whose
            // /process response hands back a universal-link/non-http URL directly as requestURL
            // (confirmed for TNG-EWALLET: requestURL was https://m.tngdigital.com.my/s/cashier/...,
            // one of EWALLET_UNIVERSAL_LINKS' own markers) would otherwise never be checked at
            // all — the WebView tries to render it as a real page instead of handing off to the
            // native app, and just hangs (no onPageFinished, no onReceivedError) until the SDK's
            // 60s /subscribe timeout eventually kills the whole attempt. Check it here first, same
            // as shouldOverrideUrlLoading does for every later redirect.
            if (!handleNonHttpScheme(url) && !handleEwalletUniversalLink(url)) {
                webView.loadUrl(url, headers ?: emptyMap())
            }
            addOverlayWebView(webView, closeButtonDisplay, onClose)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
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
        if (com.fiuu.xdkandroid.diagnostics.DiagnosticChaosEngine.activeWebViewLoadFail) {
            onFailure("Simulated WebView TLS / Connection Failure (Chaos Mode)")
            return
        }
        if (urlType == "data") {
            // Inline base64-encoded HTML payload — there's no URL to POST postData to, so
            // render it exactly like openGetUrl's "data" branch does.
            openGetUrl(url, onSuccess, onFailure, onClose, urlType, closeButtonDisplay = closeButtonDisplay, onResultPage = onResultPage)
            return
        }
        destroyWebView()
        val webView = WebView(activity)
        this.webView = webView

        configurePaymentSettings(webView)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val redirectUrl = request?.url?.toString() ?: return false
                if (handleNonHttpScheme(redirectUrl)) return true
                // KIV SCB Easy: if (handleScbEasyLink(redirectUrl, webView)) return true
                if (handleEwalletUniversalLink(redirectUrl)) return true
                val handled = handleTerminalRedirect(redirectUrl, webView, onFailure, onClose, onResultPage)
                if (!handled && request?.isForMainFrame == true) trackMainFrame(redirectUrl)
                return handled
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                trackMainFrame(url)
            }

            override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                if (FiuuLogger.enabled) Log.d("FIUU_WEBVIEW", "openPostUrl: onPageFinished: ${finishedUrl?.withoutQuery()}")
                revealWebView(webView)
                onSuccess()
                reportResultPageLoaded(finishedUrl, onResultPage)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame != true) return
                val message = "WebView error ${error?.errorCode}: ${error?.description}"
                Log.e("FIUU_WEBVIEW", message)
                revealWebView(webView)
                onFailure(message)
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                if (request?.isForMainFrame != true) return
                val statusCode = errorResponse?.statusCode
                if (statusCode == null || statusCode < 400) return
                val message = "Payment page returned HTTP $statusCode"
                Log.e("FIUU_WEBVIEW", message)
                revealWebView(webView)
                onFailure(message)
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                handleSslError(handler, error, webView, onFailure)
            }
        }
        webView.webChromeClient = createChromeClient(onClose)
        trackMainFrame(url)

        addOverlayWebView(webView, closeButtonDisplay, onClose)

        // Same initial-URL gap as openGetUrl above — shouldOverrideUrlLoading never fires for the
        // very first load, so a universal-link/non-http requestURL must be checked here too,
        // before ever handing it to postUrl().
        if (!handleNonHttpScheme(url) && !handleEwalletUniversalLink(url)) {
            val encodedPostData = postData.entries.joinToString("&") { (k, v) ->
                "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
            }.toByteArray(Charsets.UTF_8)
            webView.postUrl(url, encodedPostData)
        }
    }

    // Adds the WebView as an overlay on top of the Activity's existing content (the host app's
    // Compose UI) rather than activity.setContentView(webView), which would replace that content
    // entirely — destroyWebView()'s removeView() then simply reveals the original UI again,
    // no setContentView restoration needed. Starts invisible: the WebView is added after the
    // host's ComposeView in the same FrameLayout, so it would otherwise paint over — and hide —
    // the Compose loading overlay the instant it's attached, before the page has rendered
    // anything. Callers flip it back to VISIBLE once the page actually finishes loading (or
    // errors out). closeButtonDisplay adds a close bar above the WebView (same
    // mp_closebutton_display flag the legacy Webcore engine's own Toolbar already respects, see
    // ProvideFiuuPaymentSDK.kt) — without one, redirect/OTC channels (FPX/online banking, etc.)
    // had no visible close affordance at all, only the hardware/system back gesture below.
    private fun addOverlayWebView(webView: WebView, closeButtonDisplay: Boolean, onClose: () -> Unit) {
        webView.visibility = View.INVISIBLE
        // Cleared by revealWebView() once the page reaches any terminal state — see its own
        // comment and the close bar's onClick below.
        pageStillLoading = true

        // A plain FrameLayout root (webView alone) when there's no close bar to stack above it —
        // otherwise a vertical LinearLayout: [close bar, webView (fills the rest)]. Either way,
        // this is the node added to android.R.id.content and the one that receives the system-bar
        // margins below, so the close bar (when present) sits below the status bar too rather than
        // just the WebView doing so on its own.
        val root: ViewGroup
        if (closeButtonDisplay) {
            val container = LinearLayout(activity)
            container.orientation = LinearLayout.VERTICAL
            container.addView(createCloseBar(onClick = {
                // Same "disable while loading, explain on tap" treatment as PaymentScaffold's
                // Compose Close button — without this, tapping here mid-load tore down the
                // WebView (and the payment attempt with it) before the page, or even the /process
                // call, ever got a chance to resolve.
                if (pageStillLoading) {
                    if (FiuuLogger.enabled) Log.d("FIUU_WEBVIEW", "Close button tapped while loading, ignoring")
                    Toast.makeText(activity, CLOSE_DISABLED_WHILE_LOADING_MESSAGE, Toast.LENGTH_SHORT).show()
                } else {
                    if (FiuuLogger.enabled) Log.d("FIUU_WEBVIEW", "Close button tapped, closing WebView")
                    destroyWebView()
                    onClose()
                }
            }))
            webView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0
            ).apply { weight = 1f }
            container.addView(webView)
            root = container
        } else {
            closeBarText = null
            val container = FrameLayout(activity)
            webView.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            container.addView(webView)
            root = container
        }
        root.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        // The host app runs edge-to-edge (targetSdk 36), so android.R.id.content extends behind
        // the system bars on every edge, not just the bottom — a top status bar, a bottom
        // nav/gesture bar, and (in landscape, or on display cutout devices) left/right insets too.
        // Left unmargined, the page's own top content renders underneath the status bar (observed:
        // a gateway page's header compositing behind the clock/battery icons) the same way its
        // bottom content was observed rendering underneath the nav bar before this fix.
        // WebView's Chromium compositor sizes its rendered viewport off the View's actual
        // measured bounds, not its padding — setPadding() alone leaves the page drawing full
        // height underneath the padding (confirmed: insets fired with a real bottom=96 value but
        // the page still rendered under the nav bar). Margins force a genuine re-measure/re-layout
        // to a smaller size, which the compositor does respect.
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            if (FiuuLogger.enabled) Log.d("FIUU_WEBVIEW", "applying systemBars insets to overlay: $systemBars")
            val params = view.layoutParams as? FrameLayout.LayoutParams
            if (params != null) {
                // When the close bar is showing, its own background must extend up behind the
                // status bar instead of the whole overlay being pushed down by a top margin here
                // — see createCloseBar's own inset listener, which pads that top inset internally
                // so its blue fill still reaches the true screen edge (matching PaymentScaffold's
                // TopAppBar) rather than leaving an unpainted gap above it.
                params.topMargin = if (closeButtonDisplay) 0 else systemBars.top
                params.bottomMargin = systemBars.bottom
                params.leftMargin = systemBars.left
                params.rightMargin = systemBars.right
                view.layoutParams = params
            }
            insets
        }
        activity.findViewById<ViewGroup>(android.R.id.content).addView(root)
        ViewCompat.requestApplyInsets(root)
        overlayContainer = root

        // System back while the overlay is showing used to fall through to the Activity's
        // default back handling and close the whole app — the overlay sits outside any
        // back-stack (added straight to android.R.id.content), so nothing else ever intercepted
        // it. Re-registered per overlay so it's only enabled while one is actually showing.
        backPressedCallback?.remove()
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (FiuuLogger.enabled) Log.d("FIUU_WEBVIEW", "Back pressed, closing WebView")
                destroyWebView()
                onClose()
            }
        }
        activity.onBackPressedDispatcher.addCallback(activity, callback)
        backPressedCallback = callback
    }

    // Every onPageFinished/onReceivedError/onReceivedHttpError/onReceivedSslError branch across
    // openGetUrl/openPostUrl/handleTerminalRedirect funnels through here instead of setting
    // webView.visibility directly, so the close bar's dimmed/disabled state (see createCloseBar)
    // always clears in lockstep with the page actually becoming visible.
    private fun revealWebView(webView: WebView) {
        webView.visibility = View.VISIBLE
        pageStillLoading = false
        closeBarText?.alpha = 1f
    }

    // Mirrors PaymentScaffold's app-bar look (AppColors.Primary blue, same 64dp Material3
    // TopAppBar height, same labelLarge type — 14sp/Medium/0.1sp tracking, the style M3's
    // TextButton applies to its own label — and the same sentence-case "Close", since M3 buttons
    // don't auto-uppercase) for visual consistency with the rest of the SDK — this overlay is
    // plain Android Views rather than Compose (added directly to android.R.id.content, outside
    // the ComposeView), so it can't reuse PaymentScaffold itself.
    //
    // Two-layer structure, same trick PaymentInstructionScreen's own header uses: the OUTER
    // LinearLayout carries the blue background and WRAP_CONTENT height, so that background still
    // paints all the way to the true screen edge once the root container above stops margining
    // this bar down for the status bar. The INNER row is the fixed 64dp touch target (text +
    // click listener); the outer's own top padding — set by the inset listener below — pushes
    // just that inner row (not the background) beneath the status bar icons.
    private fun createCloseBar(onClick: () -> Unit): View {
        val density = activity.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val closeText = TextView(activity).apply {
            text = "CLOSE"
            // AppColors.Secondary — matches PaymentScaffold's "Close" TextButton color.
            setTextColor(0xFF44D62C.toInt())
            // labelLarge: 14sp / Medium (500) / 0.1sp letter-spacing — the text style M3's
            // TextButton applies by default, which is what PaymentScaffold's "Close" renders with.
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, 500, false)
            letterSpacing = 0.1f / textSize
            // Starts dimmed — addOverlayWebView sets pageStillLoading = true just before this bar
            // is created, and revealWebView() restores full alpha once the page is ready.
            alpha = if (pageStillLoading) 0.5f else 1f
            setOnClickListener { onClick() }
        }
        closeBarText = closeText
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            // 64dp — Material3's default TopAppBar height (TopAppBarDefaults.TopAppBarExpandedHeight),
            // matching PaymentScaffold's own app-bar size exactly.
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(64))
            setPadding(dp(16), 0, dp(16), 0)
            addView(closeText)
        }
        return LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF003BA0.toInt())
            addView(row)
        }.also { bar ->
            ViewCompat.setOnApplyWindowInsetsListener(bar) { view, insets ->
                val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                view.setPadding(view.paddingLeft, statusBarTop, view.paddingRight, view.paddingBottom)
                insets
            }
        }
    }

    override fun close() {
        // Just tear down our own WebView (if any was used, e.g. inline "data" HTML or POST
        // flows). Dismissal of the SDK overlay itself is the host's responsibility, driven by
        // FiuuPaymentSDK's paymentSuccess/paymentError LaunchedEffects calling onRequestClose() —
        // recreate()ing the Activity here would tear down that same Compose tree (and the
        // ViewModel/StateFlow it's observing) before those callbacks get a chance to fire.
        destroyWebView()
    }

    private fun destroyWebView() {
        backPressedCallback?.remove()
        backPressedCallback = null
        webView?.apply {
            stopLoading()
            webViewClient = WebViewClient()
            clearHistory()
            // Must detach from the window before destroy() — destroying a WebView that's still
            // attached (e.g. still a child of overlayContainer) is unsafe and can crash the
            // native Chromium engine.
            (parent as? ViewGroup)?.removeView(this)
            removeAllViews()
            destroy()
        }
        webView = null
        // The root added to android.R.id.content (see addOverlayWebView) — the WebView is only
        // ever one of its children (alongside the close bar, when shown), so removing the WebView
        // above doesn't tear down the close bar/root itself.
        overlayContainer?.let { (it.parent as? ViewGroup)?.removeView(it) }
        overlayContainer = null
        closeBarText = null
        pageStillLoading = false
        mainFrameHost = null
    }
}