# [Mobile XDK] – Fiuu Android Java

<img alt="" src="https://user-images.githubusercontent.com/38641542/74424311-a9d64000-4e8c-11ea-8d80-d811cfe66972.jpg">

This is a fully functional Fiuu Android payment module. It can be seamlessly integrated into Android Studio as a MOLPayXDK module using Gradle integration from the JCenter/Maven repository. For
reference, we’ve included an example application project called fiuuxdkproject, which demonstrates the integration with the MOLPayXDK framework.

# How does it work ?

1. User Selection:
   <br>  a. User chooses their preferred payment option or bank.
   <br>  b. Then press “Proceed.”

3. Bank Credentials:
   <br> a. User inputs their bank credentials to complete the payment process.

4. Transaction Completion:
   <br> a. Once the necessary steps are followed, the transaction is successfully completed.

#### Important Note :

To utilize this module, you must be a registered Fiuu merchant. Contact us at sales-sa@fiuu.com to obtain your own credentials for testing or production use.


## Getting Started

: Follow the “get started” guide to install Android
Studio : (https://developer.android.com/studio?gad_source=1&gclid=CjwKCAiAivGuBhBEEiwAWiFmYcRftsvawKYGV68bhK2IluVModqwUchdEU_wli2H39oLU1EdkBiGjxoC3RkQAvD_BwE&gclsrc=aw.ds)

For additional assistance with Android Studio, refer to our [online documentation](https://developer.android.com/studio/intro), which includes tutorials, samples, mobile development guidance, and a
comprehensive API reference.

## Recommended configurations

When implementing Payment XDK into your project, consider the following:

- Android SDK Version: 33

- Android API Level: 26

- Android target version: Android 8.0

- Android Studio Gradle: 7.4.2

## Installation Guidance

### All Channels

Add dependencies in build.gradle

    dependencies {
        implementation 'com.github.FiuuPayment:Mobile-XDK-Fiuu_Android_Library:<latest_version>'
    }

### All Channels / Single Channel

    private void restartmolpay() {
        paymentDetails.put(MOLPayActivity.mp_amount, "0.10");

        // TODO: Enter your merchant account credentials before test run
        paymentDetails.put(MOLPayActivity.mp_username, "");
        paymentDetails.put(MOLPayActivity.mp_password, "");
        paymentDetails.put(MOLPayActivity.mp_merchant_ID, "");
        paymentDetails.put(MOLPayActivity.mp_app_name, "");
        paymentDetails.put(MOLPayActivity.mp_verification_key, "");

        paymentDetails.put(MOLPayActivity.mp_order_ID, Calendar.getInstance().getTimeInMillis());
        paymentDetails.put(MOLPayActivity.mp_currency, "MYR");
        paymentDetails.put(MOLPayActivity.mp_country, "MY");
        paymentDetails.put(MOLPayActivity.mp_channel, "multi");
        paymentDetails.put(MOLPayActivity.mp_bill_description, "bill description");
        paymentDetails.put(MOLPayActivity.mp_bill_name, "bill name");
        paymentDetails.put(MOLPayActivity.mp_bill_email, "example@gmail.com");
        paymentDetails.put(MOLPayActivity.mp_bill_mobile, "123456789");

      //        paymentDetails.put(MOLPayActivity.mp_extended_vcode, false); // For Google Pay Only - Set true if your account enabled extended Verify Payment
      //        paymentDetails.put(MOLPayActivity.mp_channel_editing, false);
      //        paymentDetails.put(MOLPayActivity.mp_editing_enabled, true);
      paymentDetails.put(MOLPayActivity.mp_express_mode, false);
      paymentDetails.put(MOLPayActivity.mp_dev_mode, false);
      //        paymentDetails.put(MOLPayActivity.mp_closebutton_display, true);
      //        paymentDetails.put(MOLPayActivity.mp_preferred_token, "new");

        openStartActivityResult();
    }

      private void openStartActivityResult() {
            Intent intent = new Intent(MainActivity.this, MOLPayActivity.class);
            intent.putExtra(MOLPayActivity.MOLPayPaymentDetails, paymentDetails);
            paymentActivityResultLauncher.launch(intent);
      }

    ActivityResultLauncher<Intent> paymentActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d("MOLPAYXDKLibrary", "result: " + result);
                Log.d("MOLPAYXDKLibrary", "result: " + result.getResultCode());
                if (result.getResultCode() == MOLPayActivity.RESULT_OK) {
                    Log.d("MOLPAYXDKLibrary", "result: " + result.getData().getStringExtra(MOLPayActivity.MOLPayTransactionResult));

                    TextView tw = findViewById(R.id.resultTV);
                    tw.setText(result.getData().getStringExtra(MOLPayActivity.MOLPayTransactionResult));
                }

            }
    );

### Express Mode

Just add mp_express_mode & set single channel.

e.g. Express Mode to https://www.maybank2u.com.my/home/m2u/common/login.do

    paymentDetails.put(MOLPayActivity.mp_express_mode, true);
    paymentDetails.put(MOLPayActivity.mp_channel, "maybank2u");

e.g. Express Mode to Touch 'n Go payment

    paymentDetails.put(MOLPayActivity.mp_express_mode, true);
    paymentDetails.put(MOLPayActivity.mp_channel, "TNG-EWALLET");

Find all mp_channel list here https://github.com/FiuuPayment/Mobile-XDK-Fiuu_Examples/blob/master/channel-list.md

### Google Pay

Prepare paymentDetails :

    private void googlePayPayment() {
        paymentDetails = new HashMap<>();

        /*
            TODO: Follow Google’s instructions to request production access for your app: https://developers.google.com/pay/api/android/guides/test-and-deploy/request-prod-access
            *
             Choose the integration type Gateway when prompted, and provide screenshots of your app for review.
             After your app has been approved, test your integration in production by set mp_sandbox_mode = false & use production mp_verification_key & mp_merchant_ID.
             Then launching Google Pay from a signed, release build of your app.
             */
        paymentDetails.put(MOLPayActivity.mp_sandbox_mode, true); // Only set to false once you have request production access for your app

        // TODO: Enter your merchant account credentials before test run
        paymentDetails.put(MOLPayActivity.mp_merchant_ID, ""); // Your sandbox / production merchant ID
        paymentDetails.put(MOLPayActivity.mp_verification_key, ""); // Your sandbox / production verification key

        paymentDetails.put(MOLPayActivity.mp_amount, "1.01"); // Must be in 2 decimal points format
        paymentDetails.put(MOLPayActivity.mp_order_ID, Calendar.getInstance().getTimeInMillis()); // Must be unique
        paymentDetails.put(MOLPayActivity.mp_currency, "MYR"); // Must matched mp_country
        paymentDetails.put(MOLPayActivity.mp_country, "MY"); // Must matched mp_currency
        paymentDetails.put(MOLPayActivity.mp_bill_description, "The bill description");
        paymentDetails.put(MOLPayActivity.mp_bill_name, "The bill name");
        paymentDetails.put(MOLPayActivity.mp_bill_email, "payer.email@fiuu.com");
        paymentDetails.put(MOLPayActivity.mp_bill_mobile, "123456789");

        paymentDetails.put(MOLPayActivity.mp_extended_vcode, false); // Optional : Set true if your account enabled extended Verify Payment
        openGPActivityWithResult();

    }

Start payment by sending paymentDetails to ActivityGP.class :

      private void openGPActivityWithResult() {
            Intent intent = new Intent(MainActivity.this, ActivityGP.class); // Used ActivityGP for Google Pay
            intent.putExtra(MOLPayActivity.MOLPayPaymentDetails, paymentDetails);
            gpActivityResultLauncher.launch(intent);
      }

    ActivityResultLauncher<Intent> gpActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d("MOLPAYXDKLibrary", "result: " + result);
                Log.d("MOLPAYXDKLibrary", "result: " + result.getResultCode());

                if (result.getResultCode() == MOLPayActivity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String transactionResult = data.getStringExtra(MOLPayActivity.MOLPayTransactionResult);

                    if (data.getData() != null && transactionResult != null) {
                        Log.d(MOLPayActivity.MOLPAY, "MOLPay result = " + data.getStringExtra(MOLPayActivity.MOLPayTransactionResult));
                        TextView tw = findViewById(R.id.resultTV);
                        tw.setText(data.getStringExtra(MOLPayActivity.MOLPayTransactionResult));
                    }
                } else {
                    Log.e("logGooglePay", "RESULT_CANCELED data == null");
                    TextView tw = findViewById(R.id.resultTV);
                    tw.setText("result = null");
                }
            }
    );

## Payment results - Google Pay

    =========================================
    Sample transaction result in JSON string:
    =========================================

    {
        "StatCode":"00",
        "StatName":"captured",
        "TranID":"30824452",
        "Amount":"1.11",
        "Domain":"SB_molpayxdk",
        "VrfKey":"7c34xxxxxxxxxxxxxxxxxxxxxxxx2000",
        "Channel":"credit",
        "OrderID":"1717661730213",
        "Currency":"MYR",
        "ErrorCode":null,
        "ErrorDesc":null
    }

    Parameter and meaning:
    
    "StatCode" - "00" for Success, "11" for Failed, "22" for Pending. 
    
    "Amount" - The transaction amount
    "OrderID" - The transaction order ID
    "Channel" - The transaction channel description
    "TranID" - The transaction ID generated by Fiuu
    "Domain" - Your Merchant ID

    "VrfKey" - You can verify payment using this formula -> VrfKey = md5(Amount+secret_key+Domain+TranID+StatCode)
    
    * Note: secret_key = Your account Secret Key in https://portal.fiuu.com/
    * Notes: You may ignore other parameters and values not stated above

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
    
    Parameter and meaning:

    Error P03 - Your payment info format not correct   
    1) Need makesure all required parameters filled correctly.
    2) Need set mp_extended_vcode = true if enabled extended Verify Payment.

    Error A01 - "Fail to detokenize Google Pay Token given" - Error starting a payment process due to several possible reasons, please contact Fiuu support should the error persists.
    1) Misconfigure GooglePay setup
    2) API credentials (username, password, merchant id, verify key)
    3) Payment Server Offline.

## Resources

- GitHub:     https://github.com/FiuuPayment
- Website:    https://fiuu.com
- Twitter:    https://twitter.com/FiuuPayment
- YouTube:    https://www.youtube.com/c/FiuuPayment
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
