// Compile-only stub of the Samsung Pay SDK 2.22.00 API used by composeApp. Never packaged:
// at runtime the host app's real samsungpay jar supplies this class.
package com.samsung.android.sdk.samsungpay.v2.payment;

import android.content.Context;
import android.os.Bundle;
import com.samsung.android.sdk.samsungpay.v2.PartnerInfo;
import com.samsung.android.sdk.samsungpay.v2.SpaySdk;
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.CustomSheet;

public final class PaymentManager extends SpaySdk {
    public PaymentManager(Context context, PartnerInfo partnerInfo) { throw new RuntimeException("Stub!"); }

    public void updateSheet(CustomSheet customSheet) { throw new RuntimeException("Stub!"); }

    public void startInAppPayWithCustomSheet(CustomSheetPaymentInfo paymentInfo, CustomSheetTransactionInfoListener listener) {
        throw new RuntimeException("Stub!");
    }

    public interface CustomSheetTransactionInfoListener {
        void onCardInfoUpdated(CardInfo selectedCardInfo, CustomSheet customSheet);

        void onSuccess(CustomSheetPaymentInfo paymentInfo, String tokenData, Bundle extraPaymentData);

        void onFailure(int errorCode, Bundle errorData);
    }
}
