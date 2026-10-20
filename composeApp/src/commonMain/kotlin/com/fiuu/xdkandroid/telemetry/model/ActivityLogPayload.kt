package com.fiuu.xdkandroid.telemetry.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Payload model for Mobile Gateway Activity Log: https://mobile.fiuu.com/api/log/activity
 */
@Serializable
data class ActivityLogRequest(
    @SerialName("data")
    val data: TelemetryDataBlock,
    @SerialName("message")
    val message: ActivityLogMessage,
    @SerialName("checksum")
    val checksum: String,
    @SerialName("datetime")
    val datetime: String,
)

typealias ActivityLogData = TelemetryDataBlock

@Serializable
data class TelemetryDataBlock(
    @SerialName("deviceInfo")
    val deviceInfo: TelemetryDeviceInfo,
    @SerialName("productInfo")
    val productInfo: TelemetryProductInfo,
)

@Serializable
data class TelemetryDeviceInfo(
    @SerialName("os")
    val os: String = "",
    @SerialName("brand")
    val brand: String = "",
    @SerialName("model")
    val model: String = "",
    @SerialName("modelNo")
    val modelNo: String = "",
)

@Serializable
data class TelemetryProductInfo(
    @SerialName("type")
    val type: String = "XDK Native",
    @SerialName("version")
    val version: String = "1.0.0",
    @SerialName("sdkVersion")
    val sdkVersion: String = "1.0.10",
)

@Serializable
data class ActivityLogMessage(
    @SerialName("referenceNumber")
    val referenceNumber: String = "",
    @SerialName("type")
    val type: String = "event",
    @SerialName("process")
    val process: String = "",
    @SerialName("details")
    val details: String = "",
    @SerialName("platform")
    val platform: String = "Android",
    @SerialName("softwareVersion")
    val softwareVersion: String = "",
    @SerialName("networkType")
    val networkType: String = "[ConnectivityResult.none]",
    @SerialName("country")
    val country: String = "MY",
    @SerialName("build")
    val build: String = "",
    @SerialName("merchantId")
    val merchantId: String = "",
)
