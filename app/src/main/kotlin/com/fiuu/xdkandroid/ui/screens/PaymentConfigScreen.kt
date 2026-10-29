package com.fiuu.xdkandroid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import com.fiuu.xdkandroid.logger.LogLevel
import com.fiuu.xdkandroid.logger.TerminalLogManager
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiuu.xdkandroid.models.PaymentParamsState
import com.fiuu.xdkandroid.theme.GlassTokens
import com.fiuu.xdkandroid.theme.StudioFont
import com.fiuu.xdkandroid.theme.StudioSpacing
import com.fiuu.xdkandroid.ui.components.GlassCard
import com.fiuu.xdkandroid.ui.components.StudioSectionHeader

private val CHANNEL_OPTIONS = listOf("multi", "Alipay", "ShopeePay")
private val COUNTRY_OPTIONS = listOf("MY", "SG")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentConfigScreen(
    state: PaymentParamsState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = StudioSpacing.standard, vertical = StudioSpacing.standard),
        verticalArrangement = Arrangement.spacedBy(StudioSpacing.standard)
    ) {
        // Section 1: Payment & Order Parameters
        GlassCard {
            StudioSectionHeader(
                title = "Order & Channel Parameters",
                icon = Icons.Rounded.Tune,
                subtitle = "Amount, Currency, Channel & Execution"
            )

            // Amount Input
            OutlinedTextField(
                value = state.amount,
                onValueChange = { state.amount = it },
                label = { Text("Transaction Amount") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.AttachMoney,
                        contentDescription = null,
                        tint = GlassTokens.PrimaryBlue,
                        modifier = Modifier.size(StudioSpacing.iconMedium)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(StudioSpacing.md))

            // Order ID + Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.orderId,
                    onValueChange = { state.orderId = it },
                    label = { Text("Merchant Order ID") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Tag,
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
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(StudioSpacing.sm))
                IconButton(
                    onClick = { state.refreshOrderId() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Generate New Order ID",
                        tint = GlassTokens.PrimaryBlue,
                        modifier = Modifier.size(StudioSpacing.iconLarge)
                    )
                }
            }

            Spacer(modifier = Modifier.height(StudioSpacing.md))

            // Country & Currency
            Row(modifier = Modifier.fillMaxWidth()) {
                var countryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = countryExpanded,
                    onExpandedChange = { countryExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = state.country,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Country") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Public,
                                contentDescription = null,
                                tint = GlassTokens.TextMuted,
                                modifier = Modifier.size(StudioSpacing.iconMedium)
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = countryExpanded,
                        onDismissRequest = { countryExpanded = false }
                    ) {
                        COUNTRY_OPTIONS.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = {
                                    state.onCountryChange(c)
                                    countryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(StudioSpacing.md))

                OutlinedTextField(
                    value = state.currency,
                    onValueChange = { state.currency = it },
                    label = { Text("Currency") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(StudioSpacing.md))

            // Channel Selector (Presets + Dedicated Custom Text Input Option — iOS Parity)
            val isCustomChannelInitial = state.channel !in CHANNEL_OPTIONS
            var isCustomChannel by remember(state.channel) { mutableStateOf(isCustomChannelInitial) }
            var channelExpanded by remember { mutableStateOf(false) }

            Column(modifier = Modifier.fillMaxWidth()) {
                ExposedDropdownMenuBox(
                    expanded = channelExpanded,
                    onExpandedChange = { channelExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (isCustomChannel) {
                            if (state.channel.isBlank()) "Custom Channel (Enter below)" else "Custom: ${state.channel}"
                        } else {
                            state.channel
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Channel") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isCustomChannel) Icons.Rounded.Edit else Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = GlassTokens.PrimaryBlue,
                                modifier = Modifier.size(StudioSpacing.iconMedium)
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = channelExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = channelExpanded,
                        onDismissRequest = { channelExpanded = false }
                    ) {
                        CHANNEL_OPTIONS.forEach { ch ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(ch)
                                        if (!isCustomChannel && state.channel == ch) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = GlassTokens.PrimaryBlue,
                                                modifier = Modifier.size(StudioSpacing.iconSmall)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    isCustomChannel = false
                                    state.channel = ch
                                    channelExpanded = false
                                    TerminalLogManager.log(LogLevel.USER, "CHANNEL", "Selected preset channel: $ch")
                                }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = GlassTokens.GlassHairline
                        )

                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = null,
                                            tint = GlassTokens.PrimaryBlue,
                                            modifier = Modifier.size(StudioSpacing.iconSmall)
                                        )
                                        Spacer(modifier = Modifier.width(StudioSpacing.sm))
                                        Text("Custom Channel (Text Input)...", fontWeight = FontWeight.SemiBold)
                                    }
                                    if (isCustomChannel) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = GlassTokens.PrimaryBlue,
                                            modifier = Modifier.size(StudioSpacing.iconSmall)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                isCustomChannel = true
                                if (state.channel in CHANNEL_OPTIONS) {
                                    state.channel = ""
                                }
                                channelExpanded = false
                                TerminalLogManager.log(LogLevel.USER, "CHANNEL", "Switched to custom channel text input mode")
                            }
                        )
                    }
                }

                // Dedicated Custom Text Input Field with Instant Clear Button
                AnimatedVisibility(visible = isCustomChannel) {
                    Column {
                        Spacer(modifier = Modifier.height(StudioSpacing.sm))
                        OutlinedTextField(
                            value = state.channel,
                            onValueChange = { state.channel = it },
                            label = { Text("Custom Channel Identifier") },
                            placeholder = { Text("e.g. credit, fpx_mb2u, boost, grabpay") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    tint = GlassTokens.PrimaryBlue,
                                    modifier = Modifier.size(StudioSpacing.iconMedium)
                                )
                            },
                            trailingIcon = {
                                if (state.channel.isNotEmpty()) {
                                    IconButton(onClick = { state.channel = "" }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Clear Custom Channel",
                                            tint = GlassTokens.TextMuted,
                                            modifier = Modifier.size(StudioSpacing.iconMedium)
                                        )
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(StudioSpacing.standard))

            // Express Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = StudioSpacing.sm)
                ) {
                    Text(text = "Express Mode", style = StudioFont.BodyLarge)
                    Text(text = "Bypasses channel selection UI for instant settlement", style = StudioFont.Caption)
                }
                Switch(
                    checked = state.expressMode,
                    onCheckedChange = { state.expressMode = it },
                    colors = GlassTokens.switchColors()
                )
            }

            Spacer(modifier = Modifier.height(StudioSpacing.standard))

            // Allow Editing Billing Details Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = StudioSpacing.sm)
                ) {
                    Text(text = "Allow Editing Billing Details", style = StudioFont.BodyLarge)
                    Text(text = "Lets the payer edit Order Details: name, email, mobile and description", style = StudioFont.Caption)
                }
                Switch(
                    checked = state.editingEnabled,
                    onCheckedChange = { state.editingEnabled = it },
                    colors = GlassTokens.switchColors()
                )
            }

            // Per-field locks — only read by the SDK while editing is enabled
            if (state.editingEnabled) {
                FieldEditLockToggle(
                    label = "Disable Name Editing",
                    key = "mp_bill_name_edit_disabled",
                    checked = state.billNameEditDisabled,
                    onCheckedChange = { state.billNameEditDisabled = it }
                )
                FieldEditLockToggle(
                    label = "Disable Email Editing",
                    key = "mp_bill_email_edit_disabled",
                    checked = state.billEmailEditDisabled,
                    onCheckedChange = { state.billEmailEditDisabled = it }
                )
                FieldEditLockToggle(
                    label = "Disable Mobile Editing",
                    key = "mp_bill_mobile_edit_disabled",
                    checked = state.billMobileEditDisabled,
                    onCheckedChange = { state.billMobileEditDisabled = it }
                )
                FieldEditLockToggle(
                    label = "Disable Description Editing",
                    key = "mp_bill_description_edit_disabled",
                    checked = state.billDescriptionEditDisabled,
                    onCheckedChange = { state.billDescriptionEditDisabled = it }
                )
            }

            Spacer(modifier = Modifier.height(StudioSpacing.standard))

            // Close Button Display Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = StudioSpacing.sm)
                ) {
                    Text(text = "Close Button Display", style = StudioFont.BodyLarge)
                    Text(text = "Shows a close button on Webcore, Click to Pay, and redirect pages", style = StudioFont.Caption)
                }
                Switch(
                    checked = state.closeButtonDisplay,
                    onCheckedChange = { state.closeButtonDisplay = it },
                    colors = GlassTokens.switchColors()
                )
            }
        }

        // Section 2: Payer & Customer Billing Details
        GlassCard {
            StudioSectionHeader(
                title = "Payer & Billing Details",
                icon = Icons.Rounded.Person,
                subtitle = "Customer Contact & Invoice Metadata"
            )

            OutlinedTextField(
                value = state.billName,
                onValueChange = { state.billName = it },
                label = { Text("Payer Full Name") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Person,
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
                value = state.billEmail,
                onValueChange = { state.billEmail = it },
                label = { Text("Payer Email Address") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Email,
                        contentDescription = null,
                        tint = GlassTokens.PrimaryBlue,
                        modifier = Modifier.size(StudioSpacing.iconMedium)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(StudioSpacing.md))

            OutlinedTextField(
                value = state.billMobile,
                onValueChange = { state.billMobile = it },
                label = { Text("Payer Mobile Phone") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Phone,
                        contentDescription = null,
                        tint = GlassTokens.PrimaryBlue,
                        modifier = Modifier.size(StudioSpacing.iconMedium)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(StudioSpacing.md))

            OutlinedTextField(
                value = state.billDesc,
                onValueChange = { state.billDesc = it },
                label = { Text("Payment Description") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = null,
                        tint = GlassTokens.TextMuted,
                        modifier = Modifier.size(StudioSpacing.iconMedium)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Clean bottom padding
        Spacer(modifier = Modifier.height(StudioSpacing.standard))
    }
}

// Indented sub-toggle under "Allow Editing Billing Details" for one mp_bill_*_edit_disabled key.
@Composable
private fun FieldEditLockToggle(
    label: String,
    key: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = StudioSpacing.standard, top = StudioSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = StudioSpacing.sm)
        ) {
            Text(text = label, style = StudioFont.BodyLarge)
            Text(text = key, style = StudioFont.Caption)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = GlassTokens.switchColors()
        )
    }
}
