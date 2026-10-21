package com.fiuu.xdkandroid.logger

import android.content.Context
import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Background logcat process reader that captures native Android logcat calls
 * (Log.d, Log.e) emitted by XDK internal modules (FIUU_WEBVIEW, AndroidSecureTokenStore)
 * and unhandled system errors filtered strictly to the current app process PID.
 */
object LogcatStreamReader {

    private val isRunning = AtomicBoolean(false)
    private var job: Job? = null
    private var logcatProcess: java.lang.Process? = null

    // Regex detecting logcat stdout/stderr lines with optional priority tag (e.g. I/System.out:, W/System.err:)
    private val SYSTEM_STREAM_REGEX = Regex("^(?:[VDIWEF]/)?System\\.(?:out|err):?", RegexOption.IGNORE_CASE)

    fun start(context: Context) {
        if (!isRunning.compareAndSet(false, true)) {
            return
        }

        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                val myPid = Process.myPid()

                // Stream logcat tailing only new lines (-T 1) filtered to myPid and relevant payment tags
                val cmd = arrayOf(
                    "logcat",
                    "-v", "tag",
                    "-T", "1",
                    "--pid=$myPid",
                    "-s",
                    "FIUU_WEBVIEW:V",
                    "AndroidSecureTokenStore:V",
                    "XDK:V",
                    "ProvideFiuuPaymentSDK:V",
                    "FiuuPayment:V",
                    "Fiuu:V",
                    "FiuuLogger:V"
                )

                val process = Runtime.getRuntime().exec(cmd)
                logcatProcess = process

                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    while (isActive) {
                        val line = reader.readLine() ?: break
                        val trimmed = line.trim()
                        if (trimmed.isBlank()) continue

                        // Skip System.out/err lines since SystemStreamInterceptor captures them directly
                        if (SYSTEM_STREAM_REGEX.containsMatchIn(trimmed)) {
                            continue
                        }

                        // Filter out non-actionable OS / framework theme warnings
                        if (!LogRelevanceFilter.isRelevant(trimmed)) {
                            continue
                        }

                        routeLogcatLine(trimmed)
                    }
                }
            } catch (e: Exception) {
                TerminalLogManager.log(
                    LogLevel.ERROR,
                    "LOGGER",
                    "LogcatStreamReader terminated: ${e.localizedMessage}"
                )
            } finally {
                isRunning.set(false)
            }
        }
    }

    fun stop() {
        isRunning.set(false)
        job?.cancel()
        logcatProcess?.destroy()
        logcatProcess = null
    }

    private fun routeLogcatLine(line: String) {
        val colonIndex = line.indexOf(':')
        val header = if (colonIndex > 0) line.substring(0, colonIndex).trim() else ""
        val body = if (colonIndex > 0 && colonIndex + 1 < line.length) {
            line.substring(colonIndex + 1).trim()
        } else {
            line
        }

        val level: LogLevel
        val tag: String

        when {
            header.contains("FIUU_WEBVIEW") -> {
                level = if (body.contains("error", ignoreCase = true) || body.contains("failure", ignoreCase = true)) {
                    LogLevel.ERROR
                } else {
                    LogLevel.STATUS
                }
                tag = "WEBVIEW"
            }
            header.contains("AndroidSecureTokenStore") -> {
                level = LogLevel.KEYSTORE
                tag = "KEYSTORE"
            }
            header.contains("XDK") -> {
                level = LogLevel.STATUS
                tag = "XDK_NATIVE"
            }
            header.startsWith("E/") || body.startsWith("E/") -> {
                level = LogLevel.ERROR
                tag = header.substringAfter('/').ifBlank { "SYSTEM_ERR" }
            }
            else -> {
                // Remaining allowlisted tags (-s filter above) — keep them, labelled by their
                // own logcat tag. Headerless lines (e.g. "--------- beginning of main") are noise.
                if (header.isEmpty()) return
                level = LogLevel.STATUS
                tag = header.substringAfter('/').ifBlank { header }
            }
        }

        TerminalLogManager.log(level, tag, body)
    }
}
