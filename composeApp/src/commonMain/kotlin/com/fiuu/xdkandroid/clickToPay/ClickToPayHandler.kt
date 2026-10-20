package com.fiuu.xdkandroid.clickToPay

import androidx.compose.runtime.Composable
import kotlinx.serialization.json.JsonObject

// Renders the Click to Pay (Mastercard SRC) widget as a Composable hosted inside the
// existing Compose tree (e.g. via AndroidView on Android) rather than replacing the Activity's
// content view — so the surrounding FiuuPaymentSDK composition (and its paymentError/paymentSuccess
// LaunchedEffects) is never disposed while this is on screen.
expect class ClickToPayHandler() {
    @Composable
    fun PaymentScreen(
        url: String,
        headers: Map<String, String>,
        amount: Long,
        currency: String,
        country: String? = null,
        closeButtonDisplay: Boolean = true,
        secondsRemaining: (() -> Int)? = null,
        onSuccess: (JsonObject) -> Unit,
        // Registration-required fallback's own cancel/back — its copy invites the user to try a
        // different payment method, so this returns to the channel list rather than ending the
        // session. See onFullCancel for the main widget screen's own Close/back.
        onCancel: (String?) -> Unit,
        // Main widget screen's Close button/back press — the widget IS the payment page here
        // (same as any other channel's redirect WebView), so closing it ends the whole attempt
        // via the normal /inquire-confirmed result flow instead of just backing out to the grid.
        onFullCancel: (String?) -> Unit,
        onNavigateToManualCard: () -> Unit
    )
}
