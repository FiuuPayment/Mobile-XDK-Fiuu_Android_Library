package com.fiuu.xdkandroid.domain.entity

data class PaymentInitRequest(
    val amount: String? = null,
    val username: String? = null,
    val password: String? = null,
    val merchantId: String? = null,
    val appName: String? = null,
    val orderId: String? = null,
    val currency: String? = null,
    val country: String? = null,
    val verificationKey: String? = null,
    val channel: String? = null,
    val billDescription: String? = null,
    val billName: String? = null,
    val billEmail: String? = null,
    val billMobile: String? = null,
    val expressMode: Boolean = false,
    val sandboxMode: Boolean = true,
    val devMode: Boolean = false,
    val deviceInfo: String? = null,
    val checksum: String? = null,
    val apMerchantId: String? = null,
    val buildMode: Int? = null
)