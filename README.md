# [Fiuu Mobile XDK] – Android Payment Library

<img alt="" src="https://user-images.githubusercontent.com/38641542/74424311-a9d64000-4e8c-11ea-8d80-d811cfe66972.jpg">

This is a fully functional Fiuu Android payment library designed for seamless integration for android native projects.
It can be integrated using the com.molpay.molpayxdk package via Gradle, sourced from Maven repository.
For reference, we have included a sample application (com.fiuu.xdkandroid) that demonstrates the integration process with the Fiuu Android payment library.

## Getting Started

For commercial use, you must be a registered Fiuu merchant.
To obtain your credentials for testing or production purposes, please contact us at sales-sa@fiuu.com.

This library is designed for native Android projects using Android Studio.

For integration with other development frameworks, kindly refer to the following links :
a) iOS : https://cocoapods.org/pods/fiuu-mobile-xdk-cocoapods
b) Flutter : https://pub.dev/packages/fiuu_mobile_xdk_flutter
c) React Native : https://www.npmjs.com/package/fiuu-mobile-xdk-reactnative
d) Others : https://github.com/FiuuPayment

## Recommended configurations

When implementing Payment XDK into your project, consider the following:

- compileSdk: >= 34

- targetSdkVersion: >= 34

- minSdkVersion: >= 26

- Android Gradle Plugin >= 7.4.2

## Installation Guidance

### Import Library

Set maven google & mavenCentral in build.gradle (Project:) level

      buildscript {
         repositories {
            mavenCentral()
            maven {
               url 'https://maven.google.com/'
               name 'Google'
            }
            google()
         }
         dependencies {
            classpath 'com.android.tools.build:gradle:7.4.2'
            classpath 'com.github.dcendents:android-maven-gradle-plugin:1.3'
         }
      }

      allprojects {
         repositories {
            mavenCentral()
            maven {
               url 'https://maven.google.com/'
               name 'Google'
            }
         }
      }

Add dependencies in build.gradle (Module :app)

        dependencies {
                implementation 'com.github.FiuuPayment:Mobile-XDK-Fiuu_Android_Library:<latest_version>'
        }

Import the required library class

        import com.molpay.molpayxdk.MOLPayActivity;

Import these extra classes for Google Pay

        import com.molpay.molpayxdk.googlepay.ActivityGP;
        import com.molpay.molpayxdk.googlepay.UtilGP;

        import com.google.android.gms.wallet.button.ButtonConstants;
        import com.google.android.gms.wallet.button.ButtonOptions;
        import com.google.android.gms.wallet.button.PayButton;

Import the others required library classes for your class e.g.

        import androidx.activity.result.ActivityResultLauncher;
        import androidx.activity.result.contract.ActivityResultContracts;
        
        import java.util.HashMap;

### Show All Subscribed Channels (Default Page)

    HashMap<Object, Object> paymentDetails = new HashMap<>();

    private void restartmolpay() {

        // Compulsory String. Values obtained from Fiuu.
        paymentDetails.put(MOLPayActivity.mp_username, "");
        paymentDetails.put(MOLPayActivity.mp_password, "");
        paymentDetails.put(MOLPayActivity.mp_app_name, "");
        paymentDetails.put(MOLPayActivity.mp_merchant_ID, "");
        paymentDetails.put(MOLPayActivity.mp_verification_key, "");

        // Compulsory String. Payment info.
        paymentDetails.put(MOLPayActivity.mp_amount, "1.10"); // 2 decimal points format
        paymentDetails.put(MOLPayActivity.mp_order_ID, Calendar.getInstance().getTimeInMillis()); // Any unique alphanumeric String. For symbol only allowed hypen "-" and underscore "_"
        paymentDetails.put(MOLPayActivity.mp_currency, "MYR");
        paymentDetails.put(MOLPayActivity.mp_country, "MY");
        paymentDetails.put(MOLPayActivity.mp_bill_description, "The bill description");
        paymentDetails.put(MOLPayActivity.mp_bill_name, "The bill name");
        paymentDetails.put(MOLPayActivity.mp_bill_email, "payer.email@fiuu.com");
        paymentDetails.put(MOLPayActivity.mp_bill_mobile, "123456789");

        openStartActivityResult();
    }

    private void openStartActivityResult(){
        Intent intent = new Intent(MainActivity.this, MOLPayActivity.class);
        intent.putExtra(MOLPayActivity.MOLPayPaymentDetails, paymentDetails);
        paymentActivityResultLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> paymentActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() != null) {
                    Intent data = result.getData();
                    // Get the payment result in String
                    String transactionResult = data.getStringExtra(MOLPayActivity.MOLPayTransactionResult);
                    Log.d(MOLPayActivity.MOLPAY, "transaction result = " + transactionResult);
                } else {
                    // Data null handler
                }
            }
    );

### Express Mode

Required a valid mp_channel value, this will skip the payment info page and go direct to the payment screen.
Add mp_express_mode & set mp_channel as below :

e.g. Express Mode to FPX Maybank online banking

    paymentDetails.put(MOLPayActivity.mp_express_mode, true);
    paymentDetails.put(MOLPayActivity.mp_channel, "maybank2u");

e.g. Express Mode to Touch 'n Go payment

    paymentDetails.put(MOLPayActivity.mp_express_mode, true);
    paymentDetails.put(MOLPayActivity.mp_channel, "TNG-EWALLET");

Find all mp_channel list here https://github.com/FiuuPayment/Mobile-XDK-Fiuu_Examples/blob/master/channel-list.md

NOTE:
* Can only select subscribed mp_channel.
* credit channel cannot use express mode due to security reasons.

### Show Selected Channels Only

Need refer column mp_channel in https://github.com/FiuuPayment/Mobile-XDK-Fiuu_Examples/blob/master/channel-list.md
Then add the selected channels in allowedchannels[] e.g. :

        String allowedchannels[] = {"TNG-EWALLET","maybank2u"};
        paymentDetails.put(MOLPayActivity.mp_allowed_channels, allowedchannels);

This will only show maybank2u & TNG-EWALLET channels in the channel listing.

### Others Optional Parameters

Learn more about optional parameters here https://github.com/RazerMS/Mobile-XDK-RazerMS_Android_Studio/wiki/Installation-Guidance#prepare-the-payment-detail-object

        // -------------------------------- Most commonly used -------------------------------------

        // To pre-select channel, refer column mp_channel in https://github.com/FiuuPayment/Mobile-XDK-Fiuu_Examples/blob/master/channel-list.md
        // e.g. set mp_channel = credit to directly load required card info.
        paymentDetails.put(MOLPayActivity.mp_channel, "credit");

        // Simulate offline payment (demo without actual charges).
        // Need set true for Google Pay Test Environment
        paymentDetails.put(MOLPayActivity.mp_sandbox_mode, true);

        // Set true if your account enabled extended Verify Payment
        paymentDetails.put(MOLPayActivity.mp_extended_vcode, false);

        // Show close button (by default hidden)
        paymentDetails.put(MOLPayActivity.mp_closebutton_display, true);

        // Allow change channel for pre-select mp_channel
        paymentDetails.put(MOLPayActivity.mp_channel_editing, true);

        // Allow payer information editing.
        paymentDetails.put(MOLPayActivity.mp_editing_enabled, false);

        // Explicitly force disable user input by field.
        paymentDetails.put(MOLPayActivity.mp_bill_name_edit_disabled, true);
        paymentDetails.put(MOLPayActivity.mp_bill_email_edit_disabled, false);
        paymentDetails.put(MOLPayActivity.mp_bill_mobile_edit_disabled, true);
        paymentDetails.put(MOLPayActivity.mp_bill_description_edit_disabled, false);

        // Set language : EN, MS, VI, TH, FIL, MY, KM, ID, ZH.
        paymentDetails.put(MOLPayActivity.mp_language, "MS");

## Google Pay

### Requirements for Production Environment

In order to use Production or Dev account (actual transactions testing), Google Pay production access is required for your app package name.

Follow Google’s instructions to request production access for your app: https://developers.google.com/pay/api/android/guides/test-and-deploy/request-prod-access

Google Pay Console : https://pay.google.com/business/console

* Choose the integration type Gateway when prompted, and provide screenshots of your app for review.
* After your app has been approved, test your integration in production by set mp_sandbox_mode = false & use production or Dev mp_verification_key & mp_merchant_ID.
* Then launching Google Pay from a signed, release build of your app.

Request Production Access Example 1 :

![GPay Request Production 1](https://github.com/user-attachments/assets/3a9c9a77-72ce-4b3a-9cf6-294b9c579c52)

Request Production Access Example 2 :

![GPay Request Production 2](https://github.com/user-attachments/assets/8159c306-e1d0-4026-bae2-0c65dfb82aa0)

NOTE: Can only use TEST environment & Sandbox account if not yet get Google Pay production approval.

### Google Pay (Express Mode)

1) Create Google Pay button

Add Google Pay button in XML layout e.g. :

        <com.google.android.gms.wallet.button.PayButton
        android:id="@+id/googlePayButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_alignParentBottom="true"
        android:layout_margin="19dp" />

In Java class add this in onCreate :

//      TODO: Choose your preferred Google Pay button design using this guideline : https://developers.google.com/pay/api/android/guides/brand-guidelines

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
   After your app has been approved, test your integration in production by set mp_sandbox_mode = false & use production / Dev mp_verification_key & mp_merchant_ID.
   Then launching Google Pay from a signed, release build of your app.
   */

        private void googlePayPayment() {
                paymentDetails = new HashMap<>();

                // Compulsory String. Values obtained from Fiuu.
                paymentDetails.put(MOLPayActivity.mp_merchant_ID, ""); // Sandbox ID for TEST environment & Production/Dev ID once Google approved production access
                paymentDetails.put(MOLPayActivity.mp_verification_key, ""); // Sandbox ID for TEST environment & Production/Dev ID once Google approved production access
                
                // Compulsory String. Payment info.
                paymentDetails.put(MOLPayActivity.mp_amount, "1.01"); // 2 decimal points format
                paymentDetails.put(MOLPayActivity.mp_order_ID, Calendar.getInstance().getTimeInMillis()); // Any unique alphanumeric String. For symbol only allowed hypen "-" and underscore "_"
                paymentDetails.put(MOLPayActivity.mp_currency, "MYR");
                paymentDetails.put(MOLPayActivity.mp_country, "MY");
                paymentDetails.put(MOLPayActivity.mp_bill_description, "The bill description");
                paymentDetails.put(MOLPayActivity.mp_bill_name, "The bill name");
                paymentDetails.put(MOLPayActivity.mp_bill_email, "payer.email@fiuu.com");
                paymentDetails.put(MOLPayActivity.mp_bill_mobile, "123456789");
        
                paymentDetails.put(MOLPayActivity.mp_sandbox_mode, true); // true = Test Environment & false = production (required Google Pay production access approval)
                paymentDetails.put(MOLPayActivity.mp_extended_vcode, false); // Optional : Set true if your account enabled extended Verify Payment (by default false)

                openGPActivityWithResult();
        }

3) Start payment by sending paymentDetails to ActivityGP.class

       private void openGPActivityWithResult() {
            Intent intent = new Intent(MainActivity.this, ActivityGP.class); // Used ActivityGP for Google Pay
            intent.putExtra(MOLPayActivity.MOLPayPaymentDetails, paymentDetails);
            gpActivityResultLauncher.launch(intent);
       }

4) Handle activity result listener

         ActivityResultLauncher<Intent> gpActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() != null) {
                    Intent data = result.getData();
                    // Get payment result in String
                    String transactionResult = data.getStringExtra(MOLPayActivity.MOLPayTransactionResult);
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

        paymentDetails.put(MOLPayActivity.mp_merchant_ID, ""); // Sandbox ID for TEST environment & Production/Dev ID once Google approved production access
        paymentDetails.put(MOLPayActivity.mp_verification_key, ""); // Sandbox ID for TEST environment & Production/Dev ID once Google approved production access
        paymentDetails.put(MOLPayActivity.mp_sandbox_mode, true); // true = Test Environment & false = production (required Google Pay production access approval)
        paymentDetails.put(MOLPayActivity.mp_extended_vcode, false); // Optional : Set true if your account enabled extended Verify Payment (by default false)

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
    1) Need check mp_sandbox_mode, mp_merchant_ID & mp_verification_key
    2) mp_sandbox_mode = true , need use Sandbox mp_merchant_ID & mp_verification_key
    3) mp_sandbox_mode = false or if did not send mp_sandbox_mode , need use Production/Dev mp_merchant_ID & mp_verification_key
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

Submit issue to this repository or email to our support-sa@fiuu.com

Merchant Technical Support / Customer Care : support-sa@fiuu.com<br>
Sales/Reseller Enquiry : sales-sa@fiuu.com<br>
Marketing Campaign : marketing-sa@fiuu.com<br>
Channel/Partner Enquiry : channel-sa@fiuu.com<br>
Media Contact : media-sa@fiuu.com<br>
R&D and Tech-related Suggestion : technical-sa@fiuu.com<br>
Abuse Reporting : abuse-sa@fiuu.com