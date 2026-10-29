# Fiuu Android XDK sample

Sample app for merchants to try the Fiuu Android XDK **4.0.0** with their own credentials.

This repository does not contain the SDK. The app depends on the library already published on JitPack. Pushing this repository updates the sample only.

## Dependency

```kotlin
implementation("com.github.fiuu-mobile:Mobile-XDK-Fiuu_Android_Library:4.0.0")
```

`maven("https://jitpack.io")` is already in `settings.gradle.kts`. Version `4.0.0` is the published SDK (git tag `4.0.0`).

## What you need

- Android Studio, JDK 17, and an Android device or emulator (min SDK 29).
- Fiuu credentials for the environment you will test: merchant ID, verification key, username, and password. For sandbox or production credentials, contact sales-sa@fiuu.com.
- If you test a wallet that returns to the app, set the return URL scheme on the merchant account to `superapp`. The sample registers that scheme in `DeepLinkActivity`.

## Add your credentials

1. Copy the example file:

   ```bash
   cp .env.example .env
   ```

2. Fill in one environment block in `.env`. A payment needs `MERCHANT_ID`, `VERIFICATION_KEY`, `USERNAME`, and `PASSWORD`. `APP_NAME` defaults to `mobile`. `GP_MERCHANT_ID` is only for Google Pay.

3. Rebuild. Gradle reads `.env` while compiling and copies the values into the app. Editing `.env` after a build does nothing until the next build.

`.env` is gitignored. You can also type credentials on the Merchant screen; choosing Sandbox, UAT, or Production again reloads that preset from the last build.

| Preset | `mp_build_mode` | Use credentials for |
|---|---|---|
| Sandbox | `4` | Sandbox |
| UAT | `3` | UAT |
| Production | `2` | Production or dev |

Sandbox credentials only work with Sandbox. Production credentials only work with Production.

## Run

Open this folder in Android Studio and run the `app` configuration, or:

```bash
./gradlew :app:assembleDebug
```

On the Merchant screen, pick the environment that matches your `.env` block, then start a payment from the payment screen. The amount defaults to `1.01` MYR and the channel defaults to `multi`.
