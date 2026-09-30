package com.fiuu.xdk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.fiuu.xdk.googlepay.ActivityGP;
import com.fiuu.xdk.models.DeviceInfo;
import com.fiuu.xdk.network.GatewayEndpoints;
import com.fiuu.xdk.log.ActivityLog;
import com.fiuu.xdk.network.PaymentEnvironment;
import com.fiuu.xdk.utils.DeviceInfoUtil;
import com.fiuu.xdk.utils.SecurityUtils;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PaymentActivity extends AppCompatActivity {

    public final static int XDKResponseCode = 9999;
    public final static String XDKPaymentDetails = "paymentDetails";
    public final static String XDKTransactionResult = "transactionResult";
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
    public final static String mp_closebutton_display = "mp_closebutton_display";
    public final static String mp_enable_fullscreen = "mp_enable_fullscreen";
    public final static String mp_metadata = "mp_metadata";
    public final static String mp_gpay_channel = "mp_gpay_channel";
    public final static String mp_hide_googlepay = "mp_hide_googlepay";
    public final static String mp_core_env = "mp_core_env";
    public final static String device_info = "device_info";

    public final static String logXDK = "logXDK";
    private final static String mpopenpaymentwindow = "mpopenmolpaywindow://";
    private final static String mpcloseallwindows = "mpcloseallwindows://";
    private final static String mptransactionresults = "mptransactionresults://";
    private final static String mprunscriptonpopup = "mprunscriptonpopup://";
    private final static String mppinstructioncapture = "mppinstructioncapture://";
    private final static String mpclickgpbutton = "mpclickgpbutton://";
    private static final String MAIN_FRAME_FORM_GUARD =
            "(function(){if(window.__fiuuFormGuard)return;window.__fiuuFormGuard=true;"
                    + "function hostOk(action){try{var u=new URL(action,location.href);"
                    + "if(u.protocol!=='https:')return false;var h=u.hostname.toLowerCase();"
                    + "return h==='fiuu.com'||h.endsWith('.fiuu.com')||h==='molpay.com'||h.endsWith('.molpay.com');}"
                    + "catch(e){return false;}}"
                    + "function destination(form,submitter){var action=submitter&&submitter.getAttribute&&submitter.getAttribute('formaction');"
                    + "return action||form.action||location.href;}"
                    + "document.addEventListener('submit',function(ev){var form=ev.target;"
                    + "if(!form||form.tagName!=='FORM')return;"
                    + "if(!hostOk(destination(form,ev.submitter)))ev.preventDefault();},true);"
                    + "var nativeSubmit=HTMLFormElement.prototype.submit;"
                    + "HTMLFormElement.prototype.submit=function(){if(!hostOk(destination(this,null)))return;"
                    + "return nativeSubmit.apply(this,arguments);};})();";
    private final static String module_id = "module_id";
    private final static String wrapper_version = "wrapper_version";
    private final static String wrapperVersion = "43a";
    private static final String TNG_EWALLET_PACKAGE = "my.com.tngdigital.ewallet";

    private String filename;
    private Bitmap imgBitmap;

    private WebView mpMainUI, mpPaymentUI, mpBankUI;
    private HashMap<String, Object> paymentDetails = new HashMap<>();
    private Boolean isMainUILoaded = false;
    private Boolean isClosingReceipt = false;
    private Boolean isClosebuttonDisplay = false;
    private Boolean isEnableFullscreen = false;
    private String setMPMainUI = "";
    private Boolean isTNGResult = false;
    private Boolean networkIssue = false;
    private long lastCloseTapTime = 0;
    /** Extract TNG systembrowserurl only once — iframe result.php can re-fire onPageFinished. */
    private boolean tngIntermediateHandled = false;
    /** Forward RMS/MOLPay result.php to mpMainUI only once (iframe loads skip onPageStarted). */
    private boolean fiuuResultNotified = false;

    private static final Gson gson =  new Gson();
    private static DeviceInfo deviceInfo;
    /** Visible WebView checkout only; cleared in {@link #onDestroy()}. */
    private static WeakReference<PaymentActivity> sCurrent;
    private final Handler timeoutHandler = new Handler();
    /** True once mpMainUI begins loading (connection is alive). */
    private boolean hasPageStarted = false;
    /** True once mpMainUI finishes loading. */
    private boolean isPageLoaded = false;
    private int progressLoading = 0;
    /**
     * Only auto-fail if the WebView never starts loading at all (dead DNS/network).
     * Once loading has started, do not kill the session — slow pages must stay up.
     */
    private static final int CONNECTION_TIMEOUT_MS = 15000;
    private static final int NETWORK_HEAVY_DURATION_MS = 3500;
    private Toolbar paymentToolbar;

    private final Runnable connectionTimeoutRunnable = () -> {
        if (isFinishing()) {
            return;
        }
        // Living loads must never be aborted here.
        if (hasPageStarted || isPageLoaded || progressLoading > 0) {
            return;
        }
        if (mpMainUI != null) {
            mpMainUI.stopLoading();
        }
        String dataString = "{ \"error\" : \"Timeout\"  }";
        ActivityLog.error(this, "webCoreTimeout", "Timer expired before the payment page started");
        Intent result = new Intent();
        result.putExtra(XDKTransactionResult, dataString);
        setResult(RESULT_OK, result);
        finish();
    };

    private final Runnable heavyTrafficRunnable = () -> {
        if (isFinishing() || isPageLoaded) {
            return;
        }
        if (progressLoading < 30) {
            Toast.makeText(this,
                    "We are experiencing heavy traffic.\nYou may experience slight delays.",
                    Toast.LENGTH_LONG).show();
        }
    };

    private void clearLoadWatchdogs() {
        timeoutHandler.removeCallbacks(connectionTimeoutRunnable);
        timeoutHandler.removeCallbacks(heavyTrafficRunnable);
    }

    private void markMainUiStarted() {
        hasPageStarted = true;
        ActivityLog.event(this, "pageStarted", "Payment page started");
        // Connection is alive — cancel hard timeout so slow pages are never killed.
        timeoutHandler.removeCallbacks(connectionTimeoutRunnable);
    }

    private void markMainUiLoaded() {
        isPageLoaded = true;
        hasPageStarted = true;
        clearLoadWatchdogs();
    }

    // Private API
    /**
     * Optional {@link #mp_closebutton_display}:
     * - true  → show Toolbar + Close menu
     * - false / omitted / invalid → hide Toolbar completely (no ActionBar)
     */
    private void applyCloseButtonChrome() {
        if (paymentToolbar == null) {
            return;
        }
        if (Boolean.TRUE.equals(isClosebuttonDisplay)) {
            paymentToolbar.setVisibility(View.VISIBLE);
            setSupportActionBar(paymentToolbar);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayShowTitleEnabled(false);
                actionBar.setTitle("");
                actionBar.show();
            }
            paymentToolbar.setTitle("");
            invalidateOptionsMenu();
        } else {
            // Optional flag off or absent: ActionBar/Toolbar must not appear at all.
            paymentToolbar.setVisibility(View.GONE);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }
            setSupportActionBar(null);
            invalidateOptionsMenu();
        }
    }

    private void resolveCloseButtonDisplay(JSONObject json) {
        // Default: optional param omitted → hide ActionBar.
        isClosebuttonDisplay = false;
        if (json == null || !json.has(mp_closebutton_display) || json.isNull(mp_closebutton_display)) {
            return;
        }
        try {
            Object value = json.get(mp_closebutton_display);
            if (value instanceof Boolean) {
                isClosebuttonDisplay = (Boolean) value;
            } else if (value instanceof String) {
                isClosebuttonDisplay = Boolean.parseBoolean(((String) value).trim());
            } else if (value instanceof Number) {
                isClosebuttonDisplay = ((Number) value).intValue() != 0;
            } else {
                isClosebuttonDisplay = json.optBoolean(mp_closebutton_display, false);
            }
        } catch (JSONException e) {
            isClosebuttonDisplay = false;
        }
    }

    private boolean isClosingPayment = false;
    /** True if a bank or 3DS overlay was open before Close dismissed it. */
    private boolean channelWasActive = false;

    /**
     * Dismiss the visible WebView checkout using the same path as Close / Back.
     * No-op if {@link PaymentActivity} is not showing. Does not close Google Pay.
     *
     * @return {@code true} if a live activity was asked to close
     */
    public static boolean closePayment() {
        WeakReference<PaymentActivity> current = sCurrent;
        PaymentActivity activity = current != null ? current.get() : null;
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return false;
        }
        activity.runOnUiThread(activity::closepayment);
        return true;
    }

    /**
     * Close behavior:
     * - WebView / payment JS not ready yet → cancel immediately (Close must always work).
     * - Channel/bank overlay open → dismiss it, then {@code javascript:closemolpay()} in one tap.
     * - Fully loaded → {@code javascript:closemolpay()}.
     */
    private static String resultSummary(String dataString) {
        try {
            JSONObject json = new JSONObject(dataString);
            if (json.has("error")) {
                return "error=" + json.optString("error");
            }
            if (json.has("error_code")) {
                return "error_code=" + json.optString("error_code");
            }
            if (json.has("StatCode")) {
                return "StatCode=" + json.optString("StatCode");
            }
            return "result received";
        } catch (JSONException e) {
            return "result received";
        }
    }

    private void forceCancelPayment(String errorMsg) {
        clearLoadWatchdogs();
        isClosingPayment = true;
        dismissChannelOverlays();
        String dataString = "{ \"error\" : \"" + errorMsg + "\"  }";
        ActivityLog.error(this, "paymentCancel", errorMsg);
        Intent result = new Intent();
        result.putExtra(XDKTransactionResult, dataString);
        setResult(RESULT_OK, result);
        finish();
    }

    /**
     * Close behavior:
     * - Emergency double-tap within 1.5s -> force cancel immediately.
     * - WebView / payment JS not ready yet -> cancel immediately.
     * - Check if window.closemolpay is a function via evaluateJavascript. If not, cancel immediately.
     * - Fully loaded and closemolpay exists -> dismiss overlays and invoke closemolpay().
     */
    private void closepayment() {
        if (isFinishing()) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        if (now - lastCloseTapTime < 1500) {
            // User tapped Close twice rapidly: emergency force exit
            boolean channelActive = channelWasActive
                    || isClosingPayment
                    || isChannelUiActive();
            if (channelActive) {
                // Overlay/bank UI is active: transaction is likely in-flight.
                // Exit with unknown status rather than assuming unbilled cancellation.
                forceCancelPayment("Transaction status unknown");
            } else {
                forceCancelPayment(networkIssue ? "Network Issue" : "Transaction Cancelled");
            }
            return;
        }
        lastCloseTapTime = now;

        // Ignore re-entry from bank WebView onCloseWindow while we are already closing.
        if (isClosingPayment) {
            return;
        }

        if (isClosingReceipt) {
            clearLoadWatchdogs();
            finish();
            return;
        }

        // isMainUILoaded is set only after updateSdkData is injected — that is when closemolpay() exists.
        boolean paymentJsReady = Boolean.TRUE.equals(isMainUILoaded)
                && isPageLoaded
                && mpMainUI != null
                && !networkIssue;

        if (!paymentJsReady) {
            forceCancelPayment(networkIssue ? "Network Issue" : "Transaction Cancelled");
            return;
        }

        isClosingPayment = true;
        dismissChannelOverlays();

        // Verify that closemolpay() actually exists in the loaded page
        mpMainUI.evaluateJavascript("typeof closemolpay === 'function'", result -> {
            if ("true".equalsIgnoreCase(result)) {
                mpMainUI.loadUrl("javascript:closemolpay()");
                mpMainUI.postDelayed(() -> isClosingPayment = false, 600);
            } else {
                // Page loaded was not the payment app (e.g. 404 or custom error)
                forceCancelPayment(networkIssue ? "Network Issue" : "Transaction Cancelled");
            }
        });
    }

    private boolean isChannelUiActive() {
        return (mpBankUI != null) || (mpPaymentUI != null && mpPaymentUI.getVisibility() == View.VISIBLE);
    }

    /** Tear down channel/bank overlays without relying on a second Close tap. */
    private void dismissChannelOverlays() {
        if (isChannelUiActive()) {
            channelWasActive = true;
        }
        if (mpBankUI != null) {
            try {
                mpBankUI.stopLoading();
                mpBankUI.loadUrl("about:blank");
                mpBankUI.setVisibility(View.GONE);
                mpBankUI.clearCache(true);
                mpBankUI.clearHistory();
                mpBankUI.removeAllViews();
                if (mpBankUI.getParent() != null) {
                    ((ViewGroup) mpBankUI.getParent()).removeView(mpBankUI);
                }
                mpBankUI.destroy();
            } catch (Exception ignored) {
            }
            mpBankUI = null;
        }
        if (mpPaymentUI != null && mpPaymentUI.getVisibility() == View.VISIBLE) {
            try {
                mpPaymentUI.stopLoading();
                mpPaymentUI.loadUrl("about:blank");
                mpPaymentUI.setVisibility(View.GONE);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Close menu only when mp_closebutton_display == true.
        if (Boolean.TRUE.equals(isClosebuttonDisplay)) {
            getMenuInflater().inflate(R.menu.menu_payment, menu);
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.closeBtn || Objects.equals(item.getTitle(), "Close")) {
            closepayment();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        paymentDetails = (HashMap<String, Object>) getIntent().getSerializableExtra(XDKPaymentDetails);
        // For submodule wrappers
        boolean is_submodule = false;
        isTNGResult = false;
        tngIntermediateHandled = false;
        fiuuResultNotified = false;

        if (paymentDetails != null) {

            JSONObject json = new JSONObject(paymentDetails);
            resolveCloseButtonDisplay(json);

            if (json.has("mp_enable_fullscreen")) {
                try {
                    isEnableFullscreen = json.getBoolean("mp_enable_fullscreen");
                } catch (JSONException e) {
                   // Log.e(logXDK, "mp_enable_fullscreen: ", e);
                }

                if (isEnableFullscreen) {
                    setTheme(R.style.Theme_Fullscreen);
                }
            }

            PaymentEnvironment env = PaymentEnvironment.resolve(paymentDetails);
            setMPMainUI = env.getWebUiBase();

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
            paymentDetails.put(device_info, gson.toJson(DeviceInfoUtil.getDeviceInfo(this)));
            ActivityLog.bindSession(
                    String.valueOf(paymentDetails.get(mp_order_ID)),
                    String.valueOf(paymentDetails.get(mp_merchant_ID)),
                    String.valueOf(paymentDetails.get(mp_country)));
            ActivityLog.event(this, "paymentStart",
                    "channel=" + paymentDetails.get(mp_channel)
                            + " amount=" + paymentDetails.get(mp_amount)
                            + " currency=" + paymentDetails.get(mp_currency));

        }

        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        // Same error result as before, but stop init after finish to avoid crash/leak.
        if (paymentDetails == null) {
            String dataString = "{ \"error\" : \" Payment details is null.\"  }";
            Intent result = new Intent();
            result.putExtra(XDKTransactionResult, dataString);
            setResult(RESULT_OK, result);
            finish();
            return;
        }

        sCurrent = new WeakReference<>(this);

        try {
            setContentView(R.layout.activity_payment);
        } catch (RuntimeException e) {
            Log.e(logXDK, "Failed to inflate activity_payment layout", e);
            String dataString = "{ \"error\" : \" Payment UI initialization failed.\"  }";
            Intent result = new Intent();
            result.putExtra(XDKTransactionResult, dataString);
            setResult(RESULT_OK, result);
            finish();
            return;
        }


        paymentToolbar = findViewById(R.id.paymentToolbar);
        applyCloseButtonChrome();

        if (isEnableFullscreen) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }

        // Bind resources
        mpMainUI = findViewById(R.id.MPMainUI);
        mpMainUI.setTag("mpMainUI");
        configureWebView(mpMainUI);

        mpMainUI.setWebViewClient(new MPMainUIWebClient());


        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(mpMainUI, true);

        mpPaymentUI = findViewById(R.id.MPMOLPayUI);
        mpPaymentUI.setTag("mpPaymentUI");
        configureWebView(mpPaymentUI);

        mpPaymentUI.setWebViewClient(new MPMainUIWebClient());
        mpPaymentUI.setWebChromeClient(new MPMainUIWebChromeClient());

        mpPaymentUI.setLongClickable(true);
        mpPaymentUI.setOnLongClickListener(view -> {
           // Log.d(logXDK, "Long press fired!");
            mpPaymentUI.evaluateJavascript("document.getElementById(\"qrcode_img\").src", qrdata -> {
               // Log.d(logXDK, "QR data = " + qrdata);
                if (qrdata != null && !qrdata.equals("null")) {
                    String imageQrCode = qrdata.replace("data:image/png;base64,", "");
                   // Log.d(logXDK, "imageQrCode = " + imageQrCode);
                    byte[] decodedBytes = Base64.decode(imageQrCode, 0);
                    imgBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    filename = Objects.requireNonNull(paymentDetails.get("mp_order_ID")) + ".png";

                    isStoragePermissionGranted();
                }
            });
            return false;
        });
        mpPaymentUI.setVisibility(View.GONE);
        cookieManager.setAcceptThirdPartyCookies(mpPaymentUI, true);

        mpMainUI.loadUrl(setMPMainUI);

        // Soft toast only — never finish the activity while the page is still loading.
        timeoutHandler.postDelayed(heavyTrafficRunnable, NETWORK_HEAVY_DURATION_MS);
        // Hard fail only if the WebView never starts (dead connection).
        timeoutHandler.postDelayed(connectionTimeoutRunnable, CONNECTION_TIMEOUT_MS);

        // Register a callback for handling the back press
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
               // Log.e("logGooglePay" , "WebCore onBackPressed");
                closepayment();
            }
        };

        // Add the callback to the OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, callback);

        boolean isRooted = isDeviceRooted(this);

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
            // stop further execution
        }
    }

    private void nativeWebRequestUrlUpdates(String url) {
       // Log.d(logXDK, "nativeWebRequestUrlUpdates url = " + url);

        HashMap<String, String> data = new HashMap<>();
        data.put("requestPath", url);

        // Create JSON object for Payment details
        JSONObject json = new JSONObject(data);

        // Init javascript
        mpMainUI.loadUrl("javascript:nativeWebRequestUrlUpdates(" + json + ")");
    }

    /**
     * Privacy Policy / Terms must open in an external browser so the payment WebView
     * is not replaced and the user does not get stuck outside the payment flow.
     *
     * @return true if the URL was handled externally (caller should return true from shouldOverrideUrlLoading)
     */
    private boolean openExternalBrowserForLegalUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        if (host == null) {
            return false;
        }
        String hostLower = host.toLowerCase(Locale.US);
        if (!hostLower.equals("fiuu.com") && !hostLower.equals("www.fiuu.com")) {
            return false;
        }
        String path = uri.getPath();
        if (path == null) {
            return false;
        }
        String pathLower = path.toLowerCase(Locale.US);
        if (!pathLower.startsWith("/privacy-policy") && !pathLower.startsWith("/terms-of-services")) {
            return false;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
           // Log.e(logXDK, "No browser available for legal URL", e);
            return false;
        }
        return true;
    }

    private static boolean isTngCashierUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        String lower = url.toLowerCase(Locale.US);
        return lower.contains("tngdigital.com.my") && lower.contains("/s/cashier/");
    }

    /**
     * Launch Touch 'n Go eWallet for a cashier URL. False means the app is missing —
     * keep the cashier in WebView (TNG web) instead of swallowing the navigation.
     */
    private boolean tryOpenTngApp(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage(TNG_EWALLET_PACKAGE);
            if (intent.resolveActivity(getPackageManager()) == null) {
                return false;
            }
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }

    /**
     * Wallet / app-link URLs from {@code mpPaymentUI}.
     *
     * @return true if consumed (do not load in WebView)
     */
    private boolean handleWalletExternalUrl(WebView webView, String url) {
        if (url.startsWith("intent:") || url.startsWith("android-app:")) {
            return handleIntentSchemeUrl(webView, url);
        }
        if (isTngCashierUrl(url)) {
            if (tryOpenTngApp(url)) {
                isTNGResult = true;
                return true;
            }
            return false;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                return false;
            }
        }
        return true;
    }

    private boolean handleIntentSchemeUrl(WebView webView, String url) {
        try {
            Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                if (TNG_EWALLET_PACKAGE.equals(intent.getPackage())) {
                    isTNGResult = true;
                }
                return true;
            }
            String fallback = intent.getStringExtra("browser_fallback_url");
            if (fallback != null && !fallback.isEmpty()) {
                if (isTngCashierUrl(fallback) && tryOpenTngApp(fallback)) {
                    isTNGResult = true;
                    return true;
                }
                webView.loadUrl(fallback);
                return true;
            }
        } catch (Exception ignored) {
        }
        return true;
    }

    private static String unwrapJsEvaluateResult(String value) {
        if (value == null || value.isEmpty() || "null".equals(value) || "\"null\"".equals(value)) {
            return "";
        }
        String unquoted = value.trim();
        if (unquoted.length() >= 2 && unquoted.startsWith("\"") && unquoted.endsWith("\"")) {
            unquoted = unquoted.substring(1, unquoted.length() - 1);
        }
        return unquoted.replace("\\n", "").replace("\\r", "").trim();
    }

    private static boolean isIntermediateTngPage(String url) {
        return url != null && (url.contains("intermediate_appTNG-EWALLET.php")
                || url.contains("intermediate_app/processing.php")
                || url.contains("intermediate_app/process.php"));
    }

    /**
     * Fiuu/MOLPay/RMS payment result page. XDK JS {@code nativeWebRequestUrlUpdates}
     * only completes checkout when it sees {@code MOLPay/result.php} or {@code RMS/result.php}.
     */
    private static boolean isFiuuPaymentResultUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        String lower = url.toLowerCase(Locale.US);
        int query = lower.indexOf('?');
        String path = query >= 0 ? lower.substring(0, query) : lower;
        boolean gatewayHost = lower.contains("fiuu.com")
                || lower.contains("molpay.com")
                || lower.contains("razer.com")
                || path.contains("/rms/")
                || path.contains("/molpay/");
        return gatewayHost && (path.contains("molpay/result.php")
                || path.contains("rms/result.php")
                || path.contains("/result.php"));
    }

    private void notifyFiuuResultUrl(String url) {
        if (fiuuResultNotified || url == null || mpMainUI == null) {
            return;
        }
        fiuuResultNotified = true;
        nativeWebRequestUrlUpdates(url);
    }

//    private void nativeWebRequestUrlUpdatesOnFinishLoad(String url) {
//       // Log.d(logXDK, "nativeWebRequestUrlUpdatesOnFinishLoad url = " + url);
//
//        HashMap<String, String> data = new HashMap<>();
//        data.put("requestPath", url);
//
//        // Create JSON object for Payment details
//        JSONObject json = new JSONObject(data);
//
//        // Init javascript
//        mpMainUI.loadUrl("javascript:nativeWebRequestUrlUpdatesOnFinishLoad(" + json + ")");
//    }

    private class MPMainUIWebClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView webView, String url, Bitmap favicon) {
            String tagString = (String) webView.getTag();
            if (url == null || url.trim().isEmpty() || url.equals("about:blank")) {
               // Log.d(logXDK, "Invalid WebResourceRequest or null URL");
                return; // Let WebView handle null cases
            }

           // Log.d(logXDK, tagString + " onPageStarted url = " + url);

            if(tagString.equals("mpMainUI")){
                markMainUiStarted();
                return;
            }

            if (isFiuuPaymentResultUrl(url)) {
                notifyFiuuResultUrl(url);
                return;
            }

            nativeWebRequestUrlUpdates(url);

        }
        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest request) {
            String tagString = (String) webView.getTag();
            Uri uri = request.getUrl();

            if (uri == null || uri.toString().trim().isEmpty()) {
               // Log.d(logXDK, "Invalid WebResourceRequest or null URL");
                return false; // Let WebView handle null cases
            }
            String url = uri.toString();

           // Log.d(logXDK, tagString + " shouldOverrideUrlLoading url = " + url);

            // Keep payment WebView intact — open legal pages in the system browser.
            if (openExternalBrowserForLegalUrl(url)) {
                return true;
            }

            if(tagString.equals("mpPaymentUI")){
                if (isFiuuPaymentResultUrl(url)) {
                    notifyFiuuResultUrl(url);
                    return false;
                }
                if (url.contains("scbeasy/easy_app_link.html")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        // Define what your app should do if no activity can handle the intent.
                       // Log.e(logXDK, "scbeasy: ", e);
                    }
                    webView.evaluateJavascript("document.getElementById(\"ref_no\").value", ref_no -> {
                        PaymentEnvironment env = PaymentEnvironment.resolve(paymentDetails);
                        webView.loadUrl(GatewayEndpoints.getLoadingUrl(env, ref_no));
                    });
                    return true;
                }
                if (url.contains("atome-my.onelink.me") ||
                        url.contains("myboost.app") ||
                        url.contains("market://") ||
                        url.startsWith("intent:") ||
                        url.contains("alipays://") ||
                        url.contains("https://app.shopback.com/pay") ||
                        isTngCashierUrl(url)) {
                    return handleWalletExternalUrl(webView, url);
                }
            }
            if(tagString.equals("mpMainUI")){
                if (url.startsWith(mpopenpaymentwindow)) {
                    String base64String = url.replace(mpopenpaymentwindow, "");

                    // Decode base64
                    byte[] data = Base64.decode(base64String, Base64.DEFAULT);
                    String dataString = new String(data);

                    if (!dataString.isEmpty()) {
                        if (mpPaymentUI != null) {
                            mpPaymentUI.setVisibility(View.VISIBLE);
                            ActivityLog.event(PaymentActivity.this, "channelScreen", "Channel screen opened");
                            String formAction = extractFormAction(dataString);
                            byte[] postData = buildPostData(dataString);
                            if (!formAction.isEmpty() && postData != null && postData.length > 0) {
                                mpPaymentUI.post(() -> mpPaymentUI.postUrl(formAction, postData));
                            } else {
                                CookieManager.getInstance().flush();
                                final String finalDataString = dataString;
                                PaymentEnvironment env = PaymentEnvironment.resolve(paymentDetails);
                                mpPaymentUI.post(() -> mpPaymentUI.loadDataWithBaseURL(env.getPaymentBase(), finalDataString, "text/html", "UTF-8", ""));
                            }
                        } else {
                            Log.d(logXDK, "mpPaymentUI NULL avoid crash");
                        }
                    } else {
                        Log.d(logXDK, "MPMainUIWebClient mpopenpaymentwindow empty dataString");
                    }
                    return true;
                }
                if (url.startsWith(mpcloseallwindows)) {
                    if (mpBankUI != null) {
                        mpBankUI.loadUrl("about:blank");
                        mpBankUI.setVisibility(View.GONE);
                        mpBankUI.clearCache(true);
                        mpBankUI.clearHistory();
                        mpBankUI.removeAllViews();
                        if (mpBankUI.getParent() != null) {
                            ((ViewGroup) mpBankUI.getParent()).removeView(mpBankUI);
                        }
                        mpBankUI.destroy();
                        mpBankUI = null;
                    }
                    if (mpPaymentUI != null) {
                        mpPaymentUI.loadUrl("about:blank");
                        mpPaymentUI.setVisibility(View.GONE);
                        mpPaymentUI.clearCache(true);
                        mpPaymentUI.clearHistory();
                        mpPaymentUI.removeAllViews();
                        if (mpPaymentUI.getParent() != null) {
                            ((ViewGroup) mpPaymentUI.getParent()).removeView(mpPaymentUI);
                        }
                        mpPaymentUI.destroy();
                        mpPaymentUI = null;
                    }

                    return true;
                }
                if (url.startsWith(mptransactionresults)) {
                    String base64String = url.replace(mptransactionresults, "");
                   // Log.d(logXDK, "MPMainUIWebClient mptransactionresults base64String = " + base64String);

                    // Decode base64
                    byte[] data = Base64.decode(base64String, Base64.DEFAULT);
                    String dataString = new String(data);
                   // Log.d(logXDK, "MPMainUIWebClient mptransactionresults dataString = " + dataString);

                    Intent result = new Intent();
                    result.putExtra(XDKTransactionResult, dataString);

                    if (isJSONValid(dataString)) {
                       // Log.d(logXDK, "isJSONValid setResult");
                        ActivityLog.event(PaymentActivity.this, "paymentResult", resultSummary(dataString));
                        setResult(RESULT_OK, result);

                        // Check if mp_request_type is "Receipt", if it is, don't finish()
                        try {
                            JSONObject jsonResult = new JSONObject(dataString);

                           // Log.d(logXDK, "MPMainUIWebClient jsonResult = " + jsonResult);

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
                    } else {
                        Log.d(logXDK, "json not valid dont setResult");
//                    setResult(RESULT_CANCELED, result);
                    }

                    return true;
                }
                if (url.startsWith(mprunscriptonpopup)) {
                    String base64String = url.replace(mprunscriptonpopup, "");
                   // Log.d(logXDK, "MPMainUIWebClient mprunscriptonpopup base64String = " + base64String);

                    // Decode base64
                    byte[] data = Base64.decode(base64String, Base64.DEFAULT);
                    String jsString = new String(data);
                   // Log.d(logXDK, "MPMainUIWebClient mprunscriptonpopup jsString = " + jsString);

                    if (mpBankUI != null) {
                        mpBankUI.loadUrl("javascript:" + jsString);
                       // Log.d(logXDK, "mpBankUI loadUrl = " + "javascript:" + jsString);
                    }

                    return true;
                }
                if (url.startsWith(mppinstructioncapture)) {
                    String base64String = url.replace(mppinstructioncapture, "");
                   // Log.d(logXDK, "MPMainUIWebClient mppinstructioncapture base64String = " + base64String);

                    // Decode base64
                    byte[] data = Base64.decode(base64String, Base64.DEFAULT);
                    String dataString = new String(data);
                   // Log.d(logXDK, "MPMainUIWebClient mppinstructioncapture dataString = " + dataString);

                    try {
                        JSONObject jsonResult = new JSONObject(dataString);

                        String base64Img = jsonResult.getString("base64ImageUrlData");
                        filename = jsonResult.getString("filename");
                       // Log.d(logXDK, "MPMainUIWebClient jsonResult = " + jsonResult);

                        byte[] decodedBytes = Base64.decode(base64Img, 0);
                        imgBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                        isStoragePermissionGranted();

                    } catch (Throwable t) {
                       // Log.d(logXDK, "MPMainUIWebClient jsonResult error = " + t);
                    }

                    return true;
                }
                if (url.startsWith(mpclickgpbutton)) {
                    String mp_channel = "";
                   // Log.e("logGooglePay", "url = " + url);
                    // Extract the part after "://"
                    String base64Part = url.substring(url.indexOf("://") + 3);
                    // Decode the Base64 string
                    byte[] decodedBytes = Base64.decode(base64Part, Base64.DEFAULT);
                    String decodedString = new String(decodedBytes);
                   // Log.e("logGooglePay", "decodedString = " + decodedString);

                    try {
                        JSONObject json = new JSONObject(decodedString);
                        mp_channel = json.getString("mp_channel");
                       // Log.e("logGooglePay", "mp_channel = " + mp_channel);
                    } catch (Exception e) {
                       // Log.e(logXDK, "logGooglePay: ", e);
                    }

                    // Optional params checker

                    if (paymentDetails.get("mp_extended_vcode") == null) {
                        paymentDetails.put(PaymentActivity.mp_extended_vcode, false);
                    }

                    if (paymentDetails.get("mp_sandbox_mode") == null) {
                        paymentDetails.put(PaymentActivity.mp_sandbox_mode, false);
                    }

                    if (paymentDetails.get(PaymentActivity.mp_gpay_channel) == null) {
                        if (mp_channel.toLowerCase().contains("tng")) {
                            paymentDetails.put(PaymentActivity.mp_gpay_channel, new String[]{"TNG-EWALLET"});
                        } else if (mp_channel.toLowerCase().contains("shopee")) {
                            paymentDetails.put(PaymentActivity.mp_gpay_channel, new String[]{"SHOPEEPAY"});
                        } else if (mp_channel.toLowerCase().contains("credit")) {
                            paymentDetails.put(PaymentActivity.mp_gpay_channel, new String[]{"CC"});
                        } else {
                            paymentDetails.put(PaymentActivity.mp_gpay_channel, new String[]{"SHOPEEPAY", "TNG-EWALLET", "CC"});
                        }
                    }

                    paymentDetails.putIfAbsent(PaymentActivity.mp_closebutton_display, false);

                    // Required for Google Pay — same open path when present; error result instead of NPE when missing.
                    String[] requiredGpKeys = {
                            "mp_merchant_ID", "mp_verification_key", "mp_amount", "mp_order_ID",
                            "mp_currency", "mp_country", "mp_bill_description", "mp_bill_name",
                            "mp_bill_email", "mp_bill_mobile"
                    };
                    for (String key : requiredGpKeys) {
                        if (paymentDetails.get(key) == null) {
                            String dataString = "{ \"error\" : \"Missing required field: " + key + "\"  }";
                            Intent result = new Intent();
                            result.putExtra(XDKTransactionResult, dataString);
                            setResult(RESULT_OK, result);
                            finish();
                            return true;
                        }
                    }

                    openGPActivityWithResult();
                    return true;
                }

                if (request.isForMainFrame()
                        && (url.startsWith("http://") || url.startsWith("https://"))
                        && !SecurityUtils.isTrustedGatewayUrl(url)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest request) {
            if (request != null && request.getUrl() != null && webView != null) {
                final String interceptUrl = request.getUrl().toString();
                if (isFiuuPaymentResultUrl(interceptUrl)) {
                    webView.post(() -> notifyFiuuResultUrl(interceptUrl));
                }
            }
            return super.shouldInterceptRequest(webView, request);
        }

        @Override
        public void onPageFinished (WebView webView, String url) {
            String tagString = (String) webView.getTag();
            if (url == null || url.trim().isEmpty()) {
               // Log.d(logXDK, "Invalid WebResourceRequest or null URL");
                return; // Let WebView handle null cases
            }
           // Log.d(logXDK, tagString + " onPageFinished url = " + url);

            if(tagString.equals("mpPaymentUI")){
                if (isFiuuPaymentResultUrl(url)) {
                    notifyFiuuResultUrl(url);
                    return;
                }

                if (isIntermediateTngPage(url) && !tngIntermediateHandled) {
                    tngIntermediateHandled = true;

                    webView.evaluateJavascript("document.getElementById(\"systembrowserurl\").innerHTML", s -> {
                        if (fiuuResultNotified) {
                            return;
                        }
                        String payload = unwrapJsEvaluateResult(s);
                        if (payload.isEmpty()) {
                            tngIntermediateHandled = false;
                            return;
                        }
                        final String dataString;
                        try {
                            dataString = new String(Base64.decode(payload, Base64.DEFAULT)).trim();
                        } catch (IllegalArgumentException e) {
                            tngIntermediateHandled = false;
                            return;
                        }
                        if (dataString.isEmpty()) {
                            tngIntermediateHandled = false;
                            return;
                        }
                        if (isFiuuPaymentResultUrl(dataString)) {
                            notifyFiuuResultUrl(dataString);
                            return;
                        }
                        if (handleWalletExternalUrl(webView, dataString)) {
                            return;
                        }
                        if (dataString.startsWith("http://") || dataString.startsWith("https://")) {
                            webView.loadUrl(dataString);
                        }
                    });

                }
                return;
            }
            if(tagString.equals("mpMainUI")){
                markMainUiLoaded();
                if (!"about:blank".equals(url)) {
                    webView.evaluateJavascript(MAIN_FRAME_FORM_GUARD, null);
                }
                if (!isMainUILoaded && !url.equals("about:blank")) {
                    if (paymentDetails != null) {
                        isMainUILoaded = true;
                        ActivityLog.event(PaymentActivity.this, "pageReady", "Payment page ready");
                        JSONObject json = new JSONObject(paymentDetails);
                        //                   // Log.d(logXDK, "MPMainUIWebClient onPageFinished paymentDetails = " + json);
                        //                    Init javascript
                        mpMainUI.loadUrl("javascript:updateSdkData(" + json + ")");
                    }
                    else {
                        String dataString = "{ \"error\" : \" Payment details is null.\"  }";
                        //                   // Log.d(logXDK, "MPMainUIWebClient mptransactionresults dataString = " + dataString);
                        Intent result = new Intent();
                        result.putExtra(XDKTransactionResult, dataString);
                        setResult(RESULT_OK, result);
                        finish();
                    }
                }
                return;
            }
            nativeWebRequestUrlUpdates(url);
        }

        @Override
        public void onReceivedError(WebView webView, WebResourceRequest request, WebResourceError error) {
            // This is the simplest way - if this triggers, loadURL failed
            if (!request.isForMainFrame()) return;

            String tagString = (String) webView.getTag();
            // Only main payment UI network failures should block closemolpay() / mark networkIssue.
            // Channel/bank pages often emit main-frame errors (custom schemes, app links) and must not
            // force Close into the cancel path.
            int errorCode = error.getErrorCode();
            Uri uri = request.getUrl();
            String url = uri.toString();
            String detail = "code=" + errorCode
                    + " " + error.getDescription()
                    + " host=" + (uri.getHost() == null ? "" : uri.getHost());
            if ("mpMainUI".equals(tagString)) {
                networkIssue = true;
                timeoutHandler.removeCallbacks(connectionTimeoutRunnable);
                ActivityLog.error(PaymentActivity.this, "onReceivedError", detail);
            } else {
                ActivityLog.error(PaymentActivity.this, "onReceivedError", tagString + " " + detail);
            }

           // Log.d(logXDK, tagString + " onPageFinished url = " + url);
           // Log.e(logXDK, "WebViewClient " + errorCode);

        }
        @Override
        public void onReceivedHttpError(WebView webView, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(webView, request, errorResponse);
            if (!request.isForMainFrame()) return;

            String tagString = (String) webView.getTag();
            int statusCode = errorResponse.getStatusCode();
            String reasonPhrase = errorResponse.getReasonPhrase();
            Uri uri = request.getUrl();
            String url = uri.toString();
           // Log.e(logXDK, "Error statusCode: " + statusCode + " webView: " + tagString + " reasonPhrase: " + reasonPhrase + " url: " + url);

            String httpDetail = "status=" + statusCode + " " + reasonPhrase
                    + " host=" + (uri.getHost() == null ? "" : uri.getHost());
            //only handle error on fiuu side.
            if(!tagString.equals("mpMainUI")) {
                ActivityLog.error(PaymentActivity.this, "onReceivedHttpError", tagString + " " + httpDetail);
                return;
            }
            if (statusCode >= 400) {
                networkIssue = true;
                clearLoadWatchdogs();
                ActivityLog.error(PaymentActivity.this, "onReceivedHttpError", httpDetail);
            }
            if (statusCode == 503) {
               // Log.e("WebView", "HTTP 503 Service Unavailable");
                String dataString = "{ \"error\" : \"HTTP 503 Service Unavailable\"  }";
                //                   // Log.d(logXDK, "MPMainUIWebClient mptransactionresults dataString = " + dataString);
                Intent result = new Intent();
                result.putExtra(XDKTransactionResult, dataString);
                setResult(RESULT_OK, result);
                new AlertDialog.Builder(webView.getContext())
                        .setTitle("Service Unavailable")
                        .setMessage("The server is currently unavailable (503). Please try again later.")
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                finish();
                            }
                        })
                        .show();
            }
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            ActivityLog.error(PaymentActivity.this, "onReceivedSslError", ActivityLog.sslDetail(error));
           super.onReceivedSslError(view, handler, error);
        }

    }

    private class MPMainUIWebChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progressLoading = newProgress; // 0 to 100
            Log.d(logXDK, "Progress Percentage: " + progressLoading);

            // Any progress means the connection is alive — do not hard-timeout.
            if (newProgress > 0) {
                markMainUiStarted();
            }
            if (newProgress >= 100) {
                timeoutHandler.removeCallbacks(heavyTrafficRunnable);
            }
        }

        @SuppressLint("SetJavaScriptEnabled")
        @Override
        public boolean onCreateWindow(WebView webView, boolean dialog, boolean userGesture, Message resultMsg) {
            String tagString = (String) webView.getTag();

            if(tagString.equals("mpPaymentUI")){
                mpBankUI = new WebView(PaymentActivity.this);
                mpBankUI.setTag("mpBankUI");
                ActivityLog.event(PaymentActivity.this, "bankScreen", "Bank or 3DS screen opened");
                createWebView(mpBankUI, resultMsg);
                return true;
            }
            return false;
        }

        @Override
        public void onCloseWindow(WebView webView) {
            String tagString = (String) webView.getTag();

            // Only clean up the bank window. Do not call closepayment() here — that caused
            // Close to need two taps (first closed the popup, second ran closemolpay).
            if ("mpBankUI".equals(tagString)) {
                if (mpBankUI != null && webView == mpBankUI) {
                    try {
                        if (mpBankUI.getParent() != null) {
                            ((ViewGroup) mpBankUI.getParent()).removeView(mpBankUI);
                        }
                        mpBankUI.destroy();
                    } catch (Exception ignored) {
                    }
                    mpBankUI = null;
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if(isTNGResult && mpPaymentUI != null){
               // Log.d(logXDK, "onResume TNG condition");
                closepayment();
            }
        } catch (Exception e) {
           // Log.e(logXDK, "TNG: ", e);
            closepayment();
        }
    }

    @Override
    protected void onDestroy() {
        clearLoadWatchdogs();
        WeakReference<PaymentActivity> current = sCurrent;
        if (current != null && current.get() == this) {
            sCurrent = null;
        }
        if (paymentDetails != null) {
            paymentDetails.clear();
        }
        super.onDestroy();
    }


    private void openGPActivityWithResult(){
       // Log.d(logXDK, "openGPActivityWithResult paymentDetails = " + paymentDetails);
        Intent intent = new Intent(this, ActivityGP.class);
        intent.putExtra(PaymentActivity.XDKPaymentDetails, paymentDetails);
        gpActivityResultLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> gpActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {

               // Log.d("logGooglePay", "MOLPayActivity gpActivityResultLauncher result = " + result.toString());

                if (result.getData() != null) {
                    Intent data = result.getData();
                    String transactionResult = data.getStringExtra(PaymentActivity.XDKTransactionResult);

                    if (transactionResult != null) {
                        Intent intent = new Intent();
                        intent.putExtra(XDKTransactionResult, transactionResult);
                        setResult(result.getResultCode(), intent);
                        finish();
                    }
                }
            }
    );

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

    private void storeImage(Bitmap image) {
        if (image == null || filename == null || filename.trim().isEmpty()) {
            showImageSaveToast("Image not saved");
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Scoped storage: no WRITE_EXTERNAL_STORAGE prompt; write via MediaStore.
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                values.put(MediaStore.Downloads.MIME_TYPE, "image/png");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                Uri itemUri = getContentResolver().insert(collection, values);
                if (itemUri == null) {
                    throw new IllegalStateException("Failed to create MediaStore entry");
                }

                try (OutputStream out = getContentResolver().openOutputStream(itemUri)) {
                    if (out == null || !image.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        throw new IllegalStateException("Failed to write image");
                    }
                }

                values.clear();
                values.put(MediaStore.Downloads.IS_PENDING, 0);
                getContentResolver().update(itemUri, values, null, null);
            } else {
                String fullPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
                File file = new File(fullPath, filename);
                File parent = file.getParentFile();
                if (parent != null) {
                    //noinspection ResultOfMethodCallIgnored
                    parent.mkdirs();
                }
                try (FileOutputStream fOut = new FileOutputStream(file)) {
                    if (!image.compress(Bitmap.CompressFormat.PNG, 100, fOut)) {
                        throw new IllegalStateException("Failed to write image");
                    }
                    fOut.flush();
                }
                MediaScannerConnection.scanFile(this, new String[]{file.toString()}, null, null);
            }

            showImageSaveToast("Image saved");
        } catch (Exception e) {
           // Log.d(logXDK, "MPMainUIWebClient storeImage error = " + e.getMessage());
            showImageSaveToast("Image not saved");
        }
    }

    private void showImageSaveToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    /**
     * Saves QR/instruction image. On API 29+ MediaStore needs no storage runtime permission
     * (WRITE_EXTERNAL_STORAGE is ignored and will not show a system prompt). On API 28 and
     * below, request WRITE_EXTERNAL_STORAGE before writing to public Downloads.
     */
    public void isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            storeImage(imgBitmap);
            return;
        }

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            storeImage(imgBitmap);
        } else {
            ActivityCompat.requestPermissions(
                    PaymentActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_EXTERNAL_STORAGE);
        }
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_EXTERNAL_STORAGE) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            storeImage(imgBitmap);
        } else {
            Toast.makeText(this, "Image not saved", Toast.LENGTH_LONG).show();
        }
    }

    public static boolean isDeviceRooted(Context context) {
        String[] paths = {
                "/system/app/Superuser.apk",
                "/system/etc/init.d/99SuperSUDaemon",
                "/dev/com.koushikdutta.superuser.daemon/",
                "/system/xbin/daemonsu",
                "/sbin/su",
                "/system/bin/su",
                "/system/bin/failsafe/su",
                "/system/xbin/su",
                "/system/xbin/busybox",
                "/system/sd/xbin/su",
                "/data/local/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
        };
        for (String path : paths) {
            if (new File(path).exists())
                return true;
        }

        String[] knownRootAppsPackages = {
                "eu.chainfire.supersu",
                "com.noshufou.android.su",
                "com.koushikdutta.superuser",
                "com.zachspong.temprootremovejb",
                "com.ramdroid.appquarantine",
                "com.topjohnwu.magisk",
        };

        for (String pkg : knownRootAppsPackages) {
            try {
                context.getPackageManager().getPackageInfo(pkg, 0);
                return true;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return false;
    }
    public void createWebView(WebView webView, Message resultMsg){
        RelativeLayout container = findViewById(R.id.MPContainer);

        configureWebView(webView);
        webView.setWebViewClient(new MPMainUIWebClient());
        webView.setWebChromeClient(new MPMainUIWebChromeClient());

        webView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        container.addView(webView);
        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        transport.setWebView(webView);
        resultMsg.sendToTarget();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void configureWebView(WebView webView) {
        String tagString = (String) webView.getTag(); // Cast to String
       // Log.d(logXDK, "configureWebView: " + tagString);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        if(tagString.equals("mpMainUI")){
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            settings.setDomStorageEnabled(true);
            settings.setAllowFileAccess(false);
            settings.setAllowFileAccessFromFileURLs(false);
            settings.setAllowUniversalAccessFromFileURLs(false);
            settings.setAllowContentAccess(false);
            return;
        }
        if(tagString.equals("mpBankUI")){
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            settings.setAllowUniversalAccessFromFileURLs(false);
            settings.setAllowFileAccessFromFileURLs(false);
            settings.setAllowFileAccess(false);
            settings.setAllowContentAccess(false);
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
            settings.setJavaScriptCanOpenWindowsAutomatically(true);
            settings.setSupportMultipleWindows(true);
            webView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            return;
        }

        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

    }

    private String extractFormAction(String html) {
        Matcher m = Pattern.compile("action=[\"']([^\"']*)[\"']").matcher(html);
        return m.find() ? m.group(1) : "";
    }

    private byte[] buildPostData(String html) {
        try {
            StringBuilder sb = new StringBuilder();
            Matcher inputMatcher = Pattern.compile("<input([^>]*)>", Pattern.CASE_INSENSITIVE).matcher(html);
            Pattern attrPattern = Pattern.compile("([a-zA-Z_][\\w-]*)=[\"']([^\"']*)[\"']");
            while (inputMatcher.find()) {
                String attrs = inputMatcher.group(1);
                Matcher attrMatcher = attrPattern.matcher(attrs);
                String name = null, value = null;
                while (attrMatcher.find()) {
                    if ("name".equalsIgnoreCase(attrMatcher.group(1))) name = attrMatcher.group(2);
                    else if ("value".equalsIgnoreCase(attrMatcher.group(1))) value = attrMatcher.group(2);
                }
                if (name != null && value != null) {
                    if (sb.length() > 0) sb.append("&");
                    sb.append(URLEncoder.encode(name, "UTF-8"))
                      .append("=")
                      .append(URLEncoder.encode(value, "UTF-8"));
                }
            }
            return sb.toString().getBytes("UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

}
