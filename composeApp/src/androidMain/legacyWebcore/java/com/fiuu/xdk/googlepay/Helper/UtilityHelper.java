/*
 * Copyright 2023 Razer Merchant Services.
 */

package com.fiuu.xdk.googlepay.Helper;

public class UtilityHelper {
    public static String ByteArrayToHexString(byte[] byteArray) {
        StringBuilder result = new StringBuilder();
        for (byte b : byteArray) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
