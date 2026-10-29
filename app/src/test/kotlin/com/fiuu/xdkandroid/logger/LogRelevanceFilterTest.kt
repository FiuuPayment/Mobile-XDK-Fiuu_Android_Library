package com.fiuu.xdkandroid.logger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogRelevanceFilterTest {

    @Test
    fun testDropsAppCompatThemeAndDynamiteSpam() {
        val appCompatSpam = "View class android.support.v7.widget.AppCompatTextView is an AppCompat widget that can only be used with a Theme.AppCompat theme (or descendant)."
        val dynamiteSpam = "View class com.google.android.gms.wallet.dynamite.common.ui.RoundedImageViewWithBorder is an AppCompat widget that can only be used with a Theme.AppCompat theme (or descendant)."
        val dynamiteModuleNoise = "DynamiteModule: Local module descriptor class for com.google.android.gms.wallet.dynamite not found."
        val skiaNoise = "D/skia: Canvas transform error"
        val openGlNoise = "W/OpenGLRenderer: Failed to set surface dimensions"
        val choreographerNoise = "Choreographer: Skipped 35 frames! The application may be doing too much work on its main thread."
        val windowBackNoise = "OnBackInvokedCallback is not enabled for the application."
        val insetsNoise = "InsetsController: show(ime(), fromIme=true) failed: no window token"

        assertFalse("AppCompat warning must be filtered out", LogRelevanceFilter.isRelevant(appCompatSpam))
        assertFalse("Dynamite warning must be filtered out", LogRelevanceFilter.isRelevant(dynamiteSpam))
        assertFalse("Dynamite module noise must be filtered out", LogRelevanceFilter.isRelevant(dynamiteModuleNoise))
        assertFalse("Skia noise must be filtered out", LogRelevanceFilter.isRelevant(skiaNoise))
        assertFalse("OpenGL noise must be filtered out", LogRelevanceFilter.isRelevant(openGlNoise))
        assertFalse("Choreographer noise must be filtered out", LogRelevanceFilter.isRelevant(choreographerNoise))
        assertFalse("Window back callback noise must be filtered out", LogRelevanceFilter.isRelevant(windowBackNoise))
        assertFalse("Insets controller noise must be filtered out", LogRelevanceFilter.isRelevant(insetsNoise))
    }

    @Test
    fun testPreservesLegitimatePaymentSdkLogs() {
        val sdkInit = "FiuuPaymentSDK: initialize() called with channel=multi"
        val gpayHandler = "GooglePayHandler: loadPaymentData (amount=1.01, currency=MYR)"
        val paymentError = """PaymentViewModel: paymentError status=M00008 message=Transaction not found"""
        val statusError = "Status: M00008 (statusCheck=false)"
        val ktorHttp = "POST https://sandbox-mobile.fiuu.com/pay/initiate - Status 200 (124ms)"

        assertTrue("SDK init log must be preserved", LogRelevanceFilter.isRelevant(sdkInit))
        assertTrue("Google Pay handler log must be preserved", LogRelevanceFilter.isRelevant(gpayHandler))
        assertTrue("Payment error must be preserved", LogRelevanceFilter.isRelevant(paymentError))
        assertTrue("Status error must be preserved", LogRelevanceFilter.isRelevant(statusError))
        assertTrue("Ktor HTTP log must be preserved", LogRelevanceFilter.isRelevant(ktorHttp))
    }

    @Test
    fun testExtractsErrorCodeAndMessageFromPayload() {
        val payload = """{"msgType":"D7","error_code":"M00008","error_message":"Transaction not found"}"""
        val extractedCode = LogRelevanceFilter.extractErrorCode(payload)
        val extractedMsg = LogRelevanceFilter.extractErrorMessage(payload)

        assertNotNull(extractedCode)
        assertEquals("M00008", extractedCode)
        assertNotNull(extractedMsg)
        assertEquals("Transaction not found", extractedMsg)

        val statusLine = "Status: M00008 (statusCheck=false)"
        assertEquals("M00008", LogRelevanceFilter.extractErrorCode(statusLine))
    }
}
