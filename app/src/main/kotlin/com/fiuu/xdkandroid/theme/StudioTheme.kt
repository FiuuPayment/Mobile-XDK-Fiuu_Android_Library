package com.fiuu.xdkandroid.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun StudioTheme(
    content: @Composable () -> Unit
) {
    val darkTheme = isAppDarkTheme()
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF38BDF8),
            onPrimary = Color(0xFF0F172A),
            primaryContainer = Color(0xFF0E74F4),
            onPrimaryContainer = Color(0xFFF8FAFC),
            secondary = Color(0xFF38BDF8),
            onSecondary = Color(0xFF0F172A),
            surface = Color(0xFF0F172A),
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = Color(0xFF1E293B),
            onSurfaceVariant = Color(0xFFCBD5E1),
            background = Color(0xFF070B14),
            onBackground = Color(0xFFF8FAFC),
            outline = Color(0xFF64748B),
            outlineVariant = Color(0xFF475569),
            surfaceContainerHighest = Color(0xFF1E293B),
            error = Color(0xFFF87171),
            onError = Color(0xFF0F172A)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF003BA0),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFE0F2FE),
            onPrimaryContainer = Color(0xFF003BA0),
            secondary = Color(0xFF0284C7),
            onSecondary = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFF1F5F9),
            onSurfaceVariant = Color(0xFF475569),
            background = Color(0xFFF8FAFC),
            onBackground = Color(0xFF0F172A),
            outline = Color(0xFF64748B),
            outlineVariant = Color(0xFF94A3B8),
            surfaceContainerHighest = Color(0xFFE2E8F0),
            error = Color(0xFFDC2626),
            onError = Color(0xFFFFFFFF)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StudioTypography,
        content = content
    )
}
