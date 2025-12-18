package com.molpay.molpayxdk.googlepay;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
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
import com.google.android.gms.wallet.contract.TaskResultContracts;
import com.google.gson.Gson;
import com.molpay.molpayxdk.MOLPayActivity;
import com.molpay.molpayxdk.R;
import com.molpay.molpayxdk.databinding.ActivityGooglepayBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Google Pay implementation for the app
 */
public class ActivityGP extends AppCompatActivity {

    public JSONObject paymentInput = new JSONObject();

    private static final int LOAD_TRANSACTION_DATA_REQUEST_CODE = 998;
    private static final int CANCEL_GPAY_TXN = 997;

    private ViewModelGP model;

    private ProgressBar pbLoading;

    private HashMap<String, Object> paymentDetails;

    public final static String MOLPayPaymentDetails = "paymentDetails";

    public static String COUNTRY_CODE = "MY";
    public static String CURRENCY_CODE = "MYR";
    public static int PAYMENTS_ENVIRONMENT = WalletConstants.ENVIRONMENT_TEST; // 3 = TEST & 1 = PRODUCTION

    public static String createTxnResult;
    public static String tranID = "";
    public static String verificationKey = "";
    public static long minTimeOut = 60000;

    private Boolean isEnableFullscreen = false;


    // Handle potential conflict from calling loadPaymentData.
    private final ActivityResultLauncher<Task<PaymentData>> paymentDataLauncher =
            registerForActivityResult(new TaskResultContracts.GetPaymentDataResult(), result -> {
                int statusCode = result.getStatus().getStatusCode();
                switch (statusCode) {
                    case CommonStatusCodes.SUCCESS:
                        if (result.getResult() != null) {
                            handlePaymentSuccess(result.getResult());
                        } else {
                            handleError(statusCode, "CommonStatusCodes.SUCCESS Result Null");
                        }
                        break;
                    case CommonStatusCodes.CANCELED:
                        CancelGPay("");
                        break;
                    default:
                        if (result.getStatus().getStatusMessage() != null) {
                            if ( ! result.getStatus().getStatusMessage().isEmpty()) {
                                handleError(statusCode, result.getStatus().getStatusMessage());
                            } else {
                                handleError(statusCode, "CommonStatusCodes Message Empty");
                            }
                        } else {
                            handleError(statusCode, "CommonStatusCodes Message Null");
                        }
                        break;
                }
            });

    public void CancelGPay (String paymentV2Response) {
        ApiRequestService.CancelTxn(paymentV2Response, new ApiRequestService.NetworkCallback() {
            @Override
            public void onSuccess(String responseJson) {

                runOnUiThread(() -> {
                    // Safely update UI here
                    Log.e("logGooglePay", "onSuccess = " + responseJson);
                    Intent i = new Intent(ActivityGP.this, WebActivity.class); // Redirect To WebActivity (RMS library)
                    i.putExtra("cancelResponse", responseJson);
                    startActivityForResult(i, CANCEL_GPAY_TXN);
                });
            }

            @Override
            public void onFailure(String error) {
                Log.e("logGooglePay", "ActivityGP ApiRequestService.CancelTxn onFailure = " + error);

                if (error != null) {
                    if ( ! error.isEmpty()) {
                        sendCustomFailResponse("Payment aborted. Error : " + error);
                    } else {
                        sendCustomFailResponse("Payment aborted. Error : empty");
                    }
                } else {
                    sendCustomFailResponse("Payment aborted. Error : null");
                }
            }
        } , paymentDetails);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isRooted = MOLPayActivity.isDeviceRooted(ActivityGP.this);
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

        paymentDetails = (HashMap<String, Object>) getIntent().getSerializableExtra(MOLPayPaymentDetails);

        if (paymentDetails != null) {

            JSONObject json = new JSONObject(paymentDetails);

            if (json.has("mp_enable_fullscreen")) {
                try {
                    isEnableFullscreen = json.getBoolean("mp_enable_fullscreen");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (isEnableFullscreen) {
                    setTheme(R.style.Theme_Fullscreen);

                    View decorView = getWindow().getDecorView();
                    decorView.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    );
                }
            }

            COUNTRY_CODE = Objects.requireNonNull(paymentDetails.get("mp_country")).toString();
            CURRENCY_CODE = Objects.requireNonNull(paymentDetails.get("mp_currency")).toString();

            if ( ! COUNTRY_CODE.equalsIgnoreCase("MY") || ! CURRENCY_CODE.equalsIgnoreCase("MYR") ) {
                paymentDetails.put(MOLPayActivity.mp_gpay_channel, new String[] { "CC" });
            }

            verificationKey = Objects.requireNonNull(paymentDetails.get(MOLPayActivity.mp_verification_key)).toString();

            if (paymentDetails.get("mp_sandbox_mode") == null) {
                PAYMENTS_ENVIRONMENT = WalletConstants.ENVIRONMENT_PRODUCTION;
            } else {
                if (Boolean.parseBoolean(Objects.requireNonNull(paymentDetails.get("mp_sandbox_mode")).toString())) {
                    PAYMENTS_ENVIRONMENT = WalletConstants.ENVIRONMENT_TEST;
                } else {
                    PAYMENTS_ENVIRONMENT = WalletConstants.ENVIRONMENT_PRODUCTION;
                }
            }
        }

        initializeUi();

        Log.e("logGooglePay", "PAYMENTS_ENVIRONMENT = " + PAYMENTS_ENVIRONMENT);

        ApiRequestService.CreateTxn(new ApiRequestService.NetworkCallback() {
            @Override
            public void onSuccess(String responseJson) {

                runOnUiThread(() -> {
                    // Safely update UI here
                    Log.e("logGooglePay", "CreateTxn onSuccess = " + responseJson);

                    try {
                        JSONObject jsonObject = new JSONObject(responseJson);
                        String returnCode = jsonObject.getString("return_code");

                        if (returnCode.contains("fail")) {
                            String message = jsonObject.getString("message");
                            sendCustomFailResponse(message);
                            return;
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    createTxnResult = responseJson;

                    // Check Google Pay availability
                    model = new ViewModelProvider(ActivityGP.this).get(ViewModelGP.class);
                    model.canUseGooglePay.observe(ActivityGP.this, ActivityGP.this::setGooglePayAvailable);

                    // Register a callback for handling the back press
                    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
                        @Override
                        public void handleOnBackPressed() {
                            // Do nothing - prevent user from performing backpress
                            Log.e("logGooglePay", "ActivityGP backpressed");
                        }
                    };

                    // Add the callback to the OnBackPressedDispatcher
                    getOnBackPressedDispatcher().addCallback(ActivityGP.this, callback);
                });
            }

            @Override
            public void onFailure(String error) {
                Log.e("logGooglePay", "ActivityGP createTxn.php onFailure = " + error);
                // Send custom failed response
                if (error != null) {
                    if ( ! error.isEmpty()) {
                        sendCustomFailResponse("Payment aborted. Error : " + error);
                    } else {
                        sendCustomFailResponse("Payment aborted. Error : empty");
                    }
                } else {
                    sendCustomFailResponse("Payment aborted. Error : null");
                }
            }
        } , paymentDetails);
    }

    private void sendCustomFailResponse(String failMessage) {
        Log.e("logGooglePay", "sendCustomFailResponse");
        // Send custom failed response
        Map<String, Object> data = new HashMap<>();
        data.put("StatCode", "11");
        data.put("StatName", "failed");
        data.put("TranID", tranID);
        data.put("Amount", Objects.requireNonNull(paymentDetails.get(MOLPayActivity.mp_amount)).toString());
        data.put("Domain", Objects.requireNonNull(paymentDetails.get(MOLPayActivity.mp_merchant_ID)).toString());
        data.put("VrfKey", "");
        data.put("Channel", "GooglePay");
        data.put("OrderID", Objects.requireNonNull(paymentDetails.get(MOLPayActivity.mp_order_ID)).toString());
        data.put("Currency", Objects.requireNonNull(paymentDetails.get(MOLPayActivity.mp_currency)).toString());
        data.put("ErrorCode", "GOOGLEPAY_PE");
        data.put("ErrorDesc", failMessage);
        data.put("ProcessorResponseCode", null);
        data.put("ProcessorCVVResponse", null);
        data.put("SchemeTransactionID", null);
        data.put("MerchantAdviceCode", null);
        data.put("ECI", null);
        data.put("3DSVersion", null);
        data.put("ACSTransactionID", null);
        data.put("3DSTransactionID", null);

        Gson gson = new Gson();
        String jsonGPayCancel = gson.toJson(data);

        Log.e("logGooglePay", "jsonGPayCancel = " + jsonGPayCancel);

        Intent resultCancel = new Intent();
        resultCancel.putExtra(MOLPayActivity.MOLPayTransactionResult, jsonGPayCancel);
        setResult(RESULT_CANCELED, resultCancel); // pass back to MainActivity
        finish(); // finish ActivityGP
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
            Toast toast = Toast.makeText(getApplicationContext(),
                    R.string.google_pay_status_unavailable,
                    Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            sendCustomFailResponse("Payment aborted. Device not supported Google Pay. Please use other payment method.");
        }
    }

    public void requestPayment() {

        Log.e("logGooglePay", "requestPayment");
        Log.e("logGooglePay", "mp_amount = " + Objects.requireNonNull(paymentDetails.get("mp_amount")).toString());
        Log.e("logGooglePay", "totalPriceCents = " + Objects.requireNonNull(paymentDetails.get("mp_amount")).toString().replaceAll("[.,]", ""));
        // The price provided to the API should include taxes and shipping.
        // This price is not displayed to the user.
        String totalPriceCents = Objects.requireNonNull(paymentDetails.get("mp_amount")).toString().replaceAll("[,]", "");

        final Task<PaymentData> task = model.getLoadPaymentDataTask(totalPriceCents);
        task.addOnCompleteListener(paymentDataLauncher::launch);
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
        Log.e("logGooglePay", "handlePaymentSuccess");

        final String paymentInfo = paymentData.toJson();

        if (paymentDetails != null) {
            try {
                // Extended VCode setting
                if (paymentDetails.get("mp_extended_vcode") == null) {
                    paymentInput.put("extendedVCode", false);
                } else {
                    paymentInput.put("extendedVCode", Objects.requireNonNull(paymentDetails.get("mp_extended_vcode")));
                }

                // Close button setting
                if (paymentDetails.get(MOLPayActivity.mp_closebutton_display) == null) {
                    paymentInput.put("closeButton", false);
                } else {
                    paymentInput.put("closeButton", Objects.requireNonNull(paymentDetails.get(MOLPayActivity.mp_closebutton_display)));
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
                if (paymentDetails.get("mp_sandbox_mode") == null) {
                    paymentInput.put("isSandbox", false);
                } else {
                    paymentInput.put("isSandbox", Objects.requireNonNull(paymentDetails.get("mp_sandbox_mode")));
                }

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
        Log.e("logGooglePay", String.format(Locale.getDefault(), "Error code: %d, Message: %s", statusCode, message));
        sendCustomFailResponse("Payment aborted.\nError Code: " + statusCode + "\nMessage : " + message);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

//        CharSequence response;
        String response;

        if (requestCode == LOAD_TRANSACTION_DATA_REQUEST_CODE) {

            pbLoading.setVisibility(View.GONE);

            switch (resultCode) {

                case AppCompatActivity.RESULT_OK:

                    // Response Success CallBack
                    if (data != null) {
                        response = data.getStringExtra("response");

                        Log.e("logGooglePay", "RESULT_OK response = " + response);

                        Intent result = new Intent();
                        result.putExtra(MOLPayActivity.MOLPayTransactionResult, response);
                        setResult(RESULT_OK, result);
                        finish();
                    } else {
                        Log.e("logGooglePay", "RESULT_OK data = null");
                        CancelGPay("");
                    }

                    break;

                case AppCompatActivity.RESULT_CANCELED:
                    // The user cancelled the payment attempt
                    // Response Error CallBack
                    if (data != null) {
                        response = data.getStringExtra("response");
                        Log.e("logGooglePay", "RESULT_CANCELED response = " + response);
                        assert response != null;
                        if (response.contains("StatCode")) {
                            try {
                                JSONObject jsonObject = new JSONObject(response);
                                String statCode = jsonObject.getString("StatCode");
                                if (statCode.equalsIgnoreCase("11")) {
                                    Intent resultCancel = new Intent();
                                    resultCancel.putExtra(MOLPayActivity.MOLPayTransactionResult, response);
                                    setResult(RESULT_CANCELED, resultCancel); // pass back to MainActivity
                                    finish(); // finish ActivityGP
                                } else {
                                    CancelGPay("");
                                }
                            } catch (JSONException e) {
                                CancelGPay("");
                            }
                        } else {
                            CancelGPay(response);
                        }
                    } else {
                        Log.e("logGooglePay", "RESULT_CANCELED ActivityGP 1 data = null");
                        CancelGPay("");
                    }

                    break;

                case AutoResolveHelper.RESULT_ERROR:
                    Status status = AutoResolveHelper.getStatusFromIntent(data);
                    if (status != null) {
                        handleError(status.getStatusCode(), status.getStatusMessage());
                    } else {
                        Log.e("logGooglePay", "RESULT_ERROR status = null");
                        handleError(0, "AutoResolveHelper.RESULT_ERROR Status Null");
                    }
                    break;
            }
        }
        else if (requestCode == CANCEL_GPAY_TXN) {
            assert data != null;
            response = data.getStringExtra("response");
            Intent resultCancel = new Intent();
            resultCancel.putExtra(MOLPayActivity.MOLPayTransactionResult, response);
            Log.e("logGooglePay", "RESULT_CANCELED ActivityGP 2");
            setResult(RESULT_CANCELED, resultCancel); // pass back to MainActivity
            finish(); // finish ActivityGP
        }
    }
}