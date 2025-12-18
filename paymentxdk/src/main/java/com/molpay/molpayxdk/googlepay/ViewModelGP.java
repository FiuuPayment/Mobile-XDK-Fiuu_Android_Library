package com.molpay.molpayxdk.googlepay;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentsClient;

import org.json.JSONObject;

public class ViewModelGP extends AndroidViewModel {

    // A client for interacting with the Google Pay API.
    private final PaymentsClient paymentsClient;

    // LiveData with the result of whether the user can pay using Google Pay
    private final MutableLiveData<Boolean> _canUseGooglePay = new MutableLiveData<>();

    public ViewModelGP(@NonNull Application application) {
        super(application);
        paymentsClient = UtilGP.createPaymentsClient(application);

        fetchCanUseGooglePay();
    }

    public final LiveData<Boolean> canUseGooglePay = _canUseGooglePay;

    /**
     * Determine the user's ability to pay with a payment method supported by your app and display
     * a Google Pay payment button.
     */
    private void fetchCanUseGooglePay() {
        final JSONObject isReadyToPayJson = UtilGP.getIsReadyToPayRequest();
        if (isReadyToPayJson == null) {
            Log.e("logGooglePay" , "_canUseGooglePay isReadyToPayJson == null");
            _canUseGooglePay.setValue(false);
            return;
        }

        // The call to isReadyToPay is asynchronous and returns a Task. We need to provide an
        // OnCompleteListener to be triggered when the result of the call is known.
        IsReadyToPayRequest request = IsReadyToPayRequest.fromJson(isReadyToPayJson.toString());
        Task<Boolean> task = paymentsClient.isReadyToPay(request);
        task.addOnCompleteListener(
                completedTask -> {
                    Log.e("logGooglePay" , "_canUseGooglePay completedTask = " + completedTask.toString());
                    if (completedTask.isSuccessful()) {
                        Log.e("logGooglePay" , "_canUseGooglePay completedTask.isSuccessful");
                        _canUseGooglePay.setValue(completedTask.getResult());
                    } else {
                        Log.e("logGooglePay" , "_canUseGooglePay setValue(false)");
                        Exception exception = completedTask.getException();
                        Log.e("logGooglePay", "Error checking if ready to pay = " + exception);
                        Log.e("logGooglePay", "getCause = " + exception.getCause());
                        Log.e("logGooglePay", "getMessage = " + exception.getMessage());
                        _canUseGooglePay.setValue(false);
                    }
                });
    }

    /**
     * Creates a Task that starts the payment process with the transaction details included.
     *
     * @param priceCents the price to show on the payment sheet.
     * @return a Task with the payment information.
     * )
     */
    public Task<PaymentData> getLoadPaymentDataTask(final String priceCents) {

        Log.e("logGooglePay" , "getLoadPaymentDataTask");

        JSONObject paymentDataRequestJson = UtilGP.getPaymentDataRequest(priceCents);

        if (paymentDataRequestJson == null) {
            return null;
        }

        PaymentDataRequest request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString());

        return paymentsClient.loadPaymentData(request);
    }

}