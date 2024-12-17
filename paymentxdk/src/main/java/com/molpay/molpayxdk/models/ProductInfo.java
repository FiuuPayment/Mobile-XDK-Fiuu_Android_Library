package com.molpay.molpayxdk.models;

public class ProductInfo {
    private String type;
    private String version;

    public ProductInfo(String type, String version) {
        this.type = type;
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

}
