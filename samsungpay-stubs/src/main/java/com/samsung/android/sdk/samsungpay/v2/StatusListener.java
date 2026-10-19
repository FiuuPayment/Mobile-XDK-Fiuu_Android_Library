// Compile-only stub of the Samsung Pay SDK 2.22.00 API used by composeApp. Never packaged:
// at runtime the host app's real samsungpay jar supplies this class.
package com.samsung.android.sdk.samsungpay.v2;

import android.os.Bundle;

public interface StatusListener {
    void onSuccess(int status, Bundle bundle);

    void onFail(int errorCode, Bundle bundle);
}
