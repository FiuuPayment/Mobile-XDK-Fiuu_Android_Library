package com.fiuu.xdkandroid.data.remote

import io.ktor.client.HttpClient

expect class HttpClientFactory() {
    fun create(buildMode: Int): HttpClient
    fun createTelemetryClient(): HttpClient
}