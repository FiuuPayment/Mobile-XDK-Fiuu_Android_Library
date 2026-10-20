package com.fiuu.xdkandroid.ui.model

import com.fiuu.xdkandroid.data.remote.dto.InstalmentTenureResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentInquireResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentResponseDto
import com.fiuu.xdkandroid.domain.entity.CardTokenInit
import com.fiuu.xdkandroid.domain.entity.PaymentChannel

data class PaymentUiState(
    val amount: Long = 0L,
    val currency: String = "",
    val orderId: String = "",
    val name: String = "",
    val email: String = "",
    val mobile: String = "",
    val description: String = "",
    val expressMode: Boolean = false,
    val isRooted : Boolean = false,
    val loading: Boolean = false,
    val loadingMessage: String = "Loading...",
    // True while the user is back from a wallet app (TNG etc.) but /inquire still says "22"
    // (pending) — FiuuPaymentSDK shows a waiting overlay with a Cancel button instead of the
    // plain loading overlay. See PaymentViewModel.awaitWalletConfirmation().
    val awaitingWalletConfirmation: Boolean = false,
    val formError: FormError? = null,
    val paymentSuccess: PaymentResponseDto? = null,
    val processErrorResponse: PaymentResponseDto? = null,
    // Non-null while an offline/over-the-counter payment instruction (e.g. Cash-711's barcode) is
    // being shown — see PaymentInstructionInfo. Distinct from paymentSuccess: showing this screen
    // must NOT close the SDK, since the customer still needs to complete payment out-of-band; only
    // confirmPaymentInstructionAndClose() (tapping the screen's CLOSE button) does that.
    val paymentInstruction: PaymentInstructionInfo? = null,
    val paymentError: PaymentError? = null,
    val selectedChannel: PaymentChannel? = null,
    val channels: List<PaymentChannel> = emptyList(),
    // The channels /initiate opened this session for. `channels` may instead hold the richer
    // GetAvailableChannelById list, which can include channels /initiate doesn't — a specific
    // mp_channel is only payable if it's in this list (see SinglePaymentScreen).
    val initiateChannels: List<PaymentChannel> = emptyList(),
    // False from the moment a new attempt starts until /initiate's channel resolution (live fetch
    // or /initiate fallback) has actually settled at least once. Screens that pick a channel out
    // of `channels` need this to tell "still loading" apart from "loaded, genuinely no match" —
    // `channels.isEmpty()` alone can't distinguish those two states.
    val channelsResolved: Boolean = false,
    // Saved-card tokens returned by GET card-tokens (fetched right after /initiate resolves an
    // accessToken) — drives the "Saved Card" picker (masked as "**** {bin4}") in the manual
    // card-entry screen.
    val cardTokens: List<CardTokenInit> = emptyList(),
    // From card-tokens' `removable` field — whether the delete icon shows on each saved card in
    // that picker.
    val cardTokensRemovable: Boolean = false,
    val clickToPayRequest: ClickToPayRequest? = null,
    val isSamsungPayReady: Boolean = false,
    // GooglePayHandler.isReadyToPay() / ApplePayHandler.isAvailable(), checked once at launch —
    // gate the wallet buttons like isSamsungPayReady. Always false on the other platform.
    val isGooglePayReady: Boolean = false,
    val isApplePayReady: Boolean = false,
    val inquiryResult: PaymentInquireResponseDto? = null,
    // Driven by PaymentViewModel.fetchInstalmentTenure(), triggered from CardInstalmentEntryScreen
    // whenever its card number/expiry become valid. Only the ineligible-card error shape (Status/
    // error_code/error_desc) is confirmed so far — the success shape (actual plan list) isn't yet,
    // so instalmentTenureResult.errorDesc is what the UI shows in place of a generic message.
    val instalmentTenureLoading: Boolean = false,
    val instalmentTenureResult: InstalmentTenureResponseDto? = null
)

data class ClickToPayRequest(
    val url: String,
    val headers: Map<String, String>,
    val amount: Long,
    val currency: String,
    val country: String? = null
)

enum class FormError {
    INVALID_EMAIL,
    INVALID_PHONE,
    EMPTY_NAME
}

sealed class PaymentError (val code: String) : Throwable() {
    data class InvalidChannel(val channel: String?) : PaymentError("INVALID CHANNEL")
    class NetworkError : PaymentError("NETWORK ERROR")
    class NoInternetConnection : PaymentError("NO_INTERNET_CONNECTION")
    class Unknown : PaymentError("UNKNOWN ERROR")
    class GenericError : PaymentError("GENERIC ERROR")
    // cancelled: the customer dismissed the sheet, as opposed to Apple Pay failing.
    data class ApplePayError(val errorMessage: String, val cancelled: Boolean = false) : PaymentError("APPLE_PAY_ERROR")
    data class CustomError(val msg: String?, val statusCode: Int? = null, val errorCode: String? = null) : PaymentError("CATCH ERROR")
}
