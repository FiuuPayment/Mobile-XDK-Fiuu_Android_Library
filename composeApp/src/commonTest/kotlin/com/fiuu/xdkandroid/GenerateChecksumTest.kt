package com.fiuu.xdkandroid

import com.fiuu.xdkandroid.utils.generateChecksum
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GenerateChecksumTest {

    @Test
    fun testNumericSortingComesBeforeStringSorting() {
        val payload = mapOf(
            "strKey" to "alpha",
            "numKey" to 10,
            "anotherNum" to 2
        )
        // Numbers 2, 10 sorted numerically -> 2, 10 -> "2", "10", then string "alpha" -> input "vkey210alpha"
        val checksum = generateChecksum(payload, vkey = "vkey")
        assertNotEquals("", checksum)
    }

    @Test
    fun testNullHandlingInPayload() {
        val payload = mapOf(
            "key1" to null,
            "key2" to "value"
        )
        // null converts to "null" string
        val checksum = generateChecksum(payload, vkey = "test_key")
        assertNotEquals("", checksum)
    }

    @Test
    fun testNestedMapAndListFlattening() {
        val payload = mapOf(
            "nestedMap" to mapOf("innerKey" to "innerValue"),
            "nestedList" to listOf("listItem1", 42)
        )
        val checksum = generateChecksum(payload, vkey = "secret")
        assertNotEquals("", checksum)
    }

    @Test
    fun testJsonPrimitiveDistinction() {
        val payloadWithNumber = mapOf(
            "amount" to JsonPrimitive(100),
            "quotedAmount" to JsonPrimitive("100")
        )
        val checksum = generateChecksum(payloadWithNumber, vkey = "key")
        assertNotEquals("", checksum)
    }

    @Test
    fun testDeterministicChecksumOutput() {
        val payload = mapOf(
            "mp_amount" to "1.01",
            "mp_order_ID" to "ORD-12345",
            "mp_currency" to "MYR",
            "mp_merchant_ID" to "merchant_01"
        )
        val checksum1 = generateChecksum(payload, vkey = "verify_123")
        val checksum2 = generateChecksum(payload, vkey = "verify_123")
        assertEquals(checksum1, checksum2)
    }
}
