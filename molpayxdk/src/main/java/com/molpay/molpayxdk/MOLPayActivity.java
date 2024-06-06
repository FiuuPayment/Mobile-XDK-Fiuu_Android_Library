package com.molpay.molpayxdk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.molpay.molpayxdk.googlepay.ActivityGP;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Objects;

public class MOLPayActivity extends AppCompatActivity {

    public final static int MOLPayXDK = 9999;
    public final static String MOLPayPaymentDetails = "paymentDetails";
    public final static String MOLPayTransactionResult = "transactionResult";
    public final static String mp_amount = "mp_amount";
    public final static String mp_username = "mp_username";
    public final static String mp_password = "mp_password";
    public final static String mp_merchant_ID = "mp_merchant_ID";
    public final static String mp_app_name = "mp_app_name";
    public final static String mp_order_ID = "mp_order_ID";
    public final static String mp_extended_vcode = "mp_extended_vcode";
    public final static String mp_currency = "mp_currency";
    public final static String mp_country = "mp_country";
    public final static String mp_verification_key = "mp_verification_key";
    public final static String mp_channel = "mp_channel";
    public final static String mp_bill_description = "mp_bill_description";
    public final static String mp_bill_name = "mp_bill_name";
    public final static String mp_bill_email = "mp_bill_email";
    public final static String mp_bill_mobile = "mp_bill_mobile";
    public final static String mp_channel_editing = "mp_channel_editing";
    public final static String mp_editing_enabled = "mp_editing_enabled";
    public final static String mp_transaction_id = "mp_transaction_id";
    public final static String mp_request_type = "mp_request_type";
    public final static String mp_ga_enabled = "mp_ga_enabled";
    public final static String mp_filter = "mp_filter";
    public final static String mp_custom_css_url = "mp_custom_css_url";
    public final static String mp_is_escrow = "mp_is_escrow";
    public final static String mp_bin_lock = "mp_bin_lock";
    public final static String mp_bin_lock_err_msg = "mp_bin_lock_err_msg";
    public final static String mp_preferred_token = "mp_preferred_token";
    public final static String mp_tcctype = "mp_tcctype";
    public final static String mp_is_recurring = "mp_is_recurring";
    public final static String mp_allowed_channels = "mp_allowed_channels";
    public final static String mp_sandbox_mode = "mp_sandbox_mode";
    public final static String mp_secured_verified = "mp_secured_verified";
    public final static String mp_express_mode = "mp_express_mode";
    public final static String mp_advanced_email_validation_enabled = "mp_advanced_email_validation_enabled";
    public final static String mp_advanced_phone_validation_enabled = "mp_advanced_phone_validation_enabled";
    public final static String mp_bill_name_edit_disabled = "mp_bill_name_edit_disabled";
    public final static String mp_bill_email_edit_disabled = "mp_bill_email_edit_disabled";
    public final static String mp_bill_mobile_edit_disabled = "mp_bill_mobile_edit_disabled";
    public final static String mp_bill_description_edit_disabled = "mp_bill_description_edit_disabled";
    public final static String mp_dev_mode = "mp_dev_mode";
    public final static String mp_language = "mp_language";
    public final static String mp_cash_waittime = "mp_cash_waittime";
    public final static String mp_non_3DS = "mp_non_3DS";
    public final static String mp_card_list_disabled = "mp_card_list_disabled";
    public final static String mp_disabled_channels = "mp_disabled_channels";
    public final static String mp_dpa_id = "mp_dpa_id";
    public final static String mp_company = "mp_company";

    public final static String MOLPAY = "logMOLPAY";
    private final static String mpopenmolpaywindow = "mpopenmolpaywindow://";
    private final static String mpcloseallwindows = "mpcloseallwindows://";
    private final static String mptransactionresults = "mptransactionresults://";
    private final static String mprunscriptonpopup = "mprunscriptonpopup://";
    private final static String mppinstructioncapture = "mppinstructioncapture://";
    private final static String mpclickgpbutton = "mpclickgpbutton://";
    private final static String module_id = "module_id";
    private final static String wrapper_version = "wrapper_version";
    private final static String wrapperVersion = "1";

    private String base64Img;
    private String filename;
    private Bitmap imgBitmap;

    private WebView mpMainUI, mpMOLPayUI, mpBankUI;
    private HashMap<String, Object> paymentDetails;
    private Boolean isMainUILoaded = false;
    private Boolean isClosingReceipt = false;

    // Private API
    private void closemolpay() {
        mpMainUI.loadUrl("javascript:closemolpay()");
        if (isClosingReceipt) {
            isClosingReceipt = false;
            finish();
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        closemolpay();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_molpay, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // closebtn clicked
        if (id == R.id.closeBtn) {
            closemolpay();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_molpay);

        paymentDetails = (HashMap<String, Object>) getIntent().getSerializableExtra(MOLPayPaymentDetails);

        // For submodule wrappers
        boolean is_submodule = false;
        assert paymentDetails != null;
        if (paymentDetails.containsKey("is_submodule")) {
            is_submodule = Boolean.parseBoolean(Objects.requireNonNull(paymentDetails.get("is_submodule")).toString());
        }
        String submodule_module_id = null;
        if (paymentDetails.containsKey("module_id")) {
            submodule_module_id = Objects.requireNonNull(paymentDetails.get("module_id")).toString();
        }
        String submodule_wrapper_version = null;
        if (paymentDetails.containsKey("wrapper_version")) {
            submodule_wrapper_version = Objects.requireNonNull(paymentDetails.get("wrapper_version")).toString();
        }
        if (is_submodule && !Objects.equals(submodule_module_id, "") && !Objects.equals(submodule_wrapper_version, "")) {
            paymentDetails.put(module_id, submodule_module_id);
            paymentDetails.put(wrapper_version, wrapperVersion+"."+submodule_wrapper_version);
        } else {
            paymentDetails.put(module_id, "molpay-mobile-xdk-android");
            paymentDetails.put(wrapper_version, wrapperVersion);
        }

        // Bind resources
        mpMainUI = (WebView) findViewById(R.id.MPMainUI);
        mpMOLPayUI = (WebView) findViewById(R.id.MPMOLPayUI);

        // Enable js
        mpMainUI.getSettings().setJavaScriptEnabled(true);
        mpMOLPayUI.getSettings().setJavaScriptEnabled(true);

        // Hide UI by default
        mpMOLPayUI.setVisibility(View.GONE);

        // Load the main ui
        mpMainUI.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        mpMainUI.getSettings().setAllowUniversalAccessFromFileURLs(true);
        mpMainUI.setWebViewClient(new MPMainUIWebClient());
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            cookieManager.setAcceptThirdPartyCookies(mpMainUI, true);
            cookieManager.setAcceptThirdPartyCookies(mpMOLPayUI, true);
        }
        mpMainUI.loadUrl("https://pay.merchant.razer.com/RMS/API/xdk/");

        // Configure MOLPay ui
        mpMOLPayUI.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        mpMOLPayUI.getSettings().setAllowUniversalAccessFromFileURLs(true);
        mpMOLPayUI.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mpMOLPayUI.getSettings().setSupportMultipleWindows(true);
        mpMOLPayUI.getSettings().setDomStorageEnabled(true);
        mpMOLPayUI.setWebViewClient(new MPMOLPayUIWebClient());
        mpMOLPayUI.setWebChromeClient(new MPMOLPayUIWebChromeClient());
        mpMOLPayUI.getSettings().setLoadWithOverviewMode(true);
        mpMOLPayUI.getSettings().setUseWideViewPort(true);

        CookieManager.getInstance().setAcceptCookie(true);

        mpMOLPayUI.setLongClickable(true);
        mpMOLPayUI.setOnLongClickListener(view -> {
            Log.d(MOLPAY, "Long press fired!");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mpMOLPayUI.evaluateJavascript("document.getElementById(\"qrcode_img\").src", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String qrdata) {
                        Log.d(MOLPAY, "QR data = " + qrdata);
                        if(qrdata != null && !qrdata.equals("null")) {
                            String imageQrCode = qrdata.replaceAll("data:image/png;base64,", "");
                            Log.d(MOLPAY, "imageQrCode = " + imageQrCode);
                            byte[] decodedBytes = Base64.decode(imageQrCode, 0);
                            imgBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            filename = Objects.requireNonNull(paymentDetails.get("mp_order_ID")) + ".png";

                            isStoragePermissionGranted();
                        }
                    }
                });
            }
            return false;
        });

    }

    private void nativeWebRequestUrlUpdates(String url) {
        Log.d(MOLPAY, "nativeWebRequestUrlUpdates url = " + url);

        HashMap<String, String> data = new HashMap<>();
        data.put("requestPath", url);

        // Create JSON object for Payment details
        JSONObject json = new JSONObject(data);

        // Init javascript
        mpMainUI.loadUrl("javascript:nativeWebRequestUrlUpdates(" + json.toString() + ")");
    }

    private void nativeWebRequestUrlUpdatesOnFinishLoad(String url) {
        Log.d(MOLPAY, "nativeWebRequestUrlUpdatesOnFinishLoad url = " + url);

        HashMap<String, String> data = new HashMap<>();
        data.put("requestPath", url);

        // Create JSON object for Payment details
        JSONObject json = new JSONObject(data);

        // Init javascript
        mpMainUI.loadUrl("javascript:nativeWebRequestUrlUpdatesOnFinishLoad(" + json.toString() + ")");
    }

    private class MPBankUIWebClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.d(MOLPAY, "MPBankUIWebClient onPageStarted url = " + url);

            if (url != null) {
                nativeWebRequestUrlUpdates(url);
            }
        }
        @Override
        public void onPageFinished (WebView view, String url) {
            Log.d(MOLPAY, "MPBankUIWebClient onPageFinished url = " + url);
            nativeWebRequestUrlUpdates(url);
        }
    }

    private class MPMOLPayUIWebClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.d(MOLPAY, "MPMOLPayUIWebClient onPageStarted url = " + url);

            if (url != null) {
                nativeWebRequestUrlUpdates(url);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public boolean shouldOverrideUrlLoading(final WebView view, String url) {
            Log.d(MOLPAY, "MPMOLPayUIWebClient shouldOverrideUrlLoading url = " + url);
            if (url != null) {
                if (url.contains("scbeasy/easy_app_link.html")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        // Define what your app should do if no activity can handle the intent.
                        e.printStackTrace();
                    }
                    view.evaluateJavascript("document.getElementById(\"ref_no\").value", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String ref_no) {
                            Log.d(MOLPAY, "MPMOLPayUIWebClient trans_id = " + ref_no.replaceAll("\"", ""));
                            view.loadUrl("https://pay.merchant.razer.com/RMS/intermediate_app/loading.php?tranID=" + ref_no.replaceAll("\"", ""));
                        }
                    });
                    return true;
                } else if (url.contains("atome-my.onelink.me") ||
                        url.contains("myboost.app") ||
                        url.contains("market://") ||
                        url.contains("intent://")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        // Define what your app should do if no activity can handle the intent.
                        e.printStackTrace();
                    }
                    return true;
                }else if (url.contains("alipays://")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    // Define what your app should do if no activity can handle the intent.
                    e.printStackTrace();
                }
                return true;
            }
            }
            return false;
        }

	    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
		@Override
		public void onPageFinished (final WebView view, String url) {
		    Log.d(MOLPAY, "MPMOLPayUIWebClient onPageFinished url = " + url);
	//            nativeWebRequestUrlUpdates(url);

		    if (url.contains("intermediate_appTNG-EWALLET.php") || url.contains("intermediate_app/processing.php")) {

			Log.d(MOLPAY, "contains url");

			view.evaluateJavascript("document.getElementById(\"systembrowserurl\").innerHTML", s -> {
                Log.d(MOLPAY, "MPMOLPayUIWebClient base64String = " + s);

    //                // Decode base64
                byte[] data = Base64.decode(s, Base64.DEFAULT);
                String dataString = new String(data);
                Log.d(MOLPAY, "MPBankUIWebClient dataString = " + dataString);

                if (s.length() > 0) {
                    Log.d(MOLPAY, "MPMOLPayUIWebClient success");
                    Intent intent= new Intent(Intent.ACTION_VIEW, Uri.parse(dataString));
                    startActivity(intent);
                } else {
                    Log.d(MOLPAY, "MPMOLPayUIWebClient empty dataString");
                }
                    });

		    }
		}
    }

    private class MPMOLPayUIWebChromeClient extends WebChromeClient {
        @SuppressLint("SetJavaScriptEnabled")
        @Override
        public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message resultMsg) {

            Log.d(MOLPAY, "MPMOLPayUIWebChromeClient onCreateWindow resultMsg = " + resultMsg);

            RelativeLayout container = (RelativeLayout) findViewById(R.id.MPContainer);

            mpBankUI = new WebView(MOLPayActivity.this);

            mpBankUI.getSettings().setJavaScriptEnabled(true);
            mpBankUI.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
            mpBankUI.getSettings().setAllowUniversalAccessFromFileURLs(true);
            mpBankUI.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            mpBankUI.getSettings().setSupportMultipleWindows(true);

            mpBankUI.setWebViewClient(new MPBankUIWebClient());
            mpBankUI.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onCloseWindow(WebView window) {
                    closemolpay();
                }
            });

            mpBankUI.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            container.addView(mpBankUI);
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(mpBankUI);
            resultMsg.sendToTarget();
            return true;

        }
    }

    private class MPMainUIWebClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(MOLPAY, "MPMainUIWebClient shouldOverrideUrlLoading url = " + url);

            if (url != null) {
                if (url.startsWith(mpopenmolpaywindow)) {
                    String base64String = url.replace(mpopenmolpaywindow, "");
                    Log.d(MOLPAY, "MPMainUIWebClient mpopenmolpaywindow base64String = " + base64String);

                    // Decode base64
                    byte[] data = Base64.decode(base64String, Base64.DEFAULT);
                    String dataString = new String(data);
                    Log.d(MOLPAY, "MPMainUIWebClient mpopenmolpaywindow dataString = " + dataString);

                    if (dataString.length() > 0) {
                        Log.d(MOLPAY, "MPMainUIWebClient mpopenmolpaywindow success");
                        if (mpMOLPayUI != null) {
                            Log.d(MOLPAY, "mpMOLPayUI not NULL update UI");
                            mpMOLPayUI.loadDataWithBaseURL("", dataString, "text/html", "UTF-8", "");
                            mpMOLPayUI.setVisibility(View.VISIBLE);
                        } else {
                            Log.d(MOLPAY, "mpMOLPayUI NULL avoid crash");
                        }
                    } else {
                        Log.d(MOLPAY, "MPMainUIWebClient mpopenmolpaywindow empty dataString");
                    }

                }
                else if (url.startsWith(mpcloseallwindows)) {
                    if (mpBankUI != null) {
                        mpBankUI.loadUrl("about:blank");
                        mpBankUI.setVisibility(View.GONE);
                        mpBankUI.clearCache(true);
                        mpBankUI.clearHistory();
                        mpBankUI.removeAllViews();
                        mpBankUI.destroy();
                        mpBankUI = null;
                    }
                    if (mpMOLPayUI != null) {
                        mpMOLPayUI.loadUrl("about:blank");
                        mpMOLPayUI.setVisibility(View.GONE);
                        mpMOLPayUI.clearCache(true);
                        mpMOLPayUI.clearHistory();
                        mpMOLPayUI.removeAllViews();
                        mpMOLPayUI.destroy();
                        mpMOLPayUI = null;
                    }
                }
                else if (url.startsWith(mptransactionresults)) {
                    String base64String = url.replace(mptransactionresults, "");
                    Log.d(MOLPAY, "MPMainUIWebClient mptransactionresults base64String = " + base64String);

                    // Decode base64
                    byte[] data = Base64.decode(base64String, Base64.DEFAULT);
                    String dataString = new String(data);
                    Log.d(MOLPAY, "MPMainUIWebClient mptransactionresults dataString = " + dataString);

                    Intent result = new Intent();
                    result.putExtra(MOLPayTransactionResult, dataString);

                    if (isJSONValid(dataString)){
                        Log.d(MOLPAY, "isJSONValid setResult");
                        setResult(RESULT_OK, result);

                        // Check if mp_request_type is "Receipt", if it is, don't finish()
                        try {
                            JSONObject jsonResult = new JSONObject(dataString);

                            Log.d(MOLPAY, "MPMainUIWebClient jsonResult = " + jsonResult);

                            if (!jsonResult.has("mp_request_type") || !jsonResult.getString("mp_request_type").equals("Receipt") || jsonResult.has("error_code")) {
                                finish();
                            } else {
                                // Next close button click will finish() the activity
                                isClosingReceipt = true;
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
                            }
                        } catch (Throwable t) {
                            finish();
                        }
                    }
                    else {
                        Log.d(MOLPAY, "json not valid dont setResult");
//                    setResult(RESULT_CANCELED, result);
                    }

                }
                else if (url.startsWith(mprunscriptonpopup)) {
                    String base64String = url.replace(mprunscriptonpopup, "");
                    Log.d(MOLPAY, "MPMainUIWebClient mprunscriptonpopup base64String = " + base64String);

                    // Decode base64
                    byte[] data = Base64.decode(base64String, Base64.DEFAULT);
                    String jsString = new String(data);
                    Log.d(MOLPAY, "MPMainUIWebClient mprunscriptonpopup jsString = " + jsString);

                    if (mpBankUI != null) {
                        mpBankUI.loadUrl("javascript:"+jsString);
                        Log.d(MOLPAY, "mpBankUI loadUrl = " + "javascript:"+jsString);
                    }

                }
                else if (url.startsWith(mppinstructioncapture)) {
                    String base64String = url.replace(mppinstructioncapture, "");
                    Log.d(MOLPAY, "MPMainUIWebClient mppinstructioncapture base64String = " + base64String);

                    // Decode base64
                    byte[] data = Base64.decode(base64String, Base64.DEFAULT);
                    String dataString = new String(data);
                    Log.d(MOLPAY, "MPMainUIWebClient mppinstructioncapture dataString = " + dataString);

                    try {
                        JSONObject jsonResult = new JSONObject(dataString);

                        base64Img = jsonResult.getString("base64ImageUrlData");
                        filename = jsonResult.getString("filename");
                        Log.d(MOLPAY, "MPMainUIWebClient jsonResult = " + jsonResult);

                        byte[] decodedBytes = Base64.decode(base64Img, 0);
                        imgBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                        isStoragePermissionGranted();

                    } catch (Throwable t) {
                        Log.d(MOLPAY, "MPMainUIWebClient jsonResult error = " + t);
                    }

                }
                else if (url.startsWith(mpclickgpbutton)) {

                    // Extended VCode setting
                    if (paymentDetails.get("mp_extended_vcode") == null) {
                        paymentDetails.put(MOLPayActivity.mp_extended_vcode, false);
                    } else {
                        paymentDetails.put(MOLPayActivity.mp_extended_vcode, Objects.requireNonNull(paymentDetails.get("mp_extended_vcode")));
                    }

                    paymentDetails.put(MOLPayActivity.mp_sandbox_mode, Objects.requireNonNull(paymentDetails.get("mp_sandbox_mode"))); // Only set to false once you have request production access for your app
                    paymentDetails.put(MOLPayActivity.mp_merchant_ID, Objects.requireNonNull(paymentDetails.get("mp_merchant_ID"))); // Your sandbox / production merchant ID
                    paymentDetails.put(MOLPayActivity.mp_verification_key, Objects.requireNonNull(paymentDetails.get("mp_verification_key"))); // Your sandbox / production verification key
                    paymentDetails.put(MOLPayActivity.mp_amount, Objects.requireNonNull(paymentDetails.get("mp_amount"))); // Must be in 2 decimal points format
                    paymentDetails.put(MOLPayActivity.mp_order_ID, Objects.requireNonNull(paymentDetails.get("mp_order_ID"))); // Must be unique
                    paymentDetails.put(MOLPayActivity.mp_currency, Objects.requireNonNull(paymentDetails.get("mp_currency"))); // Must matched mp_country
                    paymentDetails.put(MOLPayActivity.mp_country, Objects.requireNonNull(paymentDetails.get("mp_country"))); // Must matched mp_currency
                    paymentDetails.put(MOLPayActivity.mp_bill_description, Objects.requireNonNull(paymentDetails.get("mp_bill_description")));
                    paymentDetails.put(MOLPayActivity.mp_bill_name, Objects.requireNonNull(paymentDetails.get("mp_bill_name")));
                    paymentDetails.put(MOLPayActivity.mp_bill_email, Objects.requireNonNull(paymentDetails.get("mp_bill_email")));
                    paymentDetails.put(MOLPayActivity.mp_bill_mobile, Objects.requireNonNull(paymentDetails.get("mp_bill_mobile")));

                    Intent intent = new Intent(MOLPayActivity.this, ActivityGP.class); // Used ActivityGP for Google Pay
                    intent.putExtra(MOLPayActivity.MOLPayPaymentDetails, paymentDetails);
                    startActivityForResult(intent, MOLPayActivity.MOLPayXDK);
                }
            }

            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (!isMainUILoaded && !url.equals("about:blank")) {

                isMainUILoaded = true;

                // Create JSON object for Payment details
                JSONObject json = new JSONObject(paymentDetails);
                Log.d(MOLPAY, "MPMainUIWebClient onPageFinished paymentDetails = " + json.toString());

                // Init javascript
                mpMainUI.loadUrl("javascript:updateSdkData(" + json.toString() + ")");

            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        Log.e("logGooglePay" , "MOLPAY onActivityResult requestCode = " + requestCode);
        Log.e("logGooglePay" , "MOLPAY onActivityResult resultCode = " + resultCode);

        if (requestCode == MOLPayActivity.MOLPayXDK && data != null) {
            if (data.getStringExtra(MOLPayActivity.MOLPayTransactionResult) != null) {
                Log.e("logGooglePay", "MOLPAY result = " + data.getStringExtra(MOLPayActivity.MOLPayTransactionResult));
                Intent result = new Intent();
                result.putExtra(MOLPayTransactionResult, data.getStringExtra(MOLPayActivity.MOLPayTransactionResult));
                setResult(resultCode, result);
                finish();
            }
        }

    }

    public boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            // in case JSONArray is valid as well
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    private boolean storeImage(Bitmap image) {
        String fullPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();

        try {

            FileOutputStream fOut = null;
            File file = new File(fullPath, filename);
            file.createNewFile();
            fOut = new FileOutputStream(file);

            image.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();

            MediaScannerConnection.scanFile(this,new String[] { file.toString() }, null,null);

            Toast toast = Toast.makeText(this, "Image saved", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return true;

        } catch (Exception e) {
            Log.d(MOLPAY, "MPMainUIWebClient storeImage error = " + e.getMessage());
            Toast toast = Toast.makeText(this, "Image not saved", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return false;
        }
    }

    @TargetApi(23)
    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.d(MOLPAY, "isStoragePermissionGranted Permission granted");
                storeImage(imgBitmap);
                return true;
            } else {
                Log.d(MOLPAY, "isStoragePermissionGranted Permission revoked");
                ActivityCompat.requestPermissions(MOLPayActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_STORAGE);
                return false;
            }

        }	else { //permission is automatically granted on sdk<23 upon installation
            Log.d(MOLPAY, "isStoragePermissionGranted Permission granted on sdk<23");
            storeImage(imgBitmap);
            return true;
        }
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(MOLPAY, "onRequestPermissionsResult Permission: " + permissions[0] + "was " + grantResults[0]);
                //resume tasks needing this permission

                storeImage(imgBitmap);

            } else {
                Log.d(MOLPAY, "onRequestPermissionsResult EXTERNAL_STORAGE permission was NOT granted.");
                Toast.makeText(this, "Image not saved", Toast.LENGTH_LONG).show();
            }
        }
    }
}
