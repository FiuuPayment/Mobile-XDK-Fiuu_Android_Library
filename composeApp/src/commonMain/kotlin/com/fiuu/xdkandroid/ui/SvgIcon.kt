package com.fiuu.xdkandroid.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fiuu.xdkandroid.ui.theme.AppColors.TextSecondary

@Composable
fun SvgIcon(
    imageVector: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = TextSecondary,
    size: Dp = 24.dp,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint
    )
}

@Composable
fun SvgIcon(
    resDrawablePath: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = TextSecondary,
    size: Dp = 24.dp,
    @Suppress("UNUSED_PARAMETER") marker: Unit = Unit,
) {
    Icon(
        imageVector = resDrawablePath,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint
    )
}