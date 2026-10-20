package com.fiuu.xdkandroid.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import com.fiuu.xdkandroid.diagnostics.interceptOkHttp
import com.fiuu.xdkandroid.diagnostics.DiagnosticChaosEngine

actual class HttpClientFactory {

    actual fun create(buildMode: Int): HttpClient {
        return HttpClient(OkHttp) {
            engine {
                addInterceptor { chain ->
                    com.fiuu.xdkandroid.diagnostics.DiagnosticChaosEngine.interceptOkHttp(chain)
                }
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 60000
                connectTimeoutMillis = 60000
                socketTimeoutMillis = 60000
            }
            defaultRequest {
                url(ApiConfig.baseUrl(buildMode))
            }
        }
    }

    actual fun createTelemetryClient(): HttpClient {
        return HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 10000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 10000
            }
        }
    }
}