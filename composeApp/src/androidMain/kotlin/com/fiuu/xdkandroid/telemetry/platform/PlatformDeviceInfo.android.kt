package com.fiuu.xdkandroid.telemetry.platform

import android.content.Context
import android.os.Build
import com.fiuu.xdkandroid.telemetry.model.TelemetryDeviceInfo
import java.util.concurrent.atomic.AtomicReference

object AndroidTelemetryContext {
    val contextRef = AtomicReference<Context?>(null)

    fun bind(context: Context) {
        contextRef.compareAndSet(null, context.applicationContext)
    }
}

actual fun getPlatformDeviceInfo(): TelemetryDeviceInfo {
    return TelemetryDeviceInfo(
        os = Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT.toString(),
        brand = Build.BRAND ?: Build.MANUFACTURER ?: "Android",
        model = Build.MODEL ?: "",
        modelNo = Build.DEVICE ?: Build.PRODUCT ?: ""
    )
}

actual fun getAppBuildCode(): String {
    val ctx = AndroidTelemetryContext.contextRef.get() ?: return "0"
    return try {
        val pInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pInfo.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            pInfo.versionCode.toString()
        }
    } catch (_: Throwable) {
        "0"
    }
}

actual fun getPlatformSoftwareVersion(): String {
    return Build.DISPLAY.ifBlank { Build.ID ?: "" }
}

actual fun getPlatformName(): String = "Android"
