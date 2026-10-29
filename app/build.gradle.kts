import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

// Merchant credentials live in .env at the project root (copy .env.example).
// They are compiled into BuildConfig, so rebuild after editing .env.
val envProperties = Properties()
val envFile = rootProject.file(".env")
if (envFile.exists()) {
    envFile.bufferedReader().use { reader ->
        reader.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
            .forEach { line ->
                val (key, value) = line.split("=", limit = 2)
                envProperties[key.trim()] = value.trim()
            }
    }
}

fun getEnv(key: String, defaultValue: String = ""): String {
    return envProperties.getProperty(key, defaultValue)
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
}

android {
    namespace = "com.fiuu.xdkandroid"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fiuu.xdkandroid"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "SANDBOX_MERCHANT_ID", "\"${getEnv("SANDBOX_MERCHANT_ID")}\"")
        buildConfigField("String", "SANDBOX_VERIFICATION_KEY", "\"${getEnv("SANDBOX_VERIFICATION_KEY")}\"")
        buildConfigField("String", "SANDBOX_USERNAME", "\"${getEnv("SANDBOX_USERNAME")}\"")
        buildConfigField("String", "SANDBOX_PASSWORD", "\"${getEnv("SANDBOX_PASSWORD")}\"")
        buildConfigField("String", "SANDBOX_APP_NAME", "\"${getEnv("SANDBOX_APP_NAME", "mobile")}\"")
        buildConfigField("String", "SANDBOX_GP_MERCHANT_ID", "\"${getEnv("SANDBOX_GP_MERCHANT_ID")}\"")

        buildConfigField("String", "UAT_MERCHANT_ID", "\"${getEnv("UAT_MERCHANT_ID")}\"")
        buildConfigField("String", "UAT_VERIFICATION_KEY", "\"${getEnv("UAT_VERIFICATION_KEY")}\"")
        buildConfigField("String", "UAT_USERNAME", "\"${getEnv("UAT_USERNAME")}\"")
        buildConfigField("String", "UAT_PASSWORD", "\"${getEnv("UAT_PASSWORD")}\"")
        buildConfigField("String", "UAT_APP_NAME", "\"${getEnv("UAT_APP_NAME", "mobile")}\"")
        buildConfigField("String", "UAT_GP_MERCHANT_ID", "\"${getEnv("UAT_GP_MERCHANT_ID")}\"")

        buildConfigField("String", "PROD_MERCHANT_ID", "\"${getEnv("PROD_MERCHANT_ID")}\"")
        buildConfigField("String", "PROD_VERIFICATION_KEY", "\"${getEnv("PROD_VERIFICATION_KEY")}\"")
        buildConfigField("String", "PROD_USERNAME", "\"${getEnv("PROD_USERNAME")}\"")
        buildConfigField("String", "PROD_PASSWORD", "\"${getEnv("PROD_PASSWORD")}\"")
        buildConfigField("String", "PROD_APP_NAME", "\"${getEnv("PROD_APP_NAME", "mobile")}\"")
        buildConfigField("String", "PROD_GP_MERCHANT_ID", "\"${getEnv("PROD_GP_MERCHANT_ID")}\"")

        buildConfigField("String", "DEFAULT_CUSTOM_CSS_URL", "\"${getEnv("DEFAULT_CUSTOM_CSS_URL")}\"")

        buildConfigField("String", "BILL_NAME", "\"${getEnv("BILL_NAME")}\"")
        buildConfigField("String", "BILL_EMAIL", "\"${getEnv("BILL_EMAIL")}\"")
        buildConfigField("String", "BILL_MOBILE", "\"${getEnv("BILL_MOBILE")}\"")
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                fun required(key: String): String =
                    keystoreProperties.getProperty(key)?.takeIf { it.isNotBlank() }
                        ?: throw GradleException("key.properties is missing required entry '$key'")
                storeFile = file(required("storeFile"))
                storePassword = required("storePassword")
                keyAlias = required("keyAlias")
                keyPassword = required("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("com.github.FiuuPayment:Mobile-XDK-Fiuu_Android_Library:4.0.0")

    // AndroidX & Jetpack Compose
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material:material-icons-extended:1.7.5")

    // Google Pay Button & Play Services Wallet
    implementation("com.google.android.gms:play-services-wallet:19.4.0")
    implementation("com.google.pay.button:compose-pay-button:1.2.0")

    // The sample encodes PaymentResponseDto itself. The 4.0.0 POM publishes
    // kotlinx-serialization-json as runtime, so it is not on the compile classpath.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
}
