package com.fiuu.xdkandroid.utils

// Matches the backend's reference checksum algorithm, which sorts non-numeric leaf values via
// JS's String.localeCompare(other, undefined, { sensitivity: 'base' }) — a case/diacritic
// insensitive Unicode collation, not a plain ordinal/lowercase comparison. The two disagree on
// ordering for strings containing punctuation/symbols (observed: a string starting with '{'
// sorts before "0183767464" under locale collation, but after it under ASCII ordinal compare),
// which silently breaks the checksum whenever such values are present (e.g. a JSON-as-string
// Google Pay token).
expect fun localeCompareBase(a: String, b: String): Int
