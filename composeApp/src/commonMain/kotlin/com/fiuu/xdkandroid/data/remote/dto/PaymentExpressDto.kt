package com.fiuu.xdkandroid.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject



@Serializable
data class PaymentExpressResponseDto(
    @SerialName("accessToken") val accessToken: String? = null,
    @SerialName("tokenExpiresAt") val tokenExpiresAt: Long? = null,
    @SerialName("amount") val amount: String = "",
    @SerialName("currency") val currency: String? = null,
    @SerialName("transactionId") val transactionId: String? = null,
    @SerialName("referenceNo") val referenceNo: String? = null,
    @SerialName("requestType") val requestType: String? = null,
    @SerialName("requestMethod") val requestMethod: String? = null,
    @SerialName("requestURL") val requestURL: String? = null,
    @SerialName("urlType") val urlType: String? = null,
    // Gateway-specific form fields (e.g. FPX's fpx_* fields) that must be submitted as the POST
    // body when requestMethod == "POST" and urlType == "url" — see PaymentCreateResponse.
    @SerialName("requestData") val requestData: JsonElement? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("skey") val skey: String? = null,
    @SerialName("tranID") val tranID: String? = null,
    @SerialName("paydate") val paydate: String? = null,
    @SerialName("appcode") val appcode: String? = null,
    @SerialName("channel") val channel: String? = null,
    @SerialName("error_code") val errorCode: String? = null,
    @SerialName("error_desc") val errorDesc: String? = null,
)
