# [Fiuu Mobile XDK] – Android Payment Library

<img alt="" src="https://user-images.githubusercontent.com/38641542/74424311-a9d64000-4e8c-11ea-8d80-d811cfe66972.jpg">

This is a fully functional Fiuu Android payment library designed for seamless integration for android native projects.
It can be integrated using the `com.fiuu.xdk` package via Gradle, sourced from [JitPack](https://jitpack.io/#FiuuPayment/Mobile-XDK-Fiuu_Android_Library).
For reference, this repository includes a sample application (`app` / `com.fiuu.xdkandroid`) that demonstrates integration with the Fiuu Android payment library (`paymentxdk` module).

**Current library version:** `3.34.38`

## Getting Started

For commercial use, you must be a registered Fiuu merchant.
To obtain your credentials for testing or production purposes, please contact us at sales@fiuu.com.

This library is designed for native Android projects using Android Studio.

For integration with other development frameworks, kindly refer to the following links :
* iOS : https://cocoapods.org/pods/fiuu-mobile-xdk-cocoapods
* Flutter : https://pub.dev/packages/fiuu_mobile_xdk_flutter
* React Native : https://www.npmjs.com/package/fiuu-mobile-xdk-reactnative
* Others : https://github.com/FiuuPayment

## Recommended configurations

When implementing Payment XDK into your project, use at least:

| Requirement | Minimum |
|-------------|---------|
| `minSdkVersion` | **26** |
| `compileSdk` | **37** |
| `targetSdkVersion` | **37** |
| JDK | **17** |
| Android Gradle Plugin (AGP) | **9.1.1** |
| Gradle | **9.0+** (sample uses **9.6.1**) |
| Android Studio | **Panda 3** / **2025.3.3 Patch 1** or newer (required for API 37) |

Notes:
* `minSdkVersion` **26** is required by the library. Your app cannot use a lower value.
* API / `compileSdk` **37** requires AGP **9.1.1+**. Older AGP versions are not supported for this library release.
* The sample project in this repository is built with AGP **9.2.1**, Gradle **9.6.1**, `compileSdk` / `targetSdk` **37**, and Java **17**.

## Installation Guidance

### Import Library

Add **JitPack** to your project repositories.

**settings.gradle** (or `settings.gradle.kts`):

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

Or, for projects still using root `build.gradle` / `allprojects`:

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

Add the dependency in your app module `build.gradle`:

```gradle
dependencies {
    implementation 'com.github.FiuuPayment:Mobile-XDK-Fiuu_Android_Library:3.34.38'
}
```

Replace `3.34.38` with the [latest release tag](https://github.com/FiuuPayment/Mobile-XDK-Fiuu_Android_Library/releases) if a newer version is available.

### AndroidManifest

Ensure your app declares internet access. Google Pay also requires the Wallet API meta-data:

```xml
<uses-permission android:name="android.permission.INTERNET" />

<application >
    <meta-data
        android:name="com.google.android.gms.wallet.api.enabled"
        android:value="true" />
</application>
```

### Import classes

Import the required library class:

```java
import com.fiuu.xdk.PaymentActivity;
```

Import these extra classes for Google Pay:

```java
import com.fiuu.xdk.googlepay.ActivityGP;
import com.fiuu.xdk.googlepay.UtilGP;

import com.google.android.gms.wallet.button.ButtonConstants;
import com.google.android.gms.wallet.button.ButtonOptions;
import com.google.android.gms.wallet.button.PayButton;
```

Other commonly required imports:

```java
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.util.HashMap;
```

### Setup Release Build

Add rules in `proguard-rules.pro`:

```proguard
# For WebView / Javascript bridge
# Keep all methods annotated with @JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep XDK classes (prevents obfuscation of JS bridge / payment result handling)
-keep class com.fiuu.xdk.** { *; }
```

### Show All Subscribed Channels (Default Page)

    HashMap<Object, Object> paymentDetails = new HashMap<>();

    private void restartpayment() {

        // Compulsory String. Values obtained from Fiuu.
        paymentDetails.put(PaymentActivity.mp_username, "");
        paymentDetails.put(PaymentActivity.mp_password, "");
        paymentDetails.put(PaymentActivity.mp_app_name, "");
        paymentDetails.put(PaymentActivity.mp_merchant_ID, "");
        paymentDetails.put(PaymentActivity.mp_verification_key, "");

        // Compulsory String. Payment info.
        paymentDetails.put(PaymentActivity.mp_amount, "1.10"); // 2 decimal points format
        paymentDetails.put(PaymentActivity.mp_order_ID, Calendar.getInstance().getTimeInMillis()); // Any unique alphanumeric String. For symbol only allowed hypen "-" and underscore "_"
        paymentDetails.put(PaymentActivity.mp_currency, "MYR");
        paymentDetails.put(PaymentActivity.mp_country, "MY");
        paymentDetails.put(PaymentActivity.mp_bill_description, "The bill description");
        paymentDetails.put(PaymentActivity.mp_bill_name, "The bill name");
        paymentDetails.put(PaymentActivity.mp_bill_email, "payer.email@fiuu.com");
        paymentDetails.put(PaymentActivity.mp_bill_mobile, "123456789");

        openStartActivityResult();
    }

    private void openStartActivityResult(){
        Intent intent = new Intent(MainActivity.this, PaymentActivity.class);
        intent.putExtra(PaymentActivity.XDKPaymentDetails, paymentDetails);
        paymentActivityResultLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> paymentActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() != null) {
                    Intent data = result.getData();
                    // Get the payment result in String
                    String transactionResult = data.getStringExtra(PaymentActivity.XDKTransactionResult);
                    Log.d(PaymentActivity.logXDK, "transaction result = " + transactionResult);
                } else {
                    // Data null handler
                }
            }
    );

### Express Mode

Required a valid mp_channel value, this will skip the payment info page and go direct to the payment screen.
Add mp_express_mode & set mp_channel as below : 

e.g. Express Mode to FPX Maybank online banking

    paymentDetails.put(PaymentActivity.mp_express_mode, true);
    paymentDetails.put(PaymentActivity.mp_channel, "maybank2u");

e.g. Express Mode to Touch 'n Go payment

    paymentDetails.put(PaymentActivity.mp_express_mode, true);
    paymentDetails.put(PaymentActivity.mp_channel, "TNG-EWALLET");

Find all mp_channel list here https://github.com/FiuuPayment/Mobile-XDK-Fiuu_Examples/blob/master/channel-list.md

NOTE: 
* Can only select subscribed mp_channel.
* credit channel cannot use express mode due to security reasons.

### Show Selected Channels Only

Need refer column mp_channel in https://github.com/FiuuPayment/Mobile-XDK-Fiuu_Examples/blob/master/channel-list.md
Then add the selected channels in allowedchannels[] e.g. :

        String allowedchannels[] = {"TNG-EWALLET","maybank2u"};
        paymentDetails.put(PaymentActivity.mp_allowed_channels, allowedchannels);

This will only show maybank2u & TNG-EWALLET channels in the channel listing.

### Others Optional Parameters

Learn more about optional parameters here https://github.com/FiuuPayment/Mobile-XDK-Fiuu_Android_Library/wiki/Installation-Guidance#prepare-the-payment-detail-object

        // -------------------------------- Most commonly used -------------------------------------

        // Optional, set Environment for Webview Core URL
        // paymentDetails.put(PaymentActivity.mp_core_env, "2"); //default = 2. Refer here for more info: https://github.com/FiuuPayment/Mobile-XDK-Fiuu_Android_Library?tab=readme-ov-file#environment-configuration

        // To pre-select channel, refer column mp_channel in https://github.com/FiuuPayment/Mobile-XDK-Fiuu_Examples/blob/master/channel-list.md
        // e.g. set mp_channel = credit to directly load required card info.
        paymentDetails.put(PaymentActivity.mp_channel, "credit");

        // Simulate offline payment (demo without actual charges).
        // Need set true for Google Pay Test Environment
        paymentDetails.put(PaymentActivity.mp_core_env, "4"); // sandbox

        // Set true if your account enabled extended Verify Payment
        paymentDetails.put(PaymentActivity.mp_extended_vcode, false);

        // Show close button (by default hidden)
        paymentDetails.put(PaymentActivity.mp_closebutton_display, true);

        // Allow change channel for pre-select mp_channel
        paymentDetails.put(PaymentActivity.mp_channel_editing, true);

        // Allow payer information editing.
        paymentDetails.put(PaymentActivity.mp_editing_enabled, false);

        // Explicitly force disable user input by field.
        paymentDetails.put(PaymentActivity.mp_bill_name_edit_disabled, true);
        paymentDetails.put(PaymentActivity.mp_bill_email_edit_disabled, false);
        paymentDetails.put(PaymentActivity.mp_bill_mobile_edit_disabled, true);
        paymentDetails.put(PaymentActivity.mp_bill_description_edit_disabled, false);

        // Set language : EN, MS, VI, TH, FIL, MY, KM, ID, ZH.
        paymentDetails.put(PaymentActivity.mp_language, "MS");

## Environment Configuration

The library supports multiple environments. You can configure which environment to use by setting the `mp_core_env` value.

`mp_core_env` is a string ("1", "2", "3", "4")

Each value maps to a specific environment base URL.

If no value is set, the default environment will be used.

| `mp_core_env` Value | Environment     | Base URL                            |
|---------------------|-----------------|-------------------------------------|
| `1`                 | Production - V1 | `https://pay.fiuu.com/RMS/API/xdk/` |
| `2`                 | Production - V2 | `https://xdk.fiuu.com/`             |
| `3`                 | UAT - V2        | `https://uat-xdk.fiuu.com/`         |
| `4`                 | Sandbox -V2     | `https://sandbox-xdk.fiuu.com/`     |
| *Default*           | Production - V2 | `https://xdk.fiuu.com/`             |

## Google Pay

### Requirements for Production Environment

In order to use Production or Dev account (actual transactions testing), Google Pay production access is required for your app package name.

Follow Google’s instructions to request production access for your app: https://developers.google.com/pay/api/android/guides/test-and-deploy/request-prod-access

Google Pay Console : https://pay.google.com/business/console

* Choose the integration type Gateway when prompted, and provide screenshots of your app for review.
* After your app has been approved, test your integration in production by set mp_core_env = "2" or just remove it & use production or Dev mp_verification_key & mp_merchant_ID.
* Then launching Google Pay from a signed, release build of your app.

Request Production Access Example 1 : 

![GPay Request Production 1](https://github.com/user-attachments/assets/3a9c9a77-72ce-4b3a-9cf6-294b9c579c52)

Request Production Access Example 2 :

![GPay Request Production 2](https://github.com/user-attachments/assets/8159c306-e1d0-4026-bae2-0c65dfb82aa0)

NOTE: Can only use TEST environment & Sandbox account if not yet get Google Pay production approval.

### Google Pay (Express Mode)

1) Create Google Pay button

Add Google Pay button in XML layout e.g. :

[//]: # (   TODO: For GPay e-Wallet payment method cannot use PayButton API Style & Personalization : https://developers.google.com/pay/api/android/guides/brand-guidelines)

    <ImageView
        android:id="@+id/btnGPay"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:src="@drawable/btn_gpay"
        android:layout_alignParentBottom="true"
        android:visibility="visible"/>

[//]: # (   NOTE: com.google.android.gms.wallet.button.PayButton only available for GPay Card payment method only)

    <com.google.android.gms.wallet.button.PayButton
        android:id="@+id/googlePayButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_alignParentBottom="true"
        android:layout_margin="19dp"
        android:visibility="gone"/>

In Java class add this in onCreate : 

[//]: # (      TODO: For GPay e-Wallet payment method cannot use PayButton API Style & Personalization : https://developers.google.com/pay/api/android/guides/brand-guidelines)
      
      ImageView btnGPay = findViewById(R.id.btnGPay);
      btnGPay.setOnClickListener(v -> {
      googlePayPayment();
      });

[//]: # (        NOTE: Below implementation only available to GPay Card payment method only)

        PayButton googlePayButton = findViewById(R.id.googlePayButton);

        try {
            googlePayButton.initialize(
                    ButtonOptions.newBuilder()
                            .setButtonTheme(ButtonConstants.ButtonTheme.DARK)
                            .setButtonType(ButtonConstants.ButtonType.PAY)
                            .setCornerRadius(99)
                            .setAllowedPaymentMethods(UtilGP.getAllowedPaymentMethods().toString())
                            .build()
            );
            googlePayButton.setOnClickListener(view -> {
                googlePayPayment();
            });
        } catch (JSONException e) {
            // Keep Google Pay button hidden (consider logging this to your app analytics service)
        }

2) Prepare paymentDetails

        HashMap<Object, Object> paymentDetails = new HashMap<>();

      /*
      TODO: Follow Google’s instructions to request production access for your app: https://developers.google.com/pay/api/android/guides/test-and-deploy/request-prod-access
       *
      Choose the integration type Gateway when prompted, and provide screenshots of your app for review.
      After your app has been approved, test your integration in production by set mp_core_env = "2" or just remove it & use production / Dev mp_verification_key & mp_merchant_ID.
      Then launching Google Pay from a signed, release build of your app.
      */

        private void googlePayPayment() {
                paymentDetails = new HashMap<>();

                // Compulsory String. Values obtained from Fiuu.
                paymentDetails.put(PaymentActivity.mp_merchant_ID, ""); // Sandbox ID for TEST environment & Production/Dev ID once Google approved production access
                paymentDetails.put(PaymentActivity.mp_verification_key, ""); // Sandbox ID for TEST environment & Production/Dev ID once Google approved production access
                
                // Compulsory String. Payment info.
                paymentDetails.put(PaymentActivity.mp_amount, "1.01"); // 2 decimal points format
                paymentDetails.put(PaymentActivity.mp_order_ID, Calendar.getInstance().getTimeInMillis()); // Any unique alphanumeric String. For symbol only allowed hypen "-" and underscore "_"
                paymentDetails.put(PaymentActivity.mp_currency, "MYR");
                paymentDetails.put(PaymentActivity.mp_country, "MY");
                paymentDetails.put(PaymentActivity.mp_bill_description, "The bill description");
                paymentDetails.put(PaymentActivity.mp_bill_name, "The bill name");
                paymentDetails.put(PaymentActivity.mp_bill_email, "payer.email@fiuu.com");
                paymentDetails.put(PaymentActivity.mp_bill_mobile, "123456789");
        
                paymentDetails.put(PaymentActivity.mp_core_env, "4"); // 4 = Test Environment & Default/2 = production (required Google Pay production access approval)
                
                // GPay payment methods setting examples : (by default will show all payment methods)
                paymentDetails.put(PaymentActivity.mp_gpay_channel, new String[] { "CC", "TNG-EWALLET" }); // Enable Card & TNG eWallet Only
                paymentDetails.put(PaymentActivity.mp_gpay_channel, new String[] { "SHOPEEPAY", "TNG-EWALLET" }); // Enable ShopeePay & TNG eWallet Only
                // NOTE: SHOPEEPAY & TNG-EWALLET only applicable to MY & MYR. Others currency & country only supported CC.

                // Optional
                paymentDetails.put(PaymentActivity.mp_company, "Your Company Name"); // Show merchant name in Google Pay
                paymentDetails.put(PaymentActivity.mp_closebutton_display, true); // Enable close button
                paymentDetails.put(PaymentActivity.mp_enable_fullscreen, true); //enable fullscreen
                paymentDetails.put(PaymentActivity.mp_extended_vcode, false); // Set true if your account enabled extended Verify Payment
                paymentDetails.put(PaymentActivity.mp_hide_googlepay, true); // Optional : Hide Google Pay button (by default false)

                openGPActivityWithResult();
        }

3) Start payment by sending paymentDetails to ActivityGP.class

       private void openGPActivityWithResult() {
            Intent intent = new Intent(MainActivity.this, ActivityGP.class); // Used ActivityGP for Google Pay
            intent.putExtra(PaymentActivity.XDKPaymentDetails, paymentDetails);
            gpActivityResultLauncher.launch(intent);
       }

4) Handle activity result listener

         ActivityResultLauncher<Intent> gpActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() != null) {
                    Intent data = result.getData();
                    // Get payment result in String
                    String transactionResult = data.getStringExtra(PaymentActivity.XDKTransactionResult);
                    Log.d("logGooglePay", "transactionResult = " + transactionResult);
                } else {
                    // If payment cancelled
                    Log.d("logGooglePay" , "RESULT_CANCELED data == null");
                }
            }
         );

### Google Pay (Non-Express Mode)

Same with Show All Subscribed Channels in above.

Just need to control these parameters :

        paymentDetails.put(PaymentActivity.mp_merchant_ID, ""); // Sandbox ID for TEST environment & Production/Dev ID once Google approved production access
        paymentDetails.put(PaymentActivity.mp_verification_key, ""); // Sandbox ID for TEST environment & Production/Dev ID once Google approved production access
        paymentDetails.put(PaymentActivity.mp_core_env, "4"); // 4 = Test Environment & Default/2 = production (required Google Pay production access approval)

         // Optional
         paymentDetails.put(PaymentActivity.mp_company, "Your Company Name"); // Show merchant name in Google Pay
         paymentDetails.put(PaymentActivity.mp_closebutton_display, true); // Enable close button
         paymentDetails.put(PaymentActivity.mp_enable_fullscreen, true); //enable fullscreen
         paymentDetails.put(PaymentActivity.mp_extended_vcode, false); // Set true if your account enabled extended Verify Payment
         paymentDetails.put(PaymentActivity.mp_hide_googlepay, true); // Optional : Hide Google Pay button (by default false)

         // MY (MYR) GPay payment methods setting examples : (by default will show all payment methods)
         paymentDetails.put(PaymentActivity.mp_gpay_channel, new String[] { "CC", "TNG-EWALLET" }); // Enable Card & TNG eWallet Only
         paymentDetails.put(PaymentActivity.mp_gpay_channel, new String[] { "SHOPEEPAY", "TNG-EWALLET" }); // Enable ShopeePay & TNG eWallet Only
         // NOTE: SHOPEEPAY & TNG-EWALLET only applicable to MY & MYR. Others currency & country only supported CC.

## Payment Results

### Google Pay Payment Results

    =========================================
    Sample transaction result in JSON string:
    =========================================

    {
      "StatCode": "00",
      "StatName": "captured",
      "TranID": "30959687",
      "Amount": "1.10",
      "Domain": "",
      "VrfKey": "",
      "Channel": "credit",
      "OrderID": "1741853136969",
      "Currency": "MYR",
      "ErrorCode": null,
      "ErrorDesc": null,
      "ProcessorResponseCode": "000",
      "ProcessorCVVResponse": null,
      "SchemeTransactionID": "123456",
      "MerchantAdviceCode": null,
      "ECI": null,
      "3DSVersion": "2.2",
      "ACSTransactionID": null,
      "3DSTransactionID": null
    }

    Parameter and meaning:
    
    "StatCode" - "00" for Success, "11" for Failed, "22" for Pending. 
    
    "Amount" - The transaction amount
    "OrderID" - The transaction order ID
    "Channel" - The card type use
    "TranID" - The transaction ID generated by Fiuu
    "Domain" - Your Merchant ID

    "VrfKey" - You can verify payment using this formula -> VrfKey = md5(Amount+secret_key+Domain+TranID+StatCode)
    
    * Note 1: secret_key = Your account Secret Key in https://portal.fiuu.com/
    * Note 2: The other parameters and values not described above are for recorded purpose only

    =====================================
    * Sample error result in JSON string:
    =====================================
    
    {
        "status":false,
        "error_code":"P03",
        "error_desc":"Your payment info format not correct."
    }

    {
        "error_code" = A01;
        "error_desc" = "Fail to detokenize Google Pay Token given";
        status = 0;
    }

    {
      "status": false,
      "error_code": null,
      "error_desc": "Your transaction has been denied due to merchant account issue."
    }
    
    Error info:

    Error P03 - Your payment info format not correct   
    1) Need makesure all required parameters filled correctly.
    2) Need set mp_extended_vcode = true if enabled extended Verify Payment.

    Error A01 - "Fail to detokenize Google Pay Token given" - Error starting a payment process due to several possible reasons, please contact Fiuu support if the error unresolved.
    1) Misconfigure GooglePay setup
    2) API credentials (username, password, merchant id, verify key)
    3) Payment Server Offline.

    "Your transaction has been denied due to merchant account issue."
    OR
    "This merchant is having trouble accepting your payment at the moment.Try installing the latest version of the merchant's app or use a different payment method. [OR_BIBED_13]"
    1) Need check mp_core_env, mp_merchant_ID & mp_verification_key
    2) mp_core_env = 4 , need use Sandbox mp_merchant_ID & mp_verification_key
    3) mp_core_env = 2 or if did not send mp_core_env , need use Production/Dev mp_merchant_ID & mp_verification_key
    4) Check Requirements for Production Environment notes for production/dev testing

### Other Channels Payment Results

Sample transaction result in JSON string:

    {
      "txn_ID": "2754048669",
      "paydate": 1741858781,
      "order_id": "1741858760492",
      "amount": "1.00",
      "status_code": "00",
      "channel": "THE_CHANNEL_USED",
      "err_desc": "",
      "app_code": "",
      "chksum": "abcdefghijklmnopqrstuvwxyz1234567890",
      "pInstruction": 0,
      "msgType": "C6",
      "mp_secured_verified": false
    }

Transaction result info:

      "status_code" - "00" for Success, "11" for Failed, "22" for Pending. (Pending status only applicable to cash channels only)
      "amount" - The transaction amount
      "paydate" - The transaction date
      "order_id" - The transaction order id
      "channel" - The transaction channel description
      "txn_ID" - The transaction id generated by Fiuu
      "chksum" - MD5(mp_merchant_ID + msgType + txn_ID + amount + status_code + secret_key)

      * Note 1: secret_key = Your account Secret Key in https://portal.fiuu.com/
      * Note 2: The other parameters and values not described above are for recorded purpose only

## Resources

- GitHub:     https://github.com/FiuuPayment
- Website:    https://fiuu.com/
- Twitter:    https://twitter.com/FiuuPayment
- YouTube:    https://www.youtube.com/FiuuPayment
- Facebook:   https://www.facebook.com/FiuuPayment/
- Instagram:  https://www.instagram.com/FiuuPayment/

## Support

Submit issue to this repository or email to our support@fiuu.com

Merchant Technical Support / Customer Care : support@fiuu.com<br>
Sales/Reseller Enquiry : sales@fiuu.com<br>
Marketing Campaign : marketing@fiuu.com<br>
Channel/Partner Enquiry : channel@fiuu.com<br>
Media Contact : media@fiuu.com<br>
R&D and Tech-related Suggestion : technical@fiuu.com<br>
Abuse Reporting : abuse@fiuu.com