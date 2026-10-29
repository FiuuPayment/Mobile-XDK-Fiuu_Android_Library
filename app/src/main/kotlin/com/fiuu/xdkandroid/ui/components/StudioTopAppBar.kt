package com.fiuu.xdkandroid.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiuu.xdkandroid.logger.LogLevel
import com.fiuu.xdkandroid.logger.TerminalLogManager
import com.fiuu.xdkandroid.models.PaymentParamsState
import com.fiuu.xdkandroid.theme.GlassTokens
import com.fiuu.xdkandroid.theme.StudioFont
import com.fiuu.xdkandroid.theme.StudioSpacing

@Composable
fun StudioTopAppBar(
    state: PaymentParamsState,
    currentTab: StudioTab,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = GlassTokens.TopBarBackground,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = StudioSpacing.standard, vertical = StudioSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Brand & Dynamic Screen Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(StudioSpacing.innerCardRadius))
                            .background(GlassTokens.PrimaryBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Crossfade(targetState = currentTab.icon, label = "TopBarIconCrossfade") { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = currentTab.title,
                                tint = GlassTokens.OnPrimary,
                                modifier = Modifier.size(StudioSpacing.iconMedium)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(StudioSpacing.md))
                    Column {
                        Text(
                            text = "XDK Native Test App",
                            style = StudioFont.SectionHeader
                        )
                        Crossfade(targetState = currentTab.subtitle, label = "TopBarSubtitleCrossfade") { subtitle ->
                            Text(
                                text = subtitle,
                                style = StudioFont.Label.copy(color = GlassTokens.TextMuted)
                            )
                        }
                    }
                }

                // Right Environment Badge & Quick Reset
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm)
                ) {
                    // Sandbox / UAT / Production Badge
                    val (badgeBg, badgeText, badgeLabel) = when (state.buildMode) {
                        3 -> Triple(GlassTokens.UatBadgeBg, GlassTokens.UatBadgeText, "UAT")
                        2 -> Triple(GlassTokens.ProductionBadgeBg, GlassTokens.ProductionBadgeText, "PRODUCTION")
                        else -> Triple(GlassTokens.SandboxBadgeBg, GlassTokens.SandboxBadgeText, "SANDBOX")
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(StudioSpacing.chipRadius))
                            .background(badgeBg)
                            .border(
                                width = 1.dp,
                                color = badgeText.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(StudioSpacing.chipRadius)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(badgeText)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = badgeLabel,
                                style = StudioFont.Badge.copy(
                                    color = badgeText,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }

                    // In-App Theme Switcher: Cycles through System -> Light -> Dark
                    val context = androidx.compose.ui.platform.LocalContext.current
                    IconButton(
                        onClick = {
                            val nextTheme = com.fiuu.xdkandroid.theme.ThemeManager.currentTheme.next
                            com.fiuu.xdkandroid.theme.ThemeManager.setTheme(context, nextTheme)
                            TerminalLogManager.log(LogLevel.USER, "THEME", "Switched Studio Theme to ${nextTheme.label}")
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = com.fiuu.xdkandroid.theme.ThemeManager.currentTheme.icon,
                            contentDescription = "Theme: ${com.fiuu.xdkandroid.theme.ThemeManager.currentTheme.label}",
                            tint = GlassTokens.PrimaryBlue,
                            modifier = Modifier.size(StudioSpacing.iconMedium)
                        )
                    }
                }
            }

            // Subtle Hairline Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GlassTokens.GlassHairline)
            )
        }
    }
}
