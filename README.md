# [Mobile XDK] – RazerMS Android Library

<img src="https://user-images.githubusercontent.com/38641542/74424311-a9d64000-4e8c-11ea-8d80-d811cfe66972.jpg">

Quick Guides
------------

### All Channels

Add dependencies in build.gradle

    dependencies {
        implementation 'com.github.FiuuPayment:Mobile-XDK-Fiuu_Android_Library:<latest_version>'
    }

### All Channels / Single Channel

    HashMap<String, Object> paymentDetails = new HashMap<>();
    
    // TODO: Enter your merchant account credentials before test run
    paymentDetails.put(MOLPayActivity.mp_username, "");
    paymentDetails.put(MOLPayActivity.mp_password, "");
    paymentDetails.put(MOLPayActivity.mp_merchant_ID, "");
    paymentDetails.put(MOLPayActivity.mp_app_name, "");
    paymentDetails.put(MOLPayActivity.mp_verification_key, "");
    
    // Use 'multi' for all available channels option. For individual channel seletion, please refer to https://github.com/FiuuPayment/Mobile-XDK-Fiuu_Examples/blob/master/channel-list.md
    paymentDetails.put(MOLPayActivity.mp_channel, "multi");
    paymentDetails.put(MOLPayActivity.mp_order_ID, Calendar.getInstance().getTimeInMillis());
    paymentDetails.put(MOLPayActivity.mp_currency, "MYR");
    paymentDetails.put(MOLPayActivity.mp_country, "MY");
    paymentDetails.put(MOLPayActivity.mp_amount, "1.10");
    paymentDetails.put(MOLPayActivity.mp_bill_description, "bill description");
    paymentDetails.put(MOLPayActivity.mp_bill_name, "bill name");
    paymentDetails.put(MOLPayActivity.mp_bill_email, "example@gmail.com");
    paymentDetails.put(MOLPayActivity.mp_bill_mobile, "123456789");
    
    Intent intent = new Intent(MainActivity.this, MOLPayActivity.class);
    intent.putExtra(MOLPayActivity.MOLPayPaymentDetails, paymentDetails);
    startActivityForResult(intent, MOLPayActivity.MOLPayXDK);
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MOLPayActivity.MOLPayXDK && resultCode == RESULT_OK){
            String result = data.getStringExtra(MOLPayActivity.MOLPayTransactionResult);
        }
    }

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

    HashMap<String, Object> paymentDetails = new HashMap<>();
    
    // TODO: Follow Google’s instructions to request production access for your app: https://developers.google.com/pay/api/android/guides/test-and-deploy/request-prod-access
    // Choose the integration type Gateway when prompted, and provide screenshots of your app for review.
    // After your app has been approved, test your integration in production by set mp_sandbox_mode = false & use production mp_verification_key & mp_merchant_ID.
    // Then launching Google Pay from a signed, release build of your app.
    paymentDetails.put(MOLPayActivity.mp_sandbox_mode, true); // Only set to false once you have request production access for your app
    
    // TODO: Enter your merchant account credentials before test run
    paymentDetails.put(MOLPayActivity.mp_merchant_ID, ""); // Your sandbox / production merchant ID
    paymentDetails.put(MOLPayActivity.mp_verification_key, ""); // Your sandbox / production verification key
    
    paymentDetails.put(MOLPayActivity.mp_amount, "1.11"); // Must be in 2 decimal points format
    paymentDetails.put(MOLPayActivity.mp_order_ID, Calendar.getInstance().getTimeInMillis()); // Must be unique
    paymentDetails.put(MOLPayActivity.mp_currency, "MYR"); // Must matched mp_country
    paymentDetails.put(MOLPayActivity.mp_country, "MY"); // Must matched mp_currency
    paymentDetails.put(MOLPayActivity.mp_bill_description, "The bill description");
    paymentDetails.put(MOLPayActivity.mp_bill_name, "The bill name");
    paymentDetails.put(MOLPayActivity.mp_bill_email, "payer.email@fiuu.com");
    paymentDetails.put(MOLPayActivity.mp_bill_mobile, "123456789");

    paymentDetails.put(MOLPayActivity.mp_extended_vcode, false); // Set true if your account enabled extended Verify Payment
    
    Intent intent = new Intent(MainActivity.this, ActivityGP.class); // Used ActivityGP for Google Pay
    intent.putExtra(MOLPayActivity.MOLPayPaymentDetails, paymentDetails);
    startActivityForResult(intent, MOLPayActivity.MOLPayXDK);

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

    Error A01 - "Fail to detokenize Google Pay Token given" - Error starting a payment process due to several possible reasons, please contact Razer Merchant Services support should the error persists.
    1) Misconfigure GooglePay setup
    2) API credentials (username, password, merchant id, verify key)
    3) Razer Merchant Services server offline.

## Resources

- GitHub:     https://github.com/FiuuPayment
- Website:    https://fiuu.com
- Twitter:    https://twitter.com/Fiuu_Payment
- YouTube:    https://www.youtube.com/@FiuuPayment
- Facebook:   https://www.facebook.com/FiuuPayment
- Instagram:  https://www.instagram.com/RazerMerchantServices

Issues
------------

Submit issue to this repository or email to our support-sa@razer.com

Support
-------

Merchant Technical Support / Customer Care : support-sa@razer.com <br>
Sales/Reseller Enquiry : sales-sa@razer.com <br>
Marketing Campaign : marketing-sa@razer.com <br>
Channel/Partner Enquiry : channel-sa@razer.com <br>
Media Contact : media-sa@razer.com <br>
R&D and Tech-related Suggestion : technical-sa@razer.com <br>
Abuse Reporting : abuse-sa@razer.com
