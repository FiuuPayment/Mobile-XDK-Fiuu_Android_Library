package com.fiuu.xdkandroid.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiuu.xdkandroid.data.remote.dto.PaymentResponseDto
import com.fiuu.xdkandroid.diagnostics.DiagnosticChaosEngine
import com.fiuu.xdkandroid.diagnostics.SimulatedNetworkMode
import com.fiuu.xdkandroid.logger.LogLevel
import com.fiuu.xdkandroid.logger.TerminalLogManager
import com.fiuu.xdkandroid.platform.AndroidSecureTokenStore
import com.fiuu.xdkandroid.platform.resumeFiuuPaymentStatus
import com.fiuu.xdkandroid.theme.GlassTokens
import com.fiuu.xdkandroid.theme.StudioFont
import com.fiuu.xdkandroid.theme.StudioSpacing
import com.fiuu.xdkandroid.ui.components.GlassCard
import com.fiuu.xdkandroid.ui.components.StudioSectionHeader
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private val prettyJson = Json { prettyPrint = true }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiagnosticStudioScreen(
    onTriggerPaymentFlow: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val chaosState by DiagnosticChaosEngine.state.collectAsState()

    var customDeepLinkText by remember {
        mutableStateOf("superapp://payment-result?status=00&tranID=TXN_TEST_999&amount=1.15&currency=MYR")
    }

    val secureTokenStore = remember { AndroidSecureTokenStore(context) }
    var tokenStatusText by remember { mutableStateOf<String?>("Tap to inspect") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = StudioSpacing.standard, vertical = StudioSpacing.standard),
        verticalArrangement = Arrangement.spacedBy(StudioSpacing.standard)
    ) {
        // Telemetry Overview Glass Card
        GlassCard {
            StudioSectionHeader(
                title = "Diagnostic & Chaos Studio",
                subtitle = "Deterministic edge-case simulation",
                icon = Icons.Rounded.BugReport,
                iconTint = GlassTokens.AccentOrange,
                iconBgColor = GlassTokens.AccentOrange.copy(alpha = 0.12f),
                trailingAction = {
                    OutlinedButton(
                        onClick = {
                            DiagnosticChaosEngine.resetToDefaults()
                            TerminalLogManager.log(LogLevel.STATUS, "CHAOS_RESET", "All diagnostic chaos settings reset to normal")
                            Toast.makeText(context, "Diagnostics reset to normal", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(StudioSpacing.chipRadius),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = GlassTokens.PrimaryBlue,
                            modifier = Modifier.size(StudioSpacing.iconSmall)
                        )
                        Spacer(modifier = Modifier.width(StudioSpacing.xs))
                        Text(
                            text = "Reset",
                            style = StudioFont.Caption.copy(color = GlassTokens.PrimaryBlue, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            )

            // Active Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm)
            ) {
                // Network Badge
                val isNormalNet = chaosState.networkMode == SimulatedNetworkMode.NORMAL
                Card(
                    shape = RoundedCornerShape(StudioSpacing.chipRadius),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isNormalNet) GlassTokens.StatusSuccessBg else GlassTokens.StatusErrorBg
                    ),
                    border = BorderStroke(1.dp, if (isNormalNet) GlassTokens.StatusSuccessBorder else GlassTokens.StatusErrorBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(StudioSpacing.sm)) {
                        Text(
                            text = "NETWORK",
                            style = StudioFont.Badge.copy(color = GlassTokens.TextMuted)
                        )
                        Spacer(modifier = Modifier.height(StudioSpacing.xxs))
                        Text(
                            text = chaosState.networkMode.displayName,
                            style = StudioFont.Caption.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isNormalNet) GlassTokens.StatusSuccessText else GlassTokens.StatusErrorText
                            )
                        )
                    }
                }

                // Root Badge
                val isMockRoot = chaosState.isMockRootActive
                Card(
                    shape = RoundedCornerShape(StudioSpacing.chipRadius),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!isMockRoot) GlassTokens.StatusSuccessBg else GlassTokens.StatusErrorBg
                    ),
                    border = BorderStroke(1.dp, if (!isMockRoot) GlassTokens.StatusSuccessBorder else GlassTokens.StatusErrorBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(StudioSpacing.sm)) {
                        Text(
                            text = "ROOT DEFENSE",
                            style = StudioFont.Badge.copy(color = GlassTokens.TextMuted)
                        )
                        Spacer(modifier = Modifier.height(StudioSpacing.xxs))
                        Text(
                            text = if (isMockRoot) "MOCK ACTIVE" else "CLEAN",
                            style = StudioFont.Caption.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (!isMockRoot) GlassTokens.StatusSuccessText else GlassTokens.StatusErrorText
                            )
                        )
                    }
                }
            }
        }

        // Section 1: Network Chaos Simulator
        GlassCard {
            StudioSectionHeader(
                title = "Network Latency & Fault Injector",
                subtitle = "In-process simulation without killing USB/ADB",
                icon = Icons.Rounded.NetworkCheck,
                iconTint = GlassTokens.PrimaryBlue
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(StudioSpacing.sm)
            ) {
                SimulatedNetworkMode.entries.forEach { mode ->
                    val isSelected = chaosState.networkMode == mode
                    Card(
                        shape = RoundedCornerShape(StudioSpacing.chipRadius),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) GlassTokens.PrimaryBlue else GlassTokens.StatusNeutralBg
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) GlassTokens.PrimaryBlue else GlassTokens.StatusNeutralBorder
                        ),
                        modifier = Modifier
                            .clickable {
                                DiagnosticChaosEngine.networkMode = mode
                                TerminalLogManager.log(
                                    LogLevel.HTTP,
                                    "NETWORK_CHAOS",
                                    "Network mode changed to: ${mode.displayName}",
                                    mode.description
                                )
                            }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = StudioSpacing.md, vertical = StudioSpacing.sm)) {
                            Text(
                                text = mode.displayName,
                                style = StudioFont.Label.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) GlassTokens.OnPrimary else GlassTokens.TextPrimary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(StudioSpacing.sm))
            Text(
                text = "Active description: ${chaosState.networkMode.description}",
                style = StudioFont.Code.copy(fontSize = 11.sp, color = GlassTokens.TextSecondary)
            )
        }

        // Section 2: App-to-App & E-Wallet Interruption Simulator
        GlassCard {
            StudioSectionHeader(
                title = "App-to-App & Deep Link Simulator",
                subtitle = "Test 3rd party e-wallet returns and callback intent handling",
                icon = Icons.AutoMirrored.Rounded.Send,
                iconTint = GlassTokens.PrimaryBlue
            )

            Text(
                text = "Quick Simulated Callback Dispatchers:",
                style = StudioFont.Label.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(StudioSpacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm)
            ) {
                Button(
                    onClick = {
                        val uri = "superapp://payment-result?status=00&tranID=TXN_SUCCESS_${System.currentTimeMillis().toString().takeLast(4)}&amount=1.15&currency=MYR"
                        dispatchDeepLink(context, uri)
                    },
                    shape = RoundedCornerShape(StudioSpacing.chipRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassTokens.StatusSuccess,
                        contentColor = GlassTokens.StatusSuccessOnLime
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Success (00)",
                        style = StudioFont.Label.copy(
                            color = GlassTokens.StatusSuccessOnLime,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Button(
                    onClick = {
                        val uri = "superapp://payment-result?status=11&tranID=TXN_FAIL_${System.currentTimeMillis().toString().takeLast(4)}&error_code=BANK_DECLINE"
                        dispatchDeepLink(context, uri)
                    },
                    shape = RoundedCornerShape(StudioSpacing.chipRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.StatusError),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Failed (11)",
                        style = StudioFont.Label.copy(color = Color.White, fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = {
                        val uri = "superapp://payment-result?status=USER_CANCELLED"
                        dispatchDeepLink(context, uri)
                    },
                    shape = RoundedCornerShape(StudioSpacing.chipRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassTokens.StatusWarning,
                        contentColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Cancel",
                        style = StudioFont.Label.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(StudioSpacing.md))

            // Custom Deep Link Input
            OutlinedTextField(
                value = customDeepLinkText,
                onValueChange = { customDeepLinkText = it },
                label = { Text("Custom Callback URI Scheme", style = StudioFont.Label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(StudioSpacing.sm))

            Button(
                onClick = { dispatchDeepLink(context, customDeepLinkText) },
                shape = RoundedCornerShape(StudioSpacing.buttonRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlassTokens.PrimaryBlue,
                    contentColor = GlassTokens.OnPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Dispatch Custom Deep Link Intent",
                    style = StudioFont.BodyLarge.copy(color = GlassTokens.OnPrimary, fontWeight = FontWeight.Bold)
                )
            }

            if (chaosState.lastDeepLinkReceived != null) {
                Spacer(modifier = Modifier.height(StudioSpacing.sm))
                Text(
                    text = "Last intercepted: ${chaosState.lastDeepLinkReceived}",
                    style = StudioFont.Code.copy(fontSize = 11.sp, color = GlassTokens.TextMuted)
                )
            }

            Spacer(modifier = Modifier.height(StudioSpacing.standard))

            // E-Wallet intent:// URI Verification Vectors (TC-EWL-02)
            Text(
                text = "E-Wallet intent:// URI Vectors (TC-EWL-02):",
                style = StudioFont.Label.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Tests Intent.parseUri, BROWSABLE isolation, package targeting, and web fallback",
                style = StudioFont.Caption.copy(color = GlassTokens.TextMuted)
            )
            Spacer(modifier = Modifier.height(StudioSpacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm)
            ) {
                OutlinedButton(
                    onClick = {
                        val uri = "intent:#Intent;action=android.intent.action.VIEW;package=${context.packageName};category=android.intent.category.BROWSABLE;end"
                        testIntentScheme(context, uri)
                    },
                    shape = RoundedCornerShape(StudioSpacing.chipRadius),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "App Present",
                        style = StudioFont.Label.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = {
                        val uri = "intent://pay#Intent;scheme=fakeeWallet;package=com.fake.wallet;S.browser_fallback_url=https%3A%2F%2Fxdk.fiuu.com%2Ffallback;end"
                        testIntentScheme(context, uri)
                    },
                    shape = RoundedCornerShape(StudioSpacing.chipRadius),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Web Fallback",
                        style = StudioFont.Label.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = {
                        val uri = "intent://pay#Intent;scheme=fakeeWallet;package=com.fake.wallet;end"
                        testIntentScheme(context, uri)
                    },
                    shape = RoundedCornerShape(StudioSpacing.chipRadius),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Play Store",
                        style = StudioFont.Label.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // Section 3: Security, Tamper & Edge Cases
        GlassCard {
            StudioSectionHeader(
                title = "Security, Tamper & Edge Cases",
                subtitle = "Test security alarms and edge-case crash defenses",
                icon = Icons.Rounded.Shield,
                iconTint = GlassTokens.AccentOrange,
                iconBgColor = GlassTokens.AccentOrange.copy(alpha = 0.12f)
            )

            // Root Defense Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Simulate Rooted Device (`isDeviceRooted = true`)",
                        style = StudioFont.BodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Forces SDK to trigger device security block alert",
                        style = StudioFont.Caption
                    )
                }
                Switch(
                    checked = chaosState.isMockRootActive,
                    onCheckedChange = {
                        DiagnosticChaosEngine.isMockRootActive = it
                        TerminalLogManager.log(LogLevel.STATUS, "MOCK_ROOT", "Mock root active set to: $it")
                    },
                    colors = GlassTokens.switchColors(checkedTrackColor = GlassTokens.StatusError)
                )
            }

            Spacer(modifier = Modifier.height(StudioSpacing.standard))

            // Force WebView Error Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Force WebView Load Failure",
                        style = StudioFont.BodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Tests immediate handleWebViewFailure teardown",
                        style = StudioFont.Caption
                    )
                }
                Switch(
                    checked = chaosState.forceWebViewLoadFail,
                    onCheckedChange = {
                        DiagnosticChaosEngine.forceWebViewLoadFail = it
                        TerminalLogManager.log(LogLevel.STATUS, "WEBVIEW_FAIL", "Force webview load failure set to: $it")
                    },
                    colors = GlassTokens.switchColors(checkedTrackColor = GlassTokens.StatusError)
                )
            }

            Spacer(modifier = Modifier.height(StudioSpacing.standard))

            // Double Tap Stress Button
            OutlinedButton(
                onClick = {
                    TerminalLogManager.log(LogLevel.USER, "STRESS_TEST", "Triggering rapid 5-tap concurrency stress test...")
                    repeat(5) {
                        onTriggerPaymentFlow()
                    }
                },
                shape = RoundedCornerShape(StudioSpacing.buttonRadius),
                border = BorderStroke(1.dp, GlassTokens.StatusNeutralBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = GlassTokens.StatusNeutralBg,
                    contentColor = GlassTokens.TextPrimary
                ),
                modifier = Modifier.fillMaxWidth().height(StudioSpacing.buttonHeight)
            ) {
                Text(
                    text = "Simulate 5x Rapid Double-Tap Stress",
                    style = StudioFont.BodyLarge.copy(color = GlassTokens.TextPrimary, fontWeight = FontWeight.Bold)
                )
            }
        }

        // Section 4: Hardware KeyStore Session Inspector
        GlassCard {
            StudioSectionHeader(
                title = "Hardware KeyStore Session Inspector",
                subtitle = "Encrypted hardware token store tracking (15-min TTL)",
                icon = Icons.Rounded.Lock,
                iconTint = GlassTokens.PrimaryBlue
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stored Session Status:",
                    style = StudioFont.Label.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = tokenStatusText ?: "Empty",
                    style = StudioFont.Code.copy(fontWeight = FontWeight.Bold, color = GlassTokens.PrimaryBlue)
                )
            }

            Spacer(modifier = Modifier.height(StudioSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm)
            ) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            val session = secureTokenStore.get()
                            tokenStatusText = if (session != null) "Active (Order: ${session.orderId})" else "None (Empty)"
                            TerminalLogManager.log(
                                LogLevel.KEYSTORE,
                                "KEYSTORE_INSPECT",
                                "KeyStore token probe: $tokenStatusText"
                            )
                        }
                    },
                    shape = RoundedCornerShape(StudioSpacing.chipRadius),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Probe Token", style = StudioFont.Label)
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            secureTokenStore.clear()
                            tokenStatusText = "Cleared"
                            TerminalLogManager.log(LogLevel.KEYSTORE, "KEYSTORE_PURGE", "Hardware session token cleared manually")
                            Toast.makeText(context, "Session token purged", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(StudioSpacing.chipRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.StatusError),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Purge Token", style = StudioFont.Label.copy(color = Color.White))
                }
            }
        }

        // Section 5: In-Library Telemetry & CloudWatch
        GlassCard {
            StudioSectionHeader(
                title = "In-Library Telemetry & CloudWatch",
                subtitle = "Test ping & anomaly-only WAL verification",
                icon = Icons.Rounded.NetworkCheck,
                iconTint = GlassTokens.PrimaryBlue
            )

            Spacer(modifier = Modifier.height(StudioSpacing.sm))

            Text(
                text = "Approved checkouts suppress remote logs to prevent noise. Click below to dispatch an authentic test ping signed with SHA-512 to verify AWS CloudWatch Logs ingestion.",
                style = StudioFont.Caption.copy(color = GlassTokens.TextMuted)
            )

            Spacer(modifier = Modifier.height(StudioSpacing.standard))

            Button(
                onClick = {
                    com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.testPing(
                        merchantId = "test_merchant_id"
                    )
                    TerminalLogManager.log(
                        level = LogLevel.HTTP,
                        tag = "TELEMETRY",
                        message = "Dispatched diagnostic test ping to Mobile Gateway (AWS CloudWatch)",
                        details = "Process: diagnostic_ping (signed with MobileGatewayChecksum)"
                    )
                    Toast.makeText(context, "Telemetry ping sent to CloudWatch", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(StudioSpacing.buttonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlassTokens.PrimaryBlue,
                    contentColor = GlassTokens.OnPrimary
                ),
                shape = RoundedCornerShape(StudioSpacing.buttonRadius)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = null,
                    modifier = Modifier.size(StudioSpacing.iconSmall),
                    tint = GlassTokens.OnPrimary
                )
                Spacer(modifier = Modifier.width(StudioSpacing.xs))
                Text(
                    text = "Send Telemetry Ping to CloudWatch",
                    style = StudioFont.BodyLarge.copy(color = GlassTokens.OnPrimary, fontWeight = FontWeight.Bold)
                )
            }
        }

        // Dynamic clearance for the bottom navigation bar
        Spacer(modifier = Modifier.height(StudioSpacing.pillHeight + 20.dp))
    }
}

private fun dispatchDeepLink(context: Context, urlString: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
        TerminalLogManager.log(
            LogLevel.STATUS,
            "DEEPLINK_DISPATCH",
            "Dispatched deep link intent: $urlString"
        )
    } catch (e: Exception) {
        TerminalLogManager.log(
            LogLevel.ERROR,
            "DEEPLINK_ERROR",
            "Failed to dispatch deep link: ${e.message}"
        )
        Toast.makeText(context, "Dispatch error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun testIntentScheme(context: Context, intentUrl: String) {
    TerminalLogManager.log(
        LogLevel.HTTP,
        "INTENT_TEST",
        "Testing intent:// URI handling: $intentUrl"
    )
    val parsedIntent = try {
        Intent.parseUri(intentUrl, Intent.URI_INTENT_SCHEME).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            component = null
            selector = null
        }
    } catch (e: Exception) {
        TerminalLogManager.log(LogLevel.ERROR, "INTENT_ERROR", "Malformed intent URI: ${e.message}")
        Toast.makeText(context, "Malformed intent URI", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        parsedIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(parsedIntent)
        TerminalLogManager.log(
            LogLevel.STATUS,
            "INTENT_LAUNCHED",
            "Target app launched successfully: package=${parsedIntent.`package`}, action=${parsedIntent.action}"
        )
        Toast.makeText(context, "App launched from intent://", Toast.LENGTH_SHORT).show()
    } catch (e: android.content.ActivityNotFoundException) {
        val fallbackUrl = parsedIntent.getStringExtra("browser_fallback_url")
            ?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
        if (fallbackUrl != null) {
            TerminalLogManager.log(
                LogLevel.STATUS,
                "INTENT_FALLBACK",
                "App not installed. Resolved browser_fallback_url: $fallbackUrl"
            )
            Toast.makeText(context, "Fallback URL: $fallbackUrl", Toast.LENGTH_SHORT).show()
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (ignored: Exception) {}
            return
        }

        val packageName = parsedIntent.`package`
        if (packageName != null) {
            TerminalLogManager.log(
                LogLevel.STATUS,
                "INTENT_STORE",
                "App not installed. Resolved Play Store listing: market://details?id=$packageName"
            )
            Toast.makeText(context, "Play Store target: $packageName", Toast.LENGTH_SHORT).show()
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (ignored: Exception) {}
            return
        }

        TerminalLogManager.log(
            LogLevel.ERROR,
            "INTENT_NOT_HANDLED",
            "No app or fallback URL available for intent"
        )
        Toast.makeText(context, "The app needed for this payment isn't installed", Toast.LENGTH_SHORT).show()
    }
}
