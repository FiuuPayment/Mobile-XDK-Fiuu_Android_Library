package com.samsung.android.sdk.samsungpay.v2;

import android.os.Bundle;

public interface StatusListener {
    void onSuccess(int status, Bundle bundle);

    void onFail(int errorCode, Bundle bundle);
}
