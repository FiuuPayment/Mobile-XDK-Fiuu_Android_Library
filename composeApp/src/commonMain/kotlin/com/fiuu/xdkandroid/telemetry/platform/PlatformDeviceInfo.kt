package com.fiuu.xdkandroid.telemetry.platform

import com.fiuu.xdkandroid.telemetry.model.TelemetryDeviceInfo

/**
 * Platform provider for device and runtime environment metadata.
 */
expect fun getPlatformDeviceInfo(): TelemetryDeviceInfo

/**
 * Returns host application build code / versionCode.
 */
expect fun getAppBuildCode(): String

/**
 * Returns OS software version / build display ID.
 */
expect fun getPlatformSoftwareVersion(): String

/**
 * Returns human-readable platform name ("Android" or "iOS").
 */
expect fun getPlatformName(): String
