package com.fiuu.xdkandroid.domain

import com.fiuu.xdkandroid.data.remote.HttpClientFactory
import com.fiuu.xdkandroid.data.remote.KtorApiService
import com.fiuu.xdkandroid.data.remote.dto.inquireChecksumPayload
import com.fiuu.xdkandroid.data.remote.dto.inquireRequest
import com.fiuu.xdkandroid.data.remote.dto.PaymentResponseDto
import com.fiuu.xdkandroid.data.repository.FiuuPaymentRepository
import com.fiuu.xdkandroid.platform.SecureTokenStore
import com.fiuu.xdkandroid.utils.CryptoUtils
import com.fiuu.xdkandroid.utils.DateTimeUtils
import com.fiuu.xdkandroid.utils.FiuuLogger
import com.fiuu.xdkandroid.utils.generateChecksum
import kotlinx.coroutines.CancellationException

/**
 * Resumes a status check for the most recently attempted payment session (app killed/backgrounded
 * before the result stream delivered a terminal push). [store] only ever holds a single session —
 * whichever /initiate or /express succeeded last — so there's no orderId to pass in; the host app
 * never holds the accessToken itself, only whatever orderId it generated for that last attempt.
 */
internal suspend fun resumePaymentStatus(store: SecureTokenStore): PaymentResponseDto? {
    val session = store.get() ?: return null
    val orderId = session.orderId

    val now = DateTimeUtils.getCurrentUnixTimestampString().toLong()
    if (now >= session.expiresAtEpochSeconds) {
        store.clear()
        return PaymentResponseDto(
            status = "SESSION_EXPIRED",
            statusCheck = false,
            message = "Payment session expired",
            errorCode = "SESSION_EXPIRED",
            errorDesc = "Payment session expired"
        )
    }

    val repo = FiuuPaymentRepository(KtorApiService(HttpClientFactory(), session.buildMode))
    return try {
        val timestamp = DateTimeUtils.getCurrentUnixTimestampString()
        val nonce = CryptoUtils.generateNonce()
        val idempotencyKey = CryptoUtils.generateUuid()

        // Looks up by xp_tran_id when /process or /express had returned one before the session
        // was persisted, otherwise by order ID.
        val payload = inquireChecksumPayload(session.transactionId, orderId, session.amount, session.includePending)
        val checksum = generateChecksum(payload = payload, vkey = session.verificationKey)
        val signature = CryptoUtils.generateSignature(
            method = "POST",
            path = "/inquire",
            timestamp = timestamp,
            nonce = nonce,
            checksum = checksum
        )

        val inquireResult = repo.inquirePayment(
            token = session.accessToken,
            request = inquireRequest(session.transactionId, orderId, session.amount, checksum, session.includePending),
            nonce = nonce,
            idempotencyKey = idempotencyKey,
            timestamp = timestamp,
            signature = signature
        ) ?: return null

        val response = PaymentResponseDto(
            status = inquireResult.status ?: "",
            statusCheck = inquireResult.status == "00",
            message = inquireResult.message ?: inquireResult.errorDesc ?: "",
            referenceNo = inquireResult.referenceNo,
            transactionId = inquireResult.transactionId,
            amount = inquireResult.amount,
            currency = inquireResult.currency,
            channel = inquireResult.channel,
            skey = inquireResult.skey,
            tranID = inquireResult.transactionId,
            paydate = inquireResult.paydate,
            appcode = inquireResult.appcode,
            errorCode = inquireResult.errorCode,
            errorDesc = inquireResult.errorDesc
        )

        // Stop tracking once the gateway has reached any terminal outcome — successful or not —
        // so an expired/failed session doesn't linger in storage until its TTL clear.
        if (inquireResult.isTerminal) {
            store.clear()
        }

        response
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        FiuuLogger.log("resumePaymentStatus failed: $e")
        null
    } finally {
        repo.close()
    }
}
