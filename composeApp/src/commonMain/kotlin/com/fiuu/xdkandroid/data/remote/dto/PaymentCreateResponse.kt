package com.fiuu.xdkandroid.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class PaymentCreateResponse(
    @SerialName("amount") val amount: String? = null,
    @SerialName("currency") val currency: String? = null,
    @SerialName("referenceNo") val referenceNo: String? = null,
    @SerialName("requestType") val requestType: String? = null,
    @SerialName("requestMethod") val requestMethod: String? = null,
    @SerialName("requestURL") val requestURL: String? = null,
    @SerialName("urlType") val urlType: String? = null,
    // Gateway-specific form fields (e.g. FPX's fpx_* fields) that must be submitted as the POST
    // body when requestMethod == "POST" and urlType == "url" — see AndroidWebViewBridge/
    // IosWebViewBridge openPostUrl. Typed as JsonElement, not Map<String, String>, because the
    // backend sends "" (an empty string, not {}) when there's no gateway data — see
    // PaymentViewModel's requestDataAsStringMap().
    @SerialName("requestData") val requestData: JsonElement? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("error") val error: String? = null,

    @SerialName("transactionId") val transactionId: String? = null,
    @SerialName("statusCode") val statusCode: Int? = null,

    @SerialName("skey") val skey: String? = null,
    @SerialName("paydate") val paydate: String? = null,
    @SerialName("appcode") val appcode: String? = null,
    @SerialName("channel") val channel: String? = null,
    @SerialName("xdkHTMLRedirection") val xdkHTMLRedirection: String? = null,
    @SerialName("tranID") val tranID: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("error_code") val errorCode: String? = null,
    @SerialName("error_desc") val errorDesc: String? = null,

    // Offline/over-the-counter payment instruction fields (e.g. Cash-711) — present when
    // requestType == "RESPONSE" and pInstruction == 1, meaning the customer must complete payment
    // out-of-band (at a physical counter) using the barcode/reference below rather than through a
    // WebView redirect. See PaymentViewModel's startPayment() RESPONSE branch.
    @SerialName("pInstruction") val pInstruction: Int? = null,
    @SerialName("payTxnID") val payTxnID: String? = null,
    @SerialName("order_channel") val orderChannel: String? = null,
    @SerialName("order_currency") val orderCurrency: String? = null,
    @SerialName("order_barcode_type") val orderBarcodeType: String? = null,
    @SerialName("order_txn_amt") val orderTxnAmt: String? = null,
    @SerialName("order_txn_due_datetime") val orderTxnDueDatetime: String? = null
)
