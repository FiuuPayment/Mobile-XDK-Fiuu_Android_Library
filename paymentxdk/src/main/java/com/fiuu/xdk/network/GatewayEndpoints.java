/*
 * Copyright 2024 Fiuu.
 */

package com.fiuu.xdk.network;

import com.fiuu.xdk.security.NativeSecurity;

/**
 * Strongly-typed endpoint builder replacing hardcoded URL strings across the SDK.
 */
public final class GatewayEndpoints {

    public static final String PATH_GPAY_CANCEL = NativeSecurity.getShieldedPath(0);
    public static final String PATH_GPAY_CREATE_TXN = NativeSecurity.getShieldedPath(1);
    public static final String PATH_GPAY_PAYMENT_V2 = NativeSecurity.getShieldedPath(2);
    public static final String PATH_QUERY_BY_TID = NativeSecurity.getShieldedPath(3);
    public static final String PATH_INTERMEDIATE_LOADING = NativeSecurity.getShieldedPath(4);

    private GatewayEndpoints() {
    }

    public static String getGPayCancelUrl(PaymentEnvironment env) {
        return (env != null ? env.getPaymentBase() : PaymentEnvironment.PRODUCTION.getPaymentBase()) + PATH_GPAY_CANCEL;
    }

    public static String getGPayCreateTxnUrl(PaymentEnvironment env) {
        return (env != null ? env.getPaymentBase() : PaymentEnvironment.PRODUCTION.getPaymentBase()) + PATH_GPAY_CREATE_TXN;
    }

    public static String getGPayPaymentV2Url(PaymentEnvironment env) {
        return (env != null ? env.getPaymentBase() : PaymentEnvironment.PRODUCTION.getPaymentBase()) + PATH_GPAY_PAYMENT_V2;
    }

    public static String getQueryByTidUrl(PaymentEnvironment env) {
        return (env != null ? env.getApiBase() : PaymentEnvironment.PRODUCTION.getApiBase()) + PATH_QUERY_BY_TID;
    }

    public static String getLoadingUrl(PaymentEnvironment env, String tranId) {
        String base = (env != null ? env.getPaymentBase() : PaymentEnvironment.PRODUCTION.getPaymentBase());
        String cleanId = "";
        if (tranId != null) {
            String trimmed = tranId.trim().replaceAll("^\"|\"$", "");
            if (!trimmed.isEmpty() && !"null".equalsIgnoreCase(trimmed) && !"undefined".equalsIgnoreCase(trimmed)) {
                try {
                    cleanId = java.net.URLEncoder.encode(trimmed, "UTF-8");
                } catch (java.io.UnsupportedEncodingException e) {
                    cleanId = trimmed;
                }
            }
        }
        return base + PATH_INTERMEDIATE_LOADING + "?tranID=" + cleanId;
    }
}
