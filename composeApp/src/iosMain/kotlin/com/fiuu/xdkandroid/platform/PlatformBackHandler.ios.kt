package com.fiuu.xdkandroid.platform

import androidx.compose.runtime.Composable

// No-op — see PlatformBackHandler.kt.
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
}
