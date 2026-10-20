package com.fiuu.xdkandroid.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class PaymentInquireResponseDtoTest {

    // Same config as KtorApiService's json instance.
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun testDecodeSnakeCaseResponseWithNumericTransactionId() {
        val rawJson = """
            {"transaction_id":4062656819,"merchant_id":"test_merchant","status":"22","amount":"1.01","currency":"MYR","pay_date":null,"order_id":"ORD-83669895","app_code":"","error_code":"","error_description":"","channel":"TNG-EWALLET"}
        """.trimIndent()

        val result = json.decodeFromString<PaymentInquireResponseDto>(rawJson)

        assertEquals("4062656819", result.transactionId)
        assertEquals("22", result.status)
        assertEquals("1.01", result.amount)
        assertEquals("MYR", result.currency)
        assertEquals("TNG-EWALLET", result.channel)
        assertNull(result.paydate)
        assertNull(result.appcode)
        assertNull(result.errorCode)
        assertNull(result.errorDesc)
    }

    @Test
    fun testRequestByTransactionIdSendsSameFieldsAsChecksum() {
        val payload = inquireChecksumPayload("4064143937", "ORD-43597483", "1.01")
        val body = json.parseToJsonElement(
            json.encodeToString(PaymentInquireRequestDto.serializer(), inquireRequest("4064143937", "ORD-43597483", "1.01", "cs"))
        ).jsonObject

        assertEquals(false, payload["xp_include_pending"])
        assertEquals("4064143937", body["xp_tran_id"]?.jsonPrimitive?.content)
        // Must be on the wire even though it's false — it's part of the checksum.
        assertEquals("false", body["xp_include_pending"]?.jsonPrimitive?.content)
        assertEquals(payload.keys + "xp_checksum", body.keys)
    }

    @Test
    fun testRequestByOrderIdOmitsTransactionId() {
        val payload = inquireChecksumPayload(null, "ORD-43597483", "1.01")
        val body = json.parseToJsonElement(
            json.encodeToString(PaymentInquireRequestDto.serializer(), inquireRequest(null, "ORD-43597483", "1.01", "cs"))
        ).jsonObject

        assertFalse("xp_tran_id" in payload)
        assertFalse("xp_tran_id" in body)
        assertEquals("true", body["xp_include_pending"]?.jsonPrimitive?.content)
        assertEquals(payload.keys + "xp_checksum", body.keys)
    }

    @Test
    fun testOtcRequestByTransactionIdIncludesPending() {
        val payload = inquireChecksumPayload("4064143937", "ORD-43597483", "1.01", alwaysIncludePending = true)
        val body = json.parseToJsonElement(
            json.encodeToString(
                PaymentInquireRequestDto.serializer(),
                inquireRequest("4064143937", "ORD-43597483", "1.01", "cs", alwaysIncludePending = true)
            )
        ).jsonObject

        assertEquals(true, payload["xp_include_pending"])
        assertEquals("4064143937", body["xp_tran_id"]?.jsonPrimitive?.content)
        assertEquals("true", body["xp_include_pending"]?.jsonPrimitive?.content)
        assertEquals(payload.keys + "xp_checksum", body.keys)
    }

    @Serializable
    private data class PlainStringId(@SerialName("transaction_id") val transactionId: String? = null)

    // Why LenientStringSerializer exists: without it, this Json config rejects a bare number.
    @Test
    fun testPlainStringFieldRejectsNumericTransactionId() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<PlainStringId>("""{"transaction_id":4062656819}""")
        }
    }
}
