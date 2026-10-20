plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeCompiler) apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0" apply false
}

// Convenience task to build and sync AAR & XCFramework to demo apps
tasks.register("syncDemoBinaries") {
    group = "demo"
    description = "Builds KMP binaries and distributes them to the demo apps"
    dependsOn(":composeApp:assembleRelease", ":composeApp:assembleComposeAppReleaseXCFramework")

    doLast {
        val aarSource = file("composeApp/build/outputs/aar/composeApp-release.aar")
        val xcframeworkSource = file("composeApp/build/XCFrameworks/release/ComposeApp.xcframework")

        if (aarSource.exists()) {
            val androidDest = file("demo-app/android/app/libs")
            val flutterDest = file("demo-app/flutter/flutter_fiuu_xdk/android/libs")
            val cordovaDest = file("demo-app/cordova/cordova-plugin-fiuu-xdk/src/android/libs")

            androidDest.mkdirs()
            flutterDest.mkdirs()
            cordovaDest.mkdirs()

            copy { from(aarSource); into(androidDest) }
            copy { from(aarSource); into(flutterDest) }
            copy { from(aarSource); into(cordovaDest) }
            println("✅ Synchronized AAR to Android, Flutter, and Cordova demo apps.")
        }

        if (xcframeworkSource.exists()) {
            val iosDest = file("demo-app/ios/Frameworks/ComposeApp.xcframework")
            val flutterDest = file("demo-app/flutter/flutter_fiuu_xdk/ios/Frameworks/ComposeApp.xcframework")
            val cordovaDest = file("demo-app/cordova/cordova-plugin-fiuu-xdk/src/ios/Frameworks/ComposeApp.xcframework")

            iosDest.deleteRecursively()
            flutterDest.deleteRecursively()
            cordovaDest.deleteRecursively()

            copy { from(xcframeworkSource); into(iosDest) }
            copy { from(xcframeworkSource); into(flutterDest) }
            copy { from(xcframeworkSource); into(cordovaDest) }
            println("✅ Synchronized XCFramework to iOS, Flutter, and Cordova demo apps.")
        }

        // Zero-Resource Architecture: Clean up any stale legacy compose-resources folders
        file("demo-app/ios/Frameworks/compose-resources").deleteRecursively()
        file("demo-app/flutter/flutter_fiuu_xdk/ios/Frameworks/compose-resources").deleteRecursively()
        file("demo-app/cordova/cordova-plugin-fiuu-xdk/src/ios/Frameworks/compose-resources").deleteRecursively()
    }
}