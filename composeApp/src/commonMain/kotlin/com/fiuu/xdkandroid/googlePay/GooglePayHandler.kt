package com.fiuu.xdkandroid.googlePay

import com.fiuu.xdkandroid.platform.WebViewBridge

/**
 * Platform Google Pay helpers. Pass the platform root UI object (Android: [androidx.activity.ComponentActivity]).
 */
expect class GooglePayHandler(activity: Any) {
    /** Whether Google Pay can be offered on this device, in the given [buildMode] environment. */
    fun isReadyToPay(buildMode: Int, callback: (Boolean) -> Unit)

    /**
     * Presents the Google Pay payment sheet. [merchantId] overrides GooglePayConfig's default
     * gatewayMerchantId when supplied (see mp_gp_merchant_ID). [buildMode] selects
     * WalletConstants.ENVIRONMENT_TEST vs ENVIRONMENT_PRODUCTION (see mp_sandbox_mode).
     */
    fun loadPaymentData(
        amountMinorUnits: Long,
        currencyCode: String,
        merchantId: String?,
        buildMode: Int,
        onResult: (GooglePayPaymentResult) -> Unit,
    )

    /**
     * A WebViewBridge on this handler's own activity, for when /express answers with a REDIRECT
     * (e.g. a 3DS challenge) and the caller didn't supply one — see presentGooglePayExpressButton.
     * Null when the platform/activity can't host one.
     */
    fun defaultWebViewBridge(): WebViewBridge?

    /**
     * Blocks the host screen with a loading overlay from the sheet's token until the express
     * result is ready — see presentGooglePayExpressButton. No-op where Google Pay isn't supported.
     */
    internal fun showExpressProcessing()

    internal fun hideExpressProcessing()
}
