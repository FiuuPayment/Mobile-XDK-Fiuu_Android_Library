package com.fiuu.xdkandroid.googlePay

import com.fiuu.xdkandroid.data.remote.HttpClientFactory
import com.fiuu.xdkandroid.data.remote.KtorApiService
import com.fiuu.xdkandroid.data.remote.dto.PaymentErrorDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentResponseDto
import com.fiuu.xdkandroid.data.repository.FiuuPaymentRepository
import com.fiuu.xdkandroid.domain.entity.GOOGLE_PAY_TOKEN_AS_BASE64
import com.fiuu.xdkandroid.domain.entity.reportParamWarnings
import com.fiuu.xdkandroid.domain.entity.resolveBuildMode
import com.fiuu.xdkandroid.domain.entity.toPaymentRequest
import com.fiuu.xdkandroid.platform.WebViewBridge
import com.fiuu.xdkandroid.ui.model.PaymentError
import com.fiuu.xdkandroid.viewmodel.PaymentViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * Standalone Google Pay express-checkout entry point for host apps that render their own Google
 * Pay button outside ProvideFiuuPaymentSDK (see PayButton wiring in MainActivity) and just want a
 * result back, without showing the SDK's channel-selection UI.
 *
 * Reuses PaymentViewModel.expressPayment as-is — it already owns the /express checksum, signature
 * and request-type handling (REDIRECT opens a WebViewBridge, RESPONSE is synchronous). When
 * [webViewBridge] is null (the default, for callers with no WebView surface to show one on), a
 * REDIRECT/data result surfaces as paymentError (NO_WEBVIEW_BRIDGE) rather than hanging; passing
 * one lets a 3DS challenge or other REDIRECT actually open instead. presentGooglePayExpressButton
 * does that automatically via GooglePayHandler.defaultWebViewBridge.
 */
suspend fun expressGooglePay(
    params: Map<String, Any?>,
    googlePayResultJson: String,
    webViewBridge: WebViewBridge? = null
): PaymentResponseDto {
    params.reportParamWarnings()
    val vm = PaymentViewModel(FiuuPaymentRepository(KtorApiService(HttpClientFactory(), params.resolveBuildMode())))
    webViewBridge?.let { vm.attachWebViewBridge(it) }
    try {
        val googlePayData = Json.parseToJsonElement(googlePayResultJson).jsonObject
        // Forced true regardless of params["mp_express_mode"]: this function always drives
        // PaymentViewModel.expressPayment (the /express, mp_-prefixed flow) below, but
        // PaymentRequest.putChannel() picks its JSON key prefix (mp_channel vs xp_channel) off
        // this same expressMode field. Leaving it as whatever the host passed (meant for the
        // general "skip channel-selection UI" flag, not this dedicated express entry point) means
        // an absent/false mp_express_mode silently drops mp_channel/mp_channel_type from the wire
        // payload while everything else still goes out mp_-prefixed.
        val request = params.toPaymentRequest().copy(
            googlePayData = googlePayData,
            expressMode = true,
            googlePayTokenAsBase64 = GOOGLE_PAY_TOKEN_AS_BASE64
        )

        vm.expressPayment(request)

        val finalState = vm.uiState.first { it.paymentSuccess != null || it.paymentError != null }
        finalState.paymentSuccess?.let { return it }

        val error = finalState.paymentError ?: PaymentError.GenericError()
        val customError = error as? PaymentError.CustomError
        val errorMessage = customError?.msg ?: error.code
        val errorCode = customError?.errorCode ?: customError?.statusCode?.toString() ?: error.code
        return errorResponse(code = errorCode, message = errorMessage ?: error.code)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Covers malformed googlePayResultJson / params before vm.expressPayment's own
        // try/catch takes over — without this, the exception would escape this suspend
        // function into the caller's coroutine (see presentGooglePayExpressButton) unhandled.
        return errorResponse(code = "SDK_ERROR", message = e.message ?: "Google Pay express payment failed")
    } finally {
        vm.dispose()
    }
}

private fun errorResponse(code: String, message: String): PaymentResponseDto =
    PaymentResponseDto(
        status = code,
        statusCheck = false,
        message = message,
        errorCode = code,
        errorDetails = PaymentErrorDto(
            code = code,
            message = message,
            type = "ERROR"
        )
    )

/** Outcome of [presentGooglePayExpressButton], reported via its onResult callback. */
sealed interface GooglePayExpressResult {
    /** The Google Pay sheet returned a token and the /express call has started. */
    data object InProgress : GooglePayExpressResult
    data class PaymentReceived(val response: PaymentResponseDto) : GooglePayExpressResult
    data object Cancelled : GooglePayExpressResult
    data class SheetError(val message: String) : GooglePayExpressResult
}

/**
 * Drives a host-rendered Google Pay button end-to-end: presents the payment sheet via
 * [googlePayHandler], then on a successful token calls [expressGooglePay] on [coroutineScope].
 * Companion to [expressGooglePay] for host apps that don't want to wire loadPaymentData and the
 * express call together themselves (see PayButton wiring in MainActivity).
 */
fun presentGooglePayExpressButton(
    googlePayHandler: GooglePayHandler,
    params: Map<String, Any?>,
    amountMinorUnits: Long,
    currencyCode: String,
    coroutineScope: CoroutineScope,
    webViewBridge: WebViewBridge? = null,
    onResult: (GooglePayExpressResult) -> Unit
) {
    // Host-rendered buttons (demo MainActivity, Flutter/Cordova plugins) don't pass a bridge, so a
    // REDIRECT /express result (e.g. 3DS) failed with "webViewBridge is null" — fall back to one
    // on the handler's own activity.
    val bridge = webViewBridge ?: googlePayHandler.defaultWebViewBridge()
    googlePayHandler.loadPaymentData(
        amountMinorUnits = amountMinorUnits,
        currencyCode = currencyCode,
        merchantId = params["mp_gp_merchant_ID"]?.toString(),
        buildMode = params.resolveBuildMode()
    ) { result ->
        when (result) {
            is GooglePayPaymentResult.Success -> {
                onResult(GooglePayExpressResult.InProgress)
                // Covers the host screen until the result is ready: /express (and any 3DS page's
                // /inquire) otherwise runs with the host's buttons live and nothing on screen.
                googlePayHandler.showExpressProcessing()
                coroutineScope.launch {
                    val outcome = try {
                        GooglePayExpressResult.PaymentReceived(
                            expressGooglePay(
                                params = params,
                                googlePayResultJson = result.paymentDataJson,
                                webViewBridge = bridge
                            )
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // Safety net: expressGooglePay already maps its own failures to an
                        // error PaymentResponseDto, but this still guards the host app's
                        // coroutine scope against anything unexpected escaping it.
                        GooglePayExpressResult.SheetError(e.message ?: "Google Pay express payment failed")
                    } finally {
                        googlePayHandler.hideExpressProcessing()
                    }
                    onResult(outcome)
                }
            }
            GooglePayPaymentResult.Cancelled -> onResult(GooglePayExpressResult.Cancelled)
            is GooglePayPaymentResult.Error -> onResult(GooglePayExpressResult.SheetError(result.message))
        }
    }
}
