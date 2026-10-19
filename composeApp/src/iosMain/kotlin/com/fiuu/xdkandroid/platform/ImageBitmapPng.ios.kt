package com.fiuu.xdkandroid.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

// iOS Compose Multiplatform backs ImageBitmap with Skia — round-trip through Skia's own image
// encoder rather than UIKit, since there's no direct ImageBitmap -> UIImage bridge.
actual fun ImageBitmap.toPngBytes(): ByteArray {
    val image = Image.makeFromBitmap(asSkiaBitmap())
    return image.encodeToData(EncodedImageFormat.PNG)?.bytes ?: ByteArray(0)
}
