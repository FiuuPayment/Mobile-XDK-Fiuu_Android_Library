package com.fiuu.xdkandroid.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiuu.xdkandroid.logger.LogEntry
import com.fiuu.xdkandroid.logger.LogFilter
import com.fiuu.xdkandroid.logger.LogLevel
import com.fiuu.xdkandroid.logger.TerminalLogManager
import com.fiuu.xdkandroid.theme.GlassTokens
import com.fiuu.xdkandroid.theme.StudioSpacing

@Composable
fun ObsidianTerminalDrawer(
    modifier: Modifier = Modifier,
    initialHeightDp: Float = 280f,
    onHeightChanged: ((Float) -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val logs by TerminalLogManager.logs.collectAsState()
    val activeFilter by TerminalLogManager.activeFilter.collectAsState()
    val listState = rememberLazyListState()

    // Resizable drawer height state (min 120dp, default 280dp, max 580dp)
    var drawerHeightDp by remember { mutableFloatStateOf(initialHeightDp) }
    val density = LocalDensity.current

    // Active log selected for bottom sheet inspection
    var selectedLogForDetails by remember { mutableStateOf<LogEntry?>(null) }

    // Guardrail dialog for clearing logs
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val filteredLogs = remember(logs, activeFilter) {
        if (activeFilter == LogFilter.ALL) logs else logs.filter(activeFilter::matches)
    }

    // Dynamic per-filter badge counts
    val filterCounts = remember(logs) {
        LogFilter.values().associateWith { filter -> logs.count(filter::matches) }
    }

    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    // Modal Bottom Sheet inspector when a log entry is tapped
    selectedLogForDetails?.let { entry ->
        LogDetailBottomSheet(
            entry = entry,
            onDismiss = { selectedLogForDetails = null }
        )
    }

    // Guardrail confirmation dialog for destructive clear action
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = GlassTokens.ObsidianHeader,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = GlassTokens.LogRed,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Clear Terminal Logs?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "All active session logs (${logs.size} entries) and diagnostic breadcrumbs will be permanently removed.",
                    fontSize = 13.sp,
                    color = GlassTokens.LogMuted,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        TerminalLogManager.clear()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "Terminal logs cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear All", fontWeight = FontWeight.Bold, color = GlassTokens.LogRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = GlassTokens.LogMuted)
                }
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(drawerHeightDp.dp),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = GlassTokens.ObsidianBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Drag Handle Bar (Spacious touch target >= 48dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(GlassTokens.ObsidianHeader)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            val deltaDp = with(density) { -delta.toDp().value }
                            drawerHeightDp = (drawerHeightDp + deltaDp).coerceIn(120f, 580f)
                            onHeightChanged?.invoke(drawerHeightDp)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(GlassTokens.LogMuted.copy(alpha = 0.6f))
                )
            }

            // TIER 1: Top Status & Global Action Bar (Guaranteed single-row stability)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassTokens.ObsidianHeader)
                    .padding(horizontal = StudioSpacing.md, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title + Live Dot + Counter
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(GlassTokens.LogGreen)
                    )
                    Spacer(modifier = Modifier.width(StudioSpacing.sm))
                    Text(
                        text = "LIVE TERMINAL",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${logs.size})",
                        color = GlassTokens.LogMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Global Action Buttons (Touch Target Ergonomics)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Quick Height Snap Toggle (Dock 130dp -> Mid 300dp -> Full 560dp)
                    val resizeIcon = when {
                        drawerHeightDp < 200f -> Icons.Rounded.UnfoldMore
                        drawerHeightDp < 420f -> Icons.Rounded.OpenInFull
                        else -> Icons.Rounded.UnfoldLess
                    }
                    val resizeTooltip = when {
                        drawerHeightDp < 200f -> "Expand to Standard (300dp)"
                        drawerHeightDp < 420f -> "Expand to Full (560dp)"
                        else -> "Collapse to Dock (130dp)"
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val nextHeight = when {
                                drawerHeightDp < 200f -> 300f
                                drawerHeightDp < 420f -> 560f
                                else -> 130f
                            }
                            drawerHeightDp = nextHeight
                            onHeightChanged?.invoke(nextHeight)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = resizeIcon,
                            contentDescription = resizeTooltip,
                            tint = GlassTokens.LogMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Copy Plaintext Logs
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("XDK Logs", TerminalLogManager.exportPlainText()))
                            Toast.makeText(context, "Logs copied to clipboard (${logs.size} entries)", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy Logs",
                            tint = GlassTokens.LogMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Native Share Action
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, TerminalLogManager.exportDiagnosticReport())
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share XDK Diagnostic Report")
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share Diagnostic Report",
                            tint = GlassTokens.LogCyan,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Guarded Clear Action
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showClearConfirmDialog = true
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Clear Logs",
                            tint = GlassTokens.LogRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // TIER 2: Dedicated Horizontal Scrollable Filter Tabs with Live Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassTokens.ObsidianHeader)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = StudioSpacing.md, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LogFilter.values().forEach { filter ->
                    val isSelected = filter == activeFilter
                    val count = filterCounts[filter] ?: 0
                    val activeColor = if (filter == LogFilter.ERROR) GlassTokens.LogRed else GlassTokens.LogCyan

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                if (isSelected) activeColor.copy(alpha = 0.20f)
                                else GlassTokens.ObsidianSurface
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) activeColor else GlassTokens.ObsidianBorder,
                                shape = RoundedCornerShape(7.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                TerminalLogManager.setFilter(filter)
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = filter.name,
                                color = if (isSelected) activeColor else GlassTokens.LogText,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                            )
                            Text(
                                text = "($count)",
                                color = when {
                                    isSelected -> activeColor
                                    filter == LogFilter.ERROR && count > 0 -> GlassTokens.LogRed
                                    else -> GlassTokens.LogMuted
                                },
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (filter == LogFilter.ERROR && count > 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Console Output: Empty State vs Log Stream
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(GlassTokens.ObsidianBackground)
                        .padding(StudioSpacing.lg),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Terminal,
                            contentDescription = null,
                            tint = GlassTokens.LogMuted.copy(alpha = 0.35f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(StudioSpacing.sm))
                        Text(
                            text = "No ${activeFilter.name} events captured yet",
                            color = GlassTokens.LogMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(GlassTokens.ObsidianBackground)
                        .padding(horizontal = StudioSpacing.md, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { entry ->
                        LogItemView(
                            entry = entry,
                            onOpenDetails = { selectedLogForDetails = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogItemView(
    entry: LogEntry,
    onOpenDetails: (LogEntry) -> Unit
) {
    val hasDetails = entry.hasDetails

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(GlassTokens.ObsidianSurface.copy(alpha = 0.45f))
            .border(0.5.dp, GlassTokens.ObsidianBorder.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .clickable { onOpenDetails(entry) }
            .padding(vertical = 5.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = entry.formattedTime,
                color = GlassTokens.LogMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "[${entry.level}]",
                color = entry.color,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = entry.message,
                color = GlassTokens.LogText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (hasDetails) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            (if (entry.level == LogLevel.ERROR) GlassTokens.LogRed else GlassTokens.LogCyan).copy(alpha = 0.16f)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DataObject,
                        contentDescription = "View Details",
                        tint = if (entry.level == LogLevel.ERROR) GlassTokens.LogRed else GlassTokens.LogCyan,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        if (entry.errorCode != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "↳ [${entry.errorCode}] ${entry.errorMessage ?: ""}",
                    color = GlassTokens.LogRed,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else if (!entry.details.isNullOrBlank()) {
            val preview = entry.details.lineSequence().firstOrNull()?.trim() ?: ""
            if (preview.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "↳ $preview (tap to inspect)",
                        color = GlassTokens.LogMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else if (entry.message.length > 120 || entry.message.contains("\n")) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "↳ Tap to view full payload (${entry.message.length} chars)",
                    color = GlassTokens.LogMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
