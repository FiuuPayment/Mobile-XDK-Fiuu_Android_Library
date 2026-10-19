package com.fiuu.xdkandroid.platform

import androidx.compose.ui.text.font.FontFamily

/**
 * iOS cannot reference Android `R.font.*`, and Compose iOS doesn't load `.ttf` via `Font("file.ttf")`.
 * Use the default font family unless you set up a proper multiplatform font-loading solution.
 */
actual val roboto: FontFamily = FontFamily.Default
