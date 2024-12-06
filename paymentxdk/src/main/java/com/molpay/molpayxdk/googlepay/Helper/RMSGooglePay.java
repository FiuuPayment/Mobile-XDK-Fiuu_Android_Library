/*
 * Copyright 2023 Razer Merchant Services.
 */

package com.molpay.molpayxdk.googlepay.Helper;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.molpay.molpayxdk.googlepay.ApiRequestService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Pattern;

public class RMSGooglePay {

    final Pattern ORDERID = Pattern.compile("^[a-zA-Z0-9]*$");
    final Pattern AMOUNT = Pattern.compile("^[0-9.]*$");
    final Pattern CURRENCY = Pattern.compile("^[a-zA-Z]*$");
    final Pattern BILLNAME = Pattern.compile("^[\\S\\s]+[\\S]*$");
    final Pattern BILLEMAIL = Pattern.compile("^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$");
    final Pattern BILLPHONE = Pattern.compile("^[0-9]*$");
    final Pattern BILLDESC = Pattern.compile("^[\\S\\s]+[\\S]*$");
    final Pattern MERCHANTID = Pattern.compile("^[\\S\\s]+[\\S]*$");
    final Pattern VERFICATIONKEY = Pattern.compile("^[A-Za-z0-9]+$");
    final Pattern ENV = Pattern.compile("^(?i)(true|false)$");

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Object requestPayment(String paymentInput, String paymentInfo) {

        try {

            //INPUT VALIDATION
            JSONObject paymentInputObj = new JSONObject(paymentInput);
            String orderId = paymentInputObj.getString("orderId");
            String amount = paymentInputObj.getString("amount");
            String currency = paymentInputObj.getString("currency");
            String billName = paymentInputObj.getString("billName");
            String billEmail = paymentInputObj.getString("billEmail");
            String billPhone = paymentInputObj.getString("billPhone");
            String billDesc = paymentInputObj.getString("billDesc");
            String merchantId = paymentInputObj.getString("merchantId");
            String verificationKey = paymentInputObj.getString("verificationKey");
            String isSandbox = paymentInputObj.getString("isSandbox");

            JSONObject error = new JSONObject();

            if (!ORDERID.matcher(orderId).matches()) {
                //throw new IllegalArgumentException("Invalid String");
                error.put("error", "400");
                error.put("message", "Invalid Order Id");
                return error;
            }
            else if (!AMOUNT.matcher(amount).matches()) {
                //throw new IllegalArgumentException("Invalid String");
                error.put("error", "400");
                error.put("message", "Invalid Amount");
                return error;
            }
            else if (!CURRENCY.matcher(currency).matches()) {
                //throw new IllegalArgumentException("Invalid String");
                error.put("error", "400");
                error.put("message", "Invalid Currency");
                return error;
            }
            else if (!BILLNAME.matcher(billName).matches()) {
                //throw new IllegalArgumentException("Invalid String");
                error.put("error", "400");
                error.put("message", "Invalid Billing Name");
                return error;
            }
            else if (!BILLEMAIL.matcher(billEmail).matches()) {
                //throw new IllegalArgumentException("Invalid String");
                error.put("error", "400");
                error.put("message", "Invalid Billing Email");
                return error;
            }
            else if (!BILLPHONE.matcher(billPhone).matches()) {
                //throw new IllegalArgumentException("Invalid String");
                error.put("error", "400");
                error.put("message", "Invalid Billing Phone");
                return error;
            }
            else if (!BILLDESC.matcher(billDesc).matches()) {
                //throw new IllegalArgumentException("Invalid String");
                error.put("error", "400");
                error.put("message", "Invalid Billing Description");
                return error;
            }
            else if (!MERCHANTID.matcher(merchantId).matches()) {
                //throw new IllegalArgumentException("Invalid String");
                error.put("error", "400");
                error.put("message", "Invalid Merchant Id");
                return error;
            }
            else if (!VERFICATIONKEY.matcher(verificationKey).matches()) {
                //throw new IllegalArgumentException("Invalid String");
                error.put("error", "400");
                error.put("message", "Invalid Verification Key");
                return error;
            }
            else if (!ENV.matcher(isSandbox).matches()) {
                //throw new IllegalArgumentException("Invalid String");
                error.put("error", "400");
                error.put("message", "Invalid Input Sandbox");
                return error;
            } else {
                ApiRequestService pay = new ApiRequestService();
                return pay.GetPaymentRequest(paymentInputObj, paymentInfo);
            }

        } catch (JSONException e) {
            throw new RuntimeException("The selected garment cannot be parsed from the list of elements");
        }
    }

    public Object queryPaymentResult(String transactionInput) {

        JSONObject transactionInputObj = null;

        try {
            transactionInputObj = new JSONObject(transactionInput);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ApiRequestService pay = new ApiRequestService();

        return pay.GetPaymentResult(transactionInputObj);
    }
}
