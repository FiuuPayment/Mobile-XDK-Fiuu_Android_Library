package com.fiuu.xdkandroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.fiuu.xdkandroid.diagnostics.DiagnosticChaosEngine
import com.fiuu.xdkandroid.logger.LogLevel
import com.fiuu.xdkandroid.logger.TerminalLogManager

/**
 * Intercepts incoming deep links (e.g. scheme `superapp://payment-result?...`) from external
 * e-wallets, bank 3DS authentications, or simulated test runners. Records telemetry into
 * TerminalLogManager and forwards the payload safely to MainActivity without dropping intents.
 */
class DeepLinkActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.data
        val uriString = uri?.toString() ?: ""

        TerminalLogManager.log(
            level = LogLevel.STATUS,
            tag = "DEEPLINK_INTERCEPT",
            message = "DeepLinkActivity intercepted: $uriString",
            details = "Action: ${intent?.action}\nScheme: ${uri?.scheme}\nHost: ${uri?.host}\nPath: ${uri?.path}\nQuery: ${uri?.query}"
        )

        DiagnosticChaosEngine.recordDeepLink(uriString)

        val targetIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = uri
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(targetIntent)
        finish()
    }
}
