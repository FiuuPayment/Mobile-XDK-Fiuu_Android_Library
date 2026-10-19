package com.fiuu.xdkandroid.utils

object BuildInfo {
    // Single source of truth for the published library version. composeApp/build.gradle.kts
    // parses SDK_VERSION_NAME out of this file at configure time to set the Maven publish
    // version, and FiuuXDK.podspec reads it the same way for the CocoaPods version, so
    // bumping it here is the only change needed — do not hardcode the version
    // anywhere else. Mirrors Android's versionName: the human-facing semantic version.
    const val SDK_VERSION_NAME = "4.01.01"

    // Mirrors Android's versionCode: a manually bumped build identifier in yyyyMMddNN
    // format (date + revision-of-day), so two internal builds sharing the same
    // SDK_VERSION_NAME can still be told apart (e.g. by support/QA). Not read by
    // build.gradle.kts — it never appears in the Maven publish version.
    const val SDK_VERSION_CODE = 2026091501L
}
