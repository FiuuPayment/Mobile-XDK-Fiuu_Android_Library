package com.molpay.molpayxdk.models;

public class LogDetails {

    private LogEntity entity;
    private String details;

    public LogDetails(LogEntity entity,String details) {
        this.entity = entity;
        this.details = details;
    }

    public LogEntity getEntity() {
        return entity;
    }

    public void setEntity(LogEntity entity) {
        this.entity = entity;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

}
