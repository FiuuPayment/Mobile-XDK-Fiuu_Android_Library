package com.samsung.android.sdk.samsungpay.v2;

import android.content.Context;

public abstract class SamsungPayBase extends SpaySdk {
    protected SamsungPayBase(Context context, PartnerInfo partnerInfo, String packageName) {
        super(context, partnerInfo);
    }
}
