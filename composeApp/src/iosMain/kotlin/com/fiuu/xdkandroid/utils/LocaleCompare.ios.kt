package com.fiuu.xdkandroid.utils

import platform.Foundation.NSCaseInsensitiveSearch
import platform.Foundation.NSDiacriticInsensitiveSearch
import platform.Foundation.NSOrderedAscending
import platform.Foundation.NSOrderedDescending
import platform.Foundation.NSOrderedSame
import platform.Foundation.NSString
import platform.Foundation.compare

actual fun localeCompareBase(a: String, b: String): Int {
    val result = (a as NSString).compare(b, options = NSCaseInsensitiveSearch or NSDiacriticInsensitiveSearch)
    return when (result) {
        NSOrderedAscending -> -1
        NSOrderedSame -> 0
        NSOrderedDescending -> 1
        else -> 0
    }
}
