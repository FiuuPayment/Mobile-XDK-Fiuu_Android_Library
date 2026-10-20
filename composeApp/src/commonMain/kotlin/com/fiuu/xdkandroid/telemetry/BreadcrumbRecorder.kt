package com.fiuu.xdkandroid.telemetry

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Thread-safe, bounded in-memory circular buffer for tracing intermediate checkout lifecycle steps.
 *
 * Capped at [MAX_ENTRIES] (20 entries) to ensure near-zero memory footprint (< 15 KB in RAM)
 * across budget mobile devices and multiplatform environments.
 */
object BreadcrumbRecorder {
    const val MAX_ENTRIES = 20

    data class Breadcrumb(
        val timestampMs: Long,
        val area: String,
        val process: String,
        val message: String,
        val level: String = "INFO",
    )

    private val buffer = MutableStateFlow<List<Breadcrumb>>(emptyList())

    /**
     * Records a lifecycle step in memory. Discards oldest entry if [MAX_ENTRIES] is reached.
     */
    @OptIn(ExperimentalTime::class)
    fun record(
        area: String,
        process: String,
        message: String,
        level: String = "INFO",
    ) {
        val entry = Breadcrumb(
            timestampMs = Clock.System.now().toEpochMilliseconds(),
            area = area,
            process = process,
            message = message,
            level = level,
        )
        buffer.update { current ->
            (current + entry).takeLast(MAX_ENTRIES)
        }
    }

    /**
     * Returns an immutable snapshot of current breadcrumbs.
     */
    fun snapshot(): List<Breadcrumb> = buffer.value

    /**
     * Formats current breadcrumbs into a human-readable chronological trace suitable for
     * plain-text diagnostic dumps.
     */
    @OptIn(ExperimentalTime::class)
    fun formatForDetails(): String {
        val entries = snapshot()
        if (entries.isEmpty()) return ""
        return buildString {
            append("\n--- Breadcrumbs (last ${entries.size}) ---")
            for (b in entries) {
                append("\n[")
                append(formatTimestamp(b.timestampMs))
                append("] [")
                append(b.area)
                append("] (")
                append(b.process)
                append(") ")
                append(b.message)
            }
        }
    }

    /**
     * Returns current breadcrumbs formatted as a list of chronological strings,
     * suitable for serialization into JSON arrays.
     */
    @OptIn(ExperimentalTime::class)
    fun formatAsList(): List<String> {
        val entries = snapshot()
        if (entries.isEmpty()) return emptyList()
        return entries.map { b ->
            "[${formatTimestamp(b.timestampMs)}] [${b.area}] (${b.process}) ${b.message}"
        }
    }

    /**
     * Clears all in-memory breadcrumbs.
     */
    fun clear() {
        buffer.value = emptyList()
    }

    @OptIn(ExperimentalTime::class)
    private fun formatTimestamp(timestampMs: Long): String {
        val instant = Instant.fromEpochMilliseconds(timestampMs)
        val dt = instant.toLocalDateTime(TimeZone.UTC)
        val h = dt.hour.toString().padStart(2, '0')
        val m = dt.minute.toString().padStart(2, '0')
        val s = dt.second.toString().padStart(2, '0')
        val ms = (dt.nanosecond / 1_000_000).toString().padStart(3, '0')
        return "$h:$m:$s.$ms"
    }
}
