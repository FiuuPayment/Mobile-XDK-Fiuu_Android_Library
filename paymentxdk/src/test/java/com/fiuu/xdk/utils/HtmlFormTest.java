/*
 * Copyright 2026 Fiuu.
 */

package com.fiuu.xdk.utils;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class HtmlFormTest {

    @Test
    public void apostropheInsideDoubleQuotesIsPostedWhole() {
        String html = "<form action=\"https://pay.fiuu.com/pay\" method=\"post\">"
                + "<input type=\"hidden\" name=\"bill_name\" value=\"NOR A'BIDAH\">"
                + "</form>";

        Assert.assertEquals("https://pay.fiuu.com/pay", HtmlForm.extractFormAction(html));
        Assert.assertEquals("bill_name=NOR+A%27BIDAH", post(html));
    }

    @Test
    public void escapedApostropheRoundTrips() {
        String html = "<form action=\"https://pay.fiuu.com/pay\" method=\"post\">"
                + "<input type=\"hidden\" name=\"bill_name\" value=\""
                + HtmlForm.escapeAttribute("NOR A'BIDAH")
                + "\">"
                + "</form>";

        Assert.assertEquals("bill_name=NOR+A%27BIDAH", post(html));
    }

    @Test
    public void quoteAndAmpersandRoundTrip() {
        String name = "A & B \"C\"";
        String html = "<input name=\"bill_name\" value=\"" + HtmlForm.escapeAttribute(name) + "\">";

        Assert.assertEquals("bill_name=A+%26+B+%22C%22", post(html));
    }

    @Test
    public void singleQuotedValueWithoutApostropheIsUnchanged() {
        String html = "<form action='https://pay.fiuu.com/pay'>"
                + "<input type='hidden' name='bill_name' value='NOR A BIDAH'>"
                + "</form>";

        Assert.assertEquals("https://pay.fiuu.com/pay", HtmlForm.extractFormAction(html));
        Assert.assertEquals("bill_name=NOR+A+BIDAH", post(html));
    }

    @Test
    public void plainNameIsUnchanged() {
        String html = "<input name=\"bill_name\" value=\"NOR ABIDAH\">";
        Assert.assertEquals("bill_name=NOR+ABIDAH", post(html));
    }

    private static String post(String html) {
        byte[] data = HtmlForm.buildPostData(html);
        Assert.assertNotNull(data);
        return new String(data, StandardCharsets.UTF_8);
    }
}
