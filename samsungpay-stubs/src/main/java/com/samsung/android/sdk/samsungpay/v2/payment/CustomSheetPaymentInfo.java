// Compile-only stub of the Samsung Pay SDK 2.22.00 API used by composeApp. Never packaged:
// at runtime the host app's real samsungpay jar supplies this class.
package com.samsung.android.sdk.samsungpay.v2.payment;

import com.samsung.android.sdk.samsungpay.v2.SpaySdk;
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.CustomSheet;
import java.util.List;

public class CustomSheetPaymentInfo {
    private CustomSheetPaymentInfo() { throw new RuntimeException("Stub!"); }

    public enum AddressInPaymentSheet { DO_NOT_SHOW }

    public static class Builder {
        public Builder() { throw new RuntimeException("Stub!"); }

        public Builder setMerchantId(String merchantId) { throw new RuntimeException("Stub!"); }

        public Builder setMerchantName(String merchantName) { throw new RuntimeException("Stub!"); }

        public Builder setOrderNumber(String orderNumber) { throw new RuntimeException("Stub!"); }

        public Builder setAddressInPaymentSheet(AddressInPaymentSheet addressInPaymentSheet) { throw new RuntimeException("Stub!"); }

        public Builder setAllowedCardBrands(List<SpaySdk.Brand> brands) { throw new RuntimeException("Stub!"); }

        public Builder setCustomSheet(CustomSheet customSheet) { throw new RuntimeException("Stub!"); }

        public CustomSheetPaymentInfo build() { throw new RuntimeException("Stub!"); }
    }
}
