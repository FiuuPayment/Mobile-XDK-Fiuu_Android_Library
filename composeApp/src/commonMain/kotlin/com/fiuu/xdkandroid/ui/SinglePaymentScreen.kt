package com.fiuu.xdkandroid.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fiuu.xdkandroid.components.BottomPayBar
import com.fiuu.xdkandroid.components.CustomAlertDialog
import com.fiuu.xdkandroid.components.RemoteImage
import com.fiuu.xdkandroid.components.TermsAndPrivacyConsentText
import com.fiuu.xdkandroid.domain.entity.PaymentChannel
import com.fiuu.xdkandroid.domain.entity.PaymentRequest
import com.fiuu.xdkandroid.ui.model.PaymentError
import com.fiuu.xdkandroid.ui.theme.AppColors
import com.fiuu.xdkandroid.viewmodel.PaymentViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SinglePaymentScreen(
    vm: PaymentViewModel,
    paymentRequest: PaymentRequest,
    cornerRadius: Dp = 12.dp,
    backgroundColor: Color = Color.White,
    onRequestClose: () -> Unit
) {

    val uiState by vm.uiState.collectAsState()
    val channels = uiState.channels

    // Keyed on `channels` too, not just paymentRequest.channel — channels arrives asynchronously
    // from fetchAvailableChannels(), so this must recompute once the live list lands rather than
    // permanently locking in null from the first (still-empty) composition.
    //
    // A channel /initiate didn't return isn't payable on this session, even when the live list
    // has it — reject it here rather than show a Pay button whose /process call would fail.
    // `channels` is still preferred for display since the live entry carries richer data.
    val selectedChannel = remember(paymentRequest.channel, channels, uiState.initiateChannels) {
        val matches = { c: PaymentChannel -> c.name.equals(paymentRequest.channel?.trim(), ignoreCase = true) }
        uiState.initiateChannels.firstOrNull(matches)?.let { initiated ->
            channels.firstOrNull(matches) ?: initiated
        }
    }

    LaunchedEffect(selectedChannel, uiState.channelsResolved) {
        // Only treat this as a real "channel not supported" error once channel resolution has
        // actually settled — channels.isEmpty() alone doesn't distinguish "still loading" from
        // "loaded, genuinely no match" (e.g. the merchant has zero available channels), so this
        // used to silently suppress the error in that second case too.
        if (selectedChannel == null && uiState.channelsResolved) {
            vm.setPaymentError(
                PaymentError.InvalidChannel(paymentRequest.channel)
            )
        }
    }

    // 🔴 ERROR DIALOG (belongs here)
    uiState.paymentError?.let { error ->
        val message = when (error) {
            is PaymentError.InvalidChannel ->
                "This payment channel is not supported."

            is PaymentError.NetworkError ->
                "Network error. Please check your internet connection."

            is PaymentError.Unknown ->
                "Unexpected error occurred."

            else -> {
                "Unexpected error occurred."
            }
        }

        CustomAlertDialog(
            title = "Payment Error",
            message = message,
            backgroundColor = AppColors.Primary,
            cornerRadius = 16.dp,
            confirmButtonColor = AppColors.Secondary,
            titleTextColor = AppColors.White,
            messageTextColor = AppColors.White,
            icon = {
                SvgIcon(
                    imageVector = AppIcons.Warning,
                    contentDescription = "warning",
                )
            },
            onDismiss = {
                vm.clearPaymentError()
                onRequestClose()
            }
        )
    }

    // Not resolved yet — show a loading indicator rather than a blank frame while waiting, so it
    // can't be mistaken for the (now correctly reported) "genuinely no matching channel" case.
    if (selectedChannel == null) {
        if (!uiState.channelsResolved) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    Card (
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                "Selected Channel",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            if (selectedChannel.logoUrl3 != null) {
                RemoteImage(
                    url = selectedChannel.logoUrl3,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(70.dp),
                    contentDescription = uiState.selectedChannel?.title,
                    onLoading = {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    },
                    onFailure = {
                        SvgIcon(
                            imageVector = AppIcons.BrokenImage,
                            contentDescription = "Image null",
                        )
                    }
                )
                Spacer(Modifier.size(16.dp))
            }

            Text(
                selectedChannel.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.size(16.dp))

            TermsAndPrivacyConsentText(country = paymentRequest.country)

            Spacer(Modifier.size(8.dp))


            BottomPayBar(
                enabled = true,
                amount = vm.uiState.collectAsState().value.amount,
                currency = vm.uiState.collectAsState().value.currency,
                onPay = {
                    if (selectedChannel.name.equals("ApplePay", ignoreCase = true)) {
                        // Apple Pay can't be completed by startPayment() alone — it needs a
                        // real PKPaymentAuthorizationController round trip first to obtain a
                        // token. startApplePay() does that, then calls startPayment() itself
                        // once ApplePayHandler reports success.
                        vm.startApplePay()
                    } else {
                        vm.startPayment()
                    }
                },
            )
        }


    }
}