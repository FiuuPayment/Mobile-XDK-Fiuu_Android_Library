package com.fiuu.xdkandroid.platform

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun isDeviceRooted(): Boolean

// Used to hide wallet channels (Apple Pay / Google Pay) the current OS can never actually
// present — the backend returns whatever PLUGIN channels the merchant has configured
// regardless of device, so the UI is responsible for filtering to what's actually usable.
expect fun isAndroidPlatform(): Boolean
expect fun isIOSPlatform(): Boolean