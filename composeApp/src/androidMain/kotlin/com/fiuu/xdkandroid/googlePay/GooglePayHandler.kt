package com.fiuu.xdkandroid.googlePay

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import com.fiuu.xdkandroid.platform.AndroidWebViewBridge
import com.fiuu.xdkandroid.platform.ExpressProcessingOverlay
import com.fiuu.xdkandroid.platform.WebViewBridge
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import org.json.JSONObject

actual class GooglePayHandler actual constructor(activity: Any) {

    private val activityRef: Activity =
        activity as? Activity
            ?: throw IllegalArgumentException("GooglePayHandler requires android.app.Activity")

    // Environment isn't known until a payment's params are read (mp_sandbox_mode), so this can't
    // be a fixed lazy singleton the way it used to be — rebuilt per call instead. Wallet.getPaymentsClient
    // just constructs a local client object (no I/O), so this is cheap.
    private fun paymentsClient(buildMode: Int): PaymentsClient =
        Wallet.getPaymentsClient(
            activityRef,
            Wallet.WalletOptions.Builder()
                .setEnvironment(GooglePayConfig.walletEnvironment(buildMode))
                .build(),
        )

    private var pendingResult: ((GooglePayPaymentResult) -> Unit)? = null
    private var launchIntentSender: ((IntentSenderRequest) -> Unit)? = null
    // Captured per loadPaymentData() call so onSenderResult (invoked later, once the sheet's
    // IntentSender resolves) can tell whether a RESULT_ERROR happened against the PRODUCTION
    // wallet environment.
    private var lastBuildMode: Int? = null

    fun registerLauncher(launch: (IntentSenderRequest) -> Unit) {
        launchIntentSender = launch
    }

    fun unregisterLauncher() {
        launchIntentSender = null
    }

    fun onSenderResult(resultCode: Int, data: Intent?) {
        val pending = pendingResult
        pendingResult = null
        when (resultCode) {
            Activity.RESULT_OK -> {
                val paymentData = data?.let { PaymentData.getFromIntent(it) }
                if (paymentData == null) {
                    pending?.invoke(GooglePayPaymentResult.Error("No PaymentData"))
                } else {
                    pending?.invoke(GooglePayPaymentResult.Success(paymentData.toJson()))
                }
            }
            Activity.RESULT_CANCELED -> pending?.invoke(GooglePayPaymentResult.Cancelled)
            // AutoResolveHelper.RESULT_ERROR (1): the Google Pay sheet itself failed. The bare
            // resultCode alone ("Google Pay failed (resultCode=1)") is not actionable — the real
            // reason (e.g. DEVELOPER_ERROR when the host app's package name + signing SHA-1 isn't
            // registered for the PRODUCTION environment in the Google Pay & Wallet Console, which
            // TEST doesn't enforce) is only in the Status Google attaches to the result intent.
            AutoResolveHelper.RESULT_ERROR -> {
                val status = AutoResolveHelper.getStatusFromIntent(data)
                val message = "Google Pay error ${status?.statusCode}: ${status?.statusMessage ?: "Unknown error"}"
                pending?.invoke(
                    GooglePayPaymentResult.Error(productionSigningMismatchHint()?.let { "$message. $it" } ?: message)
                )
            }
            else -> pending?.invoke(GooglePayPaymentResult.Error("Google Pay failed (resultCode=$resultCode)"))
        }
    }

    // Google Pay's PRODUCTION environment validates the calling app's signing certificate (SHA-1)
    // against what's registered in the Google Pay & Wallet Console; TEST does not enforce this.
    // A debug-signed (debuggable) build hitting PRODUCTION fails here with an opaque
    // DEVELOPER_ERROR/resultCode=1 that gives no hint why on its own — flag the likely cause
    // instead of leaving the host app to rediscover it. Not a real proof of a signing mismatch
    // (ApplicationInfo.FLAG_DEBUGGABLE only reflects android:debuggable, not the specific signing
    // key used), so it's phrased as a hint appended to the real status, not a replacement for it.
    private fun productionSigningMismatchHint(): String? {
        val buildMode = lastBuildMode ?: return null
        if (GooglePayConfig.walletEnvironment(buildMode) != WalletConstants.ENVIRONMENT_PRODUCTION) return null
        val isDebuggable = (activityRef.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!isDebuggable) return null
        return "This is a debug-signed build running against Google Pay's PRODUCTION environment — " +
            "production requires the app's signing certificate to be registered in the Google Pay " +
            "& Wallet Console. Test with a release build signed by that registered keystore, or use " +
            "a sandbox/UAT preset (TEST environment) for debug builds."
    }

    actual fun isReadyToPay(buildMode: Int, callback: (Boolean) -> Unit) {
        val request = IsReadyToPayRequest.fromJson(GooglePayConfig.isReadyToPayAllowedMethodsJson)
        paymentsClient(buildMode)
            .isReadyToPay(request)
            .addOnSuccessListener { callback(it == true) }
            .addOnFailureListener { callback(false) }
    }

    actual fun loadPaymentData(
        amountMinorUnits: Long,
        currencyCode: String,
        merchantId: String?,
        buildMode: Int,
        onResult: (GooglePayPaymentResult) -> Unit,
    ) {
        pendingResult = onResult
        lastBuildMode = buildMode
        val request = try {
            PaymentDataRequest.fromJson(
                paymentDataJson("%.2f".format(amountMinorUnits / 100.0), currencyCode, merchantId)
            )
        } catch (e: Exception) {
            // Built from GooglePayConfig/static JSON, but fromJson throws on malformed JSON —
            // without this it would crash synchronously on the PayButton click instead of
            // surfacing as a normal GooglePayPaymentResult.Error.
            pendingResult = null
            onResult(GooglePayPaymentResult.Error(e.message ?: "Invalid Google Pay request"))
            return
        }
        paymentsClient(buildMode).loadPaymentData(request).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val pending = pendingResult
                pendingResult = null
                pending?.invoke(GooglePayPaymentResult.Success(task.result.toJson()))
            } else {
                when (val e = task.exception) {
                    is ResolvableApiException -> {
                        val launcher = launchIntentSender
                        if (launcher != null) {
                            launcher(IntentSenderRequest.Builder(e.resolution).build())
                        } else {
                            try {
                                e.startResolutionForResult(activityRef, REQUEST_CODE_GOOGLE_PAY)
                            } catch (resolveEx: Exception) {
                                val pending = pendingResult
                                pendingResult = null
                                pending?.invoke(GooglePayPaymentResult.Error(resolveEx.message ?: "Could not resolve Google Pay sheet"))
                            }
                        }
                    }
                    else -> {
                        val pending = pendingResult
                        pendingResult = null
                        pending?.invoke(GooglePayPaymentResult.Error(e?.message ?: "Unknown error"))
                    }
                }
            }
        }
    }

    private fun paymentDataJson(minorUnitsString: String, currencyCode: String, merchantId: String?): String {
        val resolvedMerchantId = merchantId?.takeIf { it.isNotBlank() } ?: GooglePayConfig.merchantId
        val merchantInfo = JSONObject()
            .put("merchantName", GooglePayConfig.merchantName)
        if (resolvedMerchantId.isNotBlank() &&
            resolvedMerchantId != "REPLACE_WITH_GOOGLE_MERCHANT_ID"
        ) {
            merchantInfo.put("merchantId", resolvedMerchantId)
        }

        val request = JSONObject()
            .put("apiVersion", 2)
            .put("apiVersionMinor", 0)
            .put("merchantInfo", merchantInfo)
            .put(
                "allowedPaymentMethods",
                // Deliberately NOT passing resolvedMerchantId here — that's the host app's own
                // Google-assigned merchant ID (merchantInfo.merchantId above), a different value
                // from tokenizationSpecification.parameters.gatewayMerchantId, which identifies
                // the merchant to Fiuu's gateway backend and must stay Fiuu's fixed default (see
                // GooglePayConfig.allowedPaymentMethodsForButton's doc comment). Passing the
                // former in place of the latter tags the token with a gatewayMerchantId Fiuu's
                // backend doesn't recognize, which it fails to decrypt — surfacing as an opaque
                // 500 from /process or /express, confirmed via device logs on 2026-09-04.
                JSONObject("{" +
                    "\"methods\":${GooglePayConfig.allowedPaymentMethodsForButton()}" +
                    "}")
                    .getJSONArray("methods"),
            )
            .put(
                "transactionInfo",
                JSONObject()
                    .put("totalPriceStatus", "FINAL")
                    .put("totalPrice", minorUnitsString)
                    .put("currencyCode", currencyCode)
                    .put("checkoutOption", "COMPLETE_IMMEDIATE_PURCHASE"),
            )
        return request.toString()
    }

    actual fun defaultWebViewBridge(): WebViewBridge? =
        (activityRef as? ComponentActivity)?.let { AndroidWebViewBridge(it) }

    private val expressProcessingOverlay by lazy { ExpressProcessingOverlay(activityRef) }

    internal actual fun showExpressProcessing() = expressProcessingOverlay.show()

    internal actual fun hideExpressProcessing() = expressProcessingOverlay.hide()

    companion object {
        const val REQUEST_CODE_GOOGLE_PAY = 9912
    }
}
