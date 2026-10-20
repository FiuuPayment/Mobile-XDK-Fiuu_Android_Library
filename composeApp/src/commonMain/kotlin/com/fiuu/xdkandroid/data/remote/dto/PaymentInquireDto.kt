package com.fiuu.xdkandroid.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// xp_tran_id is intentionally omitted (not sent as null) when looking up by order ID only —
// encodeDefaults defaults to false in KtorApiService's Json config, so a null here is left out
// of the wire body entirely, matching the by-order-ID Postman sample. includePending has no
// default for the same reason: a defaulted false would be dropped from the body while still
// being counted in the checksum. Build instances via inquireRequest() so the two stay in sync.
@Serializable
data class PaymentInquireRequestDto(
    @SerialName("xp_tran_id") val transactionId: String? = null,
    @SerialName("xp_order_id") val orderId: String?,
    @SerialName("xp_amount") val amount: String?,
    @SerialName("xp_include_pending") val includePending: Boolean,
    @SerialName("xp_checksum") val checksum: String?
)

// Checksum payload for /inquire — looks up by xp_tran_id when known (from /process, /express or
// the stream), otherwise by order ID with pending results included. xp_tran_id is left out of
// the map entirely rather than mapped to null, since a present-but-null value still contributes
// to the hash. alwaysIncludePending forces xp_include_pending=true even with a transaction ID —
// set for OTC/7-11 cash payments (PaymentRequest.isOtcPayment), whose unpaid vouchers are
// pending by design.
fun inquireChecksumPayload(
    transactionId: String?,
    orderId: String?,
    amount: String?,
    alwaysIncludePending: Boolean = false
): Map<String, Any?> =
    buildMap {
        if (transactionId != null) put("xp_tran_id", transactionId)
        put("xp_order_id", orderId)
        put("xp_amount", amount)
        put("xp_include_pending", includePending(transactionId, alwaysIncludePending))
    }

fun inquireRequest(
    transactionId: String?,
    orderId: String?,
    amount: String?,
    checksum: String,
    alwaysIncludePending: Boolean = false
): PaymentInquireRequestDto =
    PaymentInquireRequestDto(
        transactionId = transactionId,
        orderId = orderId,
        amount = amount,
        includePending = includePending(transactionId, alwaysIncludePending),
        checksum = checksum
    )

private fun includePending(transactionId: String?, alwaysIncludePending: Boolean): Boolean =
    alwaysIncludePending || transactionId == null

// /inquire responds in snake_case (transaction_id, pay_date, app_code, error_description).
// transaction_id arrives as a bare number, hence LenientStringSerializer on the ID fields.
@Serializable
data class PaymentInquireResponseDto(
    @SerialName("status") val status: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("referenceNo") val referenceNo: String? = null,
    @SerialName("transaction_id")
    @Serializable(with = LenientStringSerializer::class) val transactionId: String? = null,
    @SerialName("amount") val amount: String? = null,
    @SerialName("currency") val currency: String? = null,
    @SerialName("channel") val channel: String? = null,
    @SerialName("skey") val skey: String? = null,
    @SerialName("pay_date")
    @Serializable(with = LenientStringSerializer::class) val paydate: String? = null,
    @SerialName("app_code")
    @Serializable(with = LenientStringSerializer::class) val appcode: String? = null,
    @SerialName("error_code")
    @Serializable(with = LenientStringSerializer::class) val errorCode: String? = null,
    @SerialName("error_description")
    @Serializable(with = LenientStringSerializer::class) val errorDesc: String? = null
) {
    // Gateway status "22" is pending, not an outcome — /inquire returns it (even with
    // xp_include_pending: false) while e.g. the user is still mid-way through a TNG approval.
    // Treating it as terminal ended the session early with a pending result.
    val isTerminal: Boolean get() = status != null && status != STATUS_PENDING

    companion object {
        const val STATUS_PENDING = "22"
    }
}

// The gateway result page's `k` query value (MOLPay/result.php?k=…&extname=…), or null when it's
// missing or empty. Only its presence matters — it's an opaque server-side reference, never
// decoded or trusted as a result (the result always comes from /inquire).
fun resultPageKey(url: String): String? =
    url.substringAfter('?', missingDelimiterValue = "")
        .substringBefore('#')
        .split('&')
        .firstOrNull { it.startsWith("k=") }
        ?.removePrefix("k=")
        ?.takeIf { it.isNotBlank() }

// /inquire polling schedule — PLUGIN payments once their result page has loaded, and wallet
// hand-offs (TNG etc.) that come back still pending: immediately,
// then +1s, +2s, then every 3s — 30s in total, 12 lookups. The short ramp-up covers the likely
// race where the first lookup still sees "22" (pending); the 3s cap matches the old XDK's
// q_by_tid.php polling interval.
val INQUIRE_POLL_DELAYS_MS: List<Long> = listOf(0L, 1_000L, 2_000L) + List(9) { 3_000L }

// Whether a result handed to the host ends tracking of the stored session. "22" (pending) and the
// SDK's own "408" session timeout leave the attempt unresolved, so the session is kept for
// resumeFiuuPaymentStatus — the same rule resumePaymentStatus applies (clear only when terminal).
fun shouldClearStoredSession(status: String?): Boolean =
    !status.isNullOrBlank() && status != PaymentInquireResponseDto.STATUS_PENDING && status != "408"
