/*
 * Copyright 2024 Fiuu.
 */

package com.fiuu.xdk.utils;

import android.net.Uri;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Security utilities for domain whitelisting, URL validation, and sensitive buffer clearing.
 */
public final class SecurityUtils {

    private static final Set<String> ALLOWED_EXACT_HOSTS = new HashSet<>(Arrays.asList(
            "fiuu.com",
            "www.fiuu.com",
            "xdk.fiuu.com",
            "pay.fiuu.com",
            "api.fiuu.com",
            "sandbox-xdk.fiuu.com",
            "sandbox-payment.fiuu.com",
            "sandbox-api.fiuu.com",
            "uat-xdk.fiuu.com",
            "molpay.com",
            "www.molpay.com"
    ));

    private SecurityUtils() {
    }

    /**
     * Checks whether the given URL points to a trusted Fiuu payment gateway host.
     *
     * @param url The URL string to inspect.
     * @return {@code true} if the URL has a trusted host.
     */
    public static boolean isTrustedGatewayUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        try {
            java.net.URI javaUri = new java.net.URI(url.trim());
            String scheme = javaUri.getScheme();
            String host = javaUri.getHost();
            if (scheme == null || !"https".equalsIgnoreCase(scheme)
                    || host == null || javaUri.getRawUserInfo() != null) {
                return false;
            }
            String lowerHost = host.toLowerCase(Locale.US);
            return ALLOWED_EXACT_HOSTS.contains(lowerHost)
                    || lowerHost.endsWith(".fiuu.com")
                    || lowerHost.endsWith(".molpay.com");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Overwrites sensitive memory buffers with zeroes.
     *
     * @param buffer The byte array to clear.
     */
    public static void wipeBytes(byte[] buffer) {
        if (buffer != null) {
            Arrays.fill(buffer, (byte) 0);
        }
    }

    /**
     * Overwrites sensitive character arrays with zeroes.
     *
     * @param buffer The char array to clear.
     */
    public static void wipeChars(char[] buffer) {
        if (buffer != null) {
            Arrays.fill(buffer, '\0');
        }
    }
}
