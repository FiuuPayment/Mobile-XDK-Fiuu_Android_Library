package com.fiuu.xdkandroid.platform

interface WebViewBridge {
    /**
     * Opens a URL in a WebView.
     * @param url The payment URL to load.
     * @param onSuccess Called on every completed page load inside the WebView (checkout page,
     *   bank redirect, OTP page, etc.) — NOT a payment-result signal. Terminal payment outcome is
     *   determined separately via the `/subscribe` SSE stream and confirmed with `/inquire`.
     * @param onFailure Called when a page load fails (network/SSL/HTTP error).
     * @param onClose Called when the WebView is closed by user.
     * @param onResultPage Called when the platform detects the gateway's final result page
     *   (Android: MOLPay/result.php) with its URL — once with loaded=false when navigation to it
     *   starts, and again with loaded=true when it finishes loading. The page is left to load —
     *   it triggers the stream's result event — and the caller becomes responsible for closing
     *   the WebView. Null falls back to closing immediately via onClose. iOS doesn't detect the
     *   result page, so never calls it.
     */

    fun initialize(merchantId: String, sessionId: String)
    fun startCheckOut(onComplete: (String) -> Unit)

    fun openGetUrl(
        url: String,
        onSuccess: () -> Unit,
        onFailure: (String?) -> Unit,
        onClose: () -> Unit,
        urlType: String? = null,
        headers: Map<String, String>? = null,
        // Whether the WebView shows its own close affordance (Android: a top bar; iOS: the
        // navigation Cancel button) — mirrors mp_closebutton_display's meaning on the legacy
        // Webcore engine (see ProvideFiuuPaymentSDK.kt), applied here to the redirect/OTC WebView
        // channels (e.g. FPX/online banking) use, same as PaymentRequest.closeButtonDisplay
        // already gates the Click to Pay widget's close button.
        closeButtonDisplay: Boolean = true,
        onResultPage: ((url: String, loaded: Boolean) -> Unit)? = null,
        onAppPaused: (() -> Unit)? = null,
        onAppResume: (() -> Unit)? = null
    )

    // For gateways (e.g. FPX) that require their own form fields submitted as a real POST body,
    // not just a navigation — see PaymentCreateResponse.requestData. No headers parameter: Android's
    // WebView has no API to attach custom headers to a postUrl() request, so this only carries
    // postData. If a channel ever needs both a real POST body and an auth header simultaneously,
    // this will need revisiting.
    fun openPostUrl(
        url: String,
        postData: Map<String, String>,
        onSuccess: () -> Unit,
        onFailure: (String?) -> Unit,
        onClose: () -> Unit,
        urlType: String? = null,
        closeButtonDisplay: Boolean = true,
        onResultPage: ((url: String, loaded: Boolean) -> Unit)? = null,
        onAppPaused: (() -> Unit)? = null,
        onAppResume: (() -> Unit)? = null
    )

    fun close()
}