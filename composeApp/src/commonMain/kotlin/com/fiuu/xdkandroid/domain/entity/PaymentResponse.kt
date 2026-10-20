package com.fiuu.xdkandroid.domain.entity

data class PaymentInitResponse(
    val accessToken: String? = null,
    val tokenExpiresAt: Long? = null,
    val channels: List<PaymentChannelInit> = emptyList(),
    val message: String? = null,
    val error: String? = null,
    val statusCode: String? = null
)

data class PaymentChannelInit(
    val name: String,
    val title: String,
    val maskName: String? = null,
    val channelType: String? = null,
    val isInstallment: Boolean = false,
    val logoUrl3: String? = null
)

data class CardTokenInit(
    val token: String,
    val bin: String? = null,
    val bin4: String? = null,
    val expdate: String? = null,
    val issuerBank: String? = null,
    val bankCountry: String? = null,
    val debitCredit: String? = null,
    val brand: String? = null
)
