package com.fiuu.xdkandroid.data.repository

import com.fiuu.xdkandroid.data.remote.ApiService
import com.fiuu.xdkandroid.data.remote.dto.ApiException
import com.fiuu.xdkandroid.data.remote.dto.CardTokenContainerDto
import com.fiuu.xdkandroid.data.remote.dto.CardTokensRequestDto
import com.fiuu.xdkandroid.data.remote.dto.GetAvailableChannelResponseDto
import com.fiuu.xdkandroid.data.remote.dto.InstalmentTenureRequestDto
import com.fiuu.xdkandroid.data.remote.dto.InstalmentTenureResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentExpressRequestDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentExpressResponseDto
import com.fiuu.xdkandroid.data.remote.PaymentProcessException
import com.fiuu.xdkandroid.data.remote.dto.PaymentCreateResponse
import com.fiuu.xdkandroid.data.remote.dto.PaymentInquireRequestDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentInquireResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentProcessRequestDto
import com.fiuu.xdkandroid.domain.PaymentRepository
import com.fiuu.xdkandroid.domain.entity.PaymentRequest
import com.fiuu.xdkandroid.domain.entity.PaymentInitResponse
import com.fiuu.xdkandroid.domain.entity.toExpressDto
import com.fiuu.xdkandroid.domain.entity.toInitDto
import com.fiuu.xdkandroid.utils.FiuuLogger
import kotlinx.coroutines.CancellationException

class FiuuPaymentRepository(
    private val api: ApiService
) : PaymentRepository {

    override suspend fun initPayment(
        request: PaymentRequest,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): PaymentInitResponse? {
        val response = api.initPayment(
            request = request.toInitDto(),
            nonce = nonce,
            idempotencyKey = idempotencyKey,
            timestamp = timestamp,
            signature = signature
        )
        // The raw response (accessToken, card tokens) is already logged, redacted, by
        // KtorApiService — log only a non-sensitive summary here rather than this domain object's
        // default toString(), which would include the accessToken in plain text.
        FiuuLogger.log("Payment INIT RESPONSE: accessToken=${if (response?.accessToken != null) "<present>" else "<absent>"}, channels=${response?.channels?.size ?: 0}, error=${response?.error}")
        return response
    }

    override suspend fun createPayment(
        token: String,
        request: PaymentProcessRequestDto,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): PaymentCreateResponse? {
        try {
            val response = api.createPayment(
                token = token,
                request = request,
                nonce = nonce,
                idempotencyKey = idempotencyKey,
                timestamp = timestamp,
                signature = signature
            )
            // PaymentCreateResponse doesn't carry an accessToken, but its request/requestData can
            // still be gateway-specific — the raw body is already logged, redacted, by
            // KtorApiService.createPayment. Keep this to a status-only summary.
            FiuuLogger.log("Payment CREATE RESPONSE: status=${response?.status}, requestType=${response?.requestType}")

            return response
        } catch (e: CancellationException) {
            throw e
        } catch (e: PaymentProcessException) {
            throw e
        } catch (e: ApiException) {
            // Carries the real code/message the server returned (e.g. "P10" for an invalid card)
            // — must propagate to PaymentViewModel's `catch (e: ApiException)` so the actual
            // reason surfaces instead of a generic NetworkError.
            throw e
        } catch (e: Exception) {
            FiuuLogger.log("ERROR: $e")
            return null
        }
    }

    override suspend fun removeCardToken(accessToken: String, cardToken: String): Boolean {
        return try {
            api.removeCardToken(accessToken = accessToken, cardToken = cardToken)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FiuuLogger.log("TOKEN REMOVE ERROR: $e")
            false
        }
    }

    override suspend fun fetchCardTokens(
        accessToken: String,
        request: CardTokensRequestDto,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): CardTokenContainerDto? {
        return try {
            api.fetchCardTokens(
                accessToken = accessToken,
                request = request,
                nonce = nonce,
                idempotencyKey = idempotencyKey,
                timestamp = timestamp,
                signature = signature
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FiuuLogger.log("CARD TOKENS FETCH ERROR: $e")
            null
        }
    }

    override suspend fun listenToStream(token: String): PaymentResponseDto? {
        return try {
            api.listenToStream(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FiuuLogger.log("STREAM ERROR: $e")
            null
        }
    }

    override suspend fun expressPayment(
        request: PaymentRequest,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): PaymentExpressResponseDto? {
        val response = api.expressPayment(
            request = request.toExpressDto(),
            nonce = nonce,
            idempotencyKey = idempotencyKey,
            timestamp = timestamp,
            signature = signature
        )
        // Same reasoning as initPayment above — the raw body is already logged, redacted, by
        // KtorApiService.expressPayment; avoid this domain object's default toString() here.
        FiuuLogger.log("Payment Express RESPONSE: accessToken=${if (response?.accessToken != null) "<present>" else "<absent>"}, status=${response?.status}")
        return response
    }

    override suspend fun inquirePayment(
        token: String,
        request: PaymentInquireRequestDto,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): PaymentInquireResponseDto? {
        val response = api.inquirePayment(
            token = token,
            request = request,
            nonce = nonce,
            idempotencyKey = idempotencyKey,
            timestamp = timestamp,
            signature = signature
        )
        // Same reasoning as initPayment/createPayment/expressPayment above — log only a
        // non-sensitive summary, never this DTO's default toString().
        FiuuLogger.log("Payment Inquire RESPONSE: status=${response?.status}, tranID=${response?.transactionId}")
        return response
    }

    override suspend fun getAvailableChannelById(
        merchantId: String,
        currency: String,
        checksum: String,
        username: String,
        password: String
    ): GetAvailableChannelResponseDto? {
        return try {
            val response = api.getAvailableChannelById(
                merchantId = merchantId,
                currency = currency,
                checksum = checksum,
                username = username,
                password = password
            )
            FiuuLogger.log("Get Available Channel RESPONSE: $response")
            response
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FiuuLogger.log("GET AVAILABLE CHANNEL ERROR: $e")
            null
        }
    }

    override suspend fun fetchInstalmentTenure(
        token: String,
        request: InstalmentTenureRequestDto,
        nonce: String,
        timestamp: String,
        signature: String
    ): InstalmentTenureResponseDto? {
        return try {
            api.fetchInstalmentTenure(
                token = token,
                request = request,
                nonce = nonce,
                timestamp = timestamp,
                signature = signature
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FiuuLogger.log("INSTALMENT TENURE ERROR: $e")
            null
        }
    }

    override fun close() {
        api.close()
    }
}
