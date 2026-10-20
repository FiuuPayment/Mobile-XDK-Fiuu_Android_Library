package com.fiuu.xdkandroid.samsungPay

object SamsungPayConfig {
    // Samsung Pay isn't released yet: while false it reports unavailable (tile hidden, direct and
    // express payments return an error), whether or not the host app ships Samsung's jar.
    const val ENABLED = false

    // Register your service at https://developer.samsung.com/samsung-pay
    // Set your own Samsung Pay service ID (sandbox or production) before enabling Samsung Pay.
    const val SERVICE_ID = ""
    const val MERCHANT_NAME = "FIUU"
    const val MERCHANT_ID = "FIUU"

    // Control ID used inside the CustomSheet amount box
    const val AMOUNT_CONTROL_ID = "amount"
}
