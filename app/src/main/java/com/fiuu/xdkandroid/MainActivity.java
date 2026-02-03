package com.fiuu.xdkandroid;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fiuu.xdkandroid.models.Billing;
import com.fiuu.xdkandroid.models.Merchant;
import com.fiuu.xdkandroid.models.Payment;
import com.google.android.gms.wallet.button.ButtonConstants;
import com.google.android.gms.wallet.button.ButtonOptions;
import com.google.android.gms.wallet.button.PayButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.molpay.molpayxdk.MOLPayActivity;
import com.molpay.molpayxdk.googlepay.ActivityGP;
import com.molpay.molpayxdk.googlepay.UtilGP;

import org.json.JSONException;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import android.text.TextUtils;

public class MainActivity extends AppCompatActivity {
    HashMap<Object, Object> paymentDetails = new HashMap<>();

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    MyViewPagerAdapter myViewPagerAdapter;

    private SharedViewModel viewModel;

    private Boolean
            mp_express_mode = false;
    private String mp_channel = "";
    private String mp_amount ="";
    private String mp_country = "";
    private String mp_currency = "";
    private String mp_username = "";
    private String mp_password ="";
    private String mp_appname = "";
    private String mp_merchantid = "";
    private String mp_verificationKey = "";

    private String mp_description = "";
    private String mp_payername ="";
    private String mp_payeremail = "";
    private String mp_payermobile = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);

        ScrollView scrollV = findViewById(R.id.scrollView);
        setSupportActionBar(toolbar);

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        myViewPagerAdapter = new MyViewPagerAdapter(this);
        viewPager.setAdapter(myViewPagerAdapter);
        viewModel = new ViewModelProvider(this).get(SharedViewModel.class);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                scrollV.smoothScrollTo(0, 0);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                tabLayout.getTabAt(position).select();
            }
        });

//        set default value for all data requirements
        if (viewModel.getPaymentData().getValue() == null){
            Payment model = new Payment();
            model.setChannel("multi");
            model.setAmount("1.01");
            model.setCurrency("MYR");
            model.setCountry("MY");
            model.setIsExpressMode(false);
            viewModel.setPaymentData(model);
        }
        if (viewModel.getBillingData().getValue() == null){
            Billing model = new Billing();
            model.setPayername("Payer Name");
            model.setPayermobile("123456789");
            model.setPayeremail("payer.email@fiuu.com");
            model.setDescription("The bill description");
            viewModel.setBillingData(model);
        }
        if (viewModel.getMerchantData().getValue() == null){
            Merchant model = new Merchant();
            model.setUsername("");
            model.setPassword("");
            model.setAppname("");
            model.setMerchantid("");
            model.setVerificationKey("");
            viewModel.setMerchantData(model);
        }

        viewModel.getPaymentData().observe(this, modelData -> {
            mp_express_mode =  modelData.getIsExpressMode();
            mp_channel =  modelData.getChannel();
            mp_amount =   modelData.getAmount(); // 2 decimal points format eg:1.01
            mp_country =   modelData.getCountry();  //eg: "MY"
            mp_currency =   modelData.getCurrency(); //eg: "MYR"
        });

        viewModel.getBillingData().observe(this, modelData -> {
            mp_description =  modelData.getDescription();
            mp_payername =  modelData.getPayername();
            mp_payeremail =   modelData.getPayeremail();
            mp_payermobile =   modelData.getPayermobile();
        });

        viewModel.getMerchantData().observe(this, modelData -> {
            // TODO : Set account info here before START
            mp_username = modelData.getUsername();
            mp_password = modelData.getPassword();
            mp_appname = modelData.getAppname();
            mp_merchantid = modelData.getMerchantid();
            mp_verificationKey = modelData.getVerificationKey();
        });

        boolean isRooted = MOLPayActivity.isDeviceRooted(MainActivity.this);
        if (isRooted) {
            new AlertDialog.Builder(this)
                    .setTitle("Security Alert")
                    .setMessage("This device appears to be rooted. For security reasons, this application will now close.")
                    .setCancelable(false)
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        finish();
                    })
                    .show();

            return; // stop further execution
        }

        // TODO: For GPay e-Wallet payment method cannot use PayButton API Style & Personalization : https://developers.google.com/pay/api/android/guides/brand-guidelines

        ImageView btnGPay = findViewById(R.id.btnGPay);
        btnGPay.setOnClickListener(v -> {
            googlePayPayment();
        });

//----------------------------------------------------------------------------------

//        NOTE: Below implementation only available to GPay Card payment method only

        // The Google Pay button is a layout file – take the root view
//        PayButton googlePayButton = findViewById(R.id.googlePayButton);

//        try {
//            googlePayButton.initialize(
//                    ButtonOptions.newBuilder()
//                            .setButtonTheme(ButtonConstants.ButtonTheme.DARK)
//                            .setButtonType(ButtonConstants.ButtonType.PAY)
//                            .setCornerRadius(99)
//                            .setAllowedPaymentMethods(UtilGP.getAllowedPaymentMethods().toString())
//                            .build()
//            );
//            googlePayButton.setOnClickListener(view -> {
//                googlePayPayment();
//            });
//        } catch (JSONException e) {
//            // Keep Google Pay button hidden (consider logging this to your app analytics service)
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // START clicked
        if (id == R.id.newBtn) {
            if (TextUtils.isEmpty(mp_username.trim())
            || TextUtils.isEmpty(mp_password.trim())
            || TextUtils.isEmpty(mp_appname.trim())
            || TextUtils.isEmpty(mp_merchantid.trim())
            || TextUtils.isEmpty(mp_verificationKey.trim())) {
                    new AlertDialog.Builder(this)
                        .setTitle("Merchant Info Missing")
                        .setMessage("Please fill up all info in MERCHANT tab or set all in MainActivity.java")
                        .setCancelable(false)
                        .setPositiveButton("OK", (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .show();
            } else {
                restartmolpay();
            }
        }

        return super.onOptionsItemSelected(item);
    }
    private void restartmolpay() {
        paymentDetails = new HashMap<>();

        paymentDetails.put(MOLPayActivity.mp_closebutton_display, true);
        // Compulsory String. Values obtained from Fiuu.
        paymentDetails.put(MOLPayActivity.mp_username, mp_username);
        paymentDetails.put(MOLPayActivity.mp_password, mp_password);
        paymentDetails.put(MOLPayActivity.mp_app_name, mp_appname);
        paymentDetails.put(MOLPayActivity.mp_merchant_ID, mp_merchantid);
        paymentDetails.put(MOLPayActivity.mp_verification_key, mp_verificationKey);

        // Compulsory String. Payment info.
        paymentDetails.put(MOLPayActivity.mp_express_mode, mp_express_mode);
        paymentDetails.put(MOLPayActivity.mp_channel, mp_channel);
        paymentDetails.put(MOLPayActivity.mp_amount, mp_amount); // 2 decimal points format eg:1.01
        paymentDetails.put(MOLPayActivity.mp_country, mp_country);  //eg: "MY"
        paymentDetails.put(MOLPayActivity.mp_currency, mp_currency); //eg: "MYR"

        paymentDetails.put(MOLPayActivity.mp_order_ID, Calendar.getInstance().getTimeInMillis()); // Any unique alphanumeric String. For symbol only allowed hypen "-" and underscore "_"
        paymentDetails.put(MOLPayActivity.mp_bill_description, mp_description);
        paymentDetails.put(MOLPayActivity.mp_bill_name,mp_payername);
        paymentDetails.put(MOLPayActivity.mp_bill_email, mp_payeremail);
        paymentDetails.put(MOLPayActivity.mp_bill_mobile, mp_payermobile);

        // --------------------------------- FOR WEB GOOGLE PAY ----------------------------------------

        // GPay payment methods setting examples : (by default will show all payment methods)
//        paymentDetails.put(MOLPayActivity.mp_gpay_channel, new String[] { "CC", "TNG-EWALLET" }); // Enable Card & TNG eWallet Only
//        paymentDetails.put(MOLPayActivity.mp_gpay_channel, new String[] { "SHOPEEPAY", "TNG-EWALLET" }); // Enable ShopeePay & TNG eWallet Only

//        paymentDetails.put(MOLPayActivity.mp_merchant_ID, ""); // Sandbox ID for TEST environment & Production/Dev ID once Google approved production access
//        paymentDetails.put(MOLPayActivity.mp_verification_key, ""); // Sandbox vKey for TEST environment & Production/Dev vKey once Google approved production access
//        paymentDetails.put(MOLPayActivity.mp_sandbox_mode, true); // Optional :  true = Test Environment & false = production (required Google Pay production access approval)(by default false)
//        paymentDetails.put(MOLPayActivity.mp_extended_vcode, false); // Optional : Set true if your account enabled extended Verify Payment (by default false)
//        paymentDetails.put(MOLPayActivity.mp_hide_googlepay, true); // Optional : Hide Google Pay button (by default false)
//        paymentDetails.put(MOLPayActivity.mp_company, "Your Company Name"); // Show merchant name in Google Pay

        // ------------------------------------ OPTIONAL -------------------------------------------

        // TODO: Learn more about optional parameters here https://github.com/RazerMS/Mobile-XDK-RazerMS_Android_Studio/wiki/Installation-Guidance#prepare-the-payment-detail-object

        // -------------------------------- Most commonly used -------------------------------------

        // Optional, set Environment for Webview Core URL
//        paymentDetails.put(MOLPayActivity.mp_core_env, "2"); //default

        // To pre-select channel, please refer to column mp_channel in https://github.com/RazerMS/Mobile-XDK-RazerMS_Examples/blob/master/channel-list.md
//        paymentDetails.put(MOLPayActivity.mp_channel, "maybank2u");

        // Optional, required a valid mp_channel value, this will skip the payment info page and go direct to the payment screen.
        // Channel "credit" could not use express mode due security reasons.
//        paymentDetails.put(MOLPayActivity.mp_express_mode, true);

        // Optional, show selected channels only.
//        String allowedchannels[] = {"TNG-EWALLET","maybank2u"}; // Refer to column mp_channel in https://github.com/RazerMS/Mobile-XDK-RazerMS_Examples/blob/master/channel-list.md
//        paymentDetails.put(MOLPayActivity.mp_allowed_channels, allowedchannels);

        // Optional, simulate offline payment, set boolean value to enable.
        // Need set true for Google Pay Test Environment.
//        paymentDetails.put(MOLPayActivity.mp_sandbox_mode, true);

        // Optional, for Google Pay Only - Set true if your account enabled extended Verify Payment
//        paymentDetails.put(MOLPayActivity.mp_extended_vcode, false);

        // Optional, show close button.
//        paymentDetails.put(MOLPayActivity.mp_closebutton_display, true);
//        paymentDetails.put(MOLPayActivity.mp_enable_fullscreen, true); //enable fullscreen

        // Optional, allow / block change channel for preset mp_channel
//        paymentDetails.put(MOLPayActivity.mp_channel_editing, true);

        // Optional, allow billing information editing.
//        paymentDetails.put(MOLPayActivity.mp_editing_enabled, true);

        // Optional, explicitly force disable user input by field.
//        paymentDetails.put(MOLPayActivity.mp_bill_name_edit_disabled, true);
//        paymentDetails.put(MOLPayActivity.mp_bill_email_edit_disabled, false);
//        paymentDetails.put(MOLPayActivity.mp_bill_mobile_edit_disabled, true);
//        paymentDetails.put(MOLPayActivity.mp_bill_description_edit_disabled, false);

        // Optional, EN, MS, VI, TH, FIL, MY, KM, ID, ZH.
//        paymentDetails.put(MOLPayActivity.mp_language, "MS");

        // Add metadata in JSON String format e.g.
//        paymentDetails.put(MOLPayActivity.mp_metadata, "{\"store_id\":\"MY2025HQ\"}");

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
                String transactionResult = "result = null";
                viewPager.setCurrentItem(0, true);
               //Log.d(MOLPayActivity.MOLPAY, "result: "+result);

                if (result.getData() != null) {
                    Intent data = result.getData();
                    transactionResult = data.getStringExtra(MOLPayActivity.MOLPayTransactionResult);

                    if (transactionResult != null) {
                       //Log.d(MOLPayActivity.MOLPAY, "final response = " + transactionResult);

//                        TextView tw = findViewById(R.id.resultTV);
//                        tw.setText(transactionResult);
                    }
                }
                viewModel.setTransactionResult(transactionResult);
            }
    );

    private void googlePayPayment() {
            paymentDetails = new HashMap<>();

            paymentDetails.put(MOLPayActivity.mp_merchant_ID, mp_merchantid);
            paymentDetails.put(MOLPayActivity.mp_verification_key, mp_verificationKey);

            // Compulsory String. Payment info.
            paymentDetails.put(MOLPayActivity.mp_amount, mp_amount); // 2 decimal points format eg:1.01
            paymentDetails.put(MOLPayActivity.mp_order_ID, Calendar.getInstance().getTimeInMillis()); // Any unique alphanumeric String. For symbol only allowed hypen "-" and underscore "_"

            paymentDetails.put(MOLPayActivity.mp_currency, mp_currency); //eg: "MYR"
            paymentDetails.put(MOLPayActivity.mp_country, mp_country);  //eg: "MY"
            paymentDetails.put(MOLPayActivity.mp_bill_description, mp_description);
            paymentDetails.put(MOLPayActivity.mp_bill_name,mp_payername);
            paymentDetails.put(MOLPayActivity.mp_bill_email, mp_payeremail);
            paymentDetails.put(MOLPayActivity.mp_bill_mobile, mp_payermobile);

        /*
            TODO: Follow Google’s instructions to request production access for your app: https://developers.google.com/pay/api/android/guides/test-and-deploy/request-prod-access
            *
             Choose the integration type Gateway when prompted, and provide screenshots of your app for review.
             After your app has been approved, test your integration in production by set mp_sandbox_mode = false & use production mp_verification_key & mp_merchant_ID.
             Then launching Google Pay from a signed, release build of your app.
             */

        // TODO: Enter your merchant account credentials before test run
//        paymentDetails.put(MOLPayActivity.mp_sandbox_mode, true); // true = Test Environment & false = production (required Google Pay production access approval)
//        paymentDetails.put(MOLPayActivity.mp_merchant_ID, ""); // Sandbox ID for TEST environment & Production/Dev ID once Google approved production access
//        paymentDetails.put(MOLPayActivity.mp_verification_key, ""); // Sandbox ID for TEST environment & Production/Dev ID once Google approved production access

//        paymentDetails.put(MOLPayActivity.mp_merchant_ID, "");
//        paymentDetails.put(MOLPayActivity.mp_verification_key, "");
//
//        paymentDetails.put(MOLPayActivity.mp_amount, mp_amount); // 2 decimal points format
//        paymentDetails.put(MOLPayActivity.mp_order_ID, Calendar.getInstance().getTimeInMillis()); // Any unique alphanumeric String. For symbol only allowed hypen "-" and underscore "_"
//        paymentDetails.put(MOLPayActivity.mp_currency, mp_currency);
//        paymentDetails.put(MOLPayActivity.mp_country, mp_country);
//        paymentDetails.put(MOLPayActivity.mp_bill_description, "The bill description");
//        paymentDetails.put(MOLPayActivity.mp_bill_name, "Payer name");
//        paymentDetails.put(MOLPayActivity.mp_bill_email, "payer.email@fiuu.com");
//        paymentDetails.put(MOLPayActivity.mp_bill_mobile, "123456789");

        // GPay payment methods setting examples : (by default will show all payment methods)
//        paymentDetails.put(MOLPayActivity.mp_gpay_channel, new String[] { "CC", "TNG-EWALLET" }); // Enable Card & TNG eWallet Only
//        paymentDetails.put(MOLPayActivity.mp_gpay_channel, new String[] { "SHOPEEPAY", "TNG-EWALLET" }); // Enable ShopeePay & TNG eWallet Only

        // Optional
//        paymentDetails.put(MOLPayActivity.mp_company, "Your Company Name"); // Show merchant name in Google Pay
//        paymentDetails.put(MOLPayActivity.mp_closebutton_display, true); // Enable close button
//        paymentDetails.put(MOLPayActivity.mp_enable_fullscreen, true); //enable fullscreen
//        paymentDetails.put(MOLPayActivity.mp_extended_vcode, false); // Set true if your account enabled extended Verify Payment

        openGPActivityWithResult();
    }

    private void openGPActivityWithResult() {
        Intent intent = new Intent(MainActivity.this, ActivityGP.class); // Used ActivityGP for Google Pay
        intent.putExtra(MOLPayActivity.MOLPayPaymentDetails, paymentDetails);
        gpActivityResultLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> gpActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
               //Log.d("logGooglePay", "result: "+result);
                String transactionResult = "result = null";
                if (isFinishing() || isDestroyed()) return;
                viewPager.setCurrentItem(0, true);
                if (result.getData() != null) {
                    Intent data = result.getData();
                    transactionResult = data.getStringExtra(MOLPayActivity.MOLPayTransactionResult);

                    if (transactionResult != null) {
                       //Log.d("logGooglePay", "final response = " + transactionResult);
//                        TextView tw = findViewById(R.id.resultTV);
//                        tw.setText(transactionResult);
                    }
                }
                viewModel.setTransactionResult(transactionResult);
            }
    );

}

