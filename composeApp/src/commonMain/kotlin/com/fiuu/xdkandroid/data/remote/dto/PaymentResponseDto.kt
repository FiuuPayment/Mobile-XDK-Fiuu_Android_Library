package com.fiuu.xdkandroid.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class PaymentResponseDto(
    @SerialName("status")
    val status: String = "",
    @SerialName("statusCheck")
    val statusCheck: Boolean = false,
    @SerialName("message")
    val message: String = "",

    @SerialName("requestURL")
    val requestURL: String? = null,
    @SerialName("requestMethod")
    val requestMethod: String? = null,
    @SerialName("requestType")
    val requestType: String? = null,
    @SerialName("referenceNo")
    val referenceNo: String? = null,
    @SerialName("transactionId")
    val transactionId: String? = null,

    val amount: String? = null,
    val currency: String? = null,
    val channel: String? = null,
    val skey: String? = null,
    val tranID: String? = null,
    val paydate: String? = null,
    val appcode: String? = null,
    val errorCode: String? = null,
    val errorDesc: String? = null,
    val errorDetails: PaymentErrorDto? = null
)

@Serializable
data class PaymentErrorDto(
    val code: String = "",
    val message: String = "",
    val type: String = "" // "NETWORK", "TIMEOUT", "AUTH"
)
