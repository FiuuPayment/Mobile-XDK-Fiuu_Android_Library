/*
 * Copyright 2023 Razer Merchant Services.
 */

package com.molpay.molpayxdk.googlepay.Helper;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AlgorithmHelper {
    private static final String TAG = "logGooglePay";

    public static byte[] sha256(String data) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            digest.reset();
            byte[] result = digest.digest(data.getBytes());
            return result;
        } catch (Exception exception) {
            exception.printStackTrace();
            return new byte[0];
        }
    }

    public static byte[] sha1(String data) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            byte[] result = digest.digest(data.getBytes());
            return result;
        } catch (Exception exception) {
            exception.printStackTrace();
            return new byte[0];
        }
    }

    public static byte[] md5(String data) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
            digest.reset();
            byte[] result = digest.digest(data.getBytes());
            return result;
        } catch (Exception exception) {
            exception.printStackTrace();
            return new byte[0];
        }
    }

    public static byte[] decryptAES(byte[] data, byte[] key) {
        try {
            byte[] iv = new byte[16];
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
            byte[] result = cipher.doFinal(data);
            return result;
        } catch (Exception exception) {
            return new byte[0];
        }
    }

    public static String encodeBase64(String data) {
        byte[] base64Data = data.getBytes(StandardCharsets.UTF_8);
        String result = Base64.encodeToString(base64Data, Base64.DEFAULT);
        return result;
    }
}
