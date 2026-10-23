# [Fiuu Mobile XDK] – Android Payment Library

<img alt="Fiuu Mobile XDK" src="https://user-images.githubusercontent.com/38641542/74424311-a9d64000-4e8c-11ea-8d80-d811cfe66972.jpg">

Fiuu's Android payment SDK, version **4.0.0**. It shows Fiuu's checkout screens inside your app, talks to the Fiuu payment API directly, and returns the result to a callback. It is published on JitPack and replaces the 3.x Android XDK (`com.fiuu.xdk.PaymentActivity`). If you are on 3.x, read [Migrating from 3.x](#migrating-from-3x) first.

For iOS, see [Mobile-XDK-Fiuu_Swift](https://github.com/FiuuPayment/Mobile-XDK-Fiuu_Swift). Both platforms are built from the same Kotlin Multiplatform code and take the same parameters.

You need a Fiuu merchant account to use it. For sandbox or production credentials, contact sales-sa@fiuu.com.

## Contents

- [Requirements](#requirements)
- [Setup](#setup)
- [Start a payment](#start-a-payment)
- [Handle the result](#handle-the-result)
- [Payment parameters](#payment-parameters)
- [Engines and environments](#engines-and-environments)
- [Google Pay](#google-pay)
- [Returning from e-wallet apps](#returning-from-e-wallet-apps)
- [Troubleshooting](#troubleshooting)
- [Hybrid apps](#hybrid-apps)
- [Migrating from 3.x](#migrating-from-3x)
- [For maintainers](#for-maintainers)
- [Resources](#resources)
- [Support](#support)

## Requirements

The published library (`com.github.FiuuPayment:Mobile-XDK-Fiuu_Android_Library`, currently **4.0.0**) is built with `minSdk` 29, `compileSdk` 36 and Java 17. The library's `compileSdk` applies only to the library; set your own from the table below.

### Required host settings

A lower value fails the Gradle build or the manifest merge.

| Setting | Minimum | Notes |
|---|---|---|
| `minSdkVersion` | **29** (Android 10) | Declared by this library. A host `minSdk` below 29 is rejected. Below Android 10 the system WebView no longer gets Chrome updates, and 3DS, FPX and other bank pages run in that WebView |
| `compileSdk` | **35** | Required by Compose UI and Foundation 1.8.0 (`minCompileSdk=35`). 36 is recommended (see below) |
| Android Gradle Plugin | **8.6.0** | The first AGP that supports `compileSdk` 35. Use 8.9.1 or later for 36 |
| AndroidX | enabled | The library is AndroidX only. Set `android.useAndroidX=true` |
| Java bytecode | **17** | The library is compiled for Java 17. A host on Java 8 bytecode fails when Kotlin inlines library code. JDK 17 is also needed to run AGP 8 |
| Kotlin and Compose | Compose compiler enabled in the module that shows the SDK | `ProvideFiuuPaymentSDK` is a composable. See [Setup, step 3](#3-enable-compose-in-your-app-module) |
| Host Activity | a `ComponentActivity` (for example `AppCompatActivity`) | The SDK needs it for its bank pages and Google Pay results |
| Repositories | Google, Maven Central, **JitPack** | The artifact is served from JitPack (`com.github.FiuuPayment:...`) |

### Recommended for Google Play (as of 31 Aug 2026)

The library's `compileSdk` 36 does not set your `targetSdk`. Google Play's target API rules apply to your app, not to the library.

| Setting | Recommendation |
|---|---|
| `compileSdk` | **36** (must still be 35 or higher) |
| `targetSdkVersion` | **36** for new Play apps and app updates. Existing apps need **35** or higher to stay available to users on newer Android versions |
| Android Gradle Plugin | Match `compileSdk`: **8.9.1+** for API 36, **8.6.0+** for API 35 |
| Gradle | Match AGP: 8.11.1+ with AGP 8.9, 8.7+ with AGP 8.6 |
| JDK | **17+** |

Example host `defaultConfig`:

```kotlin
android {
    compileSdk = 36   // 35 minimum; 36 recommended for Play
    defaultConfig {
        minSdk = 29
        targetSdk = 36
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

### Runtime

| Area | Requirement |
|---|---|
| OS | Android **10 (API 29)** and above |
| Bank and card pages (3DS, FPX) | System WebView, plus `INTERNET` and `ACCESS_NETWORK_STATE` (merged from the library) |
| Saving a QR code or voucher image | MediaStore, with no storage permission (every supported Android version is API 29 or higher) |
| Google Pay | Google Play services on the device. The library uses `play-services-wallet:19.4.0`. Production also needs [Google Pay production access](https://developers.google.com/pay/api/android/guides/test-and-deploy/request-prod-access). If you draw your own Google Pay button (`PayButton`, or `com.google.pay.button:compose-pay-button` in Compose), declare `com.google.android.gms:play-services-wallet:19.4.0` in your app too. The library depends on it as `implementation`, so the button classes are not passed on to your app |

## Setup

### 1. Add JitPack

`settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Groovy `settings.gradle`:

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

### 2. Add the dependency

```kotlin
dependencies {
    implementation("com.github.FiuuPayment:Mobile-XDK-Fiuu_Android_Library:4.0.0")
}
```

Release tags match the SDK version exactly, with no `v` prefix. See [releases](https://github.com/FiuuPayment/Mobile-XDK-Fiuu_Android_Library/releases) for newer versions.

The library's manifest already declares `INTERNET`, `ACCESS_NETWORK_STATE` and the Google Pay `wallet.api.enabled` meta-data, and its R8/ProGuard rules are applied to your release build automatically. You don't need to copy either into your app.

### 3. Enable Compose in your app module

`ProvideFiuuPaymentSDK` is a composable, so the module that calls it needs the Compose compiler. If your app is already on Compose, skip this.

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") // same version as your Kotlin plugin
}

android {
    buildFeatures { compose = true }
}

dependencies {
    implementation("androidx.activity:activity-compose:<version>")
}
```

A Java or XML app can keep the rest of its code as it is and add one small Kotlin Activity for checkout. The [migration section](#migrating-from-3x) has an example.

### 4. Declare `configChanges` on the payment Activity

The SDK keeps the payment state in the composition. If Android recreates the Activity mid-payment (rotation, dark mode, font scale, multi-window), the attempt is cancelled and the SDK starts over. Declare the changes on the Activity that shows the SDK so Compose handles them in place:

```xml
<activity
    android:name=".CheckoutActivity"
    android:configChanges="orientation|keyboardHidden|keyboard|screenSize|smallestScreenSize|locale|layoutDirection|fontScale|screenLayout|density|uiMode" />
```

## Start a payment

Put `ProvideFiuuPaymentSDK` in your composition when the customer taps Pay, and take it out when `onRequestClose` fires.

```kotlin
import com.fiuu.xdkandroid.data.remote.dto.PaymentResponseDto
import com.fiuu.xdkandroid.platform.ProvideFiuuPaymentSDK

class CheckoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var paying by remember { mutableStateOf(false) }

            if (!paying) {
                Button(onClick = { paying = true }) { Text("Pay") }
            } else {
                ProvideFiuuPaymentSDK(
                    params = mapOf(
                        // Credentials from Fiuu
                        "mp_username" to "your_username",
                        "mp_password" to "your_password",
                        "mp_merchant_ID" to "your_merchant_id",
                        "mp_app_name" to "your_app_name",
                        "mp_verification_key" to "your_verification_key",

                        // Order
                        "mp_amount" to "1.10", // 2 decimal places
                        "mp_order_ID" to "ORD-${System.currentTimeMillis()}", // letters, digits, - and _
                        "mp_currency" to "MYR",
                        "mp_country" to "MY",
                        "mp_bill_description" to "Order description",
                        "mp_bill_name" to "Customer name",
                        "mp_bill_email" to "customer@example.com",
                        "mp_bill_mobile" to "60123456789",

                        // Checkout
                        "mp_channel" to "multi", // show every channel on your account
                        "mp_build_mode" to 4,    // 2 = production, 3 = UAT, 4 = sandbox
                    ),
                    onPaymentResult = { result: PaymentResponseDto ->
                        // See "Handle the result"
                    },
                    onRequestClose = {
                        paying = false
                    },
                )
            }
        }
    }
}
```

### Express mode

To skip the SDK screens and go straight to one channel, set `mp_channel` to that channel and `mp_express_mode` to `true`.

FPX Maybank online banking:

```kotlin
"mp_channel" to "maybank2u",
"mp_express_mode" to true,
```

Touch 'n Go eWallet:

```kotlin
"mp_channel" to "TNG-EWALLET",
"mp_express_mode" to true,
```

Channel names are listed in [channel-list.md](https://github.com/FiuuPayment/Mobile-XDK-Fiuu_Examples/blob/master/channel-list.md) (`mp_channel` column). You can only use a channel that is enabled on your merchant account; any other channel ends with `INVALID CHANNEL` (see [Troubleshooting](#troubleshooting)). `mp_channel` is matched without regard to case or surrounding spaces.

## Handle the result

`onPaymentResult` receives a `PaymentResponseDto`. Read `status`:

| `status` | Meaning | What to do |
|---|---|---|
| `"00"` | Paid | Show success, then confirm on your server |
| `"11"` | Failed | Let the customer try again |
| `"22"` | Pending. The customer may still be approving in a wallet app, or a cash voucher was issued but not paid | Show "pending" and call `resumeFiuuPaymentStatus` later |
| `"408"` | The 5-minute session expired before the SDK could confirm the status | Same as `"22"` |
| `"USER_CANCELLED"` | The customer closed a bank or wallet page without paying | Return to your cart |
| anything else | The payment could not be started or finished. `errorCode` and `message` say why | Log `errorCode` and `message`, show a generic error. See [Troubleshooting](#troubleshooting) |

Other fields you will usually want:

| Field | Content |
|---|---|
| `transactionId`, `tranID` | Fiuu transaction ID |
| `referenceNo` | Your order ID |
| `amount`, `currency`, `channel` | As charged |
| `paydate`, `appcode` | Payment date and bank approval code, when the gateway returns them |
| `message`, `errorCode`, `errorDesc` | Reason for a non-`00` result |

`statusCheck` is `true` only when `status == "00"`. Use `status` as the source of truth.

Example of a paid result (values vary by channel; a field the gateway didn't return is `null`):

```json
{
  "status": "00",
  "statusCheck": true,
  "message": "",
  "transactionId": "2754048669",
  "tranID": "2754048669",
  "referenceNo": "ORD-1741858760492",
  "amount": "1.10",
  "currency": "MYR",
  "channel": "TNG-EWALLET",
  "paydate": "1741858781",
  "appcode": null,
  "errorCode": null,
  "errorDesc": null
}
```

Example of an error result:

```json
{
  "status": "CATCH ERROR",
  "statusCheck": false,
  "message": "Payment requires a web page (e.g. 3DS) but no WebView is available",
  "errorCode": "NO_WEBVIEW_BRIDGE"
}
```

The SDK delivers one result per payment and then calls `onRequestClose`. If the customer closes the SDK before starting a payment (CLOSE or back on the channel list), you get only `onRequestClose`, with no result.

### Pending payments: `resumeFiuuPaymentStatus`

For `"22"` and `"408"` the SDK keeps the session, encrypted in the Android Keystore, for 15 minutes. Call `resumeFiuuPaymentStatus` later, for example the next time your app opens, to get the final status. It returns `null` when there is nothing to resume.

```kotlin
import com.fiuu.xdkandroid.platform.resumeFiuuPaymentStatus

lifecycleScope.launch {
    val result = resumeFiuuPaymentStatus(context)
    if (result != null) {
        // result.status is the latest gateway status
    }
}
```

This also recovers a payment whose app was killed during a bank or 3DS page.

### Confirm payments on your server

The result in the app is for your UI. Before you ship goods or credit an account, confirm the final status on your backend with Fiuu's server-to-server notification (callback URL). A `"22"` can still turn into `"00"` or `"11"` after the app has closed.

## Payment parameters

Pass parameters as a `Map<String, Any?>`. Booleans can be `true`/`false` or the strings `"true"`/`"false"`.

### Required

| Key | Description |
|---|---|
| `mp_username`, `mp_password` | API credentials from Fiuu |
| `mp_merchant_ID` | Merchant ID |
| `mp_app_name` | App name registered with Fiuu |
| `mp_verification_key` | Verification key, used to sign every request |
| `mp_amount` | Amount, for example `"1.10"`. `"10"` and `"1.5"` are padded to two decimals |
| `mp_order_ID` | Your unique order reference |
| `mp_currency`, `mp_country` | For example `"MYR"`, `"MY"` |
| `mp_bill_name`, `mp_bill_email`, `mp_bill_mobile`, `mp_bill_description` | Customer and order details shown in Order Details |

### Optional

| Key | Default | Description |
|---|---|---|
| `mp_channel` | | `"multi"` for the channel list, or a single channel name |
| `mp_express_mode` | `false` | Skip the SDK screens and go straight to `mp_channel` |
| `mp_build_mode` | | Backend: `2` production, `3` UAT, `4` sandbox. See [Engines and environments](#engines-and-environments) |
| `mp_sandbox_mode` | `true` | Fallback when `mp_build_mode` is not set. `false` means production |
| `mp_core_env` | `"1"` | `"1"` native engine. `"2"`/`"3"`/`"4"` legacy Webcore |
| `mp_editing_enabled` | `false` | Let the customer edit name, email, mobile and description |
| `mp_bill_name_edit_disabled`, `mp_bill_email_edit_disabled`, `mp_bill_mobile_edit_disabled`, `mp_bill_description_edit_disabled` | `false` | Lock one field. Only read while `mp_editing_enabled` is `true` |
| `mp_closebutton_display` | `true` | Show the close button on full-screen pages |
| `mp_gp_merchant_ID` | | Gateway merchant ID for Google Pay |
| `mp_channel_type` | | Channel type, only when Fiuu asks you to set it |
| `mp_dev_mode` | `false` | Debug logging. Card data is redacted. It never changes the backend |

With `mp_dev_mode` on, the SDK also logs a warning for any key it will ignore, including likely typos (`mp_merchant_id` instead of `mp_merchant_ID`).

## Engines and environments

The SDK has two engines:

| `mp_core_env` | Engine | Backend |
|---|---|---|
| `"1"` (default) | Native checkout, built in Compose | Picked by `mp_build_mode` / `mp_sandbox_mode` |
| `"2"` | Legacy Webcore page, same as the 3.x XDK | Production, `https://xdk.fiuu.com/` |
| `"3"` | Legacy Webcore page | UAT, `https://uat-xdk.fiuu.com/` |
| `"4"` | Legacy Webcore page | Sandbox, `https://sandbox-xdk.fiuu.com/` |

Values outside 1 to 4 fall back to `"1"`. The old V1 endpoint (`mp_core_env = "1"` in 3.x) is retired, and `"1"` now means the native engine.

The backend is chosen once, and the first rule that gives a valid value wins:

1. `mp_core_env` `"2"`/`"3"`/`"4"`: Webcore production, UAT or sandbox.
2. `mp_build_mode` `2`/`3`/`4`: native production, UAT or sandbox (`Int` or numeric `String`).
3. `mp_sandbox_mode`: `false` is production. `true`, missing or unrecognized is sandbox.

So a session with no environment parameters runs on sandbox. Set `mp_build_mode = 2` (or `mp_sandbox_mode = false`) for live payments. The same choice sets the Google Pay environment.

## Google Pay

Google Pay works in the channel list with no extra code. You still need to do these on your side, because Google ties production access to your app:

1. Pass your gateway merchant ID as `mp_gp_merchant_ID`.
2. Pick the environment with `mp_build_mode`. `2` uses Google Pay `ENVIRONMENT_PRODUCTION` and the live Fiuu API; `3`, `4` or nothing uses `ENVIRONMENT_TEST`. Use the credentials for that environment.
3. Before going live, get Google Pay production access for your app (next section).

### Requirements for the production environment

Real transactions (production or dev account) need Google Pay production access for your app's package name. Until Google approves it, you can only use the TEST environment with a sandbox account (`mp_build_mode = 4`).

1. Follow Google's guide to [request production access](https://developers.google.com/pay/api/android/guides/test-and-deploy/request-prod-access) in the [Google Pay & Wallet Console](https://pay.google.com/business/console).
2. Choose the **Gateway** integration type when asked, and provide screenshots of your app for review.
3. Register the package name and the SHA-1 of your **release** signing key.
4. After approval, set `mp_build_mode = 2` with your production or dev `mp_merchant_ID` and `mp_verification_key`, and test Google Pay from a signed release build.

Request production access, example 1:

![Google Pay request production access, example 1](https://github.com/user-attachments/assets/3a9c9a77-72ce-4b3a-9cf6-294b9c579c52)

Request production access, example 2:

![Google Pay request production access, example 2](https://github.com/user-attachments/assets/8159c306-e1d0-4026-bae2-0c65dfb82aa0)

### Your own Google Pay button (express)

To put a Google Pay button on your own screen, create a `GooglePayHandler` on a `ComponentActivity` and call `presentGooglePayExpressButton`:

```kotlin
import com.fiuu.xdkandroid.googlePay.GooglePayExpressResult
import com.fiuu.xdkandroid.googlePay.GooglePayHandler
import com.fiuu.xdkandroid.googlePay.presentGooglePayExpressButton

val googlePayHandler = remember { GooglePayHandler(activity) }
val launcher = rememberLauncherForActivityResult(
    ActivityResultContracts.StartIntentSenderForResult()
) { result -> googlePayHandler.onSenderResult(result.resultCode, result.data) }

DisposableEffect(googlePayHandler, launcher) {
    googlePayHandler.registerLauncher { request -> launcher.launch(request) }
    onDispose { googlePayHandler.unregisterLauncher() }
}

var googlePayReady by remember { mutableStateOf(false) }
LaunchedEffect(Unit) {
    googlePayHandler.isReadyToPay(buildMode = 4) { ready -> googlePayReady = ready }
}

// In your button's onClick:
presentGooglePayExpressButton(
    googlePayHandler = googlePayHandler,
    params = paymentParams,     // same map as ProvideFiuuPaymentSDK
    amountMinorUnits = 110L,    // 1.10 MYR
    currencyCode = "MYR",
    coroutineScope = coroutineScope,
) { result ->
    when (result) {
        GooglePayExpressResult.InProgress -> { /* token received, payment running */ }
        is GooglePayExpressResult.PaymentReceived -> { /* result.response.status */ }
        GooglePayExpressResult.Cancelled -> { /* sheet closed */ }
        is GooglePayExpressResult.SheetError -> { /* result.message */ }
    }
}
```

Pass the same build mode to `isReadyToPay` that your params resolve to. Cards that need 3DS open the bank page on the handler's Activity, so it must be a `ComponentActivity` (for Flutter, `FlutterFragmentActivity`, not `FlutterActivity`). Otherwise the payment fails with `NO_WEBVIEW_BRIDGE`.

## Returning from e-wallet apps

Some channels, such as Touch 'n Go eWallet, send the customer to the wallet app to approve. After approval, the wallet opens Fiuu's return page, which redirects to the return URL scheme configured on your Fiuu merchant account (for example `yourapp://`). Your app has to handle that link:

1. **Register the scheme.** Add an `intent-filter` with `VIEW`, `DEFAULT` and `BROWSABLE`, plus `<data android:scheme="yourapp" />`. The scheme must match the one on your merchant account exactly. Otherwise the customer is left on Fiuu's return page in the browser.
2. **Bring the existing screen back; don't start a new one.** The payment session lives in the Activity showing the SDK. If the link handler recreates it or clears the task, the session and its result are lost. Route the link to the existing Activity with `FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP`, or use `launchMode="singleTop"`. The demo app's [`DeepLinkActivity`](demo-app/android/app/src/main/kotlin/com/fiuu/xdkandroid/DeepLinkActivity.kt) is a working example.
3. **Don't read the result from the link.** When your app returns to the foreground, the SDK asks Fiuu for the status and delivers it through `onPaymentResult`.

A browser tab with Fiuu's return page stays open afterwards. That is expected.

## Troubleshooting

Look at `status` first, then `errorCode` and `message`.

| What you see | Likely cause | Fix |
|---|---|---|
| Checkout fails as soon as it opens, with a credentials or authentication message | Credentials don't match the environment. Sandbox credentials only work with `mp_build_mode = 4`; production or dev credentials only with `mp_build_mode = 2` | Check `mp_build_mode`, `mp_merchant_ID`, `mp_verification_key`, `mp_username` and `mp_password` together. Remember that no environment parameter means sandbox |
| `status = "INVALID CHANNEL"` | `mp_channel` is not enabled on your merchant account, or is misspelled | Use a name from [channel-list.md](https://github.com/FiuuPayment/Mobile-XDK-Fiuu_Examples/blob/master/channel-list.md) that Fiuu has enabled for you, or `"multi"` |
| `status = "NO_INTERNET_CONNECTION"` or `"NETWORK ERROR"` | The device is offline, or the connection dropped | Let the customer retry. If a payment page had already opened, call `resumeFiuuPaymentStatus` before charging again |
| `errorCode = "NO_WEBVIEW_BRIDGE"` | The SDK, or your `GooglePayHandler`, is not hosted in a `ComponentActivity` (for example a Flutter `FlutterActivity`) | Host it in a `ComponentActivity` (`AppCompatActivity`, `FlutterFragmentActivity`) |
| `errorCode = "GOOGLE_PAY_ERROR"`, or Google Pay says "This merchant is having trouble accepting your payment at the moment... [OR_BIBED_13]" | Your app has no Google Pay production access, or the environment and credentials don't match | Use `mp_build_mode = 4` with sandbox credentials until Google approves your app. See [Requirements for the production environment](#requirements-for-the-production-environment) |
| `status = "SECURITY_ERROR"`, `errorCode = "DEVICE_ROOTED"` | The device is rooted | The SDK does not run on rooted devices |
| `status = "408"`, `errorCode = "SDK_TIMEOUT"` | The 5-minute session ended before the SDK could confirm the status | Treat as pending and call `resumeFiuuPaymentStatus` later |
| Customer is left on Fiuu's return page after a wallet app | The return URL scheme is not registered, or doesn't match your merchant account | See [Returning from e-wallet apps](#returning-from-e-wallet-apps) |

Turn on `mp_dev_mode` while testing. The SDK then logs each request and response (with card data redacted) and warns about parameters it ignores. Turn it off for release builds.

If the problem stays, email support-sa@fiuu.com with your merchant ID, order ID, `status`, `errorCode` and `message`.

## Hybrid apps

Hybrid frameworks call the SDK through a small native bridge. On Android, add the JitPack dependency to the native Android project and call `ProvideFiuuPaymentSDK` from a `ComponentActivity` (in Flutter, `FlutterFragmentActivity`). For the iOS side, see [Mobile-XDK-Fiuu_Swift](https://github.com/FiuuPayment/Mobile-XDK-Fiuu_Swift).

[`demo-app/`](demo-app/) has working Flutter (`flutter_fiuu_xdk`) and Cordova (`cordova-plugin-fiuu-xdk`) plugins that do this. React Native and Capacitor follow the same pattern with a Native Module or a Capacitor plugin. Expo needs a development build (`expo prebuild` or EAS Build), because the managed workflow cannot load native code.

## Migrating from 3.x

4.0.0 is published at the same JitPack coordinates as the 3.x XDK, but it is a new SDK with a different entry point. Changing the version number alone is not enough. To stay on the old library, pin a 3.x version such as `3.34.46`.

### What changes

| | 3.x | 4.0.0 |
|---|---|---|
| Entry point | `Intent` to `com.fiuu.xdk.PaymentActivity` (or `ActivityGP` for Google Pay) | `ProvideFiuuPaymentSDK` composable |
| Parameters | `HashMap<Object, Object>` with `PaymentActivity.mp_*` constants | `Map<String, Any?>` with the same key strings |
| Result | JSON string in `XDKTransactionResult` | `PaymentResponseDto` object in `onPaymentResult` |
| Cancel | `RESULT_CANCELED` with `null` data | `onRequestClose` only, or `status = "USER_CANCELLED"` if a payment page was already open |
| `minSdk` | 26 | 29 |
| `compileSdk` | 34 | 35 (36 recommended) |
| Java bytecode | 8 (`VERSION_1_8`) | 17 |
| Android Gradle Plugin | 7.4.2 or later | 8.6.0 or later (8.9.1 for `compileSdk` 36) |
| Close from your code | `PaymentActivity.closePayment()` | No equivalent. Removing `ProvideFiuuPaymentSDK` from the composition ends the session without a result |
| Default backend | Production V2 (no `mp_core_env`) | **Sandbox** (no `mp_build_mode`/`mp_sandbox_mode`) |
| `mp_core_env = "1"` | Production V1 (retired) | Native engine |
| Manifest and ProGuard | Added by hand | Merged from the library |

Two of these can break a working app without a compile error:

- **Production is no longer the default.** A 3.x app with no `mp_core_env` went to production. In 4.0.0 the same app goes to sandbox. Set `mp_build_mode = 2` for production.
- **`mp_core_env = "1"` changed meaning.** In 3.x it was the retired V1 endpoint. Now it is the native engine.

### Step 1: update the build

1. Raise `minSdk` from 26 to 29 and `compileSdk` from 34 to at least 35 (36 recommended for Google Play). `compileSdk` 35 needs AGP 8.6.0 or later, 36 needs AGP 8.9.1 or later with Gradle 8.11.1, and AGP 8 needs JDK 17 to run. See [Requirements](#requirements).
2. Move your app from Java 8 to Java 17 bytecode. The 3.x setup used `VERSION_1_8`; the 4.0.0 library is built for 17, and Kotlin can't inline its code into a 1.8 target.

   ```kotlin
   android {
       compileOptions {
           sourceCompatibility = JavaVersion.VERSION_17
           targetCompatibility = JavaVersion.VERSION_17
       }
   }
   kotlin {
       compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
   }
   ```
3. Change the dependency to `4.0.0`. The `maven { url 'https://jitpack.io' }` repository from 3.x stays.
4. Enable Compose in the app module ([Setup, step 3](#3-enable-compose-in-your-app-module)).
5. You can remove the `-keep class com.fiuu.** { *; }` rule and the `wallet.api.enabled` meta-data you added for 3.x. The library supplies both. Keep the `@JavascriptInterface` rule if your own app uses one.

### Step 2: replace the Activity launch

A 3.x Java integration looked like this:

```java
Intent intent = new Intent(MainActivity.this, PaymentActivity.class);
intent.putExtra(PaymentActivity.XDKPaymentDetails, paymentDetails);
paymentActivityResultLauncher.launch(intent);
// ...
String transactionResult = data.getStringExtra(PaymentActivity.XDKTransactionResult);
```

If your app is in Java, keep your existing screens and add one Kotlin Activity that hosts the SDK. Start it from Java with a plain `Intent` and read the result back:

```kotlin
class FiuuCheckoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("UNCHECKED_CAST")
        val params = (intent.getSerializableExtra(EXTRA_PARAMS) as HashMap<String, Any?>)

        setContent {
            ProvideFiuuPaymentSDK(
                params = params,
                onPaymentResult = { result ->
                    setResult(RESULT_OK, Intent()
                        .putExtra(EXTRA_STATUS, result.status)
                        .putExtra(EXTRA_TRANSACTION_ID, result.transactionId ?: result.tranID)
                        .putExtra(EXTRA_ORDER_ID, result.referenceNo)
                        .putExtra(EXTRA_MESSAGE, result.message))
                },
                onRequestClose = { finish() },
            )
        }
    }

    companion object {
        const val EXTRA_PARAMS = "fiuu_params"
        const val EXTRA_STATUS = "fiuu_status"
        const val EXTRA_TRANSACTION_ID = "fiuu_transaction_id"
        const val EXTRA_ORDER_ID = "fiuu_order_id"
        const val EXTRA_MESSAGE = "fiuu_message"
    }
}
```

Declare it in your manifest with the `configChanges` from [Setup, step 4](#4-declare-configchanges-on-the-payment-activity). From Java:

```java
HashMap<String, Object> params = new HashMap<>();
params.put("mp_amount", "1.10");
// ... the rest of your existing keys
params.put("mp_channel", "multi");
params.put("mp_build_mode", 2); // production

Intent intent = new Intent(this, FiuuCheckoutActivity.class);
intent.putExtra(FiuuCheckoutActivity.EXTRA_PARAMS, params);
checkoutLauncher.launch(intent);
```

Your `PaymentActivity.mp_*` constants are plain strings equal to their names (`PaymentActivity.mp_amount` is `"mp_amount"`), so the keys and values in your existing map carry over. Change the map's key type to `String`.

### Step 3: read the new result

| 3.x JSON field | 4.0.0 `PaymentResponseDto` |
|---|---|
| `status_code` (`StatCode` for Google Pay) | `status` |
| `txn_ID` (`TranID`) | `transactionId` / `tranID` |
| `order_id` (`OrderID`) | `referenceNo` |
| `amount`, `channel`, `paydate` | `amount`, `channel`, `paydate` |
| `app_code` | `appcode` |
| `err_desc`, `error_code`, `error_desc` | `message`, `errorCode`, `errorDesc` |
| `chksum`, `VrfKey` | not returned |

The result no longer carries `chksum` or `VrfKey`. If your app checked them, move that check to your server and verify the payment from Fiuu's callback. Also handle the new statuses `"408"` and `"USER_CANCELLED"`, and call `resumeFiuuPaymentStatus` for `"22"` and `"408"` ([Handle the result](#handle-the-result)).

### Step 4: check parameters the native engine doesn't read

These 3.x parameters only work on the Webcore engine (`mp_core_env` `"2"`/`"3"`/`"4"`). The native engine ignores them and logs a warning when `mp_dev_mode` is on:

`mp_allowed_channels`, `mp_disabled_channels`, `mp_channel_editing`, `mp_language`, `mp_extended_vcode`, `mp_company`, `mp_enable_fullscreen`, `mp_gpay_channel`, `mp_hide_googlepay`, `mp_bin_lock`, `mp_bin_lock_err_msg`, `mp_card_list_disabled`, `mp_cash_waittime`, `mp_custom_css_url`, `mp_filter`, `mp_ga_enabled`, `mp_is_escrow`, `mp_is_recurring`, `mp_metadata`, `mp_non_3DS`, `mp_preferred_token`, `mp_request_type`, `mp_secured_verified`, `mp_tcctype`, `mp_transaction_id`, `mp_advanced_email_validation_enabled`, `mp_advanced_phone_validation_enabled`, `mp_dpa_id`

Two other differences:

- `mp_editing_enabled` now defaults to `false`, so Order Details are read-only unless you turn it on.
- For a single channel use `mp_channel` (with `mp_express_mode` to skip the SDK screens). The native engine has no allow-list of several channels.

If you depend on one of these parameters, run on Webcore while you move (next section).

### Moving in two releases (optional)

You can ship 4.0.0 without changing the checkout your customers see. With `mp_core_env = "2"` (production), `"3"` (UAT) or `"4"` (sandbox), `ProvideFiuuPaymentSDK` opens the same Webcore page as 3.x and passes your whole parameter map to it, so the parameters above keep working. The result still arrives as a `PaymentResponseDto`. When the customer closes the Webcore page without paying, you get only `onRequestClose`.

When you're ready, switch to the native engine by removing `mp_core_env` (or setting `"1"`) and setting `mp_build_mode`. Use the same credentials for the same environment.

### Migration checklist

- [ ] `minSdk` 29, `compileSdk` 35 or higher (36 recommended), matching AGP, Java 17, AndroidX, Compose enabled
- [ ] Calls to `PaymentActivity.closePayment()` removed
- [ ] Dependency on `4.0.0`
- [ ] `PaymentActivity`/`ActivityGP` launch replaced with `ProvideFiuuPaymentSDK` (or a host Activity around it)
- [ ] `mp_build_mode = 2` set for production builds
- [ ] `mp_core_env = "1"` from 3.x removed or reviewed
- [ ] Result code reads `status` and handles `00`, `11`, `22`, `408` and `USER_CANCELLED`
- [ ] `resumeFiuuPaymentStatus` called on app start
- [ ] Webcore-only parameters reviewed
- [ ] `configChanges` declared on the Activity showing the SDK
- [ ] E-wallet return scheme registered and routed to the existing Activity
- [ ] Payments confirmed on your server from Fiuu's callback
- [ ] Tested on sandbox (`mp_build_mode = 4`), then production

## For maintainers

`BuildInfo.kt` (`composeApp/src/commonMain/kotlin/com/fiuu/xdkandroid/utils/BuildInfo.kt`) holds the version:

- `SDK_VERSION_NAME`, for example `"4.0.0"`. `composeApp/build.gradle.kts` and `FiuuXDK.podspec` read it for the Maven and CocoaPods versions. Bump it before tagging.
- `SDK_VERSION_CODE`, `yyyyMMddNN`, bumped on every build so two builds with the same version name can be told apart. Gradle doesn't read it.

Both appear in the SDK's footer. Don't hardcode either value anywhere else.

Before tagging, compile Android and both iOS targets:

```bash
./gradlew :composeApp:assembleRelease \
          :composeApp:compileKotlinIosArm64 \
          :composeApp:compileKotlinIosSimulatorArm64
```

To build the binaries into the demo apps, run `./scripts/setup-demo-apps.sh` (`scripts\setup-demo-apps.bat` on Windows). See [`demo-app/README.md`](demo-app/README.md).

## Resources

- GitHub: https://github.com/FiuuPayment
- Website: https://fiuu.com/
- Twitter: https://twitter.com/FiuuPayment
- YouTube: https://www.youtube.com/FiuuPayment
- Facebook: https://www.facebook.com/FiuuPayment/
- Instagram: https://www.instagram.com/FiuuPayment/

## Support

Open an issue in this repository or email support-sa@fiuu.com.

| | |
|---|---|
| Merchant technical support | support-sa@fiuu.com |
| Sales and resellers | sales-sa@fiuu.com |
| Marketing campaigns | marketing-sa@fiuu.com |
| Channel and partners | channel-sa@fiuu.com |
| Media | media-sa@fiuu.com |
| Technical suggestions | technical-sa@fiuu.com |
| Abuse reports | abuse-sa@fiuu.com |

[fiuu.com](https://fiuu.com/) · [GitHub](https://github.com/FiuuPayment)
