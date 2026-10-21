package com.fiuu.xdkandroid.logger

import java.util.regex.Pattern

/**
 * Filter that detects and eliminates non-actionable Android OS framework spam
 * (e.g. Google Play Services dynamite theme warnings, AppCompat widget inflation errors,
 * OpenGL/Skia logs, window dispatcher and insets noise) while preserving legitimate payment SDK events and errors.
 */
object LogRelevanceFilter {

    private val SPAM_PATTERNS = listOf(
        Pattern.compile("is an AppCompat widget that can only be used with a Theme\\.AppCompat", Pattern.CASE_INSENSITIVE),
        Pattern.compile("android\\.support\\.v7\\.widget\\.AppCompat", Pattern.CASE_INSENSITIVE),
        Pattern.compile("com\\.google\\.android\\.gms\\.wallet\\.dynamite", Pattern.CASE_INSENSITIVE),
        Pattern.compile("DynamiteModule: Local module descriptor", Pattern.CASE_INSENSITIVE),
        Pattern.compile("GooglePlayServicesUtil: Google Play Store is available", Pattern.CASE_INSENSITIVE),
        Pattern.compile("View class .* is an AppCompat widget", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Theme\\.AppCompat theme \\(or descendant\\)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("OpenGLRenderer", Pattern.CASE_INSENSITIVE),
        Pattern.compile("D/skia", Pattern.CASE_INSENSITIVE),
        Pattern.compile("I/chatty", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Choreographer.*Skipped.*frames", Pattern.CASE_INSENSITIVE),
        Pattern.compile("SurfaceView.*checkInputEventForConsistency", Pattern.CASE_INSENSITIVE),
        Pattern.compile("InsetsController.*show.*failed", Pattern.CASE_INSENSITIVE),
        Pattern.compile("OnBackInvokedCallback is not enabled", Pattern.CASE_INSENSITIVE),
        Pattern.compile("WindowOnBackDispatcher", Pattern.CASE_INSENSITIVE),
        Pattern.compile("ViewRootImpl", Pattern.CASE_INSENSITIVE),
        Pattern.compile("InputTransport", Pattern.CASE_INSENSITIVE),
        Pattern.compile("BLASTBufferQueue", Pattern.CASE_INSENSITIVE),
        Pattern.compile("BufferQueueProducer", Pattern.CASE_INSENSITIVE),
        Pattern.compile("RenderThread", Pattern.CASE_INSENSITIVE),
        Pattern.compile("hwui", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Gralloc", Pattern.CASE_INSENSITIVE),
        Pattern.compile("EGL_emulation", Pattern.CASE_INSENSITIVE),
        Pattern.compile("SystemStreamInterceptor active", Pattern.CASE_INSENSITIVE)
    )

    private val ERROR_CODE_PATTERNS = listOf(
        Pattern.compile("\"error_code\"\\s*:\\s*\"([A-Za-z0-9_-]+)\"", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\"errorCode\"\\s*:\\s*\"([A-Za-z0-9_-]+)\"", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Status:\\s*([A-Za-z0-9_-]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("error_code=([A-Za-z0-9_-]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("errorCode=([A-Za-z0-9_-]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(M[0-9]{5}|E[0-9]{2,3})\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(?:ERR|ERROR)[-_:]\\s*(?!(?:code|description|message|msg)\\b)([A-Za-z0-9_-]+)\\b", Pattern.CASE_INSENSITIVE)
    )

    private val ERROR_MESSAGE_PATTERNS = listOf(
        Pattern.compile("\"error_message\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\"errorMessage\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE),
        Pattern.compile("error_message=([^,\\}]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("errorMessage=([^,\\}]+)", Pattern.CASE_INSENSITIVE)
    )

    /**
     * Evaluates whether a logcat line or intercepted stream line is relevant to the app.
     * Returns false if the line matches any known non-actionable OS framework warning.
     */
    fun isRelevant(line: String): Boolean {
        if (line.isBlank()) return false
        for (pattern in SPAM_PATTERNS) {
            if (pattern.matcher(line).find()) {
                return false
            }
        }
        return true
    }

    /**
     * Attempts to extract a structured error code (e.g. M00008, 404, E06) from a message or payload.
     */
    fun extractErrorCode(text: String): String? {
        for (pattern in ERROR_CODE_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val code = matcher.group(1)?.trim()
                if (!code.isNullOrBlank() && code.length in 2..20) {
                    return code
                }
            }
        }
        return null
    }

    /**
     * Attempts to extract a human-readable error description from a payload.
     */
    fun extractErrorMessage(text: String): String? {
        for (pattern in ERROR_MESSAGE_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val msg = matcher.group(1)?.trim()
                if (!msg.isNullOrBlank()) {
                    return msg
                }
            }
        }
        return null
    }
}
