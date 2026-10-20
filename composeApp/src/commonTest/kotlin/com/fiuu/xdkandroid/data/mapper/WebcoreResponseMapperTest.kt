package com.fiuu.xdkandroid.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebcoreResponseMapperTest {

    @Test
    fun testMapSuccessfulWebcoreResponse() {
        val rawJson = """
            {
                "status_code": "00",
                "amount": "10.50",
                "currency": "MYR",
                "channel": "credit",
                "tranID": "12345678",
                "order_id": "ORD-998877",
                "msgdesc": "Approved",
                "skey": "abcdef123456",
                "paydate": "2026-09-02 12:00:00",
                "appcode": "AP123"
            }
        """.trimIndent()

        val result = WebcoreResponseMapper.mapJsonToDto(rawJson)

        assertEquals("00", result.status)
        assertTrue(result.statusCheck)
        assertEquals("Approved", result.message)
        assertEquals("10.50", result.amount)
        assertEquals("MYR", result.currency)
        assertEquals("credit", result.channel)
        assertEquals("12345678", result.tranID)
        assertEquals("ORD-998877", result.referenceNo)
        assertEquals("abcdef123456", result.skey)
        assertEquals("2026-09-02 12:00:00", result.paydate)
        assertEquals("AP123", result.appcode)
    }

    @Test
    fun testMapPendingWebcoreResponse() {
        val rawJson = """
            {
                "status_code": "11",
                "amount": "5.00",
                "currency": "MYR",
                "order_id": "ORD-112233",
                "msgdesc": "Pending Authorization"
            }
        """.trimIndent()

        val result = WebcoreResponseMapper.mapJsonToDto(rawJson)

        assertEquals("11", result.status)
        assertFalse(result.statusCheck)
        assertEquals("Pending Authorization", result.message)
        assertEquals("ORD-112233", result.referenceNo)
    }

    @Test
    fun testMapCancelledWebcoreResponse() {
        val rawJson = """
            {
                "status": "9999",
                "message": "Payment was cancelled or dismissed"
            }
        """.trimIndent()

        val result = WebcoreResponseMapper.mapJsonToDto(rawJson)

        assertEquals("9999", result.status)
        assertFalse(result.statusCheck)
        assertEquals("Payment was cancelled or dismissed", result.message)
    }

    @Test
    fun testMapFailedWebcoreResponse() {
        val rawJson = """
            {
                "status_code": "99",
                "amount": "15.00",
                "msgdesc": "Payment Declined",
                "error_code": "ERR_CARD_DECLINED",
                "error_desc": "Insufficient funds"
            }
        """.trimIndent()

        val result = WebcoreResponseMapper.mapJsonToDto(rawJson)

        assertEquals("99", result.status)
        assertFalse(result.statusCheck)
        assertEquals("Payment Declined", result.message)
        assertEquals("ERR_CARD_DECLINED", result.errorCode)
        assertEquals("Insufficient funds", result.errorDesc)
    }

    @Test
    fun testMapNullOrEmptyResponse() {
        val nullResult = WebcoreResponseMapper.mapJsonToDto(null)
        assertEquals("9999", nullResult.status)
        assertFalse(nullResult.statusCheck)
        assertEquals("Payment was cancelled or dismissed", nullResult.message)

        val emptyResult = WebcoreResponseMapper.mapJsonToDto("")
        assertEquals("9999", emptyResult.status)
        assertFalse(emptyResult.statusCheck)
        assertEquals("Payment was cancelled or dismissed", emptyResult.message)
    }

    @Test
    fun testMapMalformedJsonGracefulFallback() {
        val malformedJson = "NOT_A_VALID_JSON"
        val result = WebcoreResponseMapper.mapJsonToDto(malformedJson)

        assertEquals("9999", result.status)
        assertFalse(result.statusCheck)
        assertTrue(result.message?.contains("Failed to parse Webcore response") == true)
        assertEquals("NOT_A_VALID_JSON", result.errorDesc)
    }

    @Test
    fun testMapWebcoreNativeErrorResponse() {
        val rawJson = """
            {
                "Error": "Missing mandatory SDK data: mp_merchant_ID",
                "mp_transaction_request_type": null
            }
        """.trimIndent()

        val result = WebcoreResponseMapper.mapJsonToDto(rawJson)

        assertEquals("9999", result.status)
        assertFalse(result.statusCheck)
        assertEquals("Missing mandatory SDK data: mp_merchant_ID", result.message)
        assertEquals("Missing mandatory SDK data: mp_merchant_ID", result.errorDesc)
    }

    @Test
    fun testMapWebcoreDetailsNullError() {
        val rawJson = """{ "error" : " Payment details is null." }"""

        val result = WebcoreResponseMapper.mapJsonToDto(rawJson)

        assertEquals("9999", result.status)
        assertFalse(result.statusCheck)
        assertEquals(" Payment details is null.", result.message)
        assertEquals(" Payment details is null.", result.errorDesc)
    }

    @Test
    fun testMapDigitalWalletExpressResponse() {
        val expressSuccessJson = """
            {
                "status": "00",
                "statusCheck": true,
                "message": "Payment Successful",
                "amount": "25.00",
                "currency": "MYR",
                "channel": "GooglePay",
                "tranID": "TXN_GPAY_998877",
                "referenceNo": "ORD_GPAY_1234",
                "paydate": "2026-09-03 16:00:00"
            }
        """.trimIndent()

        val result = WebcoreResponseMapper.mapJsonToDto(expressSuccessJson)

        assertEquals("00", result.status)
        assertTrue(result.statusCheck)
        assertEquals("Payment Successful", result.message)
        assertEquals("25.00", result.amount)
        assertEquals("MYR", result.currency)
        assertEquals("GooglePay", result.channel)
        assertEquals("TXN_GPAY_998877", result.tranID)
        assertEquals("ORD_GPAY_1234", result.referenceNo)
    }
}
