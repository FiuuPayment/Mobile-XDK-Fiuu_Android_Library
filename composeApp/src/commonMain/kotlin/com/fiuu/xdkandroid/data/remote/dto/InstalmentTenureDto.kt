package com.fiuu.xdkandroid.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// POST /pay/installment/installment-tenure — looks up instalment plans available for a card
// (by PAN + expiry) and amount. Field names/order mirror the reference Postman pre-request
// script's `body` object exactly (see CardInstalmentEntryScreen usage in PaymentViewModel).
@Serializable
data class InstalmentTenureRequestDto(
    @SerialName("mp_merchant_ID")
    val merchantId: String,
    @SerialName("mp_cc_pan")
    val ccPan: String,
    @SerialName("mp_cc_month")
    val ccMonth: String,
    @SerialName("mp_cc_year")
    val ccYear: String,
    @SerialName("mp_amount")
    val amount: String,
    @SerialName("date_time")
    val dateTime: String,
    val signature: String
)

// The ineligible-card error shape, e.g.:
// { "Status": "11", "error_code": "IP110", "error_desc": "Card not eligible for installment plan." }
// errorDesc is populated on failure so the UI can show the real reason instead of a generic
// message. On success (Status == "00"), instlMthList carries the available plans instead — see
// InstalmentTenurePlanDto below.
@Serializable
data class InstalmentTenureResponseDto(
    @SerialName("Status")
    val status: String? = null,
    @SerialName("error_code")
    val errorCode: String? = null,
    @SerialName("error_desc")
    val errorDesc: String? = null,
    @SerialName("MerchantID")
    val merchantId: String? = null,
    @SerialName("Channel")
    val channel: String? = null,
    @SerialName("Issuer")
    val issuer: String? = null,
    @SerialName("InstlMthList")
    val instlMthList: List<InstalmentTenurePlanDto>? = null,
    @SerialName("Signature")
    val signature: String? = null
)

// A single selectable tenure within InstalmentTenureResponseDto.instlMthList. InstlMonth == 0
// represents the non-instalment/normal-purchase option rather than a real plan — callers rendering
// a plan picker (e.g. CardInstalmentEntryScreen) should filter it out.
@Serializable
data class InstalmentTenurePlanDto(
    @SerialName("InstlMonth")
    val instlMonth: Int,
    @SerialName("InstlMinAmt")
    val instlMinAmt: Double = 0.0
)
