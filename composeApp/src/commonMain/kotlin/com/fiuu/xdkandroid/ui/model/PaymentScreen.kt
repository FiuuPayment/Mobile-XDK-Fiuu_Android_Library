package com.fiuu.xdkandroid.ui.model

sealed class PaymentScreen {
    object Methods : PaymentScreen()
    data class Channels(val item: PayWithItem) : PaymentScreen()
    object ManualCardEntry : PaymentScreen()
    object CardInstalmentEntry : PaymentScreen()
}