package com.fiuu.xdkandroid.data.mapper

import com.fiuu.xdkandroid.data.remote.dto.PaymentResponseDto
import com.fiuu.xdkandroid.utils.FiuuLogger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Normalizes legacy Webcore JSON response strings into the strongly-typed [PaymentResponseDto]
 * expected by merchant applications and cross-platform wrappers.
 */
object WebcoreResponseMapper {
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun mapJsonToDto(rawJson: String?): PaymentResponseDto {
        if (rawJson.isNullOrBlank()) {
            FiuuLogger.log("WebcoreResponseMapper: rawJson is null or blank - treating as cancelled/dismissed")
            return PaymentResponseDto(
                status = "9999",
                statusCheck = false,
                message = "Payment was cancelled or dismissed"
            )
        }

        return try {
            FiuuLogger.log("WebcoreResponseMapper: mapping rawJson -> $rawJson")
            val element = jsonParser.parseToJsonElement(rawJson).jsonObject

            val errorMsg = element["Error"]?.jsonPrimitive?.contentOrNull
                ?: element["error"]?.jsonPrimitive?.contentOrNull

            val explicitStatusCode = element["status_code"]?.jsonPrimitive?.contentOrNull
                ?: element["status"]?.jsonPrimitive?.contentOrNull

            val explicitErrorCode = element["error_code"]?.jsonPrimitive?.contentOrNull

            val statusCode = when {
                errorMsg != null -> explicitErrorCode ?: "9999"
                explicitStatusCode != null -> explicitStatusCode
                explicitErrorCode != null -> explicitErrorCode
                else -> "9999"
            }

            val message = element["msgdesc"]?.jsonPrimitive?.contentOrNull
                ?: element["message"]?.jsonPrimitive?.contentOrNull
                ?: element["error_desc"]?.jsonPrimitive?.contentOrNull
                ?: errorMsg
                ?: ""

            val isSuccess = errorMsg == null && statusCode == "00"

            PaymentResponseDto(
                status = statusCode,
                statusCheck = isSuccess,
                message = message,
                amount = element["amount"]?.jsonPrimitive?.contentOrNull,
                currency = element["currency"]?.jsonPrimitive?.contentOrNull,
                channel = element["channel"]?.jsonPrimitive?.contentOrNull,
                skey = element["skey"]?.jsonPrimitive?.contentOrNull,
                tranID = element["tranID"]?.jsonPrimitive?.contentOrNull
                    ?: element["transactionId"]?.jsonPrimitive?.contentOrNull,
                referenceNo = element["order_id"]?.jsonPrimitive?.contentOrNull
                    ?: element["referenceNo"]?.jsonPrimitive?.contentOrNull,
                paydate = element["paydate"]?.jsonPrimitive?.contentOrNull,
                appcode = element["appcode"]?.jsonPrimitive?.contentOrNull,
                errorCode = explicitErrorCode ?: if (errorMsg != null) "9999" else null,
                errorDesc = element["error_desc"]?.jsonPrimitive?.contentOrNull ?: errorMsg
            )
        } catch (e: Exception) {
            FiuuLogger.log("WebcoreResponseMapper: failed to parse Webcore response: ${e.message}")
            PaymentResponseDto(
                status = "9999",
                statusCheck = false,
                message = "Failed to parse Webcore response: ${e.message}",
                errorDesc = rawJson
            )
        }
    }
}
