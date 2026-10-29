package com.fiuu.xdkandroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.fiuu.xdkandroid.theme.GlassTokens
import com.fiuu.xdkandroid.theme.StudioFont
import com.fiuu.xdkandroid.theme.StudioSpacing

@Composable
fun StudioSectionHeader(
    title: String,
    icon: ImageVector? = null,
    subtitle: String? = null,
    iconTint: Color = GlassTokens.PrimaryBlue,
    iconBgColor: Color = GlassTokens.PrimaryBlue.copy(alpha = 0.08f),
    modifier: Modifier = Modifier,
    trailingAction: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = StudioSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(iconBgColor, RoundedCornerShape(StudioSpacing.chipRadius))
                        .border(1.dp, iconTint.copy(alpha = 0.20f), RoundedCornerShape(StudioSpacing.chipRadius)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(StudioSpacing.iconMedium)
                    )
                }
                Spacer(modifier = Modifier.width(StudioSpacing.sm))
            }
            Column {
                Text(
                    text = title,
                    style = StudioFont.SectionHeader
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = StudioFont.Caption
                    )
                }
            }
        }
        if (trailingAction != null) {
            trailingAction()
        }
    }
}
