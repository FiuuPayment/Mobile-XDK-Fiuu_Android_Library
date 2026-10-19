package com.fiuu.xdkandroid.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.fiuu.xdkandroid.platform.PlatformBackHandler
import com.fiuu.xdkandroid.platform.PlatformStatusBarIcons
import com.fiuu.xdkandroid.ui.AppIcons
import com.fiuu.xdkandroid.ui.SvgIcon
import com.fiuu.xdkandroid.ui.theme.AppColors
import com.fiuu.xdkandroid.ui.theme.AppColors.White
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScaffold(
    showAppBar: Boolean = true,
    title: String = "Payment",
    titleFontSize: TextUnit = TextUnit.Unspecified,
    secondsRemaining: (() -> Int)? = null,
    // Matches the strip to the screen below it — Screen grey by default, White over a white page (e.g. the C2P widget).
    timerBackground: Color = AppColors.Screen,
    onNavigateBack: (() -> Unit)? = null,
    onClose: ()-> Unit,
    // While true, the back arrow and Close stay visible but dimmed/non-functional — tapping either
    // shows a snackbar instead of navigating, so the user gets feedback instead of the control
    // silently doing nothing (or, worse, tearing down a WebView/payment attempt mid-flight).
    loading: Boolean = false,
    loadingDisabledMessage: String = "Please wait, this can't be closed while loading",
    // Drawn over the whole body — timer strip and content — but never the app bar, so CLOSE stays
    // usable underneath (e.g. the C2P widget's loading overlay while its SRC scripts load).
    bodyOverlay: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showLoadingDisabledSnackbar = {
        // Guards against a tap-storm queuing up repeats of the same message — SnackbarHostState
        // queues by default (FIFO), so without this, mashing the button stacks the same toast.
        if (snackbarHostState.currentSnackbarData == null) {
            scope.launch { snackbarHostState.showSnackbar(loadingDisabledMessage) }
        }
    }

    // With the app bar hidden (mp_closebutton_display = false) its back arrow goes too — route
    // system back through the same path so e.g. the channel grid can still return to the method
    // list. Android-only; iOS has no system back.
    PlatformBackHandler(enabled = !showAppBar && onNavigateBack != null) {
        if (loading) showLoadingDisabledSnackbar() else onNavigateBack?.invoke()
    }

    // The status bar always sits over AppColors.Primary — the app bar, or the strip below when it's
    // hidden — so its icons stay white whatever the host's own setting is.
    PlatformStatusBarIcons(darkIcons = false)

    Scaffold(
        Modifier.background(White),
        // Without this Scaffold paints M3's default (lavender) background behind the body.
        containerColor = White,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!showAppBar) {
                // The hidden app bar still leaves its status bar area blue, so the white icons
                // stay readable and the top edge looks the same with or without the bar.
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(AppColors.Primary)
                )
            } else {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppColors.Primary,
                        titleContentColor = White,
                        navigationIconContentColor = White
                    ),
                    title = {
                        Text(
                            title,
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = titleFontSize,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        if (onNavigateBack != null) {
                            IconButton(
                                onClick = { if (loading) showLoadingDisabledSnackbar() else onNavigateBack() },
                                modifier = Modifier.semantics {
                                    contentDescription = "btn_nav_back"
                                    testTag = "btn_nav_back"
                                }
                            ) {
                                SvgIcon(
                                    resDrawablePath = AppIcons.ArrowBack,
                                    contentDescription = "btn_nav_back",
                                    tint = if (loading) White.copy(alpha = 0.5f) else White
                                )
                            }
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { if (loading) showLoadingDisabledSnackbar() else onClose() },
                            modifier = Modifier.semantics {
                                contentDescription = "btn_close_sdk"
                                testTag = "btn_close_sdk"
                            }
                        ) {
                            Text("CLOSE", color = if (loading) AppColors.Secondary.copy(alpha = 0.5f) else AppColors.Secondary)
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            // The timer lives in the body rather than topBar so bodyOverlay can cover it too — and
            // so it stays visible when showAppBar is false (mp_closebutton_display hides the close
            // button, not the time left).
            Column(Modifier.fillMaxSize()) {
                if (secondsRemaining != null) SessionTimerBar(secondsRemaining, timerBackground)
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    content()
                }
            }
            bodyOverlay?.invoke()
        }
    }
}

// The countdown turns red once this many seconds (or fewer) remain.
private const val TIMER_WARNING_THRESHOLD_SECONDS = 30

// Reading secondsRemaining() here scopes each tick's recomposition to this bar only;
// the app bar and screen content are unaffected.
@Composable
private fun SessionTimerBar(secondsRemaining: () -> Int, background: Color) {
    val secs = secondsRemaining().coerceAtLeast(0)
    val time = "${(secs / 60).toString().padStart(2, '0')}:${(secs % 60).toString().padStart(2, '0')}"
    val timeColor = if (secs <= TIMER_WARNING_THRESHOLD_SECONDS) AppColors.TimerWarning else AppColors.TimerNormal

    Text(
        text = buildAnnotatedString {
            append("Please complete your payment within ")
            withStyle(SpanStyle(color = timeColor, fontWeight = FontWeight.Bold)) { append(time) }
        },
        color = AppColors.TextSecondary,
        textAlign = TextAlign.End,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .semantics {
                contentDescription = "txt_session_timer"
                testTag = "txt_session_timer"
            }
    )
}
