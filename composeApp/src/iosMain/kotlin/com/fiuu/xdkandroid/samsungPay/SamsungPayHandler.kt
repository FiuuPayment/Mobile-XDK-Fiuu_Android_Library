package com.fiuu.xdkandroid.samsungPay

actual class SamsungPayHandler actual constructor(@Suppress("unused") activity: Any) {
    actual fun isReadyToPay(callback: (Boolean) -> Unit) {
        callback(false)
    }

    actual fun loadPaymentData(
        amountMinorUnits: Long,
        currencyCode: String,
        onResult: (SamsungPayPaymentResult) -> Unit,
    ) {
        onResult(SamsungPayPaymentResult.Error("Samsung Pay is Android-only in this SDK build."))
    }

    internal actual fun showExpressProcessing() = Unit

    internal actual fun hideExpressProcessing() = Unit
}
