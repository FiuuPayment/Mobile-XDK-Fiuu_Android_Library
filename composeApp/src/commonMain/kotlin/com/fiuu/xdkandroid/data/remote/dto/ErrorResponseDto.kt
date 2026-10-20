package com.fiuu.xdkandroid.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class ErrorResponseDto(
    val code: String? = null,
    val message: String? = null,
    val error: String? = null,
    val statusCode: Int? = null
)

class ApiException(message: String, val statusCode: Int?, val errorCode: String? = null) : Exception(message)
