/*
 * Copyright 2023 Razer Merchant Services.
 */

package com.molpay.molpayxdk.googlepay;

import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.molpay.molpayxdk.googlepay.Helper.ApplicationHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ApiRequestService {

    static class Production {
        static final String BASE_PAYMENT = "https://pay.merchant.razer.com/";
        static final String API_PAYMENT = "https://api.merchant.razer.com/";
    }

    static class Development {
        static final String BASE_PAYMENT = "https://sandbox.merchant.razer.com/";
        static final String API_PAYMENT = "https://sandbox.merchant.razer.com/";
    }

    public ApiRequestService() {
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
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

            if (WebActivity.isSandbox.equals("false")) {
                endPoint = Production.BASE_PAYMENT + "RMS/API/Direct/1.4.0/index.php";
            } else if (WebActivity.isSandbox.equals("true")) {
                endPoint = Development.BASE_PAYMENT + "RMS/API/Direct/1.4.0/index.php";
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
                    .appendQueryParameter("GooglePay", GooglePayBase64);

                return postRequest(uri, builder);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Object GetPaymentResult(JSONObject transaction ) {
        try {
            String endPoint = "";

            if (WebActivity.isSandbox.equals("false")) {
                endPoint = Production.API_PAYMENT + "RMS/q_by_tid.php";
            } else if (WebActivity.isSandbox.equals("true")) {
                endPoint = Development.API_PAYMENT + "RMS/q_by_tid.php";
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