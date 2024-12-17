package com.molpay.molpayxdk.models;

public class LogMessage {

    private String datetime;
    private String checksum;
    private LogDetails data;
    private DeviceInfo deviceInfo;
    private ProductInfo productInfo;

    public LogMessage(String datetime, String checksum, LogDetails data, DeviceInfo deviceInfo, ProductInfo productInfo) {
        this.datetime = datetime;
        this.checksum = checksum;
        this.data = data;
        this.deviceInfo = deviceInfo;
        this.productInfo = productInfo;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public LogDetails getData() {
        return data;
    }

    public void setData(LogDetails data) {
        this.data = data;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(DeviceInfo deviceInfo) { this.deviceInfo = deviceInfo; }

    public ProductInfo getProductInfo() {
        return productInfo;
    }

    public void setProductInfo(ProductInfo productInfo) { this.productInfo = productInfo; }
}
