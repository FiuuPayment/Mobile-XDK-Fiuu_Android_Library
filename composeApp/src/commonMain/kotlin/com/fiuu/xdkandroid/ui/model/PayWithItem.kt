package com.fiuu.xdkandroid.ui.model

import androidx.compose.ui.graphics.vector.ImageVector
import com.fiuu.xdkandroid.domain.entity.PaymentChannel

data class PayWithItem(
    val title : String,
    val channels: List<PaymentChannel> = emptyList(),
    // Static icon + caption shown instead of channel logos — for entries with no backend
    // channel data behind them (e.g. a native manual-card-entry shortcut).
    val staticIcon: ImageVector? = null,
    val staticCaption: String? = null,
    // Overrides the default PaymentScreen.Channels(item) navigation target when set.
    val destination: PaymentScreen? = null,
)