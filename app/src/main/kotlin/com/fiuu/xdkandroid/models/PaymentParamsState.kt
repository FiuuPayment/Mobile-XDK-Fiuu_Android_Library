package com.fiuu.xdkandroid.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fiuu.xdkandroid.BuildConfig

enum class EnvironmentPreset(val label: String) {
    SANDBOX("Sandbox"),
    UAT("UAT"),
    PRODUCTION("Production")
}

enum class PaymentEngine(val coreEnv: Int, val label: String) {
    NATIVE(1, "XDK Native (Compose)"),
    WEBCORE(2, "Android XDK Webcore"),
}

class PaymentParamsState {
    // Payment Tab Fields
    var amount by mutableStateOf("1.01")
    var orderId by mutableStateOf(generateOrderId())
    var country by mutableStateOf("MY")
    var currency by mutableStateOf("MYR")
    var channel by mutableStateOf("multi")
    var expressMode by mutableStateOf(false)
    private var editingEnabledState by mutableStateOf(false)
    // Turning editing off clears the per-field locks, which only mean anything while it's on.
    var editingEnabled: Boolean
        get() = editingEnabledState
        set(value) {
            editingEnabledState = value
            if (!value) {
                billNameEditDisabled = false
                billEmailEditDisabled = false
                billMobileEditDisabled = false
                billDescriptionEditDisabled = false
            }
        }
    var billNameEditDisabled by mutableStateOf(false)
    var billEmailEditDisabled by mutableStateOf(false)
    var billMobileEditDisabled by mutableStateOf(false)
    var billDescriptionEditDisabled by mutableStateOf(false)
    var closeButtonDisplay by mutableStateOf(true)

    // Billing Tab Fields — defaults are (re)applied in applyPreset(), not here.
    var billDesc by mutableStateOf("XDK Payment Studio Test")
    var billName by mutableStateOf("")
    var billEmail by mutableStateOf("")
    var billMobile by mutableStateOf("")

    var selectedEnvironment by mutableStateOf(EnvironmentPreset.SANDBOX)
    var selectedEngine by mutableStateOf(PaymentEngine.NATIVE)

    // Merchant Tab Fields
    var merchantId by mutableStateOf("")
    var verificationKey by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var appName by mutableStateOf(BuildConfig.SANDBOX_APP_NAME.ifBlank { "mobile" })
    var sandboxMode by mutableStateOf(true)
    var buildMode by mutableStateOf(4)
    var devMode by mutableStateOf(false)
    var gpMerchantId by mutableStateOf("")

    val engineName: String
        get() = selectedEngine.label

    val hasRequiredMerchantFields: Boolean
        get() = merchantId.isNotBlank() && verificationKey.isNotBlank() && username.isNotBlank() && password.isNotBlank() && appName.isNotBlank()

    init {
        applyPreset(EnvironmentPreset.SANDBOX)
    }

    fun refreshOrderId() {
        orderId = generateOrderId()
    }

    companion object {
        fun generateOrderId(): String {
            return "ORD-${System.currentTimeMillis().toString().takeLast(8)}"
        }
    }

    fun onCountryChange(newCountry: String) {
        country = newCountry
        currency = if (newCountry == "SG") "SGD" else "MYR"
    }

    fun selectEnvironment(env: EnvironmentPreset) {
        applyPreset(env)
    }

    fun applyPreset(env: EnvironmentPreset) {
        selectedEnvironment = env

        when (env) {
            EnvironmentPreset.SANDBOX -> {
                merchantId = BuildConfig.SANDBOX_MERCHANT_ID
                verificationKey = BuildConfig.SANDBOX_VERIFICATION_KEY
                username = BuildConfig.SANDBOX_USERNAME
                password = BuildConfig.SANDBOX_PASSWORD
                appName = BuildConfig.SANDBOX_APP_NAME.ifBlank { "mobile" }
                gpMerchantId = BuildConfig.SANDBOX_GP_MERCHANT_ID
                sandboxMode = true
                devMode = true
                buildMode = 4
            }
            EnvironmentPreset.UAT -> {
                merchantId = BuildConfig.UAT_MERCHANT_ID
                verificationKey = BuildConfig.UAT_VERIFICATION_KEY
                username = BuildConfig.UAT_USERNAME
                password = BuildConfig.UAT_PASSWORD
                appName = BuildConfig.UAT_APP_NAME.ifBlank { "mobile" }
                gpMerchantId = BuildConfig.UAT_GP_MERCHANT_ID
                sandboxMode = false
                devMode = true
                buildMode = 3
            }
            EnvironmentPreset.PRODUCTION -> {
                merchantId = BuildConfig.PROD_MERCHANT_ID
                verificationKey = BuildConfig.PROD_VERIFICATION_KEY
                username = BuildConfig.PROD_USERNAME
                password = BuildConfig.PROD_PASSWORD
                appName = BuildConfig.PROD_APP_NAME.ifBlank { "mobile" }
                gpMerchantId = BuildConfig.PROD_GP_MERCHANT_ID
                sandboxMode = false
                // Unlike a real merchant host app, this Studio test app's whole purpose is
                // inspecting wire traffic — keep FiuuLogger's request/response logs (checksum
                // payload, EXPRESS/PROCESS REQUEST, etc., all gated by mp_dev_mode) visible in
                // Logcat even under the Production preset. Still user-toggleable via the Dev Mode
                // switch if someone wants to verify production-silent behavior.
                devMode = true
                buildMode = 2
            }
        }

        // Same test payer regardless of environment — unlike merchant credentials above,
        // there's no per-env variant, so just reset to the .env-configured default on every
        // preset switch.
        billName = BuildConfig.BILL_NAME
        billEmail = BuildConfig.BILL_EMAIL
        billMobile = BuildConfig.BILL_MOBILE
    }

    fun toParamsMap(): Map<String, Any?> {
        return mapOf(
            "mp_amount" to amount,
            "mp_order_ID" to orderId,
            "mp_currency" to currency,
            "mp_country" to country,
            "mp_channel" to channel,
            "mp_bill_name" to billName,
            "mp_bill_email" to billEmail,
            "mp_bill_mobile" to billMobile,
            "mp_bill_description" to billDesc,
            "mp_merchant_ID" to merchantId,
            "mp_gp_merchant_ID" to gpMerchantId,
            "mp_verification_key" to verificationKey,
            "mp_build_mode" to buildMode,
            "mp_core_env" to selectedEngine.coreEnv,
            "mp_username" to username,
            "mp_password" to password,
            "mp_app_name" to appName,
            "mp_sandbox_mode" to sandboxMode.toString(),
            "mp_dev_mode" to devMode,
            "mp_express_mode" to expressMode,
            "mp_editing_enabled" to editingEnabled,
            "mp_bill_name_edit_disabled" to billNameEditDisabled,
            "mp_bill_email_edit_disabled" to billEmailEditDisabled,
            "mp_bill_mobile_edit_disabled" to billMobileEditDisabled,
            "mp_bill_description_edit_disabled" to billDescriptionEditDisabled,
            "mp_closebutton_display" to closeButtonDisplay,
            "device_info" to "Android Studio Test Suite"
        )
    }
}
