package com.fiuu.xdkandroid

import com.fiuu.xdkandroid.applepay.ApplePayExpressResult
import com.fiuu.xdkandroid.applepay.presentApplePayExpressCheckout
import com.fiuu.xdkandroid.applepay.startApplePayExpress
import com.fiuu.xdkandroid.data.remote.dto.PaymentResponseDto
import kotlinx.serialization.json.Json
import platform.Foundation.NSDictionary
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private val prettyJson = Json { prettyPrint = true }

/**
 * Swift-facing entry point for express Apple Pay — takes an NSDictionary (so Swift can pass a
 * plain [AnyHashable: Any] literal, same as startFiuuPaymentIos) and forwards to
 * [com.fiuu.xdkandroid.applepay.startApplePayExpress]. Never presents FiuuPaymentSDK's Compose UI.
 */
@Suppress("unused")
fun startApplePayExpressIos(
    paymentParams: NSDictionary,
    onResult: (PaymentResponseDto) -> Unit
) {
    val params = paymentParams.toMap()
        .mapKeys { it.key.toString() }

    startApplePayExpress(params) { result ->
        dispatch_async(dispatch_get_main_queue()) {
            onResult(result)
        }
    }
}

/**
 * Swift-facing entry point mirroring the Android demo app's presentGooglePayExpressButton wiring
 * (see MainActivity's onGooglePayClick) — surfaces each stage of the /express flow individually
 * (sheet requested/token received/user cancelled/sheet error) via [ApplePayExpressResult] instead
 * of collapsing everything into a single [PaymentResponseDto] the way [startApplePayExpressIos]
 * does, so a Swift call site can log/react to each stage the same way the Android one does.
 * [onPaymentReceived] also gets the DTO pretty-printed to JSON (mirrors MainActivity's
 * prettyJson.encodeToString) since Swift can't call kotlinx.serialization directly.
 */
@Suppress("unused")
fun presentApplePayExpressCheckoutIos(
    paymentParams: NSDictionary,
    onInProgress: () -> Unit,
    onPaymentReceived: (PaymentResponseDto, String) -> Unit,
    onCancelled: () -> Unit,
    onSheetError: (String) -> Unit
) {
    val params = paymentParams.toMap()
        .mapKeys { it.key.toString() }

    presentApplePayExpressCheckout(params) { result ->
        dispatch_async(dispatch_get_main_queue()) {
            when (result) {
                ApplePayExpressResult.InProgress -> onInProgress()
                is ApplePayExpressResult.PaymentReceived -> {
                    val json = prettyJson.encodeToString(PaymentResponseDto.serializer(), result.response)
                    onPaymentReceived(result.response, json)
                }
                ApplePayExpressResult.Cancelled -> onCancelled()
                is ApplePayExpressResult.SheetError -> onSheetError(result.message)
            }
        }
    }
}
