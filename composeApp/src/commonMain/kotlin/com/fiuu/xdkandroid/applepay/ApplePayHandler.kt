package com.fiuu.xdkandroid.applepay

expect class ApplePayHandler() {
    fun isAvailable(): Boolean

    fun requestPayment(
        merchantId: String,
        amount: String,
        currency: String,
        countryCode: String,
        onResult: (ApplePayResult) -> Unit
    )
}
