package com.fiuu.xdkandroid.data.remote

// Single source of truth for the API base URL — both platform HttpClientFactory actuals
// (androidMain/iosMain) and any commonMain code building a path off it (e.g. the Click to Pay
// URL in PaymentViewModel) should reference this rather than hardcoding the string again.
object ApiConfig {


    private const val UAT_BASE_URL: String = "https://uat-mobile.fiuu.com/pay/"
    private const val SANDBOX_BASE_URL: String = "https://sandbox-mobile.fiuu.com/pay/"
    private const val PRODUCTION_BASE_URL: String = "https://test-mobile.fiuu.com/pay/"

    private const val PRODUCTION_CHANNEL_URL: String = "https://pay.fiuu.com/MOLPay/API/InternalUsed/index.php?mod=mobile&opt=Payment/GetAvailableChannelById"
    private const val SANDBOX_CHANNEL_URL: String = "https://sandbox-payment.fiuu.com/MOLPay/API/InternalUsed/index.php?mod=mobile&opt=Payment/GetAvailableChannelById"
    private const val UAT_CHANNEL_URL: String = "https://uat-pay.fiuu.com/MOLPay/API/InternalUsed/index.php?mod=mobile&opt=Payment/GetAvailableChannelById"

    // buildMode comes from Map.resolveBuildMode() (mp_core_env → mp_build_mode → mp_sandbox_mode)
    // and must stay in sync with GooglePayConfig.walletEnvironment.
    fun baseUrl(buildMode: Int): String =
        when (buildMode) {
            4 -> SANDBOX_BASE_URL
            3 -> UAT_BASE_URL
            2 -> PRODUCTION_BASE_URL
            // Fail-safe: an unrecognized build mode must never resolve to production.
            else -> SANDBOX_BASE_URL
        }

    fun channelUrl(buildMode: Int): String =
        when (buildMode) {
            4 -> SANDBOX_CHANNEL_URL
            3 -> UAT_CHANNEL_URL
            2 -> PRODUCTION_CHANNEL_URL
            // Fail-safe: an unrecognized build mode must never resolve to production.
            else -> SANDBOX_CHANNEL_URL
        }
}
