package com.fiuu.xdkandroid.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fiuu.xdkandroid.components.CustomAlertDialog
import com.fiuu.xdkandroid.domain.entity.PaymentRequest
import com.fiuu.xdkandroid.ui.model.PaymentError
import com.fiuu.xdkandroid.ui.theme.AppColors
import com.fiuu.xdkandroid.viewmodel.PaymentViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardPaymentScreen(
    vm: PaymentViewModel,
    paymentRequest: PaymentRequest,
    cornerRadius: Dp = 12.dp,
    backgroundColor: Color = Color.White,
    onRequestClose: () -> Unit
) {

    var selectedCardId by remember { mutableStateOf<String?>(null) }
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var cvvVisible by remember { mutableStateOf(false) }

    val cards = listOf(
        SavedCard("1", "**** 0000"),
        SavedCard("2", "**** 0000")
    )


    val uiState by vm.uiState.collectAsState()
    val channels = uiState.channels

    // Keyed on `channels` too, not just paymentRequest.channel — channels arrives asynchronously
    // from fetchAvailableChannels(), so this must recompute once the live list lands rather than
    // permanently locking in null from the first (still-empty) composition.
    val selectedChannel = remember(paymentRequest.channel, channels) {
        channels.firstOrNull { it.name.equals(paymentRequest.channel?.trim(), ignoreCase = true) }
    }

    LaunchedEffect(selectedChannel, channels) {
        // Only treat this as a real "channel not supported" error once the channel list has
        // actually loaded — an empty list just means the fetch hasn't returned yet.
        if (selectedChannel == null && channels.isNotEmpty()) {
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

            PaymentError.NetworkError() ->
                "Network error. Please check your internet connection."

            PaymentError.Unknown() ->
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
                    resDrawablePath = AppIcons.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFFFAA00),
                    size = 48.dp
                )
            },
            onDismiss = {
                vm.clearPaymentError()
                onRequestClose()
            }
        )
    }

    //STOP RENDERING UI IF PAYMENT CHANNEL NOT MEET
    if (selectedChannel == null) return


    Card (
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ){
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {

            SavedCardSection(
                cards = cards,
                selectedId = selectedCardId,
                onSelect = { selectedCardId = it },
                onDelete = { /* delete */ }
            )

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            AddNewCardSection(
                cardNumber = cardNumber,
                expiry = expiry,
                cvv = cvv,
                cvvVisible = cvvVisible,
                onCardNumberChange = { cardNumber = it },
                onExpiryChange = { expiry = it },
                onCvvChange = { cvv = it },
                onToggleCvv = { cvvVisible = !cvvVisible }
            )
        }
    }


}

data class SavedCard(
    val id: String,
    val maskedNumber: String, // e.g. "**** 0000"
)

@Composable
fun SavedCardItem(
    card: SavedCard,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) Color(0xFF2F80ED) else Color(0xFFE0E0E0)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {

            RadioButton(
                selected = selected,
                onClick = onSelect
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = card.maskedNumber,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onDelete) {
                SvgIcon(
                    resDrawablePath = AppIcons.Close,
                    contentDescription = "Delete card",
                    tint = Color(0xFFFFAA00),
                    size = 48.dp
                )
            }
        }
    }
}

@Composable
private fun SavedCardSection(
    cards: List<SavedCard>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDelete: (SavedCard) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Text(
            "Saved Card",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        cards.forEach { card ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedId == card.id,
                        onClick = { onSelect(card.id) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(card.maskedNumber, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onDelete(card) }) {
                        SvgIcon(
                            resDrawablePath = AppIcons.Close,
                            contentDescription = "CLOSE",
                            tint = Color(0xFFFFAA00),
                            size = 48.dp
                        )
                    }
                }
            }
        }

        Text(
            "More cards",
            color = Color(0xFF2F80ED),
            modifier = Modifier.clickable { }
        )

        Spacer(Modifier.height(8.dp))

    }
}


@Composable
private fun AddNewCardSection(
    cardNumber: String,
    expiry: String,
    cvv: String,
    cvvVisible: Boolean,
    onCardNumberChange: (String) -> Unit,
    onExpiryChange: (String) -> Unit,
    onCvvChange: (String) -> Unit,
    onToggleCvv: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Spacer(Modifier.height(8.dp))

        Text(
            "Add New Card Number",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = cardNumber,
            onValueChange = onCardNumberChange,
            shape = RoundedCornerShape(corner = CornerSize(12.dp)),
            placeholder = { Text("XXXX XXXX XXXX XXXX") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            OutlinedTextField(
                value = expiry,
                onValueChange = onExpiryChange,
                shape = RoundedCornerShape(corner = CornerSize(12.dp)),
                placeholder = { Text("MM / YY") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = cvv,
                onValueChange = onCvvChange,
                shape = RoundedCornerShape(corner = CornerSize(12.dp)),
                placeholder = { Text("XXX") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = if (cvvVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onToggleCvv) {
                        if (cvvVisible) {
                            SvgIcon(
                                resDrawablePath = AppIcons.Visibility,
                                contentDescription = "Show Cvv",
                            )
                        } else {
                            SvgIcon(
                                resDrawablePath = AppIcons.VisibilityOff,
                                contentDescription = "Hide Cvv",
                            )
                        }
                    }
                }
            )
        }
    }
}
