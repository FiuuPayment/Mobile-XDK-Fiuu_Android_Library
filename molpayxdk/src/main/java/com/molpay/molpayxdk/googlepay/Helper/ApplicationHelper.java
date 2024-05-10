/*
 * Copyright 2023 Razer Merchant Services.
 */

package com.molpay.molpayxdk.googlepay.Helper;


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

    public String GetVCode(String amount, String merchantID, String orderId, String verifyKey) {
        byte[] hashData = AlgorithmHelper.md5(amount + merchantID + orderId + verifyKey);

        return String.format("%s", UtilityHelper.ByteArrayToHexString(hashData));
    }

    public String GetSKey(String txnID, String merchantID, String verifyKey, String amount) {
        byte[] hashData = AlgorithmHelper.md5(txnID + merchantID + verifyKey + amount);

        return String.format("%s", UtilityHelper.ByteArrayToHexString(hashData));
    }
}
