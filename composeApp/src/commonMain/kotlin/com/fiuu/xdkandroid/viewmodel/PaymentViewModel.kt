package com.fiuu.xdkandroid.viewmodel

import com.fiuu.xdkandroid.data.mapper.toDomain
import com.fiuu.xdkandroid.data.remote.ApiConfig
import com.fiuu.xdkandroid.data.remote.dto.ApiException
import com.fiuu.xdkandroid.data.remote.dto.CardTokensRequestDto
import com.fiuu.xdkandroid.data.remote.dto.InstalmentTenureRequestDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentExpressRequestDto
import com.fiuu.xdkandroid.data.remote.dto.inquireChecksumPayload
import com.fiuu.xdkandroid.data.remote.dto.inquireRequest
import com.fiuu.xdkandroid.data.remote.dto.INQUIRE_POLL_DELAYS_MS
import com.fiuu.xdkandroid.data.remote.dto.resultPageKey
import com.fiuu.xdkandroid.data.remote.dto.PaymentInquireResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentCreateResponse
import com.fiuu.xdkandroid.data.remote.dto.PaymentResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentProcessRequestDto
import com.fiuu.xdkandroid.applepay.ApplePayHandler
import com.fiuu.xdkandroid.applepay.ApplePayResult
import com.fiuu.xdkandroid.data.remote.PaymentProcessException
import com.fiuu.xdkandroid.data.remote.dto.PaymentErrorDto
import com.fiuu.xdkandroid.domain.PaymentRepository
import com.fiuu.xdkandroid.domain.entity.CardTokenInit
import com.fiuu.xdkandroid.domain.entity.PaymentChannel
import com.fiuu.xdkandroid.domain.entity.GOOGLE_PAY_TOKEN_AS_BASE64
import com.fiuu.xdkandroid.domain.entity.PaymentRequest
import com.fiuu.xdkandroid.domain.entity.isOtcPayment
import com.fiuu.xdkandroid.domain.entity.isPluginPayment
import com.fiuu.xdkandroid.domain.entity.toCardTokensApiPayload
import com.fiuu.xdkandroid.domain.entity.toInitApiPayload
import com.fiuu.xdkandroid.domain.entity.toExpressApiPayload
import com.fiuu.xdkandroid.domain.entity.toProcessApiPayload
import com.fiuu.xdkandroid.domain.entity.toPaymentRequest
import com.fiuu.xdkandroid.googlePay.GooglePayPaymentResult
import com.fiuu.xdkandroid.samsungPay.SamsungPayPaymentResult
import com.fiuu.xdkandroid.platform.NetworkConnectivityChecker
import com.fiuu.xdkandroid.platform.SecureTokenStore
import com.fiuu.xdkandroid.platform.StoredPaymentSession
import com.fiuu.xdkandroid.platform.WebViewBridge
import com.fiuu.xdkandroid.platform.isDeviceRooted
import com.fiuu.xdkandroid.ui.model.ClickToPayRequest
import com.fiuu.xdkandroid.ui.model.FormError
import com.fiuu.xdkandroid.ui.model.PaymentError
import com.fiuu.xdkandroid.ui.model.PaymentInstructionInfo
import com.fiuu.xdkandroid.ui.model.PaymentUiState
import com.fiuu.xdkandroid.utils.CryptoUtils
import com.fiuu.xdkandroid.utils.DateTimeUtils
import com.fiuu.xdkandroid.utils.FiuuLogger
import com.fiuu.xdkandroid.utils.SESSION_TIMEOUT_SECONDS
import com.fiuu.xdkandroid.utils.generateChecksum
import com.fiuu.xdkandroid.utils.redactSensitiveJson
import com.fiuu.xdkandroid.utils.redactSensitiveMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

// Matches the backend's accessToken TTL — session-resume lookups past this are rejected
// client-side rather than round-tripping to /inquire just to get a stale-token error back.
private const val SESSION_TTL_SECONDS = 15 * 60L

// How long to keep waiting for the /subscribe stream after Android detects the gateway's result
// page, before falling back to an /inquire lookup — see handleResultPageReached().
private const val RESULT_PAGE_STREAM_WAIT_MS = 10_000L

// Pause before the offline-voucher (OTC) /inquire, shown under "Confirming payment..." — see
// resolveOfflineVoucher().
private const val OFFLINE_VOUCHER_INQUIRE_DELAY_MS = 3_000L

// The result FiuuPaymentSDK's session countdown (SESSION_TIMEOUT_SECONDS) hands the host when it can't learn the real
// status — nothing in flight, or the expiry /inquire failed. See PaymentViewModel.onSessionExpired().
internal fun sessionTimeoutResult() = PaymentResponseDto(
    status = "408",
    statusCheck = false,
    message = "Payment session expired",
    errorCode = "SDK_TIMEOUT",
    errorDetails = PaymentErrorDto(
        code = "400",
        message = "The process took longer than ${SESSION_TIMEOUT_SECONDS / 60} minutes",
        type = "TIMEOUT"
    )
)

// The host result for an offline-payment voucher (Cash-711, etc.) once its instruction screen
// closes. `issued` is the /process response (IDs only — it carries no status); `inquired` is the
// /inquire result, or null when the lookup failed or returned no status. Issuing a voucher is not
// payment, so without a gateway status this is pending "22" — never "00".
internal fun offlineVoucherResult(issued: PaymentResponseDto, inquired: PaymentResponseDto?): PaymentResponseDto {
    val status = inquired?.status?.takeIf { it.isNotBlank() }
        ?: return issued.copy(
            status = PaymentInquireResponseDto.STATUS_PENDING,
            statusCheck = false,
            message = "Payment pending",
            tranID = issued.tranID ?: issued.transactionId
        )
    return inquired.copy(
        statusCheck = status == "00",
        referenceNo = inquired.referenceNo ?: issued.referenceNo,
        transactionId = inquired.transactionId ?: issued.transactionId,
        tranID = inquired.tranID ?: issued.tranID ?: issued.transactionId,
        amount = inquired.amount ?: issued.amount,
        currency = inquired.currency ?: issued.currency,
        channel = inquired.channel ?: issued.channel
    )
}

// TEMPORARY: neither GetAvailableChannelById nor /initiate reliably returns CardInstallment yet —
// hardcoded here so it can be appended to whichever channel list actually ends up in uiState
// (see appendHardcodedInstallmentChannel below). Remove once the API includes it.
private val HARDCODED_CARD_INSTALLMENT_CHANNEL = PaymentChannel(
    name = "CardInstallment",
    title = "Card Instalments",
    default = 0,
    status = 1,
    channelType = "CC",
    currency = listOf("RM", "MYR", "SGD", "PHP"),
    fileName = "indexBA.php",
    maskName = "CardInstallment",
    position = 0,
    tableExtension = "credit",
    isInstallment = true,
    logoUrl = "https://d2x73ruoixi2ei.cloudfront.net/images/logos/channels/120/visa.gif",
    logoUrl2 = "https://d2dxpp4pdqfbe4.cloudfront.net/CardInstallment.gif",
    logoUrl3 = "https://pay.fiuu.com/RMS/G/assets/img/payment_options_v2/CardInstallment.png?v=1.1"
)

// TEMPORARY: see HARDCODED_CARD_INSTALLMENT_CHANNEL above. Called from both fetchAvailableChannels()
// and initialize() so the channel shows up regardless of which of the two ends up populating
// uiState.channels for this attempt.
private fun appendHardcodedInstallmentChannel(channels: List<PaymentChannel>): List<PaymentChannel> =
    if (channels.none { it.name == HARDCODED_CARD_INSTALLMENT_CHANNEL.name }) {
        channels + HARDCODED_CARD_INSTALLMENT_CHANNEL
    } else {
        channels
    }

/**
 * Not an androidx ViewModel: this is instantiated directly via `remember {}` rather than through
 * a ViewModelStore, so nothing would ever call `ViewModel.clear()`/`onCleared()`. It owns its own
 * cancellable scope instead — callers MUST call [dispose] when the SDK leaves composition,
 * otherwise the stream coroutine (and its underlying HttpClient/socket) leaks indefinitely.
 *
 * [dispatcher] exists for tests (a virtual-time TestDispatcher); production always uses the default.
 */
class PaymentViewModel(
    private val repo: PaymentRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    private val viewModelScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _uiState = MutableStateFlow(PaymentUiState())
    private var currentRequest: PaymentRequest? = null
    private var selectedChannel: PaymentChannel? = null
    private var paymentUrl: String? = null
    private var paymentMethod: String? = null
    private var paymentRequestData: Map<String, String>? = null
    private var webViewBridge: WebViewBridge? = null
    private var secureTokenStore: SecureTokenStore? = null
    private var networkConnectivityChecker: NetworkConnectivityChecker? = null
    private var accessToken: String? = null
    // Gateway transaction ID returned by /process or /express for the current attempt — used by
    // /inquire to look up by xp_tran_id, since the stream may never deliver one (e.g. the user
    // closes the WebView or backgrounds the app before the stream pushes anything).
    private var transactionId: String? = null
    // True once card-tokens has answered for the current accessToken. Re-hitting it for the same
    // token shortly after can return an empty body, which would wipe the list we already have.
    private var cardTokensFetched = false
    private var cardTokensJob: Job? = null
    private var streamJob: Job? = null
    // Completed by startListeningStream: true once the stream delivers a result, false if it ends
    // without one. Lets handleResultPageReached() wait for the stream without polling.
    private var streamOutcome: CompletableDeferred<Boolean>? = null
    // Token of a stream that ended without a result while the attempt is still unresolved — some
    // devices abort the app's sockets while it's backgrounded (seen during TNG hand-offs:
    // "IOException: Software caused connection abort"). Cleared by cancelStream(), which every
    // resolution path goes through. checkPaymentStatusOnResume() uses it to reconnect + inquire.
    private var droppedStreamToken: String? = null
    // How many times the current attempt's stream ended without a result. Diagnostics only — it
    // never ends the attempt: on affected devices every app switch drops the stream, so a user
    // going wallet → SMS (OTP) → wallet legitimately racks up several. Reset per attempt.
    private var streamDropCount = 0
    // True from the host screen's ON_PAUSE (onAppPaused) until its ON_RESUME
    // (checkPaymentStatusOnResume) — e.g. while the user is in a wallet app.
    private var appInBackground = false
    // A WebView load failure that arrived while appInBackground. After a wallet hand-off the page
    // left behind keeps navigating, and with the app's network cut off in the background it fails
    // (seen: ShopeePay → shopeepay.com.my, ERR_NAME_NOT_RESOLVED, 2026-10-13). Ending the attempt
    // then told the host "failed" while the user could still pay in the wallet, so the failure is
    // held until resume instead — see checkPaymentStatusOnResume().
    private var deferredWebViewFailure: String? = null
    // Set by the path that has started ending the current attempt (stream result, user close,
    // wallet poll, resume /inquire, session expiry, WebView load failure, ...). Every result path
    // claims it through claimAttempt() before any side effect, so when two race only the first
    // writes a result and the other does nothing.
    private val resolvingAttempt = MutableStateFlow(false)
    // Guards handleResultPageReached() against starting a second wait for the same attempt.
    private var resultPageWaitJob: Job? = null
    // Background /inquire polling while awaitingWalletConfirmation is shown.
    private var walletConfirmationJob: Job? = null
    // The finalized result for an offline-payment instruction (Cash-711, etc.), held back from
    // uiState.paymentSuccess until the user taps CLOSE on PaymentInstructionScreen — see
    // confirmPaymentInstructionAndClose().
    private var pendingInstructionResult: PaymentResponseDto? = null
    
    // Guards against onClose firing after onFailure already reported a result — see openPayment()
    // for why that pairing exists on iOS and in one Android failure branch.
    private var webViewResultReported = false

    private val applePayHandler = ApplePayHandler()
    private val _googlePayTrigger = MutableStateFlow(false)
    private val _samsungPayTrigger = MutableStateFlow(false)
    private val _channelFromAPITrigger: Boolean = true
    // Set only once fetchAvailableChannels() has actually written a non-empty live channel list —
    // distinct from _channelFromAPITrigger (which is always true and only says the live lookup was
    // attempted). Lets initialize() tell "the live fetch hasn't produced anything yet/failed" apart
    // from "it succeeded", so a failed or skipped lookup (missing credentials, network/API error)
    // doesn't leave checkout with no selectable channels at all.
    private var liveChannelsLoaded: Boolean = false

    val uiState = _uiState.asStateFlow()
    val googlePayTrigger = _googlePayTrigger.asStateFlow()
    val samsungPayTrigger = _samsungPayTrigger.asStateFlow()

    fun attachWebViewBridge(bridge: WebViewBridge) {
        webViewBridge = bridge }

    fun attachSecureTokenStore(store: SecureTokenStore) {
        secureTokenStore = store }

    fun attachNetworkConnectivityChecker(checker: NetworkConnectivityChecker) {
        networkConnectivityChecker = checker }

    // Defaults to true (permissive) when nothing's attached, rather than blocking every call —
    // this is only ever unset if a host integration bypasses ProvideFiuuPaymentSDK's wiring
    // entirely, and a false negative here would be worse than skipping the guard.
    private fun hasInternetConnection(): Boolean =
        networkConnectivityChecker?.isConnected() != false

    // The gateway wants a page shown (REDIRECT/data, e.g. a 3DS challenge) but the host gave us no
    // WebView — e.g. Flutter on a plain FlutterActivity, or expressGooglePay() called without one.
    // A dedicated code tells the integrator what to fix instead of a bare GenericError.
    private fun noWebViewBridgeError() = PaymentError.CustomError(
        "Payment requires a web page (e.g. 3DS) but no WebView is available",
        errorCode = "NO_WEBVIEW_BRIDGE"
    )

    // Persists just enough to resume a status check after the process is killed — accessToken
    // is a general session credential (also used for /process), not inquire-scoped, so it must
    // never be handed back to host app code; only the SDK itself ever reads it back out.
    // Re-persists the session once /process hands back a transaction ID, so a resumed status
    // check after process death can look up by xp_tran_id too.
    private fun rememberTransactionId(id: String?, token: String) {
        if (id.isNullOrBlank()) return
        transactionId = id
        persistSession(token)
    }

    private fun persistSession(token: String) {
        val request = currentRequest ?: return
        val orderId = request.orderId ?: return
        val store = secureTokenStore ?: return
        viewModelScope.launch {
            try {
                store.save(
                    StoredPaymentSession(
                        orderId = orderId,
                        accessToken = token,
                        verificationKey = request.verificationKey ?: "",
                        amount = request.amount,
                        expiresAtEpochSeconds = DateTimeUtils.getCurrentUnixTimestampString().toLong() + SESSION_TTL_SECONDS,
                        buildMode = request.buildMode ?: 4,
                        transactionId = transactionId,
                        includePending = request.isOtcPayment
                    )
                )
            } catch (e: Exception) {
                FiuuLogger.log("persistSession failed: $e")
            }
        } }

    fun clearStoredSession() {
        val store = secureTokenStore ?: return
        viewModelScope.launch { store.clear() } }

    fun onStartInitiate(params: Map<String, Any?>) {
        // isDeviceRooted() (Android) spawns a "which su" subprocess and blocks reading its
        // output — called directly (not suspend), it used to run on the caller's dispatcher,
        // which for FiuuPaymentSDK's LaunchedEffect(params) is Compose's main dispatcher. That
        // blocked the UI thread — and delayed dispatching the /initiate call itself — for however
        // long the process spawn took, before anything (even the loading overlay) could render.
        // viewModelScope runs on Dispatchers.Default, so launching here moves all of this,
        // including the root check, off the main thread.
        viewModelScope.launch {
            // Parsed before the root check so the session's dev-mode flags (logging, the test
            // fault injector's arming) are already in place when isDeviceRooted() reads them.
            val request = params.toPaymentRequest()
            if (isDeviceRooted()) {
                _uiState.update { it.copy(isRooted = true) }
                return@launch
            }
            // Opportunistic cleanup: there's no background job sweeping abandoned sessions (ones
            // that were persisted but never reached a terminal state and were never resumed), so
            // the start of a new attempt is used as the trigger instead. Only removes entries
            // whose TTL has already passed — never touches a still-valid session for another order.
            secureTokenStore?.let { store ->
                launch {
                    store.clearExpired(DateTimeUtils.getCurrentUnixTimestampString().toLong())
                }
            }
            this@PaymentViewModel.currentRequest = request
            // Guard here — the single entry point for every flow (express and non-express,
            // ApplePay/GooglePay/SamsungPay/generic) — rather than in initialize() alone, since
            // several express branches below skip initialize() entirely.
            if (request.billName.isNullOrEmpty() || request.billEmail.isNullOrEmpty() ||
                request.billMobile.isNullOrEmpty() || request.billDescription.isNullOrEmpty()
            ) {
                _uiState.update { it.copy(paymentError = PaymentError.CustomError("Billing detail is missing.")) }
                return@launch
            }
            _uiState.update {
                    it -> it.copy(
                        amount = request.amount?.toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L,
                        currency = request.currency ?: "",
                        orderId = request.orderId ?: "",
                        name = request.billName ?: "",
                        email = request.billEmail ?: "",
                        mobile = request.billMobile ?: "",
                        description = request.billDescription ?: "",
                        expressMode = request.expressMode ) }
            if (_channelFromAPITrigger) {
                fetchAvailableChannels(request)
            }

            if(request.expressMode) {
                if (!request.apMerchantId.isNullOrBlank() && request.channel.equals("ApplePay", ignoreCase = true)) {
                    // Apple Pay has no server-side "get channels" step to wait on — the sheet only
                    // needs the merchant id/amount/currency we already have from params. Same
                    // token-first ordering as expressGooglePay(): get the token, THEN call /express.
                    startExpressApplePay(request)
                } else if (request.channel.equals("GooglePay", ignoreCase = true)) {
                    triggerGooglePay()
                } else if (request.channel.equals("SamsungPay", ignoreCase = true)) {
                    triggerSamsungPay()
                } else {
                    expressPayment(request)
                }
            } else {
                    initialize(request) } } }

    fun initialize(params: PaymentRequest) {
        if (params.merchantId.isNullOrEmpty() || params.verificationKey.isNullOrEmpty() || params.appName.isNullOrEmpty() ||
            params.username.isNullOrEmpty() || params.password.isNullOrEmpty()
        ) {
            _uiState.update { it.copy(paymentError = PaymentError.CustomError("Merchant detail is missing.")) }
            return
        }
        if (!hasInternetConnection()) {
            FiuuLogger.log("INIT FAILED: no internet connection")
            _uiState.update { it.copy(paymentError = PaymentError.NoInternetConnection()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }

            val timestamp = DateTimeUtils.getCurrentUnixTimestampString()
            val nonce = CryptoUtils.generateNonce()
            val idempotencyKey = CryptoUtils.generateUuid()

            // 1. Get the payload WITHOUT the checksum field yet
            // This is used for checksum generation
            val payload = params.toInitApiPayload(includeChecksum = false)

            FiuuLogger.log("checksum payload: ${redactSensitiveMap(payload)}")
            FiuuLogger.log("timestamp: $timestamp")
            FiuuLogger.log("verificationkey present: ${params.verificationKey != null}")
            
            val checksum = generateChecksum(
                payload = payload,
                vkey = params.verificationKey ?: ""
            )

            val signature = CryptoUtils.generateSignature(
                method = "POST",
                path = "/initiate",
                timestamp = timestamp,
                nonce = nonce,
                checksum = checksum
            )

            val requestWithChecksum = params.copy(
                checksum = checksum
            )
            this@PaymentViewModel.currentRequest = requestWithChecksum

            try {
                val result = repo.initPayment(
                    request = requestWithChecksum,
                    nonce = nonce,
                    idempotencyKey = idempotencyKey,
                    timestamp = timestamp,
                    signature = signature
                )
                if (result == null) {
                    FiuuLogger.log("INIT FAILED: repository returned null (network/server error)")
                    _uiState.update { it.copy(paymentError = PaymentError.NetworkError()) }
                    return@launch
                }

                val token = result.accessToken
                if (token == null) {
                    FiuuLogger.log("INIT FAILED: ${result.message}")
                    _uiState.update { it.copy(paymentError = PaymentError.CustomError(result.message)) }
                    return@launch
                }

                FiuuLogger.log("INIT SUCCESS: channels=${result.channels.size}")
                this@PaymentViewModel.accessToken = token
                this@PaymentViewModel.transactionId = null
                this@PaymentViewModel.cardTokensFetched = false
                persistSession(token)

                val domainChannels = result.channels.map { initChannel ->
                    PaymentChannel(
                        name = initChannel.name,
                        title = initChannel.title,
                        channelType = initChannel.channelType,
                        logoUrl3 = initChannel.logoUrl3,
                        status = 1
                    )
                }

                val requestedChannel = params.channel?.trim()
                if (!requestedChannel.isNullOrEmpty()) {
                    val matchedChannel = domainChannels.find { it.name.equals(requestedChannel, ignoreCase = true) }
                    if (matchedChannel != null) {
                        selectedChannel = matchedChannel
                        // Matched case-insensitively, so carry the backend's spelling forward —
                        // /process sends `channel` as-is and CASH_CHANNEL_WIRE_NAMES is case-sensitive.
                        this@PaymentViewModel.currentRequest = requestWithChecksum.copy(
                            channel = matchedChannel.name,
                            channelType = matchedChannel.channelType
                        )
                    } else if (!requestedChannel.equals("multi", ignoreCase = true)) {
                        FiuuLogger.log("check channel: not listed")
                    } else {
                        FiuuLogger.log("check channel: multichannel")
                    }
                }

                _uiState.update {
                    it.copy(
                        // When the live GetAvailableChannelById lookup has actually succeeded,
                        // keep whatever fetchAvailableChannels() has written rather than overwriting
                        // with /initiate's smaller channel list, regardless of which call finishes
                        // first. Otherwise (lookup still pending, skipped due to missing
                        // credentials, or failed) fall back to /initiate's channels so checkout
                        // isn't left with none.
                        channels = if (_channelFromAPITrigger && liveChannelsLoaded) it.channels else appendHardcodedInstallmentChannel(domainChannels),
                        initiateChannels = domainChannels,
                        // Channel resolution has settled for this attempt — screens picking a
                        // channel out of `channels` can now tell "still loading" apart from
                        // "loaded, genuinely no match" instead of only checking channels.isEmpty().
                        channelsResolved = true,
                        loading = false
                    )
                }

            } catch (e: ApiException) {
                FiuuLogger.log("INIT API ERROR: ${e.message} (${e.statusCode}, ${e.errorCode})")
                _uiState.update { it.copy(paymentError = PaymentError.CustomError(msg = e.message, statusCode = e.statusCode, errorCode = e.errorCode)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                FiuuLogger.log("INIT ERROR: $e")
                _uiState.update { it.copy(paymentError = PaymentError.CustomError(msg = e.message)) }
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        } }

    // Legacy GetAvailableChannelById lookup — separate host/checksum/auth scheme from /initiate
    // (see CryptoUtils.generateChannelChecksum), fetched independently so the channel picker gets
    // live data rather than only /initiate's smaller list. Runs fire-and-forget
    // alongside initialize()/expressPayment(): it only ever writes state.channels, and
    // initialize()'s own channel write is skipped whenever _channelFromAPITrigger is true (see
    // below) so the two calls can't clobber each other regardless of which finishes first.
    // Failures are swallowed here rather than surfaced as paymentError — this is a supplementary
    // fetch, not part of the init->process->stream flow that FiuuPaymentSDK treats as fatal.
    private fun fetchAvailableChannels(request: PaymentRequest) {
        val merchantId = request.merchantId
        val appName = request.appName
        val verificationKey = request.verificationKey
        val currency = request.currency
        val username = request.username
        val password = request.password
        if (merchantId.isNullOrEmpty() || appName.isNullOrEmpty() || verificationKey.isNullOrEmpty() ||
            currency.isNullOrEmpty() || username.isNullOrEmpty() || password.isNullOrEmpty()
        ) {
            FiuuLogger.log("fetchAvailableChannels: missing merchantId/appName/verificationKey/currency/username/password, skipping")
            return
        }
        if (!hasInternetConnection()) {
            // Log only — initialize()'s own guard (running right after this in onStartInitiate)
            // is what sets paymentError; this is a best-effort live-channel pre-fetch that already
            // falls back silently to /initiate's channel list on any other failure.
            FiuuLogger.log("fetchAvailableChannels: no internet connection, skipping")
            return
        }
        viewModelScope.launch {
            val checksum = CryptoUtils.generateChannelChecksum(
                merchantId = merchantId,
                appName = appName,
                currency = currency,
                verificationKey = verificationKey
            )
            val response = repo.getAvailableChannelById(
                merchantId = merchantId,
                currency = currency,
                checksum = checksum,
                username = username,
                password = password
            )
            if (response?.status == true) {
                // Some PLUGIN channels come back with no logo_url3 — fall back to the same
                // icon these channel names use elsewhere in the payment_options_v2 asset set.
                val pluginLogoFallbacks = mapOf(
                    "Mastercard C2P" to "https://pay.fiuu.com/RMS/G/assets/img/payment_options_v2/credit23.png?v=1.1",
                    "Google Pay" to "https://pay.fiuu.com/RMS/G/assets/img/payment_options_v2/GooglePay.png?v=1.1"
                )
                val channels = response.result.map { channel ->
                    val fallback = channel.maskName?.let { maskName ->
                        pluginLogoFallbacks.entries.firstOrNull { it.key.equals(maskName, ignoreCase = true) }?.value
                    }
                    if (channel.channelType == "PLUGIN" && channel.logoUrl3 == null && fallback != null) {
                        channel.copy(logoUrl3 = fallback)
                    } else {
                        channel
                    }
                }
                val channelsWithInstallment = appendHardcodedInstallmentChannel(channels)
                liveChannelsLoaded = true
                _uiState.update { it.copy(channels = channelsWithInstallment) }
            } else {
                FiuuLogger.log("fetchAvailableChannels: request failed or status=false: $response")
            }
        } }

    // POST card-tokens (Bearer accessToken + nonce/idempotency-key/timestamp/x-signature, same
    // shape as /process) is now the sole source for the saved-card list — /initiate no longer
    // populates state.cardTokens. Only CardManualEntryScreen shows that list, and it's only
    // reachable from the C2P widget, so this is fetched lazily from there, once per accessToken.
    // A failure just leaves no saved cards shown rather than blocking checkout, and is retried
    // on the next redirect. The loading overlay is raised synchronously, before the screen
    // switches, so the form is never interactive with the list still missing.
    private fun fetchCardTokens(accessToken: String, request: PaymentRequest) {
        if (cardTokensFetched || cardTokensJob?.isActive == true) return
        _uiState.update { it.copy(loading = true, loadingMessage = "Loading saved cards...") }
        cardTokensJob = viewModelScope.launch {
            try {
                val timestamp = DateTimeUtils.getCurrentUnixTimestampString()
                val nonce = CryptoUtils.generateNonce()
                val idempotencyKey = CryptoUtils.generateUuid()

                val payload = request.toCardTokensApiPayload(includeChecksum = false)
                val checksum = generateChecksum(payload = payload, vkey = request.verificationKey ?: "")
                val signature = CryptoUtils.generateSignature(
                    method = "POST",
                    path = "/card-tokens",
                    timestamp = timestamp,
                    nonce = nonce,
                    checksum = checksum
                )

                val cardTokensRequest = CardTokensRequestDto(
                    checksum = checksum,
                    billName = payload["xp_bill_name"],
                    billEmail = payload["xp_bill_email"],
                    billMobile = payload["xp_bill_mobile"],
                    billDescription = payload["xp_bill_description"]
                )

                val response = repo.fetchCardTokens(
                    accessToken = accessToken,
                    request = cardTokensRequest,
                    nonce = nonce,
                    idempotencyKey = idempotencyKey,
                    timestamp = timestamp,
                    signature = signature
                )
                if (response == null) {
                    FiuuLogger.log("fetchCardTokens: request failed, leaving cardTokens empty")
                    return@launch
                }
                cardTokensFetched = true
                _uiState.update {
                    it.copy(
                        cardTokens = response.tokens.map { dto -> dto.toDomain() },
                        cardTokensRemovable = response.removable == 1
                    )
                }
            } finally {
                _uiState.update { it.copy(loading = false, loadingMessage = "Loading...") }
            }
        }
    }

    fun updateName(value: String) {
        _uiState.update { it.copy(
            name = value,
            formError = if (it.formError == FormError.EMPTY_NAME)
                null else it.formError
        ) } }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(
            email = value,
            formError = if (it.formError == FormError.INVALID_EMAIL) null else it.formError
        ) } }

    fun updatePhone(value: String) {
        _uiState.update { it.copy(
            mobile = value,
            formError = if ( it.formError == FormError.INVALID_PHONE ) null else it.formError
        ) } }

    fun updateDescription(value: String) {
        _uiState.update { it.copy(description = value) } }

    fun onChannelSelected(channel: PaymentChannel) {
        com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.recordBreadcrumb(
            area = "UI",
            process = "onChannelSelected",
            message = "Selected channel: ${channel.name} (${channel.channelType})"
        )
        selectedChannel = channel
        currentRequest = currentRequest?.copy(
            channel = channel.name,
            channelType = channel.channelType
        )

        _uiState.update { it.copy(selectedChannel = channel) } }

    fun setPaymentError(error: PaymentError) {
        _uiState.update {
            it.copy(paymentError = error)
        } }

    // Mirrors onClickToPaySuccess()/startApplePay(): no channel object needed and no
    // onChannelSelected() call — putChannel() resolves the CardPayment/CC branch purely from
    // manualCardNumber, same as it resolves ApplePay/GooglePay/SamsungPay from their tokens.
    fun payWithManualCard(
        cardHolderName: String,
        cardNumber: String,
        expiry: String,
        cvv: String,
        saveCard: Boolean
    ) {
        currentRequest = currentRequest?.copy(
            manualCardHolderName = cardHolderName,
            manualCardNumber = cardNumber,
            manualCardExpiry = expiry,
            manualCardCvv = cvv,
            manualCardSaveForFuture = saveCard,
            // Clear any leftover instalment selection from a prior attempt on this session (e.g.
            // user backed out of CardInstalmentEntryScreen and paid a normal card instead) so
            // putChannel() doesn't mistakenly take the instalment branch.
            manualCardInstalmentMonth = null,
            // putChannel() checks savedCardToken before manualCardNumber, so a leftover token
            // would charge the saved card instead of the one just typed in.
            savedCardToken = null
        )
        startPayment()
    }

    // CardManualEntryScreen's Pay button when a "Saved Card" entry is selected and the user has
    // confirmed the pay-with-saved-card dialog. Mirrors payWithManualCard()'s clearing of leftover
    // instalment state; also clears any manual-entry fields from a prior attempt on this session
    // (e.g. user typed a new card, backed out, then paid with a saved one instead) so putChannel()
    // takes the savedCardToken branch rather than a stale manualCardNumber one.
    fun payWithSavedCard(token: String) {
        currentRequest = currentRequest?.copy(
            savedCardToken = token,
            manualCardHolderName = null,
            manualCardNumber = null,
            manualCardExpiry = null,
            manualCardCvv = null,
            manualCardSaveForFuture = false,
            manualCardInstalmentMonth = null
        )
        startPayment()
    }

    // CardInstalmentEntryScreen's Pay button. instalmentMonth is the InstlMonth the user picked
    // from fetchInstalmentTenure()'s result. putChannel() sends xp_channel="CREDIT10" for this
    // flow — the tenure result's bank-specific Channel (e.g. "CREDITBA") is not used for /process.
    fun payWithInstalmentCard(
        cardHolderName: String,
        cardNumber: String,
        expiry: String,
        cvv: String,
        instalmentMonth: Int
    ) {
        currentRequest = currentRequest?.copy(
            manualCardHolderName = cardHolderName,
            manualCardNumber = cardNumber,
            manualCardExpiry = expiry,
            manualCardCvv = cvv,
            manualCardSaveForFuture = false,
            manualCardInstalmentMonth = instalmentMonth.toString(),
            savedCardToken = null
        )
        startPayment()
    }

    // Calls POST /installment/installment-tenure whenever CardInstalmentEntryScreen's card number
    // and expiry become valid (canPay transitions false->true, or either field then changes while
    // still valid) — see CardInstalmentEntryScreen's LaunchedEffect. Distinct signing scheme from
    // every other endpoint here: on top of the usual x-nonce/x-timestamp/x-signature gateway
    // headers, the JSON body itself carries its own HMAC-SHA256 "signature" field
    // (CryptoUtils.hmacSha256Hex) over merchantId+pan+month+year+amount+dateTime.
    //
    // Only the ineligible-card error shape (Status/error_code/error_desc) is confirmed against a
    // real payload so far (see InstalmentTenureDto.kt) — the success shape (actual plan list)
    // isn't, so CardInstalmentEntryScreen surfaces instalmentTenureResult.errorDesc rather than
    // rendering plan options.
    fun fetchInstalmentTenure(cardNumber: String, expiry: String) {
        val request = currentRequest ?: run { FiuuLogger.log("DEBUG fetchInstalmentTenure: currentRequest is null, returning"); return }
        val token = accessToken ?: run { FiuuLogger.log("DEBUG fetchInstalmentTenure: accessToken is null, returning"); return }
        val merchantId = request.merchantId ?: return
        val verificationKey = request.verificationKey ?: return
        val amount = request.amount ?: return

        val expiryParts = expiry.split("/")
        if (expiryParts.size != 2) return
        val month = expiryParts[0]
        // CardInstalmentEntryScreen's expiry field only collects a 2-digit YY (MM/YY) — expand to
        // 4-digit YYYY for mp_cc_year, since the field is 2-digit only in the UI, not the wire.
        val year = "20" + expiryParts[1]

        viewModelScope.launch {
            _uiState.update { it.copy(instalmentTenureLoading = true) }

            val timestamp = DateTimeUtils.getCurrentUnixTimestampString()
            val nonce = CryptoUtils.generateNonce()
            val dateTime = DateTimeUtils.getLocalDateTimeCompactString()

            val instalmentSignature = CryptoUtils.hmacSha256Hex(
                key = verificationKey,
                message = merchantId + cardNumber + month + year + amount + dateTime
            )

            val payload: Map<String, Any?> = mapOf(
                "mp_merchant_ID" to merchantId,
                "mp_cc_pan" to cardNumber,
                "mp_cc_month" to month,
                "mp_cc_year" to year,
                "mp_amount" to amount,
                "date_time" to dateTime,
                "signature" to instalmentSignature
            )

            val checksum = generateChecksum(payload = payload, vkey = verificationKey)

            val signature = CryptoUtils.generateSignature(
                method = "POST",
                path = "/installment/installment-tenure",
                timestamp = timestamp,
                nonce = nonce,
                checksum = checksum
            )

            val tenureRequest = InstalmentTenureRequestDto(
                merchantId = merchantId,
                ccPan = cardNumber,
                ccMonth = month,
                ccYear = year,
                amount = amount,
                dateTime = dateTime,
                signature = instalmentSignature
            )

            try {
                val result = repo.fetchInstalmentTenure(
                    token = token,
                    request = tenureRequest,
                    nonce = nonce,
                    timestamp = timestamp,
                    signature = signature
                )
                _uiState.update { it.copy(instalmentTenureLoading = false, instalmentTenureResult = result) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                FiuuLogger.log("INSTALMENT TENURE FETCH ERROR: $e")
                _uiState.update { it.copy(instalmentTenureLoading = false, instalmentTenureResult = null) }
            }
        }
    }

    // Calls POST /token/remove (Bearer accessToken, body { "token": ... }) and drops the card
    // from state.cardTokens on success so the "Saved Card" list updates immediately.
    fun deleteSavedCard(card: CardTokenInit) {
        val token = accessToken ?: run { FiuuLogger.log("DEBUG deleteSavedCard: accessToken is null, returning"); return }
        viewModelScope.launch {
            val removed = repo.removeCardToken(accessToken = token, cardToken = card.token)
            if (removed) {
                _uiState.update { it.copy(cardTokens = it.cardTokens.filterNot { existing -> existing.token == card.token }) }
            }
        }
    }

    fun startPayment(extraParams: Map<String, String>? = null) {
        val request = currentRequest ?: run { FiuuLogger.log("DEBUG startPayment: currentRequest is null, returning"); return }
        val token = accessToken ?: run { FiuuLogger.log("DEBUG startPayment: accessToken is null, returning"); return }
        // Mastercard C2P's PLUGIN channel isn't a normal /process channel — the user selects it
        // from the grid then taps Pay like any other channel, but that tap must open the Click to
        // Pay widget (same as the QuickPay button) rather than send the channel straight to
        // /process. Once the widget succeeds, onClickToPaySuccess() stamps request.c2pData and
        // calls startPayment() again itself, so that retry falls through to the normal flow below.
        if (request.c2pData == null &&
            selectedChannel?.channelType == "PLUGIN" &&
            selectedChannel?.maskName?.equals("Mastercard C2P", ignoreCase = true) == true
        ) {
            triggerClickToPay()
            return
        }
        // Same reasoning as the Mastercard C2P branch above — Google Pay's PLUGIN channel isn't a
        // normal /process channel either. Selecting it from the grid then tapping Pay must open
        // the Google Pay sheet (same as the QuickPay button) rather than send the channel straight
        // to /process. On success, processGooglePayStart() stamps request.googlePayData and calls
        // startPayment() again itself, so that retry falls through to the normal flow below.
        if (request.googlePayData == null &&
            selectedChannel?.channelType == "PLUGIN" &&
            selectedChannel?.maskName?.equals("Google Pay", ignoreCase = true) == true
        ) {
            triggerGooglePay()
            return
        }
        if (selectedChannel == null && request.googlePayData == null && request.c2pData == null && request.samsungPayData == null && request.applePayToken == null && request.manualCardNumber == null && request.savedCardToken == null) {
                run { FiuuLogger.log("DEBUG startPayment: selectedChannel is null, returning: ${selectedChannel?.title}"); return }
            }
        if (!hasInternetConnection()) {
            FiuuLogger.log("START PAYMENT FAILED: no internet connection")
            _uiState.update { it.copy(paymentError = PaymentError.NoInternetConnection()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }

            val timestamp = DateTimeUtils.getCurrentUnixTimestampString()
            val nonce = CryptoUtils.generateNonce()
            val idempotencyKey = CryptoUtils.generateUuid()

            val payload: Map<String, Any?> = request.toProcessApiPayload(includeChecksum = false) +
                (extraParams ?: emptyMap())

            FiuuLogger.log("checksum payload: ${redactSensitiveMap(payload)}")

            val checksum = generateChecksum(
                payload = payload,
                vkey = request.verificationKey ?: ""
            )

            val signature = CryptoUtils.generateSignature(
                method = "POST",
                path = "/process",
                timestamp = timestamp,
                nonce = nonce,
                checksum = checksum
            )

            val startRequest = PaymentProcessRequestDto(
                channel = payload["xp_channel"] as? String,
                channelType = payload["xp_channel_type"] as? String,
                checksum = checksum,
                billName = payload["xp_bill_name"] as? String,
                billEmail = payload["xp_bill_email"] as? String,
                billMobile = payload["xp_bill_mobile"] as? String,
                billDescription = payload["xp_bill_description"] as? String,
                googlePay = payload["xp_googlepay"] as? JsonObject,
                click2Pay = payload["xp_click2pay"] as? JsonObject,
                samsungPay = payload["xp_samsungpay"] as? JsonObject,
                applePayToken = payload["xp_applepay"] as? JsonObject,
                cardHolderName = payload["xp_card_holder_name"] as? String,
                creditCardNo = payload["xp_credit_card_no"] as? String,
                creditCardCvv = payload["xp_credit_card_cvv"] as? String,
                creditCardExpiry = payload["xp_credit_card_expiry"] as? String,
                tokenStatus = payload["xp_token_status"] as? String,
                installmentMonth = payload["xp_installmonth"] as? String,
                creditCardToken = payload["xp_credit_card_token"] as? String
            )
            // The typed-in card is in startRequest now — don't keep the PAN/CVV/expiry around for
            // the rest of the session. Nothing after /process reads them.
            if (request.manualCardNumber != null) {
                currentRequest = currentRequest?.copy(
                    manualCardNumber = null,
                    manualCardCvv = null,
                    manualCardExpiry = null
                )
            }

        FiuuLogger.log("DEBUG startRequest: ${redactSensitiveJson(Json.encodeToString(startRequest))}")

            // Tracks whether a WebView open was dispatched in this attempt — the loading overlay
            // must stay up until the WebView itself reports success/failure via callback (see
            // openPayment()), rather than clearing the instant the async open is dispatched, which
            // used to expose a blank frame before the page had actually rendered.
            var openedWebView = false
            try {
                FiuuLogger.log("STARTING PAYMENT")
                transactionId = null
                streamDropCount = 0
                deferredWebViewFailure = null
                resolvingAttempt.value = false
                val result = repo.createPayment(
                    token = token,
                    request = startRequest,
                    nonce = nonce,
                    idempotencyKey = idempotencyKey,
                    timestamp = timestamp,
                    signature = signature
                )

                if (result != null) {
                    rememberTransactionId(result.transactionId, token)
                    val requestType = result.requestType
                    if(requestType == "REDIRECT"){
                            val url = result.requestURL
                        if (url == null) {
                            FiuuLogger.log("CREATE PAYMENT ERROR: requestURL is null — check @SerialName mapping vs actual API response field name")
                            _uiState.update { it.copy(paymentError = PaymentError.GenericError()) }
                            return@launch
                        }
                        paymentUrl = url
                        paymentMethod = result.requestMethod
                        paymentRequestData = result.requestData.toRequestDataMap()
                        val bridge = webViewBridge ?: run {
                            FiuuLogger.log("CREATE PAYMENT ERROR: webViewBridge is null")
                            _uiState.update { it.copy(paymentError = noWebViewBridgeError()) }
                            return@launch
                        }
                        _uiState.update { it.copy(loadingMessage = "Loading payment page...") }
                        openPayment(bridge, result.urlType)
                        openedWebView = true
                        startListeningStream(token)
                    } else if (requestType == "RESPONSE") {
                        val response = PaymentResponseDto(
                            status = result.status ?: "",
                            statusCheck = result.statusCode == 200,
                            message = result.message ?: "",
                            requestURL = result.requestURL,
                            requestMethod = result.requestMethod,
                            requestType = result.requestType,
                            referenceNo = result.referenceNo,
                            transactionId = result.transactionId,
                            amount = result.amount,
                            currency = result.currency,
                            channel = result.channel,
                            skey = result.skey,
                            tranID = result.tranID,
                            paydate = result.paydate,
                            appcode = result.appcode,
                            errorCode = result.errorCode,
                            errorDesc = result.errorDesc
                        )
                        if (result.pInstruction == 1) {
                            // Offline/OTC channel (e.g. Cash-711): hold the final result back and
                            // show the barcode/instruction screen instead of closing immediately —
                            // the customer still has to pay at a physical counter.
                            //
                            // Issuing the voucher is not payment — never report it as "00". The
                            // held response only supplies IDs; the status comes from /inquire when
                            // the screen closes (see resolveOfflineVoucher()).
                            pendingInstructionResult = response
                            _uiState.update {
                                it.copy(paymentInstruction = buildPaymentInstruction(result, response))
                            }
                        } else {
                            _uiState.update { it.copy(paymentSuccess = response) }
                        }
                    } else {
                            val url = result.requestURL
                        if (url == null) {
                            FiuuLogger.log("CREATE PAYMENT ERROR: requestURL is null — check @SerialName mapping vs actual API response field name")
                            _uiState.update { it.copy(paymentError = PaymentError.GenericError()) }
                            return@launch
                        }
                        paymentUrl = url
                        paymentMethod = result.requestMethod
                        paymentRequestData = result.requestData.toRequestDataMap()
                        val bridge = webViewBridge ?: run {
                            FiuuLogger.log("CREATE PAYMENT ERROR: webViewBridge is null")
                            _uiState.update { it.copy(paymentError = noWebViewBridgeError()) }
                            return@launch
                        }
                        _uiState.update { it.copy(loadingMessage = "Loading payment page...") }
                        openPayment(bridge, result.urlType)
                        openedWebView = true
                        startListeningStream(token)
                     }
                } else {
                    _uiState.update { it.copy(paymentError = PaymentError.NetworkError()) }
                    return@launch
                }
            } catch (e: ApiException) {
                FiuuLogger.log("CREATE PAYMENT API ERROR: ${e.message} (${e.statusCode}, ${e.errorCode})")
                _uiState.update { it.copy(paymentError = PaymentError.CustomError(msg = e.message, statusCode = e.statusCode, errorCode = e.errorCode)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                FiuuLogger.log("API ERROR: $e")
                _uiState.update { it.copy(paymentError = PaymentError.GenericError()) }
            } finally {
                // If a WebView was opened, its onSuccess/onFailure callback (or the back-button
                // onClose handler) owns clearing loading from here on — clearing it here too would
                // reintroduce the blank-flash bug this was added to fix.
                if (!openedWebView) {
                    _uiState.update { it.copy(loading = false) }
                }
            }
        } }

    // barcodeValue follows the rule given for the instruction screen: prefer payTxnID, falling
    // back to transactionId only when payTxnID is absent.
    private fun buildPaymentInstruction(
        result: PaymentCreateResponse,
        response: PaymentResponseDto
    ): PaymentInstructionInfo {
        val barcodeValue = result.payTxnID ?: result.transactionId ?: ""
        val currency = response.currency ?: result.orderCurrency ?: ""
        val amount = result.orderTxnAmt ?: response.amount ?: ""
        return PaymentInstructionInfo(
            serviceName = result.orderChannel ?: response.channel ?: "",
            logoUrl = selectedChannel?.logoUrl3,
            barcodeValue = barcodeValue,
            barcodeType = result.orderBarcodeType,
            payableAmountText = "$currency$amount",
            dueDateNote = result.orderTxnDueDatetime?.let { "** Please pay before $it" }
        )
    }

    // Fired from PaymentInstructionScreen's CLOSE button (and system back) — resolves the held
    // voucher through the same uiState.paymentSuccess -> onPaymentResult/onRequestClose path every
    // other outcome uses. Claims resolvingAttempt so the session timeout can't report a second result.
    fun confirmPaymentInstructionAndClose() {
        val issued = pendingInstructionResult ?: return
        if (!claimAttempt("payment instruction close")) return
        pendingInstructionResult = null
        resolveOfflineVoucher(issued)
    }

    // The customer may already have paid at the counter while the barcode was showing, so ask
    // /inquire. Anything after that (a still-unpaid voucher) is tracked by the merchant's own
    // backend, not this SDK. See documentations/offline-voucher-result-design.md.
    private fun resolveOfflineVoucher(issued: PaymentResponseDto) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, loadingMessage = "Confirming payment...") }
            delay(OFFLINE_VOUCHER_INQUIRE_DELAY_MS)
            val inquired = fetchInquireStatus()?.takeIf { it.status != null }?.toPaymentResponseDto()
            FiuuLogger.log("XDK_INQUIRE Offline voucher inquire: status=${inquired?.status}")
            // Leave `loading` and the instruction screen up — the SDK is closing (see the overlay
            // condition in FiuuPaymentSDK); clearing them flashed the channel grid.
            _uiState.update { it.copy(paymentSuccess = offlineVoucherResult(issued, inquired)) }
        }
    }

    // The backend sends "" (a JSON string), not {}, when a channel has no gateway-specific data
    // to submit — anything that isn't a JSON object is treated as "no data" rather than a parse
    // error (see PaymentCreateResponse.requestData).
    private fun JsonElement?.toRequestDataMap(): Map<String, String>? {
        val obj = this as? JsonObject ?: return null
        return obj.mapValues { (_, v) -> v.jsonPrimitive.content }
    }

    fun openPayment(webViewBridge: WebViewBridge, urlType: String? = null) {
        val url = paymentUrl ?: run {
            FiuuLogger.log("openPayment: paymentUrl is null")
            _uiState.update { it.copy(paymentError = PaymentError.GenericError()) }
            return
        }

        com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.recordBreadcrumb(
            area = "WEBVIEW",
            process = "openPayment",
            message = "Opening webview urlType=$urlType url=${url.take(60)}"
        )

        // Guards against onClose firing right after onFailure already resolved this attempt —
        // Android's redirect-failure branch calls onFailure() then onClose() directly, and iOS
        // fires onClose as the dismiss-animation completion right after onFailure. Without this,
        // that pairing would run handleWebViewClosedByUser() a second time, clobbering the
        // already-set paymentError with a bogus "user cancelled" outcome.
        //
        // Deliberately NOT set from onSuccess: on Android, onSuccess fires from onPageFinished,
        // which runs on every page load inside the WebView (bank page, OTP page, etc.), not just
        // a final outcome — latching this flag there would permanently block a later genuine
        // back-press close for the rest of the attempt. (On iOS, onSuccess is a true terminal
        // "success=true" signal immediately followed by onClose — that pairing can still reach
        // handleWebViewClosedByUser(), which resolves it via /inquire same as any other close, just
        // with a redundant "Closing..." flash; a real WebViewBridge-level user-close signal would
        // be a cleaner fix if this shows up as a problem on iOS.)
        webViewResultReported = false

        // Only Click to Pay's follow-up challenge step still needs the Fiuu session token
        // attached — every other channel's redirect target (e.g. FPX's bank endpoints) is a
        // third-party host that has no use for it and shouldn't receive it. Same
        // Authorization: Bearer scheme used for /process, /subscribe, /inquire, riding in a
        // header instead of the URL/a POST body so it never ends up in browser history, server
        // logs, or a Referer header.
        val headers = if (currentRequest?.c2pData != null) {
            accessToken?.let { mapOf("Authorization" to "Bearer $it") }
        } else null
        val closeButtonDisplay = currentRequest?.closeButtonDisplay ?: true

        // openGetUrl/openPostUrl touch the Activity/WebView (Android) or UIKit (iOS) directly —
        // both platforms require the main thread/queue, but openPayment is called from
        // viewModelScope's Dispatchers.Default coroutines.
        when {
            urlType == "data" -> {
                // Inline HTML payload — nothing to submit, just render it.
                viewModelScope.launch(Dispatchers.Main) {
                    webViewBridge.openGetUrl(
                        url = url,
                        onSuccess = { _uiState.update { it.copy(loading = false) } },
                        onFailure = { message -> webViewResultReported = true; handleWebViewFailure(message) },
                        onClose = { if (!webViewResultReported) handleWebViewClosedByUser() },
                        urlType = urlType,
                        headers = headers,
                        closeButtonDisplay = closeButtonDisplay,
                        onResultPage = { resultUrl, loaded -> if (!webViewResultReported) handleResultPageReached(resultUrl, loaded) },
                        onAppPaused = { onAppPaused() },
                        onAppResume = { checkPaymentStatusOnResume() }
                    ) }
            }
            paymentMethod == "POST" -> {
                // Some channels (e.g. FPX) return their own gateway-specific form fields in
                // requestData that must be submitted as a real POST body, not just navigated to.
                viewModelScope.launch(Dispatchers.Main) {
                    webViewBridge.openPostUrl(
                        url = url,
                        postData = paymentRequestData ?: emptyMap(),
                        onSuccess = { _uiState.update { it.copy(loading = false) } },
                        onFailure = { message -> webViewResultReported = true; handleWebViewFailure(message) },
                        onClose = { if (!webViewResultReported) handleWebViewClosedByUser() },
                        urlType = urlType,
                        closeButtonDisplay = closeButtonDisplay,
                        onResultPage = { resultUrl, loaded -> if (!webViewResultReported) handleResultPageReached(resultUrl, loaded) },
                        onAppPaused = { onAppPaused() },
                        onAppResume = { checkPaymentStatusOnResume() }
                    )
                }
            }
            else -> {
                viewModelScope.launch(Dispatchers.Main) {
                    webViewBridge.openGetUrl(
                        url = url,
                        onSuccess = { _uiState.update { it.copy(loading = false) } },
                        onFailure = { message -> webViewResultReported = true; handleWebViewFailure(message) },
                        onClose = { if (!webViewResultReported) handleWebViewClosedByUser() },
                        urlType = urlType,
                        headers = headers,
                        closeButtonDisplay = closeButtonDisplay,
                        onResultPage = { resultUrl, loaded -> if (!webViewResultReported) handleResultPageReached(resultUrl, loaded) },
                        onAppPaused = { onAppPaused() },
                        onAppResume = { checkPaymentStatusOnResume() }
                    )
                }
            }
        } }

    // Without this, a WebView load failure (SSL/cert error, host unreachable, HTTP error page)
    // previously left the user stuck on a blank overlay with no feedback until the 60s session
    // timeout in FiuuPaymentSDK.kt eventually fired — surface it immediately instead.
    private fun handleWebViewFailure(message: String?) {
        FiuuLogger.log("WEBVIEW LOAD FAILED: $message")
        if (appInBackground) {
            FiuuLogger.log("XDK_STREAM App in background — holding the load failure until resume")
            deferredWebViewFailure = message ?: "Failed to load payment page"
            return
        }
        // Another path may already be ending the attempt (e.g. the stream confirming via /inquire).
        // Every winner closes the WebView itself, so cancelling the stream here would only throw
        // away its result and report this failure instead.
        if (!claimAttempt("webview load failed")) return
        com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.logHostDecline(
            process = com.fiuu.xdkandroid.telemetry.model.TelemetryProcess.WEBVIEW_FAILURE,
            statusCode = "WEBVIEW_ERROR",
            statusMessage = message ?: "Failed to load payment page",
            source = com.fiuu.xdkandroid.telemetry.model.TelemetryOrigin.INTERNAL_SDK,
            referenceNumber = currentRequest?.orderId ?: ""
        )
        cancelStream("webview load failed")
        // close() touches the Activity/WebView (Android) or UIKit (iOS) directly, so it must run
        // on the main thread/queue. Without closing here, the orphaned WebView (added directly to
        // the activity content view on Android) keeps covering the app after the error is published.
        //
        // The error is published only after close() has run: expressGooglePay() returns — and
        // disposes this ViewModel, cancelling viewModelScope — the moment paymentError is set, so
        // setting it first cancelled the still-queued close() and left the 3DS page stuck on
        // screen with nothing behind it (confirmed on device, 2026-10-01).
        viewModelScope.launch(Dispatchers.Main) {
            webViewBridge?.close()
            _uiState.update { it.copy(loading = false) }
            setPaymentError(PaymentError.CustomError(msg = message ?: "Failed to load payment page"))
        } }

    // Fired when the user dismisses the payment WebView via the system back button (see
    // AndroidWebViewBridge's OnBackPressedCallback). The SSE stream push is the only other signal
    // that would resolve this attempt, so it's cancelled first to avoid a stale result racing in
    // after the user has already backed out. A raw cancellation isn't trustworthy on its own —
    // the gateway may have already reached a terminal state server-side (e.g. the user completed
    // 3DS in the WebView a split-second before hitting back) — so confirm with one /inquire lookup
    // before falling back to a defined USER_CANCELLED result.
    // Android reached the gateway's result page (MOLPay/result.php) and is letting it load — it
    // redirects to the backend's /pay/callback/<id>, which triggers the stream's result event.
    // Give the stream a bounded window to deliver before closing the WebView and falling back to
    // the same /inquire path a user close takes — without the bound, a stream that never delivers
    // would leave the user on the result page until the session timeout.
    //
    // Reported twice per result page: loaded=false as navigation to it starts, loaded=true once it
    // has finished loading. Non-PLUGIN channels act on the first, exactly as before. PLUGIN
    // payments act on the second: for /express sessions result.php never moves on to
    // /pay/callback (it stays on "Redirecting…"), so no stream event ever arrives — the loaded
    // result.php?k=… page is itself the completion signal, resolved via /inquire.
    private fun handleResultPageReached(url: String, loaded: Boolean) {
        val plugin = currentRequest?.isPluginPayment == true
        if (plugin && loaded && resultPageKey(url) != null) {
            resolvePluginResultByInquire()
            return
        }
        // Everything else takes the stream-wait path below, at its pre-PLUGIN-change moment:
        // non-PLUGIN on navigation (loaded=false), PLUGIN without a k once the page has loaded.
        val actsNow = if (plugin) loaded else !loaded
        if (!actsNow) return
        if (resultPageWaitJob?.isActive == true) return
        val outcome = streamOutcome
        if (streamJob?.isActive != true || outcome == null) {
            viewModelScope.launch(Dispatchers.Main) { webViewBridge?.close() }
            handleWebViewClosedByUser()
            return
        }
        FiuuLogger.log("XDK_STREAM Result page reached, waiting up to ${RESULT_PAGE_STREAM_WAIT_MS}ms for stream")
        // Deliberately doesn't claim the attempt: it hands off to the stream or to
        // handleWebViewClosedByUser(), and each of those claims it itself.
        _uiState.update { it.copy(loading = true, loadingMessage = "Confirming payment...") }
        resultPageWaitJob = viewModelScope.launch {
            val delivered = withTimeoutOrNull(RESULT_PAGE_STREAM_WAIT_MS) { outcome.await() } == true
            if (delivered) {
                // startListeningStream carries on from here (closes the WebView, inquire, result).
                FiuuLogger.log("XDK_STREAM Stream delivered after result page")
            } else {
                FiuuLogger.log("XDK_STREAM No stream result after result page, falling back to /inquire")
                withContext(Dispatchers.Main) { webViewBridge?.close() }
                handleWebViewClosedByUser()
            }
        }
    }

    // PLUGIN result page (with k) has loaded: the stream is no longer the result source. Close the
    // WebView and poll /inquire (INQUIRE_POLL_DELAYS_MS) until it reports a terminal status —
    // the first lookup can still race the gateway and see "22" (pending). If every poll stays
    // pending the last "22" is returned so the host can resolve it later via
    // resumeFiuuPaymentStatus; if every poll fails, same USER_CANCELLED fallback as a user close.
    private fun resolvePluginResultByInquire() {
        if (resultPageWaitJob?.isActive == true) return
        if (!claimAttempt("plugin result page")) return
        FiuuLogger.log("XDK_STREAM Plugin result page with k — resolving via /inquire")
        webViewResultReported = true
        cancelStream("plugin result page with k")
        _uiState.update { it.copy(loading = true, loadingMessage = "Confirming payment...") }
        resultPageWaitJob = viewModelScope.launch {
            withContext(Dispatchers.Main) { webViewBridge?.close() }
            var lastResult: PaymentInquireResponseDto? = null
            for ((attempt, delayMs) in INQUIRE_POLL_DELAYS_MS.withIndex()) {
                delay(delayMs)
                val result = fetchInquireStatus() ?: continue
                lastResult = result
                if (result.isTerminal) {
                    FiuuLogger.log("XDK_INQUIRE Poll resolved: status=${result.status} after ${attempt + 1} attempts")
                    break
                }
                FiuuLogger.log("XDK_INQUIRE Poll ${attempt + 1}: status=${result.status}, retrying")
            }
            val finalResult = lastResult?.toPaymentResponseDto()?.let {
                // /inquire has been seen omitting transaction_id — fall back to the ID the lookup
                // was made with, same as inquireStreamResult.
                it.copy(
                    transactionId = it.transactionId ?: transactionId,
                    tranID = it.tranID ?: transactionId
                )
            } ?: userCancelledResult()
            _uiState.update { it.copy(paymentSuccess = finalResult, loading = false, loadingMessage = "Loading...") }
        }
    }

    private fun userCancelledResult() = PaymentResponseDto(
        status = "USER_CANCELLED",
        statusCheck = false,
        message = "Payment was cancelled by user",
        errorCode = "USER_CANCELLED",
        errorDesc = "Payment was cancelled by user"
    )

    fun handleWebViewClosedByUser() {
        if (!claimAttempt("webview closed by user")) return
        com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.recordBreadcrumb(
            area = "WEBVIEW",
            process = "handleWebViewClosedByUser",
            message = "User closed webview, querying /inquire before fallback"
        )
        // System back during the wallet waiting state reaches the (hidden) WebView overlay's own
        // back handler first — it was registered after FiuuPaymentSDK's — and lands here, so the
        // background wallet polling must stop here too or it could report a second result.
        stopAwaitingWalletConfirmation()
        cancelStream("webview closed by user")
        // Set before launching: the WebView is already gone, and until loading is up the screen
        // underneath has an enabled CLOSE that would close the SDK mid-/inquire with no result.
        _uiState.update { it.copy(loading = true, loadingMessage = "Closing...") }
        viewModelScope.launch {
            try {
                // Run the inquire lookup and a minimum display delay concurrently so a fast
                // response doesn't flash the "Closing..." overlay on/off.
                val inquireResult = coroutineScope {
                    val inquireDeferred = async { inquireCurrentStatus() }
                    val minDelayDeferred = async { delay(1500) }
                    minDelayDeferred.await()
                    inquireDeferred.await()
                }
                val finalResult = inquireResult ?: userCancelledResult()
                _uiState.update { it.copy(paymentSuccess = finalResult) }
            } finally {
                _uiState.update { it.copy(loading = false, loadingMessage = "Loading...") }
            }
        } }

    // Standalone /inquire lookup (by xp_tran_id when /process or /express returned one, else by
    // order ID — same inquireChecksumPayload() as inquireStreamResult and
    // domain/ResumePaymentStatus.kt) — used when there's no live stream result to
    // confirm against (the user closed the WebView before the stream delivered anything, or the
    // app resumed from background — see checkPaymentStatusOnResume). Raw DTO is kept nullable-
    // status as-is so callers can tell "backend hasn't reached a terminal state yet" (status ==
    // null) apart from "lookup itself failed" (whole call returns null) — same distinction
    // ResumePaymentStatus.kt relies on to decide whether to keep tracking the session.
    private suspend fun fetchInquireStatus(): PaymentInquireResponseDto? {
        val request = currentRequest ?: return null
        val token = accessToken ?: return null
        return try {
            val timestamp = DateTimeUtils.getCurrentUnixTimestampString()
            val nonce = CryptoUtils.generateNonce()
            val idempotencyKey = CryptoUtils.generateUuid()

            val payload = inquireChecksumPayload(transactionId, request.orderId, request.amount, request.isOtcPayment)

            val checksum = generateChecksum(payload = payload, vkey = request.verificationKey ?: "")
            val signature = CryptoUtils.generateSignature(
                method = "POST",
                path = "/inquire",
                timestamp = timestamp,
                nonce = nonce,
                checksum = checksum
            )

            repo.inquirePayment(
                token = token,
                request = inquireRequest(transactionId, request.orderId, request.amount, checksum, request.isOtcPayment),
                nonce = nonce,
                idempotencyKey = idempotencyKey,
                timestamp = timestamp,
                signature = signature
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FiuuLogger.log("INQUIRE ERROR: $e")
            null
        } }

    private fun PaymentInquireResponseDto.toPaymentResponseDto(): PaymentResponseDto = PaymentResponseDto(
        status = status ?: "",
        statusCheck = status == "00",
        message = message ?: errorDesc ?: "",
        referenceNo = referenceNo,
        transactionId = transactionId,
        amount = amount,
        currency = currency,
        channel = channel,
        skey = skey,
        tranID = transactionId,
        paydate = paydate,
        appcode = appcode,
        errorCode = errorCode,
        errorDesc = errorDesc
    )

    private suspend fun inquireCurrentStatus(): PaymentResponseDto? = fetchInquireStatus()?.toPaymentResponseDto()

    // Fired when the host app returns to the foreground (e.g. LifecycleEventObserver's ON_RESUME
    // in FiuuPaymentSDK) while a payment attempt is still awaiting its result — most notably after
    // handing off to an e-wallet app (TNG, Boost, etc. — see AndroidWebViewBridge.
    // handleNonHttpScheme/handleEwalletUniversalLink) for an out-of-WebView approval step. Runs a
    // silent /inquire lookup so a result that landed while this app was backgrounded resolves
    // immediately instead of sitting until the SSE stream/60s timeout catches up — without
    // depending on the host app registering any custom deep link scheme for the return trip.
    //
    // Guarded to the "still waiting on a result" window — an active stream, or one that dropped
    // without a result (droppedStreamToken) — so it skips the initial composition resume and
    // attempts already resolved via the stream, back-press (handleWebViewClosedByUser), or a load
    // failure. A dropped stream is reconnected first so a later result still arrives; without
    // this, a device that killed the socket while the user was in TNG resumed into an endless
    // loading overlay (confirmed on device, 2026-10-01).
    //
    // Only acts when the backend returned a status at all (status != null in the raw DTO): a
    // terminal status closes the attempt; "22" (pending) switches to the cancellable waiting
    // state (awaitWalletConfirmation). A failed lookup or a status-less response is left
    // untouched.
    //
    // A load failure held while backgrounded (deferredWebViewFailure) means the page behind the
    // wallet app is now an error page: it's closed and the waiting state shown straight away, so
    // the user can still finish in the wallet or Cancel. With nothing left in flight to resolve
    // the attempt, the failure is reported as it would have been in the foreground.
    fun checkPaymentStatusOnResume() {
        appInBackground = false
        val deferredFailure = deferredWebViewFailure
        deferredWebViewFailure = null
        val droppedToken = droppedStreamToken
        FiuuLogger.log("XDK_STREAM App resumed: stream active=${streamJob?.isActive == true}, dropped=${droppedToken != null}, drops=$streamDropCount, held load failure=${deferredFailure != null}")
        if (droppedToken != null) {
            FiuuLogger.log("XDK_STREAM Reconnecting stream dropped while backgrounded")
            startListeningStream(droppedToken)
        }
        if (streamJob?.isActive != true) {
            deferredFailure?.let { handleWebViewFailure(it) }
            return
        }
        if (deferredFailure != null) {
            // onFailure already set webViewResultReported, so this close doesn't also run the
            // user-close path.
            viewModelScope.launch(Dispatchers.Main) { webViewBridge?.close() }
            awaitWalletConfirmation()
        }
        viewModelScope.launch {
            // Silent up to this point — a still-pending lookup or a failed call must not touch
            // the UI at all, otherwise every ordinary app-switch-and-back would flash a loading
            // overlay over the WebView for no reason.
            val inquireResult = fetchInquireStatus() ?: return@launch
            if (inquireResult.status == null) return@launch
            // "22" (pending): the user may still be approving in the wallet app — or may have
            // backed out of it. For wallet hand-offs there's no WebView or close button behind the
            // loading overlay, so silently ignoring "22" left them stuck (confirmed on device,
            // 2026-09-30); ending the attempt on "22" cut off users who were still approving.
            // Instead show a waiting state they can cancel. A resume while that state is already
            // showing is just this one lookup — awaitWalletConfirmation() won't restart polling.
            if (!inquireResult.isTerminal) {
                awaitWalletConfirmation()
                return@launch
            }

            if (!claimAttempt("resume inquire")) return@launch
            stopAwaitingWalletConfirmation()
            cancelStream("resume inquire returned status=${inquireResult.status}")
            withContext(Dispatchers.Main) { webViewBridge?.close() }
            // Only from here on are we committed to actually closing — close() above tears down
            // the WebView/overlay immediately, so without this the SDK would otherwise vanish and
            // hand back to the host in the same frame (observed: returning from TNG's own back
            // button landed straight back on the host app's screen with no transition at all,
            // read as a bug). Mirrors the "Confirming payment..." overlay + minimum display delay
            // handleWebViewClosedByUser/startListeningStream already use for every other closing
            // path, so this one no longer stands out as abrupt.
            _uiState.update { it.copy(loading = true, loadingMessage = "Confirming payment...") }
            delay(1500)
            _uiState.update {
                it.copy(paymentSuccess = inquireResult.toPaymentResponseDto(), loading = false, loadingMessage = "Loading...")
            }
        } }

    // Fired on the host screen's ON_PAUSE (FiuuPaymentSDK) — see appInBackground.
    fun onAppPaused() {
        appInBackground = true
    }

    // Waiting state for a wallet hand-off that came back pending: the stream stays open and
    // /inquire is polled in the background (INQUIRE_POLL_DELAYS_MS, ~30s) as a backup. A terminal
    // status from either closes the attempt. There's deliberately no separate time limit: a 10s
    // one used to end the attempt with "22" while users were still approving in TNG (confirmed on
    // device, 2026-10-01). Once the poll schedule runs out the state simply stays up, resolved by
    // the stream, a resume /inquire, Cancel, or the session timeout (onSessionExpired).
    // See documentations/wallet-pending-session-expiry-design.md.
    private fun awaitWalletConfirmation() {
        if (_uiState.value.awaitingWalletConfirmation) return
        FiuuLogger.log("XDK_STREAM Wallet payment still pending — waiting for confirmation")
        _uiState.update { it.copy(awaitingWalletConfirmation = true) }
        walletConfirmationJob = viewModelScope.launch {
            var resolved: PaymentInquireResponseDto? = null
            // The first lookup already ran in checkPaymentStatusOnResume.
            for (delayMs in INQUIRE_POLL_DELAYS_MS.drop(1)) {
                delay(delayMs)
                val result = fetchInquireStatus() ?: continue
                if (result.isTerminal) {
                    resolved = result
                    break
                }
            }
            if (resolved == null) {
                FiuuLogger.log("XDK_INQUIRE Wallet poll schedule finished, still pending — waiting on stream/resume")
                return@launch
            }
            FiuuLogger.log("XDK_INQUIRE Wallet poll resolved: status=${resolved.status}")
            if (!claimAttempt("wallet poll")) return@launch
            cancelStream("wallet poll returned status=${resolved.status}")
            // Some wallet hand-offs start from inside a WebView page (handleNonHttpScheme).
            withContext(Dispatchers.Main) { webViewBridge?.close() }
            _uiState.update {
                it.copy(awaitingWalletConfirmation = false, paymentSuccess = resolved.toPaymentResponseDto())
            }
        }
    }

    private fun stopAwaitingWalletConfirmation() {
        walletConfirmationJob?.cancel()
        _uiState.update { it.copy(awaitingWalletConfirmation = false) }
    }

    // Cancel on the waiting state: same as closing the payment page — one /inquire for the real
    // status (which may still be "22"), else USER_CANCELLED.
    fun cancelWalletConfirmation() {
        stopAwaitingWalletConfirmation()
        closeWalletAttempt()
    }

    // Wallet hand-offs still add the WebView overlay (close bar + back handler) behind the external
    // app — AndroidWebViewBridge.openGetUrl calls addOverlayWebView even when it hands the URL off.
    // handleWebViewClosedByUser() assumes the WebView is already gone (it normally runs from the
    // WebView's own close), so tear it down first or the close bar and its back handler outlive
    // the SDK (confirmed on device, 2026-09-30).
    private fun closeWalletAttempt() {
        viewModelScope.launch(Dispatchers.Main) { webViewBridge?.close() }
        handleWebViewClosedByUser()
    }

    // FiuuPaymentSDK's session countdown (SESSION_TIMEOUT_SECONDS) reached 0. With an attempt in flight (stream open,
    // stream dropped while backgrounded, or the wallet waiting state) the gateway may well have
    // finished — e.g. approved in TNG just before the limit — so ask /inquire once and return the
    // real status, falling back to the 408 only if that fails. Nothing in flight → 408 right away,
    // as before. Skipped entirely when another path is already ending the attempt
    // (resolvingAttempt), so the host still gets exactly one result.
    fun onSessionExpired() {
        if (!claimAttempt("session expired")) return
        // Voucher screen still open: same /inquire as CLOSE — the customer may have paid meanwhile.
        pendingInstructionResult?.let { issued ->
            FiuuLogger.log("XDK_INQUIRE Session expired on the payment instruction screen")
            pendingInstructionResult = null
            resolveOfflineVoucher(issued)
            return
        }
        val inFlight = streamJob?.isActive == true ||
            droppedStreamToken != null ||
            deferredWebViewFailure != null ||
            _uiState.value.awaitingWalletConfirmation
        FiuuLogger.log("XDK_INQUIRE Session expired: attempt in flight=$inFlight")
        if (!inFlight) {
            _uiState.update { it.copy(paymentSuccess = sessionTimeoutResult()) }
            return
        }
        stopAwaitingWalletConfirmation()
        cancelStream("session expired")
        viewModelScope.launch {
            withContext(Dispatchers.Main) { webViewBridge?.close() }
            _uiState.update { it.copy(loading = true, loadingMessage = "Confirming payment...") }
            val inquireResult = fetchInquireStatus()?.takeIf { it.status != null }
            FiuuLogger.log("XDK_INQUIRE Session-expiry inquire: status=${inquireResult?.status}")
            val finalResult = inquireResult?.toPaymentResponseDto()?.let {
                it.copy(
                    transactionId = it.transactionId ?: transactionId,
                    tranID = it.tranID ?: transactionId
                )
            } ?: sessionTimeoutResult()
            _uiState.update { it.copy(paymentSuccess = finalResult, loading = false, loadingMessage = "Loading...") }
        }
    }

    fun triggerClickToPay() {
        // Guard failures here are timing/transient (e.g. tapped before /initiate finished), not a
        // checkout outcome — must not set paymentError, since FiuuPaymentSDK's LaunchedEffect treats
        // any paymentError as fatal and closes the whole SDK back to the host app.
        val token = accessToken ?: run {
            FiuuLogger.log("triggerClickToPay: accessToken is null")
            return
        }
        // Click to Pay is rendered as a Composable (ClickToPayHandler) inside the same Compose
        // tree as everything else — it fetches this URL itself (with the Bearer header), so
        // relative resource paths and cross-domain cookies behave like a normal browser
        // navigation, and the surrounding paymentError/paymentSuccess machinery stays alive.
        _uiState.update {
            it.copy(
                clickToPayRequest = ClickToPayRequest(
                    url = "${ApiConfig.baseUrl(currentRequest?.buildMode ?: 4)}mastercardc2p",
                    headers = mapOf("Authorization" to "Bearer $token"),
                    amount = it.amount,
                    currency = it.currency,
                    country = currentRequest?.country
                )
            )
        } }

    fun onClickToPaySuccess(paymentData: JsonObject) {
        FiuuLogger.log("ClickToPay: finished")
        _uiState.update { it.copy(clickToPayRequest = null) }
        val c2pData = buildJsonObject {
            put("cardBrand", "Mastercard")
            paymentData.forEach { (key, value) -> put(key, value) }
        }
        currentRequest = currentRequest?.copy(c2pData = c2pData)
        startPayment() }

    // Used only by ClickToPayRegistrationRequiredView's own cancel/back — that screen's copy
    // explicitly invites the user to "cancel to choose another payment method", so its cancel
    // must behave like the rest of the SDK's "Change Payment Method" screens: drop back to the
    // channel list rather than end the session. The main widget screen's own Close/back uses
    // onClickToPayFullCancel instead (see below) — closing out of the widget itself, before any
    // card was even found, is the user abandoning the attempt, not switching methods.
    fun onClickToPayCancel(message: String?) {
        FiuuLogger.log("ClickToPay: cancelled - $message")
        _uiState.update { it.copy(clickToPayRequest = null) }
    }

    // Fired by the main Click to Pay widget screen's own Close button/back press (not the
    // registration-required fallback — see onClickToPayCancel above). Unlike that screen, this one
    // IS the payment page as far as the user is concerned — same as the redirect WebView other
    // channels open — so closing it must end the whole attempt, not just back out to try a
    // different channel. Mirrors handleWebViewClosedByUser() exactly: cancel any stream (there
    // usually isn't one yet here, since C2P only calls /process — and starts the SSE stream —
    // after the widget itself succeeds, but this stays safe if that ever changes), confirm with
    // one /inquire lookup rather than trusting a raw cancellation blindly (the gateway may already
    // have reached a terminal state), and fall back to a defined USER_CANCELLED result. Setting
    // paymentSuccess (not clickToPayRequest/paymentError) routes through FiuuPaymentSDK's normal
    // onPaymentResult + onRequestClose path, same as every other "user closed the payment page"
    // exit in the SDK.
    fun onClickToPayFullCancel(message: String?) {
        FiuuLogger.log("ClickToPay: full cancel - $message")
        if (!claimAttempt("click to pay full cancel")) return
        // One update, before launching: clearing clickToPayRequest reveals the channel grid, and
        // its CLOSE must already be disabled or it would close the SDK mid-/inquire.
        _uiState.update { it.copy(clickToPayRequest = null, loading = true, loadingMessage = "Closing...") }
        cancelStream("click to pay full cancel")
        viewModelScope.launch {
            try {
                val inquireResult = coroutineScope {
                    val inquireDeferred = async { inquireCurrentStatus() }
                    val minDelayDeferred = async { delay(1500) }
                    minDelayDeferred.await()
                    inquireDeferred.await()
                }
                val finalResult = inquireResult ?: PaymentResponseDto(
                    status = "USER_CANCELLED",
                    statusCheck = false,
                    message = message ?: "Payment was cancelled by user",
                    errorCode = "USER_CANCELLED",
                    errorDesc = message ?: "Payment was cancelled by user"
                )
                _uiState.update { it.copy(paymentSuccess = finalResult) }
            } finally {
                _uiState.update { it.copy(loading = false, loadingMessage = "Loading...") }
            }
        }
    }

    // Widget asked to route the user to the SDK's own manual card entry screen (postMessage
    // type "01"/"05"/"06") instead of cancelling outright — clear the request without surfacing a
    // paymentError, since this isn't a checkout failure. This is where card-tokens is fetched for
    // the saved-card list CardManualEntryScreen reads (state.cardTokens) — see fetchCardTokens.
    fun onClickToPayNavigateToManualCard() {
        FiuuLogger.log("ClickToPay: routing to manual card entry")
        _uiState.update { it.copy(clickToPayRequest = null) }
        val token = accessToken
        val request = currentRequest
        if (token != null && request != null) {
            fetchCardTokens(token, request)
        }
    }

    fun clearPaymentError() {
        _uiState.update {
            it.copy(paymentError = null)
        } }

    fun isApplePayAvailable(): Boolean = applePayHandler.isAvailable()

    // Mirrors triggerSamsungPay()/handleSamsungPayResult(): no channel object needed and no
    // onChannelSelected() call — toProcessApiPayload() resolves the ApplePay branch purely from
    // applePayToken, exactly like it resolves SamsungPay from samsungPayData, so
    // selectedChannel/currentRequest.channel never need to be touched here.
    fun startApplePay() {
        val request = currentRequest ?: run {
            _uiState.update { it.copy(paymentError = PaymentError.GenericError()) }
            return
        }
        val apMerchantId = request.apMerchantId ?: run {
            FiuuLogger.log("startApplePay: mp_ap_merchant_ID is missing from params")
            _uiState.update { it.copy(paymentError = PaymentError.GenericError()) }
            return
        }

        applePayHandler.requestPayment(
            merchantId = apMerchantId,
            amount = request.amount ?: "0",
            currency = request.currency ?: "MYR",
            countryCode = request.country ?: "MY",
            onResult = { result ->
                when (result) {
                    is ApplePayResult.Success -> {
                        viewModelScope.launch {
                            processApplePayStart(result.token)
                        }
                    }
                    is ApplePayResult.Error -> {
                        FiuuLogger.log("Apple Pay error: ${result.message}")
                        _uiState.update { it.copy(paymentError = PaymentError.ApplePayError(result.message)) }
                    }
                    ApplePayResult.Cancelled -> {
                        _uiState.update { it.copy(paymentError = PaymentError.ApplePayError("Payment was not completed.", cancelled = true)) }
                    }
                }
            }
        )
    }

    // Same shape as processSamsungPayStart/processGooglePayStart: attach the token to
    // currentRequest and call startPayment(), which owns `loading` from there. startPayment()
    // returns as soon as it has launched /process, so these must not clear `loading` on the way
    // out — a `finally` doing that used to race startPayment()'s own `loading = true` and leave
    // CLOSE and Pay enabled while /process was in flight. Only a throw here clears it.
    private suspend fun processApplePayStart(token: JsonObject) {
        try {
            // apMerchantId was only needed to open the PKPaymentRequest sheet — drop it now that
            // we have a token so it never reaches toProcessApiPayload/the wire.
            currentRequest = currentRequest?.copy(applePayToken = token, apMerchantId = null)
            startPayment()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FiuuLogger.log("APPLE PAY DIRECT ERROR: $e")
            _uiState.update { it.copy(loading = false, paymentError = PaymentError.GenericError()) }
        }
    }

    fun triggerGooglePay() {
        _googlePayTrigger.value = true }

    fun consumeGooglePayTrigger() {
        _googlePayTrigger.value = false }

    fun handleGooglePayResult(result: GooglePayPaymentResult) {
        when (result) {
            is GooglePayPaymentResult.Success -> {
                viewModelScope.launch {
                    processGooglePayStart(result.paymentDataJson)
                }
            }
            is GooglePayPaymentResult.Cancelled -> {
                if (currentRequest?.expressMode == true) {
                    _uiState.update {
                        it.copy(
                            paymentError = PaymentError.CustomError(
                                "Payment cancelled by user",
                                errorCode = "USER_CANCELLED"
                            )
                        )
                    }
                }
            }
            is GooglePayPaymentResult.Error -> {
                FiuuLogger.log("Google Pay error: ${result.message}")
                _uiState.update {
                    it.copy(
                        paymentError = PaymentError.CustomError(
                            result.message,
                            errorCode = "GOOGLE_PAY_ERROR"
                        )
                    )
                }
            }
        } }

    fun expressPayment(params:  PaymentRequest) {
        if (!hasInternetConnection()) {
            FiuuLogger.log("EXPRESS PAYMENT FAILED: no internet connection")
            _uiState.update { it.copy(paymentError = PaymentError.NoInternetConnection()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val timestamp = DateTimeUtils.getCurrentUnixTimestampString()
            val nonce = CryptoUtils.generateNonce()
            val idempotencyKey = CryptoUtils.generateUuid()
            val payload = params.toExpressApiPayload(includeChecksum = false)

            FiuuLogger.log("checksum payload: ${redactSensitiveMap(payload)}")
            FiuuLogger.log("timestamp: $timestamp")
            FiuuLogger.log("verificationkey present: ${params.verificationKey != null}")

            val checksum = generateChecksum(
                payload = payload,
                vkey = params.verificationKey ?: "" )
            val signature = CryptoUtils.generateSignature(
                method = "POST",
                path = "/express",
                timestamp = timestamp,
                nonce = nonce,
                checksum = checksum )
            val requestWithChecksum = params.copy(
                checksum = checksum  )
            this@PaymentViewModel.currentRequest = requestWithChecksum
            try {   
                    val result = repo.expressPayment(
                    request = requestWithChecksum,
                    nonce = nonce,
                    idempotencyKey = idempotencyKey,
                    timestamp = timestamp,
                    signature = signature )

                    if (result == null) {
                        FiuuLogger.log("INIT FAILED: repository returned null (network/server error)")
                        _uiState.update { it.copy(paymentError = PaymentError.NetworkError()) }
                        return@launch }

                    val token = result.accessToken
                    if (token == null) {
                        FiuuLogger.log("INIT FAILED: ${result.errorDesc}")
                        _uiState.update { it.copy(paymentError = PaymentError.CustomError(result.errorDesc)) }
                        return@launch }

                    FiuuLogger.log("INIT SUCCESS: status=${result.status}")
                    this@PaymentViewModel.accessToken = token
                    this@PaymentViewModel.transactionId = result.transactionId?.takeIf { it.isNotBlank() }
                    streamDropCount = 0
                    resolvingAttempt.value = false
                    persistSession(token)
                    val requestType = result.requestType
                    if(requestType == "REDIRECT"){
                        val url = result.requestURL
                        if (url == null) {
                            FiuuLogger.log("CREATE PAYMENT ERROR: requestURL is null — check @SerialName mapping vs actual API response field name")
                            _uiState.update { it.copy(paymentError = PaymentError.GenericError()) }
                            return@launch
                        }
                        paymentUrl = url
                        paymentMethod = result.requestMethod
                        paymentRequestData = result.requestData.toRequestDataMap()
                        val bridge = webViewBridge ?: run {
                            FiuuLogger.log("CREATE PAYMENT ERROR: webViewBridge is null")
                            _uiState.update { it.copy(paymentError = noWebViewBridgeError()) }
                            return@launch
                        }
                        openPayment(bridge, result.urlType)
                        startListeningStream(token) }
                    else if (requestType == "RESPONSE") {
                        val response = PaymentResponseDto(
                            status = result.status ?: "",
                            statusCheck = result.status == "00",
                            message = result.errorDesc ?: "",
                            requestURL = result.requestURL,
                            requestMethod = result.requestMethod,
                            requestType = result.requestType,
                            referenceNo = result.referenceNo,
                            transactionId = result.transactionId,
                            amount = result.amount,
                            currency = result.currency,
                            channel = result.channel,
                            skey = result.skey,
                            tranID = result.tranID,
                            paydate = result.paydate,
                            appcode = result.appcode,
                            errorCode = result.errorCode,
                            errorDesc = result.errorDesc
                        )
                        _uiState.update { it.copy(paymentSuccess = response) } }
                    else if (requestType == "data") {
                        // Gateway returned inline base64-encoded HTML to render directly, rather
                        // than a redirect URL — same WebView flow, just urlType = "data".
                        val html = result.requestURL
                        if (html == null) {
                            FiuuLogger.log("CREATE PAYMENT ERROR: requestURL is null for requestType=data")
                            _uiState.update { it.copy(paymentError = PaymentError.GenericError()) }
                            return@launch
                        }
                        paymentUrl = html
                        val bridge = webViewBridge ?: run {
                            FiuuLogger.log("CREATE PAYMENT ERROR: webViewBridge is null")
                            _uiState.update { it.copy(paymentError = noWebViewBridgeError()) }
                            return@launch
                        }
                        openPayment(bridge, urlType = "data")
                        startListeningStream(token) }
                    else {
                        _uiState.update { it.copy(paymentError = PaymentError.GenericError()) }
                        return@launch } } 
            catch (e: ApiException) {
                FiuuLogger.log("INIT API ERROR: ${e.message} (${e.statusCode}, ${e.errorCode})")
                _uiState.update { it.copy(paymentError = PaymentError.CustomError(msg = e.message, statusCode = e.statusCode, errorCode = e.errorCode)) } }
            catch (e: CancellationException) {
                throw e
            }
            catch (e: Exception) {
                FiuuLogger.log("INIT ERROR: $e")
                _uiState.update { it.copy(paymentError = PaymentError.CustomError(msg = e.message)) } }
            finally {
                    _uiState.update { it.copy(loading = false) } }
            }
        }

    // Express-mode Apple Pay: unlike GooglePay/SamsungPay's QuickPay tile (which only
    // fires after /initiate has already run), express mode skips /initiate entirely — so
    // this must get the token FIRST (no server call needed for the sheet itself, same as
    // expressGooglePay's loadPaymentData-before-network-call ordering) and only then make
    // the single /express call carrying xp_applepay.
    private fun startExpressApplePay(request: PaymentRequest) {
        val apMerchantId = request.apMerchantId
        if (apMerchantId.isNullOrBlank()) {
            FiuuLogger.log("startExpressApplePay: mp_ap_merchant_ID is missing from params")
            _uiState.update { it.copy(paymentError = PaymentError.GenericError()) }
            return
        }

        _uiState.update { it.copy(loading = true) }

        applePayHandler.requestPayment(
            merchantId = apMerchantId,
            amount = request.amount ?: "0",
            currency = request.currency ?: "MYR",
            countryCode = request.country ?: "MY",
            onResult = { result ->
                when (result) {
                    is ApplePayResult.Success -> {
                        // apMerchantId was only needed to open the PKPaymentRequest sheet — drop
                        // it now that we have a token so it never reaches toExpressApiPayload/the
                        // wire.
                        val requestWithToken = request.copy(applePayToken = result.token, apMerchantId = null)
                        currentRequest = requestWithToken
                        expressPayment(requestWithToken)
                    }
                    is ApplePayResult.Error -> {
                        FiuuLogger.log("Apple Pay error: ${result.message}")
                        _uiState.update { it.copy(loading = false, paymentError = PaymentError.ApplePayError(result.message)) }
                    }
                    ApplePayResult.Cancelled -> {
                        _uiState.update { it.copy(loading = false, paymentError = PaymentError.ApplePayError("Payment was not completed.", cancelled = true)) }
                    }
                }
            }
        )
    }

    fun setSamsungPayReady(ready: Boolean) {
        _uiState.update { it.copy(isSamsungPayReady = ready) } }

    fun triggerSamsungPay() {
        _samsungPayTrigger.value = true }

    fun consumeSamsungPayTrigger() {
        _samsungPayTrigger.value = false }

    fun handleSamsungPayResult(result: SamsungPayPaymentResult) {
        when (result) {
            is SamsungPayPaymentResult.Success -> {
                viewModelScope.launch {
                    processSamsungPayStart(result.paymentCredentialJson)
                }
            }
            is SamsungPayPaymentResult.Cancelled -> {
                if (currentRequest?.expressMode == true) {
                    _uiState.update {
                        it.copy(
                            paymentError = PaymentError.CustomError(
                                "Payment cancelled by user",
                                errorCode = "USER_CANCELLED"
                            )
                        )
                    }
                }
            }
            is SamsungPayPaymentResult.Error -> {
                FiuuLogger.log("Samsung Pay error: ${result.message}")
                _uiState.update {
                    it.copy(paymentError = PaymentError.CustomError(msg = result.message, errorCode = "SAMSUNG_PAY_ERROR"))
                }
            }
        } }

    private suspend fun processSamsungPayStart(paymentCredentialJson: String) {
        try {
            val samsungPayData = Json.parseToJsonElement(paymentCredentialJson).jsonObject
            val updatedRequest = currentRequest?.copy(samsungPayData = samsungPayData)
            currentRequest = updatedRequest
            if (updatedRequest?.expressMode == true) {
                expressPayment(updatedRequest)
            } else {
                startPayment()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FiuuLogger.log("SAMSUNG PAY DIRECT ERROR: $e")
            _uiState.update { it.copy(loading = false, paymentError = PaymentError.GenericError()) }
        } }

    private suspend fun processGooglePayStart(paymentDataJson: String) {
        try {
            val googlePayData = Json.parseToJsonElement(paymentDataJson).jsonObject
            val updatedRequest = currentRequest?.copy(
                googlePayData = googlePayData,
                googlePayTokenAsBase64 = GOOGLE_PAY_TOKEN_AS_BASE64
            )
            currentRequest = updatedRequest
            if (updatedRequest?.expressMode == true) {
                expressPayment(updatedRequest)
            } else {
                startPayment()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FiuuLogger.log("GOOGLE PAY DIRECT ERROR: $e")
            _uiState.update { it.copy(loading = false, paymentError = PaymentError.GenericError()) }
        } }

    // Only one path may end an attempt (COR-01). Must run before the caller cancels the stream,
    // stops the wallet poll or closes the WebView — a losing path doing any of that could cancel
    // the winner before it writes its result.
    private fun claimAttempt(path: String): Boolean {
        if (resolvingAttempt.compareAndSet(expect = false, update = true)) return true
        FiuuLogger.log("XDK_RESULT $path skipped — attempt already resolving")
        return false
    }

    // Logs the reason so an "XDK_STREAM Cancelled" line can be traced back to its trigger.
    private fun cancelStream(reason: String) {
        if (streamJob?.isActive == true) FiuuLogger.log("XDK_STREAM Cancelling stream: $reason")
        streamJob?.cancel()
        droppedStreamToken = null
    }

    private fun startListeningStream(token: String) {
        cancelStream("new stream starting")
        val outcome = CompletableDeferred<Boolean>()
        streamOutcome = outcome
        streamJob = viewModelScope.launch {
            val streamResult = repo.listenToStream(token)
            outcome.complete(streamResult != null)
            if (streamResult == null) {
                streamDropCount++
                FiuuLogger.log("XDK_STREAM Ended without a result (drop #$streamDropCount)")
                droppedStreamToken = token
            }
            if (streamResult != null) {
                if (!claimAttempt("stream result")) return@launch
                // close() touches the Activity/WebView (Android) or UIKit (iOS) directly —
                // both platforms require the main thread/queue, but this coroutine runs on
                // Dispatchers.Default.
                withContext(Dispatchers.Main) { webViewBridge?.close() }
                stopAwaitingWalletConfirmation()
                // close() reveals whatever screen was underneath the WebView — without a loading
                // overlay here, the /inquire confirmation call below would run silently against
                // that bare screen for however long it takes.
                _uiState.update { it.copy(loading = true, loadingMessage = "Confirming payment...") }
                // The stream push is a notification that the gateway reached a terminal state,
                // not the authoritative result — confirm it with /inquire. Fall back to the raw
                // stream result if the lookup fails so a flaky /inquire call can't hang checkout.
                // Run the inquire lookup and a minimum display delay concurrently (same pattern as
                // handleWebViewClosedByUser/inquireCurrentStatus) so a fast response doesn't flash
                // the overlay on/off.
                val finalResult = coroutineScope {
                    val inquireDeferred = async { inquireStreamResult(streamResult) }
                    val minDelayDeferred = async { delay(1500) }
                    minDelayDeferred.await()
                    inquireDeferred.await()
                } ?: streamResult
                _uiState.update { it.copy(paymentSuccess = finalResult, loading = false, loadingMessage = "Loading...") }
            }
        } }

    private suspend fun inquireStreamResult(streamResult: PaymentResponseDto): PaymentResponseDto? {
        val request = currentRequest ?: return null
        // /inquire authenticates with the accessToken issued by /initiate — read the field
        // directly rather than the token passed into startListeningStream, since that one may
        // have come from /express instead.
        val token = accessToken ?: return null
        return try {
            val timestamp = DateTimeUtils.getCurrentUnixTimestampString()
            val nonce = CryptoUtils.generateNonce()
            val idempotencyKey = CryptoUtils.generateUuid()
            // Prefer the stream's ID, falling back to the one /process or /express returned.
            val transactionId = streamResult.transactionId?.takeIf { it.isNotBlank() }
                ?: this@PaymentViewModel.transactionId
            val payload = inquireChecksumPayload(transactionId, request.orderId, request.amount, request.isOtcPayment)

            val checksum = generateChecksum(payload = payload, vkey = request.verificationKey ?: "")
            val signature = CryptoUtils.generateSignature(
                method = "POST",
                path = "/inquire",
                timestamp = timestamp,
                nonce = nonce,
                checksum = checksum
            )

            val inquireResult = repo.inquirePayment(
                token = token,
                request = inquireRequest(transactionId, request.orderId, request.amount, checksum, request.isOtcPayment),
                nonce = nonce,
                idempotencyKey = idempotencyKey,
                timestamp = timestamp,
                signature = signature
            ) ?: return null

            streamResult.copy(
                status = inquireResult.status ?: streamResult.status,
                statusCheck = inquireResult.status == "00" || streamResult.statusCheck,
                message = inquireResult.message ?: streamResult.message,
                referenceNo = inquireResult.referenceNo ?: streamResult.referenceNo,
                // After /pay/callback, /inquire has been seen returning only status/skey/deeplink
                // — fall back to the ID this lookup was made with so the host still gets one.
                transactionId = inquireResult.transactionId ?: streamResult.transactionId ?: transactionId,
                amount = inquireResult.amount ?: streamResult.amount,
                currency = inquireResult.currency ?: streamResult.currency,
                channel = inquireResult.channel ?: streamResult.channel,
                skey = inquireResult.skey ?: streamResult.skey,
                tranID = inquireResult.transactionId ?: streamResult.tranID ?: transactionId,
                paydate = inquireResult.paydate ?: streamResult.paydate,
                appcode = inquireResult.appcode ?: streamResult.appcode,
                errorCode = inquireResult.errorCode ?: streamResult.errorCode,
                errorDesc = inquireResult.errorDesc ?: streamResult.errorDesc
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FiuuLogger.log("INQUIRE ERROR: $e")
            null
        } }

    fun dispose() {
        cancelStream("view model disposed")
        viewModelScope.cancel()
        repo.close() }
        
}
