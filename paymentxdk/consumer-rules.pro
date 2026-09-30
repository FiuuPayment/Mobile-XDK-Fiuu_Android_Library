# Consumer Proguard Rules for Mobile-XDK-Fiuu_Android_Library

# Native method protection for libfiuu-sec JNI bridge
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep JavaScript interfaces for payment webview bridges
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Fiuu XDK public interface, activities, and data models
-keep class com.fiuu.xdk.** { *; }
-dontwarn com.fiuu.xdk.**
