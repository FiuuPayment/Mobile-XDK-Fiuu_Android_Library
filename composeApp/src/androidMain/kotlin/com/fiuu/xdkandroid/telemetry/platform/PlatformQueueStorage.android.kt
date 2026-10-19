package com.fiuu.xdkandroid.telemetry.platform

import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

actual class PlatformQueueStorage actual constructor() {

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()

    private fun getFile(): File {
        val ctx = AndroidTelemetryContext.contextRef.get()
        val dir = ctx?.filesDir ?: File(System.getProperty("java.io.tmpdir") ?: ".")
        return File(dir, TELEMETRY_QUEUE_FILE_NAME)
    }

    actual fun enqueue(item: QueuedTelemetryItem) {
        synchronized(lock) {
            try {
                val file = getFile()
                val list = readInternal(file).toMutableList()

                while (list.size >= MAX_TELEMETRY_QUEUE_ITEMS) {
                    list.removeAt(0)
                }
                list.add(item)

                writeInternal(file, list)
            } catch (_: Throwable) {
            }
        }
    }

    actual fun readAll(): List<QueuedTelemetryItem> {
        synchronized(lock) {
            return try {
                val file = getFile()
                readInternal(file)
            } catch (_: Throwable) {
                emptyList()
            }
        }
    }

    actual fun writeAll(items: List<QueuedTelemetryItem>) {
        synchronized(lock) {
            try {
                val file = getFile()
                val capped = if (items.size > MAX_TELEMETRY_QUEUE_ITEMS) items.takeLast(MAX_TELEMETRY_QUEUE_ITEMS) else items
                writeInternal(file, capped)
            } catch (_: Throwable) {
            }
        }
    }

    actual fun clear() {
        synchronized(lock) {
            try {
                val file = getFile()
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun readInternal(file: File): List<QueuedTelemetryItem> {
        if (!file.exists()) return emptyList()
        val text = file.readText()
        if (text.isBlank()) return emptyList()
        return json.decodeFromString<List<QueuedTelemetryItem>>(text)
    }

    private fun writeInternal(file: File, items: List<QueuedTelemetryItem>) {
        if (items.isEmpty()) {
            if (file.exists()) file.delete()
            return
        }
        val text = json.encodeToString(items)
        if (text.encodeToByteArray().size <= MAX_TELEMETRY_QUEUE_BYTES) {
            file.writeText(text)
        }
    }
}
