package com.fiuu.xdkandroid.telemetry.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

@OptIn(kotlinx.cinterop.BetaInteropApi::class)
actual class PlatformQueueStorage actual constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalForeignApi::class)
    private fun getFilePath(): String {
        val fileManager = NSFileManager.defaultManager
        val appSupportPaths = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
        val baseDir = (appSupportPaths.firstOrNull() as? String)?.takeIf { it.isNotBlank() }
            ?: (NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).firstOrNull() as? String)
            ?: ""

        if (baseDir.isNotBlank() && !fileManager.fileExistsAtPath(baseDir)) {
            fileManager.createDirectoryAtPath(baseDir, true, null, null)
        }
        return if (baseDir.isNotBlank()) "$baseDir/$TELEMETRY_QUEUE_FILE_NAME" else TELEMETRY_QUEUE_FILE_NAME
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun enqueue(item: QueuedTelemetryItem) {
        try {
            val path = getFilePath()
            val list = readInternal(path).toMutableList()

            while (list.size >= MAX_TELEMETRY_QUEUE_ITEMS) {
                list.removeAt(0)
            }
            list.add(item)

            writeInternal(path, list)
        } catch (_: Throwable) {
        }
    }

    actual fun readAll(): List<QueuedTelemetryItem> {
        return try {
            val path = getFilePath()
            readInternal(path)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    actual fun writeAll(items: List<QueuedTelemetryItem>) {
        try {
            val path = getFilePath()
            val capped = if (items.size > MAX_TELEMETRY_QUEUE_ITEMS) items.takeLast(MAX_TELEMETRY_QUEUE_ITEMS) else items
            writeInternal(path, capped)
        } catch (_: Throwable) {
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun clear() {
        try {
            val path = getFilePath()
            val fileManager = NSFileManager.defaultManager
            if (fileManager.fileExistsAtPath(path)) {
                fileManager.removeItemAtPath(path, null)
            }
        } catch (_: Throwable) {
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun readInternal(filePath: String): List<QueuedTelemetryItem> {
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(filePath)) return emptyList()
        val text = NSString.stringWithContentsOfFile(filePath, NSUTF8StringEncoding, null) ?: return emptyList()
        if (text.isBlank()) return emptyList()
        return json.decodeFromString<List<QueuedTelemetryItem>>(text)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun writeInternal(filePath: String, items: List<QueuedTelemetryItem>) {
        val fileManager = NSFileManager.defaultManager
        if (items.isEmpty()) {
            if (fileManager.fileExistsAtPath(filePath)) {
                fileManager.removeItemAtPath(filePath, null)
            }
            return
        }
        val text = json.encodeToString(items)
        if (text.encodeToByteArray().size <= MAX_TELEMETRY_QUEUE_BYTES) {
            val nsStr = NSString.create(string = text)
            nsStr.writeToFile(filePath, true, NSUTF8StringEncoding, null)
        }
    }
}
