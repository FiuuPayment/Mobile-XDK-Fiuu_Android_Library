package com.fiuu.xdkandroid;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fiuu.xdkandroid.models.Billing;
import com.fiuu.xdkandroid.models.Merchant;
import com.fiuu.xdkandroid.models.Payment;

import java.util.HashMap;

public class SharedViewModel extends ViewModel {
    private final  MutableLiveData<Billing> billingData = new MutableLiveData<>();
    private final  MutableLiveData<Merchant> merchantData = new MutableLiveData<>();
    private final  MutableLiveData<Payment> paymentData = new MutableLiveData<>();
    private final MutableLiveData<String> transactionResult = new MutableLiveData<>();

    public LiveData<Billing> getBillingData() { return billingData; }
    public LiveData<Merchant> getMerchantData() { return merchantData; }
    public LiveData<Payment> getPaymentData() { return paymentData; }
    public LiveData<String> getTransactionResult() { return transactionResult; }

    public void setBillingData(Billing data) { billingData.setValue(data); }
    public void setMerchantData(Merchant data) { merchantData.setValue(data); }
    public void setPaymentData(Payment data) { paymentData.setValue(data); }
    public void setTransactionResult(String value) { transactionResult.setValue(value); }

}