package com.molpay.molpayxdk.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.molpay.molpayxdk.models.DeviceInfo;
import com.molpay.molpayxdk.models.LogDetails;
import com.molpay.molpayxdk.models.LogMessage;
import com.molpay.molpayxdk.models.ProductInfo;
import com.molpay.molpayxdk.utils.ChecksumUtil;
import com.molpay.molpayxdk.utils.DateTimeUtil;
import com.molpay.molpayxdk.utils.DeviceInfoUtil;
import com.molpay.molpayxdk.utils.ProductInfoUtil;

import java.net.HttpURLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Logger {

    private static final String TAG = "ToP Wrapper (1.0.7)";
    private final ChecksumUtil checksumUtil;
    private ExecutorService executorService;

    private static DeviceInfo deviceInfo;
    private static ProductInfo productInfo;

    public Logger(Context context) {
        checksumUtil = ChecksumUtil.getInstance(context);
        executorService = Executors.newSingleThreadExecutor();
    }

    public void log(LogDetails data, Context context) {
        String checksum = checksumUtil.generateCheckSum();
        String datetime = DateTimeUtil.getCurrentDateTimeUTC();

        deviceInfo = DeviceInfoUtil.getDeviceInfo(context);
        productInfo = ProductInfoUtil.getProductInfo();

        LogMessage logMessage = new LogMessage(datetime, checksum, data, deviceInfo, productInfo);
        Log.d(TAG,"----LOGGER DETAILS---:\n %s"+ new Gson().toJson(data) + "\n deviceInfo: " + new Gson().toJson(deviceInfo) + "\n productInfo: " + new Gson().toJson(productInfo));
        executorService.execute(() -> sendLog(logMessage));
    }

    private void sendLog(LogMessage logMessage) {
        HttpURLConnection urlConnection = null;
        try {
            // Use LoggerApiClient to build and send the HTTP request
            LoggerApiClient.sendLog(logMessage);

            // Use Handler to post results back to the main thread
            new Handler(Looper.getMainLooper()).post(() -> {
                // If success, log result
                Log.d(TAG, "Log sent successfully!");
            });

        } catch (Exception e) {
            e.printStackTrace();
            new Handler(Looper.getMainLooper()).post(() -> {
                // If failure, log error
                Log.d(TAG, "Failed to send log: " + e.getMessage());
            });
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}