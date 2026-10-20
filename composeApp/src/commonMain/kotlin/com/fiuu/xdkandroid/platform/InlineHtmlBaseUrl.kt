package com.fiuu.xdkandroid.platform

// Inline "data" HTML from /process (urlType == "data") is an auto-submitting redirect form, e.g.
// <form method="POST" action="https://pay.fiuu.com/RMS/redirection_to_3rdparty.php">. Loading it
// with a null base URL gives the page an opaque origin, so that form POST goes out with
// Origin: null and no Referer. Using the form action's own origin instead makes it a same-origin
// POST, and stays correct across sandbox/UAT/production since the host comes from the payload
// itself rather than being hardcoded. Returns null (the previous behaviour) when there's no
// https form action to take it from.
private val FORM_ACTION_ORIGIN = Regex(
    """<form\b[^>]*\baction\s*=\s*["'](https://[^/"'?#\s]+)""",
    RegexOption.IGNORE_CASE
)

internal fun inlineHtmlBaseUrl(html: String): String? =
    FORM_ACTION_ORIGIN.find(html)?.groupValues?.get(1)?.let { "$it/" }
