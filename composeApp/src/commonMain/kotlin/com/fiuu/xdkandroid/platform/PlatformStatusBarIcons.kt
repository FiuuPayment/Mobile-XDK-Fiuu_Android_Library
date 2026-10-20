package com.fiuu.xdkandroid.platform

import androidx.compose.runtime.Composable

// Sets the status bar's icon colour while composed (dark icons for a light background behind
// them, light icons for a dark one), restoring the host's own setting on dispose. The SDK draws
// edge-to-edge under whatever icon colour the host picked, which needn't suit the SDK's own
// AppColors.Primary bar. Android-only; iOS keeps the host's status bar style.
@Composable
expect fun PlatformStatusBarIcons(darkIcons: Boolean)
