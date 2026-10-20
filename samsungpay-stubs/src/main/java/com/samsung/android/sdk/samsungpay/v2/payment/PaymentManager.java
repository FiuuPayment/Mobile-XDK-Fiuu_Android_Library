package com.samsung.android.sdk.samsungpay.v2.payment;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.samsung.android.sdk.samsungpay.v2.PartnerInfo;
import com.samsung.android.sdk.samsungpay.v2.SpaySdk;
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.CustomSheet;

public final class PaymentManager extends SpaySdk {
    public PaymentManager(Context context, PartnerInfo partnerInfo) {
        super(context, partnerInfo);
    }

    public void updateSheet(CustomSheet customSheet) {
        throw new RuntimeException("Stub!");
    }

    public void startInAppPayWithCustomSheet(
            @NonNull CustomSheetPaymentInfo paymentInfo,
            @NonNull CustomSheetTransactionInfoListener listener) {
        throw new RuntimeException("Stub!");
    }

    public interface CustomSheetTransactionInfoListener {
        void onCardInfoUpdated(CardInfo selectedCardInfo, CustomSheet customSheet);

        void onSuccess(CustomSheetPaymentInfo paymentInfo, String tokenData, Bundle extraPaymentData);

        void onFailure(int errorCode, Bundle errorData);
    }
}
