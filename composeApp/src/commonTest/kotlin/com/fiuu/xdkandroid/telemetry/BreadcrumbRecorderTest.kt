package com.fiuu.xdkandroid.telemetry

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BreadcrumbRecorderTest {

    @BeforeTest
    fun setUp() {
        BreadcrumbRecorder.clear()
    }

    @Test
    fun testRecordAndSnapshot() {
        BreadcrumbRecorder.record("SDK", "init", "SDK initialized", "INFO")
        BreadcrumbRecorder.record("UI", "render", "Checkout rendered", "DEBUG")

        val list = BreadcrumbRecorder.snapshot()
        assertEquals(2, list.size)
        assertEquals("SDK", list[0].area)
        assertEquals("init", list[0].process)
        assertEquals("SDK initialized", list[0].message)

        assertEquals("UI", list[1].area)
        assertEquals("render", list[1].process)
        assertEquals("Checkout rendered", list[1].message)
    }

    @Test
    fun testCircularBufferRolloverAt20() {
        // Record 25 entries
        for (i in 1..25) {
            BreadcrumbRecorder.record("TEST", "step", "Message $i")
        }

        val snapshot = BreadcrumbRecorder.snapshot()
        assertEquals(20, snapshot.size)
        
        // The first 5 should have been evicted; earliest remaining should be Message 6
        assertEquals("Message 6", snapshot.first().message)
        assertEquals("Message 25", snapshot.last().message)
    }

    @Test
    fun testChronologicalFormatting() {
        BreadcrumbRecorder.record("API", "call", "Initiate started")
        BreadcrumbRecorder.record("NET", "resp", "Response received")

        val formatted = BreadcrumbRecorder.formatAsList()
        assertEquals(2, formatted.size)
        assertTrue(formatted[0].contains("[API]"))
        assertTrue(formatted[0].contains("(call)"))
        assertTrue(formatted[0].contains("Initiate started"))

        assertTrue(formatted[1].contains("[NET]"))
        assertTrue(formatted[1].contains("(resp)"))
        assertTrue(formatted[1].contains("Response received"))
    }

    @Test
    fun testClear() {
        BreadcrumbRecorder.record("TEST", "step", "Message")
        assertEquals(1, BreadcrumbRecorder.snapshot().size)

        BreadcrumbRecorder.clear()
        assertTrue(BreadcrumbRecorder.snapshot().isEmpty())
        assertTrue(BreadcrumbRecorder.formatAsList().isEmpty())
    }
}
