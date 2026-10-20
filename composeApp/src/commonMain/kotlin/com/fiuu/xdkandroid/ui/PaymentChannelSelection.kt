package com.fiuu.xdkandroid.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fiuu.xdkandroid.components.BottomPayBar
import com.fiuu.xdkandroid.components.ChannelGrid
import com.fiuu.xdkandroid.components.TermsAndPrivacyConsentText
import com.fiuu.xdkandroid.components.TermsAndPrivacyStyle
import com.fiuu.xdkandroid.ui.model.PayWithItem
import com.fiuu.xdkandroid.ui.model.PaymentScreen
import com.fiuu.xdkandroid.viewmodel.PaymentViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PaymentChannelSelection(
    vm: PaymentViewModel,
    item: PayWithItem,
    country: String? = null,
    onScreenChange: (PaymentScreen) -> Unit
) {
    val uiState by vm.uiState.collectAsState()
    // uiState.selectedChannel is shared across every PaymentChannelSelection instance for the
    // whole ViewModel session — a selection made in a different PayWithItem group (e.g. eWallets)
    // would otherwise still read as "selected" here after navigating into a different group
    // (e.g. Online Banking), enabling Pay for a channel that isn't even shown on this screen.
    // Restrict what this screen treats as selected to a channel that actually belongs to it.
    val selectedInThisGroup = uiState.selectedChannel?.takeIf { sc -> item.channels.any { it.name == sc.name } }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F6F8))
                .padding(16.dp)
        ) {

            Text(
                "Please Select A Channel To Proceed",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(12.dp))

            ChannelGrid(
                channels = item.channels,
                selected = selectedInThisGroup,
                onSelect = { vm.onChannelSelected(it)}
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White)
        ) {
            BottomPayBar(
                enabled = selectedInThisGroup != null,
                amount = uiState.amount,
                currency = uiState.currency,
                onPay = {
                    val selected = selectedInThisGroup
                    // "CardPayment" (CC) has no hosted-page /process flow — Pay opens the same
                    // Click to Pay widget (vm.triggerClickToPay). C2P handles both
                    // enrolled and non-enrolled cards, and its fallbacks still route to
                    // CardManualEntryScreen (see onClickToPayNavigateToManualCard).
                    if (selected?.channelType == "CC" &&
                        selected.name.equals("CardPayment", ignoreCase = true)
                    ) {
                        vm.triggerClickToPay()
                    } else if (selected?.isInstallment == true) {
                        // Instalment-eligible CC channels (e.g. "CardInstallment") also need the
                        // card typed in natively — same reasoning as "CardPayment" above, plus an
                        // Instalment Period section keyed off that card/expiry.
                        onScreenChange(PaymentScreen.CardInstalmentEntry)
                    } else {
                        vm.startPayment()
                    }
                }
            )

            TermsAndPrivacyConsentText(
                country = country,
                style = TermsAndPrivacyStyle.Compact,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}




