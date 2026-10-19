package com.fiuu.xdkandroid.clickToPay

import androidx.compose.runtime.Composable
import kotlinx.serialization.json.JsonObject

actual class ClickToPayHandler {
    @Composable
    actual fun PaymentScreen(
        url: String,
        headers: Map<String, String>,
        amount: Long,
        currency: String,
        country: String?,
        closeButtonDisplay: Boolean,
        secondsRemaining: (() -> Int)?,
        onSuccess: (JsonObject) -> Unit,
        onCancel: (String?) -> Unit,
        onFullCancel: (String?) -> Unit,
        onNavigateToManualCard: () -> Unit
    ) {
        ClickToPayWebView(
            url = url,
            headers = headers,
            amount = amount,
            currency = currency,
            country = country,
            closeButtonDisplay = closeButtonDisplay,
            secondsRemaining = secondsRemaining,
            onSuccess = onSuccess,
            onCancel = onCancel,
            onFullCancel = onFullCancel,
            onNavigateToManualCard = onNavigateToManualCard
        )
    }
}
