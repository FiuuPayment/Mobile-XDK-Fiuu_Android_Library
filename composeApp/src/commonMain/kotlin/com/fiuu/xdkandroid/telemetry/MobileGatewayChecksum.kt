package com.fiuu.xdkandroid.telemetry

import com.fiuu.xdkandroid.telemetry.model.ActivityLogData
import com.fiuu.xdkandroid.telemetry.model.ActivityLogMessage
import com.fiuu.xdkandroid.utils.localeCompareBase
import korlibs.crypto.SHA512
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Calculates Mobile Gateway compatible SHA-512 checksums and UTC timestamps
 * for activity log payloads dispatched to https://mobile.fiuu.com/api/log/activity.
 *
 * Algorithm matches the Mobile Gateway & TapSDK specification:
 * 1. Formats datetime as 14-character UTC timestamp (yyyyMMddHHmmss).
 * 2. Flattens payload primitive fields and appends the datetime string.
 * 3. Sorts flattened elements (numbers first ascending, strings via Unicode primary collation).
 * 4. Generates SHA-512 hash over (secret + sortedString).
 */
object MobileGatewayChecksum {

    private val P_BYTES = byteArrayOf(
        25, 110, 56, 57, 29, 56, 23, 11, 25, 2, 99, 51, 105, 107, 46, 27,
        20, 110, 50, 2, 61, 52, 15, 54, 52, 50, 106, 98, 61, 40, 35, 62
    )
    private val U_BYTES = byteArrayOf(27, 56, 57, 30, 105, 110, 107, 107)

    private fun decodeKey(bytes: ByteArray): String {
        val out = ByteArray(bytes.size)
        for (i in bytes.indices) {
            out[i] = (bytes[i].toInt() xor 0x5A).toByte()
        }
        return out.decodeToString()
    }

    val PROD_GATEWAY_SECRET: String
        get() = decodeKey(P_BYTES)

    val UAT_GATEWAY_SECRET: String
        get() = decodeKey(U_BYTES)

    private class LeafItem(
        val isNumeric: Boolean,
        val numericValue: Double = 0.0,
        val stringValue: String = ""
    )

    private fun formatNumber(d: Double): String {
        val asLong = d.toLong()
        return if (d == asLong.toDouble()) asLong.toString() else d.toString()
    }

    /**
     * Returns current UTC datetime formatted as 14-digit yyyyMMddHHmmss.
     */
    @OptIn(ExperimentalTime::class)
    fun getCurrentUtcDateTime(): String {
        val instant = Clock.System.now()
        val dt = instant.toLocalDateTime(TimeZone.UTC)
        val year = dt.year.toString().padStart(4, '0')
        val month = dt.monthNumber.toString().padStart(2, '0')
        val day = dt.dayOfMonth.toString().padStart(2, '0')
        val hour = dt.hour.toString().padStart(2, '0')
        val minute = dt.minute.toString().padStart(2, '0')
        val second = dt.second.toString().padStart(2, '0')
        return "$year$month$day$hour$minute$second"
    }

    /**
     * Resolves the gateway secret based on target URL or custom override.
     */
    fun resolveGatewaySecret(targetUrl: String, customSecret: String? = null): String {
        if (!customSecret.isNullOrBlank()) {
            return customSecret.trim()
        }
        return if (targetUrl.contains("uat", ignoreCase = true) || targetUrl.contains("sandbox", ignoreCase = true)) {
            UAT_GATEWAY_SECRET
        } else {
            PROD_GATEWAY_SECRET
        }
    }

    /**
     * Generates SHA-512 checksum for the given activity log data and message.
     */
    fun generateChecksum(
        secret: String,
        datetime: String,
        data: ActivityLogData,
        message: ActivityLogMessage
    ): String {
        val json = Json { ignoreUnknownKeys = true }
        val payloadMap = buildMap<String, JsonElement> {
            put("data", json.encodeToJsonElement(data))
            put("message", json.encodeToJsonElement(message))
        }
        val jsonTree = JsonObject(payloadMap)
        return generateChecksumFromJson(secret, datetime, jsonTree)
    }

    /**
     * Generates checksum from an arbitrary JSON element tree.
     */
    fun generateChecksumFromJson(
        secret: String,
        datetime: String,
        element: JsonElement
    ): String {
        val items = mutableListOf<LeafItem>()
        items.add(LeafItem(isNumeric = false, stringValue = datetime))
        collectPrimitives(element, items)

        items.sortWith(Comparator { a, b ->
            when {
                a.isNumeric && b.isNumeric -> a.numericValue.compareTo(b.numericValue)
                a.isNumeric -> -1
                b.isNumeric -> 1
                else -> localeCompareBase(a.stringValue, b.stringValue)
            }
        })

        val sorted = buildString {
            for (item in items) {
                append(if (item.isNumeric) formatNumber(item.numericValue) else item.stringValue)
            }
        }

        val rawInput = secret + sorted
        return SHA512.digest(rawInput.encodeToByteArray()).hexLower
    }

    private fun collectPrimitives(element: JsonElement?, items: MutableList<LeafItem>) {
        if (element == null) return
        when (element) {
            is JsonObject -> {
                for ((key, value) in element.entries) {
                    if (key == "checksum" || key == "datetime") continue
                    collectPrimitives(value, items)
                }
            }
            is JsonArray -> {
                for (child in element) {
                    collectPrimitives(child, items)
                }
            }
            is JsonPrimitive -> {
                val num = if (!element.isString) element.content.toDoubleOrNull() else null
                if (num != null) {
                    items.add(LeafItem(isNumeric = true, numericValue = num))
                } else {
                    items.add(LeafItem(isNumeric = false, stringValue = element.content))
                }
            }
        }
    }
}
