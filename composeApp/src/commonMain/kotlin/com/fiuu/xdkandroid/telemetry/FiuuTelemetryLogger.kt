package com.fiuu.xdkandroid.telemetry

import com.fiuu.xdkandroid.telemetry.model.TelemetryEventType
import com.fiuu.xdkandroid.telemetry.model.TelemetryOrigin
import com.fiuu.xdkandroid.telemetry.model.TelemetryProcess
import com.fiuu.xdkandroid.utils.FiuuLogger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Unified in-library telemetry and observability engine for FiuuXDK.
 *
 * Implements low-noise, anomaly-only telemetry across both Modern CMP and Legacy Webcore engines:
 * 1. Granular milestones are recorded silently in-memory as breadcrumbs ([BreadcrumbRecorder]).
 * 2. Outbound checkout requests are buffered in RAM as a Write-Ahead Log (WAL).
 * 3. On success ("00"), buffered requests are discarded and 0 remote HTTP logs are dispatched.
 * 4. On decline, timeout, or crash, the buffered request is flushed first, followed by the decline
 *    response with the full breadcrumb trail attached.
 * 5. Intentional user aborts (Cancel / Back button) and pending results ("22") are stamped in RAM
 *    only (0 remote logs).
 */
object FiuuTelemetryLogger {

    private val json = Json { prettyPrint = false }

    /**
     * Records an in-memory breadcrumb step without network overhead.
     * Mirrors to console stdout when [FiuuLogger.enabled] (mp_dev_mode) is true.
     */
    fun recordBreadcrumb(
        area: String,
        process: String,
        message: String,
        level: String = "INFO",
    ) {
        val safeMessage = TelemetryRedactor.redact(message)
        BreadcrumbRecorder.record(
            area = area,
            process = process,
            message = safeMessage,
            level = level,
        )
        if (FiuuLogger.enabled) {
            println("[FiuuTelemetry] [Breadcrumb] [$area] ($process) $safeMessage")
        }
    }

    /**
     * Buffers an outbound checkout request in memory (WAL).
     */
    fun bufferOutboundRequest(
        process: String,
        details: String,
        referenceNumber: String = "",
        merchantId: String = "",
        countryCode: String = "MY",
        networkType: String = "[ConnectivityResult.none]",
        customUrl: String? = null,
        customSecret: String? = null,
    ) {
        recordBreadcrumb(
            area = "HTTP_REQUEST",
            process = process,
            message = "Outbound request dispatched ref=$referenceNumber",
            level = "INFO"
        )
        TelemetryDispatcher.bufferOutboundRequest(
            process = process,
            details = details,
            referenceNumber = referenceNumber,
            merchantId = merchantId,
            countryCode = countryCode,
            networkType = networkType,
            customUrl = customUrl,
            customSecret = customSecret,
        )
    }

    /**
     * Records a successful checkout approval ("00").
     * Suppresses remote HTTP dispatch and silently discards the buffered outbound request.
     */
    fun logHostApproval(
        process: String,
        statusCode: String = "00",
        latencyMs: Long = 0,
    ) {
        recordBreadcrumb(
            area = "HTTP_RESPONSE",
            process = process,
            message = "Response approved statusCode=$statusCode latencyMs=$latencyMs",
            level = "INFO"
        )
        TelemetryDispatcher.discardBufferedRequest()
    }

    /**
     * Logs a host bank or e-wallet decline (status != "00").
     * Flushes the buffered outbound request first (Entry 1), then dispatches the decline (Entry 2).
     */
    fun logHostDecline(
        process: String,
        statusCode: String,
        statusMessage: String,
        source: String = TelemetryOrigin.EXTERNAL_HOST_BANK,
        latencyMs: Long = 0,
        referenceNumber: String = "",
        merchantId: String = "",
        countryCode: String = "MY",
        networkType: String = "[ConnectivityResult.none]",
        customUrl: String? = null,
        customSecret: String? = null,
    ) {
        recordBreadcrumb(
            area = "HTTP_RESPONSE",
            process = process,
            message = "Host decline: statusCode=$statusCode msg=$statusMessage source=$source",
            level = "WARN"
        )

        // 1. Flush buffered outbound request WAL
        TelemetryDispatcher.flushBufferedRequest()

        // 2. Build decline details with attached breadcrumbs
        val crumbs = BreadcrumbRecorder.formatAsList()
        val detailsJson = buildJsonObject {
            put("statusCode", statusCode)
            put("statusMessage", statusMessage)
            put("source", source)
            put("latencyMs", latencyMs)
            if (crumbs.isNotEmpty()) {
                putJsonArray("breadcrumbs") {
                    for (c in crumbs) add(kotlinx.serialization.json.JsonPrimitive(c))
                }
            }
        }.toString()

        TelemetryDispatcher.dispatchAsync(
            process = process,
            details = detailsJson,
            type = TelemetryEventType.HOST_DECLINE,
            referenceNumber = referenceNumber,
            merchantId = merchantId,
            countryCode = countryCode,
            networkType = networkType,
            customUrl = customUrl,
            customSecret = customSecret,
        )
    }

    /**
     * Logs a terminal transaction outcome or unhandled exception.
     * User cancellations (USER_CANCELLED / 311) are suppressed from remote dispatch.
     */
    fun logTerminalOutcome(
        status: String,
        message: String,
        process: String = TelemetryProcess.ONLINE_PAYMENT_OUTCOME,
        source: String = TelemetryOrigin.INTERNAL_SDK,
        referenceNumber: String = "",
        merchantId: String = "",
        countryCode: String = "MY",
        networkType: String = "[ConnectivityResult.none]",
        customUrl: String? = null,
    ) {
        // Suppress intentional user cancellations
        if (status == "USER_CANCELLED" || status == "311") {
            recordBreadcrumb(
                area = "OUTCOME",
                process = process,
                message = "User cancelled payment manually",
                level = "INFO"
            )
            TelemetryDispatcher.discardBufferedRequest()
            return
        }

        // Pending ("22") is not a failure — e.g. an issued cash voucher not yet paid at the counter,
        // or a wallet still awaiting approval. Breadcrumb only, like a user cancel.
        if (status == "22") {
            recordBreadcrumb(
                area = "OUTCOME",
                process = process,
                message = "Payment pending: msg=$message source=$source",
                level = "INFO"
            )
            TelemetryDispatcher.discardBufferedRequest()
            return
        }

        // Happy path approved
        if (status == "00" || status == "SUCCESS") {
            logHostApproval(process = process, statusCode = status)
            return
        }

        // Anomaly / Terminal failure
        recordBreadcrumb(
            area = "OUTCOME",
            process = process,
            message = "Terminal outcome failed: status=$status msg=$message source=$source",
            level = "ERROR"
        )

        // Flush buffered request if still pending
        TelemetryDispatcher.flushBufferedRequest()

        val crumbs = BreadcrumbRecorder.formatAsList()
        val detailsJson = buildJsonObject {
            put("status", status)
            put("message", message)
            put("source", source)
            put("origin", source)
            if (crumbs.isNotEmpty()) {
                putJsonArray("breadcrumbs") {
                    for (c in crumbs) add(kotlinx.serialization.json.JsonPrimitive(c))
                }
            }
        }.toString()

        TelemetryDispatcher.dispatchAsync(
            process = process,
            details = detailsJson,
            type = TelemetryEventType.TERMINAL_FAILURE,
            referenceNumber = referenceNumber,
            merchantId = merchantId,
            countryCode = countryCode,
            networkType = networkType,
            customUrl = customUrl,
        )
    }

    /**
     * Manually triggers draining of offline queued logs from disk.
     */
    fun drainQueue() {
        TelemetryDispatcher.drainQueue()
    }

    /**
     * Clears recorded in-memory breadcrumbs.
     */
    fun clearBreadcrumbs() {
        BreadcrumbRecorder.clear()
    }

    /**
     * Test ping utility allowing developers and QA to verify telemetry connectivity.
     */
    @OptIn(kotlin.time.ExperimentalTime::class)
    fun testPing(
        merchantId: String = "",
        customUrl: String? = null,
    ) {
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        recordBreadcrumb("DIAGNOSTICS", "testPing", "Diagnostic ping requested")
        val details = buildJsonObject {
            put("action", "test_ping")
            put("timestamp", now)
            putJsonArray("breadcrumbs") {
                for (c in BreadcrumbRecorder.formatAsList()) add(kotlinx.serialization.json.JsonPrimitive(c))
            }
        }.toString()

        TelemetryDispatcher.dispatchAsync(
            process = "diagnostic_ping",
            details = details,
            type = TelemetryEventType.EVENT,
            referenceNumber = "TEST_$now",
            merchantId = merchantId,
            countryCode = "MY",
            networkType = "[ConnectivityResult.test]",
            customUrl = customUrl,
        )
    }
}
