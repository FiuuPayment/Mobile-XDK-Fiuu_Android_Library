package com.fiuu.xdkandroid.applepay

actual class ApplePayHandler actual constructor() {

    actual fun isAvailable(): Boolean = false

    actual fun requestPayment(
        merchantId: String,
        amount: String,
        currency: String,
        countryCode: String,
        onResult: (ApplePayResult) -> Unit
    ) {
        onResult(ApplePayResult.Error("Apple Pay is not available on Android"))
    }
}
