package com.fiuu.xdkandroid.platform

/**
 * Synchronous internet-reachability check, consulted by PaymentViewModel before /initiate,
 * /process and /subscribe so a device with no connection fails fast with
 * PaymentError.NoInternetConnection instead of hanging until the HTTP client's own timeout.
 */
interface NetworkConnectivityChecker {
    fun isConnected(): Boolean
}
