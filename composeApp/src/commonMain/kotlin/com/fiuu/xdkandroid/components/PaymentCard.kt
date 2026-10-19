package com.fiuu.xdkandroid.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PaymentCard(
    label: String,
    labelFontWeight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier,
    layout: PaymentCardLayout = PaymentCardLayout.HORIZONTAL,
    cornerRadius: Dp = 12.dp,
    padding: PaddingValues = PaddingValues(8.dp),
    backgroundColor: Color = Color.White,
    channelIcon: @Composable () -> Unit = {},
    rightContent: @Composable () -> Unit
){
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        when(layout) {
            PaymentCardLayout.HORIZONTAL -> {
                Row(
                    Modifier.fillMaxWidth().padding(padding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row (
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        channelIcon()
                        Spacer(Modifier.size(8.dp))
                        CustomText(label, fontWeight = labelFontWeight)
                    }

                    rightContent()
                }
            }

            PaymentCardLayout.VERTICAL -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(padding),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CustomText(label, fontWeight = labelFontWeight)
                    rightContent()
                }
            }
        }

    }
}