package com.fiuu.xdkandroid.utils

// Single on/off switch for every debug log line in the SDK, driven by mp_dev_mode (see
// Map<String, Any?>.toPaymentRequest()). Defaults to disabled so integrating apps get silent
// output unless they explicitly opt in. Route all debug output through here instead of raw
// println/Log.d calls so one flag governs the whole SDK.
object FiuuLogger {
    var enabled: Boolean = false

    fun log(message: String) {
        if (enabled) println(message)
    }

    // Integration mistakes (bad or unknown params) — printed even with mp_dev_mode off, since the
    // mistake may be in mp_dev_mode itself. Never pass credentials or card data here.
    fun warn(message: String) {
        println("[FiuuXDK] WARNING: $message")
    }
}

// Keys whose value must never reach a log line, even under mp_dev_mode: card PAN/CVV/expiry,
// bearer/card tokens, merchant credentials, and the wallet plugin blobs (Google/Samsung/Apple Pay,
// Click to Pay), which carry a cryptogram or gateway token in their nested fields. mp_dev_mode is
// meant for integration troubleshooting, not to expose the values that let someone actually charge
// a card — this is defense in depth on top of that flag, not a replacement for it.
private val SENSITIVE_JSON_KEYS = setOf(
    "xp_credit_card_no", "creditCardNo",
    "xp_credit_card_cvv", "creditCardCvv",
    "xp_credit_card_expiry", "creditCardExpiry",
    "xp_credit_card_token", "creditCardToken",
    // Instalment-tenure request (InstalmentTenureRequestDto) — the full PAN and its expiry.
    "mp_cc_pan", "ccPan",
    "mp_cc_month", "ccMonth",
    "mp_cc_year", "ccYear",
    "accessToken",
    "mp_password", "password",
    "mp_verification_key", "verificationKey",
    "token", "cardToken",
    "xp_googlepay", "googlePay",
    "xp_samsungpay", "samsungPay",
    "xp_applepay", "applePayToken",
    "xp_click2pay", "click2Pay",
    "paymentData"
)

/**
 * Best-effort redaction of a JSON string before it's written to a debug log: replaces the value
 * of any key in [SENSITIVE_JSON_KEYS] with "[REDACTED]", whether that value is a string or a
 * nested object. Not a security boundary by itself (logging is already gated by mp_dev_mode) —
 * this is defense in depth so PAN/CVV/expiry/tokens never land in Logcat/stdout even when a host
 * app leaves dev mode on in a shipped build.
 */
fun redactSensitiveJson(raw: String): String {
    val out = StringBuilder(raw.length)
    var i = 0
    while (i < raw.length) {
        if (raw[i] != '"') {
            out.append(raw[i])
            i++
            continue
        }
        val tokenStart = i
        var j = i + 1
        while (j < raw.length && raw[j] != '"') {
            if (raw[j] == '\\') j++
            j++
        }
        val token = raw.substring((tokenStart + 1).coerceAtMost(raw.length), j.coerceAtMost(raw.length))
        if (token in SENSITIVE_JSON_KEYS) {
            var p = (j + 1).coerceAtMost(raw.length)
            while (p < raw.length && raw[p].isWhitespace()) p++
            if (p < raw.length && raw[p] == ':') {
                p++
                while (p < raw.length && raw[p].isWhitespace()) p++
                if (p < raw.length && raw[p] == '{') {
                    var depth = 0
                    var c = p
                    while (c < raw.length) {
                        when (raw[c]) {
                            '{' -> depth++
                            '}' -> { depth--; if (depth == 0) { c++; break } }
                        }
                        c++
                    }
                    out.append(raw, i, p).append("\"[REDACTED]\"")
                    i = c
                    continue
                } else if (p < raw.length && raw[p] == '"') {
                    var c = p + 1
                    while (c < raw.length && raw[c] != '"') {
                        if (raw[c] == '\\') c++
                        c++
                    }
                    c++
                    out.append(raw, i, p).append("\"[REDACTED]\"")
                    i = c
                    continue
                }
            }
        }
        out.append(raw, i, (j + 1).coerceAtMost(raw.length))
        i = j + 1
    }
    return out.toString()
}

// Same redaction, applied to a Map (e.g. the checksum payload built before it's serialized) —
// used at call sites that log a Map<String, *> directly rather than a JSON-encoded DTO string.
fun redactSensitiveMap(map: Map<String, Any?>): Map<String, Any?> =
    map.mapValues { (key, value) -> if (key in SENSITIVE_JSON_KEYS) "[REDACTED]" else value }
