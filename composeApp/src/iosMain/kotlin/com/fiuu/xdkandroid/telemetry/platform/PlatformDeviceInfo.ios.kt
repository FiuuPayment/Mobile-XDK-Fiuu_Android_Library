package com.fiuu.xdkandroid.telemetry.platform

import com.fiuu.xdkandroid.telemetry.model.TelemetryDeviceInfo
import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

actual fun getPlatformDeviceInfo(): TelemetryDeviceInfo {
    val device = UIDevice.currentDevice
    return TelemetryDeviceInfo(
        os = device.systemVersion,
        brand = "Apple",
        model = device.model,
        modelNo = device.model
    )
}

actual fun getAppBuildCode(): String {
    return try {
        val bundle = NSBundle.mainBundle
        val build = bundle.infoDictionary?.get("CFBundleVersion") as? String
        build ?: "0"
    } catch (_: Throwable) {
        "0"
    }
}

actual fun getPlatformSoftwareVersion(): String {
    return UIDevice.currentDevice.systemVersion
}

actual fun getPlatformName(): String = "iOS"
