package com.fiuu.xdkandroid.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fiuu.xdkandroid.ui.model.PayWithItem
import com.fiuu.xdkandroid.ui.theme.AppColors

@Composable
fun PaymentMethods(
    items: List<PayWithItem>,
    onItemClick: (PayWithItem) -> Unit
) {
    PaymentCard(
        label = "Pay With",
        layout = PaymentCardLayout.VERTICAL
    ) {
        Column(
            modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.White),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                PaymentMethodRow(item,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }

}
