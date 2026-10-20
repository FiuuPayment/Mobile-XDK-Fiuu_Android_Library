import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

// Single source of truth for the published library version: BuildInfo.kt (commonMain), so the
// JitPack publish version and the version shown in the SDK's own UI footer can never drift apart.
// Only SDK_VERSION_NAME feeds the Maven publish version — SDK_VERSION_CODE is an internal build
// identifier (mirrors Android's versionCode) and never appears here.
val sdkVersion = file("src/commonMain/kotlin/com/fiuu/xdkandroid/utils/BuildInfo.kt")
    .readText()
    .let { Regex("""SDK_VERSION_NAME\s*=\s*"([^"]+)"""").find(it) }
    ?.groupValues
    ?.get(1)
    ?: error("Could not read SDK_VERSION_NAME from BuildInfo.kt")

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization")
    `maven-publish`
}


kotlin {
    jvmToolchain(17)
    val xcframework = XCFramework("ComposeApp")

    @Suppress("OPT_IN_USAGE")
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        publishLibraryVariants("release")
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
        }
    }
//    jvm()

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true

            binaryOption(
                "bundleId",
                "com.fiuu.payment.compose"
            )

            xcframework.add(this)
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.material3)

//            implementation(libs.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.http)
            implementation(libs.androidx.webkit.v1120)
            implementation(libs.androidx.browser.v150)
            implementation("com.google.android.gms:play-services-wallet:19.4.0")
            // Compile-only: Samsung's SDK isn't ours to redistribute and would clash with a host's own copy.
            // Hosts that offer Samsung Pay add the jar to their app; SamsungPayHandler checks for it (BLT-03).
            // We compile against stand-in classes, so Samsung's jar isn't in the repo either.
            compileOnly(project(":samsungpay-stubs"))

            // Legacy Webcore dependencies
            implementation("androidx.appcompat:appcompat:1.7.0")
            implementation("com.google.android.material:material:1.12.0")
            implementation("androidx.constraintlayout:constraintlayout:2.1.4")
            implementation("com.google.zxing:core:3.5.3")
            implementation("com.google.code.gson:gson:2.11.0")
            implementation("com.squareup.okhttp3:okhttp:4.12.0")
            implementation("org.apache.commons:commons-lang3:3.14.0")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.material)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.krypto)
            implementation(libs.kotlinx.datetime)

//            implementation(libs.material.icons.extended)
            implementation(libs.ktor.core)
            implementation(libs.ktor.negotiation)
            implementation(libs.ktor.serialization)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.serialization.core)
            api(libs.kotlinx.coroutines.core)
        }

        iosMain.dependencies {
            implementation(libs.ktor.darwin)

        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

compose.resources {
    packageOfResClass = "com.fiuu.xdkandroid.generated.resources"
}

android {
    namespace = "com.fiuu.xdk"
    compileSdk = 36

    sourceSets.getByName("main") {
        java.srcDirs("src/androidMain/legacyWebcore/java")
        res.srcDirs("src/androidMain/legacyWebcore/res")
    }

    defaultConfig {
        minSdk = 29
        lint.targetSdk = 36
        consumerProguardFiles("consumer-rules.pro")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
//    debugImplementation(libs.ui.tooling)
}

publishing {
    publications {
        withType<MavenPublication> {
            groupId = "com.github.FiuuPayment"
            version = sdkVersion
        }
    }
}

// Only the Android publication is what JitPack ships; renaming every publication
// (kotlinMultiplatform, iOS...) to the same artifactId would make them collide. Done in
// afterEvaluate because the Kotlin plugin assigns "composeApp-android" late and would overwrite it.
// Must stay the repo name so 3.34.x hosts upgrade by bumping the version only.
afterEvaluate {
    publishing.publications.named<MavenPublication>("androidRelease") {
        artifactId = "Mobile-XDK-Fiuu_Android_Library"
    }
}


configurations.all {
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib:2.1.21")
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.21")
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.1.21")
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib-common:2.1.21")
}
