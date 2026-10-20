package com.fiuu.xdkandroid.telemetry.platform

import kotlinx.serialization.Serializable

const val MAX_TELEMETRY_QUEUE_ITEMS: Int = 10
const val MAX_TELEMETRY_QUEUE_BYTES: Int = 32 * 1024
const val TELEMETRY_QUEUE_FILE_NAME: String = "fiuu_telemetry_queue.json"

@Serializable
data class QueuedTelemetryItem(
    val url: String,
    val body: String,
    val timestampMs: Long
)

/**
 * Platform-specific persistent disk storage for offline telemetry retry queue.
 * Strictly bounded to [MAX_TELEMETRY_QUEUE_ITEMS] (10 events) and [MAX_TELEMETRY_QUEUE_BYTES] (32 KB).
 */
expect class PlatformQueueStorage() {
    fun enqueue(item: QueuedTelemetryItem)
    fun readAll(): List<QueuedTelemetryItem>
    fun writeAll(items: List<QueuedTelemetryItem>)
    fun clear()
}
