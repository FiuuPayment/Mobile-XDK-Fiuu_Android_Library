package com.fiuu.xdkandroid.data.mapper

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.fiuu.xdkandroid.ui.AppIcons

@Composable
fun PaymentChannelImage(
    title: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val vector = when (title.lowercase()) {
        "googlepay", "google pay", "gpay" ->
            AppIcons.GooglePay

        "samsungpay", "samsung pay" ->
            AppIcons.SamsungPay

        "applepay", "apple pay" ->
            AppIcons.ApplePay

        "clicktopay", "click to pay", "mastercard c2p", "card", "credit card" ->
            AppIcons.CreditCard

        else ->
            AppIcons.DefaultChannel // fallback
    }

    Image(
        painter = rememberVectorPainter(vector),
        contentDescription = contentDescription,
        modifier = modifier.size(40.dp)
    )
}