package com.fiuu.xdkandroid.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.fiuu.xdkandroid.utils.encodeCode128B

// Draws a Code 128B barcode for [value] by scaling its module-width sequence to fill the given
// modifier's bounds. Every non-stop symbol contributes an even number of modules, so global
// index parity (even = bar, odd = space) stays correct across symbol boundaries without needing
// to track state per symbol.
@Composable
fun Code128Barcode(
    value: String,
    modifier: Modifier = Modifier,
    barColor: Color = Color.Black
) {
    val widths = remember(value) { encodeCode128B(value) }
    Canvas(modifier = modifier) {
        val totalModules = widths.sum()
        if (totalModules <= 0) return@Canvas
        val moduleWidth = size.width / totalModules
        var x = 0f
        widths.forEachIndexed { index, width ->
            val barWidth = width * moduleWidth
            if (index % 2 == 0) {
                drawRect(
                    color = barColor,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height)
                )
            }
            x += barWidth
        }
    }
}
