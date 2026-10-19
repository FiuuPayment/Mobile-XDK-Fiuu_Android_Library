package com.fiuu.xdkandroid.telemetry

/**
 * PCI-DSS and security redactor for FiuuXDK in-library telemetry.
 * Ensures zero unencrypted cardholder data, PIN blocks, tokens, or merchant secrets are
 * stored in local queues or transmitted to remote telemetry aggregators.
 */
object TelemetryRedactor {

    // Regex matching potential credit/debit PANs (13 to 19 digits), unformatted and formatted with dashes/spaces
    private val PAN_REGEX = Regex("""\b([0-9]{6})[0-9]{3,9}([0-9]{4})\b""")
    private val FORMATTED_PAN_REGEX = Regex("""\b([0-9]{4})[- ]([0-9]{2})[0-9]{2}[- ]([0-9]{4})[- ]([0-9]{4})\b""")

    // Sensitive keys whose values must never be stored or dispatched in cleartext
    val SENSITIVE_KEYS: Set<String> = setOf(
        "xp_credit_card_no", "creditcardno", "cardno",
        "xp_credit_card_cvv", "creditcardcvv", "cvv", "cvc",
        "xp_credit_card_expiry", "creditcardexpiry",
        "pinblock", "pinblockksn", "ksn", "purchasepinblock", "purchaseksn",
        "requestdata",
        "mp_password", "password",
        "mp_verification_key", "verificationkey", "vkey", "skey",
        "token", "cardtoken", "accesstoken",
        "xp_googlepay", "googlepay",
        "xp_samsungpay", "samsungpay",
        "xp_applepay", "applepaytoken",
        "xp_click2pay", "click2pay",
        "paymentdata",
        "checksum", "signature", "x-signature",
        "secretkey", "privatekey"
    )

    /**
     * Masks PANs and replaces known sensitive keys in plain strings or JSON snippets.
     */
    fun redact(input: String?): String {
        if (input.isNullOrBlank()) return ""

        // 1. Mask credit/debit PANs (both formatted and continuous)
        var result = FORMATTED_PAN_REGEX.replace(input) { match ->
            "${match.groupValues[1]}${match.groupValues[2]}******${match.groupValues[4]}"
        }
        result = PAN_REGEX.replace(result) { match ->
            "${match.groupValues[1]}******${match.groupValues[2]}"
        }

        // 2. Redact key-value patterns (e.g. "key": "value", "key": { ... }, or key=value)
        for (key in SENSITIVE_KEYS) {
            // Pattern for JSON string value: "key"\s*:\s*"value"
            val jsonStringPattern = Regex("""(?i)("?$key"?\s*[:=]\s*")([^"]*)(")""")
            result = jsonStringPattern.replace(result) { m ->
                "${m.groupValues[1]}[REDACTED]${m.groupValues[3]}"
            }

            // Pattern for JSON object/nested value: "key"\s*:\s*\{...\}
            val jsonObjectPattern = Regex("""(?i)("?$key"?\s*[:=]\s*)(\{[\s\S]*?\})""")
            result = jsonObjectPattern.replace(result) { m ->
                "${m.groupValues[1]}\"[REDACTED]\""
            }

            // Pattern for unquoted key=value or "key": 123 (stopping at delimiter)
            val unquotedPattern = Regex("""(?i)("?$key"?\s*[:=]\s*)(?!\"|\[REDACTED\])([^,\s&}\]]+)""")
            result = unquotedPattern.replace(result) { m ->
                "${m.groupValues[1]}[REDACTED]"
            }
        }

        return result
    }

    /**
     * Redacts sensitive key-value pairs in a Map structure.
     */
    fun redactMap(map: Map<String, Any?>): Map<String, Any?> {
        return map.mapValues { (key, value) ->
            val normalizedKey = key.lowercase()
            when {
                normalizedKey in SENSITIVE_KEYS -> "[REDACTED]"
                value is Map<*, *> -> @Suppress("UNCHECKED_CAST") redactMap(value as Map<String, Any?>)
                value is String -> redact(value)
                else -> value
            }
        }
    }
}
