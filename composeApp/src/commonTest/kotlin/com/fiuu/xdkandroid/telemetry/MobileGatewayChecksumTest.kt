package com.fiuu.xdkandroid.telemetry

import com.fiuu.xdkandroid.telemetry.model.ActivityLogData
import com.fiuu.xdkandroid.telemetry.model.ActivityLogMessage
import com.fiuu.xdkandroid.telemetry.model.TelemetryDeviceInfo
import com.fiuu.xdkandroid.telemetry.model.TelemetryProductInfo
import com.fiuu.xdkandroid.utils.localeCompareBase
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MobileGatewayChecksumTest {

    @Test
    fun testGetCurrentUtcDateTimeFormat() {
        val dt = MobileGatewayChecksum.getCurrentUtcDateTime()
        assertEquals(14, dt.length)
        assertTrue(dt.all { it.isDigit() })
    }

    @Test
    fun testResolveGatewaySecret() {
        val prodSecret = MobileGatewayChecksum.resolveGatewaySecret("https://mobilegateway.fiuu.com/RMS/API/Direct/activitylog.php")
        val uatSecret = MobileGatewayChecksum.resolveGatewaySecret("https://uat-mobilegateway.fiuu.com/RMS/API/Direct/activitylog.php")
        val sandboxSecret = MobileGatewayChecksum.resolveGatewaySecret("https://sandbox-mobilegateway.fiuu.com/RMS/API/Direct/activitylog.php")
        val customSecret = MobileGatewayChecksum.resolveGatewaySecret("https://any-url.com", "custom_override_secret")

        assertEquals(MobileGatewayChecksum.PROD_GATEWAY_SECRET, prodSecret)
        assertEquals(MobileGatewayChecksum.UAT_GATEWAY_SECRET, uatSecret)
        assertEquals(MobileGatewayChecksum.UAT_GATEWAY_SECRET, sandboxSecret)
        assertEquals("custom_override_secret", customSecret)
    }

    @Test
    fun testGenerateChecksumDeterministicSha512() {
        val jsonTree = JsonObject(
            mapOf(
                "key" to JsonPrimitive("value"),
                "order" to JsonPrimitive("ORD-12345")
            )
        )
        val timestamp = "20260925120000"
        val secret = "test_secret"

        val checksum1 = MobileGatewayChecksum.generateChecksumFromJson(secret, timestamp, jsonTree)
        val checksum2 = MobileGatewayChecksum.generateChecksumFromJson(secret, timestamp, jsonTree)

        assertEquals(checksum1, checksum2)
        assertEquals(128, checksum1.length)
        assertTrue(checksum1.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun testGenerateChecksumWithTypedModels() {
        val data = ActivityLogData(
            deviceInfo = TelemetryDeviceInfo(os = "Android", brand = "Google", model = "Pixel", modelNo = "Pixel 8"),
            productInfo = TelemetryProductInfo(type = "XDK Native", version = "1.0.0", sdkVersion = "1.0.10")
        )
        val message = ActivityLogMessage(
            referenceNumber = "ORD-12345",
            process = "ONLINE_INITIATE",
            type = "event",
            details = "Initiating payment"
        )
        val timestamp = "20260925150000"
        val secret = MobileGatewayChecksum.UAT_GATEWAY_SECRET

        val checksum = MobileGatewayChecksum.generateChecksum(secret, timestamp, data, message)
        assertEquals(128, checksum.length)
        assertTrue(checksum.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun testLocaleCompareBaseCollation() {
        // Unicode primary collation: case-insensitive alphabetical ordering
        assertTrue(localeCompareBase("Apple", "banana") < 0)
        assertTrue(localeCompareBase("Zebra", "apple") > 0)
        assertEquals(0, localeCompareBase("Apple", "apple"))
        assertTrue(localeCompareBase("beta", "alpha") > 0)
    }
}
