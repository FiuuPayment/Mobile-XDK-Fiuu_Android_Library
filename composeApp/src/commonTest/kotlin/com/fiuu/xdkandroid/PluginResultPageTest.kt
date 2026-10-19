package com.fiuu.xdkandroid

import com.fiuu.xdkandroid.data.remote.dto.PaymentInquireResponseDto
import com.fiuu.xdkandroid.data.remote.dto.INQUIRE_POLL_DELAYS_MS
import com.fiuu.xdkandroid.data.remote.dto.resultPageKey
import com.fiuu.xdkandroid.domain.entity.isPluginPayment
import com.fiuu.xdkandroid.domain.entity.toPaymentRequest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PluginResultPageTest {

    // --- resultPageKey ---

    @Test
    fun testResultPageKeyFromObservedExpressUrl() {
        val url = "https://pay.fiuu.com/MOLPay/result.php?k=a05682b0494067963148beb11619ee07475933a7065083f6d5&extname=Visa"
        assertEquals("a05682b0494067963148beb11619ee07475933a7065083f6d5", resultPageKey(url))
    }

    @Test
    fun testResultPageKeyNotFirstParam() {
        assertEquals("abc123", resultPageKey("https://pay.fiuu.com/MOLPay/result.php?extname=Visa&k=abc123"))
    }

    @Test
    fun testResultPageKeyEmptyIsNull() {
        assertNull(resultPageKey("https://pay.fiuu.com/MOLPay/result.php?k=&extname=Visa"))
    }

    @Test
    fun testResultPageKeyMissingIsNull() {
        assertNull(resultPageKey("https://pay.fiuu.com/MOLPay/result.php?extname=Visa"))
        assertNull(resultPageKey("https://pay.fiuu.com/MOLPay/result.php"))
    }

    @Test
    fun testResultPageKeyIgnoresSimilarParamNames() {
        // "kk=" / "sk=" must not be mistaken for k.
        assertNull(resultPageKey("https://pay.fiuu.com/MOLPay/result.php?kk=abc&sk=def"))
    }

    @Test
    fun testResultPageKeyIgnoresFragment() {
        assertEquals("abc", resultPageKey("https://pay.fiuu.com/MOLPay/result.php?k=abc#top"))
    }

    // --- isPluginPayment ---

    private val baseRequest = mapOf<String, Any?>(
        "mp_merchant_ID" to "merchant",
        "mp_amount" to "1.01",
        "mp_order_ID" to "ORD-1",
        "mp_currency" to "MYR",
    ).toPaymentRequest()

    private val someJson = JsonObject(mapOf("token" to JsonPrimitive("x")))

    @Test
    fun testIsPluginPaymentForPluginData() {
        assertTrue(baseRequest.copy(googlePayData = someJson).isPluginPayment)
        assertTrue(baseRequest.copy(samsungPayData = someJson).isPluginPayment)
        assertTrue(baseRequest.copy(c2pData = someJson).isPluginPayment)
        assertTrue(baseRequest.copy(applePayToken = someJson).isPluginPayment)
    }

    @Test
    fun testIsPluginPaymentForChannelType() {
        assertTrue(baseRequest.copy(channelType = "PLUGIN").isPluginPayment)
        assertTrue(baseRequest.copy(channelType = "plugin").isPluginPayment)
    }

    @Test
    fun testIsNotPluginPaymentForOtherChannels() {
        assertFalse(baseRequest.isPluginPayment)
        assertFalse(baseRequest.copy(channel = "FPX_MB2U", channelType = "IB").isPluginPayment)
        assertFalse(baseRequest.copy(channel = "CardPayment", channelType = "CC").isPluginPayment)
    }

    // --- polling schedule / stop rule ---

    @Test
    fun testPollScheduleRampsThenCapsAtThreeSeconds() {
        val delays = INQUIRE_POLL_DELAYS_MS
        assertEquals(listOf(0L, 1_000L, 2_000L), delays.take(3))
        assertTrue(delays.drop(3).all { it == 3_000L })
    }

    @Test
    fun testPollScheduleStaysWithinTotal() {
        val delays = INQUIRE_POLL_DELAYS_MS
        assertEquals(30_000L, delays.sum())
        // 0 + 1 + 2 + 9 × 3 = 30 s → 12 lookups.
        assertEquals(12, delays.size)
    }

    @Test
    fun testPollStopsOnlyOnTerminalStatus() {
        assertTrue(PaymentInquireResponseDto(status = "00").isTerminal)
        assertTrue(PaymentInquireResponseDto(status = "11").isTerminal)
        assertFalse(PaymentInquireResponseDto(status = "22").isTerminal)
        assertFalse(PaymentInquireResponseDto(status = null).isTerminal)
    }
}
