package com.fiuu.xdkandroid.logger

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.ArrayDeque

enum class LogFilter {
    ALL, HTTP, SSE, CRYPTO, KEYSTORE, STATUS, ERROR;

    fun matches(entry: LogEntry): Boolean = when (this) {
        ALL -> true
        HTTP -> entry.level == LogLevel.HTTP
        SSE -> entry.level == LogLevel.SSE
        CRYPTO -> entry.level == LogLevel.CRYPTO
        KEYSTORE -> entry.level == LogLevel.KEYSTORE
        STATUS -> entry.level == LogLevel.STATUS
        ERROR -> entry.level == LogLevel.ERROR
    }
}

object TerminalLogManager {
    private const val MAX_LOGS = 500
    private const val MAX_BREADCRUMBS = 10

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _activeFilter = MutableStateFlow(LogFilter.ALL)
    val activeFilter: StateFlow<LogFilter> = _activeFilter.asStateFlow()

    // Thread-safe FIFO breadcrumb queue tracking sequential pre-event history
    private val _breadcrumbsQueue = ArrayDeque<String>(MAX_BREADCRUMBS)

    init {
        log(LogLevel.INIT, "STUDIO", "XDK Developer Test Studio initialized")
    }

    fun setFilter(filter: LogFilter) {
        _activeFilter.value = filter
    }

    fun log(
        level: LogLevel,
        tag: String,
        message: String,
        details: String? = null,
        errorCode: String? = null,
        errorMessage: String? = null
    ) {
        val sanitizedMessage = LogSanitizer.sanitize(message)
        val sanitizedDetails = details?.let { LogSanitizer.sanitize(it) }

        // Track breadcrumb entry
        val breadcrumbItem = "[$tag] ${sanitizedMessage.take(100)}"
        synchronized(_breadcrumbsQueue) {
            if (_breadcrumbsQueue.size >= MAX_BREADCRUMBS) {
                _breadcrumbsQueue.removeFirst()
            }
            _breadcrumbsQueue.add(breadcrumbItem)
        }

        // Attach pre-event breadcrumbs snapshot for high-value logs (errors, milestones, HTTP/Crypto)
        val shouldAttachBreadcrumbs = level in listOf(
            LogLevel.ERROR,
            LogLevel.STATUS,
            LogLevel.HTTP,
            LogLevel.CRYPTO,
            LogLevel.USER
        ) || !sanitizedDetails.isNullOrBlank()

        val breadcrumbsSnapshot = if (shouldAttachBreadcrumbs) {
            synchronized(_breadcrumbsQueue) { _breadcrumbsQueue.toList() }
        } else {
            emptyList()
        }

        // Extract error code and message if not explicitly provided — ERROR entries only, so a
        // STATUS line like "Status: 22" isn't shown as a failure code.
        val resolvedErrorCode = errorCode ?: if (level != LogLevel.ERROR) null
            else LogRelevanceFilter.extractErrorCode(sanitizedMessage)
                ?: sanitizedDetails?.let { LogRelevanceFilter.extractErrorCode(it) }

        val resolvedErrorMessage = errorMessage ?: LogRelevanceFilter.extractErrorMessage(sanitizedMessage)
            ?: sanitizedDetails?.let { LogRelevanceFilter.extractErrorMessage(it) }

        val entry = LogEntry(
            level = level,
            tag = tag,
            message = sanitizedMessage,
            details = sanitizedDetails,
            errorCode = resolvedErrorCode,
            errorMessage = resolvedErrorMessage,
            breadcrumbs = breadcrumbsSnapshot
        )

        _logs.update { current ->
            val updated = current + entry
            if (updated.size > MAX_LOGS) updated.takeLast(MAX_LOGS) else updated
        }
    }

    fun clear() {
        _logs.value = emptyList()
        synchronized(_breadcrumbsQueue) {
            _breadcrumbsQueue.clear()
        }
        log(LogLevel.USER, "CONSOLE", "Terminal logs cleared")
    }

    fun getBreadcrumbs(count: Int = 10): List<LogEntry> {
        val current = _logs.value
        return if (current.size > count) current.takeLast(count) else current
    }

    fun getBreadcrumbStrings(count: Int = 30): List<String> {
        val snapshot = synchronized(_breadcrumbsQueue) { _breadcrumbsQueue.toList() }
        return if (snapshot.size > count) snapshot.takeLast(count) else snapshot
    }

    fun exportPlainText(): String {
        return _logs.value.joinToString("\n") { entry ->
            val detailSuffix = entry.details?.let { "\n    └─ $it" } ?: ""
            val errorSuffix = entry.errorCode?.let { " [Code: $it]" } ?: ""
            "${entry.formattedTime} [${entry.level}] [${entry.tag}]${errorSuffix} ${entry.message}$detailSuffix"
        }
    }

    fun exportDiagnosticReport(): String {
        val sb = StringBuilder()
        sb.append("=====================================================\n")
        sb.append("         FIUU ONLINE XDK DIAGNOSTIC REPORT           \n")
        sb.append("=====================================================\n")
        sb.append("Generated At: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
        sb.append("Total Log Entries: ${_logs.value.size}\n")
        sb.append("Active Filter: ${_activeFilter.value.name}\n")
        sb.append("Pre-Event Breadcrumbs Registered: ${_breadcrumbsQueue.size}\n")
        sb.append("-----------------------------------------------------\n")
        sb.append(exportPlainText())
        sb.append("\n=====================================================\n")
        return sb.toString()
    }
}
