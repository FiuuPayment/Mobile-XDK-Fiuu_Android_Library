package com.fiuu.xdkandroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiuu.xdkandroid.components.Code128Barcode
import com.fiuu.xdkandroid.components.RemoteImage
import com.fiuu.xdkandroid.components.TermsAndPrivacyConsentText
import com.fiuu.xdkandroid.components.TermsAndPrivacyStyle
import com.fiuu.xdkandroid.platform.ImageSaver
import com.fiuu.xdkandroid.platform.PlatformBackHandler
import com.fiuu.xdkandroid.platform.captureViewRegionToPngBytes
import com.fiuu.xdkandroid.platform.rememberViewCaptureHandle
import com.fiuu.xdkandroid.platform.toPngBytes
import com.fiuu.xdkandroid.ui.model.PaymentInstructionInfo
import com.fiuu.xdkandroid.ui.theme.AppColors
import com.fiuu.xdkandroid.utils.FiuuLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// Shown when the /process response comes back with requestType == "RESPONSE" and
// pInstruction == 1 — offline/over-the-counter channels (Cash-711, etc.) where the customer must
// finish paying at a physical counter using this barcode. Deliberately has no back navigation to
// a previous screen: both the CLOSE action and the system/hardware back button finalize the
// pending result the same way (see PaymentViewModel.confirmPaymentInstructionAndClose()) rather
// than one of them silently leaving the SDK in a half-finished state.
@Composable
fun PaymentInstructionScreen(
    instruction: PaymentInstructionInfo,
    imageSaver: ImageSaver?,
    country: String? = null,
    onClose: () -> Unit
) {
    // Route the system back button through the same finalize path as CLOSE — without this,
    // pressing back fell through to the host Activity's default behavior instead.
    PlatformBackHandler(onBack = onClose)

    // No rememberGraphicsLayer() convenience in this Compose UI version — create/release the
    // layer manually through LocalGraphicsContext, same lifecycle rememberGraphicsLayer() wraps.
    val graphicsContext = LocalGraphicsContext.current
    val graphicsLayer = remember(graphicsContext) {
        graphicsContext.createGraphicsLayer().apply {
            // Default compositing strategy doesn't reliably retain a readable buffer for
            // toImageBitmap() — the on-screen draw still looks correct either way, but without
            // this the saved PNG comes back blank/black.
            compositingStrategy = CompositingStrategy.Offscreen
        }
    }
    DisposableEffect(graphicsLayer) {
        onDispose { graphicsContext.releaseGraphicsLayer(graphicsLayer) }
    }
    val coroutineScope = rememberCoroutineScope()

    // Android-specific fallback path for the capture below — see ViewCapture.kt. captureBounds
    // is filled in by the Box's onGloballyPositioned so the save button knows exactly which
    // screen region to redraw.
    val viewCaptureHandle = rememberViewCaptureHandle()
    var captureBounds by remember { mutableStateOf<Rect?>(null) }

    // Feedback for the Save Image button — without this, a failure (e.g. a null output stream,
    // or imageSaver never having been wired for this host app) looked identical to the button
    // doing nothing at all, with no way to tell which actually happened.
    var saveStatusMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(saveStatusMessage) {
        if (saveStatusMessage == null) return@LaunchedEffect
        delay(3000)
        saveStatusMessage = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Screen)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.Primary)
                // background is applied before this, so the blue bar still extends up behind
                // the status bar — only the CLOSE text (and its padding) gets pushed below it.
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "CLOSE",
                color = AppColors.Secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onClose)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // The capture modifier lives on this plain Box wrapping the Card, not on the Card
            // itself — Card's own elevation/shadow is implemented with its own internal graphics
            // layer, and nesting our record() directly on top of that came back blank when read
            // back via toImageBitmap(). Recording a copy here (while still rendering the real
            // content straight to the screen via drawContent()) is what actually captures it.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        captureBounds = coordinates.boundsInRoot()
                    }
                    .drawWithContent {
                        graphicsLayer.record(
                            density = this,
                            layoutDirection = this.layoutDirection,
                            size = IntSize(size.width.roundToInt(), size.height.roundToInt())
                        ) {
                            this@drawWithContent.drawContent()
                        }
                        drawContent()
                    }
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppColors.Screen)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            RemoteImage(
                                url = instruction.logoUrl,
                                contentDescription = instruction.serviceName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(64.dp)
                            )
                        }

                        HorizontalDivider(color = AppColors.Border)

                        Column(modifier = Modifier.padding(16.dp)) {
                            InfoRow(label = "Services", value = instruction.serviceName)
                            Spacer(Modifier.height(16.dp))
                            InfoRow(label = "Transaction ID", value = instruction.barcodeValue)
                            Spacer(Modifier.height(24.dp))

                            if (instruction.barcodeValue.isNotBlank()) {
                                Code128Barcode(
                                    value = instruction.barcodeValue,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(70.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = instruction.barcodeValue,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(24.dp))
                            }

                            InfoRow(
                                label = "Payable Amount",
                                value = instruction.payableAmountText,
                                valueColor = AppColors.Error
                            )

                            instruction.dueDateNote?.let { note ->
                                Spacer(Modifier.height(24.dp))
                                Text(
                                    text = "Note",
                                    color = AppColors.TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = note,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pinned footer (Save Image button + Terms/Privacy), same placement as
        // CardManualEntryScreen's BottomPayBar + TermsAndPrivacyConsentText below the scrollable
        // content area, rather than scrolling away with the card.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.White)
                // Same reasoning as the top bar's statusBars padding: keep the white footer
                // above the system nav bar/gesture area instead of letting it render underneath.
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Padding scoped to this inner Column only — matching BottomPayBar's own structure —
            // so it doesn't also apply to TermsAndPrivacyConsentText below, which sets its own.
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        val saver = imageSaver
                        if (saver == null) {
                            FiuuLogger.log("PaymentInstructionScreen: Save Image tapped but no ImageSaver was provided")
                            saveStatusMessage = "Saving isn't available on this device."
                            return@Button
                        }
                        coroutineScope.launch {
                            // Prefer the View-based redraw (Android) — GraphicsLayer.toImageBitmap()
                            // came back blank in testing — and fall back to it (iOS, or if bounds
                            // weren't measured yet) when the View path isn't available.
                            val bounds = captureBounds
                            val viewBytes = bounds?.let {
                                captureViewRegionToPngBytes(
                                    viewCaptureHandle,
                                    it.topLeft,
                                    IntSize(it.width.roundToInt(), it.height.roundToInt())
                                )
                            }
                            val bytes = viewBytes ?: graphicsLayer.toImageBitmap().toPngBytes()
                            // saveImage's own I/O (MediaStore insert + file write on Android) is
                            // synchronous — run it off the composition's dispatcher so it can't
                            // block the UI thread.
                            var success = false
                            var errorMessage: String? = null
                            withContext(Dispatchers.IO) {
                                saver.saveImage(bytes, "fiuu_${instruction.barcodeValue}.png") { ok, message ->
                                    success = ok
                                    errorMessage = message
                                }
                            }
                            saveStatusMessage = if (success) {
                                "Image saved to your device."
                            } else {
                                FiuuLogger.log("PaymentInstructionScreen: save failed: $errorMessage")
                                "Failed to save image${errorMessage?.let { ": $it" } ?: "."}"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Secondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Image to Device", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                saveStatusMessage?.let { message ->
                    Text(
                        text = message,
                        color = if (message.startsWith("Failed")) AppColors.Error else AppColors.Success,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            TermsAndPrivacyConsentText(
                country = country,
                style = TermsAndPrivacyStyle.Compact,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = AppColors.TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = AppColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
