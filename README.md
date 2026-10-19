FiuuXDKLibrary is a **Kotlin Multiplatform (KMP)** SDK targeting **Android + iOS**.

## Project layout

- **`composeApp/`**: the publishable KMP library (Android AAR + iOS framework).
- **`sampleAndroidApp/`**: Android sample app that uses the library.
- **`iosApp/`**: iOS sample app (SwiftUI) that uses the generated `ComposeApp` framework.

## Android usage (via JitPack)

### 1) Add JitPack repository

In your app's `settings.gradle(.kts)`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

### 2) Add the dependency

In your Android app module `build.gradle(.kts)`:

```kotlin
dependencies {
    implementation("com.github.FiuuPayment:Mobile-XDK-Fiuu_Android_Library:<TAG_OR_VERSION>")
}
```

> Replace `<TAG_OR_VERSION>` with your Git tag, e.g. `1.0.4` or `v1.0.4` depending on how you tagged in GitHub/JitPack.

### Bumping the SDK version

`BuildInfo.kt` (`composeApp/src/commonMain/kotlin/com/fiuu/xdkandroid/utils/BuildInfo.kt`) is the
single source of truth for the library version, split like an Android app's `versionName`/`versionCode`:

- `SDK_VERSION_NAME` (e.g. `"1.0.4"`) — the semantic version. Shown in the SDK's own payment UI
  footer ("Version ...") and parsed by `composeApp/build.gradle.kts` at configure time to set the
  Maven publish version — bump this before tagging a release.
- `SDK_VERSION_CODE` (e.g. `2026091501L`, `yyyyMMddNN`) — an internal build identifier, bumped on
  every build so two builds sharing the same `SDK_VERSION_NAME` can still be told apart (e.g. by
  support/QA on Sandbox/UAT). Shown alongside the version in the UI footer; never read by Gradle
  and never part of the Maven publish version.

Don't hardcode either value anywhere else.

### 3) Use the SDK from Compose

From your Android UI:

```kotlin
val paymentParams = mapOf<String, Any?>(
    "mp_amount" to "1.01",
    "mp_order_ID" to "ORD-123456",
    "mp_currency" to "MYR",
    "mp_country" to "MY",
    "mp_merchant_ID" to "your_merchant_id",
    "mp_verification_key" to "your_verification_key",
    "mp_username" to "your_username",
    "mp_password" to "your_password",
    "mp_core_env" to "1", // "1" = Modern Native Compose (Default), "2".."4" = Legacy Webcore
    "mp_build_mode" to 4  // Native backend: 2 = production, 3 = UAT, 4 = sandbox
)

ProvideFiuuPaymentSDK(
    params = paymentParams,
    onPaymentResult = { result: PaymentResponseDto ->
        // Handle result (result.status == "00" indicates success)
    },
    onRequestClose = {
        // Handle user cancellation / sheet dismissal
    }
)
```

**Pending results.** A `result.status` of `"22"` means the payment was still pending when the SDK
closed (for example, the customer hadn't finished approving in their e-wallet app). `"408"` means
the 5-minute session expired before the SDK could confirm the status. In both cases the SDK keeps
the session, so call `resumeFiuuPaymentStatus` later (for example, the next time the app opens) to
get the final status.

**Declare `configChanges` on the hosting Activity.** The SDK keeps its payment state in the
composition, so if Android recreates the Activity mid-payment (rotation, dark-mode toggle, font
scale, multi-window), the in-flight attempt is cancelled and the SDK starts over. Declare the
changes on the Activity that calls `ProvideFiuuPaymentSDK` so Compose handles them in place:

```xml
<activity
    android:name=".YourPaymentActivity"
    android:configChanges="orientation|keyboardHidden|keyboard|screenSize|smallestScreenSize|locale|layoutDirection|fontScale|screenLayout|density|uiMode" />
```

The Flutter and Cordova plugins already declare this on their own payment activity.

## Dual-Engine Architecture (`mp_core_env`)

`FiuuXDKLibrary` supports dual payment execution engines to allow a smooth transition from legacy WebCore integrations to modern Compose Multiplatform native interfaces:

| `mp_core_env` | Mode | Engine / Technology | Target URL |
|---|---|---|---|
| **`"1"`** *(Default)* | **XDK Native** | Compose Multiplatform + Direct REST APIs | `https://sandbox-mobile.fiuu.com/pay/` (or UAT/Prod) |
| **`"4"`** | **Webcore (Legacy V2)** | WebCore Angular SPA (Sandbox) | `https://sandbox-xdk.fiuu.com/` |
| **`"3"`** | **Webcore (Legacy V2)** | WebCore Angular SPA (UAT) | `https://uat-xdk.fiuu.com/` |
| **`"2"`** | **Webcore (Legacy V2)** | WebCore Angular SPA (Production) | `https://xdk.fiuu.com/` |

> [!NOTE]
> **Clean 1–4 Schema**: The legacy V1 RMS endpoint has been sunset, and **`mp_core_env = "1"`** is now designated as the **Modern Native Engine**. Developers can seamlessly integrate `mp_core_env = "1"` for modern native checkout, or `"2"`, `"3"`, `"4"` for legacy WebCore compatibility. Omitted or null values automatically default to `"1"`.

### Choosing the environment

The backend is resolved once, in this order (the first one that picks a value wins):

1. **`mp_core_env`** `"2"` / `"3"` / `"4"`: Webcore production / UAT / sandbox.
2. **`mp_build_mode`** `2` / `3` / `4`: native engine production / UAT / sandbox (`Int` or numeric `String`).
3. **`mp_sandbox_mode`**: `false` means production. `true`, absent or unrecognized means sandbox.

Invalid values fall through to the next step, and the SDK logs a warning. The same result also selects the
Google Pay environment, which is `ENVIRONMENT_PRODUCTION` only for production. `mp_dev_mode` controls logging
only and never changes the backend.

## Google Pay setup

Google Pay Express mode works out of the box once you add the dependency — the required
`com.google.android.gms.wallet.api.enabled` manifest entry and R8 keep rules are bundled with the
library and merge into your app automatically. You still need to do the following on your side,
since Google Pay production access is tied to your app's own identity, not this library's:

1. **Register your app with the [Google Pay & Wallet Console](https://pay.google.com/business/console)** under your own package name and release-signing SHA-1 fingerprint. This is required before Google Pay will work in production for your app.
2. **Control sandbox vs. production** with the environment params (see [Choosing the environment](#choosing-the-environment)). The Google Pay environment always follows the resolved backend:
   - `mp_build_mode = 2` (or `mp_sandbox_mode = false` with no `mp_build_mode`): production (Google Pay `ENVIRONMENT_PRODUCTION` + the live Fiuu API).
   - `mp_build_mode = 3` / `4`, or nothing set: UAT / sandbox with Google Pay `ENVIRONMENT_TEST` (the default, so a forgotten flag never accidentally processes real money).
   - Pass the same build mode to `GooglePayHandler.isReadyToPay(buildMode, …)`.
3. **Host `presentGooglePayExpressButton` from a `ComponentActivity`** (e.g. `AppCompatActivity`, or `FlutterFragmentActivity` for Flutter — not a plain `FlutterActivity`). Cards that need 3DS come back from `/express` as a redirect, and the SDK opens that page on the activity you passed to `GooglePayHandler`; without a `ComponentActivity` the payment fails with `NO_WEBVIEW_BRIDGE`.

No other setup is required — the manifest meta-data and ProGuard/R8 rules ship with the library.

## Samsung Pay setup

The Samsung Pay SDK is **not** bundled in the AAR — Samsung distributes it to registered partners
through the [Samsung Developer portal](https://developer.samsung.com/pay), and a bundled copy
would clash with your own. The library only compiles against it, so to offer Samsung Pay:

1. Download the Samsung Pay SDK (version **2.22.00 or later**) from the Samsung Developer portal.
2. Put the jar in your app module's `libs/` folder and add it:
   ```kotlin
   dependencies {
       implementation(files("libs/samsungpay_2.22.00.jar"))
   }
   ```

Without the jar the SDK still works — Samsung Pay is just reported as unavailable, so its tile
is hidden and the express button returns an error. The manifest entries Samsung Pay needs and
the R8 rules ship with the library.

> **Confirm payments on your server.** The result the SDK returns to your app (including a
> pending `22` if the gateway hasn't settled yet) is for your UI. Always confirm the final status
> on your own backend via Fiuu's server-to-server notification before fulfilling an order.

## Returning from e-wallet apps (app-to-app)

Some channels (e.g. Touch 'n Go eWallet) hand the user off to the wallet app to approve the
payment. Once it's approved, the wallet opens Fiuu's return page, which redirects to the
**return URL scheme configured for your merchant account with Fiuu** (for example
`yourapp://`). That link is what brings the user back to your app, so your app has to handle it:

1. **Register the scheme.** It must match the one configured for your merchant account exactly.
   If it isn't registered, the user is left on Fiuu's return page in the browser.
   - Android: an `intent-filter` with `VIEW`, `DEFAULT` and `BROWSABLE` plus
     `<data android:scheme="yourapp" />`.
   - iOS: a `CFBundleURLTypes` entry in `Info.plist`.
2. **Bring back the existing screen. Don't start a new one.** The payment session lives in the
   activity or view controller that's showing the SDK. If the deep link handler recreates it or
   clears the task, the session and its result are lost. On Android, route the link to the
   existing activity with `FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP` (or
   `launchMode="singleTop"`). The demo app's
   [`DeepLinkActivity`](demo-app/android/app/src/main/kotlin/com/fiuu/xdkandroid/DeepLinkActivity.kt)
   is a working example.
3. **Don't read the payment result from the link.** You don't need to. When your app comes back
   to the foreground, the SDK checks the payment status with Fiuu itself and delivers the result
   through `onPaymentResult` as usual.

The return opens Fiuu's page in the user's browser on the way back, so a browser tab stays open
afterwards. That's expected.

## iOS usage (via the generated KMP framework)

iOS does **not** consume this SDK via JitPack. For iOS, you integrate the KMP framework (`ComposeApp`) built from the project.

### Option A (recommended for this repo): run the sample iOS app

1) Open `iosApp/iosApp.xcodeproj` in Xcode
2) Select the `iosApp` scheme
3) Build/Run

The project includes a build phase that runs:

- `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`

This generates and links the `ComposeApp.framework` automatically.

### Calling Kotlin from Swift

In Swift, import the framework and call the exported function from `startFiuuPaymentIos.kt`:

```swift
import SwiftUI
import UIKit
import ComposeApp

let params: [AnyHashable: Any] = [
    "payment_url": "https://your-payment-url.com/..."
]

let vc = StartFiuuPaymentIosKt.startFiuuPaymentIos(paymentParams: params)
rootVC.present(vc, animated: true)
```

## Native iOS (Swift) integration example

### SwiftUI button → present the payment UIViewController

```swift
import SwiftUI
import UIKit
import ComposeApp

struct ContentView: View {
    var body: some View {
        Button("Start Fiuu Payment") {
            guard let rootVC = UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene })
                .first?.windows.first?.rootViewController
            else { return }

            let params: [AnyHashable: Any] = [
                "payment_url": "https://your-payment-url.com/..."
            ]

            let vc = StartFiuuPaymentIosKt.startFiuuPaymentIos(paymentParams: params)
            rootVC.present(vc, animated: true)
        }
    }
}
```

## Hybrid apps

Hybrid frameworks (Flutter / React Native / Capacitor) typically call into native code.
The recommended approach is:

- **Android**: add the SDK in the Android host app (Gradle), then expose a small native bridge (MethodChannel / Native Module / Capacitor Plugin) that triggers `FiuuPaymentSDK`.
- **iOS**: integrate the `ComposeApp` framework (Xcode), then expose a small native bridge that presents `StartFiuuPaymentIosKt.startFiuuPaymentIos(...)`.

### Flutter (MethodChannel)

- **Android**: add the JitPack dependency in `android/app/build.gradle(.kts)` and implement a `MethodChannel` handler that launches your payment UI.
- **iOS**: integrate the `ComposeApp` framework into `Runner.xcodeproj`, then in `AppDelegate.swift` handle the channel method and present the `UIViewController` returned by `StartFiuuPaymentIosKt.startFiuuPaymentIos(...)`.

### React Native (CLI) (Native Module)

- **Android**: add the dependency in `android/app/build.gradle`, create a `NativeModule` that shows the payment UI.
- **iOS**: add `ComposeApp` framework to the Xcode project, create an Objective‑C/Swift native module that presents the payment `UIViewController`.

### Expo (managed) / Expo Dev Client

Expo managed workflow cannot use custom native code unless you:
- use **Expo Dev Client** (`expo prebuild`) or
- build via **EAS Build** with native modules.

Once you’re in a prebuild/dev-client workflow, follow the same approach as React Native CLI above.

### Ionic Capacitor

- **Android**: add the dependency in the native Android project under `android/` and write a Capacitor plugin that triggers `FiuuPaymentSDK`.
- **iOS**: add `ComposeApp` framework under `ios/` and write a Capacitor plugin that presents `StartFiuuPaymentIosKt.startFiuuPaymentIos(...)`.

### Preflight build (useful before tagging / JitPack)

Run these locally to catch iOS compilation issues early:

```bash
./gradlew :composeApp:assembleRelease \
         :composeApp:compileKotlinIosArm64 \
         :composeApp:compileKotlinIosSimulatorArm64
```

---

Learn more about Kotlin Multiplatform: `https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html`
The project includes a build phase that runs:
- `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`
This generates and links the `ComposeApp.framework` automatically.
### Calling Kotlin from Swift
In Swift, import the framework and call the exported function from `startFiuuPaymentIos.kt`:
```swift
import SwiftUI
import UIKit
import ComposeApp
let params: [AnyHashable: Any] = [
    "payment_url": "https://your-payment-url.com/..."
]
let vc = StartFiuuPaymentIosKt.startFiuuPaymentIos(paymentParams: params)
rootVC.present(vc, animated: true)
```
## Native iOS (Swift) integration example
### SwiftUI button → present the payment UIViewController
```swift
import SwiftUI
import UIKit
import ComposeApp
struct ContentView: View {
    var body: some View {
        Button("Start Fiuu Payment") {
            guard let rootVC = UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene })
                .first?.windows.first?.rootViewController
            else { return }
            let params: [AnyHashable: Any] = [
                "payment_url": "https://your-payment-url.com/..."
            ]
            let vc = StartFiuuPaymentIosKt.startFiuuPaymentIos(paymentParams: params)
            rootVC.present(vc, animated: true)
        }
    }
}
```
## Hybrid apps
Hybrid frameworks (Flutter / React Native / Capacitor) typically call into native code.
The recommended approach is:
- **Android**: add the SDK in the Android host app (Gradle), then expose a small native bridge (MethodChannel / Native Module / Capacitor Plugin) that triggers `FiuuPaymentSDK`.
- **iOS**: integrate the `ComposeApp` framework (Xcode), then expose a small native bridge that presents `StartFiuuPaymentIosKt.startFiuuPaymentIos(...)`.
### Flutter (MethodChannel)
- **Android**: add the JitPack dependency in `android/app/build.gradle(.kts)` and implement a `MethodChannel` handler that launches your payment UI.
- **iOS**: integrate the `ComposeApp` framework into `Runner.xcodeproj`, then in `AppDelegate.swift` handle the channel method and present the `UIViewController` returned by `StartFiuuPaymentIosKt.startFiuuPaymentIos(...)`.
### React Native (CLI) (Native Module)
- **Android**: add the dependency in `android/app/build.gradle`, create a `NativeModule` that shows the payment UI.
- **iOS**: add `ComposeApp` framework to the Xcode project, create an Objective‑C/Swift native module that presents the payment `UIViewController`.
### Expo (managed) / Expo Dev Client
Expo managed workflow cannot use custom native code unless you:
- use **Expo Dev Client** (`expo prebuild`) or
- build via **EAS Build** with native modules.
Once you’re in a prebuild/dev-client workflow, follow the same approach as React Native CLI above.
### Ionic Capacitor

- **Android**: add the dependency in the native Android project under `android/` and write a Capacitor plugin that triggers `FiuuPaymentSDK`.
- **iOS**: add `ComposeApp` framework under `ios/` and write a Capacitor plugin that presents `StartFiuuPaymentIosKt.startFiuuPaymentIos(...)`.

### Preflight build (useful before tagging / JitPack)

Run these locally to catch iOS compilation issues early:

```bash
./gradlew :composeApp:assembleRelease \
         :composeApp:compileKotlinIosArm64 \
         :composeApp:compileKotlinIosSimulatorArm64
```

---

Learn more about Kotlin Multiplatform: `https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html`