package com.fiuu.xdkandroid.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentChannel (
    val id: String? = null,
    val name: String,
    val title: String,
    val default: Int? = null,
    val status: Int,

    @SerialName("channel_type")
    val channelType: String? = null,

    val currency: List<String> = emptyList(),
    val displayHPP: String? = null,

    @SerialName("filename")
    val fileName: String? = null,

    @SerialName("maskname")
    val maskName: String? = null,
    val publicMaskName: String? = null,
    val position: Int? = null,

    @SerialName("tbl_extname")
    val tableExtension: String? = null,

    @SerialName("is_installment")
    val isInstallment: Boolean = false,

    @SerialName("logo_url")
    val logoUrl: String? = null,

    @SerialName("logo_url2")
    val logoUrl2: String? = null,

    @SerialName("logo_url3")
    val logoUrl3: String? = null
)