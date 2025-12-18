package com.fiuu.xdkandroid;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import android.widget.TextView;

import com.google.android.gms.wallet.button.ButtonConstants;
import com.google.android.gms.wallet.button.ButtonOptions;
import com.google.android.gms.wallet.button.PayButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.molpay.molpayxdk.MOLPayActivity;
import com.molpay.molpayxdk.googlepay.ActivityGP;
import com.molpay.molpayxdk.googlepay.UtilGP;

import org.json.JSONException;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    HashMap<Object, Object> paymentDetails = new HashMap<>();
    SwitchMaterial switchMaterial = findViewById(R.id.switch_material);
    EditText editTextChannel = findViewById(R.id.edt_mp_channel);
    EditText editTextAmount = findViewById(R.id.edt_mp_amount);
    EditText editTextCurrency = findViewById(R.id.edt_mp_currency);
    EditText editTextCountry = findViewById(R.id.edt_mp_country);


    private void restartmolpay() {
        paymentDetails = new HashMap<>();
        boolean isExpressMode = switchMaterial.isChecked();
        String mp_channel = editTextChannel.getText().toString();
        String mp_amount = editTextAmount.getText().toString();
        String mp_country = editTextCountry.getText().toString();
        String mp_currency = editTextCurrency.getText().toString();


        // Compulsory String. Values obtained from Fiuu.
        paymentDetails.put(MOLPayActivity.mp_username, "");
        paymentDetails.put(MOLPayActivity.mp_password, "");
        paymentDetails.put(MOLPayActivity.mp_app_name, "");
        paymentDetails.put(MOLPayActivity.mp_merchant_ID, "");
        paymentDetails.put(MOLPayActivity.mp_verification_key, "");

        // Compulsory String. Payment info.
        paymentDetails.put(MOLPayActivity.mp_express_mode, isExpressMode);
        paymentDetails.put(MOLPayActivity.mp_channel, mp_channel);
        paymentDetails.put(MOLPayActivity.mp_closebutton_display, true);
        paymentDetails.put(MOLPayActivity.mp_amount, mp_amount); // 2 decimal points format eg:1.01
        paymentDetails.put(MOLPayActivity.mp_order_ID, Calendar.getInstance().getTimeInMillis()); // Any unique alphanumeric String. For symbol only allowed hypen "-" and underscore "_"
        paymentDetails.put(MOLPayActivity.mp_country, mp_country);  //eg: "MY"
        paymentDetails.put(MOLPayActivity.mp_currency, mp_currency); //eg: "MYR"
        paymentDetails.put(MOLPayActivity.mp_bill_description, "The bill description");
        paymentDetails.put(MOLPayActivity.mp_bill_name, "Payer Name");
        paymentDetails.put(MOLPayActivity.mp_bill_email, "payer.email@fiuu.com");
        paymentDetails.put(MOLPayActivity.mp_bill_mobile, "123456789");

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

               //Log.d(MOLPayActivity.MOLPAY, "result: "+result);

                if (result.getData() != null) {
                    Intent data = result.getData();
                    String transactionResult = data.getStringExtra(MOLPayActivity.MOLPayTransactionResult);

                    if (transactionResult != null) {
                       //Log.d(MOLPayActivity.MOLPAY, "final response = " + transactionResult);
                        TextView tw = findViewById(R.id.resultTV);
                        tw.setText(transactionResult);
                    }
                } else {
                   //Log.e(MOLPayActivity.MOLPAY , "data == null");
                    TextView tw = findViewById(R.id.resultTV);
                    tw.setText("result = null");
                }
            }
    );

    private void googlePayPayment() {
        paymentDetails = new HashMap<>();

        String mp_amount = editTextAmount.getText().toString();
        String mp_country = editTextCountry.getText().toString();
        String mp_currency = editTextCurrency.getText().toString();

        /*
            TODO: Follow Google’s instructions to request production access for your app: https://developers.google.com/pay/api/android/guides/test-and-deploy/request-prod-access
            *
             Choose the integration type Gateway when prompted, and provide screenshots of your app for review.
             After your app has been approved, test your integration in production by set mp_sandbox_mode = false & use production mp_verification_key & mp_merchant_ID.
             Then launching Google Pay from a signed, release build of your app.
             */

        // TODO: Enter your merchant account credentials before test run
//        paymentDetails.put(MOLPayActivity.mp_sandbox_mode, true); // true = Test Environment & false = production (required Google Pay production access approval)
        paymentDetails.put(MOLPayActivity.mp_merchant_ID, ""); // Sandbox ID for TEST environment & Production/Dev ID once Google approved production access
        paymentDetails.put(MOLPayActivity.mp_verification_key, ""); // Sandbox ID for TEST environment & Production/Dev ID once Google approved production access

        paymentDetails.put(MOLPayActivity.mp_amount, mp_amount); // 2 decimal points format
        paymentDetails.put(MOLPayActivity.mp_order_ID, Calendar.getInstance().getTimeInMillis()); // Any unique alphanumeric String. For symbol only allowed hypen "-" and underscore "_"
        paymentDetails.put(MOLPayActivity.mp_currency, mp_currency);
        paymentDetails.put(MOLPayActivity.mp_country, mp_country);
        paymentDetails.put(MOLPayActivity.mp_bill_description, "The bill description");
        paymentDetails.put(MOLPayActivity.mp_bill_name, "Payer name");
        paymentDetails.put(MOLPayActivity.mp_bill_email, "payer.email@fiuu.com");
        paymentDetails.put(MOLPayActivity.mp_bill_mobile, "123456789");

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

                if (result.getData() != null) {
                    Intent data = result.getData();
                    String transactionResult = data.getStringExtra(MOLPayActivity.MOLPayTransactionResult);

                    if (transactionResult != null) {
                       //Log.d("logGooglePay", "final response = " + transactionResult);
                        TextView tw = findViewById(R.id.resultTV);
                        tw.setText(transactionResult);
                    }
                } else {
                   //Log.e("logGooglePay" , "data == null");
                    TextView tw = findViewById(R.id.resultTV);
                    tw.setText("result = null");
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        countryDropDown();
        channelDropDown();

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
            restartmolpay();
        }

        return super.onOptionsItemSelected(item);
    }
    public void countryDropDown() {
        EditText editTextCurrency = findViewById(R.id.edt_mp_currency);
        AutoCompleteTextView editTextCountry = findViewById(R.id.edt_mp_country);

        List<Country> countries = Arrays.asList(
                new Country("MY", "MYR"),
                new Country("SG", "SGD")
        );

        // Use simple_list_item_1 - it only has ONE TextView
        ArrayAdapter<Country> adapter = new ArrayAdapter<Country>(
                this,
                android.R.layout.simple_list_item_1,  // Single TextView layout
                countries
        ) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                // Get the view from parent (handles recycling)
                View view = super.getView(position, convertView, parent);

                Country env = getItem(position);
                if (env != null) {
                    // simple_list_item_1 has @android:id/text1 as the TextView
                    TextView textView = view.findViewById(android.R.id.text1);
                    textView.setText(env.getName()); // Show only name
                }
                return view;
            }
        };

        editTextCountry.setAdapter(adapter);
        editTextCountry.setText("MY", false);

        editTextCountry.setOnItemClickListener((parent, view, position, id) -> {
            Country selected = adapter.getItem(position);
            if (selected != null) {
                String nameSelected = selected.getName();
                String currencySelected = selected.getCurrency();
                editTextCurrency.setText(currencySelected);
            }
        });


    }
    public void channelDropDown(){
        AutoCompleteTextView channelDropdown  = findViewById(R.id.edt_mp_channel);
        String[] channels = new String[]{"TNG-EWALLET","maybank2u"};

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, channels);
        channelDropdown .setAdapter(adapter);

// Default selection without filtering
        channelDropdown.setText("TNG-EWALLET", false);

// Listen for selection
        channelDropdown .setOnItemClickListener((parent, view, position, id) -> {
            String selected = adapter.getItem(position);


        });

    }
}

class Country {
    private String name;
    private String currency;

    public Country(String name, String id) {
        this.name = name;
        this.currency = id;
    }

    public String getName() { return name; }
    public String getCurrency() { return currency; }

    @Override
    public String toString() {
        return name; // This shows in the collapsed AutoCompleteTextView
    }
}