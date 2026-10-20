package com.fiuu.xdkandroid.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fiuu.xdkandroid.ui.AppIcons
import com.fiuu.xdkandroid.ui.SvgIcon
import com.fiuu.xdkandroid.ui.theme.AppColors

@Composable
fun ExpandableCardPayment(
    label: String,
    labelFontWeight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    cornerRadius: Dp = 12.dp,
    padding: Dp = 16.dp,
    backgroundColor: Color = AppColors.White,
    headerContent: @Composable () -> Unit,
    expandedContent: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomText(label, fontWeight = labelFontWeight, modifier = Modifier.weight(1f))

                headerContent()

                if (expanded) {
                    SvgIcon(
                        resDrawablePath = AppIcons.ExpandMore,
                        contentDescription = "Expand",
                    )
                } else {
                    SvgIcon(
                        resDrawablePath = AppIcons.ExpandRight,
                        contentDescription = "Close",
                    )
                }
            }

            // Expandable area
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = padding, end = padding, bottom = padding)
                ) {
                    expandedContent()
                }
            }
        }
    }
}