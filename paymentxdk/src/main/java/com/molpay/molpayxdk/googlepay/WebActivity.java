/*
 * Copyright 2023 Razer Merchant Services.
 */

package com.molpay.molpayxdk.googlepay;

import android.content.Intent;
import android.graphics.Color;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import com.molpay.molpayxdk.R;
import com.molpay.molpayxdk.googlepay.Helper.RMSGooglePay;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Objects;

public class WebActivity extends AppCompatActivity {

    private WebView wvGateway;
    private ProgressBar pbLoading;
    private AppCompatTextView tvLoading;
    public Transaction transaction = new Transaction();

    public static boolean statCodeValueSuccess = false;

    public static String isSandbox = "";

    private CountDownTimer countDownTimer;
    private String requestType = "";
    public static String paymentV2Requery = "0";
    private String paymentInput;
    private String paymentInfo;
    private boolean requeryPaymentV2 = false;
    private Boolean isClosebuttonDisplay = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e("logGooglePay" , "WebActivity");

        setContentView(R.layout.activity_web);

        Intent intent = getIntent();
        paymentInput = intent.getStringExtra("paymentInput");
        paymentInfo = intent.getStringExtra("paymentInfo");

        Log.e("logGooglePay" , "after getStringExtra 1");

        if (paymentInput != null) {
            // Transcation model from paymentInput
            JSONObject paymentInputObj = null;
            try {
                paymentInputObj = new JSONObject(paymentInput);
                transaction.setVkey(paymentInputObj.getString("verificationKey"));
                isSandbox = paymentInputObj.getString("isSandbox");
                Log.e("logGooglePay" , "WebActivity isSandbox = " + isSandbox);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        tvLoading = findViewById(R.id.tvLoading);
        pbLoading = findViewById(R.id.pbLoading);
        wvGateway = findViewById(R.id.webView);
        wvGateway.setBackgroundColor(Color.WHITE);
        wvGateway.getSettings().setDomStorageEnabled(true);
        wvGateway.getSettings().setJavaScriptEnabled(true);
        wvGateway.getSettings().setAllowUniversalAccessFromFileURLs(true);
        wvGateway.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        wvGateway.getSettings().setSupportMultipleWindows(true);

        // Mobile web view settings
        wvGateway.getSettings().setLoadWithOverviewMode(true);
        wvGateway.getSettings().setUseWideViewPort(true);
        wvGateway.getSettings().setSupportZoom(true);
        wvGateway.getSettings().setBuiltInZoomControls(true);
        wvGateway.getSettings().setDisplayZoomControls(false);

        Log.e("logGooglePay" , "before get cancelResponse");

        String cancelResponse = intent.getStringExtra("cancelResponse");

        if (cancelResponse != null) {
            Log.e("logGooglePay" , "cancelResponse != null");

            try {
                // Convert the JSON string into a JSONObject
                JSONObject responseBody = new JSONObject(cancelResponse);
                Log.e("logGooglePay", "-1 set minTimeOut 60000");
                ActivityGP.minTimeOut = 60000;
                onRequestData(responseBody);
                Log.e("logGooglePay" , "cancelResponse = " + cancelResponse);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Log.e("logGooglePay" , "bypass return cancelResponse");

        runPaymentThread ();

        // Register a callback for handling the back press
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Do nothing - prevent user from performing backpress
                Log.e("logGooglePay" , "WebActivity GP backpressed");
            }
        };

        // Add the callback to the OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void runPaymentThread () {
        new Thread(() -> {
            PaymentThread paymentThread = new PaymentThread();
            paymentThread.setValue(paymentInput, paymentInfo);
            paymentThread.run(); // Run thread work

            try {
                JSONObject paymentResult = new JSONObject(new JSONObject(paymentThread.getValue()).getString("responseBody"));

                runOnUiThread(() -> {
                    Log.e("logGooglePay", "thread paymentResult = " + paymentResult);
                    onRequestData(paymentResult); // Restart polling logic
                });

            } catch (JSONException e) {
                runOnUiThread(() -> {
                    Log.e("logGooglePay", "JSONException = " + e);
                    Intent resultCancel = new Intent();
                    resultCancel.putExtra("response", String.valueOf(e));
                    setResult(RESULT_CANCELED, resultCancel);
                    finish();
                });
            }
        }).start();
    }

    private void onStartTimOut() {

        long interval = 3000;
        final String[] queryResultStr = {null};
        final String[] trasactionJsonStr = {null};

        Log.e("logGooglePay" , "onStartTimOut ActivityGP.minTimeOut = " + ActivityGP.minTimeOut);

        // Query Transaction ID for every 3 second in 1 minute
        countDownTimer = new CountDownTimer(ActivityGP.minTimeOut, interval) {

            @Override
            public void onTick(long millisUntilFinished) {

                JSONObject transactionObject = new JSONObject();
                try {
                    transactionObject.put("txID", transaction.getTxID());
                    transactionObject.put("amount", transaction.getAmount());
                    transactionObject.put("merchantId", transaction.getDomain());
                    transactionObject.put("verificationKey", transaction.getVkey());
                    trasactionJsonStr[0] = transactionObject.toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                QueryResultThread queryResultThread = new QueryResultThread();
                queryResultThread.setValue(trasactionJsonStr[0]); // set value

                Thread thread = new Thread(queryResultThread);
                thread.start();

                try {
                    thread.join();
                    queryResultStr[0] = queryResultThread.getValue();

                    if (queryResultStr[0] != null) {
                        try {
                            JSONObject queryResultObj = new JSONObject(queryResultStr[0]);
                            String responseBody = queryResultObj.getString("responseBody");
                            JSONObject responseBodyObj = new JSONObject(responseBody);

                            // If StatCode
                            if (responseBodyObj.has("StatCode")){
                                String statCodeValue = responseBodyObj.getString("StatCode");
                                String channelValue = responseBodyObj.getString("Channel");

////                                TODO 1: For Testing User Case Only
//                                if (millisUntilFinished < 50000) {
//                                    statCodeValue = "00";
//                                }

                                Log.e("logGooglePay" , "statCodeValue " + statCodeValue);

                                if (statCodeValue.equals("00")) {
                                    if (statCodeValueSuccess) {
                                        Log.e("logGooglePay" , "statCodeValueSuccess finish");
                                        onFinish();
                                    }
                                } else if (statCodeValue.equals("11")) {
                                    cancel();
                                    pbLoading.setVisibility(View.GONE);
                                    tvLoading.setVisibility(View.GONE);

                                    String errorCode;
                                    try {
                                        errorCode = responseBodyObj.getString("ErrorCode");
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }
                                    String errorDesc = null;
                                    try {
                                        errorDesc = responseBodyObj.getString("ErrorDesc");
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }

                                    if (errorCode.equalsIgnoreCase("GOOGLEPAY_C1")) {
                                        Log.e("logGooglePay", "Send cancel response = " + responseBodyObj);
                                        Intent resultCancel = new Intent();
                                        resultCancel.putExtra("response", String.valueOf(responseBodyObj));
                                        setResult(RESULT_CANCELED, resultCancel);
                                        finish();
                                    } else {
                                        Log.e("logGooglePay" , "Proceed show error text");
                                        new AlertDialog.Builder(WebActivity.this)
                                                .setTitle("Payment Failed")
                                                .setMessage(errorCode + " : " + errorDesc)
                                                .setCancelable(false)
                                                .setPositiveButton("CLOSE", (dialog, which) -> {
                                                    Log.e("logGooglePay" , "RESULT_CANCELED WebActivity 1 responseBodyObj = " + responseBodyObj);
                                                    Intent resultCancel = new Intent();
                                                    resultCancel.putExtra("response", String.valueOf(responseBodyObj));
                                                    setResult(RESULT_CANCELED, resultCancel);
                                                    finish();
                                                }).show();
                                    }
                                }  else if (statCodeValue.equals("22")) {
                                    if (channelValue.contains("ShopeePay") || channelValue.contains("TNG-EWALLET")) {
                                        Log.e("logGooglePay", "E-Wallet - need requery payment_v2");
                                        countDownTimer.cancel(); // Stop current countdown

                                        if (millisUntilFinished > 3000) {
                                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                                pbLoading.setVisibility(View.VISIBLE);
                                                tvLoading.setVisibility(View.VISIBLE);
                                                // Reduce minTimeOut by 3 seconds
                                                ActivityGP.minTimeOut -= 3000;
                                                paymentV2Requery = "1";
                                                requeryPaymentV2 = true;
                                                runPaymentThread ();
                                            }, 3000); // 3-second delay

                                        } else {
                                            // Timeout too short, cancel payment
                                            Log.e("logGooglePay", "Timeout too short, canceling payment");
                                            Log.e("logGooglePay", "responseBodyObj = " + responseBodyObj);
                                            Intent resultCancel = new Intent();
                                            resultCancel.putExtra("response", String.valueOf(responseBodyObj));
                                            setResult(RESULT_CANCELED, resultCancel);
                                            finish();
                                        }
                                    }
                                    else {
                                        // Do Nothing - It will auto handle q_by_tid.php
                                        Log.e("logGooglePay" , "CARD - Do Nothing it will auto handle by q_by_tid.php");
                                    }
                                }
                            } else {

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFinish() {
                try {
                    JSONObject queryResultObj = new JSONObject(queryResultStr[0]);
                    String responseBody = queryResultObj.getString("responseBody");

                    JSONObject responseBodyObj = new JSONObject(responseBody);

                    Intent intent = new Intent();
                    intent.putExtra("response", String.valueOf(responseBodyObj));

                    Log.e("logGooglePay" , "onFinish response = " + String.valueOf(responseBodyObj));

                    // If timeout / cancel
                    if (!responseBodyObj.has("StatCode")){
                        setResult(RESULT_CANCELED, intent);
                    } else {
                        if (responseBodyObj.getString("StatCode").equalsIgnoreCase("22")) {
                            setResult(RESULT_CANCELED, intent);
                        } else {
                            setResult(RESULT_OK, intent);
                        }
                    }

                    countDownTimer.cancel();
                    finish();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        countDownTimer.start();
    }

    private String xdkHTMLRedirection = "";
    private void onLoadHtmlWebView(String plainHtml) {

//        wvGateway.setVisibility(View.VISIBLE);
        pbLoading.setVisibility(View.VISIBLE);
        tvLoading.setVisibility(View.VISIBLE);
        statCodeValueSuccess = false;

        String encodedHtml = Base64.encodeToString(plainHtml.getBytes(), Base64.NO_PADDING);

        Log.e("logGooglePay" , "plainHtml = " + plainHtml);

        if (plainHtml.contains("xdkHTMLRedirection")) {
            xdkHTMLRedirection = StringUtils.substringBetween(plainHtml, "xdkHTMLRedirection' value='", "'");
            wvGateway.loadData(xdkHTMLRedirection, "text/html", "base64");
        } else if (requestType.equalsIgnoreCase("REDIRECT")) {
            wvGateway.loadData(encodedHtml, "text/html", "base64");
            Log.e("logGooglePay" , "requeryPaymentV2 = " + requeryPaymentV2);
            if ( ! requeryPaymentV2 ) {
                pbLoading.setVisibility(View.GONE);
                tvLoading.setVisibility(View.GONE);
                wvGateway.setVisibility(View.VISIBLE);
            }
        } else {
            wvGateway.loadData(encodedHtml, "text/html", "base64");
        }

        wvGateway.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {

                if (request.getUrl().toString().contains("result.php")) {
                    statCodeValueSuccess = true;
                    pbLoading.setVisibility(View.VISIBLE);
                    tvLoading.setVisibility(View.VISIBLE);
                    wvGateway.setVisibility(View.GONE);
                }

                return super.shouldOverrideUrlLoading(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                super.onReceivedSslError(view, handler, error);
            }
        });
    }

    public void onRequestData(JSONObject response) {

        try {
            if (response.has("error_code") && response.has("error_desc")) {
                Intent intent = new Intent();
                String strResponse = response.toString();
                intent.putExtra("response", strResponse);
                setResult(RESULT_CANCELED, intent);
                finish();
            }
            if (response.has("TxnID")) {
                try {
                    transaction.setTxID(response.getString("TxnID"));
                    transaction.setDomain(response.getString("MerchantID"));
                    transaction.setAmount(response.getString("TxnAmount"));
                    transaction.setVkey(ActivityGP.verificationKey);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (response.has("TxnData") && !response.has("pInstruction")) {

                    onStartTimOut();

                    JSONObject txnData = response.getJSONObject("TxnData");

                    StringBuilder html = new StringBuilder();
                    html.append(String.format("<form id='prForm' action='%s' method='%s'>\n",
                            txnData.getString("RequestURL"),
                            txnData.getString("RequestMethod"))
                    );
                    if (txnData.has("AppDeepLinkURL")) {
//                        AppData.getInstance().setRedirectAppUrl(txnData.getString("AppDeepLinkURL"));
                    }
                    if (txnData.has("RequestType")) {
                        requestType = txnData.getString("RequestType");
                    }
                    if (txnData.has("RequestData")) {

                        if (txnData.get("RequestData") instanceof JSONObject) {
                            JSONObject requestData = txnData.getJSONObject("RequestData");

                            Iterator<String> keys = requestData.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();

                                if (requestData.get(key) instanceof JSONObject) {
                                    // Do nothing
                                } else {
                                    if (requestData.has("checkoutUrl")) {
//                                        AppData.getInstance().setRedirectAppUrl(requestData.getString("checkoutUrl"));
                                    }
                                    html.append(String.format("<input type='hidden' name='%s' value='%s'>\n", key, requestData.getString(key)));
                                }
                            }
                        }
                    }

                    html.append("</form>");
                    html.append("<script> document.getElementById('prForm').submit();</script>");

                    onLoadHtmlWebView(html.toString());
                } else {
                    Intent intent = new Intent();
                    String strResponse = response.toString();
                    intent.putExtra("response", strResponse);
                    setResult(RESULT_CANCELED, intent);
                    finish();
                }
            } else {
                Intent intent = new Intent();
                String strResponse = response.toString();
                intent.putExtra("response", strResponse);
                setResult(RESULT_CANCELED, intent);
                finish();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public class PaymentThread implements Runnable {
        private volatile String resp;
        private String paymentInput;
        private String  paymentInfo;

        public String getValue() {
            return resp;
        }

        public void setValue(String paymentInput, String  paymentInfo) {
            this.paymentInput = paymentInput;
            this.paymentInfo = paymentInfo;
        }

        @Override
        public void run() {
            RMSGooglePay pay = new RMSGooglePay();
            JSONObject result;
            result = (JSONObject) pay.requestPayment(paymentInput, paymentInfo);
            resp = result.toString();
        }
    }

    public class QueryResultThread implements Runnable {
        private volatile String resp;
        private String transaction;

        public String getValue() {
            return resp;
        }

        public void setValue(String transaction) {
            this.transaction = transaction;
        }

        @Override
        public void run() {

                RMSGooglePay pay = new RMSGooglePay();
                JSONObject result = (JSONObject) pay.queryPaymentResult(transaction);
                if (result != null) {
                    resp = result.toString();
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        Log.e("logGooglePay" , "onCreateOptionsMenu paymentInput = " + paymentInput);

        if (paymentInput != null) {
            JSONObject json = null;
            try {
                json = new JSONObject(paymentInput);

                Log.e("logGooglePay" , "onCreateOptionsMenu");

                if (json.has("closeButton")) {
                    isClosebuttonDisplay = json.getBoolean("closeButton");
                }
            } catch (JSONException e) {
                return false;
            }

            if (isClosebuttonDisplay) {
                getMenuInflater().inflate(R.menu.menu_molpay, menu);
                return super.onCreateOptionsMenu(menu);
            }
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Log.e("logGooglePay", "Get Menu: " + item.getTitle());
        if (Objects.equals(item.getTitle(), "Close")) {
            setResult(RESULT_CANCELED, null);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) {
            Log.e("logGooglePay", "onDestroy countDownTimer NOT NULL");
            countDownTimer.cancel();
        }
        super.onDestroy();
    }
}