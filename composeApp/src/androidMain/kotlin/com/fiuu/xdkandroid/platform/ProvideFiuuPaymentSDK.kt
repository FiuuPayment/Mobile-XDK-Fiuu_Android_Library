package com.fiuu.xdkandroid.platform

import FiuuPaymentSDK
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.fiuu.xdkandroid.data.remote.dto.PaymentResponseDto
import com.fiuu.xdkandroid.domain.entity.reportParamWarnings
import com.fiuu.xdkandroid.domain.entity.resolveCoreEnv
import com.fiuu.xdkandroid.domain.resumePaymentStatus
import com.fiuu.xdkandroid.googlePay.GooglePayHandler
import com.fiuu.xdkandroid.samsungPay.SamsungPayHandler
import localGooglePayment
import localImageSaver
import localNetworkConnectivityChecker
import localSamsungPayment
import localSamsungPayReadiness
import localSecureTokenStore
import localWebViewBridge

@Composable
fun ProvideFiuuPaymentSDK(
    params: Map<String, Any?>,
    onPaymentResult: (PaymentResponseDto) -> Unit,
    onRequestClose: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) {
        context.findComponentActivity() ?: throw IllegalStateException("ProvideFiuuPaymentSDK must be used within a ComponentActivity")
    }

    androidx.compose.runtime.LaunchedEffect(activity) {
        com.fiuu.xdkandroid.telemetry.platform.AndroidTelemetryContext.bind(activity)
        com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.drainQueue()
    }

    // 1 = native engine; 2/3/4 = legacy Webcore — same parse resolveBuildMode() uses, so the
    // engine choice and the backend choice can't disagree (e.g. "03" was native here but UAT there)
    val coreEnv = params.resolveCoreEnv()
    val isLegacyWebcore = coreEnv != 1

    androidx.compose.runtime.LaunchedEffect(params) {
        params.reportParamWarnings()
    }

    if (isLegacyWebcore) {
        val webcoreLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val rawResult = result.data?.getStringExtra(com.fiuu.xdk.PaymentActivity.XDKTransactionResult)
            com.fiuu.xdkandroid.utils.FiuuLogger.log("ProvideFiuuPaymentSDK: Webcore finished resultCode=${result.resultCode}, rawResult=$rawResult")
            if (rawResult != null) {
                val responseDto = com.fiuu.xdkandroid.data.mapper.WebcoreResponseMapper.mapJsonToDto(rawResult)
                if (responseDto.status == "00" || responseDto.statusCheck) {
                    com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.logHostApproval(
                        process = com.fiuu.xdkandroid.telemetry.model.TelemetryProcess.ONLINE_PAYMENT_OUTCOME,
                        statusCode = responseDto.status
                    )
                } else {
                    com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.logHostDecline(
                        process = com.fiuu.xdkandroid.telemetry.model.TelemetryProcess.ONLINE_PAYMENT_OUTCOME,
                        statusCode = responseDto.status,
                        statusMessage = responseDto.message,
                        source = com.fiuu.xdkandroid.telemetry.model.TelemetryOrigin.EXTERNAL_HOST_BANK,
                        referenceNumber = responseDto.referenceNo ?: ""
                    )
                }
                onPaymentResult(responseDto)
            } else if (result.resultCode == android.app.Activity.RESULT_CANCELED) {
                com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.recordBreadcrumb(
                    area = "WEBCORE_ANDROID",
                    process = "resultCanceled",
                    message = "PaymentActivity closed by user"
                )
                onRequestClose()
            } else {
                onRequestClose()
            }
        }

        // Saved state, so it survives the host Activity being recreated while PaymentActivity is
        // in front (low memory, "Don't keep activities"). Without it, the recreated composition
        // re-ran this effect and opened a second payment page, while the first one's result was
        // still delivered to webcoreLauncher (whose registration is saved too).
        var webcoreLaunched by androidx.compose.runtime.saveable.rememberSaveable(params) {
            androidx.compose.runtime.mutableStateOf(false)
        }
        androidx.compose.runtime.LaunchedEffect(params) {
            if (webcoreLaunched) return@LaunchedEffect
            webcoreLaunched = true
            com.fiuu.xdkandroid.utils.FiuuLogger.log("ProvideFiuuPaymentSDK: Launching Legacy Webcore PaymentActivity (mp_core_env = $coreEnv)")
            val detailsMap = HashMap<String, Any?>(params)
            // PaymentActivity string-matches "2"/"3"/"4" and treats anything else as production
            detailsMap["mp_core_env"] = coreEnv.toString()
            detailsMap["is_submodule"] = true
            detailsMap["module_id"] = "fiuu-xdk-native-wrapper"
            detailsMap["wrapper_version"] = com.fiuu.xdkandroid.utils.BuildInfo.SDK_VERSION_NAME
            if (!detailsMap.containsKey("mp_closebutton_display")) {
                detailsMap["mp_closebutton_display"] = true
            }

            val intent = android.content.Intent(activity, com.fiuu.xdk.PaymentActivity::class.java).apply {
                putExtra(com.fiuu.xdk.PaymentActivity.XDKPaymentDetails, detailsMap)
            }
            webcoreLauncher.launch(intent)
        }
    } else {
        val webViewBridge = remember(activity) {
            AndroidWebViewBridge(activity)
        }

        val googlePayHandler = remember(activity) { GooglePayHandler(activity) }
        val samsungPayHandler = remember(activity) { SamsungPayHandler(activity) }
        val secureTokenStore = remember(activity) { AndroidSecureTokenStore(activity) }
        val imageSaver = remember(activity) { AndroidImageSaver(activity) }
        val networkConnectivityChecker = remember(activity) { AndroidNetworkConnectivityChecker(activity) }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            googlePayHandler.onSenderResult(result.resultCode, result.data)
        }

        DisposableEffect(googlePayHandler, launcher) {
            googlePayHandler.registerLauncher { request -> launcher.launch(request) }
            onDispose { googlePayHandler.unregisterLauncher() }
        }

        CompositionLocalProvider(
            localWebViewBridge provides webViewBridge,
            localGooglePayment provides { amountMinorUnits, currencyCode, merchantId, buildMode, onResult ->
                googlePayHandler.loadPaymentData(amountMinorUnits, currencyCode, merchantId, buildMode, onResult)
            },
            localSamsungPayment provides { amountMinorUnits, currencyCode, onResult ->
                samsungPayHandler.loadPaymentData(amountMinorUnits, currencyCode, onResult)
            },
            localSamsungPayReadiness provides { callback ->
                samsungPayHandler.isReadyToPay(callback)
            },
            localSecureTokenStore provides secureTokenStore,
            localImageSaver provides imageSaver,
            localNetworkConnectivityChecker provides networkConnectivityChecker
        ) {
            FiuuPaymentSDK(
                params = params,
                onPaymentResult = onPaymentResult,
                onRequestClose = onRequestClose,
            )
        }
    }
}

private fun Context.findComponentActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
}

/**
 * Resumes a status check for the most recently attempted payment interrupted mid-checkout (app
 * killed/backgrounded before a result was delivered). Returns null if no session is being tracked
 * (never started, already completed, or its 15-minute window already passed and was cleared). The
 * session's accessToken never leaves the SDK — it was persisted internally (encrypted,
 * AndroidKeyStore-backed) the moment /initiate or /express succeeded, and is only ever read back
 * out here.
 */
suspend fun resumeFiuuPaymentStatus(context: Context): PaymentResponseDto? {
    val store = AndroidSecureTokenStore(context.applicationContext)
    return resumePaymentStatus(store)
}
