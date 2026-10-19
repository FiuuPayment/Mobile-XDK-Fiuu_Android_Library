// Compile-only stand-in for the Samsung Pay SDK (samsungpay_2.22.00.jar), which isn't ours to
// redistribute. composeApp depends on this with compileOnly, so none of it ends up in the AAR —
// hosts that offer Samsung Pay still add the real jar to their app (BLT-03).
// Constant values must match the real SDK exactly: Kotlin inlines static final primitives/Strings.
plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.fiuu.samsungpay.stubs"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
