package com.fiuu.xdkandroid.samsungPay

/** Result of presenting the Samsung Pay sheet (Android); iOS is currently unsupported. */
sealed interface SamsungPayPaymentResult {
    data class Success(val paymentCredentialJson: String) : SamsungPayPaymentResult
    data object Cancelled : SamsungPayPaymentResult
    data class Error(val message: String) : SamsungPayPaymentResult
}
