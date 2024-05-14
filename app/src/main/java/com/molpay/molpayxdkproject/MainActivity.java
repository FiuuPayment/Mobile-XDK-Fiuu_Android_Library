package com.molpay.molpayxdkproject;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.wallet.button.ButtonConstants;
import com.google.android.gms.wallet.button.ButtonOptions;
import com.google.android.gms.wallet.button.PayButton;
import com.molpay.molpayxdk.MOLPayActivity;
import com.molpay.molpayxdk.googlepay.ActivityGP;
import com.molpay.molpayxdk.googlepay.UtilGP;

import org.json.JSONException;

import java.util.Calendar;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private PayButton googlePayButton;

    private void restartmolpay() {
        HashMap<String, Object> paymentDetails = new HashMap<>();
        paymentDetails.put(MOLPayActivity.mp_amount, "1.10");

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

        // TODO: Learn more about optional parameters here https://github.com/RazerMS/Mobile-XDK-RazerMS_Android_Studio/wiki/Installation-Guidance#prepare-the-payment-detail-object
//        paymentDetails.put(MOLPayActivity.mp_channel_editing, false);
//        paymentDetails.put(MOLPayActivity.mp_editing_enabled, true);
//        paymentDetails.put(MOLPayActivity.mp_express_mode, false);
//        paymentDetails.put(MOLPayActivity.mp_dev_mode, false);
//        paymentDetails.put(MOLPayActivity.mp_preferred_token, "new");

        Intent intent = new Intent(MainActivity.this, MOLPayActivity.class);
        intent.putExtra(MOLPayActivity.MOLPayPaymentDetails, paymentDetails);
        startActivityForResult(intent, MOLPayActivity.MOLPayXDK);
    }

    private void googlePayPayment() {
        HashMap<String, Object> paymentDetails = new HashMap<>();

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

        paymentDetails.put(MOLPayActivity.mp_amount, "1.11"); // Must be in 2 decimal points format
        paymentDetails.put(MOLPayActivity.mp_order_ID, Calendar.getInstance().getTimeInMillis()); // Must be unique
        paymentDetails.put(MOLPayActivity.mp_currency, "MYR"); // Must matched mp_country
        paymentDetails.put(MOLPayActivity.mp_country, "MY"); // Must matched mp_currency
        paymentDetails.put(MOLPayActivity.mp_bill_description, "The bill description");
        paymentDetails.put(MOLPayActivity.mp_bill_name, "The bill name");
        paymentDetails.put(MOLPayActivity.mp_bill_email, "payer.email@fiuu.com");
        paymentDetails.put(MOLPayActivity.mp_bill_mobile, "123456789");

        Intent intent = new Intent(MainActivity.this, ActivityGP.class); // Used ActivityGP for Google Pay
        intent.putExtra(MOLPayActivity.MOLPayPaymentDetails, paymentDetails);
        startActivityForResult(intent, MOLPayActivity.MOLPayXDK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        Log.e("logGooglePay" , "onActivityResult requestCode = " + requestCode);
        Log.e("logGooglePay" , "onActivityResult resultCode = " + resultCode);

        if (requestCode == MOLPayActivity.MOLPayXDK && resultCode == RESULT_OK){
            Log.d(MOLPayActivity.MOLPAY, "MOLPay result = "+data.getStringExtra(MOLPayActivity.MOLPayTransactionResult));
            TextView tw = (TextView)findViewById(R.id.resultTV);
            tw.setText(data.getStringExtra(MOLPayActivity.MOLPayTransactionResult));
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // The Google Pay button is a layout file – take the root view
        googlePayButton = findViewById(R.id.googlePayButton);

        try {
            // TODO: Choose your preferred Google Pay button : https://developers.google.com/pay/api/android/guides/brand-guidelines
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
}
