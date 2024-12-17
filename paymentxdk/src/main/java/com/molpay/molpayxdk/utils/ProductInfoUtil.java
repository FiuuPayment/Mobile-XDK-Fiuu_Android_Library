package com.molpay.molpayxdk.utils;

import com.molpay.molpayxdk.BuildConfig;
import com.molpay.molpayxdk.models.ProductInfo;

public class ProductInfoUtil {

    public static ProductInfo getProductInfo() {
        String type = "XDKA";
        String version = BuildConfig.XDKAVersion;

        return new ProductInfo(type,version);
    }
}
