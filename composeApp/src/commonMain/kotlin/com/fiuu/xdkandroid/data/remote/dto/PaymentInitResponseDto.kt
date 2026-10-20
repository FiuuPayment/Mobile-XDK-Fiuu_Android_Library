package com.fiuu.xdkandroid.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentInitResponseDto(
    @SerialName("accessToken")
    val accessToken: String? = null,
    @SerialName("tokenExpiresAt")
    val tokenExpiresAt: Long? = null,
    @SerialName("channels")
    val channels: List<PaymentChannelDto> = emptyList()
    // NOTE: /initiate's own "cardToken" field is intentionally left unparsed here — the saved-card
    // list is now sourced solely from POST card-tokens (see PaymentViewModel.fetchCardTokens).
    // CardTokenContainerDto/CardTokenDto below are that endpoint's response shape.
)

@Serializable
data class CardTokenContainerDto(
    @SerialName("removable")
    val removable: Int? = null,
    @SerialName("tokens")
    val tokens: List<CardTokenDto> = emptyList()
)

@Serializable
data class PaymentChannelDto(
    @SerialName("name")
    val name: String,
    @SerialName("title")
    val title: String,
    @SerialName("maskName")
    val maskName: String? = null,
    @SerialName("channelType")
    val channelType: String? = null,
    @SerialName("isInstallment")
    val isInstallment: Boolean = false,
    @SerialName("logoUrl3")
    val logoUrl3: String? = null
)

@Serializable
data class CardTokenDto(
    @SerialName("token")
    val token: String,
    @SerialName("bin")
    val bin: String? = null,
    @SerialName("bin4")
    val bin4: String? = null,
    @SerialName("expdate")
    val expdate: String? = null,
    @SerialName("issuer_bank")
    val issuerBank: String? = null,
    @SerialName("bank_country")
    val bankCountry: String? = null,
    @SerialName("debit_credit")
    val debitCredit: String? = null,
    @SerialName("brand")
    val brand: String? = null
)
