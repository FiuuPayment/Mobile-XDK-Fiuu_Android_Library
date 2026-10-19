package com.fiuu.xdkandroid.utils

import korlibs.crypto.SHA256
import kotlinx.serialization.json.JsonPrimitive

// Mirrors the reference Postman generateChecksum_fn: numbers and non-numbers are tracked
// separately so they can be sorted the way JS's typeof-based comparator sorts them (see below).
private class Leaf(val isNumeric: Boolean, val numericValue: Double = 0.0, val stringValue: String = "")

private fun formatNumber(d: Double): String {
    val asLong = d.toLong()
    return if (d == asLong.toDouble()) asLong.toString() else d.toString()
}

fun generateChecksum(
    payload: Map<String, Any?>,
    vkey: String = ""
): String {

    fun flattenObject(obj: Any?): List<Leaf> {
        val result = mutableListOf<Leaf>()

        when (obj) {
            // JsonPrimitive.isString distinguishes a JSON number/boolean/null literal (unquoted)
            // from a JSON string (quoted) — this is what JS's `typeof x === 'number'` is keying
            // off of, e.g. Google Pay's apiVersion: 2 vs mp_amount: "1.01".
            is JsonPrimitive -> {
                val numeric = if (!obj.isString) obj.content.toDoubleOrNull() else null
                if (numeric != null) {
                    result.add(Leaf(isNumeric = true, numericValue = numeric))
                } else {
                    result.add(Leaf(isNumeric = false, stringValue = obj.content))
                }
            }
            is Map<*, *> -> {
                for (value in obj.values) {
                    result.addAll(flattenObject(value))
                }
            }
            is List<*> -> {
                for (item in obj) {
                    result.addAll(flattenObject(item))
                }
            }
            is Boolean -> result.add(Leaf(isNumeric = false, stringValue = obj.toString()))
            is Number -> result.add(Leaf(isNumeric = true, numericValue = obj.toDouble()))
            // The JS reference pushes a literal null (stringified as "null") rather than skipping
            // it, so a missing/null field still contributes to the hash.
            null -> result.add(Leaf(isNumeric = false, stringValue = "null"))
            else -> result.add(Leaf(isNumeric = false, stringValue = obj.toString()))
        }

        return result
    }

    // 2. Sort: numbers first (ascending numeric order), then everything else case-insensitively —
    // matches the reference comparator's isANum/isBNum/localeCompare branches exactly.
    fun sortAndStringify(leaves: List<Leaf>): List<String> {
        val sortedLeaves = leaves.sortedWith(Comparator { a, b ->
            when {
                a.isNumeric && b.isNumeric -> a.numericValue.compareTo(b.numericValue)
                a.isNumeric -> -1
                b.isNumeric -> 1
                else -> localeCompareBase(a.stringValue, b.stringValue)
            }
        })
        return sortedLeaves.map { if (it.isNumeric) formatNumber(it.numericValue) else it.stringValue }
    }

    // 1. Flatten payload values only, then sort — this drives the real checksum, so it must use
    // the raw payload (redacting here would change the hash the backend expects).
    val sortedValues = sortAndStringify(flattenObject(payload))

    // 3. Construct the input: vkey + joined sorted values
    val input = vkey + sortedValues.joinToString("")

    // Logging must never leak PAN/CVV/expiry/tokens, even under mp_dev_mode (same guarantee
    // redactSensitiveJson/-Map give every other debug log line — see FiuuLogger.kt). Flattening
    // loses key names, so redaction has to happen on the payload map *before* flattening; do that
    // in a separate pass used only for these log lines, never for the actual checksum above.
    val redactedSortedValues = sortAndStringify(flattenObject(redactSensitiveMap(payload)))
    FiuuLogger.log("SORTED VALUES: $redactedSortedValues")
    // vkey is a long-lived merchant secret, not a per-transaction value — never print it, even
    // with logging enabled. It's always the input's prefix by construction (see above).
    FiuuLogger.log("FINAL CHECKSUM INPUT: ***REDACTED***${redactedSortedValues.joinToString("")}")

    return SHA256.digest(input.encodeToByteArray())
        .hexLower
}
