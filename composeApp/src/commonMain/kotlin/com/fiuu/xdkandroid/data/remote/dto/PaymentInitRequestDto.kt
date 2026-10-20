package com.fiuu.xdkandroid.data.remote.dto

import com.fiuu.xdkandroid.domain.entity.PaymentRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class PaymentInitRequestDto(
    @SerialName("mp_merchant_ID") val merchantId: String?,
    @SerialName("mp_amount") val amount: String?,
    @SerialName("mp_app_name") val appName: String?,
    @SerialName("mp_order_ID") val orderId: String?,
    @SerialName("mp_currency") val currency: String?,
    @SerialName("mp_country") val country: String?,
    @SerialName("mp_bill_name") val billName: String?,
    @SerialName("mp_bill_email") val billEmail: String?,
    @SerialName("mp_bill_mobile") val billMobile: String?,
    @SerialName("mp_bill_description") val billDescription: String?,
    @SerialName("mp_checksum") val checksum: String?
)

@Serializable
data class PaymentProcessRequestDto(
    @SerialName("xp_channel") val channel: String?,
    @SerialName("xp_channel_type") val channelType: String?,
    @SerialName("xp_checksum") val checksum: String?,
    @SerialName("xp_bill_name") val billName: String? = null,
    @SerialName("xp_bill_email") val billEmail: String? = null,
    @SerialName("xp_bill_mobile") val billMobile: String? = null,
    @SerialName("xp_bill_description") val billDescription: String? = null,
    @SerialName("xp_googlepay") val googlePay: JsonObject? = null,
    @SerialName("xp_click2pay") val click2Pay: JsonObject? = null,
    @SerialName("xp_samsungpay") val samsungPay: JsonObject? = null,
    @SerialName("xp_applepay") val applePayToken: JsonElement? = null,
    @SerialName("xp_card_holder_name") val cardHolderName: String? = null,
    @SerialName("xp_credit_card_no") val creditCardNo: String? = null,
    @SerialName("xp_credit_card_cvv") val creditCardCvv: String? = null,
    @SerialName("xp_credit_card_expiry") val creditCardExpiry: String? = null,
    @SerialName("xp_token_status") val tokenStatus: String? = null,
    @SerialName("xp_installmonth") val installmentMonth: String? = null,
    // Set only when paying with a previously saved card token (xp_token_status = "2") — see
    // PaymentRequest.putChannel's savedCardToken branch.
    @SerialName("xp_credit_card_token") val creditCardToken: String? = null
)

@Serializable
data class TokenRemoveRequestDto(
    @SerialName("token") val token: String
)

// POST card-tokens body — same xp_ billing-field shape as PaymentProcessRequestDto's billing
// subset (no channel fields), checksummed the same way (see PaymentRequest.toCardTokensApiPayload).
@Serializable
data class CardTokensRequestDto(
    @SerialName("xp_checksum") val checksum: String?,
    @SerialName("xp_bill_name") val billName: String?,
    @SerialName("xp_bill_email") val billEmail: String?,
    @SerialName("xp_bill_mobile") val billMobile: String?,
    @SerialName("xp_bill_description") val billDescription: String?
)

@Serializable
data class PaymentExpressRequestDto(
    @SerialName("mp_merchant_ID") val merchantId: String?,
    @SerialName("mp_amount") val amount: String?,
    @SerialName("mp_app_name") val appName: String?,
    @SerialName("mp_order_ID") val orderId: String?,
    @SerialName("mp_currency") val currency: String?,
    @SerialName("mp_channel") val channel: String?,
    @SerialName("mp_channel_type") val channelType: String?,
    @SerialName("mp_country") val country: String?,
    @SerialName("mp_bill_name") val billName: String?,
    @SerialName("mp_bill_email") val billEmail: String?,
    @SerialName("mp_bill_mobile") val billMobile: String?,
    @SerialName("mp_bill_description") val billDescription: String?,
    @SerialName("mp_checksum") val checksum: String?,
    @SerialName("xp_googlepay") val googlePay: JsonObject? = null,
    // Only populated when PaymentRequest.googlePayTokenAsBase64 is enabled — a base64-encoded
    // copy of googlePay sent alongside it under this separate key, not a replacement (see
    // PaymentRequest.putChannel).
    @SerialName("xp_googlepay64") val googlePay64: String? = null,
    @SerialName("xp_samsungpay") val samsungPay: JsonObject? = null,
    @SerialName("xp_applepay") val applePayToken: JsonElement? = null
)



