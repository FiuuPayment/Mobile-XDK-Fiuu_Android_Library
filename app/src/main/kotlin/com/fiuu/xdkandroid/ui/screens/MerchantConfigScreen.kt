package com.fiuu.xdkandroid.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiuu.xdkandroid.logger.LogLevel
import com.fiuu.xdkandroid.logger.TerminalLogManager
import com.fiuu.xdkandroid.models.PaymentEngine
import com.fiuu.xdkandroid.models.PaymentParamsState
import com.fiuu.xdkandroid.theme.GlassTokens
import com.fiuu.xdkandroid.theme.StudioFont
import com.fiuu.xdkandroid.theme.StudioSpacing
import com.fiuu.xdkandroid.ui.components.GlassCard
import com.fiuu.xdkandroid.ui.components.StudioSectionHeader

@Composable
fun MerchantConfigScreen(
    state: PaymentParamsState
) {
    var showValidationErrors by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = StudioSpacing.standard, vertical = StudioSpacing.standard),
        verticalArrangement = Arrangement.spacedBy(StudioSpacing.standard)
    ) {
        GlassCard {
            StudioSectionHeader(
                title = "Payment Engine",
                icon = Icons.Rounded.Payment,
                subtitle = "mp_core_env: 1 = native wrapper, 2 = git2u Android XDK Webcore"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm)
            ) {
                PaymentEngine.entries.forEach { engine ->
                    val selected = state.selectedEngine == engine
                    OutlinedButton(
                        onClick = {
                            state.selectedEngine = engine
                            TerminalLogManager.log(
                                LogLevel.USER,
                                "ENGINE",
                                "Selected ${engine.label} (mp_core_env=${engine.coreEnv})"
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) GlassTokens.PrimaryBlue else Color.Transparent,
                            contentColor = if (selected) GlassTokens.OnPrimary else GlassTokens.TextPrimary
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selected) GlassTokens.PrimaryBlue else GlassTokens.GlassBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .semantics {
                                contentDescription = "btn_engine_${engine.name.lowercase()}"
                                testTag = "btn_engine_${engine.name.lowercase()}"
                            }
                    ) {
                        Text(
                            text = if (engine == PaymentEngine.NATIVE) "Native" else "Webcore",
                            style = StudioFont.Label.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }

        // --- Environment Presets (Sandbox, UAT, Production) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm)
        ) {
            // Preset Button 1: Sandbox
            val isSandbox = state.selectedEnvironment == com.fiuu.xdkandroid.models.EnvironmentPreset.SANDBOX
            OutlinedButton(
                onClick = {
                    state.selectEnvironment(com.fiuu.xdkandroid.models.EnvironmentPreset.SANDBOX)
                    TerminalLogManager.log(LogLevel.USER, "PRESET", "Loaded Sandbox Preset (mp_build_mode=${state.buildMode})")
                },
                shape = RoundedCornerShape(StudioSpacing.chipRadius),
                border = BorderStroke(
                    1.dp,
                    if (isSandbox) GlassTokens.PrimaryBlue else GlassTokens.GlassBorder
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSandbox) GlassTokens.PrimaryBlue else GlassTokens.StatusNeutralBg,
                    contentColor = if (isSandbox) GlassTokens.OnPrimary else GlassTokens.TextPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .semantics {
                        contentDescription = "btn_preset_sandbox"
                        testTag = "btn_preset_sandbox"
                    }
            ) {
                Text(
                    text = "Sandbox",
                    style = StudioFont.Label.copy(
                        color = if (isSandbox) GlassTokens.OnPrimary else GlassTokens.TextPrimary,
                        fontWeight = if (isSandbox) FontWeight.Bold else FontWeight.Medium
                    )
                )
            }

            // Preset Button 2: UAT
            val isUat = state.selectedEnvironment == com.fiuu.xdkandroid.models.EnvironmentPreset.UAT
            OutlinedButton(
                onClick = {
                    state.selectEnvironment(com.fiuu.xdkandroid.models.EnvironmentPreset.UAT)
                    TerminalLogManager.log(LogLevel.USER, "PRESET", "Loaded UAT Preset (mp_build_mode=${state.buildMode})")
                },
                shape = RoundedCornerShape(StudioSpacing.chipRadius),
                border = BorderStroke(
                    1.dp,
                    if (isUat) GlassTokens.PrimaryBlue else GlassTokens.GlassBorder
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isUat) GlassTokens.PrimaryBlue else GlassTokens.StatusNeutralBg,
                    contentColor = if (isUat) GlassTokens.OnPrimary else GlassTokens.TextPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .semantics {
                        contentDescription = "btn_preset_uat"
                        testTag = "btn_preset_uat"
                    }
            ) {
                Text(
                    text = "UAT",
                    style = StudioFont.Label.copy(
                        color = if (isUat) GlassTokens.OnPrimary else GlassTokens.TextPrimary,
                        fontWeight = if (isUat) FontWeight.Bold else FontWeight.Medium
                    )
                )
            }

            // Preset Button 3: Production
            val isProd = state.selectedEnvironment == com.fiuu.xdkandroid.models.EnvironmentPreset.PRODUCTION
            OutlinedButton(
                onClick = {
                    state.selectEnvironment(com.fiuu.xdkandroid.models.EnvironmentPreset.PRODUCTION)
                    TerminalLogManager.log(LogLevel.USER, "PRESET", "Loaded Production Preset (mp_build_mode=${state.buildMode})")
                },
                shape = RoundedCornerShape(StudioSpacing.chipRadius),
                border = BorderStroke(
                    1.dp,
                    if (isProd) GlassTokens.PrimaryBlue else GlassTokens.GlassBorder
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isProd) GlassTokens.PrimaryBlue else GlassTokens.StatusNeutralBg,
                    contentColor = if (isProd) GlassTokens.OnPrimary else GlassTokens.TextPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .semantics {
                        contentDescription = "btn_preset_prod"
                        testTag = "btn_preset_prod"
                    }
            ) {
                Text(
                    text = "Prod",
                    style = StudioFont.Label.copy(
                        color = if (isProd) GlassTokens.OnPrimary else GlassTokens.TextPrimary,
                        fontWeight = if (isProd) FontWeight.Bold else FontWeight.Medium
                    )
                )
            }
        }

        // --- Active Gateway & Endpoint Live Telemetry Card ---
        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = null,
                    tint = GlassTokens.PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = state.engineName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(StudioSpacing.badgeRadius),
                    color = GlassTokens.PrimaryBlue.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, GlassTokens.PrimaryBlue.copy(alpha = 0.30f))
                ) {
                    Text(
                        text = "mp_core_env: ${state.selectedEngine.coreEnv}",
                        style = StudioFont.Badge.copy(
                            color = GlassTokens.PrimaryBlue,
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.padding(horizontal = StudioSpacing.sm, vertical = StudioSpacing.xs)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(StudioSpacing.badgeRadius),
                    color = GlassTokens.PrimaryBlue.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, GlassTokens.PrimaryBlue.copy(alpha = 0.30f))
                ) {
                    Text(
                        text = "mp_build_mode: ${state.buildMode}",
                        style = StudioFont.Badge.copy(
                            color = GlassTokens.PrimaryBlue,
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.padding(horizontal = StudioSpacing.sm, vertical = StudioSpacing.xs)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(StudioSpacing.badgeRadius),
                    color = if (state.sandboxMode) GlassTokens.StatusWarningBg else GlassTokens.StatusSuccessBg,
                    border = BorderStroke(
                        1.dp,
                        if (state.sandboxMode) GlassTokens.StatusWarningBorder else GlassTokens.StatusSuccessBorder
                    )
                ) {
                    Text(
                        text = "mp_sandbox_mode: ${state.sandboxMode}",
                        style = StudioFont.Badge.copy(
                            color = if (state.sandboxMode) GlassTokens.StatusWarningText else GlassTokens.StatusSuccessText,
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.padding(horizontal = StudioSpacing.sm, vertical = StudioSpacing.xs)
                    )
                }
            }
        }

        // --- Merchant Credentials Card ---
        GlassCard {
            StudioSectionHeader(
                title = "Merchant Credentials",
                icon = Icons.Rounded.Key,
                subtitle = "Gateway Auth & Parameter Configuration"
            )

            OutlinedTextField(
                value = state.merchantId,
                onValueChange = { state.merchantId = it },
                label = {
                    Text(
                        buildAnnotatedString {
                            append("mp_merchant_ID")
                            withStyle(SpanStyle(color = GlassTokens.StatusErrorText)) { append(" *") }
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Badge,
                        contentDescription = null,
                        tint = GlassTokens.PrimaryBlue,
                        modifier = Modifier.size(StudioSpacing.iconMedium)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                isError = showValidationErrors && state.merchantId.isBlank(),
                supportingText = if (showValidationErrors && state.merchantId.isBlank()) {
                    { Text("Required", color = GlassTokens.StatusErrorText) }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(StudioSpacing.md))

            OutlinedTextField(
                value = state.appName,
                onValueChange = { state.appName = it },
                label = {
                    Text(
                        buildAnnotatedString {
                            append("mp_app_name")
                            withStyle(SpanStyle(color = GlassTokens.StatusErrorText)) { append(" *") }
                        }
                    )
                },
                placeholder = { Text("mobile") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Smartphone,
                        contentDescription = null,
                        tint = GlassTokens.PrimaryBlue,
                        modifier = Modifier.size(StudioSpacing.iconMedium)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                isError = showValidationErrors && state.appName.isBlank(),
                supportingText = if (showValidationErrors && state.appName.isBlank()) {
                    { Text("Required", color = GlassTokens.StatusErrorText) }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(StudioSpacing.md))

            OutlinedTextField(
                value = state.gpMerchantId,
                onValueChange = { state.gpMerchantId = it },
                label = { Text("mp_gp_merchant_ID") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Payment,
                        contentDescription = null,
                        tint = GlassTokens.PrimaryBlue,
                        modifier = Modifier.size(StudioSpacing.iconMedium)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(StudioSpacing.md))

            OutlinedTextField(
                value = state.verificationKey,
                onValueChange = { state.verificationKey = it },
                label = {
                    Text(
                        buildAnnotatedString {
                            append("mp_verification_key")
                            withStyle(SpanStyle(color = GlassTokens.StatusErrorText)) { append(" *") }
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Key,
                        contentDescription = null,
                        tint = GlassTokens.PrimaryBlue,
                        modifier = Modifier.size(StudioSpacing.iconMedium)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                isError = showValidationErrors && state.verificationKey.isBlank(),
                supportingText = if (showValidationErrors && state.verificationKey.isBlank()) {
                    { Text("Required", color = GlassTokens.StatusErrorText) }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(StudioSpacing.md))

            OutlinedTextField(
                value = state.username,
                onValueChange = { state.username = it },
                label = {
                    Text(
                        buildAnnotatedString {
                            append("mp_username")
                            withStyle(SpanStyle(color = GlassTokens.StatusErrorText)) { append(" *") }
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.AccountCircle,
                        contentDescription = null,
                        tint = GlassTokens.TextMuted,
                        modifier = Modifier.size(StudioSpacing.iconMedium)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                isError = showValidationErrors && state.username.isBlank(),
                supportingText = if (showValidationErrors && state.username.isBlank()) {
                    { Text("Required", color = GlassTokens.StatusErrorText) }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(StudioSpacing.md))

            OutlinedTextField(
                value = state.password,
                onValueChange = { state.password = it },
                label = {
                    Text(
                        buildAnnotatedString {
                            append("mp_password")
                            withStyle(SpanStyle(color = GlassTokens.StatusErrorText)) { append(" *") }
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = GlassTokens.TextMuted,
                        modifier = Modifier.size(StudioSpacing.iconMedium)
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                isError = showValidationErrors && state.password.isBlank(),
                supportingText = if (showValidationErrors && state.password.isBlank()) {
                    { Text("Required", color = GlassTokens.StatusErrorText) }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(StudioSpacing.standard))

            // Dev Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Developer Verbose Logging", style = StudioFont.BodyLarge)
                    Text(text = "Emits low-level HTTP & crypto packet logs", style = StudioFont.Caption)
                }
                Switch(
                    checked = state.devMode,
                    onCheckedChange = { state.devMode = it },
                    colors = GlassTokens.switchColors()
                )
            }
        }

        // Clean bottom padding
        Spacer(modifier = Modifier.height(StudioSpacing.standard))
    }
}
