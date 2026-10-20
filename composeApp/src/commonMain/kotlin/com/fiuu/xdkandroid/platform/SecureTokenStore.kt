package com.fiuu.xdkandroid.platform

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement

// accessToken is the same bearer token used for /process and /inquire (not read-only), so it
// must never cross into host app code — it's persisted here, encrypted, and only ever read back
// out by the SDK itself so a killed/backgrounded process can resume a status check.
@Serializable
data class StoredPaymentSession(
    val orderId: String,
    val accessToken: String,
    val verificationKey: String,
    val amount: String?,
    val expiresAtEpochSeconds: Long,
    // Which backend (ApiConfig.baseUrl) the original /initiate or /express call used — a resumed
    // /inquire must hit the same host, so this rides along with the session rather than being
    // re-derived (the host app's current mp_sandbox_mode value may differ by the time resume runs).
    val buildMode: Int = 4,
    // Gateway transaction ID from /process or /express, once known — lets a resumed /inquire
    // look up by xp_tran_id instead of falling back to order ID.
    val transactionId: String? = null,
    // OTC/7-11 payment (PaymentRequest.isOtcPayment) — a resumed /inquire must send
    // xp_include_pending=true, same as the live session did.
    val includePending: Boolean = false
)

// Single-slot store: only the most recently persisted session is kept, so a new /initiate or
// /express overwrites whatever was there before. Resuming always means "the last payment
// attempted on this device" — there's no per-orderId lookup, matching the public
// resumeFiuuPaymentStatus() entry points (no orderId parameter).
interface SecureTokenStore {
    suspend fun save(session: StoredPaymentSession)
    suspend fun get(): StoredPaymentSession?
    suspend fun clear()

    // Drops the stored session if its TTL has already passed as of [nowEpochSeconds]. This is how
    // a session that was persisted but then abandoned (interrupted, never resumed, never reached a
    // terminal state) eventually gets swept: there's no background job, so this only runs
    // opportunistically when a new /initiate or /express call starts (see
    // PaymentViewModel.onStartInitiate).
    suspend fun clearExpired(nowEpochSeconds: Long)
}

private val storedSessionJson = Json { ignoreUnknownKeys = true }

// A pre-buildMode schema persisted a `sandboxMode: Boolean` field where `buildMode: Int` now
// lives. A plain strict decode throws on that unknown key; simply ignoring unknown keys would
// decode it silently but always fall back to buildMode's default (4/sandbox), which would resume
// a legacy *production* session against the sandbox host instead. Read the legacy field out first
// and map it across so both platform SecureTokenStore implementations share one migration path.
fun decodeStoredPaymentSession(raw: String): StoredPaymentSession? {
    return try {
        val obj = storedSessionJson.parseToJsonElement(raw) as? JsonObject ?: return null
        val session = storedSessionJson.decodeFromJsonElement<StoredPaymentSession>(obj)
        if (obj.containsKey("buildMode")) return session
        val legacySandboxMode = (obj["sandboxMode"] as? JsonPrimitive)?.booleanOrNull
        if (legacySandboxMode != null) {
            session.copy(buildMode = if (legacySandboxMode) 4 else 2)
        } else {
            session
        }
    } catch (e: Exception) {
        null
    }
}
