package com.fiuu.xdkandroid

import com.fiuu.xdkandroid.data.remote.dto.PaymentResponseDto
import com.fiuu.xdkandroid.data.remote.dto.shouldClearStoredSession
import com.fiuu.xdkandroid.viewmodel.offlineVoucherResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// See documentations/offline-voucher-result-design.md.
class OfflineVoucherResultTest {

    // The /process RESPONSE for a voucher carries no status — only IDs.
    private val issued = PaymentResponseDto(
        status = "",
        statusCheck = false,
        referenceNo = "ORDER-1",
        transactionId = "30012345",
        amount = "10.00",
        currency = "MYR",
        channel = "Cash-711"
    )

    @Test
    fun testFailedLookupIsPendingNeverPaid() {
        val result = offlineVoucherResult(issued, inquired = null)
        assertEquals("22", result.status)
        assertFalse(result.statusCheck)
        assertEquals("30012345", result.transactionId)
        assertEquals("30012345", result.tranID)
        assertEquals("ORDER-1", result.referenceNo)
    }

    @Test
    fun testBlankLookupStatusIsPending() {
        val result = offlineVoucherResult(issued, PaymentResponseDto(status = "  "))
        assertEquals("22", result.status)
        assertFalse(result.statusCheck)
    }

    @Test
    fun testIssuedStatusCheckIsNotTrusted() {
        val result = offlineVoucherResult(issued.copy(statusCheck = true), inquired = null)
        assertFalse(result.statusCheck)
    }

    @Test
    fun testUnpaidVoucherIsPending() {
        val result = offlineVoucherResult(issued, PaymentResponseDto(status = "22"))
        assertEquals("22", result.status)
        assertFalse(result.statusCheck)
        assertEquals("30012345", result.transactionId)
    }

    @Test
    fun testPaidAtCounterIsSuccess() {
        val result = offlineVoucherResult(
            issued,
            PaymentResponseDto(status = "00", statusCheck = false, paydate = "1759737600")
        )
        assertEquals("00", result.status)
        assertTrue(result.statusCheck)
        assertEquals("1759737600", result.paydate)
        // /inquire may omit IDs — fall back to the ones /process returned.
        assertEquals("30012345", result.transactionId)
        assertEquals("30012345", result.tranID)
        assertEquals("ORDER-1", result.referenceNo)
        assertEquals("Cash-711", result.channel)
    }

    @Test
    fun testOtherGatewayStatusPassesThrough() {
        val result = offlineVoucherResult(issued, PaymentResponseDto(status = "11", message = "Expired"))
        assertEquals("11", result.status)
        assertFalse(result.statusCheck)
        assertEquals("Expired", result.message)
    }

    @Test
    fun testPendingVoucherKeepsStoredSession() {
        assertFalse(shouldClearStoredSession(offlineVoucherResult(issued, inquired = null).status))
    }
}
