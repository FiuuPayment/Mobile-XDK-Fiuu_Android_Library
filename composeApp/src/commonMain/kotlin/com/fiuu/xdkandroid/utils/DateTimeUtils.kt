package com.fiuu.xdkandroid.utils

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object DateTimeUtils {
    /**
     * Returns the current Unix timestamp in seconds as a String.
     * Example: "1774429708"
     */
    @OptIn(ExperimentalTime::class)
    fun getCurrentUnixTimestampString(): String {
        return Clock.System.now().epochSeconds.toString()
    }

    /**
     * Returns the current local date/time as "yyyyMMddHHmmss", matching the instalment-tenure
     * endpoint's pre-request script (`now.getFullYear()`/`getMonth()`/etc, which are local-time
     * JS Date accessors, not UTC).
     */
    @OptIn(ExperimentalTime::class)
    fun getLocalDateTimeCompactString(): String {
        val dt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val pad = { n: Int -> n.toString().padStart(2, '0') }
        return "${dt.year}${pad(dt.monthNumber)}${pad(dt.day)}${pad(dt.hour)}${pad(dt.minute)}${pad(dt.second)}"
    }
}
