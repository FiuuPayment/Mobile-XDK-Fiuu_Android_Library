package com.fiuu.xdkandroid.models;

public class Payment {
    private Boolean mp_is_express_mode = false;
    private String mp_channel = "";
    private String mp_amount ="";
    private String mp_country = "";
    private String mp_currency = "";

    public Payment() {}

    public Boolean getIsExpressMode() { return mp_is_express_mode; }
    public String getChannel() { return mp_channel; }
    public String getAmount() { return mp_amount; }
    public String getCountry() { return mp_country; }
    public String getCurrency() { return mp_currency; }

    public void setIsExpressMode(Boolean mp_is_express_mode) { this.mp_is_express_mode = mp_is_express_mode; }
    public void   setChannel(String mp_channel) { this.mp_channel = mp_channel; }
    public  void  setAmount(String mp_amount) { this.mp_amount = mp_amount; }
    public  void  setCountry(String mp_country) { this.mp_country = mp_country; }
    public  void  setCurrency(String mp_currency) { this.mp_currency = mp_currency; }


}