package com.fiuu.xdkandroid.data.remote

class PaymentProcessException(
    val code: String?,
    val apiMessage: String
) : Exception("[$code] $apiMessage")
