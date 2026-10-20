package com.fiuu.xdkandroid.googlePay

import android.app.Activity
import android.content.Intent
import com.fiuu.xdkandroid.data.remote.dto.PaymentResponseDto
import com.fiuu.xdkandroid.utils.FiuuLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json

/**
 * Native bridge allowing legacy Android Webcore (PaymentActivity) to delegate Google Pay sheet
 * presentation and /express settlement directly to modern KMP GooglePayHandler.
 */
object WebcoreGooglePayBridge {
    private var activeHandler: GooglePayHandler? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @JvmStatic
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == GooglePayHandler.REQUEST_CODE_GOOGLE_PAY) {
            activeHandler?.onSenderResult(resultCode, data)
            return true
        }
        return false
    }

    @JvmStatic
    fun startGooglePay(
        activity: Activity,
        params: Map<String, Any?>,
        onResult: (PaymentResponseDto) -> Unit,
        onCancel: () -> Unit,
        onError: (String) -> Unit
    ) {
        val amountStr = params["mp_amount"]?.toString() ?: "0"
        val parsedAmount = amountStr.trim().toBigDecimalOrNull()
        if (parsedAmount == null || parsedAmount.signum() <= 0) {
            FiuuLogger.log("WebcoreGooglePayBridge: invalid mp_amount ($amountStr)")
            onError("Invalid payment amount")
            return
        }
        val amountMinorUnits = parsedAmount
            .setScale(2, java.math.RoundingMode.HALF_UP)
            .movePointRight(2)
            .toLong()
        val currency = params["mp_currency"]?.toString() ?: "MYR"

        FiuuLogger.log("WebcoreGooglePayBridge: Starting Google Pay (amount=$amountStr, currency=$currency)")

        val handler = GooglePayHandler(activity)
        activeHandler = handler

        presentGooglePayExpressButton(
            googlePayHandler = handler,
            params = params,
            amountMinorUnits = amountMinorUnits,
            currencyCode = currency,
            // No webViewBridge: presentGooglePayExpressButton falls back to one on this activity
            // (GooglePayHandler.defaultWebViewBridge) for REDIRECT results such as 3DS.
            coroutineScope = scope
        ) { result ->
            when (result) {
                is GooglePayExpressResult.PaymentReceived -> {
                    activeHandler = null
                    onResult(result.response)
                }
                GooglePayExpressResult.Cancelled -> {
                    activeHandler = null
                    onCancel()
                }
                is GooglePayExpressResult.SheetError -> {
                    activeHandler = null
                    onError(result.message)
                }
                GooglePayExpressResult.InProgress -> {
                    FiuuLogger.log("WebcoreGooglePayBridge: Google Pay token received, express processing in progress")
                }
            }
        }
    }

    @JvmStatic
    fun toJsonString(dto: PaymentResponseDto): String {
        return try {
            json.encodeToString(PaymentResponseDto.serializer(), dto)
        } catch (e: Exception) {
            """{"status":"${dto.status}","statusCheck":${dto.statusCheck},"message":"${dto.message}"}"""
        }
    }
}
