package com.fiuu.xdkandroid.utils

// How long a payment session lasts: FiuuPaymentSDK's on-screen countdown, and how long /subscribe
// may stay open (including the silence before the result, since the backend sends nothing until
// then). Must be a whole number of minutes — the 408 result's message states it in minutes.
internal const val SESSION_TIMEOUT_SECONDS = 300
