package com.fiuu.xdkandroid.viewmodel

import com.fiuu.xdkandroid.data.remote.dto.CardTokenContainerDto
import com.fiuu.xdkandroid.data.remote.dto.CardTokensRequestDto
import com.fiuu.xdkandroid.data.remote.dto.GetAvailableChannelResponseDto
import com.fiuu.xdkandroid.data.remote.dto.InstalmentTenureRequestDto
import com.fiuu.xdkandroid.data.remote.dto.InstalmentTenureResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentCreateResponse
import com.fiuu.xdkandroid.data.remote.dto.PaymentExpressResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentInquireRequestDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentInquireResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentProcessRequestDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentResponseDto
import com.fiuu.xdkandroid.domain.PaymentRepository
import com.fiuu.xdkandroid.domain.entity.PaymentChannelInit
import com.fiuu.xdkandroid.domain.entity.PaymentInitResponse
import com.fiuu.xdkandroid.domain.entity.PaymentRequest
import com.fiuu.xdkandroid.platform.WebViewBridge
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// COR-01: every path that ends an attempt claims it first (claimAttempt), so when two race only
// the first writes a result. Each test lets one path claim the attempt while its /inquire is still
// in flight, then fires a second path and checks that the second one does nothing: no second
// /inquire, no overlay change, and exactly one result.
@OptIn(ExperimentalCoroutinesApi::class)
class ClaimAttemptTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeRepository()
    private val bridge = FakeWebViewBridge()
    private lateinit var vm: PaymentViewModel

    @BeforeTest
    fun setUp() {
        // The ViewModel hops to Dispatchers.Main to touch the WebView.
        Dispatchers.setMain(dispatcher)
        vm = PaymentViewModel(repo, dispatcher)
        vm.attachWebViewBridge(bridge)
    }

    @AfterTest
    fun tearDown() {
        vm.dispose()
        Dispatchers.resetMain()
    }

    @Test
    fun userCloseAfterStreamClaimedIsIgnored() = runTest(dispatcher) {
        val results = recordResults()
        startAttempt()

        repo.stream.complete(PaymentResponseDto(status = "00", statusCheck = true))
        runCurrent()
        assertEquals(1, repo.inquireCalls, "stream confirms via /inquire")

        bridge.userCloses()
        runCurrent()
        assertEquals("Confirming payment...", vm.uiState.value.loadingMessage)

        repo.inquire.complete(PaymentInquireResponseDto(status = "00"))
        advanceUntilIdle()

        assertEquals(1, repo.inquireCalls)
        assertEquals(listOf("00"), results.map { it.status })
    }

    @Test
    fun sessionExpiryAfterStreamClaimedIsIgnored() = runTest(dispatcher) {
        val results = recordResults()
        startAttempt()

        repo.stream.complete(PaymentResponseDto(status = "00", statusCheck = true))
        runCurrent()

        vm.onSessionExpired()
        runCurrent()

        repo.inquire.complete(PaymentInquireResponseDto(status = "00"))
        advanceUntilIdle()

        assertEquals(1, repo.inquireCalls)
        assertEquals(listOf("00"), results.map { it.status })
    }

    @Test
    fun sessionExpiryAfterUserCloseClaimedIsIgnored() = runTest(dispatcher) {
        val results = recordResults()
        startAttempt()

        bridge.userCloses()
        runCurrent()
        assertEquals("Closing...", vm.uiState.value.loadingMessage)

        vm.onSessionExpired()
        runCurrent()
        assertEquals("Closing...", vm.uiState.value.loadingMessage)

        repo.inquire.complete(PaymentInquireResponseDto(status = "11"))
        advanceUntilIdle()

        assertEquals(1, repo.inquireCalls)
        assertEquals(listOf("11"), results.map { it.status })
    }

    @Test
    fun userCloseAfterResumeInquireClaimedIsIgnored() = runTest(dispatcher) {
        val results = recordResults()
        startAttempt()

        // Returning from a wallet app: the resume /inquire already sees a terminal status.
        repo.inquire.complete(PaymentInquireResponseDto(status = "00"))
        vm.checkPaymentStatusOnResume()
        runCurrent()
        assertEquals(1, repo.inquireCalls)

        bridge.userCloses()
        advanceUntilIdle()

        assertEquals(1, repo.inquireCalls)
        assertEquals(listOf("00"), results.map { it.status })
        assertNull(vm.uiState.value.paymentError)
    }

    @Test
    fun unclaimedAttemptStillResolves() = runTest(dispatcher) {
        // Guards against the claim being left set: with nothing racing, the user close resolves.
        val results = recordResults()
        startAttempt()

        repo.inquire.complete(null)
        bridge.userCloses()
        advanceUntilIdle()

        assertEquals(listOf("USER_CANCELLED"), results.map { it.status })
    }

    // Bug 22: after a wallet hand-off the page left behind fails with the app's network cut off.
    // That failure must not end the attempt while the user may still be paying in the wallet.
    @Test
    fun loadFailureWhileBackgroundedIsHeldUntilResume() = runTest(dispatcher) {
        val results = recordResults()
        startAttempt()

        vm.onAppPaused()
        bridge.pageFails("net::ERR_NAME_NOT_RESOLVED")
        advanceUntilIdle()
        assertNull(vm.uiState.value.paymentError)
        assertEquals(0, bridge.closeCalls)
        assertEquals(0, repo.inquireCalls)

        vm.checkPaymentStatusOnResume()
        runCurrent()
        assertTrue(vm.uiState.value.awaitingWalletConfirmation, "waiting state replaces the dead page")
        assertEquals(1, bridge.closeCalls)
        assertEquals(1, repo.inquireCalls)

        repo.inquire.complete(PaymentInquireResponseDto(status = "00"))
        advanceUntilIdle()

        assertEquals(listOf("00"), results.map { it.status })
        assertNull(vm.uiState.value.paymentError)
        assertFalse(vm.uiState.value.awaitingWalletConfirmation)
    }

    @Test
    fun heldLoadFailureAfterStreamResultIsDropped() = runTest(dispatcher) {
        val results = recordResults()
        startAttempt()

        vm.onAppPaused()
        bridge.pageFails("net::ERR_NAME_NOT_RESOLVED")
        repo.stream.complete(PaymentResponseDto(status = "00", statusCheck = true))
        repo.inquire.complete(PaymentInquireResponseDto(status = "00"))
        advanceUntilIdle()

        // The stream already ended the attempt, so the held failure has nothing left to report.
        vm.checkPaymentStatusOnResume()
        advanceUntilIdle()

        assertEquals(listOf("00"), results.map { it.status })
        assertNull(vm.uiState.value.paymentError)
    }

    @Test
    fun loadFailureInForegroundStillEndsAttempt() = runTest(dispatcher) {
        startAttempt()

        bridge.pageFails("net::ERR_CONNECTION_REFUSED")
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.paymentError)
        assertEquals(1, bridge.closeCalls)
    }

    // initialize → startPayment → WebView open, stream listening. Leaves the attempt in flight.
    private fun TestScope.startAttempt() {
        vm.initialize(REQUEST)
        advanceUntilIdle()
        vm.startPayment()
        advanceUntilIdle()
        assertNotNull(bridge.onClose, "payment WebView should be open")
        assertEquals(1, repo.streamCalls, "stream should be listening")
    }

    // Every distinct paymentSuccess the ViewModel writes, in order.
    private fun TestScope.recordResults(): List<PaymentResponseDto> {
        val results = mutableListOf<PaymentResponseDto>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.map { it.paymentSuccess }.distinctUntilChanged().filterNotNull()
                .collect { results += it }
        }
        return results
    }

    private companion object {
        val REQUEST = PaymentRequest(
            amount = "1.10",
            username = "user",
            password = "pass",
            merchantId = "merchant",
            appName = "app",
            orderId = "order-1",
            currency = "MYR",
            country = "MY",
            verificationKey = "vkey",
            channel = "fpx",
            billName = "Name",
            billEmail = "a@b.c",
            billMobile = "0123456789",
            billDescription = "desc"
        )
    }
}

// /initiate and /process succeed immediately. The stream and /inquire are held open until the test
// completes them, so a test can fire a second path while the first is still mid-call.
private class FakeRepository : PaymentRepository {
    val stream = CompletableDeferred<PaymentResponseDto?>()
    val inquire = CompletableDeferred<PaymentInquireResponseDto?>()
    var streamCalls = 0
    var inquireCalls = 0

    override suspend fun initPayment(
        request: PaymentRequest,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ) = PaymentInitResponse(
        accessToken = "token",
        channels = listOf(PaymentChannelInit(name = "fpx", title = "FPX", channelType = "IB"))
    )

    override suspend fun createPayment(
        token: String,
        request: PaymentProcessRequestDto,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ) = PaymentCreateResponse(
        requestType = "REDIRECT",
        requestMethod = "GET",
        requestURL = "https://bank.example/pay",
        transactionId = "tx-1"
    )

    override suspend fun listenToStream(token: String): PaymentResponseDto? {
        streamCalls++
        return stream.await()
    }

    override suspend fun inquirePayment(
        token: String,
        request: PaymentInquireRequestDto,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): PaymentInquireResponseDto? {
        inquireCalls++
        return inquire.await()
    }

    override suspend fun removeCardToken(accessToken: String, cardToken: String) = false

    override suspend fun fetchCardTokens(
        accessToken: String,
        request: CardTokensRequestDto,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): CardTokenContainerDto? = null

    override suspend fun expressPayment(
        request: PaymentRequest,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): PaymentExpressResponseDto? = null

    override suspend fun getAvailableChannelById(
        merchantId: String,
        currency: String,
        checksum: String,
        username: String,
        password: String
    ): GetAvailableChannelResponseDto? = null

    override suspend fun fetchInstalmentTenure(
        token: String,
        request: InstalmentTenureRequestDto,
        nonce: String,
        timestamp: String,
        signature: String
    ): InstalmentTenureResponseDto? = null

    override fun close() {}
}

// Keeps the callbacks of the last opened page so a test can play the user closing it or the page
// failing to load, and counts close() calls.
private class FakeWebViewBridge : WebViewBridge {
    var onClose: (() -> Unit)? = null
    var onFailure: ((String?) -> Unit)? = null
    var closeCalls = 0

    fun userCloses() = onClose!!.invoke()
    fun pageFails(message: String) = onFailure!!.invoke(message)

    override fun initialize(merchantId: String, sessionId: String) {}
    override fun startCheckOut(onComplete: (String) -> Unit) {}

    override fun openGetUrl(
        url: String,
        onSuccess: () -> Unit,
        onFailure: (String?) -> Unit,
        onClose: () -> Unit,
        urlType: String?,
        headers: Map<String, String>?,
        closeButtonDisplay: Boolean,
        onResultPage: ((url: String, loaded: Boolean) -> Unit)?,
        onAppPaused: (() -> Unit)?,
        onAppResume: (() -> Unit)?
    ) {
        this.onClose = onClose
        this.onFailure = onFailure
    }

    override fun openPostUrl(
        url: String,
        postData: Map<String, String>,
        onSuccess: () -> Unit,
        onFailure: (String?) -> Unit,
        onClose: () -> Unit,
        urlType: String?,
        closeButtonDisplay: Boolean,
        onResultPage: ((url: String, loaded: Boolean) -> Unit)?,
        onAppPaused: (() -> Unit)?,
        onAppResume: (() -> Unit)?
    ) {
        this.onClose = onClose
        this.onFailure = onFailure
    }

    override fun close() { closeCalls++ }
}
