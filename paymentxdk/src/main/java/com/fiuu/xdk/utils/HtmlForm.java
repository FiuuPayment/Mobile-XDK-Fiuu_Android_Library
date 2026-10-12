/*
 * Copyright 2026 Fiuu.
 */

package com.fiuu.xdk.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes HTML form fields without treating an apostrophe inside a
 * double-quoted value as the end of the attribute.
 */
public final class HtmlForm {

    private static final Pattern FORM_ACTION = Pattern.compile(
            "action=(?:\"([^\"]*)\"|'([^']*)')", Pattern.CASE_INSENSITIVE);
    private static final Pattern INPUT_TAG = Pattern.compile(
            "<input([^>]*)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTRIBUTE = Pattern.compile(
            "([a-zA-Z_][\\w-]*)=(?:\"([^\"]*)\"|'([^']*)')");

    private HtmlForm() {
    }

    /**
     * Escape a value for a double-quoted HTML attribute.
     */
    public static String escapeAttribute(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public static String extractFormAction(String html) {
        if (html == null) {
            return "";
        }
        Matcher matcher = FORM_ACTION.matcher(html);
        if (!matcher.find()) {
            return "";
        }
        return unescapeHtml(quotedValue(matcher, 1));
    }

    /**
     * @return {@code application/x-www-form-urlencoded} bytes, or null when the HTML cannot be read
     */
    public static byte[] buildPostData(String html) {
        try {
            StringBuilder sb = new StringBuilder();
            if (html != null) {
                Matcher inputMatcher = INPUT_TAG.matcher(html);
                while (inputMatcher.find()) {
                    String attrs = inputMatcher.group(1);
                    Matcher attrMatcher = ATTRIBUTE.matcher(attrs);
                    String name = null;
                    String value = null;
                    while (attrMatcher.find()) {
                        String attrName = attrMatcher.group(1);
                        String attrValue = unescapeHtml(quotedValue(attrMatcher, 2));
                        if ("name".equalsIgnoreCase(attrName)) {
                            name = attrValue;
                        } else if ("value".equalsIgnoreCase(attrName)) {
                            value = attrValue;
                        }
                    }
                    if (name != null && value != null) {
                        if (sb.length() > 0) {
                            sb.append("&");
                        }
                        sb.append(URLEncoder.encode(name, "UTF-8"))
                                .append("=")
                                .append(URLEncoder.encode(value, "UTF-8"));
                    }
                }
            }
            return sb.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    private static String quotedValue(Matcher matcher, int doubleQuoteGroup) {
        String doubleQuoted = matcher.group(doubleQuoteGroup);
        return doubleQuoted != null ? doubleQuoted : matcher.group(doubleQuoteGroup + 1);
    }

    static String unescapeHtml(String value) {
        if (value == null || value.indexOf('&') < 0) {
            return value;
        }
        StringBuilder out = new StringBuilder(value.length());
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c != '&') {
                out.append(c);
                i++;
                continue;
            }
            int semi = value.indexOf(';', i + 1);
            if (semi < 0 || semi - i > 12) {
                out.append(c);
                i++;
                continue;
            }
            String decoded = decodeEntity(value.substring(i + 1, semi));
            if (decoded == null) {
                out.append(c);
                i++;
            } else {
                out.append(decoded);
                i = semi + 1;
            }
        }
        return out.toString();
    }

    private static String decodeEntity(String entity) {
        if ("amp".equals(entity)) {
            return "&";
        }
        if ("quot".equals(entity)) {
            return "\"";
        }
        if ("apos".equals(entity)) {
            return "'";
        }
        if ("lt".equals(entity)) {
            return "<";
        }
        if ("gt".equals(entity)) {
            return ">";
        }
        if (entity.length() > 1 && entity.charAt(0) == '#') {
            try {
                int codePoint = entity.charAt(1) == 'x' || entity.charAt(1) == 'X'
                        ? Integer.parseInt(entity.substring(2), 16)
                        : Integer.parseInt(entity.substring(1));
                if (Character.isValidCodePoint(codePoint)) {
                    return new String(Character.toChars(codePoint));
                }
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
