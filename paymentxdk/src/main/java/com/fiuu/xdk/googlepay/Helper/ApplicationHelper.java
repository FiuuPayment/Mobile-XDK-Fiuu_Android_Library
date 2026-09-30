/*
 * Copyright 2023 Razer Merchant Services.
 */

package com.fiuu.xdk.googlepay.Helper;


import com.fiuu.xdk.security.NativeSecurity;

public class ApplicationHelper {
    private static ApplicationHelper single_instance = null;

    protected ApplicationHelper() {
    }

    public static ApplicationHelper getInstance() {
        if (single_instance == null) {
            single_instance = new ApplicationHelper();
        }
        return single_instance;
    }

    public String GetVCode(String amount, String merchantID, String orderId, String verifyKey, String currency, boolean extendedVCode) {
        return NativeSecurity.calculateVCode(amount, merchantID, orderId, verifyKey, currency, extendedVCode);
    }

    public String GetSKey(String txnID, String merchantID, String verifyKey, String amount) {
        return NativeSecurity.calculateSKey(txnID, merchantID, verifyKey, amount);
    }
}
