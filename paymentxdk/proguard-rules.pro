# Native method protection for libfiuu-sec
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep JavaScript interfaces for payment webview bridges
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Fiuu XDK public interface and models
-keep class com.fiuu.xdk.** { *; }
