package com.fiuu.xdkandroid.domain.entity

import com.fiuu.xdkandroid.utils.FiuuLogger

// Every mp_* key the native engine reads from host params. Add new params here when it starts
// reading them.
internal val NATIVE_PARAM_KEYS = setOf(
    "mp_amount", "mp_ap_merchant_ID", "mp_app_name", "mp_bill_description",
    "mp_bill_description_edit_disabled", "mp_bill_email", "mp_bill_email_edit_disabled",
    "mp_bill_mobile", "mp_bill_mobile_edit_disabled", "mp_bill_name",
    "mp_bill_name_edit_disabled", "mp_build_mode", "mp_channel", "mp_channel_type", "mp_closebutton_display",
    "mp_core_env", "mp_country", "mp_currency", "mp_dev_mode", "mp_editing_enabled",
    "mp_express_mode", "mp_gp_merchant_ID", "mp_merchant_ID", "mp_order_ID", "mp_password", "mp_sandbox_mode",
    "mp_username", "mp_verification_key"
)

// mp_* keys only the legacy Webcore engines use: both forward the whole params map to the hosted
// Webcore page (IosWebcoreViewController, legacyWebcore PaymentActivity), which acts on these.
// The native engine never reads them, so passing one with mp_core_env 1 has no effect — e.g.
// mp_bin_lock or mp_allowed_channels kept by a merchant moving from Webcore to native.
internal val WEBCORE_ONLY_PARAM_KEYS = setOf(
    "mp_advanced_email_validation_enabled", "mp_advanced_phone_validation_enabled",
    "mp_allowed_channels", "mp_bin_lock",
    "mp_bin_lock_err_msg", "mp_card_list_disabled", "mp_cash_waittime", "mp_channel_editing",
    "mp_company", "mp_custom_css_url", "mp_disabled_channels", "mp_dpa_id",
    "mp_enable_fullscreen", "mp_extended_vcode", "mp_filter", "mp_ga_enabled", "mp_gpay_channel",
    "mp_hide_googlepay", "mp_is_escrow", "mp_is_recurring", "mp_language", "mp_metadata",
    "mp_non_3DS", "mp_preferred_token", "mp_request_type", "mp_secured_verified", "mp_tcctype",
    "mp_transaction_id"
)

// Every mp_* key either engine reads. Used to flag keys a host passed that nothing will read
// (usually a typo, e.g. mp_merchant_id for mp_merchant_ID).
internal val KNOWN_PARAM_KEYS = NATIVE_PARAM_KEYS + WEBCORE_ONLY_PARAM_KEYS

private val KNOWN_PARAM_KEYS_BY_LOWERCASE = KNOWN_PARAM_KEYS.associateBy { it.lowercase() }

// Reads a Boolean param the way toPaymentRequest() always has: a real Boolean, or exactly
// "true"/"false". Anything else (absent, "TRUE", "yes", 1) is [default] — paramWarnings() reports
// the unrecognized ones.
internal fun Map<String, Any?>.boolParam(key: String, default: Boolean): Boolean =
    when (val value = this[key]) {
        is Boolean -> value
        null -> default
        else -> value.toString().toBooleanStrictOrNull() ?: default
    }

private fun Any?.describe(): String = if (this is String) "\"$this\"" else "$this"

/**
 * Lists every param the SDK will silently ignore or replace with a fallback. Pure (no logging),
 * so it's testable; [reportParamWarnings] logs the result. Never changes how a value resolves —
 * the resolvers in PaymentRequest.kt stay the single source of truth for that.
 *
 * Only ever echoes values of the environment/flag params below, never credentials, and only key
 * names for unknown keys.
 */
fun Map<String, Any?>.paramWarnings(): List<String> {
    val warnings = mutableListOf<String>()
    val coreEnv = resolveCoreEnv()

    this["mp_core_env"]?.let { raw ->
        if (raw.toString().toIntOrNull() !in 1..4) {
            warnings += "mp_core_env=${raw.describe()} is not 1-4; using 1 (native engine)."
        }
    }

    // mp_core_env 2/3/4 overrides mp_build_mode, so an invalid one doesn't matter there.
    if (coreEnv == 1) {
        this["mp_build_mode"]?.let { raw ->
            if (raw.toString().toIntOrNull() !in setOf(2, 3, 4)) {
                val fallback = if (resolveSandboxMode()) "4 (sandbox)" else "2 (production)"
                warnings += "mp_build_mode=${raw.describe()} is not 2, 3 or 4; " +
                    "falling back to mp_sandbox_mode -> $fallback."
            }
        }
    }

    this["mp_sandbox_mode"]?.let { raw ->
        val recognized = raw is Boolean ||
            (raw is String && (raw.equals("true", true) || raw.equals("false", true)))
        if (!recognized) {
            warnings += "mp_sandbox_mode=${raw.describe()} is not true/false; treating it as true (sandbox)."
        }
    }

    listOf(
        "mp_express_mode" to false,
        "mp_editing_enabled" to false,
        "mp_bill_name_edit_disabled" to false,
        "mp_bill_email_edit_disabled" to false,
        "mp_bill_mobile_edit_disabled" to false,
        "mp_bill_description_edit_disabled" to false,
        "mp_dev_mode" to false,
        "mp_closebutton_display" to true
    ).forEach { (key, default) ->
        val raw = this[key] ?: return@forEach
        if (raw !is Boolean && raw.toString().toBooleanStrictOrNull() == null) {
            warnings += "$key=${raw.describe()} is not true/false; using $default."
        }
    }

    // Webcore (mp_core_env 2/3/4) forwards every param to the hosted page, which may read keys
    // this SDK doesn't know about — only the native engine can say a key goes unread.
    if (coreEnv == 1) {
        keys.filter { it.startsWith("mp_") && it !in KNOWN_PARAM_KEYS }.sorted().forEach { key ->
            val suggestion = KNOWN_PARAM_KEYS_BY_LOWERCASE[key.lowercase()]
            warnings += if (suggestion != null) {
                "Unknown param $key is ignored. Did you mean $suggestion?"
            } else {
                "Unknown param $key is ignored."
            }
        }
        val webcoreOnly = keys.filter { it in WEBCORE_ONLY_PARAM_KEYS }.sorted()
        if (webcoreOnly.isNotEmpty()) {
            warnings += "Webcore-only params are ignored by the native engine (mp_core_env 1): " +
                "${webcoreOnly.joinToString()}. They only take effect with mp_core_env 2-4."
        }
    }
    return warnings
}

// Call once per session, at each public entry point — not from the resolvers or
// toPaymentRequest(), which run repeatedly (recomposition, per-flow re-parses).
fun Map<String, Any?>.reportParamWarnings() {
    paramWarnings().forEach { FiuuLogger.warn(it) }
}
