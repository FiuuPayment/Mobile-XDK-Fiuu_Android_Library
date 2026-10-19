package com.fiuu.xdkandroid.data.remote.dto

import com.fiuu.xdkandroid.domain.entity.PaymentChannel
import kotlinx.serialization.Serializable

// GetAvailableChannelById's channel objects already match PaymentChannel's field names/SerialNames
// exactly (checked against a real response), so the domain entity is decoded
// straight from the wire rather than through an intermediate DTO.
@Serializable
data class GetAvailableChannelResponseDto(
    val status: Boolean = false,
    val result: List<PaymentChannel> = emptyList()
)
