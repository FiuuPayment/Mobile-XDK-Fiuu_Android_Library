package com.molpay.molpayxdk.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {

    // Define the date pattern and formatter as static final
    private static final String DATE_PATTERN = "yyyyMMddHHmmss";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    // Private constructor to prevent instantiation
    private DateTimeUtil() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    // Static method to get the current date-time in UTC
    public static String getCurrentDateTimeUTC() {
        LocalDateTime now = LocalDateTime.now(); // Get the current time in UTC
        return now.format(FORMATTER);
    }
}