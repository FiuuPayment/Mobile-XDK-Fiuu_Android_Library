package com.fiuu.xdkandroid

import android.app.Application
import com.fiuu.xdkandroid.logger.CrashHandler
import com.fiuu.xdkandroid.logger.LogcatStreamReader
import com.fiuu.xdkandroid.logger.SystemStreamInterceptor
import com.fiuu.xdkandroid.utils.FiuuLogger

class XdkTestApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Install 2-tier flight recorder crash catcher
        CrashHandler.install(this)

        // 2. Explicitly enable XDK internal library logging (FiuuLogger in composeApp-release.aar)
        FiuuLogger.enabled = true

        // 3. Install System.out / System.err stream interception
        SystemStreamInterceptor.install()

        // 4. Start logcat process reader for native bridge tags (FIUU_WEBVIEW, AndroidSecureTokenStore)
        LogcatStreamReader.start(this)

        // 5. Initialize ThemeManager for persistent Light/Dark studio theming
        com.fiuu.xdkandroid.theme.ThemeManager.init(this)
    }
}
