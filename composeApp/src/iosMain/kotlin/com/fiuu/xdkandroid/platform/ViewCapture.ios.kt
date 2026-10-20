package com.fiuu.xdkandroid.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.uikit.LocalUIViewController
import com.fiuu.xdkandroid.utils.FiuuLogger
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

@Composable
actual fun rememberViewCaptureHandle(): Any? = LocalUIViewController.current

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun captureViewRegionToPngBytes(handle: Any?, topLeftPx: Offset, sizePx: IntSize): ByteArray? {
    val view = (handle as? UIViewController)?.view
        ?: (handle as? UIView)
        ?: UIApplication.sharedApplication.keyWindow?.rootViewController?.view
        ?: (UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow)?.rootViewController?.view
        ?: run {
            FiuuLogger.log("ViewCapture.ios: Unable to locate active UIView for snapshot")
            return null
        }

    if (sizePx.width <= 0 || sizePx.height <= 0) {
        FiuuLogger.log("ViewCapture.ios: Invalid capture bounds size: ${sizePx.width}x${sizePx.height}")
        return null
    }

    // Compose layout coordinates are provided in physical pixels (px), while UIKit drawing context
    // and view bounds operate in logical points (pt). Normalize px to pt using screen scale to prevent
    // 3x/9x canvas over-allocation, letterboxing, and capture bounds displacement.
    val scale = (view.window?.screen?.scale
        ?: UIApplication.sharedApplication.keyWindow?.screen?.scale
        ?: platform.UIKit.UIScreen.mainScreen.scale).coerceAtLeast(1.0)

    val widthPt = sizePx.width.toDouble() / scale
    val heightPt = sizePx.height.toDouble() / scale
    val originXPt = topLeftPx.x.toDouble() / scale
    val originYPt = topLeftPx.y.toDouble() / scale

    val targetSize = CGSizeMake(widthPt, heightPt)

    // Using `scale` ensures UIKit creates a bitmap matching native retina resolution
    // (e.g. 360pt * 3.0 = 1080px) for crisp barcodes while strictly bounding the canvas to the card.
    UIGraphicsBeginImageContextWithOptions(targetSize, false, scale)
    val context = UIGraphicsGetCurrentContext()
    if (context == null) {
        UIGraphicsEndImageContext()
        FiuuLogger.log("ViewCapture.ios: UIGraphicsGetCurrentContext was null")
        return null
    }

    // Shift the context so the card's top-left corner maps to (0, 0). Any views outside the card
    // (such as top navigation bar, bottom pay button, and terms) are clipped by the targetSize bounds.
    CGContextTranslateCTM(context, -originXPt, -originYPt)

    // drawViewHierarchyInRect snapshots the entire compositor framebuffer, including CAMetalLayer
    val drawn = view.drawViewHierarchyInRect(view.bounds, afterScreenUpdates = true)
    val image = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    if (!drawn || image == null) {
        FiuuLogger.log("ViewCapture.ios: Failed to draw view hierarchy (drawn=$drawn, image=$image)")
        return null
    }

    val pngData = UIImagePNGRepresentation(image)
    if (pngData == null) {
        FiuuLogger.log("ViewCapture.ios: UIImagePNGRepresentation returned null")
        return null
    }

    val bytes = pngData.toByteArray()
    FiuuLogger.log("ViewCapture.ios: Successfully captured card slip image: ${bytes.size} bytes (${widthPt.toInt()}x${heightPt.toInt()} pt @${scale.toInt()}x)")
    return bytes
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size <= 0) return ByteArray(0)
    val bytes = ByteArray(size)
    bytes.usePinned { pinned ->
        platform.posix.memcpy(pinned.addressOf(0), this.bytes, length)
    }
    return bytes
}
