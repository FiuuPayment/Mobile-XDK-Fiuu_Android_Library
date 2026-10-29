package com.fiuu.xdkandroid

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.fiuu.xdkandroid.logger.LogLevel
import com.fiuu.xdkandroid.logger.TerminalLogManager
import com.fiuu.xdkandroid.models.PaymentParamsState
import com.fiuu.xdkandroid.ui.TestStudioApp
import com.fiuu.xdkandroid.data.remote.dto.PaymentResponseDto
import com.fiuu.xdkandroid.googlePay.GooglePayHandler
import com.fiuu.xdkandroid.googlePay.GooglePayExpressResult
import com.fiuu.xdkandroid.googlePay.presentGooglePayExpressButton
import com.fiuu.xdkandroid.platform.resumeFiuuPaymentStatus
import com.fiuu.xdkandroid.samsungPay.SamsungPayExpressResult
import com.fiuu.xdkandroid.samsungPay.SamsungPayHandler
import com.fiuu.xdkandroid.samsungPay.presentSamsungPayExpressButton
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.math.roundToLong

private val prettyJson = Json { prettyPrint = true }

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)

        setContent {
            com.fiuu.xdkandroid.theme.StudioTheme {
                val coroutineScope = rememberCoroutineScope()
            val state = remember { PaymentParamsState() }

            val googlePayHandler = remember { GooglePayHandler(this@MainActivity) }
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult()
            ) { result ->
                googlePayHandler.onSenderResult(result.resultCode, result.data)
            }
            DisposableEffect(googlePayHandler, launcher) {
                googlePayHandler.registerLauncher { request -> launcher.launch(request) }
                onDispose { googlePayHandler.unregisterLauncher() }
            }

            val samsungPayHandler = remember { SamsungPayHandler(this@MainActivity) }

            var isGooglePayReady by remember { mutableStateOf(false) }
            var isSamsungPayReady by remember { mutableStateOf(false) }

            LaunchedEffect(state.buildMode) {
                googlePayHandler.isReadyToPay(state.buildMode) { ready ->
                    isGooglePayReady = ready
                    TerminalLogManager.log(LogLevel.INIT, "GPAY", "Google Pay Readiness: $ready")
                }
                samsungPayHandler.isReadyToPay { ready ->
                    isSamsungPayReady = ready
                    TerminalLogManager.log(LogLevel.INIT, "SAMPAY", "Samsung Pay Readiness: $ready")
                }
            }

            TestStudioApp(
                state = state,
                isGooglePayReady = isGooglePayReady,
                isSamsungPayReady = isSamsungPayReady,
                onGooglePayClick = {
                    val amountMinor = state.amount.toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
                    TerminalLogManager.log(LogLevel.HTTP, "GPAY_START", "Requesting Google Pay sheet (amountMinor=$amountMinor)...")
                    presentGooglePayExpressButton(
                        googlePayHandler = googlePayHandler,
                        params = state.toParamsMap(),
                        amountMinorUnits = amountMinor,
                        currencyCode = state.currency,
                        coroutineScope = coroutineScope
                    ) { result ->
                        when (result) {
                            GooglePayExpressResult.InProgress -> {
                                TerminalLogManager.log(LogLevel.HTTP, "GPAY_TOKEN", "Google Pay token received, processing /express...")
                            }
                            is GooglePayExpressResult.PaymentReceived -> {
                                val json = prettyJson.encodeToString(PaymentResponseDto.serializer(), result.response)
                                TerminalLogManager.log(
                                    level = if (result.response.status == "00") LogLevel.STATUS else LogLevel.ERROR,
                                    tag = "GPAY_RESULT",
                                    message = "Status: ${result.response.status}",
                                    details = json
                                )
                                Toast.makeText(this@MainActivity, "Google Pay result received", Toast.LENGTH_SHORT).show()
                                state.refreshOrderId()
                                TerminalLogManager.log(LogLevel.STATUS, "ORDER_ID", "Auto-generated new Order ID: ${state.orderId}")
                            }
                            GooglePayExpressResult.Cancelled -> {
                                TerminalLogManager.log(LogLevel.USER, "GPAY_CANCEL", "Google Pay sheet cancelled by user")
                                state.refreshOrderId()
                            }
                            is GooglePayExpressResult.SheetError -> {
                                TerminalLogManager.log(LogLevel.ERROR, "GPAY_ERROR", result.message)
                            }
                        }
                    }
                },
                onSamsungPayClick = {
                    val amountMinor = state.amount.toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
                    TerminalLogManager.log(LogLevel.HTTP, "SAMPAY_START", "Requesting Samsung Pay sheet (amountMinor=$amountMinor)...")
                    presentSamsungPayExpressButton(
                        samsungPayHandler = samsungPayHandler,
                        params = state.toParamsMap(),
                        amountMinorUnits = amountMinor,
                        currencyCode = state.currency,
                        coroutineScope = coroutineScope
                    ) { result ->
                        when (result) {
                            SamsungPayExpressResult.InProgress -> {
                                TerminalLogManager.log(LogLevel.HTTP, "SAMPAY_TOKEN", "Samsung Pay token received, processing /express...")
                            }
                            is SamsungPayExpressResult.PaymentReceived -> {
                                val json = prettyJson.encodeToString(PaymentResponseDto.serializer(), result.response)
                                TerminalLogManager.log(
                                    level = if (result.response.status == "00") LogLevel.STATUS else LogLevel.ERROR,
                                    tag = "SAMPAY_RESULT",
                                    message = "Status: ${result.response.status}",
                                    details = json
                                )
                                Toast.makeText(this@MainActivity, "Samsung Pay result received", Toast.LENGTH_SHORT).show()
                                state.refreshOrderId()
                                TerminalLogManager.log(LogLevel.STATUS, "ORDER_ID", "Auto-generated new Order ID: ${state.orderId}")
                            }
                            SamsungPayExpressResult.Cancelled -> {
                                TerminalLogManager.log(LogLevel.USER, "SAMPAY_CANCEL", "Samsung Pay sheet cancelled by user")
                                state.refreshOrderId()
                            }
                            is SamsungPayExpressResult.SheetError -> {
                                TerminalLogManager.log(LogLevel.ERROR, "SAMPAY_ERROR", result.message)
                            }
                        }
                    }
                },
                onCheckStatusClick = {
                    coroutineScope.launch {
                        TerminalLogManager.log(LogLevel.KEYSTORE, "INQUIRE", "Reading KeyStore encrypted session & calling /inquire...")
                        val result = resumeFiuuPaymentStatus(this@MainActivity)
                        if (result != null) {
                            val json = prettyJson.encodeToString(PaymentResponseDto.serializer(), result)
                            TerminalLogManager.log(
                                level = if (result.status == "00") LogLevel.STATUS else LogLevel.ERROR,
                                tag = "INQUIRE_RESULT",
                                message = "Resumed Session Status: ${result.status} (tranID=${result.transactionId})",
                                details = json
                            )
                        } else {
                            TerminalLogManager.log(LogLevel.KEYSTORE, "INQUIRE_NULL", "No resumable payment session found in KeyStore")
                        }
                    }
                }
            )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        val status = uri.getQueryParameter("status") ?: uri.getQueryParameter("StatCode")
        val tranId = uri.getQueryParameter("tranID") ?: uri.getQueryParameter("TxnID")
        val amount = uri.getQueryParameter("amount")
        val currency = uri.getQueryParameter("currency")

        TerminalLogManager.log(
            level = if (status == "00") LogLevel.STATUS else LogLevel.ERROR,
            tag = "DEEPLINK_ROUTED",
            message = "MainActivity processed return deep link (status=$status, tranId=$tranId)",
            details = "Full URI: $uri\nAmount: $currency $amount\nAction: ${intent.action}"
        )
        Toast.makeText(this, "Deep Link Callback: status=$status, tranId=$tranId", Toast.LENGTH_LONG).show()
    }
}
