package com.fiuu.xdkandroid.cardManual

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiuu.xdkandroid.components.BottomPayBar
import com.fiuu.xdkandroid.components.PayBarButtonStyle
import com.fiuu.xdkandroid.components.PaymentScaffold
import com.fiuu.xdkandroid.components.TermsAndPrivacyConsentText
import com.fiuu.xdkandroid.components.TermsAndPrivacyStyle
import com.fiuu.xdkandroid.domain.entity.CardTokenInit
import com.fiuu.xdkandroid.ui.AppIcons
import com.fiuu.xdkandroid.ui.SvgIcon
import com.fiuu.xdkandroid.ui.theme.AppColors

// Mirrors the hosted "Change Payment Method" card page: a saved-card picker on top of a manual
// entry form, used when the caller wants a fully native fallback instead of the WebView-hosted
// checkout page. Saved cards come straight from GET card-tokens, fetched after /initiate resolves
// an accessToken (PaymentUiState.cardTokens) — only the bin4 is shown, never the full PAN.
@Composable
fun CardManualEntryScreen(
    amount: Long,
    currency: String,
    country: String? = null,
    cardTokens: List<CardTokenInit> = emptyList(),
    // From cardToken.removable in the /initiate response.
    cardTokensRemovable: Boolean = false,
    loading: Boolean = false,
    // false (mp_closebutton_display) hides the app bar — see PaymentScaffold.
    showAppBar: Boolean = true,
    secondsRemaining: (() -> Int)? = null,
    onBack: () -> Unit = {},
    onClose: () -> Unit,
    onDeleteSavedCard: (CardTokenInit) -> Unit = {},
    // saveCard maps to xp_token_status ("1" to save/tokenize this card, "0" not to) for a freshly
    // typed-in card. When savedCardToken is non-null instead, the other card fields are ignored —
    // the caller charges that token directly (xp_token_status = "2") after the user confirms the
    // pay-with-saved-card dialog this screen shows on Pay.
    onPayNow: (savedCardToken: String?, cardHolderName: String, card: String, expiry: String, cvv: String, saveCard: Boolean) -> Unit
) {
    var selectedSavedCardToken by remember { mutableStateOf<String?>(null) }
    var showSavedCardConfirm by remember { mutableStateOf(false) }
    // Non-null while the remove-saved-card confirmation dialog is showing for that card.
    var pendingDeleteCard by remember { mutableStateOf<CardTokenInit?>(null) }

    var cardHolderName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var cardNumberVisible by remember { mutableStateOf(true) }
    var expiryDateValue by remember { mutableStateOf(TextFieldValue("")) }
    var cvv by remember { mutableStateOf("") }
    var cvvVisible by remember { mutableStateOf(false) }
    var saveCard by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    // The saved-card picker and the new-card form are mutually exclusive: picking a saved card
    // wipes anything typed below, and touching the form drops the saved-card selection. Focus is
    // cleared on selection so the next tap on a field fires onFocusChanged again.
    fun selectSavedCard(token: String) {
        selectedSavedCardToken = token
        cardHolderName = ""
        cardNumber = ""
        expiryDateValue = TextFieldValue("")
        cvv = ""
        saveCard = false
        focusManager.clearFocus()
    }
    val clearSavedCardOnFocus = Modifier.onFocusChanged { state ->
        if (state.isFocused) selectedSavedCardToken = null
    }

    // A saved card needs none of the new-card fields below — selecting one is enough to pay.
    val canPay = selectedSavedCardToken != null ||
        (cardHolderName.isNotBlank() &&
            cardNumber.matches(Regex("^\\d{12,19}$")) &&
            expiryDateValue.text.matches(Regex("^(0[1-9]|1[0-2])/\\d{2}$")) &&
            cvv.matches(Regex("^\\d{3,4}$")))

    // PaymentScaffold is the same app-bar component every other screen in the SDK uses
    // (PaymentMultiChannel, ClickToPayRegistrationRequiredView, etc.) — its M3 Scaffold/TopAppBar
    // handles status-bar/nav-bar insets automatically, which a manually drawn Box top bar does not.
    PaymentScaffold(
        showAppBar = showAppBar,
        title = "Change Payment Method",
        titleFontSize = 15.sp,
        secondsRemaining = secondsRemaining,
        onNavigateBack = onBack,
        onClose = onClose,
        loading = loading
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Screen)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Saved card + add new card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (cardTokens.isNotEmpty()) {
                        Text(
                            text = "Saved Card",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Spacer(Modifier.height(12.dp))

                        cardTokens.forEach { card ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AppColors.Screen, RoundedCornerShape(8.dp))
                                    .clickable { selectSavedCard(card.token) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedSavedCardToken == card.token,
                                    onClick = { selectSavedCard(card.token) },
                                    colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary)
                                )
                                Text(
                                    text = "**** ${card.bin4 ?: "0000"}",
                                    fontSize = 16.sp,
                                    color = AppColors.TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (cardTokensRemovable) {
                                    IconButton(onClick = { pendingDeleteCard = card }) {
                                        SvgIcon(
                                            resDrawablePath = AppIcons.Close,
                                            contentDescription = "Remove saved card",
                                            tint = AppColors.Error
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }

                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = AppColors.Border)
                        Spacer(Modifier.height(16.dp))
                    }

                    Text(text = "Card Holder Name", fontSize = 14.sp, color = AppColors.TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = cardHolderName,
                        onValueChange = { input -> cardHolderName = input },
                        modifier = Modifier.fillMaxWidth().then(clearSavedCardOnFocus),
                        placeholder = { Text("Name on Card", color = AppColors.TextHint) },
                        shape = RoundedCornerShape(corner = CornerSize(12.dp)),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Add New Card Number",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = { cardNumberVisible = !cardNumberVisible },
                            modifier = Modifier.height(24.dp).width(24.dp)
                        ) {
                            SvgIcon(
                                resDrawablePath = if (cardNumberVisible) AppIcons.Visibility else AppIcons.VisibilityOff,
                                contentDescription = "Toggle card number visibility",
                                tint = AppColors.TextSecondary,
                                size = 18.dp
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { input -> cardNumber = input.filter { it.isDigit() }.take(19) },
                        modifier = Modifier.fillMaxWidth().then(clearSavedCardOnFocus),
                        placeholder = { Text("XXXX XXXX XXXX XXXX", color = AppColors.TextHint) },
                        shape = RoundedCornerShape(corner = CornerSize(12.dp)),
                        singleLine = true,
                        visualTransformation = if (cardNumberVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Expiry Date", fontSize = 14.sp, color = AppColors.TextPrimary)
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = expiryDateValue,
                                onValueChange = { input ->
                                    // Auto-insert "/" as MM/YY is typed; force the cursor to the end
                                    // so Compose doesn't leave it stranded before the inserted slash.
                                    val digits = input.text.filter { it.isDigit() }.take(4)
                                    val formatted = if (digits.length > 2) {
                                        digits.substring(0, 2) + "/" + digits.substring(2)
                                    } else {
                                        digits
                                    }
                                    expiryDateValue = TextFieldValue(formatted, selection = TextRange(formatted.length))
                                },
                                modifier = Modifier.fillMaxWidth().then(clearSavedCardOnFocus),
                                placeholder = { Text("MM / YY", color = AppColors.TextHint) },
                                shape = RoundedCornerShape(corner = CornerSize(12.dp)),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "CVV", fontSize = 14.sp, color = AppColors.TextPrimary)
                                Spacer(Modifier.width(4.dp))
                                IconButton(
                                    onClick = { cvvVisible = !cvvVisible },
                                    modifier = Modifier.height(20.dp).width(20.dp)
                                ) {
                                    SvgIcon(
                                        resDrawablePath = if (cvvVisible) AppIcons.Visibility else AppIcons.VisibilityOff,
                                        contentDescription = "Toggle CVV visibility",
                                        tint = AppColors.TextSecondary,
                                        size = 16.dp
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = cvv,
                                onValueChange = { input -> cvv = input.filter { it.isDigit() }.take(4) },
                                modifier = Modifier.fillMaxWidth().then(clearSavedCardOnFocus),
                                placeholder = { Text("XXX", color = AppColors.TextHint) },
                                shape = RoundedCornerShape(corner = CornerSize(12.dp)),
                                singleLine = true,
                                visualTransformation = if (cvvVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                saveCard = !saveCard
                                selectedSavedCardToken = null
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = saveCard,
                            onCheckedChange = {
                                saveCard = it
                                selectedSavedCardToken = null
                            },
                            colors = CheckboxDefaults.colors(checkedColor = AppColors.Primary)
                        )
                        Text(
                            text = "Save this card for future payments",
                            fontSize = 14.sp,
                            color = AppColors.TextPrimary
                        )
                    }
                }
            }
        }

        // Pay bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.White)
        ) {
            BottomPayBar(
                enabled = canPay,
                amount = amount,
                currency = currency,
                style = PayBarButtonStyle.Emphasized,
                onPay = {
                    if (selectedSavedCardToken != null) {
                        // Saved-card charges skip straight to /process on confirm — surface a
                        // confirmation step first since, unlike the manual-entry path, there's no
                        // card form left on screen for the user to double-check before paying.
                        showSavedCardConfirm = true
                    } else {
                        onPayNow(null, cardHolderName, cardNumber, expiryDateValue.text, cvv, saveCard)
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

    if (showSavedCardConfirm) {
        val selectedCard = cardTokens.find { it.token == selectedSavedCardToken }
        SavedCardPaymentConfirmDialog(
            bin4 = selectedCard?.bin4 ?: "0000",
            onConfirm = {
                showSavedCardConfirm = false
                onPayNow(selectedSavedCardToken, cardHolderName, cardNumber, expiryDateValue.text, cvv, saveCard)
            },
            onDismiss = { showSavedCardConfirm = false }
        )
    }

    // Removing a saved card is irreversible (the token is deleted server-side), so ask first
    // rather than firing the delete API straight from the X tap.
    pendingDeleteCard?.let { card ->
        SavedCardRemoveConfirmDialog(
            bin4 = card.bin4 ?: "0000",
            onConfirm = {
                pendingDeleteCard = null
                if (selectedSavedCardToken == card.token) selectedSavedCardToken = null
                onDeleteSavedCard(card)
            },
            onDismiss = { pendingDeleteCard = null }
        )
    }
}

@Composable
private fun SavedCardRemoveConfirmDialog(
    bin4: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove Saved Card") },
        text = { Text("Are you sure you want to remove the card ending in $bin4?") },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                Text("Remove", color = AppColors.Error)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppColors.TextSecondary)
            }
        }
    )
}

@Composable
private fun SavedCardPaymentConfirmDialog(
    bin4: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Payment") },
        text = { Text("Proceed with payment using card ending in $bin4?") },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                Text("Confirm", color = AppColors.Primary)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppColors.TextSecondary)
            }
        }
    )
}
