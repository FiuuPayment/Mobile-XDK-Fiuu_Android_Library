package com.fiuu.xdkandroid

import com.fiuu.xdkandroid.utils.NumberFormatter
import kotlin.test.Test
import kotlin.test.assertEquals

class NumberFormatterTest {

    @Test
    fun testFormatMinorAmountStandard() {
        assertEquals("1234.56", NumberFormatter.formatMinorAmount(123456L))
        assertEquals("1.01", NumberFormatter.formatMinorAmount(101L))
        assertEquals("0.05", NumberFormatter.formatMinorAmount(5L))
        assertEquals("0.00", NumberFormatter.formatMinorAmount(0L))
    }

    @Test
    fun testFormatMinorAmountLargeValue() {
        // Exceeds Int32 MAX_VALUE (2147483647) to ensure Long arithmetic does not overflow
        val largeAmount = 214748364800L
        assertEquals("2147483648.00", NumberFormatter.formatMinorAmount(largeAmount))
    }

    @Test
    fun testFormatAmountDouble() {
        assertEquals("12.34", NumberFormatter.formatAmount(1234.0))
        assertEquals("1.01", NumberFormatter.formatAmount(101.0))
        assertEquals("0.00", NumberFormatter.formatAmount(0.0))
    }
}
