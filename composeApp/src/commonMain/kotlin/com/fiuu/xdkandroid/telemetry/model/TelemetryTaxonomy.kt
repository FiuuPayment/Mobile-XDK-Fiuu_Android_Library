package com.fiuu.xdkandroid.telemetry.model

/**
 * Standard origin and source attribution tags for telemetry events.
 * Identifies the exact layer or partner entity responsible for a transaction decline or failure.
 */
object TelemetryOrigin {
    /** Host bank, card scheme, or card issuer (e.g. status 78, 05, 51, 14). */
    const val EXTERNAL_HOST_BANK = "EXTERNAL_HOST_BANK"

    /** Partner e-wallet provider (e.g. Touch 'n Go, GrabPay, Boost, ShopeePay E01, E04, E09). */
    const val EXTERNAL_E_WALLET = "EXTERNAL_E_WALLET"

    /** PayNet Online Banking / FPX switch error (e.g. bank offline, session timeout). */
    const val EXTERNAL_FPX = "EXTERNAL_FPX"

    /** Fiuu / RazerVT API Gateway credential, validation, or checksum error (e.g. status 11, 99). */
    const val GATEWAY_REJECTION = "GATEWAY_REJECTION"

    /** Local client, device operating system, network drop, timeout, or SDK policy. */
    const val INTERNAL_SDK = "INTERNAL_SDK"
}

/**
 * Standard event types accepted by the Mobile Gateway Activity Log schema.
 */
object TelemetryEventType {
    const val REQUEST = "request"
    const val RESPONSE = "response"
    const val HOST_DECLINE = "host_decline"
    const val TERMINAL_FAILURE = "terminal_failure"
    const val ERROR = "error"
    const val EVENT = "event"
}

/**
 * Standardized process labels ensuring clear distinction between outbound requests,
 * inbound host responses, webview transitions, and terminal outcomes in CloudWatch insights.
 */
object TelemetryProcess {
    // Handshake & Init
    const val INITIATE_REQUEST = "initiate_request"
    const val INITIATE_HOST_RESPONSE = "initiate_host_response"

    // Process Payment
    const val CREATE_PAYMENT_REQUEST = "createPayment_request"
    const val CREATE_PAYMENT_HOST_RESPONSE = "createPayment_host_response"

    // Express Payment
    const val EXPRESS_REQUEST = "express_request"
    const val EXPRESS_HOST_RESPONSE = "express_host_response"

    // Inquiry & Status Poll
    const val INQUIRE_REQUEST = "inquire_request"
    const val INQUIRE_HOST_RESPONSE = "inquire_host_response"

    // Channels & Config
    const val GET_CHANNELS_REQUEST = "getChannels_request"
    const val GET_CHANNELS_HOST_RESPONSE = "getChannels_host_response"

    // Webview Milestones & Transitions
    const val WEBVIEW_LOAD = "webView_load"
    const val WEBVIEW_FAILURE = "webView_failure"
    const val WEBVIEW_REDIRECT = "webView_redirect"

    // Native Wallets
    const val APPLE_PAY_OUTCOME = "applePay_outcome"
    const val GOOGLE_PAY_OUTCOME = "googlePay_outcome"
    const val SAMSUNG_PAY_OUTCOME = "samsungPay_outcome"

    // Terminal Outcome
    const val ONLINE_PAYMENT_OUTCOME = "onlinePaymentOutcome"
}
