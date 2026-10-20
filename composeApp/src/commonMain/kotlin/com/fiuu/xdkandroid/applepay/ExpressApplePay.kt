package com.fiuu.xdkandroid.applepay

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

/**
 * Standalone Apple Pay express-checkout entry point for host apps that want the native
 * PKPaymentAuthorizationController sheet only, without presenting FiuuPaymentSDK's Compose UI at
 * all (no ComposeUIViewController is ever created on this path).
 *
 * Mirrors [com.fiuu.xdkandroid.googlePay.expressGooglePay] / [com.fiuu.xdkandroid.samsungPay.expressSamsungPay]
 * in structure and limitations: a REDIRECT/data result will surface as paymentError because no
 * WebViewBridge is attached.
 */
suspend fun expressApplePay(
    params: Map<String, Any?>,
    applePayToken: JsonObject
): PaymentResponseDto {
    params.reportParamWarnings()
    val vm = PaymentViewModel(FiuuPaymentRepository(KtorApiService(HttpClientFactory(), params.resolveBuildMode())))
    try {
        // apMerchantId was only needed to open the PKPaymentRequest sheet (see
        // presentApplePayExpressCheckout) — drop it now that we have a token so it never reaches
        // toExpressApiPayload/the wire.
        //
        // expressMode forced true regardless of params["mp_express_mode"] — see the matching note
        // in ExpressGooglePay.expressGooglePay(). This entry point always drives
        // PaymentViewModel.expressPayment (/express), and PaymentRequest.putChannel() picks its
        // JSON key prefix (mp_channel vs xp_channel) off this same field.
        val request = params.toPaymentRequest().copy(applePayToken = applePayToken, apMerchantId = null, expressMode = true)

        vm.expressPayment(request)

        val finalState = vm.uiState.first { it.paymentSuccess != null || it.paymentError != null }
        finalState.paymentSuccess?.let { return it }

        val error = finalState.paymentError ?: PaymentError.GenericError()
        val customError = error as? PaymentError.CustomError
        val errorMessage = customError?.msg ?: error.code
        val errorCode = customError?.errorCode ?: customError?.statusCode?.toString() ?: error.code
        return applePayErrorResponse(code = errorCode, message = errorMessage ?: error.code)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        return applePayErrorResponse(code = "SDK_ERROR", message = e.message ?: "Apple Pay express payment failed")
    } finally {
        vm.dispose()
    }
}

internal fun applePayErrorResponse(code: String, message: String): PaymentResponseDto =
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

// See the comment at its assignment site in presentApplePayExpressCheckout for why this exists.
private var activeExpressApplePayHandler: ApplePayHandler? = null

/** Outcome of [presentApplePayExpressCheckout], reported via its onResult callback. */
sealed interface ApplePayExpressResult {
    /** The Apple Pay sheet returned a token and the /express call has started. */
    data object InProgress : ApplePayExpressResult
    data class PaymentReceived(val response: PaymentResponseDto) : ApplePayExpressResult
    data object Cancelled : ApplePayExpressResult
    data class SheetError(val message: String) : ApplePayExpressResult
}

/**
 * Presents the native Apple Pay sheet directly (merchantId/amount/currency/country come straight
 * from [params], no network call needed to show it) and, once a token is authorized, calls
 * [expressApplePay] to complete the payment in one /express request. Unlike Google/Samsung Pay
 * there's no host-rendered button to wire up first — PKPaymentAuthorizationController is its own
 * system UI, so this single function is the whole entry point.
 */
fun presentApplePayExpressCheckout(
    params: Map<String, Any?>,
    onResult: (ApplePayExpressResult) -> Unit
) {
    val merchantId = params["mp_ap_merchant_ID"]?.toString()
    if (merchantId.isNullOrBlank()) {
        onResult(ApplePayExpressResult.SheetError("mp_ap_merchant_ID is missing from params"))
        return
    }
    val amount = params["mp_amount"]?.toString() ?: "0"
    val currency = params["mp_currency"]?.toString() ?: "MYR"
    val countryCode = params["mp_country"]?.toString() ?: "MY"

    // ApplePayHandler itself holds the strong refs that keep the PKPaymentAuthorizationController
    // and its delegate alive while the sheet is open — but nothing kept the ApplePayHandler
    // instance ITSELF alive if it's created as a bare temporary here. Without this top-level
    // reference, it could be deallocated (taking the delegate with it) before the user ever
    // interacts with the sheet, which would silently break dismissal (e.g. tapping Cancel).
    val handler = ApplePayHandler()
    activeExpressApplePayHandler = handler

    handler.requestPayment(
        merchantId = merchantId,
        amount = amount,
        currency = currency,
        countryCode = countryCode,
        onResult = { result ->
            activeExpressApplePayHandler = null
            when (result) {
                is ApplePayResult.Success -> {
                    onResult(ApplePayExpressResult.InProgress)
                    CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
                        try {
                            val response = expressApplePay(params, result.token)
                            onResult(ApplePayExpressResult.PaymentReceived(response))
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            // Safety net: expressApplePay already maps its own failures to an
                            // error PaymentResponseDto, but this still guards the caller's
                            // coroutine against anything unexpected escaping it.
                            onResult(ApplePayExpressResult.SheetError(e.message ?: "Apple Pay express payment failed"))
                        }
                    }
                }
                ApplePayResult.Cancelled -> onResult(ApplePayExpressResult.Cancelled)
                is ApplePayResult.Error -> onResult(ApplePayExpressResult.SheetError(result.message))
            }
        }
    )
}

/**
 * Convenience wrapper around [presentApplePayExpressCheckout] for callers (e.g. Swift) that just
 * want a single [PaymentResponseDto] callback — same calling convention as [startFiuuPaymentIos] —
 * instead of switching over [ApplePayExpressResult]'s InProgress/Cancelled/SheetError variants.
 * Never presents FiuuPaymentSDK's Compose UI; only the native Apple Pay sheet is shown.
 */
fun startApplePayExpress(
    params: Map<String, Any?>,
    onResult: (PaymentResponseDto) -> Unit
) {
    presentApplePayExpressCheckout(params) { result ->
        when (result) {
            is ApplePayExpressResult.PaymentReceived -> onResult(result.response)
            ApplePayExpressResult.Cancelled ->
                onResult(applePayErrorResponse("APPLE_PAY_CANCELLED", "Payment was not completed."))
            is ApplePayExpressResult.SheetError ->
                onResult(applePayErrorResponse("APPLE_PAY_ERROR", result.message))
            ApplePayExpressResult.InProgress -> { /* no-op — only the terminal result matters here */ }
        }
    }
}
