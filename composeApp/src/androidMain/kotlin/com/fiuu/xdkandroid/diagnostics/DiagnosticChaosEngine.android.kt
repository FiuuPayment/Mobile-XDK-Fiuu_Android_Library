package com.fiuu.xdkandroid.diagnostics

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.net.ConnectException
import java.net.SocketTimeoutException

internal fun DiagnosticChaosEngine.interceptOkHttp(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val mode = activeNetworkMode

    return when (mode) {
        SimulatedNetworkMode.NORMAL -> {
            chain.proceed(request)
        }
        SimulatedNetworkMode.SLOW_3G -> {
            try {
                Thread.sleep(2000)
            } catch (_: InterruptedException) {}
            chain.proceed(request)
        }
        SimulatedNetworkMode.SLOW_2G -> {
            try {
                Thread.sleep(4500)
            } catch (_: InterruptedException) {}
            chain.proceed(request)
        }
        SimulatedNetworkMode.PACKET_LOSS -> {
            val roll = (1..100).random()
            if (roll <= 30) {
                try {
                    Thread.sleep(1500)
                } catch (_: InterruptedException) {}
                throw SocketTimeoutException("DiagnosticChaosEngine: Simulated 30% packet loss timeout on ${request.url}")
            } else {
                chain.proceed(request)
            }
        }
        SimulatedNetworkMode.OFFLINE -> {
            throw ConnectException("DiagnosticChaosEngine: Simulated offline mode: Connection refused to ${request.url.host}")
        }
        SimulatedNetworkMode.FAULT_500 -> {
            val jsonMedia = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = """{"status":"11","error_code":"500","error_desc":"Simulated Gateway 500 Internal Error"}""".toResponseBody(jsonMedia)
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Internal Server Error (Simulated Chaos)")
                .body(body)
                .build()
        }
        SimulatedNetworkMode.FAULT_504 -> {
            val jsonMedia = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = """{"status":"11","error_code":"504","error_desc":"Simulated Gateway 504 Timeout"}""".toResponseBody(jsonMedia)
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(504)
                .message("Gateway Timeout (Simulated Chaos)")
                .body(body)
                .build()
        }
        SimulatedNetworkMode.FAULT_400 -> {
            val jsonMedia = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = """{"status":"11","error_code":"400","error_desc":"Simulated Bad Request: Invalid Nonce / Signature Mismatch"}""".toResponseBody(jsonMedia)
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(400)
                .message("Bad Request (Simulated Chaos)")
                .body(body)
                .build()
        }
    }
}
