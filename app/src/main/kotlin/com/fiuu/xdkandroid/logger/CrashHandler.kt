package com.fiuu.xdkandroid.logger

import android.content.Context
import android.content.SharedPreferences
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 2-Tier Flight Recorder Crash Handler.
 * Captures unhandled thread crashes and saves a snapshot along with preceding
 * breadcrumbs to SharedPreferences, surfacing the crash diagnostics in the live terminal
 * upon the next app cold boot.
 */
class CrashHandler private constructor(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()

            val breadcrumbs = TerminalLogManager.getBreadcrumbs(10).joinToString("\n") { entry ->
                "${entry.formattedTime} [${entry.level}] [${entry.tag}] ${entry.message}"
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val exceptionTitle = "${throwable::class.java.simpleName}: ${throwable.message ?: "No message"}"

            // Synchronous commit to guarantee persistence before process death
            prefs.edit()
                .putString(KEY_CRASH_TIMESTAMP, timestamp)
                .putString(KEY_CRASH_TITLE, exceptionTitle)
                .putString(KEY_CRASH_STACK, stackTrace)
                .putString(KEY_CRASH_BREADCRUMBS, breadcrumbs)
                .putBoolean(KEY_HAS_CRASH, true)
                .commit()

            TerminalLogManager.log(
                LogLevel.ERROR,
                "FATAL_CRASH",
                exceptionTitle,
                details = stackTrace
            )
        } catch (_: Exception) {
            // Failsafe - do not prevent crash delivery to default handler
        } finally {
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun checkAndReportPreviousCrash() {
        if (!prefs.getBoolean(KEY_HAS_CRASH, false)) return

        val timestamp = prefs.getString(KEY_CRASH_TIMESTAMP, "Unknown")
        val title = prefs.getString(KEY_CRASH_TITLE, "Unknown Exception")
        val stack = prefs.getString(KEY_CRASH_STACK, "")
        val breadcrumbs = prefs.getString(KEY_CRASH_BREADCRUMBS, "")

        val detailReport = buildString {
            append("Crash Timestamp: ").append(timestamp).append("\n\n")
            append("--- PRE-CRASH USER BREADCRUMBS ---\n")
            append(if (breadcrumbs.isNullOrBlank()) "(No breadcrumbs recorded)" else breadcrumbs)
            append("\n\n--- EXCEPTION STACK TRACE ---\n")
            append(stack)
        }

        TerminalLogManager.log(
            level = LogLevel.ERROR,
            tag = "CRASH_RECORDER",
            message = "PREVIOUS FLIGHT RECORDER CRASH: $title",
            details = detailReport
        )

        // Clear previous crash to avoid duplicate alerts
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "xdk_flight_recorder_crash_prefs"
        private const val KEY_HAS_CRASH = "key_has_crash"
        private const val KEY_CRASH_TIMESTAMP = "key_crash_timestamp"
        private const val KEY_CRASH_TITLE = "key_crash_title"
        private const val KEY_CRASH_STACK = "key_crash_stack"
        private const val KEY_CRASH_BREADCRUMBS = "key_crash_breadcrumbs"

        fun install(context: Context): CrashHandler {
            val handler = CrashHandler(context.applicationContext)
            Thread.setDefaultUncaughtExceptionHandler(handler)
            handler.checkAndReportPreviousCrash()
            return handler
        }
    }
}
