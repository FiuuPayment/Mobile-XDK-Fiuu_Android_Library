package com.fiuu.xdkandroid

import FiuuPaymentSDK
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import com.fiuu.xdkandroid.data.remote.dto.PaymentResponseDto
import com.fiuu.xdkandroid.domain.entity.reportParamWarnings
import com.fiuu.xdkandroid.domain.entity.resolveCoreEnv
import com.fiuu.xdkandroid.domain.resumePaymentStatus
import com.fiuu.xdkandroid.platform.IosImageSaver
import com.fiuu.xdkandroid.platform.IosNetworkConnectivityChecker
import com.fiuu.xdkandroid.platform.IosSecureTokenStore
import com.fiuu.xdkandroid.platform.IosWebViewBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import localImageSaver
import localNetworkConnectivityChecker
import localSecureTokenStore
import localWebViewBridge
import platform.Foundation.NSDictionary
import platform.UIKit.UIViewController
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

import com.fiuu.xdkandroid.platform.createIosWebcoreViewController

@Suppress("unused")
fun startFiuuPaymentIos(
    paymentParams: NSDictionary,
    onResult: (PaymentResponseDto) -> Unit,
    onClose: () -> Unit
): UIViewController {
    val params = paymentParams.toMap()
        .mapKeys { it.key.toString() }

    // 1 = native engine; 2/3/4 = legacy Webcore — same parse resolveBuildMode() uses, so the
    // engine choice and the backend choice can't disagree (e.g. "03" was native here but UAT there)
    val coreEnv = params.resolveCoreEnv()
    params.reportParamWarnings()

    if (coreEnv != 1) {
        return createIosWebcoreViewController(
            // IosWebcoreViewController string-matches "2"/"3"/"4" and treats anything else as production
            params = params + ("mp_core_env" to coreEnv.toString()),
            onResult = onResult,
            onClose = onClose
        )
    }

    com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.drainQueue()

    val bridge = IosWebViewBridge()
    // Created once per session (not inside the composable) so recomposition doesn't start extra
    // NWPathMonitors, and so its first path snapshot is already in flight before /initiate.
    // Cancelled on both teardown paths below: the SDK's own onRequestClose, and composition
    // disposal (covers a host that dismisses the VC itself without onRequestClose firing).
    val networkConnectivityChecker = IosNetworkConnectivityChecker()

    // Use a var so the lambdas below can reference the VC after it's created
    var vcRef: UIViewController? = null

    // DoNothing: PaymentScaffold already shrinks its body by the keyboard (imePadding) so each form
    // scrolls the focused field above it. The default, FocusableAboveKeyboard, also slides the whole
    // view up — the iOS twin of Android's adjustPan — which pushed the app bar off-screen and left a
    // gap over the keyboard (see ProvideFiuuPaymentSDK's soft-input mode).
    val vc = ComposeUIViewController(configure = {
        enforceStrictPlistSanityCheck = false
        onFocusBehavior = OnFocusBehavior.DoNothing
    }) {
        CompositionLocalProvider(
            localWebViewBridge provides bridge,
            localSecureTokenStore provides IosSecureTokenStore(),
            localImageSaver provides IosImageSaver(viewControllerProvider = { vcRef }),
            localNetworkConnectivityChecker provides networkConnectivityChecker
        ) {
            DisposableEffect(Unit) {
                onDispose { networkConnectivityChecker.cancel() }
            }
            FiuuPaymentSDK(
                params = params,
                onPaymentResult = { result ->
                    dispatch_async(dispatch_get_main_queue()) {
                        onResult(result)
                    }
                },
                onRequestClose = {
                    dispatch_async(dispatch_get_main_queue()) {
                        vcRef?.dismissViewControllerAnimated(true, completion = null)
                        bridge.controller = null
                        networkConnectivityChecker.cancel()
                        vcRef = null
                        onClose()
                    }
                }
            )
        }
    }

    vcRef = vc
    bridge.controller = vc
    return vc
}

// Fire-and-forget entry point, so it needs its own scope — there's no composable/ViewModel scope
// available for a one-off call like this outside an active checkout.
private val resumeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/**
 * Resumes a status check for the most recently attempted payment interrupted mid-checkout (app
 * killed/backgrounded before a result was delivered). [onResult] is always dispatched on the main
 * queue, and receives null if no session is being tracked (never started, already completed, or
 * its 15-minute window already passed and was cleared). The session's accessToken never leaves the
 * SDK — it was persisted internally (Keychain-backed) the moment /initiate or /express succeeded,
 * and is only ever read back out here.
 */
@Suppress("unused")
fun resumeFiuuPaymentStatus(onResult: (PaymentResponseDto?) -> Unit) {
    val store = IosSecureTokenStore()
    resumeScope.launch {
        val result = resumePaymentStatus(store)
        dispatch_async(dispatch_get_main_queue()) { onResult(result) }
    }
}
