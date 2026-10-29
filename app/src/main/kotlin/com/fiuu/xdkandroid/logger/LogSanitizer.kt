package com.fiuu.xdkandroid.logger

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * PCI-DSS compliant log sanitizer.
 * Automatically masks Primary Account Numbers (PANs), CVVs, passwords, security PINs,
 * merchant verification keys, authorization tokens, and customer PII before logs reach
 * the in-app terminal or storage.
 */
object LogSanitizer {

    // Brand-agnostic PAN candidate detector: any 13-19 digit run, so every network/IIN range is covered
    // (Mastercard 2-series, UnionPay, Maestro, Discover 644-649, 19-digit PANs, ...) without an issuer allowlist.
    // Supports contiguous digits and space/hyphen-separated 4-4-4-4(-3) and 4-6-4/5 groupings.
    private val PAN_PATTERN = Pattern.compile(
        "\\b(?:[0-9]{13,19}|[0-9]{4}(?:[\\s-][0-9]{4}){2}[\\s-][0-9]{1,4}(?:[\\s-][0-9]{1,3})?|[0-9]{4}[\\s-][0-9]{6}[\\s-][0-9]{4,5})\\b"
    )

    // Complete redaction keys (passwords, cvvs, tokens, secrets)
    private val FULL_REDACTION_KEYS = listOf(
        "password",
        "mp_password",
        "merchant_password",
        "merchantPassword",
        "cvv",
        "cvc",
        "xp_credit_card_cvv",
        "creditCardCvv",
        "pin",
        "passcode",
        "accessToken",
        "cardToken",
        "token",
        "auth_token",
        "secret",
        "client_secret",
        "xp_googlepay",
        "xp_samsungpay",
        "xp_applepay",
        "xp_click2pay"
    )

    // Hint-preserving keys (verification keys)
    private val HINT_KEYS = listOf(
        "verificationKey",
        "mp_verification_key",
        "merchant_vkey",
        "vkey"
    )

    // Customer PII keys
    private val PII_KEYS = listOf(
        "cust_email",
        "mp_cust_email",
        "custEmail",
        "email",
        "cust_phone",
        "mp_cust_phone",
        "custPhone",
        "phone"
    )

    private val ALL_SENSITIVE_KEYS = FULL_REDACTION_KEYS + HINT_KEYS + PII_KEYS

    // Regex pattern matching JSON-style "key": "value" or "key": 123
    private val JSON_KEY_VALUE_PATTERN = Pattern.compile(
        "\"(" + ALL_SENSITIVE_KEYS.joinToString("|") + ")\"\\s*:\\s*(?:\"((?:[^\"\\\\]|\\\\.)*)\"|([0-9a-zA-Z._-]+))",
        Pattern.CASE_INSENSITIVE
    )

    // Regex pattern matching query parameter or plain key-value: key=value
    private val QUERY_PARAM_PATTERN = Pattern.compile(
        "\\b(" + ALL_SENSITIVE_KEYS.joinToString("|") + ")=([^&\\s,]+)",
        Pattern.CASE_INSENSITIVE
    )

    // Regex matching Authorization Bearer tokens in headers
    private val AUTH_BEARER_PATTERN = Pattern.compile(
        "(Authorization\\s*:\\s*Bearer\\s+)[^\\s\\r\\n,]+",
        Pattern.CASE_INSENSITIVE
    )

    // Regex matching standalone email addresses not preceded by asterisks
    private val STANDALONE_EMAIL_PATTERN = Pattern.compile(
        "(?<![\\*A-Za-z0-9._%+-])([A-Za-z0-9._%+-]+)@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})\\b"
    )

    /**
     * Sanitizes input text by masking credit card PANs, redacting credentials, and masking PII.
     */
    fun sanitize(input: String?): String {
        if (input.isNullOrEmpty()) return ""

        var result: String = input

        // 1. Mask Credit Card PANs (retaining first 4 and last 4 digits)
        val panMatcher = PAN_PATTERN.matcher(result)
        val sbPan = StringBuffer()
        while (panMatcher.find()) {
            val pan = panMatcher.group() ?: ""
            val cleanPan = pan.filter { it in '0'..'9' }
            val masked = if (cleanPan.length >= 8) {
                val prefix = cleanPan.substring(0, 4)
                val suffix = cleanPan.substring(cleanPan.length - 4)
                val stars = "*".repeat(cleanPan.length - 8)
                "$prefix$stars$suffix"
            } else {
                "[REDACTED_PAN]"
            }
            panMatcher.appendReplacement(sbPan, masked)
        }
        panMatcher.appendTail(sbPan)
        result = sbPan.toString()

        // 2. Redact/Mask JSON key-values
        val jsonMatcher = JSON_KEY_VALUE_PATTERN.matcher(result)
        val sbJson = StringBuffer()
        while (jsonMatcher.find()) {
            val key = jsonMatcher.group(1)?.lowercase() ?: ""
            val rawValue = jsonMatcher.group(2) ?: jsonMatcher.group(3) ?: ""
            val maskedValue = maskValueByKey(key, rawValue)
            val matchedKey = jsonMatcher.group(1) ?: key
            jsonMatcher.appendReplacement(sbJson, Matcher.quoteReplacement("\"$matchedKey\":\"$maskedValue\""))
        }
        jsonMatcher.appendTail(sbJson)
        result = sbJson.toString()

        // 3. Redact/Mask Query params / form key-values
        val queryMatcher = QUERY_PARAM_PATTERN.matcher(result)
        val sbQuery = StringBuffer()
        while (queryMatcher.find()) {
            val key = queryMatcher.group(1)?.lowercase() ?: ""
            val rawValue = queryMatcher.group(2) ?: ""
            val maskedValue = maskValueByKey(key, rawValue)
            val matchedKey = queryMatcher.group(1) ?: key
            queryMatcher.appendReplacement(sbQuery, Matcher.quoteReplacement("$matchedKey=$maskedValue"))
        }
        queryMatcher.appendTail(sbQuery)
        result = sbQuery.toString()

        // 4. Redact Authorization Bearer headers
        val authMatcher = AUTH_BEARER_PATTERN.matcher(result)
        val sbAuth = StringBuffer()
        while (authMatcher.find()) {
            val prefix = authMatcher.group(1) ?: "Authorization: Bearer "
            authMatcher.appendReplacement(sbAuth, Matcher.quoteReplacement("${prefix}[REDACTED]"))
        }
        authMatcher.appendTail(sbAuth)
        result = sbAuth.toString()

        // 5. Mask standalone email addresses (if not already masked)
        val emailMatcher = STANDALONE_EMAIL_PATTERN.matcher(result)
        val sbEmail = StringBuffer()
        while (emailMatcher.find()) {
            val userPart = emailMatcher.group(1) ?: ""
            val domainPart = emailMatcher.group(2) ?: ""
            val maskedUser = if (userPart.length > 2) {
                "${userPart.first()}***${userPart.last()}"
            } else {
                "***"
            }
            emailMatcher.appendReplacement(sbEmail, "$maskedUser@$domainPart")
        }
        emailMatcher.appendTail(sbEmail)
        result = sbEmail.toString()

        return result
    }

    private fun maskValueByKey(key: String, rawValue: String): String {
        return when {
            HINT_KEYS.any { it.equals(key, ignoreCase = true) } -> {
                if (rawValue.length > 6) {
                    "${rawValue.take(3)}***${rawValue.takeLast(3)}"
                } else {
                    "[REDACTED]"
                }
            }
            key.contains("email", ignoreCase = true) -> {
                val atIdx = rawValue.indexOf('@')
                if (atIdx > 1) {
                    val user = rawValue.substring(0, atIdx)
                    val domain = rawValue.substring(atIdx)
                    val maskedUser = if (user.length > 2) {
                        "${user.first()}***${user.last()}"
                    } else {
                        "***"
                    }
                    "$maskedUser$domain"
                } else {
                    "[REDACTED_EMAIL]"
                }
            }
            key.contains("phone", ignoreCase = true) -> {
                if (rawValue.length > 6) {
                    val prefix = rawValue.take(4)
                    val suffix = rawValue.takeLast(2)
                    "$prefix******$suffix"
                } else {
                    "[REDACTED_PHONE]"
                }
            }
            else -> "[REDACTED]"
        }
    }
}
