package com.fiuu.xdkandroid.logger

import androidx.compose.ui.graphics.Color
import com.fiuu.xdkandroid.theme.GlassTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    INIT, CRYPTO, HTTP, SSE, KEYSTORE, STATUS, ERROR, USER
}

data class LogEntry(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val details: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val breadcrumbs: List<String> = emptyList()
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))

    val hasDetails: Boolean
        get() = !details.isNullOrBlank() || !errorCode.isNullOrBlank() || breadcrumbs.isNotEmpty() || message.length > 120 || message.contains('\n')

    val color: Color
        get() = when (level) {
            LogLevel.INIT -> GlassTokens.LogCyan
            LogLevel.CRYPTO -> GlassTokens.LogPurple
            LogLevel.HTTP -> GlassTokens.LogCyan
            LogLevel.SSE -> GlassTokens.LogGreen
            LogLevel.KEYSTORE -> GlassTokens.LogBlue
            LogLevel.STATUS -> GlassTokens.LogGreen
            LogLevel.ERROR -> GlassTokens.LogRed
            LogLevel.USER -> GlassTokens.LogAmber
        }
}
