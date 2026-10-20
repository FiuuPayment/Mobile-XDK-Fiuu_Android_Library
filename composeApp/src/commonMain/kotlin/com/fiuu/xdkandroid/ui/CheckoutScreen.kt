package com.fiuu.xdkandroid.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fiuu.xdkandroid.ui.model.PaymentUiState
import com.fiuu.xdkandroid.ui.theme.AppColors
import com.fiuu.xdkandroid.viewmodel.PaymentViewModel


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CheckoutScreen(
    vm: PaymentViewModel,
    state: PaymentUiState
) {
   Card (
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                "Selected Channel",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            // Reuses the same Click to Pay flow as the QuickPay button (ClickToPayHandler,
            // driven by state.clickToPayRequest) instead of a separate C2P integration —
            // PaymentMultiChannel renders the handler once clickToPayRequest is set.
            LaunchedEffect(Unit) {
                vm.triggerClickToPay()
            }
        }
    }
}