package com.fiuu.xdkandroid.googlePay

/** Result of presenting the Google Pay sheet (Android); iOS is currently unsupported. */
sealed interface GooglePayPaymentResult {
    data class Success(val paymentDataJson: String) : GooglePayPaymentResult
    data object Cancelled : GooglePayPaymentResult
    data class Error(val message: String) : GooglePayPaymentResult
}
