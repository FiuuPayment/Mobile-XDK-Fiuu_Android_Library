package com.fiuu.xdkandroid

import com.fiuu.xdkandroid.data.remote.dto.shouldClearStoredSession
import com.fiuu.xdkandroid.viewmodel.sessionTimeoutResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// See documentations/wallet-pending-session-expiry-design.md.
class WalletPendingSessionExpiryTest {

    // --- shouldClearStoredSession ---

    @Test
    fun testPendingAndTimeoutKeepStoredSession() {
        assertFalse(shouldClearStoredSession("22"))
        assertFalse(shouldClearStoredSession("408"))
    }

    @Test
    fun testMissingStatusKeepsStoredSession() {
        assertFalse(shouldClearStoredSession(null))
        assertFalse(shouldClearStoredSession(""))
        assertFalse(shouldClearStoredSession("  "))
    }

    @Test
    fun testFinalResultsClearStoredSession() {
        assertTrue(shouldClearStoredSession("00"))
        assertTrue(shouldClearStoredSession("11"))
        assertTrue(shouldClearStoredSession("USER_CANCELLED"))
    }

    // --- sessionTimeoutResult ---

    @Test
    fun testSessionTimeoutResultMatchesPreviousHostContract() {
        val result = sessionTimeoutResult()
        assertEquals("408", result.status)
        assertFalse(result.statusCheck)
        assertEquals("Payment session expired", result.message)
        assertEquals("SDK_TIMEOUT", result.errorCode)
        assertEquals("400", result.errorDetails?.code)
        assertEquals("The process took longer than 5 minutes", result.errorDetails?.message)
        assertEquals("TIMEOUT", result.errorDetails?.type)
    }
}
