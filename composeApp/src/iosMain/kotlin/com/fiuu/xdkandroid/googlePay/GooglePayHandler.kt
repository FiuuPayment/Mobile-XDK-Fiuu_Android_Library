package com.fiuu.xdkandroid.googlePay

import com.fiuu.xdkandroid.platform.WebViewBridge

actual class GooglePayHandler actual constructor(@Suppress("unused") activity: Any) {
    actual fun isReadyToPay(buildMode: Int, callback: (Boolean) -> Unit) {
        callback(false)
    }

    actual fun loadPaymentData(
        amountMinorUnits: Long,
        currencyCode: String,
        merchantId: String?,
        buildMode: Int,
        onResult: (GooglePayPaymentResult) -> Unit,
    ) {
        onResult(GooglePayPaymentResult.Error("Google Pay is Android-only in this SDK build."))
    }

    actual fun defaultWebViewBridge(): WebViewBridge? = null

    internal actual fun showExpressProcessing() = Unit

    internal actual fun hideExpressProcessing() = Unit
}
