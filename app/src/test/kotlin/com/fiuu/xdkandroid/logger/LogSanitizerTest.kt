package com.fiuu.xdkandroid.logger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogSanitizerTest {

    @Test
    fun testPanMaskingPreservesFirstAndLastFourDigits() {
        // Contiguous PAN
        val visa = "Card: 4111222233334444 submitted"
        val sanitizedVisa = LogSanitizer.sanitize(visa)
        assertEquals("Card: 4111********4444 submitted", sanitizedVisa)

        // Mastercard
        val mastercard = "Processed MC 5500000000000004 for order"
        val sanitizedMastercard = LogSanitizer.sanitize(mastercard)
        assertEquals("Processed MC 5500********0004 for order", sanitizedMastercard)

        // Spaced PAN
        val spaced = "Card: 4111 2222 3333 4444 submitted"
        val sanitizedSpaced = LogSanitizer.sanitize(spaced)
        assertEquals("Card: 4111********4444 submitted", sanitizedSpaced)

        // Hyphenated PAN
        val hyphenated = "Card: 4111-2222-3333-4444 submitted"
        val sanitizedHyphenated = LogSanitizer.sanitize(hyphenated)
        assertEquals("Card: 4111********4444 submitted", sanitizedHyphenated)
    }

    @Test
    fun testPanMaskingCoversRangesOutsideLegacyIssuerAllowlist() {
        // Mastercard 2-series (2221-2720)
        assertEquals("MC2 2223********0011 ok", LogSanitizer.sanitize("MC2 2223000048400011 ok"))
        assertEquals("MC2 2223********0011 ok", LogSanitizer.sanitize("MC2 2223 0000 4840 0011 ok"))

        // UnionPay 16-digit and 19-digit
        assertEquals("CUP 6200********0005", LogSanitizer.sanitize("CUP 6200000000000005"))
        assertEquals("CUP 6250***********0005", LogSanitizer.sanitize("CUP 6250000000000000005"))

        // Maestro 67xx, 19-digit grouped 4-4-4-4-3
        assertEquals("Maestro 6759***********0003", LogSanitizer.sanitize("Maestro 6759-0000-0000-0000-003"))

        // Discover 644-649 and Diners 14-digit 4-6-4
        assertEquals("Disc 6445********0003", LogSanitizer.sanitize("Disc 6445000000000003"))
        assertEquals("Diners 3056******5904", LogSanitizer.sanitize("Diners 3056 930902 5904"))

        // Non-PAN numbers outside 13-19 digits stay untouched
        assertEquals("ts=1727500000 id=123456789012", LogSanitizer.sanitize("ts=1727500000 id=123456789012"))
    }

    @Test
    fun testJsonKeyValueRedaction() {
        val rawJson = """{"password": "dummy_test_password", "accessToken": "dummy_test_jwt_token", "amount": "10.00"}"""
        val sanitized = LogSanitizer.sanitize(rawJson)

        assertFalse("Password should be redacted", sanitized.contains("dummy_test_password"))
        assertFalse("AccessToken should be redacted", sanitized.contains("dummy_test_jwt_token"))
        assertTrue("Password key should have [REDACTED]", sanitized.contains("\"password\":\"[REDACTED]\""))
        assertTrue("AccessToken key should have [REDACTED]", sanitized.contains("\"accessToken\":\"[REDACTED]\""))
        assertTrue("Amount should remain untouched", sanitized.contains("\"amount\": \"10.00\""))
    }

    @Test
    fun testJsonRedactionHandlesEscapedQuotes() {
        val rawJson = """{"mp_password":"prefix\"SECRET_SUFFIX","amount":"10.00"}"""
        val sanitized = LogSanitizer.sanitize(rawJson)

        assertFalse("Suffix after escaped quote should be redacted", sanitized.contains("SECRET_SUFFIX"))
        assertEquals("""{"mp_password":"[REDACTED]","amount":"10.00"}""", sanitized)

        val vkeyJson = """{"mp_verification_key":"abc\\def\"ghi\\xyz"}"""
        val vkeySanitized = LogSanitizer.sanitize(vkeyJson)
        assertFalse(vkeySanitized.contains("ghi"))
        assertEquals("""{"mp_verification_key":"abc***xyz"}""", vkeySanitized)
    }

    @Test
    fun testQueryParamRedaction() {
        val rawUrl = "https://pay.fiuu.com/callback?orderId=ORD123&cvv=123&mp_password=mySecretPass&currency=MYR"
        val sanitized = LogSanitizer.sanitize(rawUrl)

        assertFalse(sanitized.contains("mySecretPass"))
        assertFalse(sanitized.contains("cvv=123"))
        assertTrue(sanitized.contains("cvv=[REDACTED]"))
        assertTrue(sanitized.contains("mp_password=[REDACTED]"))
        assertTrue(sanitized.contains("orderId=ORD123"))
        assertTrue(sanitized.contains("currency=MYR"))
    }

    @Test
    fun testHintPreservingKeys() {
        val raw = "Loading verificationKey=sandbox_vkey_98765 for merchant"
        val sanitized = LogSanitizer.sanitize(raw)

        assertFalse(sanitized.contains("sandbox_vkey_98765"))
        assertTrue("Should preserve prefix and suffix hint", sanitized.contains("verificationKey=san***765"))

        val jsonRaw = """{"vkey": "prod_vkey_abcdef123"}"""
        val jsonSanitized = LogSanitizer.sanitize(jsonRaw)
        assertFalse(jsonSanitized.contains("prod_vkey_abcdef123"))
        assertTrue("Should preserve json vkey hint", jsonSanitized.contains("\"vkey\":\"pro***123\""))
    }

    @Test
    fun testAuthHeaderRedaction() {
        val header = "POST /api HTTP/1.1\nAuthorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9\nHost: pay.fiuu.com"
        val sanitized = LogSanitizer.sanitize(header)

        assertFalse(sanitized.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"))
        assertTrue(sanitized.contains("Authorization: Bearer [REDACTED]"))
    }

    @Test
    fun testCustomerPiiMasking() {
        val raw = "Customer details: cust_email=john.doe@example.com&cust_phone=+60123456789"
        val sanitized = LogSanitizer.sanitize(raw)

        assertFalse(sanitized.contains("john.doe@example.com"))
        assertFalse(sanitized.contains("+60123456789"))
        assertTrue(sanitized.contains("cust_email=j***e@example.com"))
        assertTrue(sanitized.contains("cust_phone=+601******89"))
    }
}
