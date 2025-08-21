package com.molpay.molpayxdk.utils;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.molpay.molpayxdk.models.DeviceInfo;

public class DeviceInfoUtil {

    public static DeviceInfo getDeviceInfo(Context context) {
        String platform = "Android";
        String os = Build.VERSION.RELEASE;
        String brand = Build.MANUFACTURER;
        String model = Build.MODEL;
        String modelNo = Build.DEVICE;

        int sdkVersion = Build.VERSION.SDK_INT;

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String carrierName = telephonyManager.getNetworkOperatorName();

        return new DeviceInfo(platform,os, brand,model,modelNo, sdkVersion, carrierName);
    }
}
