package com.fiuu.xdkandroid.platform

import androidx.compose.ui.graphics.ImageBitmap

// Saves a rendered screen (e.g. PaymentInstructionScreen's barcode card) to the device's photo
// gallery. Provided via the localImageSaver CompositionLocal, wired by each platform's SDK entry
// point (ProvideFiuuPaymentSDK.kt on Android, startFiuuPaymentIos.kt on iOS) — same pattern as
// WebViewBridge/SecureTokenStore.
interface ImageSaver {
    // onResult carries a diagnostic message on failure — surfaced directly to the user on the
    // instruction screen, since FiuuLogger's own log line is silent unless mp_dev_mode is on and
    // a silent failure here previously looked indistinguishable from the button doing nothing.
    fun saveImage(bytes: ByteArray, fileName: String, onResult: (success: Boolean, errorMessage: String?) -> Unit)
}

// Converts a captured composable's ImageBitmap to PNG bytes. Split out as expect/actual because
// ImageBitmap's underlying representation differs per platform (android.graphics.Bitmap on
// Android, a Skia bitmap on iOS) — there's no common encoder in Compose Multiplatform itself.
expect fun ImageBitmap.toPngBytes(): ByteArray
