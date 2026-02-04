package com.fiuu.xdkandroid.models;

public class Billing {
    private String mp_description = "";
    private String mp_payername ="";
    private String mp_payeremail = "";
    private String mp_payermobile = "";

    public Billing() {}

    public String getDescription() { return mp_description; }
    public String getPayername() { return mp_payername; }
    public String getPayeremail() { return mp_payeremail; }
    public String getPayermobile() { return mp_payermobile; }

    public void   setDescription(String mp_description) { this.mp_description = mp_description; }
    public  void  setPayername(String mp_payername) {  this.mp_payername = mp_payername; }
    public  void  setPayeremail(String mp_payeremail) {this.mp_payeremail = mp_payeremail; }
    public  void  setPayermobile(String mp_payermobile) {  this.mp_payermobile = mp_payermobile; }


}