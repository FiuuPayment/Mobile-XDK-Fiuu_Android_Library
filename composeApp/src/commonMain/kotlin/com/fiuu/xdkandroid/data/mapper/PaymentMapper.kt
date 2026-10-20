package com.fiuu.xdkandroid.data.mapper

import com.fiuu.xdkandroid.data.remote.dto.CardTokenDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentChannelDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentInitResponseDto
import com.fiuu.xdkandroid.domain.entity.CardTokenInit
import com.fiuu.xdkandroid.domain.entity.PaymentChannelInit
import com.fiuu.xdkandroid.domain.entity.PaymentInitResponse

fun PaymentInitResponseDto.toDomain(): PaymentInitResponse {
    return PaymentInitResponse(
        accessToken = accessToken,
        tokenExpiresAt = tokenExpiresAt,
        channels = channels.map { it.toDomain() }
    )
}

fun PaymentChannelDto.toDomain(): PaymentChannelInit {
    return PaymentChannelInit(
        name = name,
        title = title,
        maskName = maskName,
        channelType = channelType,
        isInstallment = isInstallment,
        logoUrl3 = logoUrl3
    )
}

fun CardTokenDto.toDomain(): CardTokenInit {
    return CardTokenInit(
        token = token,
        bin = bin,
        bin4 = bin4,
        expdate = expdate,
        issuerBank = issuerBank,
        bankCountry = bankCountry,
        debitCredit = debitCredit,
        brand = brand
    )
}
