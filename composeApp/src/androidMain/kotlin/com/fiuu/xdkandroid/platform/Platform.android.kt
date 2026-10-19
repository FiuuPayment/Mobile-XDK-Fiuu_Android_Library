package com.fiuu.xdkandroid.platform

import android.os.Build
import java.io.File

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun isAndroidPlatform(): Boolean = true
actual fun isIOSPlatform(): Boolean = false

private val ROOT_BINARY_PATHS = arrayOf(
    "/sbin/su",
    "/system/bin/su",
    "/system/xbin/su",
    "/data/local/xbin/su",
    "/data/local/bin/su",
    "/system/sd/xbin/su",
    "/system/bin/failsafe/su",
    "/data/local/su",
    "/su/bin/su",
    "/system/app/Superuser.apk",
    "/system/app/Magisk.apk"
)

actual fun isDeviceRooted(): Boolean {
    if (com.fiuu.xdkandroid.diagnostics.DiagnosticChaosEngine.activeMockRoot) {
        return true
    }
    val hasTestKeys = Build.TAGS?.contains("test-keys") == true
    val hasRootBinary = ROOT_BINARY_PATHS.any { File(it).exists() }
    val hasRootShell = try {
        Runtime.getRuntime().exec(arrayOf("which", "su")).inputStream.bufferedReader().readLine() != null
    } catch (e: Exception) {
        false
    }
    return hasTestKeys || hasRootBinary || hasRootShell
}