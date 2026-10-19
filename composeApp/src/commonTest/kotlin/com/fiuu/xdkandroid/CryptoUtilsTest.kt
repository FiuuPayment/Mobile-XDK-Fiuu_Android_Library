package com.fiuu.xdkandroid

import com.fiuu.xdkandroid.utils.CryptoUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CryptoUtilsTest {

    @Test
    fun testGenerateNonce() {
        val nonce1 = CryptoUtils.generateNonce()
        val nonce2 = CryptoUtils.generateNonce()
        assertTrue(nonce1.isNotEmpty())
        assertTrue(nonce2.isNotEmpty())
        assertNotEquals(nonce1, nonce2)
        assertTrue(nonce1.contains("-"))
    }

    @Test
    fun testGenerateUuid() {
        val uuid1 = CryptoUtils.generateUuid()
        val uuid2 = CryptoUtils.generateUuid()
        assertNotEquals(uuid1, uuid2)
        assertEquals(36, uuid1.length)
        assertTrue(uuid1.contains("-"))
    }

    @Test
    fun testGenerateSignature() {
        val sig = CryptoUtils.generateSignature(
            method = "POST",
            path = "/initiate",
            timestamp = "1725177600",
            nonce = "randomnonce1234567890123456789012",
            checksum = "abcdef0123456789abcdef0123456789"
        )
        assertEquals(64, sig.length)
        assertTrue(sig.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun testGenerateChannelChecksum() {
        val chk = CryptoUtils.generateChannelChecksum(
            merchantId = "test_merchant",
            appName = "test_app",
            currency = "MYR",
            verificationKey = "secret_key"
        )
        // Legacy channel checksum uses SHA1 (40 hex chars)
        assertEquals(40, chk.length)
        assertTrue(chk.all { it.isDigit() || it in 'a'..'f' })
    }
}
