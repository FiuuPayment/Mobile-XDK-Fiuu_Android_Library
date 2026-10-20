package com.fiuu.xdkandroid.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InlineHtmlBaseUrlTest {

    @Test
    fun testFormActionOriginIsUsedAsBaseUrl() {
        // Shape of the /process urlType=data payload (MBPAY), decoded.
        val html = """<!DOCTYPE html><html lang="en"><head><meta charset="utf-8"/></head><body onload="document.forms[0].submit()"><form id="redirect" method="POST" action="https://pay.fiuu.com/RMS/redirection_to_3rdparty.php" accept-charset="UTF-8"><input type="hidden" name="q" value="abc" /></form></body></html>"""
        assertEquals("https://pay.fiuu.com/", inlineHtmlBaseUrl(html))
    }

    @Test
    fun testSingleQuotedActionWithPortAndQuery() {
        val html = """<FORM method='post' action='https://sandbox-payment.fiuu.com:8443?x=1'></FORM>"""
        assertEquals("https://sandbox-payment.fiuu.com:8443/", inlineHtmlBaseUrl(html))
    }

    @Test
    fun testNoHttpsFormActionReturnsNull() {
        assertNull(inlineHtmlBaseUrl("""<form action="/relative/path"></form>"""))
        assertNull(inlineHtmlBaseUrl("""<form action="http://insecure.example.com/x"></form>"""))
        assertNull(inlineHtmlBaseUrl("""<html><body>No form here</body></html>"""))
    }
}
