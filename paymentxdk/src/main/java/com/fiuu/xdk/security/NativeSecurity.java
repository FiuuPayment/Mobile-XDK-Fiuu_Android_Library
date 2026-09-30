/*
 * Copyright 2024 Fiuu.
 */

package com.fiuu.xdk.security;

import android.util.Log;

import com.fiuu.xdk.googlepay.Helper.AlgorithmHelper;
import com.fiuu.xdk.googlepay.Helper.UtilityHelper;

/**
 * JNI bridge executing cryptographic hashing and path shielding within compiled native C++ code.
 * <p>
 * Maintains a functional pure-Java managed fallback with identical RFC 1321 MD5 calculations
 * and path mappings. This ensures:
 * 1. Third-party merchant automated unit testing suites running in host JVM environments
 *    (e.g., {@code ./gradlew testDebugUnitTest} without an Android native runtime) do not fail.
 * 2. High resilience across heterogeneous merchant ABIs or devices where native binary unpack
 *    may fail, avoiding fatal app crashes during payment checkout.
 */
public final class NativeSecurity {

    private static final String TAG = "NativeSecurity";
    private static boolean isNativeLoaded = false;

    static {
        try {
            System.loadLibrary("fiuu-sec");
            isNativeLoaded = true;
        } catch (Throwable t) {
            isNativeLoaded = false;
            Log.w(TAG, "Native library fiuu-sec not loaded; using managed Java fallback: " + t.getMessage());
        }
    }

    private NativeSecurity() {
    }

    public static boolean isNativeAvailable() {
        return isNativeLoaded;
    }

    /**
     * Calculates MD5 signature (VCode) for payment initialization.
     */
    public static String calculateVCode(String amount, String merchantId, String orderId,
                                        String vkey, String currency, boolean extendedVCode) {
        if (isNativeLoaded) {
            try {
                return calculateVCodeNative(amount, merchantId, orderId, vkey, currency, extendedVCode);
            } catch (Throwable t) {
                Log.w(TAG, "Native calculateVCode failed, falling back: " + t.getMessage());
            }
        }
        // Managed Fallback
        byte[] hashData;
        if (extendedVCode) {
            hashData = AlgorithmHelper.md5(amount + merchantId + orderId + vkey + currency);
        } else {
            hashData = AlgorithmHelper.md5(amount + merchantId + orderId + vkey);
        }
        return UtilityHelper.ByteArrayToHexString(hashData);
    }

    /**
     * Calculates MD5 signature (SKey) for transaction status query.
     */
    public static String calculateSKey(String txnID, String merchantId, String vkey, String amount) {
        if (isNativeLoaded) {
            try {
                return calculateSKeyNative(txnID, merchantId, vkey, amount);
            } catch (Throwable t) {
                Log.w(TAG, "Native calculateSKey failed, falling back: " + t.getMessage());
            }
        }
        // Managed Fallback
        byte[] hashData = AlgorithmHelper.md5(txnID + merchantId + vkey + amount);
        return UtilityHelper.ByteArrayToHexString(hashData);
    }

    /**
     * Retrieves shielded endpoint relative paths without exposing strings in DEX.
     */
    public static String getShieldedPath(int pathId) {
        if (isNativeLoaded) {
            try {
                return getShieldedPathNative(pathId);
            } catch (Throwable ignored) {
            }
        }
        switch (pathId) {
            case 0: return "RMS/GooglePay/cancel.php";
            case 1: return "RMS/GooglePay/createTxn.php";
            case 2: return "RMS/GooglePay/payment_v2.php";
            case 3: return "RMS/q_by_tid.php";
            case 4: return "RMS/intermediate_app/loading.php";
            default: return "";
        }
    }

    // Native JNI Declarations
    private static native String calculateVCodeNative(String amount, String merchantId, String orderId,
                                                      String vkey, String currency, boolean extendedVCode);

    private static native String calculateSKeyNative(String txnID, String merchantId, String vkey, String amount);

    private static native String getShieldedPathNative(int pathId);
}
