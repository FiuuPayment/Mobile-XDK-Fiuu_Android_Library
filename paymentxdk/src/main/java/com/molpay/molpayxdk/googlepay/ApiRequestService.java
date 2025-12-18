/*
 * Copyright 2023 Razer Merchant Services.
 */

package com.molpay.molpayxdk.googlepay;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.wallet.WalletConstants;
import com.molpay.molpayxdk.MOLPayActivity;
import com.molpay.molpayxdk.googlepay.Helper.ApplicationHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import okhttp3.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

public class ApiRequestService {

    static class Production {
        static final String BASE_PAYMENT = "https://pay.fiuu.com/";
        static final String API_PAYMENT = "https://api.fiuu.com/";
    }

    static class Development {
        static final String SB_PAYMENT_FIUU = "https://sandbox-payment.fiuu.com/";
        static final String SB_API_FIUU = "https://sandbox-api.fiuu.com/";
    }

    public static String merchantName = "";
    private static String signature;
    private static Boolean extendedVcode;

    public ApiRequestService() {
    }

    public interface NetworkCallback {
        void onSuccess(String responseJson);
        void onFailure(String error);
    }

    public static void CancelTxn(String paymentV2Response, NetworkCallback callback, HashMap<String, Object> paymentDetails) {

        Log.e("logGooglePay", "ActivityGP.tranID = " + ActivityGP.tranID);

        String endPoint = "";

        if (ActivityGP.PAYMENTS_ENVIRONMENT == WalletConstants.ENVIRONMENT_PRODUCTION) {
            endPoint = Production.BASE_PAYMENT + "RMS/GooglePay/cancel.php";
        } else if (ActivityGP.PAYMENTS_ENVIRONMENT == WalletConstants.ENVIRONMENT_TEST) {
            endPoint = Development.SB_PAYMENT_FIUU + "RMS/GooglePay/cancel.php";
        }

        Log.e("logGooglePay", endPoint);

        OkHttpClient client = new OkHttpClient();
        RequestBody formBody;

        if (paymentV2Response.isEmpty()) {
            // Cancel before proceed payment V2
            formBody = new FormBody.Builder()
                    .add("MerchantID", Objects.requireNonNull(paymentDetails.get(MOLPayActivity.mp_merchant_ID)).toString())
                    .add("ReferenceNo", Objects.requireNonNull(paymentDetails.get(MOLPayActivity.mp_order_ID)).toString())
                    .add("TxnID", ActivityGP.tranID)
                    .add("TxnType", "SALS")
                    .add("TxnCurrency", Objects.requireNonNull(paymentDetails.get(MOLPayActivity.mp_currency)).toString())
                    .add("TxnAmount", Objects.requireNonNull(paymentDetails.get(MOLPayActivity.mp_amount)).toString())
                    .add("mpsl_version", "2")
                    .build();
        } else {
            // Cancel after get payment v2 error
            FormBody.Builder formBuilder = new FormBody.Builder();
            try {
                JSONObject json = new JSONObject(paymentV2Response);
                Iterator<String> keys = json.keys();

                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = json.getString(key);
                    formBuilder.add(key, value);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                callback.onFailure("Invalid JSON format: " + e.getMessage());
                return;
            }

            formBody = formBuilder.build();
        }

        // Build the request
        Request request = new Request.Builder()
                .url(endPoint)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
//                Log.e("logGooglePay", "ApiRequestService cancel.php onFailure = " + e.getMessage());
                if (e instanceof UnknownHostException) {
                    // No internet or DNS issue
                    callback.onFailure("Unable to reach the server. Please check your internet connection or use other payment method. " + e.getMessage());
                } else if (e instanceof SocketTimeoutException) {
                    // Server took too long to respond
                    callback.onFailure("Request timed out. Please try again later or use other payment method. " + e.getMessage());
                } else if (e instanceof ConnectException) {
                    // Could not connect to server
                    callback.onFailure("Unable to connect to the server. Please try again later or use other payment method. " + e.getMessage());
                } else if (e instanceof SSLHandshakeException
                        || e instanceof SSLPeerUnverifiedException) {
                    // SSL certificate problem
                    callback.onFailure("We’re having trouble connecting. Please try again later or use other payment method. " + e.getMessage());
                } else {
                    // Fallback for anything else
                    callback.onFailure("An unexpected error occurred. Please try again or use other payment method. " + e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
//                    Log.e("logGooglePay", "Unexpected response: " + response.toString());
                    callback.onFailure("Unexpected response. Please try again or use other payment method. " + response.toString());
                } else {
                    String responseBody = response.body().string();
                    Log.e("logGooglePay", "onResponse responseBody = " + responseBody);
                    callback.onSuccess(responseBody);
                }
            }
        });
    }

    public static void CreateTxn(NetworkCallback callback , HashMap<String, Object> paymentDetails) {

        OkHttpClient client = new OkHttpClient();
        FormBody formBody = null;
        String endPoint = "";

        if (ActivityGP.PAYMENTS_ENVIRONMENT == WalletConstants.ENVIRONMENT_PRODUCTION) {
            endPoint = Production.BASE_PAYMENT + "RMS/GooglePay/createTxn.php";
        } else if (ActivityGP.PAYMENTS_ENVIRONMENT == WalletConstants.ENVIRONMENT_TEST) {
            endPoint = Development.SB_PAYMENT_FIUU + "RMS/GooglePay/createTxn.php";
        }

        Log.e("logGooglePay", endPoint);

        if (paymentDetails != null) {

            Log.e("logGooglePay", "paymentDetails NOT NULL");

            if (paymentDetails.get("mp_extended_vcode") == null) {
                extendedVcode = false;
            } else {
                Log.e("logGooglePay", "mp_extended_vcode = " + paymentDetails.get("mp_extended_vcode"));
                extendedVcode = (Boolean) paymentDetails.get("mp_extended_vcode");
            }

            signature = ApplicationHelper.getInstance().GetVCode(
                    Objects.requireNonNull(paymentDetails.get("mp_amount")).toString(),
                    Objects.requireNonNull(paymentDetails.get("mp_merchant_ID")).toString(),
                    Objects.requireNonNull(paymentDetails.get("mp_order_ID")).toString(),
                    Objects.requireNonNull(paymentDetails.get("mp_verification_key")).toString(),
                    Objects.requireNonNull(paymentDetails.get("mp_currency")).toString(),
                    extendedVcode
            );

            FormBody.Builder formBuilder = new FormBody.Builder()
                    .add("MerchantID", Objects.requireNonNull(paymentDetails.get("mp_merchant_ID")).toString())
                    .add("ReferenceNo", Objects.requireNonNull(paymentDetails.get("mp_order_ID")).toString())
                    .add("TxnType", "SALS")
                    .add("TxnCurrency", Objects.requireNonNull(paymentDetails.get("mp_currency")).toString())
                    .add("TxnAmount", Objects.requireNonNull(paymentDetails.get("mp_amount")).toString())
                    .add("Signature", signature)
                    .add("CustName", Objects.requireNonNull(paymentDetails.get("mp_bill_name")).toString())
                    .add("CustContact", Objects.requireNonNull(paymentDetails.get("mp_bill_mobile")).toString())
                    .add("CustEmail", Objects.requireNonNull(paymentDetails.get("mp_bill_email")).toString())
                    .add("mpsl_version", "2")
                    .add("vc_channel", "indexAN")
                    .add("ReturnURL", "")
                    .add("NotificationURL", "")
                    .add("CallbackURL", "")
                    .add("ExpirationTime", "");

            // Handle paymentMethods[] from String[] mp_gpay_channel
            if (paymentDetails.get("mp_gpay_channel") != null) {
                String[] gpayChannels = (String[]) paymentDetails.get("mp_gpay_channel");
                for (int i = 0; i < Objects.requireNonNull(gpayChannels).length; i++) {
                    formBuilder.add("paymentMethods[" + i + "]", gpayChannels[i]);
                }
            } else {
                formBuilder.add("paymentMethods[" + 0 + "]", "CC");
                formBuilder.add("paymentMethods[" + 1 + "]", "TNG-EWALLET");
                formBuilder.add("paymentMethods[" + 2 + "]", "SHOPEEPAY");
            }

            formBody = formBuilder.build();

            // Log all fields
            for (int i = 0; i < formBody.size(); i++) {
                String name = formBody.encodedName(i);
                String value = formBody.encodedValue(i);
                Log.e("logGooglePay", name + " = " + value);
            }

            Request request = new Request.Builder()
                    .url(endPoint)
                    .post(formBody)
                    .build();

            Log.e("logGooglePay", "before client.newCall");

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
//                    Log.e("logGooglePay", "ApiRequestServicec createTxn.php onFailure = " + e.getMessage());
                    if (e instanceof UnknownHostException) {
                        // No internet or DNS issue
                        callback.onFailure("Unable to reach the server. Please check your internet connection or use other payment method. " + e.getMessage());
                    } else if (e instanceof SocketTimeoutException) {
                        // Server took too long to respond
                        callback.onFailure("Request timed out. Please try again later or use other payment method. " + e.getMessage());
                    } else if (e instanceof ConnectException) {
                        // Could not connect to server
                        callback.onFailure("Unable to connect to the server. Please try again later or use other payment method. " + e.getMessage());
                    } else if (e instanceof SSLHandshakeException
                            || e instanceof SSLPeerUnverifiedException) {
                        // SSL certificate problem
                        callback.onFailure("We’re having trouble connecting. Please try again later or use other payment method. " + e.getMessage());
                    } else {
                        // Fallback for anything else
                        callback.onFailure("An unexpected error occurred. Please try again or use other payment method. " + e.getMessage());
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
//                        Log.e("logGooglePay", "Unexpected response: " + response.toString());
                        callback.onFailure("Unexpected response. Please try again or use other payment method. " + response.toString());
                    } else {
                        String responseBody = response.body().string();
                        Log.e("logGooglePay", "onResponse responseBody = " + responseBody);

                        if (paymentDetails.get("mp_company") != null) {
                            merchantName = Objects.requireNonNull(paymentDetails.get("mp_company")).toString();
                        } else {
                            JSONObject jsonObject;
                            try {
                                jsonObject = new JSONObject(responseBody);
                                merchantName = jsonObject.getString("DBA");
                            } catch (JSONException e) {
                                merchantName = "";
                            }
                        }
                        callback.onSuccess(responseBody);
                    }
                }
            });
        } else {
            Log.e("logGooglePay", "paymentDetails == NULL");
        }

    }

    public Object GetPaymentRequest(JSONObject paymentInput, String paymentInfo ) {

        try {
            String endPoint = "";
            String txnType = "SALS";
            String orderId = paymentInput.getString("orderId");
            String amount = paymentInput.getString("amount");
            String currency = paymentInput.getString("currency");
            boolean extendedVCode = paymentInput.getBoolean("extendedVCode");
            String billName = paymentInput.getString("billName");
            String billEmail = paymentInput.getString("billEmail");
            String billPhone = paymentInput.getString("billPhone");
            String billDesc = paymentInput.getString("billDesc");
            String merchantId = paymentInput.getString("merchantId");
            String verificationKey = paymentInput.getString("verificationKey");

            if (ActivityGP.PAYMENTS_ENVIRONMENT == WalletConstants.ENVIRONMENT_PRODUCTION) {
                endPoint = Production.BASE_PAYMENT + "RMS/GooglePay/payment_v2.php";
            } else if (ActivityGP.PAYMENTS_ENVIRONMENT == WalletConstants.ENVIRONMENT_TEST) {
                endPoint = Development.SB_PAYMENT_FIUU + "RMS/GooglePay/payment_v2.php";
            }

            Uri uri = Uri.parse(endPoint)
                    .buildUpon()
                    .build();

            //"Signature": "<MD5(amount+merchantID+referenceNo+Vkey)>",
            String vCode = ApplicationHelper.getInstance().GetVCode(
                amount,
                merchantId,
                orderId,
                verificationKey,
                currency,
                extendedVCode
            );

            String GooglePayBase64 = Base64.getEncoder()
                                    .encodeToString(paymentInfo.getBytes());

            String requery;
            if (WebActivity.paymentV2Requery.isEmpty()) {
                requery = "0";
            } else {
                requery = WebActivity.paymentV2Requery;
            }

            Log.e("logGooglePay", "endPoint = " + endPoint);
            Log.e("logGooglePay", "MerchantID = " + merchantId);
            Log.e("logGooglePay", "ReferenceNo = " + orderId);
            Log.e("logGooglePay", "TxnType = " + txnType);
            Log.e("logGooglePay", "TxnCurrency = " + currency);
            Log.e("logGooglePay", "TxnAmount = " + amount);
            Log.e("logGooglePay", "CustName = " + billName);
            Log.e("logGooglePay", "CustEmail = " + billEmail);
            Log.e("logGooglePay", "CustContact = " + billPhone);
            Log.e("logGooglePay", "CustDesc = " + billDesc);
            Log.e("logGooglePay", "Signature = " + vCode);
            Log.e("logGooglePay", "mpsl_version = 2");
            Log.e("logGooglePay", "requery = " + requery);
            Log.e("logGooglePay", "GooglePay = " + GooglePayBase64);

            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("MerchantID", merchantId)
                    .appendQueryParameter("ReferenceNo", orderId)
                    .appendQueryParameter("TxnType", txnType)
                    .appendQueryParameter("TxnCurrency", currency)
                    .appendQueryParameter("TxnAmount", amount)
                    .appendQueryParameter("CustName", billName)
                    .appendQueryParameter("CustEmail", billEmail)
                    .appendQueryParameter("CustContact", billPhone)
                    .appendQueryParameter("CustDesc", billDesc)
                    .appendQueryParameter("Signature", vCode)
                    .appendQueryParameter("mpsl_version", "2")
                    .appendQueryParameter("tranID", ActivityGP.tranID)
                    .appendQueryParameter("requery", requery)
                    .appendQueryParameter("GooglePay", GooglePayBase64);

            WebActivity.paymentV2Requery = "0";

                return postRequest(uri, builder);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Object GetPaymentResult(JSONObject transaction ) {
        try {
            String endPoint = "";

            if (ActivityGP.PAYMENTS_ENVIRONMENT == WalletConstants.ENVIRONMENT_PRODUCTION) {
                endPoint = Production.API_PAYMENT + "RMS/q_by_tid.php";
            } else if (ActivityGP.PAYMENTS_ENVIRONMENT == WalletConstants.ENVIRONMENT_TEST) {
                endPoint = Development.SB_API_FIUU + "RMS/q_by_tid.php";
            }

            Uri uri = Uri.parse(endPoint)
                    .buildUpon()
                    .build();

            String txID = transaction.getString("txID");
            String amount = transaction.getString("amount");
            String merchantId = transaction.getString("merchantId");
            String verificationKey = transaction.getString("verificationKey");

            String sKey = ApplicationHelper.getInstance().GetSKey(
                    txID,
                    merchantId,
                    verificationKey,
                    amount
            );

            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("amount", amount)
                    .appendQueryParameter("txID", txID)
                    .appendQueryParameter("domain", merchantId)
                    .appendQueryParameter("skey", sKey)
                    .appendQueryParameter("url", "")
                    .appendQueryParameter("type", "2");

            return postRequest(uri, builder);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject postRequest(final Uri uri, final Uri.Builder params) throws JSONException {

        HttpURLConnection httpConnection = null;
        try {

            URL url = new URL(uri.toString());
            httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setRequestMethod("POST");
            httpConnection.setRequestProperty("Accept", "application/json");
            httpConnection.setRequestProperty("Cookies", "PHPSESSID=ad6081qpihsb9en1nr9nivbkl3");
            httpConnection.setRequestProperty("SDK-Version", "4.0.0");
            httpConnection.setDoOutput(true);
            httpConnection.setDoInput(true);

            String query = params.build().getEncodedQuery();

            OutputStream outputStream = httpConnection.getOutputStream();

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            writer.write(query);
            writer.flush();
            writer.close();

            outputStream.close();

            return parse(httpConnection);
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject(String.format("{\"exception\":\"%s\"}", e.getMessage()));
        } finally {
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }

    }

    private JSONObject parse(HttpURLConnection httpURLConnection) throws JSONException {

        JSONObject response = new JSONObject();

        try {
            response.put("statusCode", httpURLConnection.getResponseCode());
            response.put("responseMessage", httpURLConnection.getResponseMessage());
            response.put("responseBody", getResponseBody(httpURLConnection));

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject(String.format("{\"exception\":\"%s\"}", e.getMessage()));
        }
    }

    public static String getResponseBody(HttpURLConnection conn) {

        BufferedReader br = null;
        StringBuilder body = null;
        String line = "";

        try {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            body = new StringBuilder();

            while ((line = br.readLine()) != null)
                body.append(line);

            return body.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}