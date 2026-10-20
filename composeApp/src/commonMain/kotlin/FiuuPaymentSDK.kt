import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fiuu.xdkandroid.components.CustomAlertDialog
import com.fiuu.xdkandroid.data.remote.HttpClientFactory
import com.fiuu.xdkandroid.data.remote.KtorApiService
import com.fiuu.xdkandroid.data.remote.dto.PaymentErrorDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentResponseDto
import com.fiuu.xdkandroid.data.remote.dto.shouldClearStoredSession
import com.fiuu.xdkandroid.data.repository.FiuuPaymentRepository
import com.fiuu.xdkandroid.domain.entity.resolveBuildMode
import com.fiuu.xdkandroid.domain.entity.toPaymentRequest
import com.fiuu.xdkandroid.googlePay.GooglePayPaymentResult
import com.fiuu.xdkandroid.samsungPay.SamsungPayPaymentResult
import com.fiuu.xdkandroid.platform.ImageSaver
import com.fiuu.xdkandroid.platform.NetworkConnectivityChecker
import com.fiuu.xdkandroid.platform.PlatformBackHandler
import com.fiuu.xdkandroid.platform.SecureTokenStore
import com.fiuu.xdkandroid.platform.WebViewBridge
import com.fiuu.xdkandroid.ui.PaymentMultiChannel
import com.fiuu.xdkandroid.ui.model.PaymentScreen
import com.fiuu.xdkandroid.utils.FiuuLogger
import com.fiuu.xdkandroid.utils.SESSION_TIMEOUT_SECONDS
import com.fiuu.xdkandroid.viewmodel.PaymentViewModel
import kotlin.math.roundToLong
import kotlinx.coroutines.delay


val localWebViewBridge = staticCompositionLocalOf<WebViewBridge> {
    error("WebViewBridge not provided")
}

val localGooglePayment = staticCompositionLocalOf<((amountMinorUnits: Long, currencyCode: String, merchantId: String?, buildMode: Int, onResult: (GooglePayPaymentResult) -> Unit) -> Unit)?> {
    null
}

val localSamsungPayment = staticCompositionLocalOf<((amountMinorUnits: Long, currencyCode: String, onResult: (SamsungPayPaymentResult) -> Unit) -> Unit)?> {
    null
}

val localSamsungPayReadiness = staticCompositionLocalOf<((Boolean) -> Unit) -> Unit> {
    { callback -> callback(false) }
}

val localGooglePayReadiness = staticCompositionLocalOf<(buildMode: Int, callback: (Boolean) -> Unit) -> Unit> {
    { _, callback -> callback(false) }
}

val localSecureTokenStore = staticCompositionLocalOf<SecureTokenStore> {
    error("SecureTokenStore not provided")
}

val localImageSaver = staticCompositionLocalOf<ImageSaver?> { null }

val localNetworkConnectivityChecker = staticCompositionLocalOf<NetworkConnectivityChecker> {
    error("NetworkConnectivityChecker not provided")
}

@Composable
fun FiuuPaymentSDK(
    params: Map<String, Any?>,
    onPaymentResult: (PaymentResponseDto) -> Unit,
    onRequestClose: () -> Unit,
) {
    val webViewBridge = localWebViewBridge.current
    val googlePayPayment = localGooglePayment.current
    val samsungPayPayment = localSamsungPayment.current
    val checkSamsungPayReady = localSamsungPayReadiness.current
    val checkGooglePayReady = localGooglePayReadiness.current
    val secureTokenStore = localSecureTokenStore.current
    val imageSaver = localImageSaver.current
    val networkConnectivityChecker = localNetworkConnectivityChecker.current

    // Keyed by build mode so switching environment (sandbox/UAT/production) across recompositions
    // rebuilds the PaymentViewModel — and its KtorApiService/HttpClient, which bakes the base host
    // in at construction — instead of keeping the client pointed at whichever host was resolved
    // the first time this composable entered composition.
    val vm = remember(params.resolveBuildMode()) { PaymentViewModel(FiuuPaymentRepository(
        KtorApiService(
            HttpClientFactory(),
            params.resolveBuildMode()
        )
    )) }
    DisposableEffect(vm) {
        onDispose { vm.dispose() }
    }

    // Catches the return trip from a native e-wallet handoff (TNG, Boost, etc. — see
    // AndroidWebViewBridge.handleNonHttpScheme/handleEwalletUniversalLink) without requiring the
    // host app to register any custom deep link scheme: whenever this app comes back to the
    // foreground, ask the ViewModel to silently confirm via /inquire whether the attempt already
    // reached a terminal state while backgrounded. checkPaymentStatusOnResume() itself is a no-op
    // unless a payment is actually in flight (stream active, or dropped without a result — then it
    // reconnects) and the backend has genuinely
    // resolved (status != null), so this is safe to fire on every resume, including the initial
    // composition.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> vm.checkPaymentStatusOnResume()
                Lifecycle.Event.ON_PAUSE -> vm.onAppPaused()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val uiState by vm.uiState.collectAsState()
    var screen by remember { mutableStateOf<PaymentScreen>(PaymentScreen.Methods) }
    var isLoading by remember { mutableStateOf(true) }
    val secondsRemainingState = remember { mutableStateOf(SESSION_TIMEOUT_SECONDS) }

    // While loading is up (e.g. a close confirming via /inquire), closing the SDK would dispose the
    // ViewModel mid-request and the host would get no result. Reads the ViewModel's current value,
    // not the composed uiState: a tap or back queued right after loading is set is dispatched
    // before the next recomposition, and the composed value would still say false (seen on device).
    fun closeBlockedByLoading(source: String): Boolean {
        val state = vm.uiState.value
        if (state.loading) FiuuLogger.log("XDK_RESULT $source ignored while loading: ${state.loadingMessage}")
        return state.loading
    }
    val requestCloseUnlessLoading: () -> Unit = {
        if (!closeBlockedByLoading("close")) onRequestClose()
    }

    PlatformBackHandler(enabled = true) {
        if (uiState.awaitingWalletConfirmation) {
            vm.cancelWalletConfirmation()
        } else if (closeBlockedByLoading("back")) {
            // ignored — see closeBlockedByLoading
        } else if (screen !is PaymentScreen.Methods) {
            screen = PaymentScreen.Methods
        } else {
            com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.recordBreadcrumb("SDK", "backHandler", "User backed out or requested close")
            onRequestClose()
        }
    }

    LaunchedEffect(Unit) {
        while (secondsRemainingState.value > 0) {
            delay(1000L)
            secondsRemainingState.value--
        }
        com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.recordBreadcrumb(
            area = "SDK",
            process = "sessionTimeout",
            message = "${SESSION_TIMEOUT_SECONDS}-second countdown expired, invoking vm.onSessionExpired()"
        )
        // The ViewModel decides the result: one /inquire if an attempt is in flight (the user may
        // have just approved), else the 408 (sessionTimeoutResult). It arrives via paymentSuccess
        // below, like every other result, so the host gets exactly one.
        vm.onSessionExpired()
    }

    // Stable lambda — no keys in remember, so same object reference across recompositions.
    // State is only read when the lambda is invoked inside PaymentScaffold's SessionTimerBar,
    // so only that bar recomposes each tick, not this entire composable.
    val secondsRemaining: () -> Int = remember { { secondsRemainingState.value } }

    LaunchedEffect(params) {
        isLoading = true
        com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.clearBreadcrumbs()
        com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.recordBreadcrumb("SDK", "init", "FiuuPaymentSDK started orderId=${params["mp_order_ID"]}")
        vm.attachWebViewBridge(webViewBridge)
        vm.attachSecureTokenStore(secureTokenStore)
        vm.attachNetworkConnectivityChecker(networkConnectivityChecker)

        try{
            vm.onStartInitiate(params)
        } finally {
            isLoading = false
        }
    }

    // Several ViewModel paths can each write paymentSuccess for the same attempt (e.g. the resume
    // /inquire and the stream both resolving after a TNG hand-off) — the host gets only the first.
    // Errors claim it too: every error is delivered and closes the SDK, same as a success.
    var resultDelivered by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.paymentSuccess) {
        val written = uiState.paymentSuccess ?: return@LaunchedEffect
        if (resultDelivered) {
            FiuuLogger.log("XDK_RESULT Dropping duplicate result status=${written.status}")
            return@LaunchedEffect
        }
        resultDelivered = true
        // statusCheck is derived from status here, the one place every result reaches the host.
        // The writers used to disagree (an OR across stream + /inquire, HTTP 200 on a direct
        // /process RESPONSE, false on a raw stream "00"), so a host checking statusCheck could
        // read a paid order as failed — or a failed one as paid (COR-05).
        val streamResult = written.copy(statusCheck = written.status == "00")
        // "22" and the 408 timeout are unresolved — keep the session for resumeFiuuPaymentStatus.
        if (shouldClearStoredSession(streamResult.status)) vm.clearStoredSession()
        if (streamResult.statusCheck) {
            com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.logHostApproval(
                process = com.fiuu.xdkandroid.telemetry.model.TelemetryProcess.ONLINE_PAYMENT_OUTCOME,
                statusCode = streamResult.status
            )
        } else {
            com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.logTerminalOutcome(
                status = streamResult.status,
                message = streamResult.message,
                process = com.fiuu.xdkandroid.telemetry.model.TelemetryProcess.ONLINE_PAYMENT_OUTCOME,
                referenceNumber = streamResult.referenceNo ?: "",
                merchantId = params["mp_merchant_ID"]?.toString() ?: "",
                countryCode = params["mp_country"]?.toString() ?: "MY",
            )
        }
        onPaymentResult(streamResult)
        onRequestClose()
    }

    LaunchedEffect(uiState.paymentError) {
        val error = uiState.paymentError ?: return@LaunchedEffect
        // Outside express mode, cancelling the Apple Pay sheet is shown as PaymentMultiChannel's
        // "Payment Not Completed" dialog, and dismissing it returns to the channel list — the same
        // soft return Google Pay and Samsung Pay give a non-express cancel. Treating it as fatal
        // here closed the whole SDK before that dialog could be read. An Apple Pay *error* still
        // ends the session, like GOOGLE_PAY_ERROR / SAMSUNG_PAY_ERROR.
        if (error is com.fiuu.xdkandroid.ui.model.PaymentError.ApplePayError && error.cancelled && !uiState.expressMode) {
            return@LaunchedEffect
        }
        if (resultDelivered) {
            FiuuLogger.log("XDK_RESULT Dropping error after result was delivered: ${error.code}")
            return@LaunchedEffect
        }
        resultDelivered = true
        vm.clearStoredSession()
        val customError = error as? com.fiuu.xdkandroid.ui.model.PaymentError.CustomError
        val errorMessage = customError?.msg ?: error.code
        val errorCode = customError?.errorCode ?: customError?.statusCode?.toString() ?: error.code

        com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.logTerminalOutcome(
            status = error.code,
            message = errorMessage,
            source = com.fiuu.xdkandroid.telemetry.model.TelemetryOrigin.INTERNAL_SDK,
            referenceNumber = params["mp_order_ID"]?.toString() ?: "",
            merchantId = params["mp_merchant_ID"]?.toString() ?: "",
            countryCode = params["mp_country"]?.toString() ?: "MY",
        )

        onPaymentResult(
            PaymentResponseDto(
                status = error.code,
                statusCheck = false,
                message = errorMessage,
                errorCode = errorCode,
                errorDetails = PaymentErrorDto(
                    code = errorCode,
                    message = errorMessage,
                    type = "ERROR"
                )
            )
        )
        onRequestClose()
    }

    val googlePayTriggered by vm.googlePayTrigger.collectAsState()
    LaunchedEffect(googlePayTriggered) {
        if (!googlePayTriggered) return@LaunchedEffect
        vm.consumeGooglePayTrigger()
        val amountMinorUnits = params["mp_amount"]?.toString()?.toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
        val currencyCode = params["mp_currency"]?.toString() ?: ""
        val gpMerchantId = params["mp_gp_merchant_ID"]?.toString()
        val buildMode = params.resolveBuildMode()
        googlePayPayment?.invoke(amountMinorUnits, currencyCode, gpMerchantId, buildMode) { result ->
            vm.handleGooglePayResult(result)
        }
    }

    LaunchedEffect(Unit) {
        checkSamsungPayReady { ready -> vm.setSamsungPayReady(ready) }
        checkGooglePayReady(params.resolveBuildMode()) { ready -> vm.setGooglePayReady(ready) }
        vm.checkApplePayReady()
    }

    val samsungPayTriggered by vm.samsungPayTrigger.collectAsState()
    LaunchedEffect(samsungPayTriggered) {
        if (!samsungPayTriggered) return@LaunchedEffect
        vm.consumeSamsungPayTrigger()
        val amountMinorUnits = params["mp_amount"]?.toString()?.toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
        val currencyCode = params["mp_currency"]?.toString() ?: ""
        samsungPayPayment?.invoke(amountMinorUnits, currencyCode) { result ->
            vm.handleSamsungPayResult(result)
        }
    }

    //UI logic for loading
    //check params if express right away startPayment else open paymentflow

    Box(modifier = Modifier.fillMaxSize()) {
        val paymentRequest = remember(params){
            params.toPaymentRequest()
        }

            PaymentMultiChannel(
                vm = vm,
                paymentRequest = paymentRequest,
                screen = screen,
                showAppBar = paymentRequest.closeButtonDisplay,
                onScreenChange = { screen = it },
                onRequestClose = requestCloseUnlessLoading,
                secondsRemaining = secondsRemaining,
                imageSaver = imageSaver

            )

        if (uiState.awaitingWalletConfirmation) {
            WalletConfirmationOverlay(onCancel = vm::cancelWalletConfirmation)
        } else if (isLoading || uiState.loading) {
            FullScreenLoadingOverlay(uiState.loadingMessage)
        } else if (uiState.paymentSuccess != null) {
            // A result means the SDK is closing (onRequestClose runs a frame later, then the
            // host's dismiss animation). Keep blocking input meanwhile — paths that clear `loading`
            // alongside the result otherwise exposed the screen underneath, Pay button and all.
            FullScreenLoadingOverlay("Closing...")
        }

        if (uiState.isRooted) {
            CustomAlertDialog(
                title = "Security Alert",
                message = "This device appears to be rooted. For security reasons, this application will now close.",
                cancelable = false,
                onDismiss = {
                    if (resultDelivered) {
                        FiuuLogger.log("XDK_RESULT Dropping root-device result after result was delivered")
                        return@CustomAlertDialog
                    }
                    resultDelivered = true
                    onPaymentResult(
                        PaymentResponseDto(
                            status = "SECURITY_ERROR",
                            statusCheck = false,
                            message = "Device is rooted/jailbroken",
                            errorCode = "DEVICE_ROOTED",
                            errorDetails = PaymentErrorDto(
                                code = "DEVICE_ROOTED",
                                message = "Device is rooted/jailbroken",
                                type = "SECURITY"
                            )
                        )
                    )
                    onRequestClose()
                }
            )
        }
    }
}

// Shown when the user is back from a wallet app (TNG etc.) but the payment is still pending —
// the SDK keeps waiting for the result, and Cancel lets them stop (see
// PaymentViewModel.cancelWalletConfirmation).
@Composable
fun WalletConfirmationOverlay(onCancel: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit){},
        contentAlignment = Alignment.Center
    ) {
        Column (horizontalAlignment = Alignment.CenterHorizontally){
            CircularProgressIndicator(color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Waiting for payment confirmation...", color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = onCancel) {
                Text(text = "Cancel", color = Color.White)
            }
        }
    }
}

@Composable
fun FullScreenLoadingOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit){},
        contentAlignment = Alignment.Center
    ) {
        Column (horizontalAlignment = Alignment.CenterHorizontally){
            CircularProgressIndicator(color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = message, color = Color.White)
        }
    }
}

