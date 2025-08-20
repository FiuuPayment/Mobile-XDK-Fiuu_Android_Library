############################################
#  General settings / optimizations
############################################

# Keep class/method names for all Activities, Services & BroadcastReceivers
-keep class * extends android.app.Activity
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver

# Keep all custom view classes (anything that extends View)
-keep class * extends android.view.View {
    <init>(android.content.Context);
    <init>(android.content.Context, android.util.AttributeSet);
    <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep Enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

############################################
#  WebView / Javascript bridge
############################################
# Keep all methods annotated with @JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

############################################
#  MOLPay SDK (prevents obfuscation of JS functions)
############################################
-keep class com.molpay.** { *; }

############################################
#  Gson / JSON (optional - but commonly used)
############################################
# If you use Gson or any other reflection based JSON library
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.** { *; }

############################################
#  Optional: OkHttp / Retrofit (only if you use them)
############################################
# -dontwarn okhttp3.**
# -keep class okhttp3.** { *; }
# -dontwarn retrofit2.**
# -keep class retrofit2.** { *; }
