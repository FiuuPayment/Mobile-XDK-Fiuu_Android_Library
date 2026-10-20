package com.fiuu.xdkandroid.applepay

import com.fiuu.xdkandroid.utils.FiuuLogger
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import platform.Foundation.NSData
import platform.Foundation.NSDecimalNumber
import platform.PassKit.PKPayment
import platform.PassKit.PKPaymentAuthorizationController
import platform.PassKit.PKPaymentAuthorizationControllerDelegateProtocol
import platform.PassKit.PKPaymentAuthorizationResult
import platform.PassKit.PKPaymentAuthorizationStatus
import platform.PassKit.PKPaymentMethodTypeCredit
import platform.PassKit.PKPaymentMethodTypeDebit
import platform.PassKit.PKPaymentMethodTypePrepaid
import platform.PassKit.PKPaymentMethodTypeStore
import platform.PassKit.PKPaymentNetworkAmex
import platform.PassKit.PKPaymentNetworkMasterCard
import platform.PassKit.PKPaymentNetworkVisa
import platform.PassKit.PKPaymentRequest
import platform.PassKit.PKPaymentSummaryItem
import platform.darwin.NSObject

actual class ApplePayHandler actual constructor() {

    // Strong references held for the duration of the payment sheet presentation.
    // PKPaymentAuthorizationController.delegate is WEAK — this Kotlin-side strong ref
    // prevents the delegate from being GC'd while the sheet is open.
    private var activeController: PKPaymentAuthorizationController? = null
    private var activeDelegate: ApplePayDelegate? = null

    private val supportedNetworks = listOf(
        PKPaymentNetworkVisa,
        PKPaymentNetworkMasterCard,
        PKPaymentNetworkAmex
    )

    // canMakePayments() only checks hardware/software capability, not whether a card is
    // provisioned — canMakePaymentsUsingNetworks(supportedNetworks) would return false (and
    // block the sheet from ever presenting) on a capable device that just has no card set up
    // yet. Presenting anyway lets PassKit show its own "Set Up Apple Pay" prompt.
    actual fun isAvailable(): Boolean =
        PKPaymentAuthorizationController.canMakePayments()

    @OptIn(ExperimentalForeignApi::class)
    actual fun requestPayment(
        merchantId: String,
        amount: String,
        currency: String,
        countryCode: String,
        onResult: (ApplePayResult) -> Unit
    ) {
        if (!isAvailable()) {
            onResult(ApplePayResult.Error("Apple Pay is not available on this device"))
            return
        }

        val paymentRequest = PKPaymentRequest().apply {
            merchantIdentifier = merchantId
            this.supportedNetworks = this@ApplePayHandler.supportedNetworks
            merchantCapabilities = 1u // PKMerchantCapabilityThreeDS
            this.currencyCode = currency
            this.countryCode = countryCode
            paymentSummaryItems = listOf(
                PKPaymentSummaryItem.summaryItemWithLabel(
                    "Total",
                    NSDecimalNumber.decimalNumberWithString(amount)
                )
            )
        }

        val delegate = ApplePayDelegate(
            merchantId = merchantId,
            onResult = onResult,
            onDone = {
                activeDelegate = null
                activeController = null
            }
        )
        // Must assign BEFORE presentWithCompletion so the strong ref exists
        // before any delegate callbacks can fire.
        activeDelegate = delegate

        val controller = PKPaymentAuthorizationController(paymentRequest = paymentRequest)
        controller.delegate = delegate
        activeController = controller

        controller.presentWithCompletion { presented ->
            if (!presented) {
                delegate.sendResult(ApplePayResult.Error("Apple Pay sheet could not be presented"))
                activeDelegate = null
                activeController = null
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class ApplePayDelegate(
    private val merchantId: String,
    private val onResult: (ApplePayResult) -> Unit,
    private val onDone: () -> Unit
) : NSObject(), PKPaymentAuthorizationControllerDelegateProtocol {

    private var resultSent = false

    fun sendResult(result: ApplePayResult) {
        if (resultSent) return
        resultSent = true
        onResult(result)
    }

    override fun paymentAuthorizationController(
        controller: PKPaymentAuthorizationController,
        didAuthorizePayment: PKPayment,
        handler: (PKPaymentAuthorizationResult?) -> Unit
    ) {
        val token = buildToken(didAuthorizePayment)
        // The token carries the payment cryptogram (data/signature/header) — never log its
        // contents, even under mp_dev_mode.
        FiuuLogger.log("Apple Pay authorization ${if (token != null) "produced a token" else "failed to produce a token"}")

        if (token != null) {
            sendResult(ApplePayResult.Success(token))
            handler(PKPaymentAuthorizationResult(
                status = PKPaymentAuthorizationStatus.PKPaymentAuthorizationStatusSuccess,
                errors = null
            ))
        } else {
            sendResult(ApplePayResult.Error("Failed to decode Apple Pay payment token"))
            handler(PKPaymentAuthorizationResult(
                status = PKPaymentAuthorizationStatus.PKPaymentAuthorizationStatusFailure,
                errors = null
            ))
        }
    }

    override fun paymentAuthorizationControllerDidFinish(
        controller: PKPaymentAuthorizationController
    ) {
        // Must call dismiss here — PKPaymentAuthorizationController does NOT auto-dismiss.
        controller.dismissWithCompletion {
            // sendResult is a no-op if already called (e.g. success path).
            // Covers the case where the user cancels or dismisses after "Payment Not Completed".
            this.sendResult(ApplePayResult.Cancelled)
            this.onDone()
        }
    }

    private fun buildToken(payment: PKPayment): JsonObject? {
        val paymentDataStr = payment.token.paymentData.toUtf8String() ?: return null

        val paymentDataObj = try {
            Json.parseToJsonElement(paymentDataStr).jsonObject
        } catch (e: Exception) {
            return null
        }

        val data          = paymentDataObj["data"]?.jsonPrimitive?.content ?: ""
        val signature     = paymentDataObj["signature"]?.jsonPrimitive?.content ?: ""
        val header        = paymentDataObj["header"]?.jsonObject
        val ephemeralKey  = header?.get("ephemeralPublicKey")?.jsonPrimitive?.content ?: ""
        val publicKeyHash = header?.get("publicKeyHash")?.jsonPrimitive?.content ?: ""
        val transactionId = header?.get("transactionId")?.jsonPrimitive?.content ?: ""
        val version       = paymentDataObj["version"]?.jsonPrimitive?.content ?: ""

        val paymentMethod = payment.token.paymentMethod
        val paymentType = when (paymentMethod.type) {
            PKPaymentMethodTypeDebit   -> "debit"
            PKPaymentMethodTypeCredit  -> "credit"
            PKPaymentMethodTypeStore   -> "store"
            PKPaymentMethodTypePrepaid -> "prepaid"
            else                       -> "unknown"
        }

        val tokenObject = buildJsonObject {
            put("transactionIdentifier", payment.token.transactionIdentifier)
            put("merchantIdentifier", merchantId)
            putJsonObject("paymentMethod") {
                put("displayName", paymentMethod.displayName ?: "")
                put("network", paymentMethod.network ?: "")
                put("type", paymentType)
            }
            putJsonObject("paymentData") {
                put("version", version)
                put("data", data)
                put("signature", signature)
                putJsonObject("header") {
                    put("ephemeralPublicKey", ephemeralKey)
                    put("publicKeyHash", publicKeyHash)
                    put("transactionId", transactionId)
                }
            }
        }

        return tokenObject
    }
}

// Converts NSData containing UTF-8 JSON bytes to a Kotlin String.
// Uses raw byte access to avoid the unreliable NSString.create(data:encoding:)
// companion function in Kotlin/Native.
@OptIn(ExperimentalForeignApi::class)
private fun NSData.toUtf8String(): String? {
    val length = this.length.toInt()
    if (length == 0) return null
    val rawBytes = this.bytes?.reinterpret<ByteVar>() ?: return null
    val byteArray = ByteArray(length) { i -> rawBytes[i] }
    return byteArray.decodeToString()
}
