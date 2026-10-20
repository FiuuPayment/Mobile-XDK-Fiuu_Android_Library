package com.fiuu.xdkandroid.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiuu.xdkandroid.components.AmountCard
import com.fiuu.xdkandroid.components.CustomAlertDialog
import com.fiuu.xdkandroid.components.CustomText
import com.fiuu.xdkandroid.components.OrderDetailsCard
import com.fiuu.xdkandroid.components.PaymentMethods
import com.fiuu.xdkandroid.components.PaymentScaffold
import com.fiuu.xdkandroid.components.QuickPay
import com.fiuu.xdkandroid.cardManual.CardManualEntryScreen
import com.fiuu.xdkandroid.cardManual.CardInstalmentEntryScreen
import com.fiuu.xdkandroid.domain.entity.PaymentChannel
import com.fiuu.xdkandroid.domain.entity.PaymentRequest
import com.fiuu.xdkandroid.domain.entity.isBillDescriptionEditable
import com.fiuu.xdkandroid.domain.entity.isBillEmailEditable
import com.fiuu.xdkandroid.domain.entity.isBillMobileEditable
import com.fiuu.xdkandroid.domain.entity.isBillNameEditable
import com.fiuu.xdkandroid.clickToPay.ClickToPayHandler
import com.fiuu.xdkandroid.platform.ImageSaver
import com.fiuu.xdkandroid.platform.isAndroidPlatform
import com.fiuu.xdkandroid.platform.isIOSPlatform
import com.fiuu.xdkandroid.ui.model.PayWithItem
import com.fiuu.xdkandroid.ui.model.PaymentError
import com.fiuu.xdkandroid.ui.model.PaymentScreen
import com.fiuu.xdkandroid.ui.theme.AppColors
import com.fiuu.xdkandroid.utils.BuildInfo
import com.fiuu.xdkandroid.utils.FiuuLogger
import com.fiuu.xdkandroid.viewmodel.PaymentViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PaymentMultiChannel(
        vm: PaymentViewModel,
        paymentRequest: PaymentRequest,
        screen: PaymentScreen,
        onScreenChange: (PaymentScreen) -> Unit,
        showAppBar: Boolean = true,
        onRequestClose: () -> Unit,
        secondsRemaining: () -> Int,
        imageSaver: ImageSaver? = null
    ) {
        val state by vm.uiState.collectAsState()

        // Offline/OTC payment instruction (Cash-711, etc.) — takes over the whole screen with no
        // back navigation, same as the ManualCardEntry/CardInstalmentEntry bypasses below.
        state.paymentInstruction?.let { instruction ->
            PaymentInstructionScreen(
                instruction = instruction,
                imageSaver = imageSaver,
                country = paymentRequest.country,
                onClose = vm::confirmPaymentInstructionAndClose
            )
            return
        }

        val channels = state.channels
        val eWalletChannels = channels.filter { it.channelType == "EW"}
        val fpxChannels = channels.filter { it.channelType == "IB" }
        val ccChannels = channels.filter { it.channelType == "CC" }
        val bnplChannels = channels.filter { it.channelType == "BNPL" }
        val cashChannels = channels.filter { it.channelType == "OTC" }
        // The backend returns whatever PLUGIN channels the merchant has configured regardless of
        // the device it's rendering on (e.g. Apple Pay for an Android-only integration) — filter
        // each wallet down to the platform that can actually present it, and to a device that can
        // pay with it: state.isApplePayReady / isGooglePayReady / isSamsungPayReady come from each
        // handler's own readiness check, since "iOS"/"Android" alone isn't "has that wallet".
        // Mastercard Click to Pay (maskname "Mastercard C2P", titled after the merchant, e.g.
        // "RAZER MERCHANT SERVICES") is hidden — C2P is only reachable via the card flow.
        val pluginPaymentChannels = channels.filter { it.channelType == "PLUGIN" }.filter { channel ->
            val maskName = channel.maskName.orEmpty()
            when {
                maskName.contains("C2P", ignoreCase = true) ||
                    maskName.contains("Click to Pay", ignoreCase = true) -> false
                maskName.contains("ApplePay", ignoreCase = true) ||
                    maskName.contains("Apple Pay", ignoreCase = true) -> isIOSPlatform() && state.isApplePayReady
                maskName.contains("SamsungPay", ignoreCase = true) ||
                    maskName.contains("Samsung Pay", ignoreCase = true) -> isAndroidPlatform() && state.isSamsungPayReady
                maskName.contains("GooglePay", ignoreCase = true) ||
                    maskName.contains("Google Pay", ignoreCase = true) -> isAndroidPlatform() && state.isGooglePayReady
                else -> true
            }
        }
        val clickToPayHandler = remember { ClickToPayHandler() }

    val clickToPayRequest = state.clickToPayRequest
    if (clickToPayRequest != null) {
        clickToPayHandler.PaymentScreen(
            url = clickToPayRequest.url,
            headers = clickToPayRequest.headers,
            amount = clickToPayRequest.amount,
            currency = clickToPayRequest.currency,
            country = clickToPayRequest.country,
            closeButtonDisplay = paymentRequest.closeButtonDisplay,
            secondsRemaining = secondsRemaining,
            onSuccess = vm::onClickToPaySuccess,
            onCancel = vm::onClickToPayCancel,
            onFullCancel = vm::onClickToPayFullCancel,
            onNavigateToManualCard = {
                vm.onClickToPayNavigateToManualCard()
                onScreenChange(PaymentScreen.ManualCardEntry)
            }
        )
        return
    }

    // CardManualEntryScreen draws its own full-screen PaymentScaffold (top bar + insets) — nesting
    // it inside this composable's own PaymentScaffold below would consume system-bar insets twice
    // (once per Scaffold), leaving an unstyled gap above/below where neither one paints. Bypass
    // this Scaffold entirely instead, same as the clickToPayRequest early-return above.
    if (screen is PaymentScreen.ManualCardEntry) {
        CardManualEntryScreen(
            amount = state.amount,
            currency = state.currency,
            country = paymentRequest.country,
            cardTokens = state.cardTokens,
            cardTokensRemovable = state.cardTokensRemovable,
            loading = state.loading,
            showAppBar = showAppBar,
            secondsRemaining = secondsRemaining,
            onBack = { onScreenChange(PaymentScreen.Methods) },
            onClose = onRequestClose,
            onDeleteSavedCard = { card -> vm.deleteSavedCard(card) },
            onPayNow = { savedCardToken, cardHolderName, card, expiry, cvv, saveCard ->
                if (savedCardToken != null) {
                    // CardManualEntryScreen has already shown its own confirmation dialog before
                    // invoking this callback — proceed straight to /process.
                    vm.payWithSavedCard(savedCardToken)
                } else {
                    vm.payWithManualCard(cardHolderName, card, expiry, cvv, saveCard)
                }
            }
        )
        return
    }

    // Same reasoning as the ManualCardEntry branch above — draws its own full-screen
    // PaymentScaffold, so it must bypass this composable's Scaffold too.
    if (screen is PaymentScreen.CardInstalmentEntry) {
        CardInstalmentEntryScreen(
            amount = state.amount,
            currency = state.currency,
            country = paymentRequest.country,
            instalmentTenureLoading = state.instalmentTenureLoading,
            instalmentTenureResult = state.instalmentTenureResult,
            loading = state.loading,
            showAppBar = showAppBar,
            secondsRemaining = secondsRemaining,
            onBack = { onScreenChange(PaymentScreen.Methods) },
            onClose = onRequestClose,
            onCardOrExpiryValid = { card, expiry -> vm.fetchInstalmentTenure(card, expiry) },
            onPayNow = { cardHolderName, card, expiry, cvv, instalmentMonth ->
                vm.payWithInstalmentCard(
                    cardHolderName = cardHolderName,
                    cardNumber = card,
                    expiry = expiry,
                    cvv = cvv,
                    instalmentMonth = instalmentMonth
                )
            }
        )
        return
    }
        val pluginChannels = pluginPaymentChannels

        LaunchedEffect(channels) {
            FiuuLogger.log("[Channels] all=${channels.map { "${it.name}(${it.channelType})" }}")
            FiuuLogger.log("[Channels] plugin=${pluginChannels.map { "${it.name} maskName=${it.maskName}" }}")
        }

    (state.paymentError as? PaymentError.ApplePayError)?.let { error ->
        CustomAlertDialog(
            title = "Payment Not Completed",
            message = error.errorMessage,
            backgroundColor = AppColors.Primary,
            cornerRadius = 16.dp,
            confirmButtonColor = AppColors.Secondary,
            titleTextColor = AppColors.White,
            messageTextColor = AppColors.White,
            onDismiss = { vm.clearPaymentError() }
        )
    }

    PaymentScaffold(
        showAppBar = showAppBar,
        // Title is only meaningful once a channel type has been picked — the initial
        // method list screen shows no title at all.
        title = if (screen is PaymentScreen.Channels) "Change Payment Method" else "",
        titleFontSize = 15.sp,
        secondsRemaining = secondsRemaining,
        onNavigateBack = when (screen) {
            PaymentScreen.Methods -> null
            is PaymentScreen.Channels -> { { onScreenChange(PaymentScreen.Methods) } }
            PaymentScreen.ManualCardEntry -> null // unreachable — handled by the early return above
            PaymentScreen.CardInstalmentEntry -> null // unreachable — handled by the early return above
        },
        onClose = { onRequestClose() },
        loading = state.loading
    ) {

        when (screen) {
            PaymentScreen.Methods -> {
                // The footer is a row of its own under the list, not overlaid on it — overlaid, it
                // covered the list's last card and, with the keyboard up, the focused Order Details field.
                Column(Modifier.fillMaxSize().background(Color(0xFFF4F6F8))) {
                    LazyColumn (
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item {
                            Column {
                                AmountCard(state.amount)

                                Spacer(modifier = Modifier.padding(top = 8.dp))

                                //Getting user info from paymentrequest
                                OrderDetailsCard(
                                    state = state,
                                    nameEditable = paymentRequest.isBillNameEditable,
                                    emailEditable = paymentRequest.isBillEmailEditable,
                                    mobileEditable = paymentRequest.isBillMobileEditable,
                                    descriptionEditable = paymentRequest.isBillDescriptionEditable,
                                    onName = vm::updateName,
                                    onEmail = vm::updateEmail,
                                    onPhone = vm::updatePhone,
                                    onDesc = vm::updateDescription
                                )

                                Spacer(modifier = Modifier.padding(top = 8.dp))

                                val isMultiChannel = paymentRequest.channel?.trim()?.lowercase() == "multi"
                                // One Quick Pay card for every wallet button — Apple Pay used to get
                                // its own card, so iOS showed an empty card plus an Apple Pay card.
                                val quickPayItems = buildList {
                                    if (isAndroidPlatform() && state.isGooglePayReady) {
                                        add(PaymentChannel(name = "GooglePay", title = "Google Pay", status = 1))
                                    }
                                    if (state.isSamsungPayReady) {
                                        add(PaymentChannel(name = "SamsungPay", title = "Samsung Pay", status = 1))
                                    }
                                    if (isMultiChannel && !paymentRequest.apMerchantId.isNullOrBlank() && isIOSPlatform() && state.isApplePayReady) {
                                        add(
                                            pluginChannels.firstOrNull {
                                                it.maskName?.contains("ApplePay", ignoreCase = true) == true
                                            } ?: PaymentChannel(
                                                name = "ApplePay",
                                                title = "Apple Pay",
                                                status = 1,
                                                maskName = "ApplePay",
                                                channelType = "PLUGIN"
                                            )
                                        )
                                    }
                                }
                                if (quickPayItems.isNotEmpty()) {
                                    QuickPay(items = quickPayItems) { item ->
                                        if (state.loading) return@QuickPay
                                        when {
                                            item.name == "GooglePay" -> vm.triggerGooglePay()
                                            item.name == "SamsungPay" -> vm.triggerSamsungPay()
                                            item.maskName?.contains("ApplePay", ignoreCase = true) == true -> vm.startApplePay()
                                        }
                                    }
                                    Spacer(modifier = Modifier.padding(top = 8.dp))
                                }

                                when {
                                    isMultiChannel ->{
                                        Column {
                                            //SELECTION CHANNEL
                                            PaymentMethods(
                                                items = listOf(
                                                    // Always visible — native manual card entry, independent of
                                                    // whether the backend returned any CC-type channels.
                                                    PayWithItem(
                                                        title = "Credit/Debit Card",
                                                        channels = ccChannels,
                                                        // staticIcon = AppIcons.CreditCard,
                                                        // staticCaption = "",
                                                        // destination = PaymentScreen.ManualCardEntry
                                                    )
                                                ) + listOf(
                                                    PayWithItem(
                                                        title = "Online Banking",
                                                        channels = fpxChannels,
                                                    ),
                                                    PayWithItem(
                                                        title = "eWallets",
                                                        channels = eWalletChannels
                                                    ),
                                                    PayWithItem(
                                                        title = "Buy Now Pay Later",
                                                        channels = bnplChannels
                                                    ),
                                                    PayWithItem(
                                                        title = "Fiuu CASH",
                                                        channels = cashChannels
                                                    ),
                                                    // No "Plugin" tile: its wallets (Google/Samsung/Apple Pay) are
                                                    // offered by the Quick Pay card above.
                                                ).filter { it.channels.isNotEmpty() }
                                            ) {
                                                    item -> onScreenChange(item.destination ?: PaymentScreen.Channels(item))
                                            }
                                        }
                                    } else -> {
                                        if (paymentRequest.channel?.trim()?.lowercase()?.contains("credit") == true) {
                                            CheckoutScreen(vm, state)
                                        }else {
                                            SinglePaymentScreen(
                                                vm = vm,
                                                paymentRequest,
                                                onRequestClose = onRequestClose
                                            )
                                        }

                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val envTag = when (paymentRequest.buildMode) {
                            4 -> "[Sandbox] "
                            3 -> "[UAT] "
                            else -> ""
                        }
                        CustomText(
                            label = "Version $envTag${BuildInfo.SDK_VERSION_NAME} (${BuildInfo.SDK_VERSION_CODE})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Image(
                            bitmap = rememberPoweredByMolpay(),
                            contentDescription = "Powered by MOLPay",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.height(16.dp),
                        )
                    }
                }
            }

            is PaymentScreen.Channels -> {
                PaymentChannelSelection(
                    vm = vm,
                    item = screen.item,
                    country = paymentRequest.country,
                    onScreenChange = onScreenChange
                )
            }

            // Unreachable — handled by the early return above, before this Scaffold is built.
            PaymentScreen.ManualCardEntry -> Unit
            PaymentScreen.CardInstalmentEntry -> Unit
        }
    }
}
