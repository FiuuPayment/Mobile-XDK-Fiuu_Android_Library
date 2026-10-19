package com.fiuu.xdkandroid.data.remote

import com.fiuu.xdkandroid.data.mapper.toDomain
import com.fiuu.xdkandroid.data.remote.dto.ApiException
import com.fiuu.xdkandroid.data.remote.dto.CardTokenContainerDto
import com.fiuu.xdkandroid.data.remote.dto.CardTokensRequestDto
import com.fiuu.xdkandroid.data.remote.dto.ErrorResponseDto
import com.fiuu.xdkandroid.data.remote.dto.GetAvailableChannelResponseDto
import com.fiuu.xdkandroid.data.remote.dto.InstalmentTenureRequestDto
import com.fiuu.xdkandroid.data.remote.dto.InstalmentTenureResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentExpressRequestDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentExpressResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentCreateResponse
import com.fiuu.xdkandroid.data.remote.dto.PaymentInitRequestDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentInitResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentInquireRequestDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentInquireResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentResponseDto
import com.fiuu.xdkandroid.data.remote.dto.PaymentProcessRequestDto
import com.fiuu.xdkandroid.data.remote.dto.TokenRemoveRequestDto
import com.fiuu.xdkandroid.domain.entity.PaymentInitResponse
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.plugins.timeout
import io.ktor.client.request.prepareGet
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.util.encodeBase64
import io.ktor.utils.io.readUTF8Line
import com.fiuu.xdkandroid.utils.FiuuLogger
import com.fiuu.xdkandroid.utils.SESSION_TIMEOUT_SECONDS
import com.fiuu.xdkandroid.utils.redactSensitiveJson
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.TimeSource

class KtorApiService(
    httpClientFactory: HttpClientFactory,
    // Fail-safe default: any call site that doesn't explicitly pass this (there shouldn't be
    // any left) lands on sandbox rather than silently hitting production.
    private val buildMode: Int = 4
) : ApiService {

    private val httpClient = httpClientFactory.create(buildMode)
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    // Used only for /process body so that null optional fields (e.g. xp_applepay) are omitted.
    private val processJson = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val subscribePath = "subscribe"

    private companion object {
        const val INQUIRE_REQUEST_TIMEOUT_MS = 15_000L
    }

    override suspend fun initPayment(
        request: PaymentInitRequestDto,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): PaymentInitResponse {
        val jsonBody = try {
            json.encodeToString(PaymentInitRequestDto.serializer(), request)
        } catch (e: Exception) {
            "Error serializing: ${e.message}"
        }
        
        FiuuLogger.log("--- Request Debug Info (Init) ---")
        FiuuLogger.log("Headers:")
        FiuuLogger.log("  x-nonce: $nonce")
        FiuuLogger.log("  x-idempotency-key: $idempotencyKey")
        FiuuLogger.log("  x-timestamp: $timestamp")
        FiuuLogger.log("  x-signature: <redacted>")
        FiuuLogger.log("PAYMENT INIT REQUEST:\n${redactSensitiveJson(jsonBody)}")
        FiuuLogger.log("---------------------------")

        com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.bufferOutboundRequest(
            process = com.fiuu.xdkandroid.telemetry.model.TelemetryProcess.INITIATE_REQUEST,
            details = jsonBody,
            referenceNumber = request.orderId ?: "",
            merchantId = request.merchantId ?: "",
            countryCode = request.country ?: "MY"
        )

        val httpResponse: HttpResponse = httpClient.post("initiate") {
            contentType(ContentType.Application.Json)
            header("x-nonce", nonce)
            header("x-idempotency-key", idempotencyKey)
            header("x-timestamp", timestamp)
            header("x-signature", signature)
            setBody(jsonBody)
        }

        FiuuLogger.log("Full URL called: ${httpResponse.request.url}")
        return when (httpResponse.status) {
            HttpStatusCode.OK, HttpStatusCode.Created -> {
                val responseText = httpResponse.bodyAsText()
                FiuuLogger.log("PAYMENT INIT RESPONSE:\n${redactSensitiveJson(responseText)}")
                com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.logHostApproval(
                    process = com.fiuu.xdkandroid.telemetry.model.TelemetryProcess.INITIATE_HOST_RESPONSE,
                    statusCode = "00"
                )
                val responseDto = json.decodeFromString<PaymentInitResponseDto>(responseText)
                responseDto.toDomain()
            }
            else -> {
                val errorText = httpResponse.bodyAsText()
                FiuuLogger.log("PAYMENT CREATE RESPONSE ERROR:\n $errorText")
                val errorDto = try {
                    json.decodeFromString<ErrorResponseDto>(errorText)
                } catch (e: Exception) {
                    null
                }
                com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.logHostDecline(
                    process = com.fiuu.xdkandroid.telemetry.model.TelemetryProcess.INITIATE_HOST_RESPONSE,
                    statusCode = (errorDto?.statusCode ?: httpResponse.status.value).toString(),
                    statusMessage = errorDto?.message ?: "Error: ${httpResponse.status.value}",
                    source = com.fiuu.xdkandroid.telemetry.model.TelemetryOrigin.GATEWAY_REJECTION,
                    referenceNumber = request.orderId ?: ""
                )
                throw ApiException(
                    message = errorDto?.message ?: "Error: ${httpResponse.status.value}",
                    statusCode = errorDto?.statusCode ?: httpResponse.status.value,
                    errorCode = errorDto?.code
                )
            }
        }
    }

    override suspend fun createPayment(
        token: String,
        request: PaymentProcessRequestDto,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): PaymentCreateResponse {
        val jsonBody = try {
            json.encodeToString(PaymentProcessRequestDto.serializer(), request)
        } catch (e: Exception) {
            "Error serializing: ${e.message}"
        }

        FiuuLogger.log("--- Request Debug Info (Create Payment) ---")
        FiuuLogger.log("Authorization: Bearer <redacted>")
        FiuuLogger.log("Headers:")
        FiuuLogger.log("  x-nonce: $nonce")
        FiuuLogger.log("  x-idempotency-key: $idempotencyKey")
        FiuuLogger.log("  x-timestamp: $timestamp")
        FiuuLogger.log("  x-signature: <redacted>")
        FiuuLogger.log("PAYMENT CREATE REQUEST:\n${redactSensitiveJson(jsonBody)}")
        FiuuLogger.log("---------------------------")

        com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.bufferOutboundRequest(
            process = com.fiuu.xdkandroid.telemetry.model.TelemetryProcess.CREATE_PAYMENT_REQUEST,
            details = jsonBody
        )

        val httpResponse: HttpResponse = httpClient.post("process") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            header("x-nonce", nonce)
            header("x-idempotency-key", idempotencyKey)
            header("x-timestamp", timestamp)
            header("x-signature", signature)
            setBody(jsonBody)
        }

        FiuuLogger.log("Full URL called: ${httpResponse.request.url}")

        return when (httpResponse.status) {
            HttpStatusCode.OK, HttpStatusCode.Created -> {
                val rawJson = httpResponse.bodyAsText()
                FiuuLogger.log("DEBUG createPayment response JSON: ${redactSensitiveJson(rawJson)}")
                com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.logHostApproval(
                    process = com.fiuu.xdkandroid.telemetry.model.TelemetryProcess.CREATE_PAYMENT_HOST_RESPONSE,
                    statusCode = "00"
                )
                json.decodeFromString<PaymentCreateResponse>(rawJson)
            }
            else -> {
                val errorText = httpResponse.bodyAsText()
                FiuuLogger.log("PAYMENT CREATE RESPONSE:\n $errorText")
                val errorDto = try {
                    json.decodeFromString<ErrorResponseDto>(errorText)
                } catch (e: Exception) {
                    null
                }
                com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.logHostDecline(
                    process = com.fiuu.xdkandroid.telemetry.model.TelemetryProcess.CREATE_PAYMENT_HOST_RESPONSE,
                    statusCode = (errorDto?.statusCode ?: httpResponse.status.value).toString(),
                    statusMessage = errorDto?.message ?: errorDto?.error ?: "Error: ${httpResponse.status.value}",
                    source = com.fiuu.xdkandroid.telemetry.model.TelemetryOrigin.GATEWAY_REJECTION
                )
                throw ApiException(
                    message = errorDto?.message ?: errorDto?.error ?: "Error: ${httpResponse.status.value}",
                    statusCode = errorDto?.statusCode ?: httpResponse.status.value,
                    errorCode = errorDto?.code
                )
            }
        }
    }

    override suspend fun expressPayment(
        request: PaymentExpressRequestDto,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): PaymentExpressResponseDto {
        val jsonBody = json.encodeToString(PaymentExpressRequestDto.serializer(), request)
        FiuuLogger.log("--- Express API ---")
        FiuuLogger.log("EXPRESS REQUEST:\n${redactSensitiveJson(jsonBody)}")

        com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.bufferOutboundRequest(
            process = com.fiuu.xdkandroid.telemetry.model.TelemetryProcess.EXPRESS_REQUEST,
            details = jsonBody,
            referenceNumber = request.orderId ?: "",
            merchantId = request.merchantId ?: "",
            countryCode = request.country ?: "MY"
        )

        val httpResponse: HttpResponse = httpClient.post("express") {
            contentType(ContentType.Application.Json)
            header("x-nonce", nonce)
            header("x-idempotency-key", idempotencyKey)
            header("x-timestamp", timestamp)
            header("x-signature", signature)
            setBody(jsonBody)
        }

        val responseText = httpResponse.bodyAsText()
        FiuuLogger.log("Express API Status: ${httpResponse.status}")
        FiuuLogger.log("Express API Response: ${redactSensitiveJson(responseText)}")

        return when (httpResponse.status) {
            HttpStatusCode.OK, HttpStatusCode.Created -> {
                com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.logHostApproval(
                    process = com.fiuu.xdkandroid.telemetry.model.TelemetryProcess.EXPRESS_HOST_RESPONSE,
                    statusCode = "00"
                )
                try {
                    json.decodeFromString<PaymentExpressResponseDto>(responseText)
                } catch (e: Exception) {
                    throw Exception("Express API returned unexpected response: $responseText")
                }
            }
            else -> {
                val errorDto = try {
                    json.decodeFromString<ErrorResponseDto>(responseText)
                } catch (e: Exception) {
                    null
                }
                com.fiuu.xdkandroid.telemetry.FiuuTelemetryLogger.logHostDecline(
                    process = com.fiuu.xdkandroid.telemetry.model.TelemetryProcess.EXPRESS_HOST_RESPONSE,
                    statusCode = (errorDto?.statusCode ?: httpResponse.status.value).toString(),
                    statusMessage = errorDto?.message ?: errorDto?.error ?: "Error: ${httpResponse.status.value}",
                    source = com.fiuu.xdkandroid.telemetry.model.TelemetryOrigin.GATEWAY_REJECTION,
                    referenceNumber = request.orderId ?: ""
                )
                throw ApiException(
                    message = errorDto?.message ?: errorDto?.error ?: "Error: ${httpResponse.status.value}",
                    statusCode = errorDto?.statusCode ?: httpResponse.status.value,
                    errorCode = errorDto?.code
                )
            }
        }
    }

    override suspend fun inquirePayment(
        token: String,
        request: PaymentInquireRequestDto,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): PaymentInquireResponseDto {
        val jsonBody = json.encodeToString(PaymentInquireRequestDto.serializer(), request)
        FiuuLogger.log("--- Inquire API ---")
        FiuuLogger.log("Authorization: Bearer <redacted>")
        FiuuLogger.log("INQUIRE REQUEST:\n$jsonBody")
        FiuuLogger.log("Full URL called: ${ApiConfig.baseUrl(buildMode)}inquire")

        val httpResponse: HttpResponse = httpClient.post("inquire") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            header("x-nonce", nonce)
            header("x-idempotency-key", idempotencyKey)
            header("x-timestamp", timestamp)
            header("x-signature", signature)
            setBody(jsonBody)
            // Every /inquire caller has a fallback (stream result or USER_CANCELLED), so a hung
            // lookup shouldn't hold the "Closing..." overlay for the client-wide 60s.
            timeout { requestTimeoutMillis = INQUIRE_REQUEST_TIMEOUT_MS }
        }

        val responseText = httpResponse.bodyAsText()
        FiuuLogger.log("Inquire API Response:\n$responseText")

        return when (httpResponse.status) {
            HttpStatusCode.OK, HttpStatusCode.Created -> {
                try {
                    json.decodeFromString<PaymentInquireResponseDto>(responseText)
                } catch (e: Exception) {
                    throw Exception("Inquire API returned unexpected response: $responseText")
                }
            }
            else -> {
                val errorDto = try {
                    json.decodeFromString<ErrorResponseDto>(responseText)
                } catch (e: Exception) {
                    null
                }
                throw ApiException(
                    message = errorDto?.message ?: errorDto?.error ?: "Error: ${httpResponse.status.value}",
                    statusCode = errorDto?.statusCode ?: httpResponse.status.value,
                    errorCode = errorDto?.code
                )
            }
        }
    }

    // Legacy MOLPay endpoint: different host (ApiConfig.channelUrl(buildMode) is absolute, so
    // it overrides the defaultRequest base URL rather than resolving against it), form-urlencoded
    // body instead of JSON, and HTTP Basic auth (mp_username/mp_password) instead of the
    // x-signature/Bearer scheme used everywhere else. The body is exactly 2 fields per the
    // Postman collection — "Checksum" (capital C) and "Params", the latter itself a JSON string
    // of {merchantID, currency} rather than separate top-level fields; PHP's $_POST is
    // case-sensitive, so a lowercase "checksum" field is silently ignored server-side and reads
    // back as "Checksum value is empty."
    override suspend fun getAvailableChannelById(
        merchantId: String,
        currency: String,
        checksum: String,
        username: String,
        password: String
    ): GetAvailableChannelResponseDto {
        val credentials = "$username:$password".encodeToByteArray().encodeBase64()
        val paramsJson = buildJsonObject {
            put("merchantID", merchantId)
            put("currency", currency)
        }.toString()

        FiuuLogger.log("--- Get Available Channel API ---")
        FiuuLogger.log("Authorization: Basic <redacted>")
        FiuuLogger.log("BODY: Checksum=$checksum Params=$paramsJson")

        val httpResponse: HttpResponse = httpClient.post(ApiConfig.channelUrl(buildMode)) {
            header("Authorization", "Basic $credentials")
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("Checksum", checksum)
                        append("Params", paramsJson)
                    }
                )
            )
        }

        FiuuLogger.log("Full URL called: ${httpResponse.request.url}")

        return when (httpResponse.status) {
            HttpStatusCode.OK, HttpStatusCode.Created -> {
                val responseText = httpResponse.bodyAsText()
                FiuuLogger.log("GET AVAILABLE CHANNEL RESPONSE:\n$responseText")
                json.decodeFromString<GetAvailableChannelResponseDto>(responseText)
            }
            else -> {
                val errorText = httpResponse.bodyAsText()
                FiuuLogger.log("GET AVAILABLE CHANNEL ERROR: $errorText")
                throw ApiException(
                    message = "Error: ${httpResponse.status.value}",
                    statusCode = httpResponse.status.value
                )
            }
        }
    }

    override suspend fun fetchInstalmentTenure(
        token: String,
        request: InstalmentTenureRequestDto,
        nonce: String,
        timestamp: String,
        signature: String
    ): InstalmentTenureResponseDto {
        val jsonBody = json.encodeToString(InstalmentTenureRequestDto.serializer(), request)

        FiuuLogger.log("--- Instalment Tenure API ---")
        FiuuLogger.log("Authorization: Bearer <redacted>")
        FiuuLogger.log("Headers:")
        FiuuLogger.log("  x-nonce: $nonce")
        FiuuLogger.log("  x-timestamp: $timestamp")
        FiuuLogger.log("  x-signature: <redacted>")
        FiuuLogger.log("INSTALMENT TENURE REQUEST:\n${redactSensitiveJson(jsonBody)}")

        val httpResponse: HttpResponse = httpClient.post("installment/installment-tenure") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            header("x-nonce", nonce)
            header("x-timestamp", timestamp)
            header("x-signature", signature)
            setBody(jsonBody)
        }

        FiuuLogger.log("Full URL called: ${httpResponse.request.url}")

        val responseText = httpResponse.bodyAsText()
        FiuuLogger.log("Instalment Tenure API Status: ${httpResponse.status}")
        FiuuLogger.log("INSTALMENT TENURE RESPONSE:\n$responseText")

        return when (httpResponse.status) {
            HttpStatusCode.OK, HttpStatusCode.Created ->
                json.decodeFromString<InstalmentTenureResponseDto>(responseText)
            else -> {
                // A declined/ineligible-card result (e.g. Status "11"/error_code "IP110") comes
                // back in this same body shape — try it before falling back to the generic
                // ErrorResponseDto/ApiException path other endpoints use.
                val tenureError = try {
                    json.decodeFromString<InstalmentTenureResponseDto>(responseText)
                } catch (e: Exception) {
                    null
                }
                if (tenureError?.errorDesc != null) return tenureError

                val errorDto = try {
                    json.decodeFromString<ErrorResponseDto>(responseText)
                } catch (e: Exception) {
                    null
                }
                throw ApiException(
                    message = errorDto?.message ?: errorDto?.error ?: "Error: ${httpResponse.status.value}",
                    statusCode = errorDto?.statusCode ?: httpResponse.status.value,
                    errorCode = errorDto?.code
                )
            }
        }
    }

    override suspend fun removeCardToken(accessToken: String, cardToken: String) {
        val jsonBody = json.encodeToString(TokenRemoveRequestDto.serializer(), TokenRemoveRequestDto(token = cardToken))
        FiuuLogger.log("--- Token Remove API ---")
        FiuuLogger.log("Authorization: Bearer <redacted>")
        FiuuLogger.log("TOKEN REMOVE REQUEST:\n${redactSensitiveJson(jsonBody)}")

        val httpResponse: HttpResponse = httpClient.post("token/remove") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            setBody(jsonBody)
        }

        FiuuLogger.log("Token Remove API Status: ${httpResponse.status}")

        if (httpResponse.status != HttpStatusCode.OK && httpResponse.status != HttpStatusCode.Created) {
            val errorText = httpResponse.bodyAsText()
            FiuuLogger.log("Token Remove Error Body: $errorText")
            val errorDto = try {
                json.decodeFromString<ErrorResponseDto>(errorText)
            } catch (e: Exception) {
                null
            }
            throw ApiException(
                message = errorDto?.message ?: errorDto?.error ?: "Error: ${httpResponse.status.value}",
                statusCode = errorDto?.statusCode ?: httpResponse.status.value,
                errorCode = errorDto?.code
            )
        }
    }

    // POST card-tokens — Bearer accessToken + the same nonce/idempotency-key/timestamp/x-signature
    // headers as /process, since the body is checksummed the same way (xp_ billing fields only).
    override suspend fun fetchCardTokens(
        accessToken: String,
        request: CardTokensRequestDto,
        nonce: String,
        idempotencyKey: String,
        timestamp: String,
        signature: String
    ): CardTokenContainerDto {
        val jsonBody = json.encodeToString(CardTokensRequestDto.serializer(), request)
        FiuuLogger.log("--- Card Tokens API ---")
        FiuuLogger.log("Authorization: Bearer <redacted>")
        FiuuLogger.log("Headers:")
        FiuuLogger.log("  x-nonce: $nonce")
        FiuuLogger.log("  x-idempotency-key: $idempotencyKey")
        FiuuLogger.log("  x-timestamp: $timestamp")
        FiuuLogger.log("  x-signature: <redacted>")
        FiuuLogger.log("CARD TOKENS REQUEST:\n${redactSensitiveJson(jsonBody)}")

        val httpResponse: HttpResponse = httpClient.post("card-tokens") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            header("x-nonce", nonce)
            header("x-idempotency-key", idempotencyKey)
            header("x-timestamp", timestamp)
            header("x-signature", signature)
            setBody(jsonBody)
        }

        FiuuLogger.log("Card Tokens API Status: ${httpResponse.status}")

        if (httpResponse.status != HttpStatusCode.OK && httpResponse.status != HttpStatusCode.Created) {
            val errorText = httpResponse.bodyAsText()
            FiuuLogger.log("Card Tokens Error Body: $errorText")
            val errorDto = try {
                json.decodeFromString<ErrorResponseDto>(errorText)
            } catch (e: Exception) {
                null
            }
            throw ApiException(
                message = errorDto?.message ?: errorDto?.error ?: "Error: ${httpResponse.status.value}",
                statusCode = errorDto?.statusCode ?: httpResponse.status.value,
                errorCode = errorDto?.code
            )
        }

        val rawJson = httpResponse.bodyAsText()
        FiuuLogger.log("Card Tokens Response: ${redactSensitiveJson(rawJson)}")
        // Observed in the wild: a 200/201 with a genuinely empty body (e.g. re-hitting this
        // endpoint for the same access token shortly after an earlier call) — treat that as "no
        // saved cards" rather than letting decodeFromString throw on EOF.
        if (rawJson.isBlank()) return CardTokenContainerDto()
        return json.decodeFromString(CardTokenContainerDto.serializer(), rawJson)
    }

    override suspend fun listenToStream(token: String): PaymentResponseDto? {
        // XDK_STREAM logs carry elapsed ms since connect, so a log dump shows whether the
        // connection opened, the server never sent an event, or an event failed to parse.
        val started = TimeSource.Monotonic.markNow()
        fun elapsed() = "${started.elapsedNow().inWholeMilliseconds}ms"
        FiuuLogger.log("XDK_STREAM Connecting: ${ApiConfig.baseUrl(buildMode)}$subscribePath")
        return try {
            // prepareGet().execute streams the body — a plain get() buffers the whole response
            // before returning, which for an open-ended SSE connection never returns while the
            // payment page is up. The client-wide 60s request timeout would also cut the stream
            // off mid-payment, so it's widened to SESSION_TIMEOUT_SECONDS here. The socket
            // timeout must be widened too: the backend sends nothing until the result, so the
            // client-wide 60s socket timeout dropped every stream after a minute of silence.
            httpClient.prepareGet(subscribePath) {
                header("Authorization", "Bearer $token")
                header("Accept", "text/event-stream")
                timeout {
                    requestTimeoutMillis = SESSION_TIMEOUT_SECONDS * 1000L
                    socketTimeoutMillis = SESSION_TIMEOUT_SECONDS * 1000L
                }
            }.execute { response ->
                FiuuLogger.log("XDK_STREAM Response headers received after ${elapsed()}: status=${response.status}, content-type=${response.headers["Content-Type"]}")

                if (!response.status.isSuccess()) {
                    FiuuLogger.log("XDK_STREAM Non-success status, body: ${response.bodyAsText()}")
                    return@execute null
                }

                val channel = response.bodyAsChannel()
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: continue
                    if (line.startsWith("data:").not()) continue

                    val eventJson = line.removePrefix("data:").trim()
                    FiuuLogger.log("XDK_STREAM Received after ${elapsed()}: $eventJson")

                    val payload = try {
                        json.decodeFromString(PaymentResponseDto.serializer(), eventJson)
                    } catch (parseError: Exception) {
                        FiuuLogger.log("XDK_STREAM Parse error: ${parseError.message}")
                        continue
                    }
                    FiuuLogger.log("XDK_STREAM Parsed: status=${payload.status}, transactionId=${payload.transactionId}")
                    return@execute payload
                }
                FiuuLogger.log("XDK_STREAM Closed by server after ${elapsed()} with no parsable data event")
                null
            }
        } catch (e: CancellationException) {
            FiuuLogger.log("XDK_STREAM Cancelled after ${elapsed()}")
            throw e
        } catch (e: Exception) {
            FiuuLogger.log("XDK_STREAM Stream error after ${elapsed()}: ${e::class.simpleName}: ${e.message}")
            null
        }
    }

    override fun close() {
        httpClient.close()
    }
}
