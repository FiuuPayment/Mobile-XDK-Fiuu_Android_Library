package com.fiuu.xdkandroid.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import com.fiuu.xdkandroid.data.mapper.PaymentChannelImage
import com.fiuu.xdkandroid.domain.entity.PaymentChannel
import com.fiuu.xdkandroid.ui.AppIcons
import com.fiuu.xdkandroid.ui.SvgIcon

@Composable
fun QuickPay(
    items: List<PaymentChannel>,
    onItemClick: (PaymentChannel) -> Unit
) {
    PaymentCard(
        padding = PaddingValues(start = 16.dp, end = 24.dp, top = 16.dp, bottom = 16.dp),
        layout = PaymentCardLayout.VERTICAL,
        label = "Quick Pay",
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)        ) {
            items.forEach { item ->
                val quickPayTag = when (item.name.lowercase()) {
                    "googlepay" -> "quickpay_googlepay"
                    "clicktopay" -> "quickpay_clicktopay"
                    "samsungpay" -> "quickpay_samsungpay"
                    "applepay" -> "quickpay_applepay"
                    else -> "quickpay_${item.name.lowercase()}"
                }
                PaymentCard(
                    label = item.title,
                    modifier = Modifier
                        .semantics {
                            contentDescription = quickPayTag
                            testTag = quickPayTag
                        }
                        .clickable { onItemClick(item) },
                    channelIcon = {
                        PaymentChannelImage(
                            title = item.name,
                            contentDescription = item.maskName,
                        )
                    },
                    rightContent = {
                        SvgIcon(
                            resDrawablePath = AppIcons.ExpandRight,
                            contentDescription = "Expand",
                        )
                    }
                )

            }
        }

    }
}
