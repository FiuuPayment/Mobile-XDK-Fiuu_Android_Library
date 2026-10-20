package com.fiuu.xdkandroid.samsungPay

/**
 * Platform Samsung Pay helpers. Pass the platform root UI object (Android: [androidx.activity.ComponentActivity]).
 * Samsung Pay is Android-only; the iOS actual is a no-op stub.
 */
expect class SamsungPayHandler(activity: Any) {
    /** Whether Samsung Pay is available and ready on this device. */
    fun isReadyToPay(callback: (Boolean) -> Unit)

    /** Presents the Samsung Pay payment sheet. */
    fun loadPaymentData(
        amountMinorUnits: Long,
        currencyCode: String,
        onResult: (SamsungPayPaymentResult) -> Unit,
    )

    /**
     * Blocks the host screen with a loading overlay from the sheet's credential until the express
     * result is ready — see presentSamsungPayExpressButton. No-op on iOS.
     */
    internal fun showExpressProcessing()

    internal fun hideExpressProcessing()
}
