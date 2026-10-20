package com.fiuu.xdkandroid

import com.fiuu.xdkandroid.domain.entity.resolveAmount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResolveAmountTest {

    private fun amount(value: Any?) = mapOf("mp_amount" to value).resolveAmount()

    @Test
    fun padsToTwoDecimalPlaces() {
        assertEquals("1.00", amount("1"))
        assertEquals("1.00", amount("1.0"))
        assertEquals("1.00", amount(1.0))
        assertEquals("1.00", amount(1))
        assertEquals("1.50", amount("1.5"))
        assertEquals("1.00", amount("1."))
        assertEquals("1.01", amount("1.01"))
        assertEquals("0.00", amount("0"))
        assertEquals("10.00", amount(" 10 "))
    }

    @Test
    fun nullStaysNull() {
        assertNull(amount(null))
        assertNull(emptyMap<String, Any?>().resolveAmount())
    }

    @Test
    fun leavesUnroundableOrInvalidValuesUnchanged() {
        assertEquals("1.234", amount("1.234"))
        assertEquals("abc", amount("abc"))
        assertEquals(".5", amount(".5"))
    }
}
