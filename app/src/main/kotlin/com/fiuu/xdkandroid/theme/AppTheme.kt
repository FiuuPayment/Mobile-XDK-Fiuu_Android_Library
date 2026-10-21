package com.fiuu.xdkandroid.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTheme(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    val icon: ImageVector
        get() = when (this) {
            SYSTEM -> Icons.Rounded.BrightnessAuto
            LIGHT -> Icons.Rounded.LightMode
            DARK -> Icons.Rounded.DarkMode
        }

    val next: AppTheme
        get() = when (this) {
            SYSTEM -> LIGHT
            LIGHT -> DARK
            DARK -> SYSTEM
        }
}

object ThemeManager {
    private const val PREFS_NAME = "xdk_studio_theme_prefs"
    private const val KEY_THEME = "app_theme_mode"

    var currentTheme by mutableStateOf(AppTheme.SYSTEM)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_THEME, AppTheme.SYSTEM.name)
        currentTheme = runCatching { AppTheme.valueOf(saved ?: AppTheme.SYSTEM.name) }.getOrDefault(AppTheme.SYSTEM)
    }

    fun setTheme(context: Context, newTheme: AppTheme) {
        currentTheme = newTheme
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, newTheme.name)
            .apply()
    }
}

@Composable
fun isAppDarkTheme(): Boolean {
    return when (ThemeManager.currentTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
}
