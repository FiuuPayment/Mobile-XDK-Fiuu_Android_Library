package com.fiuu.xdkandroid.models;

public class Merchant {
    private String mp_username = "";
    private String mp_password ="";
    private String mp_appname = "";
    private String mp_merchantid = "";
    private String mp_verificationKey = "";

    public Merchant() {}

    public String getUsername() { return mp_username; }
    public String getPassword() { return mp_password; }
    public String getAppname() { return mp_appname; }
    public String getMerchantid() { return mp_merchantid; }
    public String getVerificationKey() { return mp_verificationKey; }

    public void   setUsername(String mp_username) { this.mp_username = mp_username; }
    public  void  setPassword(String mp_password) {  this.mp_password = mp_password; }
    public  void  setAppname(String mp_appname) {this.mp_appname = mp_appname; }
    public  void  setMerchantid(String mp_merchantid) {  this.mp_merchantid = mp_merchantid; }
    public  void  setVerificationKey(String mp_verificationKey) {  this.mp_verificationKey = mp_verificationKey; }

}