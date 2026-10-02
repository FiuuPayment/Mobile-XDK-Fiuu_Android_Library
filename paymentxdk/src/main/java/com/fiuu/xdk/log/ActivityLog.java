package com.fiuu.xdk.log;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.net.ConnectivityManager;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.Log;

import com.fiuu.xdk.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Posts one payment-attempt trace to the mobile activity log.
 * Steps stay in memory until the attempt ends, so AWS stores a single row
 * whose process name is the channel.
 */
public final class ActivityLog {

    public static final String DEFAULT_TELEMETRY_URL = "https://mobile.fiuu.com/api/log/activity";
    private static final String TAG = "ActivityLog";
    private static final MediaType JSON = MediaType.get("application/json; charset=UTF-8");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final int MAX_STEPS = 40;
    private static final int MAX_FIELD = 800;
    private static final int MAX_DETAILS = 4000;
    private static final String CHECKOUT = "checkout";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final OkHttpClient CLIENT = new OkHttpClient();

    private static volatile String telemetryUrl = DEFAULT_TELEMETRY_URL;
    private static final byte[] MARK = {(byte) 0xa3, (byte) 0x17, (byte) 0x5c, (byte) 0xe1, (byte) 0x2b, (byte) 0x90, (byte) 0x44, (byte) 0x6f};
    private static volatile String sessionNote;
    private static volatile Trace current;

    private static final class Step {
        final String name;
        final String at;
        final String request;
        final String response;
        int count = 1;

        Step(String name, String at, String request, String response) {
            this.name = name;
            this.at = at;
            this.request = request;
            this.response = response;
        }
    }

    private static final class Trace {
        final Context app;
        final String referenceNumber;
        final String merchantId;
        final String country;
        String channel;
        boolean posted;
        final List<Step> steps = new ArrayList<>();

        Trace(Context app, String referenceNumber, String merchantId, String country, String channel) {
            this.app = app;
            this.referenceNumber = referenceNumber;
            this.merchantId = merchantId;
            this.country = country;
            this.channel = channel;
        }
    }

    private ActivityLog() {
    }

    /**
     * Optional override for the in-app request note. A blank value restores the built-in note.
     */
    public static void setSessionNote(String note) {
        if (note == null || note.trim().isEmpty()) {
            sessionNote = null;
            return;
        }
        sessionNote = note.trim();
    }

    public static void setTelemetryUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            telemetryUrl = DEFAULT_TELEMETRY_URL;
            return;
        }
        telemetryUrl = url.trim();
    }

    /**
     * Opens a trace, or keeps the open trace when the order id is unchanged.
     * A more specific channel replaces {@code checkout}. Returns true when a new trace was opened.
     */
    public static boolean begin(Context context, String reference, String merchant, String countryCode, String channel) {
        if (context == null) {
            return false;
        }
        Context app = context.getApplicationContext();
        String ref = reference == null ? "" : reference;
        String merchantId = merchant == null ? "" : merchant;
        String country = countryCode == null ? "" : countryCode;
        String normalized = normalizeChannel(channel);
        synchronized (ActivityLog.class) {
            installCrashHandler();
            Trace trace = current;
            if (trace != null && !trace.posted && trace.referenceNumber.equals(ref)) {
                if (!CHECKOUT.equals(normalized)) {
                    trace.channel = normalized;
                }
                return false;
            }
            if (trace != null && !trace.posted) {
                trace.posted = true;
                current = null;
                dispatch(trace, "interrupted", "interrupted", false);
            }
            current = new Trace(app, ref, merchantId, country, normalized);
            return true;
        }
    }

    /** Forces the open trace onto a channel such as {@code GooglePay}. */
    public static void setChannel(String channel) {
        String normalized = normalizeChannel(channel);
        if (CHECKOUT.equals(normalized)) {
            return;
        }
        synchronized (ActivityLog.class) {
            Trace trace = current;
            if (trace != null && !trace.posted) {
                trace.channel = normalized;
            }
        }
    }

    /** Sets the channel only while the trace is still {@code checkout}. */
    public static void setChannelIfUnset(String channel) {
        String normalized = normalizeChannel(channel);
        if (CHECKOUT.equals(normalized)) {
            return;
        }
        synchronized (ActivityLog.class) {
            Trace trace = current;
            if (trace != null && !trace.posted && CHECKOUT.equals(trace.channel)) {
                trace.channel = normalized;
            }
        }
    }

    public static void step(String name, String request, String response) {
        Trace trace;
        synchronized (ActivityLog.class) {
            trace = current;
        }
        if (trace == null || trace.posted) {
            return;
        }
        String safeName = name == null || name.trim().isEmpty() ? "event" : name.trim();
        String safeRequest = trimField(request);
        String safeResponse = trimField(response);
        synchronized (trace) {
            if (trace.posted) {
                return;
            }
            if (!trace.steps.isEmpty()) {
                Step last = trace.steps.get(trace.steps.size() - 1);
                if (last.name.equals(safeName)
                        && same(last.request, safeRequest)
                        && same(last.response, safeResponse)) {
                    last.count++;
                    return;
                }
            }
            trace.steps.add(new Step(safeName, LocalDateTime.now().format(STAMP), safeRequest, safeResponse));
            if (trace.steps.size() > MAX_STEPS) {
                trace.steps.remove(0);
            }
        }
    }

    /**
     * Posts the open trace once. Later calls do nothing until {@link #begin} opens another attempt.
     */
    public static void finish(Context context, String type, String outcome) {
        Trace trace;
        synchronized (ActivityLog.class) {
            trace = current;
            if (trace == null || trace.posted) {
                return;
            }
            trace.posted = true;
            current = null;
        }
        dispatch(trace, type, outcome, false);
    }

    /** Reads selected fields out of a JSON payload. Missing fields are skipped. */
    public static String pick(String json, String... keys) {
        if (json == null || json.isEmpty() || keys == null) {
            return "";
        }
        try {
            JSONObject object = new JSONObject(json);
            StringBuilder builder = new StringBuilder();
            for (String key : keys) {
                if (key == null || !object.has(key) || object.isNull(key)) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(key).append('=').append(object.optString(key));
            }
            return builder.toString();
        } catch (JSONException e) {
            return "";
        }
    }

    public static String sslDetail(SslError error) {
        if (error == null) {
            return "cert error";
        }
        String host = "";
        if (error.getUrl() != null) {
            Uri uri = Uri.parse(error.getUrl());
            host = uri.getHost() == null ? "" : uri.getHost();
        }
        StringBuilder detail = new StringBuilder(sslReason(error.getPrimaryError()));
        detail.append(" host=").append(host);
        SslCertificate certificate = error.getCertificate();
        if (certificate != null) {
            if (certificate.getIssuedTo() != null) {
                detail.append(" subject=").append(certificate.getIssuedTo().getDName());
            }
            if (certificate.getIssuedBy() != null) {
                detail.append(" issuer=").append(certificate.getIssuedBy().getDName());
            }
            if (certificate.getValidNotBeforeDate() != null) {
                detail.append(" notBefore=").append(certificate.getValidNotBeforeDate());
            }
            if (certificate.getValidNotAfterDate() != null) {
                detail.append(" notAfter=").append(certificate.getValidNotAfterDate());
            }
        }
        return detail.toString();
    }

    private static void installCrashHandler() {
        Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
        if (handler instanceof CrashHook) {
            return;
        }
        Thread.setDefaultUncaughtExceptionHandler(new CrashHook(handler));
    }

    private static final class CrashHook implements Thread.UncaughtExceptionHandler {
        private final Thread.UncaughtExceptionHandler previous;

        CrashHook(Thread.UncaughtExceptionHandler previous) {
            this.previous = previous;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable error) {
            try {
                recordCrash(error);
            } catch (Throwable ignored) {
            }
            if (previous != null) {
                previous.uncaughtException(thread, error);
            }
        }
    }

    private static void recordCrash(Throwable error) {
        if (error == null) {
            return;
        }
        String message = error.getClass().getSimpleName();
        if (error.getMessage() != null && !error.getMessage().isEmpty()) {
            message = message + ": " + error.getMessage();
        }
        step("crash", message, crashStack(error));
        Trace trace;
        synchronized (ActivityLog.class) {
            trace = current;
            if (trace == null || trace.posted) {
                return;
            }
            trace.posted = true;
            current = null;
        }
        dispatch(trace, "crash", "crash", true);
    }

    private static String crashStack(Throwable error) {
        StackTraceElement[] frames = error.getStackTrace();
        int limit = Math.min(frames.length, 8);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(frames[i].toString());
        }
        return builder.toString();
    }

    private static void dispatch(Trace trace, String type, String outcome, boolean sync) {
        if (trace.app == null) {
            return;
        }
        String safeType = sanitizeType(type);
        String safeOutcome = outcome == null || outcome.trim().isEmpty() ? safeType : outcome.trim();
        List<Step> steps;
        synchronized (trace) {
            steps = new ArrayList<>(trace.steps);
        }
        String details = buildDetails(trace.channel, safeOutcome, steps);
        String channel = trace.channel;
        Session session = new Session(trace.referenceNumber, trace.merchantId, trace.country);
        Context app = trace.app;
        if (sync) {
            post(app, safeType, channel, details, session);
            return;
        }
        EXECUTOR.execute(() -> post(app, safeType, channel, details, session));
    }

    private static String sanitizeType(String type) {
        if ("error".equals(type) || "interrupted".equals(type) || "crash".equals(type) || "info".equals(type)) {
            return type;
        }
        return "info";
    }

    private static String normalizeChannel(String channel) {
        if (channel == null) {
            return CHECKOUT;
        }
        String value = channel.trim();
        if (value.isEmpty() || "null".equalsIgnoreCase(value)) {
            return CHECKOUT;
        }
        return value;
    }

    private static String trimField(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= MAX_FIELD) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_FIELD);
    }

    private static boolean same(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    static String buildDetails(String channel, String outcome, List<Step> source) {
        List<Step> steps = new ArrayList<>(source);
        String text = renderDetails(channel, outcome, steps);
        while (text.length() > MAX_DETAILS && steps.size() > 1) {
            steps.remove(0);
            text = renderDetails(channel, outcome, steps);
        }
        if (text.length() > MAX_DETAILS) {
            return text.substring(text.length() - MAX_DETAILS);
        }
        return text;
    }

    private static String renderDetails(String channel, String outcome, List<Step> steps) {
        try {
            JSONArray array = new JSONArray();
            for (Step step : steps) {
                JSONObject item = new JSONObject();
                item.put("step", step.name);
                item.put("at", step.at);
                if (step.request != null && !step.request.isEmpty()) {
                    item.put("request", step.request);
                }
                if (step.response != null && !step.response.isEmpty()) {
                    item.put("response", step.response);
                }
                if (step.count > 1) {
                    item.put("count", step.count);
                }
                array.put(item);
            }
            JSONObject root = new JSONObject();
            root.put("channel", channel == null ? CHECKOUT : channel);
            root.put("outcome", outcome == null ? "" : outcome);
            root.put("steps", array);
            return root.toString();
        } catch (JSONException e) {
            return "{\"channel\":\"" + channel + "\",\"outcome\":\"" + outcome + "\"}";
        }
    }

    private static String sslReason(int code) {
        switch (code) {
            case SslError.SSL_EXPIRED:
                return "cert expired";
            case SslError.SSL_NOTYETVALID:
                return "cert not yet valid";
            case SslError.SSL_IDMISMATCH:
                return "cert hostname mismatch";
            case SslError.SSL_UNTRUSTED:
                return "cert untrusted";
            case SslError.SSL_DATE_INVALID:
                return "cert date invalid";
            case SslError.SSL_INVALID:
                return "cert invalid";
            default:
                return "cert error " + code;
        }
    }

    static String checksum(String secret, String datetime, JSONObject payload) {
        List<Object> items = new ArrayList<>();
        items.add(datetime);
        collectLeaves(payload, items);
        items.sort(ActivityLog::compareAsc);
        StringBuilder joined = new StringBuilder();
        for (Object item : items) {
            if (item != null) {
                joined.append(item);
            }
        }
        return sha512Hex(secret + joined);
    }

    private static void collectLeaves(JSONObject object, List<Object> items) {
        if (object == null) {
            return;
        }
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            Object value = object.opt(keys.next());
            if (value instanceof JSONObject) {
                collectLeaves((JSONObject) value, items);
            } else if (value != null && value != JSONObject.NULL) {
                items.add(value);
            }
        }
    }

    private static int compareAsc(Object a, Object b) {
        boolean aNum = a instanceof Number;
        boolean bNum = b instanceof Number;
        if (aNum && bNum) {
            return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
        }
        if (aNum) {
            return -1;
        }
        if (bNum) {
            return 1;
        }
        Collator collator = Collator.getInstance(Locale.ROOT);
        collator.setStrength(Collator.PRIMARY);
        return collator.compare(String.valueOf(a), String.valueOf(b));
    }

    private static String sha512Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return "";
        }
    }

    static String gatewayDatetime() {
        return LocalDateTime.now(ZoneOffset.UTC).format(DATETIME);
    }

    static JSONObject buildPayload(
            String os,
            String brand,
            String model,
            String modelNo,
            String productVersion,
            String sdkVersion,
            String softwareVersion,
            String networkType,
            String build,
            String type,
            String process,
            String details,
            String datetime,
            String checksum,
            Session session) throws Exception {
        JSONObject deviceInfo = new JSONObject();
        deviceInfo.put("os", os);
        deviceInfo.put("brand", brand);
        deviceInfo.put("model", model);
        deviceInfo.put("modelNo", modelNo);

        JSONObject productInfo = new JSONObject();
        productInfo.put("type", "XDK Android");
        productInfo.put("version", productVersion);
        productInfo.put("sdkVersion", sdkVersion);

        JSONObject data = new JSONObject();
        data.put("deviceInfo", deviceInfo);
        data.put("productInfo", productInfo);

        JSONObject message = new JSONObject();
        message.put("referenceNumber", session.referenceNumber);
        message.put("type", type);
        message.put("process", process);
        message.put("details", details);
        message.put("platform", "Android");
        message.put("softwareVersion", softwareVersion);
        message.put("networkType", networkType);
        message.put("country", session.country);
        message.put("build", build);
        message.put("merchantId", session.merchantId);

        JSONObject root = new JSONObject();
        root.put("data", data);
        root.put("message", message);
        root.put("checksum", checksum);
        root.put("datetime", datetime);
        return root;
    }

    private static final class Session {
        final String referenceNumber;
        final String merchantId;
        final String country;

        Session(String referenceNumber, String merchantId, String country) {
            this.referenceNumber = referenceNumber;
            this.merchantId = merchantId;
            this.country = country;
        }
    }

    private static void post(Context context, String type, String process, String details, Session session) {
        try {
            String datetime = gatewayDatetime();
            String secret = readS();
            JSONObject body = buildPayload(
                    Build.VERSION.RELEASE,
                    Build.MANUFACTURER,
                    Build.MODEL,
                    Build.DEVICE,
                    appVersion(context),
                    BuildConfig.XDKAVersion,
                    Build.DISPLAY,
                    networkType(context),
                    appBuild(context),
                    type,
                    process,
                    details,
                    datetime,
                    "",
                    session);
            JSONObject unsigned = new JSONObject(body.toString());
            unsigned.remove("checksum");
            unsigned.remove("datetime");
            body.put("checksum", checksum(secret, datetime, unsigned));
            String payload = body.toString();

            Request request = new Request.Builder()
                    .url(telemetryUrl)
                    .addHeader("Content-Type", "application/json; charset=UTF-8")
                    .addHeader("Accept", "application/json")
                    .post(RequestBody.create(payload, JSON))
                    .build();
            try (okhttp3.Response response = CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Log.w(TAG, "activity log HTTP " + response.code() + " process=" + process);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "activity log failed process=" + process + ": " + e.getMessage());
        }
    }

    private static String readS() {
        String note = sessionNote;
        if (note != null && !note.isEmpty()) {
            return note;
        }
        return join(TraceParts.P0, TraceParts.P1, TraceParts.P2, TraceParts.P3);
    }

    private static String join(byte[]... parts) {
        int length = 0;
        for (byte[] part : parts) {
            length += part.length;
        }
        byte[] raw = new byte[length];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, raw, offset, part.length);
            offset += part.length;
        }
        char[] chars = new char[raw.length];
        for (int i = 0; i < raw.length; i++) {
            chars[i] = (char) ((raw[i] & 0xff) ^ (MARK[i % MARK.length] & 0xff));
            raw[i] = 0;
        }
        String value = new String(chars);
        java.util.Arrays.fill(chars, '\0');
        return value;
    }

    private static String appVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionName == null ? "" : info.versionName;
        } catch (Exception e) {
            return "";
        }
    }

    private static String appBuild(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= 28) {
                return Long.toString(info.getLongVersionCode());
            }
            return Integer.toString(info.versionCode);
        } catch (Exception e) {
            return "";
        }
    }

    private static String networkType(Context context) {
        try {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager == null) {
                return "none";
            }
            Network network = manager.getActiveNetwork();
            if (network == null) {
                return "none";
            }
            NetworkCapabilities caps = manager.getNetworkCapabilities(network);
            if (caps == null) {
                return "none";
            }
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "wifi";
            }
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return "mobile";
            }
            return "other";
        } catch (Exception e) {
            return "none";
        }
    }
}
