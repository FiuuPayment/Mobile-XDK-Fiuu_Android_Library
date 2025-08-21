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
