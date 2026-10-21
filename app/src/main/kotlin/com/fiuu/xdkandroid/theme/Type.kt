package com.fiuu.xdkandroid.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun createStudioTypography(isDark: Boolean): Typography {
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    return Typography(
        headlineMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            color = textPrimary
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            color = textPrimary
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = textPrimary
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = textPrimary
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = textSecondary
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            color = textMuted
        )
    )
}

val StudioTypography: Typography
    @Composable get() = createStudioTypography(isAppDarkTheme())

object StudioFont {
    val Display: TextStyle
        @Composable get() = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = GlassTokens.TextPrimary
        )

    val TitleLarge: TextStyle
        @Composable get() = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = GlassTokens.TextPrimary
        )

    val SectionHeader: TextStyle
        @Composable get() = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = GlassTokens.TextPrimary
        )

    val BodyLarge: TextStyle
        @Composable get() = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = GlassTokens.TextPrimary
        )

    val Body: TextStyle
        @Composable get() = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = GlassTokens.TextSecondary
        )

    val Label: TextStyle
        @Composable get() = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = GlassTokens.TextSecondary
        )

    val Caption: TextStyle
        @Composable get() = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            color = GlassTokens.TextMuted
        )

    val Badge: TextStyle
        @Composable get() = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = GlassTokens.TextPrimary
        )

    val Code: TextStyle
        @Composable get() = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = GlassTokens.TextPrimary
        )
}
