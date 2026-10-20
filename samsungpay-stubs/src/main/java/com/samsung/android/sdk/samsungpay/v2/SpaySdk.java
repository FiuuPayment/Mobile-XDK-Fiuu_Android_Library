package com.samsung.android.sdk.samsungpay.v2;

import android.content.Context;

public abstract class SpaySdk {
    public static final int ERROR_NONE = 0;
    public static final int ERROR_USER_CANCELED = -7;
    public static final int ERROR_DEVICE_NOT_SAMSUNG = -350;
    public static final int ERROR_SPAY_PKG_NOT_FOUND = -351;
    public static final int ERROR_SPAY_SETUP_NOT_COMPLETED = -356;

    public static final int SPAY_NOT_SUPPORTED = 0;
    public static final int SPAY_NOT_READY = 1;
    public static final int SPAY_READY = 2;

    public static final String PARTNER_SERVICE_TYPE = "PartnerServiceType";

    protected SpaySdk(Context context, PartnerInfo partnerInfo) {
        throw new RuntimeException("Stub!");
    }

    public enum Brand {
        AMERICANEXPRESS, MASTERCARD, VISA, DISCOVER, CHINAUNIONPAY, UNKNOWN_CARD, OCTOPUS, ECI,
        PAGOBANCOMAT, ELO, MADA
    }

    public enum ServiceType {
        INAPP_PAYMENT, APP2APP, WEB_PAYMENT, W3C, MOBILEWEB_PAYMENT, INTERNAL_APK
    }
}
