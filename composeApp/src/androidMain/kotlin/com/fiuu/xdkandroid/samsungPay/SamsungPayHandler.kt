package com.fiuu.xdkandroid.samsungPay

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import com.fiuu.xdkandroid.platform.ExpressProcessingOverlay
import com.fiuu.xdkandroid.utils.FiuuLogger
import com.samsung.android.sdk.samsungpay.v2.PartnerInfo
import com.samsung.android.sdk.samsungpay.v2.SamsungPay
import com.samsung.android.sdk.samsungpay.v2.SpaySdk
import com.samsung.android.sdk.samsungpay.v2.StatusListener
import com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_NOT_READY
import com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_NOT_SUPPORTED
import com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_READY
import com.samsung.android.sdk.samsungpay.v2.payment.CardInfo
import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo
import com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.AmountBoxControl
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.AmountConstants
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.CustomSheet

private const val AMOUNT_ITEM_ID = "totalAmount"

// Any of these packages present means Samsung Pay/Wallet is installed. Used only as a fallback
// when the SDK's own getSamsungPayStatus() call misreports ERROR_SPAY_PKG_NOT_FOUND.
private val samsungPayPackages = listOf(
    "com.samsung.android.spay",
    "com.samsung.android.samsungpay.gear",
    "com.samsung.android.wallet"
)

// Set to true to bypass Samsung Pay SDK and return a fake token — for flow testing only
private const val DEBUG_MOCK_MODE = false

// The Samsung Pay SDK is compileOnly: the host app supplies the jar (BLT-03). Without it, any
// Samsung class reference throws NoClassDefFoundError — an Error, which the catch blocks below
// don't see — so both entry points check this first and report Samsung Pay as unavailable.
private val isSamsungPaySdkPresent: Boolean by lazy {
    try {
        Class.forName("com.samsung.android.sdk.samsungpay.v2.SamsungPay")
        true
    } catch (_: ClassNotFoundException) {
        FiuuLogger.log("SamsungPay SDK jar not found in the host app — Samsung Pay disabled")
        false
    }
}

actual class SamsungPayHandler actual constructor(activity: Any) {

    private val activityRef: Activity =
        activity as? Activity
            ?: throw IllegalArgumentException("SamsungPayHandler requires android.app.Activity")

    private val partnerInfo: PartnerInfo by lazy {
        val serviceBundle = Bundle()
        serviceBundle.putString(SpaySdk.PARTNER_SERVICE_TYPE, SpaySdk.ServiceType.INAPP_PAYMENT.toString())
        PartnerInfo(SamsungPayConfig.SERVICE_ID, serviceBundle)
    }

    actual fun isReadyToPay(callback: (Boolean) -> Unit) {
        if (DEBUG_MOCK_MODE) {
            FiuuLogger.log("SamsungPay isReadyToPay DEBUG_MOCK_MODE=true → ready=true")
            callback(true)
            return
        }
        if (!isSamsungPaySdkPresent) {
            callback(false)
            return
        }
        try {
            SamsungPay(activityRef, partnerInfo).getSamsungPayStatus(object : StatusListener {
                override fun onSuccess(status: Int, bundle: Bundle) {
                    val statusName = when (status) {
                        SPAY_READY -> "SPAY_READY"
                        SPAY_NOT_READY -> "SPAY_NOT_READY"
                        SPAY_NOT_SUPPORTED -> "SPAY_NOT_SUPPORTED"
                        else -> "UNKNOWN($status)"
                    }
                    FiuuLogger.log("SamsungPay getSamsungPayStatus onSuccess status=$statusName")
                    callback(status == SPAY_READY)
                }

                override fun onFail(errorCode: Int, bundle: Bundle) {
                    val extra = bundle.keySet()?.joinToString { "$it=${bundle.get(it)}" } ?: ""
                    FiuuLogger.log("SamsungPay getSamsungPayStatus onFail errorCode=$errorCode extra=[$extra] serviceId=${SamsungPayConfig.SERVICE_ID}")
                    if (errorCode == SpaySdk.ERROR_SPAY_PKG_NOT_FOUND) {
                        // SDK v2.22.00 binds against the legacy package name and reports "not found"
                        // even when only the newer Samsung Wallet package is installed — fall back to
                        // a direct PackageManager check for this one known-broken case only. Any other
                        // error (e.g. PARTNER_INFO_INVALID) is a real not-ready/misconfiguration signal
                        // and must not be masked by this fallback.
                        val found = samsungPayPackages.any { pkg ->
                            try { activityRef.packageManager.getPackageInfo(pkg, 0); true }
                            catch (_: PackageManager.NameNotFoundException) { false }
                        }
                        FiuuLogger.log("SamsungPay isReadyToPay ERROR_SPAY_PKG_NOT_FOUND fallback packageCheck=$found")
                        callback(found)
                    } else {
                        callback(false)
                    }
                }
            })
        } catch (e: Exception) {
            FiuuLogger.log("SamsungPay getSamsungPayStatus threw: ${e.message}")
            callback(false)
        }
    }

    actual fun loadPaymentData(
        amountMinorUnits: Long,
        currencyCode: String,
        onResult: (SamsungPayPaymentResult) -> Unit,
    ) {
        if (DEBUG_MOCK_MODE) {
            // Returns a fake token so the full ViewModel → API path can be tested without a real Samsung device
            val fakeToken = """{"token":"SAMSUNG_PAY_MOCK_TOKEN","method":"3DS","type":"S","3ds":{"type":"S","v":"2.0"}}"""
            onResult(SamsungPayPaymentResult.Success(fakeToken))
            return
        }
        if (!isSamsungPaySdkPresent) {
            onResult(SamsungPayPaymentResult.Error("Samsung Pay SDK is not included in this app"))
            return
        }

        val amountDouble = amountMinorUnits / 100.0

        val amountControl = AmountBoxControl(SamsungPayConfig.AMOUNT_CONTROL_ID, currencyCode)
        amountControl.addItem(AMOUNT_ITEM_ID, "Total", amountDouble, "")
        amountControl.setAmountTotal(amountDouble, AmountConstants.FORMAT_TOTAL_PRICE_ONLY)

        val customSheet = CustomSheet()
        customSheet.addControl(amountControl)

        val paymentInfo = try {
            CustomSheetPaymentInfo.Builder()
                .setMerchantId(SamsungPayConfig.MERCHANT_ID)
                .setMerchantName(SamsungPayConfig.MERCHANT_NAME)
                .setOrderNumber("ORDER-${System.currentTimeMillis()}")
                .setCustomSheet(customSheet)
                .setAddressInPaymentSheet(CustomSheetPaymentInfo.AddressInPaymentSheet.DO_NOT_SHOW)
                .setAllowedCardBrands(
                    listOf(SpaySdk.Brand.VISA, SpaySdk.Brand.MASTERCARD)
                )
                .build()
        } catch (e: Exception) {
            onResult(SamsungPayPaymentResult.Error(e.message ?: "Invalid Samsung Pay request"))
            return
        }

        val paymentManager = PaymentManager(activityRef, partnerInfo)

        paymentManager.startInAppPayWithCustomSheet(
            paymentInfo,
            object : PaymentManager.CustomSheetTransactionInfoListener {
                override fun onCardInfoUpdated(selectedCardInfo: CardInfo, customSheet: CustomSheet) {
                    paymentManager.updateSheet(customSheet)
                }

                // onSuccess: paymentInfo is the completed sheet, tokenData is the payment credential JSON
                override fun onSuccess(
                    paymentInfo: CustomSheetPaymentInfo,
                    tokenData: String,
                    extraPaymentData: Bundle,
                ) {
                    onResult(SamsungPayPaymentResult.Success(tokenData))
                }

                override fun onFailure(errorCode: Int, errorData: Bundle) {
                    val extra = errorData.keySet()?.joinToString { "$it=${errorData.get(it)}" } ?: ""
                    FiuuLogger.log("SamsungPay onFailure errorCode=$errorCode extra=[$extra]")
                    when (errorCode) {
                        SpaySdk.ERROR_USER_CANCELED ->
                            onResult(SamsungPayPaymentResult.Cancelled)
                        SpaySdk.ERROR_NONE ->
                            onResult(SamsungPayPaymentResult.Error("Samsung Pay is not available on this device"))
                        SpaySdk.ERROR_DEVICE_NOT_SAMSUNG ->
                            onResult(SamsungPayPaymentResult.Error("Samsung Pay is only supported on Samsung devices"))
                        SpaySdk.ERROR_SPAY_PKG_NOT_FOUND ->
                            onResult(SamsungPayPaymentResult.Error("Samsung Pay app is not installed"))
                        SpaySdk.ERROR_SPAY_SETUP_NOT_COMPLETED ->
                            onResult(SamsungPayPaymentResult.Error("Samsung Pay setup is not complete. Please add a card in the Samsung Pay app."))
                        else ->
                            onResult(SamsungPayPaymentResult.Error("Samsung Pay failed (errorCode=$errorCode extra=$extra)"))
                    }
                }
            }
        )
    }

    private val expressProcessingOverlay by lazy { ExpressProcessingOverlay(activityRef) }

    internal actual fun showExpressProcessing() = expressProcessingOverlay.show()

    internal actual fun hideExpressProcessing() = expressProcessingOverlay.hide()
}
