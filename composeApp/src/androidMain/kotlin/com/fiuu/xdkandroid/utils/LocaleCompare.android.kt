package com.fiuu.xdkandroid.utils

import java.text.Collator
import java.util.Locale

actual fun localeCompareBase(a: String, b: String): Int {
    val collator = Collator.getInstance(Locale.ROOT)
    collator.strength = Collator.PRIMARY
    return collator.compare(a, b)
}
