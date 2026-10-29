package com.fiuu.xdkandroid.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiuu.xdkandroid.logger.LogEntry
import com.fiuu.xdkandroid.logger.LogLevel
import com.fiuu.xdkandroid.theme.GlassTokens
import com.fiuu.xdkandroid.theme.StudioSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDetailBottomSheet(
    entry: LogEntry,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GlassTokens.ObsidianBackground,
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(GlassTokens.TextMuted.copy(alpha = 0.6f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = StudioSpacing.standard)
                .padding(bottom = StudioSpacing.xl)
                .verticalScroll(scrollState)
        ) {
            // Header Row: Category & Level pills + Timestamp + Close Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Category Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(entry.color.copy(alpha = 0.18f))
                            .border(1.dp, entry.color.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = entry.tag.uppercase(),
                            color = entry.color,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(StudioSpacing.sm))

                    // Log Level Pill
                    Text(
                        text = entry.level.name,
                        color = entry.color,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.formattedTime,
                        color = GlassTokens.LogMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(StudioSpacing.xs))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = GlassTokens.LogMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(StudioSpacing.md))

            // Primary Message Text
            Text(
                text = entry.message,
                color = GlassTokens.LogText,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp
            )

            // Error Banner (if error code or error level detected)
            if (entry.errorCode != null || entry.level == LogLevel.ERROR) {
                Spacer(modifier = Modifier.height(StudioSpacing.md))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(GlassTokens.LogRed.copy(alpha = 0.12f))
                        .border(1.dp, GlassTokens.LogRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(StudioSpacing.md)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = "Error",
                            tint = GlassTokens.LogRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(StudioSpacing.sm))
                        Column {
                            val codeDisplay = entry.errorCode?.let { "[$it] " } ?: ""
                            val msgDisplay = entry.errorMessage ?: (if (entry.errorCode != null) "Transaction failure code mapped" else "Execution failure reported")
                            Text(
                                text = "$codeDisplay$msgDisplay",
                                color = GlassTokens.LogRed,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Metadata / Multi-line Details Section
            if (!entry.details.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(StudioSpacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "METADATA / PAYLOAD DETAILS",
                        color = GlassTokens.LogMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("XDK Log Payload", entry.details))
                            Toast.makeText(context, "Payload copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy Details",
                            tint = GlassTokens.LogCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(StudioSpacing.xs))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp, max = 220.dp)
                        .clip(RoundedCornerShape(StudioSpacing.chipRadius))
                        .background(GlassTokens.ObsidianSurface)
                        .border(1.dp, GlassTokens.ObsidianBorder, RoundedCornerShape(StudioSpacing.chipRadius))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = entry.details,
                        color = GlassTokens.LogText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp
                    )
                }
            }

            // Pre-Event Breadcrumbs Section
            if (entry.breadcrumbs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(StudioSpacing.standard))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PRE-EVENT BREADCRUMBS (${entry.breadcrumbs.size} PRIOR STEPS)",
                        color = GlassTokens.LogMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(modifier = Modifier.height(StudioSpacing.xs))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(StudioSpacing.chipRadius))
                        .background(GlassTokens.ObsidianSurface)
                        .border(1.dp, GlassTokens.ObsidianBorder, RoundedCornerShape(StudioSpacing.chipRadius))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    entry.breadcrumbs.forEachIndexed { index, crumb ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(GlassTokens.ObsidianHeader),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = GlassTokens.LogCyan,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = crumb,
                                color = GlassTokens.LogMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Bottom Quick Actions: Copy Entire Log & Share
            Spacer(modifier = Modifier.height(StudioSpacing.lg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // Copy Full Log Action
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(GlassTokens.ObsidianBorder.copy(alpha = 0.6f))
                        .border(1.dp, GlassTokens.ObsidianBorder, RoundedCornerShape(6.dp))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val formatted = "${entry.formattedTime} [${entry.level}] [${entry.tag}] ${entry.message}\n" +
                                (entry.errorCode?.let { "Error Code: $it\n" } ?: "") +
                                (entry.details?.let { "Details:\n$it\n" } ?: "") +
                                (if (entry.breadcrumbs.isNotEmpty()) "Breadcrumbs:\n${entry.breadcrumbs.joinToString("\n")}\n" else "")
                            clipboard.setPrimaryClip(ClipData.newPlainText("XDK Diagnostic Item", formatted))
                            Toast.makeText(context, "Log item copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy Item",
                        tint = GlassTokens.LogCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Copy Log",
                        color = GlassTokens.LogText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(StudioSpacing.sm))

                // Share Item Action
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(GlassTokens.PrimaryBlue.copy(alpha = 0.8f))
                        .border(1.dp, GlassTokens.PrimaryBlueLight.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .clickable {
                            val formatted = "=== FIUU XDK DIAGNOSTIC ITEM ===\n" +
                                "Time: ${entry.formattedTime}\n" +
                                "Level: ${entry.level}\n" +
                                "Tag: ${entry.tag}\n" +
                                "Message: ${entry.message}\n" +
                                (entry.errorCode?.let { "Error Code: $it\n" } ?: "") +
                                (entry.details?.let { "Details:\n$it\n" } ?: "") +
                                (if (entry.breadcrumbs.isNotEmpty()) "Breadcrumbs:\n${entry.breadcrumbs.joinToString("\n")}\n" else "")
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, formatted)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Log Item"))
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Share Item",
                        tint = GlassTokens.OnPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Share",
                        color = GlassTokens.OnPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
