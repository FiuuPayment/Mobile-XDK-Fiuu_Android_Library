package com.fiuu.xdkandroid.platform

import androidx.compose.runtime.Composable

// Intercepts the system/hardware back gesture while composed, routing it to [onBack] instead of
// whatever default back behavior the host Activity would otherwise apply (e.g. closing the whole
// SDK). Android-only in practice — androidx.activity.compose.BackHandler has no iOS equivalent,
// and there's no comparable system back gesture to intercept there (the SDK is presented as a
// full-screen modal with no back-swipe wired up), so the iOS actual is a no-op.
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
