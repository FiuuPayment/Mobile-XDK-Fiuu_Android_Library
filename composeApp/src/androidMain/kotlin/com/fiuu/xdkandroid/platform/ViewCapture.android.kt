package com.fiuu.xdkandroid.platform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import java.io.ByteArrayOutputStream

@Composable
actual fun rememberViewCaptureHandle(): Any? = LocalView.current

actual fun captureViewRegionToPngBytes(handle: Any?, topLeftPx: Offset, sizePx: IntSize): ByteArray? {
    val view = handle as? View ?: return null
    if (sizePx.width <= 0 || sizePx.height <= 0) return null
    val bitmap = Bitmap.createBitmap(sizePx.width, sizePx.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    // Shifts the region we want into the bitmap's (0,0) origin — everything else the view draws
    // lands outside the bitmap's bounds and is clipped away by the canvas automatically.
    canvas.translate(-topLeftPx.x, -topLeftPx.y)
    view.draw(canvas)
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}
