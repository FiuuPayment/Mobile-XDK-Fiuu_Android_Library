package com.fiuu.xdkandroid.domain.entity

import com.fiuu.xdkandroid.data.remote.dto.PaymentInitRequestDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentExpressRequestDto
import com.fiuu.xdkandroid.diagnostics.DiagnosticChaosEngine
import com.fiuu.xdkandroid.utils.FiuuLogger
import io.ktor.util.encodeBase64
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

private val CARD_CHANNEL_KEYWORDS = listOf("card", "_cnp", "_unionpay", "_visa", "_mastercard", "_diners")

// Channel name -> the name putChannel sends as mp_channel/xp_channel for cash channels.
private val CASH_CHANNEL_WIRE_NAMES = mapOf(
    "Cash-99SM" to "CASH-99",
    "Cash-711" to "CASH-711",
    "Cash-KKMart" to "CASH-KK",
    "Cash-Cosway" to "CASH-COSWAY",
    "Cash-Th" to "CASH-TH",
    "ShopeePay" to "SHOPEEPAY"
)

// Feature toggle for the /express Google Pay flow only (see PaymentRequest.putChannel): when
// true, xp_googlepay is sent as a base64-encoded JSON string instead of a nested JsonObject.
// Flip this to change the wire format for every host app without touching the params map.
// Shared (not per-call-site) so every place that attaches googlePayData — PaymentViewModel's
// in-SDK processGooglePayStart and ExpressGooglePay's standalone host-button flow alike — stays
// in sync rather than one of them silently defaulting PaymentRequest.googlePayTokenAsBase64 to
// false.
const val GOOGLE_PAY_TOKEN_AS_BASE64 = false

data class PaymentRequest(
    val amount: String? = null,
    val username: String? = null,
    val password: String? = null,
    val merchantId: String? = null,
    val appName: String? = null,
    val orderId: String? = null,
    val currency: String? = null,
    val country: String? = null,
    val verificationKey: String? = null,
    val channel: String? = null,
    val channelType: String? = null,
    val billDescription: String? = null,
    val billName: String? = null,
    val billEmail: String? = null,
    val billMobile: String? = null,
    val expressMode: Boolean = false,
    // mp_editing_enabled: whether the payer can edit the Order Details fields (name, email,
    // mobile, description). The mp_bill_*_edit_disabled flags then lock individual fields; they
    // have no effect while editingEnabled is false. UI-only — never sent to the server or part
    // of the checksum. Read them through the isBill*Editable properties below.
    val editingEnabled: Boolean = false,
    val billNameEditDisabled: Boolean = false,
    val billEmailEditDisabled: Boolean = false,
    val billMobileEditDisabled: Boolean = false,
    val billDescriptionEditDisabled: Boolean = false,
    val sandboxMode: Boolean = true,
    // Drives FiuuLogger.enabled (see toPaymentRequest()) — not otherwise part of any wire
    // payload or the checksum.
    val devMode: Boolean = false,
    val deviceInfo: String? = null,
    val checksum: String? = null,
    // merchant.* identifier registered for Apple Pay. Used only locally to decide whether to
    // show the Apple Pay tile and as PKPaymentRequest.merchantIdentifier for the native sheet —
    // it is never sent to the server (cleared once applePayToken is set) and never affects the
    // checksum.
    val apMerchantId: String? = null,
    // Gateway merchant ID for Google Pay's tokenizationSpecification/merchantInfo — used only
    // locally to build the Google Pay sheet request (see GooglePayConfig), never sent to the
    // server. Falls back to GooglePayConfig's default when not supplied.
    val gpMerchantId: String? = null,
    // Set when an express-mode payment is being completed with a Google Pay token (see
    // expressGooglePay) — folded into toExpressApiPayload/toExpressDto as xp_channel/
    // xp_channel_type/xp_googlepay so a single /express call carries both mp_* and xp_* data.
    val googlePayData: JsonObject? = null,
    // On/off switch for the /express call only (see putChannel): when true, xp_googlepay is sent
    // as a base64-encoded JSON string instead of a nested JsonObject. Not host-controlled — set
    // internally by PaymentViewModel (see GOOGLE_PAY_TOKEN_AS_BASE64) when it attaches
    // googlePayData, rather than parsed from the params map.
    val googlePayTokenAsBase64: Boolean = false,
    val c2pData: JsonObject? = null,
    val samsungPayData: JsonObject? = null,
    // Set when startApplePay()'s PKPaymentAuthorizationController flow completes — folded into
    // toExpressApiPayload/toProcessApiPayload as xp_channel/xp_channel_type/xp_applepay,
    // same as googlePayData/samsungPayData.
    val applePayToken: JsonObject? = null,
    // Set when the user manually enters a new card via CardManualEntryScreen — folded into
    // toProcessApiPayload as xp_channel="CardPayment"/xp_channel_type="CC" plus the individual
    // xp_card_holder_name/xp_credit_card_no/xp_credit_card_cvv/xp_credit_card_expiry/
    // xp_token_status fields. Unlike googlePayData/samsungPayData/etc. these are flat scalars,
    // not a JSON blob — the manual-entry endpoint takes the raw card fields directly rather than
    // a platform-issued token.
    val manualCardHolderName: String? = null,
    val manualCardNumber: String? = null,
    val manualCardExpiry: String? = null,
    val manualCardCvv: String? = null,
    val manualCardSaveForFuture: Boolean = false,
    // Set only for CardInstalmentEntryScreen's flow (see PaymentViewModel.payWithInstalmentCard):
    // the chosen InstlMonth, sent as xp_installmonth alongside xp_channel="CREDIT10" in place of
    // the generic "CardPayment" channel used for a normal manual card. Null for a non-instalment
    // manual card payment.
    val manualCardInstalmentMonth: String? = null,
    // Set when the user confirms paying with a previously saved card from CardManualEntryScreen's
    // "Saved Card" picker (see PaymentViewModel.payWithSavedCard) — folded into toProcessApiPayload
    // as xp_channel="credit23"/xp_channel_type="CC"/xp_credit_card_token=<this>/xp_token_status="2". Distinct
    // from manualCardNumber, which is a freshly typed-in card rather than a stored token.
    val savedCardToken: String? = null,
    // Which backend environment (ApiConfig.baseUrl) / Wallet environment (GooglePayConfig) this
    // session talks to — see Map<String, Any?>.resolveBuildMode() below for how it's derived.
    val buildMode: Int? = null,
    // Whether native full-screen sub-screens (e.g. the live Click to Pay widget) should render
    // their own close affordance. Not part of any wire payload — mirrors the legacy Webcore
    // engine's mp_closebutton_display default (see ProvideFiuuPaymentSDK.kt) so the two engines
    // behave the same when the param is omitted.
    val closeButtonDisplay: Boolean = true )

// Drives both ApiConfig.baseUrl and GooglePayConfig.walletEnvironment, so this is the one place
// mp_sandbox_mode is interpreted — every reader must go through here rather than re-parsing the
// raw params value, or the two could drift apart. Accepts a real Boolean or a "true"/"false"
// String (host apps have passed both; a strict `as? Boolean` cast silently swallowed String
// values and fell back to false). Defaults to sandbox (fail-safe) when absent or unrecognized,
// so a forgotten flag can't silently process real money.
fun Map<String, Any?>.resolveSandboxMode(): Boolean =
    when (val value = this["mp_sandbox_mode"]) {
        is Boolean -> value
        is String -> !value.equals("false", ignoreCase = true)
        else -> true
    }

fun Map<String, Any?>.resolveDevMode(): Boolean = boolParam("mp_dev_mode", default = false)

// Host apps may pass mp_amount as a whole number ("10") or with one decimal ("1.0", or a Double
// 1.0) instead of "10.00" — the Fiuu backend expects a decimal string with 2 places, and the
// checksum must be generated from exactly what gets sent, so this normalization has to happen once
// here rather than relying on every host app to format it correctly. Padded as a string, not via
// Double, so no float rounding. More than 2 decimals or anything non-numeric is passed through
// unchanged — never silently round a money amount.
fun Map<String, Any?>.resolveAmount(): String? {
    val value = this["mp_amount"]?.toString()?.trim() ?: return null
    val whole = value.substringBefore('.')
    val fraction = value.substringAfter('.', missingDelimiterValue = "")
    if (whole.isEmpty() || !whole.all { it.isDigit() } || !fraction.all { it.isDigit() }) return value
    if (fraction.length > 2) return value
    return "$whole.${fraction.padEnd(2, '0')}"
}

// mp_core_env is 1 (native engine) or 2/3/4 (legacy Webcore production/UAT/sandbox). Anything
// else — absent, malformed, or out of range — resolves to 1.
fun Map<String, Any?>.resolveCoreEnv(): Int {
    val parsed = when (val value = this["mp_core_env"]) {
        is Int -> value
        is String -> value.toIntOrNull()
        else -> value?.toString()?.toIntOrNull()
    }
    return parsed?.takeIf { it in 1..4 } ?: 1
}

fun Map<String, Any?>.resolveBuildMode(): Int {
    // mp_core_env 2/3/4 overrides mp_build_mode; 1 (the default) leaves it to mp_build_mode
    val coreEnv = this.resolveCoreEnv()
    if (coreEnv != 1) return coreEnv

    // Fail-safe default when mp_build_mode is absent or invalid — resolves based on sandboxMode only
    val fallback = when {
        this.resolveSandboxMode() -> 4 // Sandbox
        else -> 2 // Production
    }
    val rawValue = this["mp_build_mode"] ?: return fallback
    val parsed = when (rawValue) {
        is Int -> rawValue
        is String -> rawValue.toIntOrNull()
        else -> rawValue.toString().toIntOrNull()
    }
    return if (parsed == 2 || parsed == 3 || parsed == 4) parsed else fallback
}

fun Map<String, Any?>.toPaymentRequest(): PaymentRequest {
    val request = PaymentRequest(
        amount = this.resolveAmount(),
        currency = this["mp_currency"]?.toString(),
        country = this["mp_country"]?.toString(),
        orderId = this["mp_order_ID"]?.toString(),
        username = this["mp_username"]?.toString(),
        password = this["mp_password"]?.toString(),
        merchantId = this["mp_merchant_ID"]?.toString(),
        appName = this["mp_app_name"]?.toString(),
        verificationKey = this["mp_verification_key"]?.toString(),
        channel = this["mp_channel"]?.toString(),
        channelType = this["mp_channel_type"]?.toString(),
        billDescription = this["mp_bill_description"]?.toString(),
        billName = this["mp_bill_name"]?.toString(),
        billEmail = this["mp_bill_email"]?.toString(),
        billMobile = this["mp_bill_mobile"]?.toString(),
        expressMode = boolParam("mp_express_mode", default = false),
        editingEnabled = boolParam("mp_editing_enabled", default = false),
        billNameEditDisabled = boolParam("mp_bill_name_edit_disabled", default = false),
        billEmailEditDisabled = boolParam("mp_bill_email_edit_disabled", default = false),
        billMobileEditDisabled = boolParam("mp_bill_mobile_edit_disabled", default = false),
        billDescriptionEditDisabled = boolParam("mp_bill_description_edit_disabled", default = false),
        sandboxMode = this.resolveSandboxMode(),
        devMode = this.resolveDevMode(),
        deviceInfo = this["device_info"]?.toString(),
        apMerchantId = this["mp_ap_merchant_ID"]?.toString(),
        gpMerchantId = this["mp_gp_merchant_ID"]?.toString(),
        buildMode = this.resolveBuildMode(),
        closeButtonDisplay = boolParam("mp_closebutton_display", default = true)
    )
    // FiuuLogger.enabled must be set here, not deferred to a caller, so every log line from
    // this point on (checksum/signature generation, API request/response bodies) is gated by
    // devMode without every call site re-checking the flag.
    FiuuLogger.enabled = request.devMode
    // Same idea for the test fault injector: its toggles are public (the demo apps drive them),
    // so they only take effect for a dev-mode session that isn't on the production backend.
    DiagnosticChaosEngine.isArmed = request.devMode && request.buildMode != 2
    return request
}

fun PaymentRequest.toInitApiPayload(includeChecksum: Boolean = true): Map<String, String?> {
    val payload = mutableMapOf(
        "mp_merchant_ID" to merchantId,
        "mp_amount" to amount,
        "mp_app_name" to (appName ?: "mobile"),
        "mp_order_ID" to orderId,
        "mp_currency" to currency,
        "mp_country" to country,
        "mp_bill_name" to billName,
        "mp_bill_email" to billEmail,
        "mp_bill_mobile" to billMobile,
        "mp_bill_description" to billDescription
    )
    if (includeChecksum) {
        payload["mp_checksum"] = checksum
    }
    return payload }

// Whether each Order Details field is editable: mp_editing_enabled, minus that field's
// mp_bill_*_edit_disabled flag.
val PaymentRequest.isBillNameEditable: Boolean get() = editingEnabled && !billNameEditDisabled
val PaymentRequest.isBillEmailEditable: Boolean get() = editingEnabled && !billEmailEditDisabled
val PaymentRequest.isBillMobileEditable: Boolean get() = editingEnabled && !billMobileEditDisabled
val PaymentRequest.isBillDescriptionEditable: Boolean
    get() = editingEnabled && !billDescriptionEditDisabled

// True for the PLUGIN channel type (Google Pay, Samsung Pay, Click to Pay, Apple Pay). Mirrors
// putChannel below, which writes mp_channel_type="PLUGIN" onto the wire from the attached plugin
// data rather than PaymentRequest.channelType — host apps usually don't pass mp_channel_type.
val PaymentRequest.isPluginPayment: Boolean
    get() = channelType.equals("PLUGIN", ignoreCase = true) ||
        googlePayData != null || samsungPayData != null || c2pData != null || applePayToken != null

// True for over-the-counter cash channels (channel type OTC, which includes 7-11 / Cash-711).
// /inquire for these always sends xp_include_pending=true — see inquireChecksumPayload.
val PaymentRequest.isOtcPayment: Boolean
    get() = channelType.equals("OTC", ignoreCase = true) ||
        channel.equals("Cash-711", ignoreCase = true)

// Shared by toExpressApiPayload/toProcessApiPayload — the two payloads differ only in field name
// prefix (mp_ vs xp_) and in toExpressApiPayload never having c2pData set, but the channel/
// channelType priority order and the extra xp_* side-channel value are identical. keyPrefix is
// passed in by the caller rather than derived from request.expressMode: toExpressDto() always
// reads channel/channelType back out under "mp_channel"/"mp_channel_type" (matching
// PaymentExpressRequestDto's @SerialName), so toExpressApiPayload must always write mp_ regardless
// of expressMode — the standalone Apple/Google/Samsung Pay express-button entry points
// (ExpressApplePay.kt etc.) call toExpressApiPayload with expressMode left false (they don't go
// through the SDK's mp_express_mode toggle), which used to make putChannel write xp_channel here
// and leave PaymentExpressRequestDto.channel null on the wire.
private fun PaymentRequest.putChannel(payload: MutableMap<String, Any?>, keyPrefix: String) {
    when {
        googlePayData != null -> {
            payload["${keyPrefix}channel"] = "GooglePay"
            payload["${keyPrefix}channel_type"] = "PLUGIN"
            payload["xp_googlepay"] = googlePayData
            // googlePayTokenAsBase64 only applies to /express (expressMode == true, see the
            // keyPrefix branch above) — adds a side-by-side base64 copy under a separate key
            // rather than replacing xp_googlepay, so /process is unaffected either way.
            if (googlePayTokenAsBase64) {
                payload["xp_googlepay64"] = googlePayData.toString().encodeToByteArray().encodeBase64()
            }
        }
        samsungPayData != null -> {
            payload["${keyPrefix}channel"] = "SamsungPay"
            payload["${keyPrefix}channel_type"] = "PLUGIN"
            payload["xp_samsungpay"] = samsungPayData
        }
        c2pData != null -> {
            payload["${keyPrefix}channel"] = "Click2Pay"
            payload["${keyPrefix}channel_type"] = "PLUGIN"
            payload["xp_click2pay"] = c2pData
        }
        applePayToken != null -> {
            // apMerchantId is deliberately not read here — it's only a local input to the popup
            // (PKPaymentRequest.merchantIdentifier) and is cleared once applePayToken is set, so
            // the ApplePay branch is selected purely off the token, same as googlePayData/
            // samsungPayData.
            payload["${keyPrefix}channel"] = "ApplePay"
            payload["${keyPrefix}channel_type"] = "PLUGIN"
            payload["xp_applepay"] = applePayToken
        }
        savedCardToken != null -> {
            // Confirmed /process shape for paying with a previously saved card token: same
            // "CREDITAN" channel as a freshly typed card below, but xp_credit_card_token carries
            // the saved token instead of raw card fields, and xp_token_status="2" tells the
            // backend this is an existing-token charge rather than a save ("1") or no-save ("0").
            payload["${keyPrefix}channel"] = "CREDITAN"
            payload["${keyPrefix}channel_type"] = "CC"
            payload["xp_credit_card_token"] = savedCardToken
            payload["xp_token_status"] = "2"
        }
        manualCardNumber != null && manualCardInstalmentMonth != null -> {
            // Confirmed /process shape for an instalment payment: xp_channel is always
            // "CREDIT10", not the generic "CardPayment" used below nor the bank-specific Channel
            // (e.g. "CREDITBA") the tenure lookup returns. xp_token_status is always "0" here —
            // no save-card option on CardInstalmentEntryScreen.
            payload["${keyPrefix}channel"] = "CREDIT10"
            payload["${keyPrefix}channel_type"] = "CC"
            payload["xp_card_holder_name"] = manualCardHolderName
            payload["xp_credit_card_no"] = manualCardNumber
            payload["xp_credit_card_cvv"] = manualCardCvv
            payload["xp_credit_card_expiry"] = manualCardExpiry
            payload["xp_installmonth"] = manualCardInstalmentMonth
            payload["xp_token_status"] = "0"
        }
        manualCardNumber != null -> {
            payload["${keyPrefix}channel"] = "CREDITAN"
            payload["${keyPrefix}channel_type"] = "CC"
            payload["xp_card_holder_name"] = manualCardHolderName
            payload["xp_credit_card_no"] = manualCardNumber
            payload["xp_credit_card_cvv"] = manualCardCvv
            payload["xp_credit_card_expiry"] = manualCardExpiry
            payload["xp_token_status"] = if (manualCardSaveForFuture) "1" else "0"
        }
        channel in CASH_CHANNEL_WIRE_NAMES -> {
            // Cash channels go on the wire under different names (e.g. "Cash-99SM" -> "CASH-99").
            payload["${keyPrefix}channel"] = CASH_CHANNEL_WIRE_NAMES.getValue(channel!!)
            payload["${keyPrefix}channel_type"] = channelType
        }
        CARD_CHANNEL_KEYWORDS.any { channel?.lowercase()?.contains(it) == true } -> {
            payload["${keyPrefix}channel"] = channel
            payload["${keyPrefix}channel_type"] = "CC"
        }
        channelType == null -> {
            payload["${keyPrefix}channel"] = channel
            payload["${keyPrefix}channel_type"] = "EW"
        }
        else -> {
            payload["${keyPrefix}channel"] = channel
            payload["${keyPrefix}channel_type"] = channelType
        }
    }
}

fun PaymentRequest.toExpressApiPayload(includeChecksum: Boolean = true): Map<String, Any?> {
    val payload = mutableMapOf<String, Any?>(
        "mp_merchant_ID" to merchantId,
        "mp_amount" to amount,
        "mp_app_name" to (appName ?: "mobile"),
        "mp_order_ID" to orderId,
        "mp_currency" to currency,
        "mp_country" to country,
        "mp_bill_name" to billName,
        "mp_bill_email" to billEmail,
        "mp_bill_mobile" to billMobile,
        "mp_bill_description" to billDescription,
    )
    // Google Pay express checkout: fold xp_* fields into the same payload so a single
    // generateChecksum call produces one mp_checksum covering both mp_* and xp_* values.
    putChannel(payload, keyPrefix = "mp_")
    if (includeChecksum) {
        payload["mp_checksum"] = checksum
    }
    return payload }

fun PaymentRequest.toProcessApiPayload(includeChecksum: Boolean = true): Map<String, Any?> {
    val payload = mutableMapOf<String, Any?>(
        "xp_bill_name" to billName,
        "xp_bill_email" to billEmail,
        "xp_bill_mobile" to billMobile,
        "xp_bill_description" to billDescription,
    )

    putChannel(payload, keyPrefix = "xp_")

    if (includeChecksum) {
        payload["xp_checksum"] = checksum
    }
    return payload }

// POST card-tokens' checksum only covers the billing fields — no channel fields, unlike
// toProcessApiPayload — so this can't reuse that function without pulling in xp_channel/etc.
// and desyncing the checksum from what's actually in CardTokensRequestDto's body.
fun PaymentRequest.toCardTokensApiPayload(includeChecksum: Boolean = true): Map<String, String?> {
    val payload = mutableMapOf(
        "xp_bill_name" to billName,
        "xp_bill_email" to billEmail,
        "xp_bill_mobile" to billMobile,
        "xp_bill_description" to billDescription,
    )
    if (includeChecksum) {
        payload["xp_checksum"] = checksum
    }
    return payload }

fun PaymentRequest.toInitDto(): PaymentInitRequestDto {
    return PaymentInitRequestDto(
        merchantId = merchantId,
        amount = amount,
        appName = appName ?: "mobile",
        orderId = orderId,
        currency = currency,
        country = country,
        billName = billName,
        billEmail = billEmail,
        billMobile = billMobile,
        billDescription = billDescription,
        checksum = checksum
    ) }

fun PaymentRequest.toExpressDto(): PaymentExpressRequestDto {
    // Built from the same map used for the checksum (toExpressApiPayload) so the request body
    // sent over the wire can never drift from what was actually checksummed.
    val payload = toExpressApiPayload(includeChecksum = false)
    val googlePayValue = payload["xp_googlepay"] as? JsonObject
    // Only present when googlePayTokenAsBase64 is on (see putChannel) — a base64-encoded copy of
    // googlePayValue sent alongside it under a separate key, not a replacement.
    val googlePay64Value = payload["xp_googlepay64"] as? String
    val samsungPayValue = payload["xp_samsungpay"] as? JsonObject
    val applePayValue = payload["xp_applepay"] as? JsonObject
    return PaymentExpressRequestDto(
        merchantId = payload["mp_merchant_ID"] as String?,
        amount = payload["mp_amount"] as String?,
        appName = payload["mp_app_name"] as String?,
        orderId = payload["mp_order_ID"] as String?,
        currency = payload["mp_currency"] as String?,
        country = payload["mp_country"] as String?,
        billName = payload["mp_bill_name"] as String?,
        billEmail = payload["mp_bill_email"] as String?,
        billMobile = payload["mp_bill_mobile"] as String?,
        billDescription = payload["mp_bill_description"] as String?,
        channel = payload["mp_channel"] as String?,
        channelType = payload["mp_channel_type"] as String?,
        checksum = checksum,
        googlePay = googlePayValue,
        googlePay64 = googlePay64Value,
        samsungPay = samsungPayValue,
        applePayToken = applePayValue
    ) }


