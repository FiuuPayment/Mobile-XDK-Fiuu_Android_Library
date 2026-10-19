package com.fiuu.xdkandroid.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait
import platform.darwin.dispatch_time
import kotlin.concurrent.Volatile

/**
 * Backed by NWPathMonitor (Network.framework), started once at construction and kept running for
 * this checker's lifetime — there's no synchronous reachability query in that API, so [connected]
 * is a cached snapshot updated on every path change rather than polled per [isConnected] call.
 *
 * The first snapshot is delivered asynchronously after nw_path_monitor_start, and FiuuPaymentSDK's
 * LaunchedEffect can call /initiate before it lands — so until then [connected] is null (unknown)
 * and [isConnected] briefly blocks for that first callback instead of guessing. If the monitor
 * still hasn't reported after [FIRST_SNAPSHOT_TIMEOUT_NS], connectivity is treated as unavailable
 * (false): a pending snapshot must never let the preflight pass. The timeout is deliberately
 * generous since NWPathMonitor normally reports within milliseconds, so hitting it means the
 * monitor itself is stuck rather than just slow.
 *
 * The owner must call [cancel] when the checkout ends, or the monitor keeps running (and holding
 * its queue) for the rest of the process's lifetime.
 */
@OptIn(ExperimentalForeignApi::class)
class IosNetworkConnectivityChecker : NetworkConnectivityChecker {

    @Volatile
    private var connected: Boolean? = null

    @Volatile
    private var cancelled = false

    private val firstSnapshot = dispatch_semaphore_create(0)
    private val monitor = nw_path_monitor_create()
    private val queue = dispatch_queue_create("com.fiuu.xdkandroid.network-monitor", null)

    init {
        nw_path_monitor_set_queue(monitor, queue)
        // Runs on the serial [queue], so the isFirst check can't race another update.
        nw_path_monitor_set_update_handler(monitor) { path ->
            val isFirst = connected == null
            connected = path != null && nw_path_get_status(path) == nw_path_status_satisfied
            if (isFirst) dispatch_semaphore_signal(firstSnapshot)
        }
        nw_path_monitor_start(monitor)
    }

    override fun isConnected(): Boolean {
        if (com.fiuu.xdkandroid.diagnostics.DiagnosticChaosEngine.activeSimulatedOffline) {
            return false
        }
        connected?.let { return it }
        val deadline = dispatch_time(DISPATCH_TIME_NOW, FIRST_SNAPSHOT_TIMEOUT_NS)
        if (dispatch_semaphore_wait(firstSnapshot, deadline) == 0L) {
            // Pass the single signal on so any other caller that was also waiting wakes too.
            dispatch_semaphore_signal(firstSnapshot)
        }
        return connected ?: false
    }

    /** Stops the monitor. Idempotent, so every checkout teardown path can call it safely. */
    fun cancel() {
        if (cancelled) return
        cancelled = true
        nw_path_monitor_cancel(monitor)
        // Wake any caller still waiting on a first snapshot that will now never arrive; with
        // [connected] still null it returns false instead of sitting out the full timeout.
        dispatch_semaphore_signal(firstSnapshot)
    }

    private companion object {
        const val FIRST_SNAPSHOT_TIMEOUT_NS = 2_000_000_000L
    }
}
