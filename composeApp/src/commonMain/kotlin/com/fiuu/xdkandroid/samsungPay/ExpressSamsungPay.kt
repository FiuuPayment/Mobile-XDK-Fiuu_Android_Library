package com.fiuu.xdkandroid.samsungPay

import com.fiuu.xdkandroid.data.remote.HttpClientFactory
import com.fiuu.xdkandroid.data.remote.KtorApiService
import com.fiuu.xdkandroid.data.remote.dto.PaymentErrorDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentResponseDto
import com.fiuu.xdkandroid.data.repository.FiuuPaymentRepository
import com.fiuu.xdkandroid.domain.entity.reportParamWarnings
import com.fiuu.xdkandroid.domain.entity.resolveBuildMode
import com.fiuu.xdkandroid.domain.entity.toPaymentRequest
import com.fiuu.xdkandroid.ui.model.PaymentError
import com.fiuu.xdkandroid.viewmodel.PaymentViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * Standalone Samsung Pay express-checkout entry point for host apps that render their own Samsung
 * Pay button outside ProvideFiuuPaymentSDK and just want a result back, without showing the SDK's
 * channel-selection UI.
 *
 * Mirrors [com.fiuu.xdkandroid.googlePay.expressGooglePay] in structure and limitations: a
 * REDIRECT/data result will surface as paymentError because no WebViewBridge is attached.
 */
suspend fun expressSamsungPay(
    params: Map<String, Any?>,
    samsungPayCredentialJson: String
): PaymentResponseDto {
    params.reportParamWarnings()
    val vm = PaymentViewModel(FiuuPaymentRepository(KtorApiService(HttpClientFactory(), params.resolveBuildMode())))
    try {
        val samsungPayData = Json.parseToJsonElement(samsungPayCredentialJson).jsonObject
        // Forced true regardless of params["mp_express_mode"] — see the matching note in
        // ExpressGooglePay.expressGooglePay(). This entry point always drives
        // PaymentViewModel.expressPayment (/express), and PaymentRequest.putChannel() picks its
        // JSON key prefix (mp_channel vs xp_channel) off this same field.
        val request = params.toPaymentRequest().copy(samsungPayData = samsungPayData, expressMode = true)

        vm.expressPayment(request)

        val finalState = vm.uiState.first { it.paymentSuccess != null || it.paymentError != null }
        finalState.paymentSuccess?.let { return it }

        val error = finalState.paymentError ?: PaymentError.GenericError()
        val customError = error as? PaymentError.CustomError
        val errorMessage = customError?.msg ?: error.code
        val errorCode = customError?.errorCode ?: customError?.statusCode?.toString() ?: error.code
        return samsungPayErrorResponse(code = errorCode, message = errorMessage ?: error.code)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        return samsungPayErrorResponse(code = "SDK_ERROR", message = e.message ?: "Samsung Pay express payment failed")
    } finally {
        vm.dispose()
    }
}

private fun samsungPayErrorResponse(code: String, message: String): PaymentResponseDto =
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

/** Outcome of [presentSamsungPayExpressButton]. */
sealed interface SamsungPayExpressResult {
    data object InProgress : SamsungPayExpressResult
    data class PaymentReceived(val response: PaymentResponseDto) : SamsungPayExpressResult
    data object Cancelled : SamsungPayExpressResult
    data class SheetError(val message: String) : SamsungPayExpressResult
}

/**
 * Drives a host-rendered Samsung Pay button end-to-end: presents the payment sheet via
 * [samsungPayHandler], then on a successful token calls [expressSamsungPay] on [coroutineScope].
 */
fun presentSamsungPayExpressButton(
    samsungPayHandler: SamsungPayHandler,
    params: Map<String, Any?>,
    amountMinorUnits: Long,
    currencyCode: String,
    coroutineScope: CoroutineScope,
    onResult: (SamsungPayExpressResult) -> Unit
) {
    samsungPayHandler.loadPaymentData(
        amountMinorUnits = amountMinorUnits,
        currencyCode = currencyCode
    ) { result ->
        when (result) {
            is SamsungPayPaymentResult.Success -> {
                onResult(SamsungPayExpressResult.InProgress)
                // Same as presentGooglePayExpressButton: block the host screen during /express.
                samsungPayHandler.showExpressProcessing()
                coroutineScope.launch {
                    val outcome = try {
                        SamsungPayExpressResult.PaymentReceived(
                            expressSamsungPay(
                                params = params,
                                samsungPayCredentialJson = result.paymentCredentialJson
                            )
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        SamsungPayExpressResult.SheetError(e.message ?: "Samsung Pay express payment failed")
                    } finally {
                        samsungPayHandler.hideExpressProcessing()
                    }
                    onResult(outcome)
                }
            }
            SamsungPayPaymentResult.Cancelled -> onResult(SamsungPayExpressResult.Cancelled)
            is SamsungPayPaymentResult.Error -> onResult(SamsungPayExpressResult.SheetError(result.message))
        }
    }
}
