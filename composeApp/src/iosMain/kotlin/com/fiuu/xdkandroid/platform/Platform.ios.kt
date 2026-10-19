package com.fiuu.xdkandroid.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun isAndroidPlatform(): Boolean = false
actual fun isIOSPlatform(): Boolean = true

private val JAILBREAK_PATHS = arrayOf(
    "/Applications/Cydia.app",
    "/Applications/Sileo.app",
    "/Library/MobileSubstrate/MobileSubstrate.dylib",
    "/bin/bash",
    "/usr/sbin/sshd",
    "/etc/apt",
    "/private/var/lib/apt"
)

actual fun isDeviceRooted(): Boolean {
    if (com.fiuu.xdkandroid.diagnostics.DiagnosticChaosEngine.activeMockRoot) {
        return true
    }
    val fileManager = NSFileManager.defaultManager
    val hasJailbreakFile = JAILBREAK_PATHS.any { fileManager.fileExistsAtPath(it) }
    return hasJailbreakFile || canWriteOutsideSandbox(fileManager)
}

@OptIn(ExperimentalForeignApi::class)
private fun canWriteOutsideSandbox(fileManager: NSFileManager): Boolean {
    val testPath = "/private/jailbreak_test.txt"
    val written = fileManager.createFileAtPath(testPath, contents = null, attributes = null)
    if (written) {
        fileManager.removeItemAtPath(testPath, error = null)
    }
    return written
}