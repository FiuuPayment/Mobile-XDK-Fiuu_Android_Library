package com.fiuu.xdkandroid.telemetry

import com.fiuu.xdkandroid.data.remote.HttpClientFactory
import com.fiuu.xdkandroid.telemetry.model.ActivityLogMessage
import com.fiuu.xdkandroid.telemetry.model.ActivityLogRequest
import com.fiuu.xdkandroid.telemetry.model.TelemetryDataBlock
import com.fiuu.xdkandroid.telemetry.model.TelemetryEventType
import com.fiuu.xdkandroid.telemetry.model.TelemetryProductInfo
import com.fiuu.xdkandroid.telemetry.platform.PlatformQueueStorage
import com.fiuu.xdkandroid.telemetry.platform.QueuedTelemetryItem
import com.fiuu.xdkandroid.telemetry.platform.getAppBuildCode
import com.fiuu.xdkandroid.telemetry.platform.getPlatformDeviceInfo
import com.fiuu.xdkandroid.telemetry.platform.getPlatformName
import com.fiuu.xdkandroid.telemetry.platform.getPlatformSoftwareVersion
import com.fiuu.xdkandroid.utils.BuildInfo
import com.fiuu.xdkandroid.utils.FiuuLogger
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Asynchronous, non-blocking telemetry dispatcher for FiuuXDK.
 *
 * Implements:
 * 1. Write-Ahead Logging (WAL) buffering for outbound checkout requests.
 * 2. Anomaly-only flush triggering on payment decline, timeout, or crash.
 * 3. Fallback disk queue persistence (max 10 events) for offline cellular drops.
 * 4. Automatic offline queue drainage upon network restoration.
 */
object TelemetryDispatcher {

    const val DEFAULT_TELEMETRY_URL = "https://mobile.fiuu.com/api/log/activity"

    private val dispatcherScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val httpClient: HttpClient = HttpClientFactory().createTelemetryClient()
    private val queueStorage = PlatformQueueStorage()
    private val json = Json { ignoreUnknownKeys = true }

    private val isDraining = MutableStateFlow(false)

    data class BufferedOutboundRequest(
        val process: String,
        val details: String,
        val referenceNumber: String,
        val merchantId: String,
        val countryCode: String,
        val networkType: String,
        val customUrl: String? = null,
        val customSecret: String? = null,
        val timestampMs: Long = 0L,
    )

    private val pendingOutboundRequest = MutableStateFlow<BufferedOutboundRequest?>(null)

    /**
     * Buffers an outbound request (WAL) in RAM.
     * If the transaction succeeds (status "00"), this buffer is silently discarded.
     * If the transaction fails or times out, this buffer is flushed to CloudWatch.
     */
    @OptIn(ExperimentalTime::class)
    fun bufferOutboundRequest(
        process: String,
        details: String,
        referenceNumber: String,
        merchantId: String,
        countryCode: String,
        networkType: String,
        customUrl: String? = null,
        customSecret: String? = null,
    ) {
        val entry = BufferedOutboundRequest(
            process = process,
            details = details,
            referenceNumber = referenceNumber,
            merchantId = merchantId,
            countryCode = countryCode,
            networkType = networkType,
            customUrl = customUrl,
            customSecret = customSecret,
            timestampMs = Clock.System.now().toEpochMilliseconds(),
        )
        pendingOutboundRequest.value = entry
        if (FiuuLogger.enabled) {
            println("[FiuuTelemetry] [WAL] Buffered outbound request: $process (ref=$referenceNumber)")
        }
    }

    /**
     * Discards the buffered outbound request (called on approved transaction / "00").
     */
    fun discardBufferedRequest() {
        val discarded = pendingOutboundRequest.getAndUpdate { null }
        if (discarded != null && FiuuLogger.enabled) {
            println("[FiuuTelemetry] [WAL] Happy path: Discarded buffered request ${discarded.process}")
        }
    }

    /**
     * Flushes the buffered outbound request to the gateway (called immediately on decline/error).
     */
    fun flushBufferedRequest() {
        val buffered = pendingOutboundRequest.getAndUpdate { null } ?: return
        if (FiuuLogger.enabled) {
            println("[FiuuTelemetry] [WAL] Anomaly detected: Flushing buffered outbound request: ${buffered.process}")
        }
        dispatchAsync(
            process = buffered.process,
            details = buffered.details,
            type = TelemetryEventType.REQUEST,
            referenceNumber = buffered.referenceNumber,
            merchantId = buffered.merchantId,
            countryCode = buffered.countryCode,
            networkType = buffered.networkType,
            customUrl = buffered.customUrl,
            customSecret = buffered.customSecret,
        )
    }

    /**
     * Dispatches a telemetry event asynchronously. Never throws or blocks calling threads.
     */
    @OptIn(ExperimentalTime::class)
    fun dispatchAsync(
        process: String,
        details: String,
        type: String,
        referenceNumber: String,
        merchantId: String,
        countryCode: String,
        networkType: String,
        customUrl: String? = null,
        customSecret: String? = null,
    ) {
        dispatcherScope.launch {
            try {
                val targetUrl = customUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_TELEMETRY_URL
                val datetime = MobileGatewayChecksum.getCurrentUtcDateTime()
                val redactedDetails = TelemetryRedactor.redact(details)

                val dataBlock = TelemetryDataBlock(
                    deviceInfo = getPlatformDeviceInfo(),
                    productInfo = TelemetryProductInfo(
                        type = "XDK Native",
                        version = "1.0.0",
                        sdkVersion = BuildInfo.SDK_VERSION_NAME,
                    )
                )

                val messageObj = ActivityLogMessage(
                    referenceNumber = referenceNumber,
                    type = type,
                    process = process,
                    details = redactedDetails,
                    platform = getPlatformName(),
                    softwareVersion = getPlatformSoftwareVersion(),
                    networkType = networkType,
                    country = countryCode.ifBlank { "MY" },
                    build = getAppBuildCode(),
                    merchantId = merchantId,
                )

                val secret = MobileGatewayChecksum.resolveGatewaySecret(targetUrl, customSecret)
                val checksum = runCatching {
                    MobileGatewayChecksum.generateChecksum(secret, datetime, dataBlock, messageObj)
                }.getOrElse { "" }

                val requestPayload = ActivityLogRequest(
                    data = dataBlock,
                    message = messageObj,
                    checksum = checksum,
                    datetime = datetime,
                )

                val jsonBody = json.encodeToString(requestPayload)

                if (FiuuLogger.enabled) {
                    println("[FiuuTelemetry] [Dispatch] Preparing $process ($type) -> $targetUrl (checksum=${checksum.take(12)}...)")
                }

                val success = postHttp(targetUrl, jsonBody, process, type)
                if (!success) {
                    if (FiuuLogger.enabled) {
                        println("[FiuuTelemetry] [Queue] Network dispatch failed. Persisting to offline queue.")
                    }
                    queueStorage.enqueue(QueuedTelemetryItem(targetUrl, jsonBody, Clock.System.now().toEpochMilliseconds()))
                } else {
                    drainQueueInternal()
                }
            } catch (t: Throwable) {
                if (FiuuLogger.enabled) {
                    println("[FiuuTelemetry] [Error] Telemetry dispatch exception: ${t.message}")
                }
            }
        }
    }

    /**
     * Drains the offline retry queue asynchronously.
     */
    fun drainQueue() {
        dispatcherScope.launch {
            drainQueueInternal()
        }
    }

    private suspend fun postHttp(apiUrl: String, jsonBody: String, process: String = "", type: String = ""): Boolean {
        return try {
            val response = httpClient.post(apiUrl) {
                contentType(ContentType.Application.Json)
                header("Accept", "application/json")
                setBody(jsonBody)
            }
            val ok = response.status.isSuccess()
            if (FiuuLogger.enabled) {
                println("[FiuuTelemetry] [HTTP] $process ($type) -> Status: ${response.status.value}")
            }
            ok
        } catch (t: Throwable) {
            if (FiuuLogger.enabled) {
                println("[FiuuTelemetry] [HTTP] $process ($type) -> Failed with exception: ${t.message}")
            }
            false
        }
    }

    private suspend fun drainQueueInternal() {
        if (!isDraining.compareAndSet(expect = false, update = true)) return
        try {
            val items = queueStorage.readAll()
            if (items.isEmpty()) return

            if (FiuuLogger.enabled) {
                println("[FiuuTelemetry] [Queue] Draining ${items.size} queued items from disk...")
            }

            val remaining = mutableListOf<QueuedTelemetryItem>()
            var networkHealthy = true

            for (item in items) {
                if (networkHealthy) {
                    val ok = postHttp(item.url, item.body, "offlineQueueDrain", "retry")
                    if (!ok) {
                        networkHealthy = false
                        remaining.add(item)
                    }
                } else {
                    remaining.add(item)
                }
            }

            if (remaining.isEmpty()) {
                queueStorage.clear()
                if (FiuuLogger.enabled) {
                    println("[FiuuTelemetry] [Queue] Offline queue drained completely.")
                }
            } else {
                queueStorage.writeAll(remaining)
                if (FiuuLogger.enabled) {
                    println("[FiuuTelemetry] [Queue] Partial drain: ${remaining.size} items remaining in queue.")
                }
            }
        } catch (_: Throwable) {
        } finally {
            isDraining.value = false
        }
    }
}
