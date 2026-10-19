package com.fiuu.xdkandroid.cardManual

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
import com.fiuu.xdkandroid.data.remote.dto.InstalmentTenurePlanDto
import com.fiuu.xdkandroid.data.remote.dto.InstalmentTenureResponseDto
import com.fiuu.xdkandroid.ui.AppIcons
import com.fiuu.xdkandroid.ui.SvgIcon
import com.fiuu.xdkandroid.ui.theme.AppColors

// Native card-entry screen for a CC channel where PaymentChannel.isInstallment is true (e.g. the
// "CardInstallment" channel). Reachable from PaymentChannelSelection's Pay button, same as
// CardManualEntryScreen's "CardPayment" special case. Instalment plans depend on the card number
// and expiry typed here — whenever they become valid (or change while still valid),
// onCardOrExpiryValid fires so the caller can call PaymentViewModel.fetchInstalmentTenure().
// instalmentTenureResult carries either the ineligible-card error shape (Status/error_code/
// error_desc) or, on success, instlMthList — the plans rendered below. The selected InstlMonth is
// tracked locally (selectedInstalmentMonth) and required before Pay is enabled; it's passed out
// via onPayNow's instalmentMonth param for the caller to forward to
// PaymentViewModel.payWithInstalmentCard(). No save-card option here — xp_token_status is always
// "0" for an instalment payment (see PaymentRequest.putChannel).
@Composable
fun CardInstalmentEntryScreen(
    amount: Long,
    currency: String,
    country: String? = null,
    instalmentTenureLoading: Boolean = false,
    instalmentTenureResult: InstalmentTenureResponseDto? = null,
    loading: Boolean = false,
    // false (mp_closebutton_display) hides the app bar — see PaymentScaffold.
    showAppBar: Boolean = true,
    secondsRemaining: (() -> Int)? = null,
    onBack: () -> Unit = {},
    onClose: () -> Unit,
    onCardOrExpiryValid: (cardNumber: String, expiry: String) -> Unit = { _, _ -> },
    onPayNow: (cardHolderName: String, card: String, expiry: String, cvv: String, instalmentMonth: Int) -> Unit
) {
    // InstlMonth == 0 represents the non-instalment/normal-purchase option, not a real plan.
    val instalmentPlans = remember(instalmentTenureResult) {
        instalmentTenureResult?.instlMthList?.filter { it.instlMonth > 0 } ?: emptyList()
    }
    var selectedInstalmentMonth by remember { mutableStateOf<Int?>(null) }
    var planPickerExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(instalmentPlans) {
        selectedInstalmentMonth = instalmentPlans.firstOrNull()?.instlMonth
        planPickerExpanded = instalmentPlans.isNotEmpty()
    }
    var cardHolderName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var cardNumberVisible by remember { mutableStateOf(true) }
    var expiryDateValue by remember { mutableStateOf(TextFieldValue("")) }
    var cvv by remember { mutableStateOf("") }
    var cvvVisible by remember { mutableStateOf(false) }

    // Deliberately excludes selectedInstalmentMonth — that's only ever set after a successful
    // tenure fetch, and the fetch below is only triggered once these fields are valid, so folding
    // it in here would deadlock (fetch waits on a plan, plan waits on the fetch).
    val cardFieldsValid = cardHolderName.isNotBlank() &&
        cardNumber.matches(Regex("^\\d{12,19}$")) &&
        expiryDateValue.text.matches(Regex("^(0[1-9]|1[0-2])/\\d{2}$")) &&
        cvv.matches(Regex("^\\d{3,4}$"))
    val canPay = cardFieldsValid && selectedInstalmentMonth != null

    // Fires the tenure lookup whenever (card number, expiry) becomes valid, and again whenever
    // either changes while it stays valid — CVV alone changing does not re-trigger it, since the
    // lookup only depends on PAN + expiry.
    var lastFetchedCardExpiry by remember { mutableStateOf<Pair<String, String>?>(null) }
    LaunchedEffect(cardFieldsValid, cardNumber, expiryDateValue.text) {
        if (cardFieldsValid) {
            val key = cardNumber to expiryDateValue.text
            if (key != lastFetchedCardExpiry) {
                lastFetchedCardExpiry = key
                onCardOrExpiryValid(cardNumber, expiryDateValue.text)
            }
        }
    }

    // PaymentScaffold is the same app-bar component every other screen in the SDK uses — its M3
    // Scaffold/TopAppBar handles status-bar/nav-bar insets automatically.
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
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Card Holder Name", fontSize = 14.sp, color = AppColors.TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = cardHolderName,
                        onValueChange = { input -> cardHolderName = input },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Name on Card", color = AppColors.TextHint) },
                        shape = RoundedCornerShape(corner = CornerSize(12.dp)),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Card Number",
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
                        modifier = Modifier.fillMaxWidth(),
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
                                modifier = Modifier.fillMaxWidth(),
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
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("XXX", color = AppColors.TextHint) },
                                shape = RoundedCornerShape(corner = CornerSize(12.dp)),
                                singleLine = true,
                                visualTransformation = if (cvvVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = AppColors.Border)
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Instalment Period",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))

                    if (instalmentTenureLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(16.dp).width(16.dp),
                                strokeWidth = 2.dp,
                                color = AppColors.Primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Checking available instalment plans...",
                                fontSize = 14.sp,
                                color = AppColors.TextSecondary
                            )
                        }
                    } else if (instalmentPlans.isNotEmpty()) {
                        InstalmentPlanPicker(
                            plans = instalmentPlans,
                            selectedMonth = selectedInstalmentMonth,
                            expanded = planPickerExpanded,
                            onExpandToggle = { planPickerExpanded = !planPickerExpanded },
                            onPlanSelected = { month ->
                                selectedInstalmentMonth = month
                                planPickerExpanded = false
                            }
                        )
                    } else {
                        // Prefer the backend's actual reason (e.g. "Card not eligible for
                        // installment plan.") over the generic fallback, which only shows before
                        // the first lookup or if the call didn't return a recognizable error.
                        Text(
                            text = instalmentTenureResult?.errorDesc
                                ?: "No instalment plans available for this card/expiry.",
                            fontSize = 14.sp,
                            color = AppColors.Error
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
                    selectedInstalmentMonth?.let { month ->
                        onPayNow(cardHolderName, cardNumber, expiryDateValue.text, cvv, month)
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
}

// Outlined "Select Plan" box (mirrors an OutlinedTextField's floating label) that expands into a
// list of selectable instalment tenures below it, matching the design's dropdown-with-embedded-
// list look rather than a floating popup menu.
@Composable
private fun InstalmentPlanPicker(
    plans: List<InstalmentTenurePlanDto>,
    selectedMonth: Int?,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onPlanSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = AppColors.Primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable(onClick = onExpandToggle)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedMonth?.let { "Instalment $it months" } ?: "Select a plan",
                    fontSize = 16.sp,
                    color = AppColors.TextPrimary
                )
                SvgIcon(
                    resDrawablePath = AppIcons.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = AppColors.Primary,
                    modifier = if (expanded) Modifier.rotate(180f) else Modifier
                )
            }
            Text(
                text = "Select Plan",
                fontSize = 12.sp,
                color = AppColors.Primary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 12.dp, y = (-8).dp)
                    .background(AppColors.White)
                    .padding(horizontal = 4.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Screen, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            ) {
                plans.forEachIndexed { index, plan ->
                    if (index > 0) {
                        HorizontalDivider(color = AppColors.Border)
                    }
                    val isSelected = plan.instlMonth == selectedMonth
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlanSelected(plan.instlMonth) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Instalment ${plan.instlMonth} months",
                            fontSize = 15.sp,
                            color = AppColors.TextPrimary
                        )
                        OutlinedButton(
                            onClick = { onPlanSelected(plan.instlMonth) },
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, AppColors.Primary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary)
                        ) {
                            Text(text = if (isSelected) "Selected" else "Select", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
