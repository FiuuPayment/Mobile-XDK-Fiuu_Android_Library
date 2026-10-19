package com.fiuu.xdkandroid

import com.fiuu.xdkandroid.diagnostics.DiagnosticChaosEngine
import com.fiuu.xdkandroid.diagnostics.SimulatedNetworkMode
import com.fiuu.xdkandroid.domain.entity.toPaymentRequest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiagnosticChaosEngineTest {

    @BeforeTest
    fun setUp() {
        DiagnosticChaosEngine.networkMode = SimulatedNetworkMode.OFFLINE
        DiagnosticChaosEngine.isMockRootActive = true
        DiagnosticChaosEngine.forceWebViewLoadFail = true
    }

    @AfterTest
    fun tearDown() {
        DiagnosticChaosEngine.resetToDefaults()
        DiagnosticChaosEngine.isArmed = false
    }

    private fun assertInert() {
        assertFalse(DiagnosticChaosEngine.isArmed)
        assertEquals(SimulatedNetworkMode.NORMAL, DiagnosticChaosEngine.activeNetworkMode)
        assertFalse(DiagnosticChaosEngine.activeSimulatedOffline)
        assertFalse(DiagnosticChaosEngine.activeMockRoot)
        assertFalse(DiagnosticChaosEngine.activeWebViewLoadFail)
    }

    @Test
    fun `toggles are inert without mp_dev_mode`() {
        mapOf<String, Any?>("mp_sandbox_mode" to true).toPaymentRequest()
        assertInert()
    }

    @Test
    fun `toggles are inert on the production backend even with mp_dev_mode`() {
        mapOf<String, Any?>("mp_dev_mode" to true, "mp_build_mode" to 2).toPaymentRequest()
        assertInert()
    }

    @Test
    fun `toggles take effect for a dev-mode non-production session`() {
        mapOf<String, Any?>("mp_dev_mode" to true, "mp_sandbox_mode" to true).toPaymentRequest()
        assertTrue(DiagnosticChaosEngine.isArmed)
        assertTrue(DiagnosticChaosEngine.activeSimulatedOffline)
        assertTrue(DiagnosticChaosEngine.activeMockRoot)
        assertTrue(DiagnosticChaosEngine.activeWebViewLoadFail)
    }

    @Test
    fun `a later production session disarms a previous dev session`() {
        mapOf<String, Any?>("mp_dev_mode" to true).toPaymentRequest()
        mapOf<String, Any?>("mp_sandbox_mode" to false).toPaymentRequest()
        assertInert()
    }
}
