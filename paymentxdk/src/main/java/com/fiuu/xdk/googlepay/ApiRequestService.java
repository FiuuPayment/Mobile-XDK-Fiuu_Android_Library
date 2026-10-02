/*
 * Copyright 2023 Razer Merchant Services.
 */

package com.fiuu.xdk.googlepay;

import android.net.Uri;
import android.util.Log;

import com.fiuu.xdk.PaymentActivity;
import com.fiuu.xdk.googlepay.Helper.ApplicationHelper;
import com.fiuu.xdk.network.GatewayEndpoints;
import com.fiuu.xdk.network.PaymentEnvironment;
import com.google.android.gms.wallet.WalletConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiRequestService {

    public static String merchantName = "";
    private static String signature;
    private static Boolean extendedVcode;

    public ApiRequestService() {
    }

    private static PaymentEnvironment getGPayEnvironment() {
        if (ActivityGP.PAYMENTS_ENVIRONMENT == WalletConstants.ENVIRONMENT_PRODUCTION) {
            return PaymentEnvironment.PRODUCTION;
        } else {
            return PaymentEnvironment.SANDBOX;
        }
    }

    public interface NetworkCallback {
        void onSuccess(String responseJson);
        void onFailure(String error);
    }

    public static void CancelTxn(String paymentV2Response, NetworkCallback callback, HashMap<String, Object> paymentDetails) {

        String endPoint = GatewayEndpoints.getGPayCancelUrl(getGPayEnvironment());

        OkHttpClient client = new OkHttpClient();
        RequestBody formBody;

        if (paymentV2Response.isEmpty()) {
            // Cancel before proceed payment V2
            formBody = new FormBody.Builder()
                    .add("MerchantID", Objects.requireNonNull(paymentDetails.get(PaymentActivity.mp_merchant_ID)).toString())
                    .add("ReferenceNo", Objects.requireNonNull(paymentDetails.get(PaymentActivity.mp_order_ID)).toString())
                    .add("TxnID", ActivityGP.tranID)
                    .add("TxnType", "SALS")
                    .add("TxnCurrency", Objects.requireNonNull(paymentDetails.get(PaymentActivity.mp_currency)).toString())
                    .add("TxnAmount", Objects.requireNonNull(paymentDetails.get(PaymentActivity.mp_amount)).toString())
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
                    callback.onSuccess(responseBody);
                }
            }
        });
    }

    public static void CreateTxn(NetworkCallback callback , HashMap<String, Object> paymentDetails) {

        OkHttpClient client = new OkHttpClient();
        FormBody formBody = null;
        String endPoint = GatewayEndpoints.getGPayCreateTxnUrl(getGPayEnvironment());

        if (paymentDetails != null) {

            if (paymentDetails.get("mp_extended_vcode") == null) {
                extendedVcode = false;
            } else {
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
            }

            try {
                appendBinLockFields(formBuilder, paymentDetails);
            } catch (IllegalArgumentException e) {
                callback.onFailure(e.getMessage());
                return;
            }

            formBody = formBuilder.build();

            Request request = new Request.Builder()
                    .url(endPoint)
                    .post(formBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
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
                        callback.onFailure("Unexpected response. Please try again or use other payment method. " + response.toString());
                    } else {
                        String responseBody = response.body().string();

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

            String endPoint = GatewayEndpoints.getGPayPaymentV2Url(getGPayEnvironment());

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

            JSONObject googlePay = new JSONObject(paymentInfo);
            googlePay.put("internalVersion", 2);
            String GooglePayBase64 = Base64.getEncoder()
                                    .encodeToString(googlePay.toString().getBytes(StandardCharsets.UTF_8));

            String requery;
            if (WebActivity.paymentV2Requery.isEmpty()) {
                requery = "0";
            } else {
                requery = WebActivity.paymentV2Requery;
            }

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

            try {
                appendBinLockFields(builder, paymentInput);
            } catch (IllegalArgumentException e) {
                return null;
            }

            WebActivity.paymentV2Requery = "0";

            return postRequest(uri, builder);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Forward merchant BIN lock from paymentDetails onto Google Pay form posts.
     * Accepts String[], Collection, JSONArray, JSON string, or comma-separated string.
     */
    static void appendBinLockFields(FormBody.Builder formBuilder, HashMap<String, Object> paymentDetails) {
        if (formBuilder == null || paymentDetails == null) {
            return;
        }
        List<String> bins = extractBinLockList(paymentDetails.get(PaymentActivity.mp_bin_lock));
        for (int i = 0; i < bins.size(); i++) {
            formBuilder.add("mp_bin_lock[" + i + "]", bins.get(i));
        }
        Object errMsg = paymentDetails.get(PaymentActivity.mp_bin_lock_err_msg);
        if (errMsg != null) {
            String msg = errMsg.toString().trim();
            if (!msg.isEmpty()) {
                formBuilder.add("mp_bin_lock_err_msg", msg);
            }
        }
    }

    static void appendBinLockFields(Uri.Builder builder, JSONObject paymentInput) {
        if (builder == null || paymentInput == null) {
            return;
        }
        List<String> bins = extractBinLockList(paymentInput.opt("mp_bin_lock"));
        for (int i = 0; i < bins.size(); i++) {
            builder.appendQueryParameter("mp_bin_lock[" + i + "]", bins.get(i));
        }
        String errMsg = paymentInput.optString("mp_bin_lock_err_msg", "").trim();
        if (!errMsg.isEmpty()) {
            builder.appendQueryParameter("mp_bin_lock_err_msg", errMsg);
        }
    }

    static List<String> extractBinLockList(Object raw) {
        List<String> bins = new ArrayList<>();
        if (raw == null) {
            return bins;
        }
        boolean sawCandidate = false;
        if (raw instanceof String[]) {
            for (String value : (String[]) raw) {
                sawCandidate |= addBinValue(bins, value);
            }
            return requireValidBins(sawCandidate, bins);
        }
        if (raw instanceof Collection) {
            for (Object value : (Collection<?>) raw) {
                sawCandidate |= addBinValue(bins, value);
            }
            return requireValidBins(sawCandidate, bins);
        }
        if (raw instanceof JSONArray) {
            JSONArray array = (JSONArray) raw;
            for (int i = 0; i < array.length(); i++) {
                sawCandidate |= addBinValue(bins, array.optString(i, null));
            }
            return requireValidBins(sawCandidate, bins);
        }
        String text = raw.toString().trim();
        if (text.isEmpty()) {
            return bins;
        }
        if (text.startsWith("[")) {
            try {
                JSONArray array = new JSONArray(text.replace('\'', '"'));
                for (int i = 0; i < array.length(); i++) {
                    sawCandidate |= addBinValue(bins, array.optString(i, null));
                }
                return requireValidBins(sawCandidate, bins);
            } catch (JSONException ignored) {
                throw binLockError();
            }
        }
        if (text.contains(",")) {
            for (String part : text.split(",")) {
                sawCandidate |= addBinValue(bins, part);
            }
            return requireValidBins(sawCandidate, bins);
        }
        sawCandidate = addBinValue(bins, text);
        return requireValidBins(sawCandidate, bins);
    }

    private static List<String> requireValidBins(boolean sawCandidate, List<String> bins) {
        if (sawCandidate && bins.isEmpty()) {
            throw binLockError();
        }
        return bins;
    }

    private static IllegalArgumentException binLockError() {
        return new IllegalArgumentException("mp_bin_lock contains no valid 6-8 digit BIN");
    }

    private static boolean addBinValue(List<String> bins, Object value) {
        if (value == null) {
            return false;
        }
        String bin = value.toString().trim();
        if (bin.isEmpty()) {
            return false;
        }
        if (bin.matches("\\d{6,8}")) {
            bins.add(bin);
        }
        return true;
    }

    public Object GetPaymentResult(JSONObject transaction ) {
        try {
            String endPoint = GatewayEndpoints.getQueryByTidUrl(getGPayEnvironment());

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