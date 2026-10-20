package com.fiuu.xdkandroid.clickToPay

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.fiuu.xdkandroid.ui.AppIcons
import com.fiuu.xdkandroid.ui.SvgIcon
import com.fiuu.xdkandroid.ui.theme.AppColors
import com.fiuu.xdkandroid.utils.FiuuLogger
import com.fiuu.xdkandroid.utils.redactSensitiveJson
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

// Platform-independent pieces of the Click to Pay widget host, shared by the Android
// (ClickToPayWebView, android.webkit.WebView) and iOS (ClickToPayWebView, WKWebView) actuals.

// Name of the JS object the hosted mastercardc2p page calls (AndroidBridge.postMessage(json)).
// Android injects it via addJavascriptInterface; iOS defines a shim with this name that forwards
// to a WKScriptMessageHandler, so the same hosted page works unchanged on both platforms.
internal const val C2P_BRIDGE_NAME = "AndroidBridge"

// The live widget's 30s "still loading" fallback — ~14s was the slowest normal load observed
// on-device (Mastercard/Visa's own cross-domain scripts).
internal const val C2P_LOAD_TIMEOUT_MS = 30_000L

// Routes one AndroidBridge.postMessage(json) from the hosted widget page. `type` field:
//01 -> submission error (e.g. encryptCard rejecting the card/CVV) — route to manual card entry
//02 -> no Card
//03 -> want to add new card
//04 -> can complete C2P payment, carries `paymentData`
//05 -> Error
//06 -> Error
// Must be called on the main thread — every callback here drives Compose state.
internal fun dispatchC2PBridgeMessage(
    message: String,
    onSuccess: (JsonObject) -> Unit,
    onNotRegistered: () -> Unit,
    onSubmitError: (message: String) -> Unit,
    onNavigateToManualCard: () -> Unit
) {
    FiuuLogger.log("C2P bridge: raw message=${redactSensitiveJson(message)}")
    val json = try {
        Json.parseToJsonElement(message).jsonObject
    } catch (e: Exception) {
        FiuuLogger.log("C2P bridge: invalid JSON: ${redactSensitiveJson(message)}")
        return
    }
    val type = json["type"]?.jsonPrimitive?.content
    FiuuLogger.log("C2P bridge: type=$type")
    when (type) {
        "01" -> {
            val summary = json["messsage"]?.jsonPrimitive?.content ?: "Card submission failed"
            val detail = json["error"]?.jsonPrimitive?.content
            val fullMessage = if (detail != null) "$summary: $detail" else summary
            FiuuLogger.log("C2P bridge: submission error: $fullMessage")
            onNavigateToManualCard()
        }
        "02", "03" -> {
            FiuuLogger.log("C2P bridge: not registered for Click to Pay (type $type)")
            onNotRegistered()
        }
        "05", "06" -> {
            FiuuLogger.log("C2P bridge: routing to manual card entry (type $type)")
            onNavigateToManualCard()
        }
        "04" -> {
            val paymentData = try {
                json["paymentData"]?.jsonObject
            } catch (e: IllegalArgumentException) {
                null
            }
            if (paymentData == null) {
                FiuuLogger.log("C2P bridge: finished but paymentData missing")
                onSubmitError("Click to Pay did not return payment data")
            } else {
                FiuuLogger.log("C2P bridge: finished")
                onSuccess(paymentData)
            }
        }
        else -> FiuuLogger.log("C2P bridge: state $type")
    }
}

// The live widget page defines window.onAndroidMessage(jsonString) itself (see init.js), which
// forwards { event: "button_clicked", data: { card, expiry, cvv } } into its own onAddNewCard
// handler — this is how natively-collected card details get fed back into the SRC SDK session.
// Returns the full JS statement to evaluate; the payload is passed as a properly escaped JS
// string literal (JSON string encoding), never spliced into quotes by hand.
@OptIn(ExperimentalTime::class)
internal fun c2pNewCardScript(card: String, expiry: String, cvv: String): String {
    val payload = buildJsonObject {
        put("event", "button_clicked")
        put("timestamp", Clock.System.now().toEpochMilliseconds())
        putJsonObject("data") {
            put("card", card)
            put("expiry", expiry)
            put("cvv", cvv)
        }
    }.toString()
    // Masked debug log to confirm shape/expiry without logging full PAN/CVV (PCI data).
    val maskedCard = if (card.length > 4) "${"*".repeat(card.length - 4)}${card.takeLast(4)}" else card
    FiuuLogger.log("C2P submitNewCardToWidget: sending event=button_clicked card=$maskedCard expiry=$expiry cvvLength=${cvv.length}")
    return "onAndroidMessage(${JsonPrimitive(payload)});"
}

// No backend registration endpoint exists yet — show a local placeholder styled like the
// hosted "Change Payment Method" card page (see CardManualEntryScreen), minus the Saved Card
// section since this placeholder never has a card-token list to show. "Pay Now" submits the
// typed card details back into the live widget (see submitNewCardToWidget); back returns to the
// channel list (onNavigateBack), the explicit Close button ends the whole attempt (onClose).
@Composable
internal fun ClickToPayRegistrationRequiredView(
    amount: Long,
    currency: String,
    country: String?,
    closeButtonDisplay: Boolean,
    secondsRemaining: (() -> Int)?,
    errorMessage: String?,
    onPayNow: (card: String, expiry: String, cvv: String) -> Unit,
    onNavigateBack: (String?) -> Unit,
    onClose: (String?) -> Unit
) {
    var cardNumber by remember { mutableStateOf("") }
    var cardNumberVisible by remember { mutableStateOf(true) }
    var expiryDateValue by remember { mutableStateOf(TextFieldValue("")) }
    var cvv by remember { mutableStateOf("") }
    var cvvVisible by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf(errorMessage) }

    val canPay = cardNumber.matches(Regex("^\\d{12,19}$")) &&
        expiryDateValue.text.matches(Regex("^(0[1-9]|1[0-2])/\\d{2}$")) &&
        cvv.matches(Regex("^\\d{3,4}$"))

    // PaymentScaffold is the same app-bar component every other screen in the SDK uses
    // (PaymentMultiChannel, etc.) — its M3 Scaffold/TopAppBar handles status-bar/nav-bar insets
    // automatically, which a manually drawn Box top bar does not. showAppBar mirrors
    // mp_closebutton_display's meaning on the legacy Webcore engine: false hides the bar (and its
    // back arrow) entirely — the outer ClickToPayWebView's BackHandler still routes hardware back
    // through the same onNavigateBack path as the arrow.
    PaymentScaffold(
        showAppBar = closeButtonDisplay,
        secondsRemaining = secondsRemaining,
        title = "Change Payment Method",
        titleFontSize = 15.sp,
        onNavigateBack = { onNavigateBack("User cancel payment") },
        onClose = { onClose("User cancel payment") }
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
                // Add new card — this placeholder never has a card-token list, so no Saved
                // Card section (compare CardManualEntryScreen, which shows one when tokens exist).
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("XXXX XXXX XXXX XXXX", color = AppColors.TextHint) },
                        shape = RoundedCornerShape(12.dp),
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
                                    // Auto-insert "/" as MM/YY is typed; force the cursor to the
                                    // end so Compose doesn't leave it stranded before the slash.
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
                                shape = RoundedCornerShape(12.dp),
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
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                visualTransformation = if (cvvVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }

                    if (!formError.isNullOrEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(text = formError.orEmpty(), color = AppColors.Error, fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "This card isn't registered for Click to Pay yet. Please register to continue, or cancel to choose another payment method.",
                        fontSize = 13.sp,
                        color = AppColors.TextSecondary
                    )
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
                    val card = cardNumber
                    val expiry = expiryDateValue.text
                    val cvvValue = cvv
                    when {
                        !card.matches(Regex("^\\d{12,19}$")) -> formError = "Enter a valid card number."
                        !expiry.matches(Regex("^(0[1-9]|1[0-2])/\\d{2}$")) -> formError = "Enter a valid expiry date (MM/YY)."
                        !cvvValue.matches(Regex("^\\d{3,4}$")) -> formError = "Enter a valid CVV."
                        else -> {
                            formError = null
                            onPayNow(card, expiry, cvvValue)
                        }
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
