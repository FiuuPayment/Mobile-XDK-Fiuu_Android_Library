package com.molpay.molpayxdk.utils;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SecurityUtils {

    private static Properties properties;
    private static Context context;

    public static void initialize(Context ctx) {
        context = ctx;
        loadProperties();
    }

    private static void loadProperties() {
        try {
            InputStream inputStream = context.getAssets().open("secret.properties");
            properties = new Properties();
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load properties", e);
        }
    }

    public static String getSecretKey() {
        if (properties == null) {
            throw new IllegalStateException("SecretUtil not initialized");
        }
        return properties.getProperty("SECRET_KEY");
    }
}
