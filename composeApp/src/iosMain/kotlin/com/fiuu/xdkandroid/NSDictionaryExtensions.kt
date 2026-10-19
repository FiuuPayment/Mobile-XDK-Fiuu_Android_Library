package com.fiuu.xdkandroid

import platform.Foundation.NSDictionary

fun NSDictionary.toMap(): Map<Any?, Any?> {
    val result = mutableMapOf<Any?, Any?>()

    // keyEnumerator exists in NSDictionary
    val enumerator = keyEnumerator()
    while (true) {
        val key = enumerator.nextObject() ?: break
        result[key] = objectForKey(key)
    }

    return result
}