package com.fiuu.xdkandroid.ui.model

// Drives PaymentInstructionScreen — shown when the /process response comes back with
// requestType == "RESPONSE" and pInstruction == 1 (offline/over-the-counter channels such as
// Cash-711, where the customer must complete payment at a physical counter using this barcode).
data class PaymentInstructionInfo(
    val serviceName: String,
    val logoUrl: String?,
    val barcodeValue: String,
    val barcodeType: String?,
    val payableAmountText: String,
    val dueDateNote: String?
)
