package com.fiuu.xdkandroid.applepay

import kotlinx.serialization.json.JsonObject

sealed class ApplePayResult {
    data class Success(val token: JsonObject) : ApplePayResult()
    data class Error(val message: String) : ApplePayResult()
    object Cancelled : ApplePayResult()
}
