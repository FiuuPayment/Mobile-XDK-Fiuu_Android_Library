package com.fiuu.xdkandroid

import com.fiuu.xdkandroid.utils.redactSensitiveJson
import com.fiuu.xdkandroid.utils.redactSensitiveMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RedactionTest {

    @Test
    fun testRedactSensitiveJsonScalars() {
        val rawJson = """{"mp_password": "supersecretpassword", "mp_amount": "10.00", "xp_credit_card_no": "4111222233334444"}"""
        val redacted = redactSensitiveJson(rawJson)
        assertFalse(redacted.contains("supersecretpassword"))
        assertFalse(redacted.contains("4111222233334444"))
        assertTrue(redacted.contains("\"[REDACTED]\""))
        assertTrue(redacted.contains("\"mp_amount\": \"10.00\""))
    }

    @Test
    fun testRedactSensitiveJsonNestedObject() {
        val rawJson = """{"xp_googlepay": {"token": "secret_token_data", "type": "PAYMENT_GATEWAY"}, "orderId": "123"}"""
        val redacted = redactSensitiveJson(rawJson)
        assertFalse(redacted.contains("secret_token_data"))
        assertTrue(redacted.contains("\"[REDACTED]\""))
        assertTrue(redacted.contains("\"orderId\": \"123\""))
    }

    // The instalment-tenure request carries the full PAN under its own mp_cc_* names, and a
    // saved-card /process carries the reusable token — both used to be logged in the clear.
    @Test
    fun testRedactsInstalmentPanAndSavedCardToken() {
        val tenure = """{"mp_merchant_ID":"m1","mp_cc_pan":"4111222233334444","mp_cc_month":"12","mp_cc_year":"2030","mp_amount":"10.00"}"""
        val redactedTenure = redactSensitiveJson(tenure)
        assertFalse(redactedTenure.contains("4111222233334444"))
        assertFalse(redactedTenure.contains("2030"))
        assertTrue(redactedTenure.contains("\"mp_amount\":\"10.00\""))

        val process = """{"xp_channel":"CREDITAN","xp_credit_card_token":"tok_saved_123","xp_token_status":"2"}"""
        val redactedProcess = redactSensitiveJson(process)
        assertFalse(redactedProcess.contains("tok_saved_123"))
        assertTrue(redactedProcess.contains("\"xp_token_status\":\"2\""))
    }

    @Test
    fun testRedactSensitiveMap() {
        val map = mapOf(
            "mp_verification_key" to "secret_vkey",
            "mp_amount" to "100.00",
            "xp_credit_card_cvv" to "123"
        )
        val redacted = redactSensitiveMap(map)
        assertEquals("[REDACTED]", redacted["mp_verification_key"])
        assertEquals("[REDACTED]", redacted["xp_credit_card_cvv"])
        assertEquals("100.00", redacted["mp_amount"])
    }
}
