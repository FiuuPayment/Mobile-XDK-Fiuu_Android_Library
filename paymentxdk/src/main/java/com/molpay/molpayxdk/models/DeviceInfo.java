package com.molpay.molpayxdk.models;

public class DeviceInfo {
    private String platform;
    private String os;
    private String brand;
    private String model;
    private String modelNo;
    private int sdkVersion;
    private String carrierName;

    public DeviceInfo(String platform, String os, String brand, String model, String modelNo, int sdkVersion, String carrierName) {
        this.platform = platform;
        this.os = os;
        this.brand = brand;
        this.model = model;
        this.modelNo = modelNo;
        this.sdkVersion = sdkVersion;
        this.carrierName = carrierName;
    }

    public String getBrand() {
        return brand;
    }

    public String getPlatform() { return platform; }

    public String getModel() {
        return model;
    }

    public String getModelNo() {
        return modelNo;
    }

    public String getOs() {
        return os;
    }

    public int getSdkVersion() {
        return sdkVersion;
    }

    public String getCarrierName() {
        return carrierName;
    }

}
