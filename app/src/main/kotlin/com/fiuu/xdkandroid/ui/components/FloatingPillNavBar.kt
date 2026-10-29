package com.fiuu.xdkandroid.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiuu.xdkandroid.theme.GlassTokens
import com.fiuu.xdkandroid.theme.StudioFont
import com.fiuu.xdkandroid.theme.StudioSpacing

enum class StudioTab(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    PAYMENT("Payment", "Payment & Billing Details", Icons.Rounded.CreditCard),
    MERCHANT("Merchant", "Merchant Credentials & Gateway", Icons.Rounded.VpnKey),
    FLIGHT_DECK("Test & Log", "Flight Deck & Test Console", Icons.Rounded.RocketLaunch),
    DIAGNOSTICS("Diagnostics", "Chaos Studio & Edge Cases", Icons.Rounded.BugReport)
}

@Composable
fun FloatingPillNavBar(
    selectedTab: StudioTab,
    onTabSelected: (StudioTab) -> Unit,
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = StudioSpacing.standard, vertical = StudioSpacing.sm),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(StudioSpacing.pillRadius),
                colors = CardDefaults.cardColors(containerColor = GlassTokens.PillBackground),
                border = BorderStroke(1.dp, GlassTokens.PillBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StudioTab.values().forEach { tab ->
                        val isSelected = tab == selectedTab
                        PillNavItem(
                            tab = tab,
                            isSelected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTabSelected(tab)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PillNavItem(
    tab: StudioTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tabTag = "tab_" + tab.name.lowercase()

    Box(
        modifier = Modifier
            .semantics {
                contentDescription = tabTag
                testTag = tabTag
            }
            .defaultMinSize(
                minWidth = StudioSpacing.minTouchTarget,
                minHeight = StudioSpacing.minTouchTarget
            )
            .clip(RoundedCornerShape(StudioSpacing.xl))
            .background(if (isSelected) GlassTokens.PillActive else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White),
                onClick = onClick
            )
            .padding(horizontal = if (isSelected) 14.dp else 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.animateContentSize()
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                tint = if (isSelected) Color.White else GlassTokens.PillInactive,
                modifier = Modifier.size(StudioSpacing.iconStandard)
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tab.title,
                    style = StudioFont.Body.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
