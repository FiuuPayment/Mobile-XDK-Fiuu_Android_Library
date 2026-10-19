package com.fiuu.xdkandroid.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiuu.xdkandroid.ui.theme.AppColors
import com.fiuu.xdkandroid.utils.NumberFormatter

enum class PayBarButtonStyle {
    // Plain M3 button styling — used by SinglePaymentScreen, PaymentChannelSelection.
    Default,

    // 52dp height, bold 16sp label with a trailing "→", and a lighter disabled palette — used by
    // CardManualEntryScreen.
    Emphasized
}

@Composable
fun BottomPayBar(
    enabled: Boolean,
    amount: Long,
    currency: String,
    onPay: () -> Unit,
    modifier: Modifier = Modifier,
    style: PayBarButtonStyle = PayBarButtonStyle.Default
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
    ) {
        val buttonModifier = if (style == PayBarButtonStyle.Emphasized) {
            Modifier.fillMaxWidth().height(52.dp)
        } else {
            Modifier.fillMaxWidth()
        }
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Secondary,
                disabledContainerColor = if (style == PayBarButtonStyle.Emphasized) AppColors.Border else AppColors.TextSecondary,
                contentColor = AppColors.White,
                disabledContentColor = if (style == PayBarButtonStyle.Emphasized) AppColors.TextSecondary else AppColors.TextPrimary
            ),
            onClick = onPay,
            enabled = enabled,
            modifier = buttonModifier.semantics {
                contentDescription = "btn_pay_now"
                testTag = "btn_pay_now"
            },
            shape = RoundedCornerShape(12.dp)
        ) {
            val label = "Pay ($currency ${NumberFormatter.formatMinorAmount(amount)})"
            if (style == PayBarButtonStyle.Emphasized) {
                Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("→", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            } else {
                Text(label)
            }
        }
    }
}
