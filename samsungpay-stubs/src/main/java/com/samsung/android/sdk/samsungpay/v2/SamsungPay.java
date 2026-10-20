package com.samsung.android.sdk.samsungpay.v2;

import android.content.Context;

public final class SamsungPay extends SamsungPayBase {
    public SamsungPay(Context context, PartnerInfo partnerInfo) {
        super(context, partnerInfo, null);
    }

    public void getSamsungPayStatus(StatusListener listener) {
        throw new RuntimeException("Stub!");
    }
}
