package com.fiuu.xdkandroid.utils

import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray
import korlibs.crypto.HMAC
import korlibs.crypto.MD5
import korlibs.crypto.SHA1
import korlibs.crypto.SHA256
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object CryptoUtils {
    @OptIn(ExperimentalUuidApi::class)
    fun generateUuid(): String {
        return Uuid.random().toString()
    }

    /**
     * Generates a nonce similar to the Postman script: timestamp-random
     * Example: 1774429708000-a1b2c3d4e5
     */
    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    fun generateNonce(): String {
        val timestamp = Clock.System.now().toEpochMilliseconds().toString()
        val charPool : List<Char> = ('a'..'z') + ('0'..'9')
        // Uuid.random() draws from the platform's cryptographically secure RNG (SecureRandom /
        // the OS CSPRNG); kotlin.random.Random is predictable, which defeats a replay nonce.
        // Bytes 6 and 8 carry the UUID version/variant bits, so only the fully random ones are used.
        val randomBytes = Uuid.random().toByteArray()
        val randomString = listOf(0, 1, 2, 3, 4, 5, 7, 9, 10, 11)
            .map { i -> charPool[(randomBytes[i].toInt() and 0xFF) % charPool.size] }
            .joinToString("")

        return "$timestamp-$randomString"
    }

    /**
     * Generates a signature by joining method, path, timestamp, nonce, and checksum with newlines
     * and hashing the result with SHA256.
     */
    fun generateSignature(
        method: String,
        path: String,
        timestamp: String,
        nonce: String,
        checksum: String
    ): String {
        val rawString = listOf(
            method,
            path,
            timestamp,
            nonce,
            checksum
        ).joinToString("\n")

        // The raw string (timestamp/nonce/checksum/method/path) and the signature it produces are
        // exactly the material needed to replay or forge a request — never write either to a log,
        // even under mp_dev_mode. A completion marker is enough to confirm this step ran.
        val signature = SHA256.digest(rawString.toByteArray(Charsets.UTF_8)).hex.lowercase()
        FiuuLogger.log("Signature generated for $method $path")
        return signature
    }

    /**
     * Checksum for the legacy GetAvailableChannelById endpoint (mirrors the Postman pre-request
     * script): MD5(verificationKey) -> SHA1(merchantId + appName + md5VKeyHex) as a "password
     * hash" -> SHA1(merchantId + currency + passwordHash) as the final checksum. Unrelated to the
     * SHA256 flatten-and-sort checksum used by /initiate, /process, /inquire (see
     * utils/GenerateChecksum.kt) — this endpoint predates that scheme.
     */
    fun generateChannelChecksum(
        merchantId: String,
        appName: String,
        currency: String,
        verificationKey: String
    ): String {
        val md5VKey = MD5.digest(verificationKey.toByteArray(Charsets.UTF_8)).hex.lowercase()
        val passwordHash = SHA1.digest(
            "$merchantId$appName$md5VKey".toByteArray(Charsets.UTF_8)
        ).hex.lowercase()
        val checksumString = "$merchantId$currency$passwordHash"
        return SHA1.digest(checksumString.toByteArray(Charsets.UTF_8)).hex.lowercase()
    }

    /**
     * HMAC-SHA256 over `message`, keyed with `key`, hex-encoded. Mirrors the instalment-tenure
     * endpoint's pre-request script (`crypto.subtle.sign('HMAC', ...)`) — distinct from
     * [generateSignature]'s SHA256(method/path/timestamp/nonce/checksum), which still separately
     * signs the request as x-signature. This is a body-level field the instalment-tenure endpoint
     * expects in addition to that header.
     */
    fun hmacSha256Hex(key: String, message: String): String {
        return HMAC.hmacSHA256(
            key.toByteArray(Charsets.UTF_8),
            message.toByteArray(Charsets.UTF_8)
        ).hexLower
    }
}
