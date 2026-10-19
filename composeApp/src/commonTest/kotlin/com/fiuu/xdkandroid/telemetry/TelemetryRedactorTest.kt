package com.fiuu.xdkandroid.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TelemetryRedactorTest {

    @Test
    fun testPanMasking16Digits() {
        val input = "Customer card 4111222233334444 failed at gateway"
        val redacted = TelemetryRedactor.redact(input)
        
        assertFalse(redacted.contains("4111222233334444"), "Raw PAN must never appear")
        assertTrue(redacted.contains("411122******4444"), "PAN must be masked with 6-asterisk bin/last4 format")
    }

    @Test
    fun testPanMaskingWithDashesAndSpaces() {
        val inputDashes = "Card 5500-1122-3344-5566 was rejected"
        val redactedDashes = TelemetryRedactor.redact(inputDashes)
        assertFalse(redactedDashes.contains("5500-1122-3344-5566"))
        assertTrue(redactedDashes.contains("550011******5566"))

        val inputSpaces = "Card 4000 1234 5678 9010 was rejected"
        val redactedSpaces = TelemetryRedactor.redact(inputSpaces)
        assertFalse(redactedSpaces.contains("4000 1234 5678 9010"))
        assertTrue(redactedSpaces.contains("400012******9010"))
    }

    @Test
    fun testSensitiveKeysRedactedInJson() {
        val jsonInput = """
            {
                "mp_verification_key": "secret_vkey_xyz123",
                "mp_password": "supersecretpassword",
                "checksum": "a1b2c3d4e5f6",
                "xp_applepay": "pk_token_payload_987",
                "xp_credit_card_cvv": "999",
                "mp_amount": "100.50",
                "mp_order_ID": "ORD-998877"
            }
        """.trimIndent()

        val redacted = TelemetryRedactor.redact(jsonInput)

        assertFalse(redacted.contains("secret_vkey_xyz123"), "mp_verification_key must be redacted")
        assertFalse(redacted.contains("supersecretpassword"), "mp_password must be redacted")
        assertFalse(redacted.contains("a1b2c3d4e5f6"), "checksum must be redacted")
        assertFalse(redacted.contains("pk_token_payload_987"), "xp_applepay must be redacted")
        assertFalse(redacted.contains("999"), "CVV must be redacted")

        // Non-sensitive fields must remain intact
        assertTrue(redacted.contains("100.50"), "Amount must be preserved")
        assertTrue(redacted.contains("ORD-998877"), "Order ID must be preserved")
    }

    @Test
    fun testRedactMap() {
        val map = mapOf(
            "mp_verification_key" to "my_secret_key",
            "mp_password" to "my_secret_pass",
            "xp_credit_card_no" to "4111222233334444",
            "card_note" to "Paid with 4111222233334444",
            "mp_amount" to "50.00",
            "mp_merchant_ID" to "merchant_01"
        )

        val redactedMap = TelemetryRedactor.redactMap(map)

        assertEquals("[REDACTED]", redactedMap["mp_verification_key"])
        assertEquals("[REDACTED]", redactedMap["mp_password"])
        assertEquals("[REDACTED]", redactedMap["xp_credit_card_no"])
        assertEquals("Paid with 411122******4444", redactedMap["card_note"])
        assertEquals("50.00", redactedMap["mp_amount"])
        assertEquals("merchant_01", redactedMap["mp_merchant_ID"])
    }
}
