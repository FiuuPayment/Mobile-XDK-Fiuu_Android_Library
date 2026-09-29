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

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Posts payment-process traces to the mobile activity log.
 * The URL defaults to {@link #DEFAULT_TELEMETRY_URL} and can be overridden
 * the same way {@code TapSDKConfig.telemetryUrl} overrides it in the tap SDK.
 */
public final class ActivityLog {

    public static final String DEFAULT_TELEMETRY_URL = "https://mobile.fiuu.com/api/log/activity";
    private static final String TAG = "ActivityLog";
    private static final MediaType JSON = MediaType.get("application/json; charset=UTF-8");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int MAX_BREADCRUMBS = 40;
    private static final int MAX_DETAILS = 4000;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final List<String> BREADCRUMBS = new ArrayList<>();
    private static final Set<String> SENT = new HashSet<>();
    private static final OkHttpClient CLIENT = new OkHttpClient();

    private static volatile String telemetryUrl = DEFAULT_TELEMETRY_URL;
    private static final byte[] MARK = {(byte) 0xa3, (byte) 0x17, (byte) 0x5c, (byte) 0xe1, (byte) 0x2b, (byte) 0x90, (byte) 0x44, (byte) 0x6f};
    private static volatile String sessionNote;

    private static volatile Session session = new Session("","","");

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

    public static void bindSession(String reference, String merchant, String countryCode) {
        session = new Session(
                reference == null ? "" : reference,
                merchant == null ? "" : merchant,
                countryCode == null ? "" : countryCode);
        synchronized (BREADCRUMBS) {
            BREADCRUMBS.clear();
        }
        synchronized (SENT) {
            SENT.clear();
        }
    }

    public static void event(Context context, String process, String details) {
        send(context, "info", process, details);
    }

    public static void error(Context context, String process, String details) {
        send(context, "error", process, details);
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

    private static void send(Context context, String type, String process, String details) {
        if (context == null) {
            return;
        }
        Context app = context.getApplicationContext();
        String safeType = type == null ? "" : type;
        String safeProcess = process == null ? "" : process;
        String safeDetails = details == null ? "" : details;
        String key = safeType + "\n" + safeProcess + "\n" + safeDetails;
        synchronized (SENT) {
            if (!SENT.add(key)) {
                return;
            }
        }
        note(safeProcess, safeDetails);
        String traced = withBreadcrumbs(safeDetails);
        Session queuedSession = session;
        EXECUTOR.execute(() -> post(app, safeType, safeProcess, traced, key, queuedSession));
    }

    private static void note(String process, String details) {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        String line = "[" + stamp + "] [" + process + "] " + details;
        synchronized (BREADCRUMBS) {
            BREADCRUMBS.add(line);
            if (BREADCRUMBS.size() > MAX_BREADCRUMBS) {
                BREADCRUMBS.remove(0);
            }
        }
    }

    private static String withBreadcrumbs(String details) {
        StringBuilder body = new StringBuilder(details);
        body.append("\n--- Breadcrumbs ---");
        synchronized (BREADCRUMBS) {
            for (String line : BREADCRUMBS) {
                body.append('\n').append(line);
            }
        }
        if (body.length() > MAX_DETAILS) {
            return body.substring(body.length() - MAX_DETAILS);
        }
        return body.toString();
    }

    private static void post(Context context, String type, String process, String details,
                             String sentKey, Session session) {
        try {
            String datetime = gatewayDatetime();
            String s = readS();
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
            body.put("checksum", checksum(s, datetime, unsigned));
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
                    forget(sentKey);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "activity log failed process=" + process + ": " + e.getMessage());
            forget(sentKey);
        }
    }

    private static void forget(String sentKey) {
        synchronized (SENT) {
            SENT.remove(sentKey);
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
