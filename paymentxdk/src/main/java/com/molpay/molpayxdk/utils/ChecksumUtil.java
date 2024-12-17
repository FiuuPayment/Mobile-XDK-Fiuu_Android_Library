package com.molpay.molpayxdk.utils;

import android.content.Context;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChecksumUtil {

    public final static String TAG = "Fiuu_XDKA_LOGGER";

    private static ChecksumUtil instance;
    private String secretKey;

    private ChecksumUtil() {
        // Private to prevent instantiation
    }

    public static ChecksumUtil getInstance(Context context) {
        if (instance == null) {
            instance = new ChecksumUtil();
            instance.initSecretKey(context.getApplicationContext()); // Use application context
        }
        return instance;
    }

    private void initSecretKey(Context context) {
        SecurityUtils.initialize(context);
        secretKey = SecurityUtils.getSecretKey();
        Log.d(TAG, "initSecretKey: "+secretKey);
    }

    private String dateTime(){
        return DateTimeUtil.getCurrentDateTimeUTC();
    }

    public String generateCheckSum() {
        String input = secretKey + dateTime();
        Log.d(TAG,"generateCheckSum: "+input);
        return generateSHA512(input);
    }

    private String generateSHA512(String input) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            Log.d(TAG, "Checksum: "+hexString);
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}