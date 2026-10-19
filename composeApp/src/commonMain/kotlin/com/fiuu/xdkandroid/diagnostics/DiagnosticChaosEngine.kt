package com.fiuu.xdkandroid.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class SimulatedNetworkMode(val displayName: String, val description: String) {
    NORMAL("Normal (Passthrough)", "Real network connectivity with zero artificial delay or drops"),
    SLOW_3G("Slow 3G Latency", "Injects 2,000ms artificial latency on all REST API calls"),
    SLOW_2G("Slow 2G Latency", "Injects 4,500ms artificial latency on all REST API calls"),
    PACKET_LOSS("Packet Loss (30%)", "Drops 30% of requests with simulated SocketTimeoutException"),
    OFFLINE("Simulated Offline", "Simulates complete network disconnect (ConnectException) without killing ADB/USB"),
    FAULT_500("Gateway 500 Error", "Returns HTTP 500 Internal Server Error on API requests"),
    FAULT_504("Gateway 504 Timeout", "Returns HTTP 504 Gateway Timeout on API requests"),
    FAULT_400("Bad Request 400", "Returns HTTP 400 Invalid Nonce / Bad Request on API requests")
}

data class ChaosState(
    val networkMode: SimulatedNetworkMode = SimulatedNetworkMode.NORMAL,
    val isMockRootActive: Boolean = false,
    val forceWebViewLoadFail: Boolean = false,
    val lastDeepLinkReceived: String? = null
)

// Test-only fault injector for the demo apps' Diagnostic Studio. It ships in the release
// library (the demo apps drive it from Swift and Kotlin, so it has to stay public), which means
// any code in a host app can flip these toggles. They're therefore inert unless the current
// session is armed: mp_dev_mode on and not talking to the production backend — see
// toPaymentRequest(). The SDK's own hooks read only the internal `active*` accessors below,
// never the raw toggles, so a merchant's production session can't be pushed into fake offline,
// fake root or forced WebView failures.
object DiagnosticChaosEngine {
    private val _state = MutableStateFlow(ChaosState())
    val state: StateFlow<ChaosState> = _state.asStateFlow()

    /** Whether the toggles take effect in the current session. Set by the SDK, never by the host. */
    var isArmed: Boolean = false
        internal set

    var networkMode: SimulatedNetworkMode
        get() = _state.value.networkMode
        set(value) {
            _state.update { it.copy(networkMode = value) }
        }

    var isMockRootActive: Boolean
        get() = _state.value.isMockRootActive
        set(value) {
            _state.update { it.copy(isMockRootActive = value) }
        }

    var forceWebViewLoadFail: Boolean
        get() = _state.value.forceWebViewLoadFail
        set(value) {
            _state.update { it.copy(forceWebViewLoadFail = value) }
        }

    var lastDeepLinkReceived: String?
        get() = _state.value.lastDeepLinkReceived
        set(value) {
            _state.update { it.copy(lastDeepLinkReceived = value) }
        }

    internal val activeNetworkMode: SimulatedNetworkMode
        get() = if (isArmed) networkMode else SimulatedNetworkMode.NORMAL

    internal val activeMockRoot: Boolean
        get() = isArmed && isMockRootActive

    internal val activeWebViewLoadFail: Boolean
        get() = isArmed && forceWebViewLoadFail

    internal val activeSimulatedOffline: Boolean
        get() = activeNetworkMode == SimulatedNetworkMode.OFFLINE

    fun recordDeepLink(url: String) {
        _state.update { it.copy(lastDeepLinkReceived = url) }
    }

    fun resetToDefaults() {
        _state.value = ChaosState()
    }
}
