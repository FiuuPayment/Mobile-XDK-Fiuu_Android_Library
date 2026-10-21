package com.fiuu.xdkandroid.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiuu.xdkandroid.R
import com.fiuu.xdkandroid.models.PaymentParamsState
import com.fiuu.xdkandroid.theme.GlassTokens
import com.fiuu.xdkandroid.theme.StudioFont
import com.fiuu.xdkandroid.theme.StudioSpacing
import com.fiuu.xdkandroid.ui.components.GlassCard
import com.fiuu.xdkandroid.ui.components.ObsidianTerminalDrawer
import com.google.pay.button.ButtonType
import com.google.pay.button.PayButton
import com.fiuu.xdkandroid.googlePay.GooglePayConfig

@Composable
fun FlightDeckScreen(
    state: PaymentParamsState,
    onStartPayment: () -> Unit,
    onGooglePayClick: () -> Unit,
    onSamsungPayClick: () -> Unit,
    onCheckStatusClick: () -> Unit,
    isGooglePayReady: Boolean,
    isSamsungPayReady: Boolean
) {
    var terminalHeightDp by remember { mutableFloatStateOf(280f) }
    var showSessionInfoDialog by remember { mutableStateOf(false) }
    var showMissingCredentialsDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = StudioSpacing.standard, vertical = StudioSpacing.standard),
            verticalArrangement = Arrangement.spacedBy(StudioSpacing.standard)
        ) {
            // Active Parameters Summary Glass Card
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MERCHANT ORDER ID",
                            style = StudioFont.Badge.copy(
                                color = GlassTokens.TextMuted,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = state.orderId,
                            style = StudioFont.BodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = GlassTokens.TextPrimary
                            )
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TOTAL AMOUNT",
                            style = StudioFont.Badge.copy(
                                color = GlassTokens.TextMuted,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${state.currency} ${state.amount}",
                            style = StudioFont.TitleLarge.copy(
                                fontSize = 16.sp,
                                color = GlassTokens.PrimaryBlue
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(StudioSpacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Channel: ${state.channel}",
                        style = StudioFont.Label.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = "Payer: ${state.billName}",
                        style = StudioFont.Label.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }

            // PRIMARY ACTION: Start Full Payment Flow
            Button(
                onClick = {
                    if (!state.hasRequiredMerchantFields) {
                        showMissingCredentialsDialog = true
                    } else {
                        onStartPayment()
                    }
                },
                shape = RoundedCornerShape(StudioSpacing.buttonRadius),
                contentPadding = PaddingValues(vertical = 14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.AccentOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(StudioSpacing.buttonHeight)
                    .semantics {
                        contentDescription = "btn_start_payment_flow"
                        testTag = "btn_start_payment_flow"
                    }
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(StudioSpacing.iconLarge)
                )
                Spacer(modifier = Modifier.width(StudioSpacing.sm))
                Text(
                    text = "START PAYMENT FLOW",
                    style = StudioFont.SectionHeader.copy(color = Color.White)
                )
            }

            // 1-TAP DIGITAL WALLETS
            Text(
                text = "⚡ 1-Tap Express Digital Wallets",
                style = StudioFont.Body.copy(fontWeight = FontWeight.SemiBold, color = GlassTokens.TextSecondary)
            )

            // Google Pay Button (Official Google Pay Compose Button)
            PayButton(
                onClick = onGooglePayClick,
                allowedPaymentMethods = GooglePayConfig.allowedPaymentMethodsForButton(),
                type = ButtonType.Plain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(StudioSpacing.buttonHeight)
                    .semantics {
                        contentDescription = "btn_google_pay_express"
                        testTag = "btn_google_pay_express"
                    },
                enabled = isGooglePayReady
            )

            // Samsung Pay Button (Official Samsung Pay vector brand badge)
            if (isSamsungPayReady) {
                Button(
                    onClick = onSamsungPayClick,
                    // Pill shape, matching the Google Pay PayButton's default radius above
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassTokens.BrandSamsungPay,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(StudioSpacing.buttonHeight)
                        .semantics {
                            contentDescription = "btn_samsung_pay_express"
                            testTag = "btn_samsung_pay_express"
                        },
                    contentPadding = PaddingValues(horizontal = StudioSpacing.standard, vertical = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.samsung_pay_mark),
                            contentDescription = "Samsung Pay Mark",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(StudioSpacing.sm))
                        Text(
                            text = "Pay with Samsung Pay",
                            style = StudioFont.SectionHeader.copy(color = Color.White)
                        )
                    }
                }
            }

            // STATUS RESUMPTION ACTION & INFO
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(StudioSpacing.xs)
            ) {
                OutlinedButton(
                    onClick = onCheckStatusClick,
                    shape = RoundedCornerShape(StudioSpacing.buttonRadius),
                    modifier = Modifier
                        .weight(1f)
                        .height(StudioSpacing.buttonHeight)
                        .semantics {
                            contentDescription = "btn_resume_session"
                            testTag = "btn_resume_session"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = GlassTokens.PrimaryBlue,
                        modifier = Modifier.size(StudioSpacing.iconMedium)
                    )
                    Spacer(modifier = Modifier.width(StudioSpacing.sm))
                    Text(
                        text = "Check Last Session Status",
                        style = StudioFont.Body.copy(color = GlassTokens.PrimaryBlue, fontWeight = FontWeight.SemiBold)
                    )
                }

                IconButton(
                    onClick = { showSessionInfoDialog = true },
                    modifier = Modifier.size(StudioSpacing.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Session Status Info",
                        tint = GlassTokens.TextMuted,
                        modifier = Modifier.size(StudioSpacing.iconStandard)
                    )
                }
            }

            // Dynamic clearance strictly matching the active terminal drawer height
            Spacer(modifier = Modifier.height((terminalHeightDp + 20f).dp))
        }

        // Interrupted Session Recovery Explanation Dialog
        if (showSessionInfoDialog) {
            AlertDialog(
                onDismissRequest = { showSessionInfoDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = GlassTokens.PrimaryBlue,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = "Interrupted Session Recovery",
                        style = StudioFont.TitleLarge.copy(fontSize = 16.sp)
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.sm)) {
                        Text(
                            text = "What is this?",
                            style = StudioFont.Body.copy(fontWeight = FontWeight.SemiBold, color = GlassTokens.PrimaryBlue)
                        )
                        Text(
                            text = "When a transaction starts, the SDK encrypts and saves the session token in the hardware KeyStore with a 15-minute TTL.",
                            style = StudioFont.Label.copy(color = GlassTokens.TextSecondary, lineHeight = 16.sp)
                        )
                        Text(
                            text = "When is it used?",
                            style = StudioFont.Body.copy(fontWeight = FontWeight.SemiBold, color = GlassTokens.PrimaryBlue)
                        )
                        Text(
                            text = "If the app is killed by the OS (due to low memory) or closed by the user while authenticating in an external bank 3DS or FPX page, calling resumeFiuuPaymentStatus() upon re-launch checks the final status via /inquire and clears the session.",
                            style = StudioFont.Label.copy(color = GlassTokens.TextSecondary, lineHeight = 16.sp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSessionInfoDialog = false }) {
                        Text("Understood", style = StudioFont.BodyLarge.copy(color = GlassTokens.PrimaryBlue, fontWeight = FontWeight.Bold))
                    }
                }
            )
        }

        // Missing Credentials Pre-Flight Validation Dialog
        if (showMissingCredentialsDialog) {
            AlertDialog(
                onDismissRequest = { showMissingCredentialsDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = GlassTokens.StatusWarning,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = "Merchant Credentials Required",
                        style = StudioFont.TitleLarge.copy(fontSize = 16.sp)
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.sm)) {
                        Text(
                            text = "Mandatory merchant parameters are missing:",
                            style = StudioFont.Body.copy(fontWeight = FontWeight.SemiBold, color = GlassTokens.TextPrimary)
                        )
                        Text(
                            text = "To start a payment flow, please fill in Merchant ID, App Name, Verification Key, Username, and Password under the Merchant tab, or click below to auto-load the Sandbox preset.",
                            style = StudioFont.Label.copy(color = GlassTokens.TextSecondary, lineHeight = 16.sp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            state.applyPreset(com.fiuu.xdkandroid.models.EnvironmentPreset.SANDBOX)
                            showMissingCredentialsDialog = false
                            onStartPayment()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlassTokens.PrimaryBlue,
                            contentColor = GlassTokens.OnPrimary
                        )
                    ) {
                        Text(
                            text = "Load Sandbox & Start",
                            style = StudioFont.Body.copy(color = GlassTokens.OnPrimary, fontWeight = FontWeight.Bold)
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMissingCredentialsDialog = false }) {
                        Text("Dismiss", style = StudioFont.Body.copy(color = GlassTokens.TextMuted))
                    }
                }
            )
        }

        // Resizable Obsidian Terminal Drawer (Pinned to bottom of Flight Deck)
        ObsidianTerminalDrawer(
            modifier = Modifier.align(Alignment.BottomCenter),
            initialHeightDp = terminalHeightDp,
            onHeightChanged = { terminalHeightDp = it }
        )
    }
}
