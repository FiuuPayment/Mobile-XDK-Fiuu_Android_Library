package com.fiuu.xdkandroid.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class AndroidNetworkConnectivityChecker(context: Context) : NetworkConnectivityChecker {

    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    override fun isConnected(): Boolean {
        if (com.fiuu.xdkandroid.diagnostics.DiagnosticChaosEngine.activeSimulatedOffline) {
            return false
        }
        val manager = connectivityManager ?: return true
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
