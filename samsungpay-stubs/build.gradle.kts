// Compile-only stand-ins for the Samsung Pay SDK classes SamsungPayHandler uses, so the library
// builds without Samsung's jar in the repo (it isn't ours to redistribute, BLT-03). Never packaged:
// composeApp depends on this compileOnly, and at runtime the host app's real jar supplies the classes.
//
// Signatures, constant values and @NonNull annotations are copied from samsungpay_2.22.00.jar
// (javap -v -constants). Kotlin inlines `static final` constants and reads the nullability
// annotations (they decide its null checks), so either one being wrong changes the bytecode that
// ships in the AAR. Re-check against the real jar when upgrading the Samsung Pay SDK or using a new
// class/member.
plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.fiuu.xdk.samsungpaystubs"
    compileSdk = 36
    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly("androidx.annotation:annotation:1.9.1")
}
