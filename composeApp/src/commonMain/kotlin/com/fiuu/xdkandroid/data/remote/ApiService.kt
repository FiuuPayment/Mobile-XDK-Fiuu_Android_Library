package com.fiuu.xdkandroid.data.remote

import com.fiuu.xdkandroid.data.remote.dto.CardTokenContainerDto
import com.fiuu.xdkandroid.data.remote.dto.CardTokensRequestDto
import com.fiuu.xdkandroid.data.remote.dto.GetAvailableChannelResponseDto
import com.fiuu.xdkandroid.data.remote.dto.InstalmentTenureRequestDto
import com.fiuu.xdkandroid.data.remote.dto.InstalmentTenureResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentExpressRequestDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentExpressResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentCreateResponse
import com.fiuu.xdkandroid.data.remote.dto.PaymentInitRequestDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentInquireRequestDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentInquireResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentProcessRequestDto
import com.fiuu.xdkandroid.domain.entity.PaymentInitResponse

interface ApiService {
    suspend fun removeCardToken(accessToken: String, cardToken: String)

    suspend fun fetchCardTokens(
        accessToken: String,
        request: CardTokensRequestDto,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): CardTokenContainerDto

    suspend fun initPayment(
        request: PaymentInitRequestDto,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): PaymentInitResponse

    suspend fun createPayment(
        token: String,
        request: PaymentProcessRequestDto,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): PaymentCreateResponse

    suspend fun listenToStream(token: String): PaymentResponseDto?

    suspend fun expressPayment(
        request: PaymentExpressRequestDto,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): PaymentExpressResponseDto

    suspend fun inquirePayment(
        token: String,
        request: PaymentInquireRequestDto,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): PaymentInquireResponseDto

    suspend fun getAvailableChannelById(
        merchantId: String,
        currency: String,
        checksum: String,
        username: String,
        password: String
    ): GetAvailableChannelResponseDto

    suspend fun fetchInstalmentTenure(
        token: String,
        request: InstalmentTenureRequestDto,
        nonce: String,
        timestamp: String,
        signature: String
    ): InstalmentTenureResponseDto

    fun close()
}
