/*
 * Copyright 2024 Fiuu.
 */

package com.fiuu.xdk.network;

import java.util.Map;

/**
 * Single source of truth for payment gateway environments and base URLs.
 */
public enum PaymentEnvironment {
    PRODUCTION(
            "https://xdk.fiuu.com/",
            "https://pay.fiuu.com/",
            "https://api.fiuu.com/"
    ),
    SANDBOX(
            "https://sandbox-xdk.fiuu.com/",
            "https://sandbox-payment.fiuu.com/",
            "https://sandbox-api.fiuu.com/"
    ),
    UAT(
            "https://uat-xdk.fiuu.com/",
            "https://pay.fiuu.com/",
            "https://api.fiuu.com/"
    ),
    RMS_FALLBACK(
            "https://pay.fiuu.com/RMS/API/xdk/",
            "https://pay.fiuu.com/",
            "https://api.fiuu.com/"
    );

    private final String webUiBase;
    private final String paymentBase;
    private final String apiBase;

    PaymentEnvironment(String webUiBase, String paymentBase, String apiBase) {
        this.webUiBase = webUiBase;
        this.paymentBase = paymentBase;
        this.apiBase = apiBase;
    }

    public String getWebUiBase() {
        return webUiBase;
    }

    public String getPaymentBase() {
        return paymentBase;
    }

    public String getApiBase() {
        return apiBase;
    }

    /**
     * Resolve environment from payment details bundle consolidating
     * {@code mp_core_env} and {@code mp_sandbox_mode}.
     *
     * @param details HashMap containing merchant payment configuration.
     * @return Resolved {@link PaymentEnvironment}.
     */
    public static PaymentEnvironment resolve(Map<String, ?> details) {
        if (details == null) {
            return PRODUCTION;
        }

        Object coreEnv = details.get("mp_core_env");
        String core = coreEnv == null ? "" : String.valueOf(coreEnv).trim();
        if (!core.isEmpty()) {
            if ("4".equals(core)) {
                return SANDBOX;
            }
            if ("1".equals(core)) {
                return RMS_FALLBACK;
            }
            if ("3".equals(core)) {
                return UAT;
            }
            return PRODUCTION;
        }

        Object sandbox = details.get("mp_sandbox_mode");
        if (Boolean.parseBoolean(String.valueOf(sandbox))) {
            return SANDBOX;
        }
        return PRODUCTION;
    }
}
