package com.fiuu.xdkandroid.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

// Opaque per-platform handle used by captureViewRegionToPngBytes() — the underlying Android View
// on Android, unused (always null) on iOS, which instead relies on GraphicsLayer.toImageBitmap()
// for the same capture (see PaymentInstructionScreen).
@Composable
expect fun rememberViewCaptureHandle(): Any?

// Redraws the region of [handle] bounded by [topLeftPx]/[sizePx] (root-relative device pixels)
// directly into a software canvas and returns it PNG-encoded. Android-only: GraphicsLayer's
// toImageBitmap() came back blank for this screen's Card even after isolating the capture onto a
// separate wrapping Box (see PaymentInstructionScreen) — redrawing through the underlying View
// instead sidesteps that hardware-layer readback path entirely, at the cost of not being able to
// capture just a sub-composable in isolation (this always redraws the whole screen, clipped to
// the given region by the destination bitmap's own bounds). Returns null on iOS (handle is always
// null there) or if capture otherwise isn't possible, so callers should fall back to the
// GraphicsLayer path when this returns null.
expect fun captureViewRegionToPngBytes(handle: Any?, topLeftPx: Offset, sizePx: IntSize): ByteArray?
