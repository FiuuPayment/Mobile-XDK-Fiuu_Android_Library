package com.fiuu.xdkandroid.utils

object NumberFormatter {

    fun formatAmount(amount: Double): String {
        val rounded = amount.toInt() / 100.0
        return rounded.toString().let {
            if (it.contains(".")) {
                val parts = it.split(".")
                parts[0] + "." + parts[1].padEnd(2, '0').take(2)
            } else {
                "$it.00"
            }
        }
    }

    // Formats a Long amount already in minor units (e.g. 123456 -> "1234.56") without ever
    // converting through Double/Int — formatAmount(Double) truncates to Int32 internally, so any
    // amount above ~21.4M major units silently wraps. Long division/modulo has no such ceiling.
    fun formatMinorAmount(amount: Long): String {
        val major = amount / 100
        val minor = (amount % 100).let { if (it < 0) -it else it }
        return "$major.${minor.toString().padStart(2, '0')}"
    }
}
