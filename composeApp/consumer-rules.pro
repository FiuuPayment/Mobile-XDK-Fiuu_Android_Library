# Google Pay / Play Services Wallet — keeps request/response model classes from being stripped
# or renamed by R8 in consuming apps that enable minification.
-keep class com.google.android.gms.wallet.** { *; }
-keep class com.google.android.gms.common.api.** { *; }

# Samsung Pay SDK — supplied by the host app, not this library (compileOnly). -dontwarn lets hosts
# without the jar minify; -keep protects the SDK for hosts that add it.
-dontwarn com.samsung.android.sdk.samsungpay.**
-keep class com.samsung.android.sdk.samsungpay.** { *; }
