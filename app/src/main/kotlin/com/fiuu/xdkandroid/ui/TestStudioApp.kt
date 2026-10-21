package com.fiuu.xdkandroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import com.fiuu.xdkandroid.logger.LogLevel
import com.fiuu.xdkandroid.logger.TerminalLogManager
import com.fiuu.xdkandroid.models.PaymentParamsState
import com.fiuu.xdkandroid.theme.GlassTokens
import com.fiuu.xdkandroid.ui.components.FloatingPillNavBar
import com.fiuu.xdkandroid.ui.components.StudioTab
import com.fiuu.xdkandroid.ui.components.StudioTopAppBar
import com.fiuu.xdkandroid.ui.screens.DiagnosticStudioScreen
import com.fiuu.xdkandroid.ui.screens.FlightDeckScreen
import com.fiuu.xdkandroid.ui.screens.MerchantConfigScreen
import com.fiuu.xdkandroid.ui.screens.PaymentConfigScreen
import com.fiuu.xdkandroid.data.remote.dto.PaymentResponseDto
import com.fiuu.xdkandroid.platform.ProvideFiuuPaymentSDK
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private val prettyJson = Json { prettyPrint = true }

@Composable
fun TestStudioApp(
    state: PaymentParamsState,
    onGooglePayClick: () -> Unit,
    onSamsungPayClick: () -> Unit,
    onCheckStatusClick: () -> Unit,
    isGooglePayReady: Boolean,
    isSamsungPayReady: Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { StudioTab.entries.size })
    val currentTab = StudioTab.entries[pagerState.currentPage]
    var isSdkPresented by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassTokens.CanvasGradient)
    ) {
        if (isSdkPresented) {
            ProvideFiuuPaymentSDK(
                params = state.toParamsMap(),
                onPaymentResult = { result ->
                    val jsonOutput = prettyJson.encodeToString(PaymentResponseDto.serializer(), result)
                    // 00/11/22 are gateway results and USER_CANCELLED is the user's choice — only anything else (408, SDK_ERROR, SECURITY_ERROR, ...) is an SDK error.
                    val isNormalResult = result.status in setOf("00", "11", "22", "USER_CANCELLED")
                    TerminalLogManager.log(
                        level = if (isNormalResult) LogLevel.STATUS else LogLevel.ERROR,
                        tag = "PAYMENT_RESULT",
                        message = "Status: ${result.status} (statusCheck=${result.statusCheck})",
                        details = jsonOutput
                    )
                    isSdkPresented = false
                    state.refreshOrderId()
                    // TerminalLogManager.log(
                    //     level = LogLevel.STATUS,
                    //     tag = "ORDER_ID",
                    //     message = "Auto-generated new Order ID for next payment: ${state.orderId}"
                    // )
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(StudioTab.FLIGHT_DECK.ordinal)
                    }
                },
                onRequestClose = {
                    // TerminalLogManager.log(LogLevel.STATUS, "PAYMENT_CLOSE", "Payment sheet closed by user")
                    isSdkPresented = false
                    state.refreshOrderId()
                    // TerminalLogManager.log(
                    //     level = LogLevel.STATUS,
                    //     tag = "ORDER_ID",
                    //     message = "Auto-generated new Order ID for next payment: ${state.orderId}"
                    // )
                }
            )
        } else {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    StudioTopAppBar(
                        state = state,
                        currentTab = currentTab
                    )
                },
                bottomBar = {
                    FloatingPillNavBar(
                        selectedTab = currentTab,
                        onTabSelected = { tab ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(tab.ordinal)
                            }
                        }
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        // Edge-to-edge: shrink the tabs by the keyboard so their forms can scroll
                        // the last field above it (adjustResize in the manifest delivers the inset).
                        .consumeWindowInsets(padding)
                        .imePadding()
                        // Tapping empty space closes the keyboard; fields/buttons/scrolls consume their own touches.
                        .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (StudioTab.entries[page]) {
                            StudioTab.PAYMENT -> PaymentConfigScreen(
                                state = state
                            )
                            StudioTab.MERCHANT -> MerchantConfigScreen(
                                state = state
                            )
                            StudioTab.FLIGHT_DECK -> FlightDeckScreen(
                                state = state,
                                onStartPayment = {
                                    TerminalLogManager.log(LogLevel.INIT, "START_FLOW", "Launching Fiuu Payment SDK Sheet...")
                                    isSdkPresented = true
                                },
                                onGooglePayClick = onGooglePayClick,
                                onSamsungPayClick = onSamsungPayClick,
                                onCheckStatusClick = onCheckStatusClick,
                                isGooglePayReady = isGooglePayReady,
                                isSamsungPayReady = isSamsungPayReady
                            )
                            StudioTab.DIAGNOSTICS -> DiagnosticStudioScreen(
                                onTriggerPaymentFlow = {
                                    TerminalLogManager.log(LogLevel.INIT, "STRESS_FLOW", "Stress launching Fiuu Payment SDK Sheet...")
                                    isSdkPresented = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
