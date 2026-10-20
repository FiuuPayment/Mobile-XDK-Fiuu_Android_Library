package com.samsung.android.sdk.samsungpay.v2.payment;

import androidx.annotation.NonNull;
import com.samsung.android.sdk.samsungpay.v2.SpaySdk;
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.CustomSheet;
import java.util.List;

public class CustomSheetPaymentInfo {
    private CustomSheetPaymentInfo() {
        throw new RuntimeException("Stub!");
    }

    public enum AddressInPaymentSheet {
        DO_NOT_SHOW, NEED_BILLING_SPAY, NEED_SHIPPING_SPAY, SEND_SHIPPING, NEED_BILLING_SEND_SHIPPING,
        NEED_BILLING_AND_SHIPPING
    }

    public static class Builder {
        public Builder() {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public Builder setMerchantId(String merchantId) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public Builder setMerchantName(String merchantName) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public Builder setOrderNumber(String orderNumber) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public Builder setAddressInPaymentSheet(AddressInPaymentSheet addressInPaymentSheet) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public Builder setAllowedCardBrands(List<SpaySdk.Brand> brands) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public Builder setCustomSheet(CustomSheet customSheet) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public CustomSheetPaymentInfo build() {
            throw new RuntimeException("Stub!");
        }
    }
}
