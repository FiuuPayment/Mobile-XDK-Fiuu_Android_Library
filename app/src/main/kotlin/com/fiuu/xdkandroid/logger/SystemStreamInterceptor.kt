package com.fiuu.xdkandroid.logger

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

// API responses are logged as one line of JSON, and their contents describe the transaction —
// "error_code" keys, a status 11 "Payment failed" — not an SDK failure. Only words outside the
// JSON count, plus an HTTP 4xx/5xx status (e.g. "Inquire API Status: 500").
private val JSON_BODY = Regex("\\{.*\\}")
private val HTTP_ERROR_STATUS = Regex("(\"statusCode\"\\s*:\\s*|API Status:\\s*)[45]\\d\\d\\b")

private fun looksLikeError(line: String): Boolean {
    if (HTTP_ERROR_STATUS.containsMatchIn(line)) return true
    val upper = line.replace(JSON_BODY, "").uppercase()
    return upper.contains("ERROR") || upper.contains("EXCEPTION") || upper.contains("FAILED")
}

/**
 * Intercepts System.out and System.err streams to capture internal diagnostic
 * logs emitted by the XDK Kotlin Multiplatform library (composeApp-release.aar)
 * via FiuuLogger.log() and standard JVM runtime errors.
 */
object SystemStreamInterceptor {

    private val isInstalled = AtomicBoolean(false)
    private var originalOut: PrintStream? = null
    private var originalErr: PrintStream? = null

    fun install() {
        if (!isInstalled.compareAndSet(false, true)) {
            return
        }

        originalOut = System.out
        originalErr = System.err

        val interceptorOut = PrintStream(StreamFilter(originalOut!!, isError = false), true)
        val interceptorErr = PrintStream(StreamFilter(originalErr!!, isError = true), true)

        System.setOut(interceptorOut)
        System.setErr(interceptorErr)
        // Plumbing notification silenced to prevent terminal startup noise
    }

    private class StreamFilter(
        private val downstream: OutputStream,
        private val isError: Boolean
    ) : OutputStream() {

        private val buffer = ByteArrayOutputStream(2048)
        private val isRouting = ThreadLocal.withInitial { false }

        override fun write(b: Int) {
            downstream.write(b)
            synchronized(buffer) {
                if (b == '\n'.code) {
                    flushLine()
                } else if (b != '\r'.code) {
                    buffer.write(b)
                }
            }
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            downstream.write(b, off, len)
            synchronized(buffer) {
                for (i in off until (off + len)) {
                    val ch = b[i].toInt()
                    if (ch == '\n'.code) {
                        flushLine()
                    } else if (ch != '\r'.code) {
                        buffer.write(ch)
                    }
                }
            }
        }

        override fun flush() {
            downstream.flush()
            synchronized(buffer) {
                // Do not prematurely slice mid-line buffers on stream flush unless exceeding 32KB
                if (buffer.size() > 32768) {
                    flushLine()
                }
            }
        }

        override fun close() {
            downstream.close()
            synchronized(buffer) {
                if (buffer.size() > 0) {
                    flushLine()
                }
            }
        }

        private fun flushLine() {
            val line = buffer.toString("UTF-8").trim()
            buffer.reset()
            if (line.isBlank()) return

            classifyAndRoute(line, isError)
        }

        private fun classifyAndRoute(line: String, isErr: Boolean) {
            // Guard against recursive interception
            if (isRouting.get() == true) return

            // Filter out non-actionable OS / framework theme warnings
            if (!LogRelevanceFilter.isRelevant(line)) {
                return
            }

            try {
                isRouting.set(true)

                if (isErr) {
                    // Only route actionable JVM errors
                    val upper = line.uppercase()
                    if (upper.contains("EXCEPTION") || upper.contains("ERROR") || upper.contains("FATAL") || upper.contains("FIUU")) {
                        val (displayMsg, detailsPayload) = summarizeOversizedPayload(line)
                        TerminalLogManager.log(LogLevel.ERROR, "STDERR", displayMsg, details = detailsPayload)
                    }
                    return
                }

                val upper = line.uppercase()

                val level: LogLevel
                val tag: String

                when {
                    looksLikeError(line) -> {
                        level = LogLevel.ERROR
                        tag = "SDK_ERROR"
                    }
                    upper.contains("--- REQUEST DEBUG INFO") ||
                    upper.contains("PAYMENT INIT RESPONSE") ||
                    upper.contains("PAYMENT CREATE RESPONSE") ||
                    upper.contains("PAYMENT EXPRESS RESPONSE") ||
                    upper.contains("PAYMENT INQUIRE RESPONSE") ||
                    upper.contains("GET AVAILABLE CHANNEL") ||
                    line.contains("https://") || line.contains("http://") -> {
                        level = LogLevel.HTTP
                        tag = "KTOR"
                    }
                    upper.contains("CHECKSUM") || upper.contains("SIGNATURE") || upper.contains("CRYPTOUTILS") -> {
                        level = LogLevel.CRYPTO
                        tag = "CRYPTO"
                    }
                    upper.contains("INQUIRE") || upper.contains("STREAM") || upper.contains("SSE") -> {
                        level = LogLevel.SSE
                        tag = "STREAM"
                    }
                    upper.contains("KEYSTORE") || upper.contains("ENCRYPTED SESSION") || upper.contains("SECURETOKENSTORE") -> {
                        level = LogLevel.KEYSTORE
                        tag = "KEYSTORE"
                    }
                    upper.contains("CLICKTOPAY") || upper.contains("C2P") || upper.contains("WEBVIEW") || upper.contains("REDIRECT") -> {
                        level = LogLevel.STATUS
                        tag = "WEBVIEW"
                    }
                    upper.contains("SAMPAY") || upper.contains("SAMSUNGPAYHANDLER") -> {
                        level = LogLevel.STATUS
                        tag = "SAMPAY"
                    }
                    upper.contains("GPAY") || upper.contains("GOOGLEPAY") -> {
                        level = LogLevel.STATUS
                        tag = "GPAY"
                    }
                    line.startsWith("DEBUG ") || upper.contains("FIUU") || upper.contains("XDK") -> {
                        level = LogLevel.STATUS
                        tag = "XDK_CORE"
                    }
                    else -> {
                        // Drop unclassified stdout noise (Strict Payment-Only Ingestion)
                        return
                    }
                }

                val (displayMsg, detailsPayload) = summarizeOversizedPayload(line)
                TerminalLogManager.log(level, tag, displayMsg, details = detailsPayload)
            } finally {
                isRouting.set(false)
            }
        }

        private fun summarizeOversizedPayload(line: String): Pair<String, String?> {
            val upper = line.uppercase()

            // 1. Channel Inquiry Response: summarize to channel names and count
            if (upper.contains("GET AVAILABLE CHANNEL") || upper.contains("GETAVAILABLECHANNELRESPONSEDTO")) {
                val channelMatcher = Pattern.compile("\\b(?:name|title)=([A-Za-z0-9_ -]+)", Pattern.CASE_INSENSITIVE).matcher(line)
                val channels = mutableListOf<String>()
                while (channelMatcher.find()) {
                    val name = channelMatcher.group(1)?.trim()
                    if (!name.isNullOrBlank() && name != "null" && !channels.contains(name)) {
                        channels.add(name)
                    }
                }
                if (channels.isNotEmpty()) {
                    val preview = channels.take(4).joinToString(", ")
                    val count = channels.size
                    val more = if (count > 4) " + ${count - 4} more" else ""
                    val summary = "GET AVAILABLE CHANNELS: $count channels ($preview$more)"

                    // Debloat redundant CDN image urls and null fields in details
                    val cleanedDetails = line
                        .replace(Regex("logoUrl[23]=https?://[^,)\\s]+[, ]?"), "")
                        .replace(Regex("(?:displayHPP|publicMaskName|id)=null[, ]?"), "")
                        .trim()

                    return Pair(summary, cleanedDetails)
                }
            }

            // 2. Multi-line or oversized HTTP Request/Response debug info (>180 chars)
            if (line.length > 180 || line.contains('\n')) {
                val firstLine = line.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: ""
                val cleanSummary = if (firstLine.length > 120) {
                    firstLine.take(120) + "... [${line.length} bytes]"
                } else {
                    "$firstLine... [${line.length} bytes]"
                }
                return Pair(cleanSummary, line)
            }

            return Pair(line, null)
        }
    }
}
