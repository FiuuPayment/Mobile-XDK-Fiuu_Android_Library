package com.fiuu.xdkandroid.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fiuu.xdkandroid.theme.GlassTokens
import com.fiuu.xdkandroid.theme.StudioSpacing

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = StudioSpacing.cardRadius,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = GlassTokens.GlassSurface
        ),
        border = BorderStroke(1.dp, GlassTokens.GlassBorder),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(StudioSpacing.standard),
            content = content
        )
    }
}
