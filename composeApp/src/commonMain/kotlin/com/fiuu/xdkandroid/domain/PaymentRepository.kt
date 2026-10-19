package com.fiuu.xdkandroid.domain

import com.fiuu.xdkandroid.data.remote.dto.CardTokenContainerDto
import com.fiuu.xdkandroid.data.remote.dto.CardTokensRequestDto
import com.fiuu.xdkandroid.data.remote.dto.GetAvailableChannelResponseDto
import com.fiuu.xdkandroid.data.remote.dto.InstalmentTenureRequestDto
import com.fiuu.xdkandroid.data.remote.dto.InstalmentTenureResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentExpressRequestDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentExpressResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentCreateResponse
import com.fiuu.xdkandroid.data.remote.dto.PaymentInquireRequestDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentInquireResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentProcessRequestDto
import com.fiuu.xdkandroid.domain.entity.PaymentRequest
import com.fiuu.xdkandroid.domain.entity.PaymentInitResponse

interface PaymentRepository {
    // Returns true on success, false if the call failed (network or API error) — the caller
    // decides how to surface that rather than this layer throwing into the ViewModel.
    suspend fun removeCardToken(accessToken: String, cardToken: String): Boolean

    // Returns null if the call failed (network/server error) — same "swallow and return null"
    // contract as getAvailableChannelById; the saved-card list is a supplementary fetch, not part
    // of the init->process->stream checkout flow, so a failure here just leaves no saved cards
    // shown rather than blocking checkout.
    suspend fun fetchCardTokens(
        accessToken: String,
        request: CardTokensRequestDto,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): CardTokenContainerDto?

    suspend fun initPayment(
        request: PaymentRequest,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): PaymentInitResponse?

    suspend fun createPayment(
        token: String,
        request: PaymentProcessRequestDto,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): PaymentCreateResponse?

    suspend fun listenToStream(token: String): PaymentResponseDto?

    suspend fun expressPayment(
        request: PaymentRequest,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): PaymentExpressResponseDto?

    suspend fun inquirePayment(
        token: String,
        request: PaymentInquireRequestDto,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): PaymentInquireResponseDto?

    // Returns null if the call failed (network/server error) — same "swallow and return null"
    // contract as the other repository methods, since a failed channel lookup here is a
    // non-fatal supplementary fetch, not part of the init->process->stream checkout flow.
    suspend fun getAvailableChannelById(
        merchantId: String,
        currency: String,
        checksum: String,
        username: String,
        password: String
    ): GetAvailableChannelResponseDto?

    // Returns null if the call failed (network/server error) — same "swallow and return null"
    // contract as getAvailableChannelById above; a failed tenure lookup just means the Instalment
    // Period section keeps showing its no-plans-available state.
    suspend fun fetchInstalmentTenure(
        token: String,
        request: InstalmentTenureRequestDto,
        nonce: String,
        timestamp: String,
        signature: String
    ): InstalmentTenureResponseDto?

    fun close()
}
