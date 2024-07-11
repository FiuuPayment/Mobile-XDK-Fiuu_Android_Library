package com.molpay.molpayxdk.googlepay;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.WalletConstants;
import com.molpay.molpayxdk.MOLPayActivity;
import com.molpay.molpayxdk.R;
import com.molpay.molpayxdk.databinding.ActivityGooglepayBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

/**
 * Google Pay implementation for the app
 */
public class ActivityGP extends AppCompatActivity {

    public JSONObject paymentInput = new JSONObject();

    private static final int LOAD_TRANSACTION_DATA_REQUEST_CODE = 998;

    private ViewModelGP model;

    private ProgressBar pbLoading;

    private HashMap<String, Object> paymentDetails;

    public final static String MOLPayPaymentDetails = "paymentDetails";

    public static String COUNTRY_CODE = "MY";
    public static String CURRENCY_CODE = "MYR";
    public static int PAYMENTS_ENVIRONMENT = WalletConstants.ENVIRONMENT_TEST; // 3 = TEST & 1 = PRODUCTION

    // Handle potential conflict from calling loadPaymentData.
    ActivityResultLauncher<IntentSenderRequest> resolvePaymentForResult = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                Log.e("logGooglePay" , "resolvePaymentForResult");
                Log.e("logGooglePay" , "result.getResultCode() = " + result.getResultCode());
                switch (result.getResultCode()) {
                    case Activity.RESULT_OK:
                        Intent resultData = result.getData();
                        if (resultData != null) {
                            PaymentData paymentData = PaymentData.getFromIntent(result.getData());
                            if (paymentData != null) {
                                handlePaymentSuccess(paymentData);
                            }
                        }
                        break;

                    case Activity.RESULT_CANCELED:
                        // The user cancelled the payment attempt
                        setResult(RESULT_CANCELED, null);
                        finish();
                        break;

                    default:
                        // If Result = 1 finish with no response
                        setResult(RESULT_FIRST_USER, null);
                        finish();
                        break;
                }
            });

    /**
     * Initialize the Google Pay API on creation of the activity
     *
     * @see Activity#onCreate(Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        paymentDetails = (HashMap<String, Object>) getIntent().getSerializableExtra(MOLPayPaymentDetails);

        if (paymentDetails != null) {
            COUNTRY_CODE = Objects.requireNonNull(paymentDetails.get("mp_country")).toString();
            CURRENCY_CODE = Objects.requireNonNull(paymentDetails.get("mp_currency")).toString();

            if (Boolean.parseBoolean(Objects.requireNonNull(paymentDetails.get("mp_sandbox_mode")).toString())) {
                PAYMENTS_ENVIRONMENT = WalletConstants.ENVIRONMENT_TEST;
            } else {
                PAYMENTS_ENVIRONMENT = WalletConstants.ENVIRONMENT_PRODUCTION;
            }

        }

        initializeUi();

        // Check Google Pay availability
        model = new ViewModelProvider(this).get(ViewModelGP.class);
        model.canUseGooglePay.observe(this, this::setGooglePayAvailable);

        // Register a callback for handling the back press
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Do nothing - prevent user from performing backpress
                Log.e("logGooglePay" , "ActivityGP backpressed");
            }
        };

        // Add the callback to the OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void initializeUi() {

        // Use view binding to access the UI elements
        ActivityGooglepayBinding layoutBinding = ActivityGooglepayBinding.inflate(getLayoutInflater());
        setContentView(layoutBinding.getRoot());

        pbLoading = layoutBinding.pbLoading;
    }

    /**
     * If isReadyToPay returned {@code true}, show the button and hide the "checking" text.
     * Otherwise, notify the user that Google Pay is not available. Please adjust to fit in with
     * your current user flow. You are not required to explicitly let the user know if isReadyToPay
     * returns {@code false}.
     *
     * @param available isReadyToPay API response.
     */
    private void setGooglePayAvailable(boolean available) {
        if (available) {
            requestPayment();
        } else {
            Toast.makeText(this, R.string.google_pay_status_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    public void requestPayment() {

        Log.e("logGooglePay" , "requestPayment");
        Log.e("logGooglePay" , "mp_amount = " + Objects.requireNonNull(paymentDetails.get("mp_amount")).toString());
        Log.e("logGooglePay" , "totalPriceCents = " + Objects.requireNonNull(paymentDetails.get("mp_amount")).toString().replaceAll("[.,]",""));

        // The price provided to the API should include taxes and shipping.
        // This price is not displayed to the user.
        long totalPriceCents = Long.parseLong(Objects.requireNonNull(paymentDetails.get("mp_amount")).toString().replaceAll("[.,]",""));

        final Task<PaymentData> task = model.getLoadPaymentDataTask(totalPriceCents);

        task.addOnCompleteListener(completedTask -> {
            Log.e("logGooglePay" , "addOnCompleteListener");

            if (completedTask.isSuccessful()) {
                handlePaymentSuccess(completedTask.getResult());
            } else {
                Exception exception = completedTask.getException();
                if (exception instanceof ResolvableApiException) {
                    PendingIntent resolution = ((ResolvableApiException) exception).getResolution();
                    resolvePaymentForResult.launch(new IntentSenderRequest.Builder(resolution).build());

                } else if (exception instanceof ApiException) {
                    ApiException apiException = (ApiException) exception;
                    handleError(apiException.getStatusCode(), apiException.getMessage());

                } else {
                    handleError(CommonStatusCodes.INTERNAL_ERROR, "Unexpected non API" +
                            " exception when trying to deliver the task result to an activity!");
                }
            }

        });
    }

    /**
     * PaymentData response object contains the payment information, as well as any additional
     * requested information, such as billing and shipping address.
     *
     * @param paymentData A response object returned by Google after a payer approves payment.
     * @see <a href="https://developers.google.com/pay/api/android/reference/
     * object#PaymentData">PaymentData</a>
     */
    private void handlePaymentSuccess(PaymentData paymentData) {

        pbLoading.setVisibility(View.VISIBLE);
        Log.e("logGooglePay" , "handlePaymentSuccess");

        final String paymentInfo = paymentData.toJson();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault());

        if (paymentDetails != null) {
            try {
                // Extended VCode setting
                if (paymentDetails.get("mp_extended_vcode") == null) {
                    paymentInput.put("extendedVCode", false);
                } else {
                    paymentInput.put("extendedVCode", Objects.requireNonNull(paymentDetails.get("mp_extended_vcode")));
                }

                // TODO: Send the payment info e.g. (all info are compulsory) :
                paymentInput.put("orderId", Objects.requireNonNull(paymentDetails.get("mp_order_ID")).toString()); // Unique payment order id
                paymentInput.put("amount", Objects.requireNonNull(paymentDetails.get("mp_amount")).toString()); // Payment amount
                paymentInput.put("currency", CURRENCY_CODE); // Payment currency
                paymentInput.put("billName", Objects.requireNonNull(paymentDetails.get("mp_bill_name")).toString()); // Payer name
                paymentInput.put("billEmail", Objects.requireNonNull(paymentDetails.get("mp_bill_email")).toString()); // Payer email
                paymentInput.put("billPhone", Objects.requireNonNull(paymentDetails.get("mp_bill_mobile")).toString()); // Payer phone
                paymentInput.put("billDesc", Objects.requireNonNull(paymentDetails.get("mp_bill_description")).toString()); // Payment description
                paymentInput.put("merchantId", Objects.requireNonNull(paymentDetails.get("mp_merchant_ID")).toString()); // Your registered merchantId
                paymentInput.put("verificationKey", Objects.requireNonNull(paymentDetails.get("mp_verification_key")).toString()); // Your registered verificationKey

            /*
            TODO: Follow Google’s instructions to request production access for your app: https://developers.google.com/pay/api/android/guides/test-and-deploy/request-prod-access
            *
             Choose the integration type Gateway when prompted, and provide screenshots of your app for review.
             After your app has been approved, test your integration in production by setting the environment to GooglePayEnvironment.Production
             Then launching Google Pay from a signed, release build of your app.
             Remember to use your live mode verificationKey & merchantId. Set isSandbox = false for production environment.
             */
                paymentInput.put("isSandbox", Objects.requireNonNull(paymentDetails.get("mp_sandbox_mode")).toString()); // True = Testing ; False = Production

                JSONObject paymentInputObj = paymentInput;

                String paymentInput = paymentInputObj.toString();

                runOnUiThread(() -> {
                    Intent i = new Intent(ActivityGP.this, WebActivity.class); // Redirect To WebActivity (RMS library)
                    i.putExtra("paymentInput", paymentInput);
                    i.putExtra("paymentInfo", paymentInfo);
                    startActivityForResult(i, LOAD_TRANSACTION_DATA_REQUEST_CODE);
                });

            } catch (JSONException e) {
//            Log.e(Constants.LOG_GOOGLE_PAY, "handlePaymentSuccess JSONException: " + e);
            }
        }

    }

    /**
     * At this stage, the user has already seen a popup informing them an error occurred. Normally,
     * only logging is required.
     *
     * @param statusCode holds the value of any constant from CommonStatusCode or one of the
     *                   WalletConstants.ERROR_CODE_* constants.
     * @see <a href="https://developers.google.com/android/reference/com/google/android/gms/wallet/
     * WalletConstants#constant-summary">Wallet Constants Library</a>
     */
    private void handleError(int statusCode, @Nullable String message) {
//        Log.e("loadPaymentData failed", String.format(Locale.getDefault(), "Error code: %d, Message: %s", statusCode, message));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

//        CharSequence response;
        String response = "";

        if (requestCode == LOAD_TRANSACTION_DATA_REQUEST_CODE) {

            pbLoading.setVisibility(View.GONE);

            switch (resultCode) {

                case AppCompatActivity.RESULT_OK:

                    // Response Success CallBack
                    if (data != null) {
                        response = data.getStringExtra("response");

                        Log.e("logGooglePay" , "RESULT_OK response = " + response);

                        Intent result = new Intent();
                        result.putExtra(MOLPayActivity.MOLPayTransactionResult, response);
                        setResult(RESULT_OK, result);
                    } else {
                        Log.e("logGooglePay" , "RESULT_OK data = null");
                        setResult(RESULT_OK, null);
                    }

                    finish();

                    break;

                case AppCompatActivity.RESULT_CANCELED:
                    // The user cancelled the payment attempt
                    // Response Error CallBack
                    if (data != null) {
                        response = data.getStringExtra("response");
                        Log.e("logGooglePay" , "RESULT_CANCELED response = " + response);
                        Intent resultCancel = new Intent();
                        resultCancel.putExtra(MOLPayActivity.MOLPayTransactionResult, response);
                        setResult(RESULT_CANCELED, resultCancel);
                    } else {
                        Log.e("logGooglePay" , "RESULT_CANCELED data = null");
                        setResult(RESULT_CANCELED, null);
                    }

                    finish();
                    break;

                case AutoResolveHelper.RESULT_ERROR:
                    Status status = AutoResolveHelper.getStatusFromIntent(data);
                    if (status != null) {
                        handleError(status.getStatusCode() , status.getStatusMessage());
                    } else {
                        Log.e("logGooglePay" , "RESULT_ERROR status = null");
                        handleError(0 , "");
                    }
                    break;
            }
        }
    }

}