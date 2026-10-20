package com.fiuu.xdkandroid.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// uat and VCode are used only to compute Signature — they are NOT sent in the request body
@Serializable
data class PaymentGPayPluginRequestDto(
    val xp_channel: String,
    val xp_channel_type: String,
    val xp_checksum: String,
    val xp_googlepay: JsonObject
)

@Serializable
data class PaymentGPayPluginResponseDto(
    @SerialName("amount") val amount: String? = null,
    @SerialName("currency") val currency: String? = null,
    @SerialName("transactionId") val transactionId: String? = null,
    @SerialName("referenceNo") val referenceNo: String? = null,
    @SerialName("requestType") val requestType: String? = null,
    @SerialName("requestMethod") val requestMethod: String? = null,
    @SerialName("requestURL") val requestURL: String? = null,
    @SerialName("urlType") val urlType: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("statusCode") val statusCode: Int? = null
)
