package com.fiuu.xdkandroid.googlePay

import com.google.android.gms.wallet.WalletConstants

object GooglePayConfig {
    // Driven by mp_sandbox_mode (see PaymentRequest.resolveBuildMode) so it stays in sync with
    // ApiConfig.baseUrl — a host app can't end up hitting production API but the Wallet TEST
    // environment, or vice versa.
    fun walletEnvironment(buildMode: Int): Int =
        if (buildMode == 2) WalletConstants.ENVIRONMENT_PRODUCTION else WalletConstants.ENVIRONMENT_TEST

    // From Fiuu
    const val merchantName: String = "molpay"
    const val merchantId: String = "molpay"

    val isReadyToPayAllowedMethodsJson: String
        get() =
            """
            {
              "apiVersion": 2,
              "apiVersionMinor": 0,
              "allowedPaymentMethods": [{
                "type": "CARD",
                "parameters": {
                  "allowedAuthMethods": ["PAN_ONLY", "CRYPTOGRAM_3DS"],
                  "allowedCardNetworks": ["MASTERCARD", "VISA", "AMEX"],
                  "assuranceDetailsRequired": true
                }
              }]
            }
            """.trimIndent()

    // gatewayMerchantId identifies the merchant to FIUU's own gateway backend — it is NOT the
    // host app's Google-assigned merchant ID (mp_gp_merchant_ID, used for merchantInfo.merchantId
    // in GooglePayHandler.paymentDataJson instead). Confirmed against the legacy native XDK's
    // UtilGP.java, which hardcodes this to the literal "molpay" unconditionally, for every
    // merchant, in every environment — never derived per-host-app. Callers should not override
    // this default; a prior version of GooglePayHandler mistakenly passed the host app's own
    // Google merchant ID here instead, which produced a token Fiuu's backend couldn't decrypt
    // (opaque 500 from /process or /express), confirmed via device logs on 2026-09-04.
    //
    // assuranceDetailsRequired: true — matches the legacy native XDK's UtilGP.java. Google Pay
    // only includes paymentMethodData.info.assuranceDetails (which carries accountVerified /
    // cardHolderAuthenticated) for cards it can actually verify; without this flag it can still
    // return a PAN_ONLY card that lacks that object entirely. Fiuu's /process endpoint requires
    // googlePay.paymentMethodData.info.accountVerified, so an unfiltered PAN_ONLY card fails
    // there with a 400 ("accountVerified is required"). This flag makes Google Pay only surface
    // cards where assurance details are actually available, instead of narrowing
    // allowedAuthMethods down to CRYPTOGRAM_3DS-only (which would exclude PAN_ONLY cards
    // wholesale, even ones that do carry assurance details).
    fun allowedPaymentMethodsForButton(gatewayMerchantId: String = merchantId): String =
        """
            [{
              "type": "CARD",
              "parameters": {
                "allowedAuthMethods": ["PAN_ONLY", "CRYPTOGRAM_3DS"],
                "allowedCardNetworks": ["MASTERCARD", "VISA", "AMEX"],
                "assuranceDetailsRequired": true
              },
              "tokenizationSpecification": {
                "type": "PAYMENT_GATEWAY",
                "parameters": {
                  "gateway": "$merchantName",
                  "gatewayMerchantId": "$gatewayMerchantId"
                }
              }
            }]
            """.trimIndent()
}