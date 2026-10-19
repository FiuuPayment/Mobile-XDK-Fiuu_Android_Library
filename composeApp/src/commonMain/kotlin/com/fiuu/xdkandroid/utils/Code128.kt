package com.fiuu.xdkandroid.utils

// Standard ISO/IEC 15417 Code 128 pattern table (values 0-106): each entry is a sequence of
// module widths for alternating bar/space/bar/space/... starting with a bar (the stop pattern
// carries a 7th module — the trailing termination bar). Subset B only (printable ASCII 32..127)
// is implemented, since every offline-payment barcode this SDK renders so far uses numeric
// transaction identifiers, which fit comfortably in subset B.
private val CODE128_PATTERNS = arrayOf(
    "212222", "222122", "222221", "121223", "121322", "131222", "122213", "122312", "132212", "221213",
    "221312", "231212", "112232", "122132", "122231", "113222", "123122", "123221", "223211", "221132",
    "221231", "213212", "223112", "312131", "311222", "321122", "321221", "312212", "322112", "322211",
    "212123", "212321", "232121", "111323", "131123", "131321", "112313", "132113", "132311", "211313",
    "231113", "231311", "112133", "112331", "132131", "113123", "113321", "133121", "313121", "211331",
    "231131", "213113", "213311", "213131", "311123", "311321", "331121", "312113", "312311", "332111",
    "314111", "221411", "431111", "111224", "111422", "121124", "121421", "141122", "141221", "112214",
    "112412", "122114", "122411", "142112", "142211", "241211", "221114", "413111", "241112", "134111",
    "111242", "121142", "121241", "114212", "124112", "124211", "411212", "421112", "421211", "212141",
    "214121", "412121", "111143", "111341", "131141", "114113", "114311", "411113", "411311", "113141",
    "114131", "311141", "411131", "211412", "211214", "211232", "2331112"
)

private const val START_B = 104
private const val STOP = 106

/**
 * Encodes [data] as Code 128 subset B and returns the module-width sequence (alternating
 * bar/space, starting with a bar at index 0). Characters outside the encodable range (' '..DEL)
 * are dropped rather than thrown — a partially-wrong-but-renderable barcode from unexpected
 * backend data is more useful here than a crash on the instruction screen.
 */
fun encodeCode128B(data: String): IntArray {
    val values = mutableListOf(START_B)
    data.forEach { c ->
        val v = c.code - 32
        if (v in 0..95) values.add(v)
    }
    var checksum = values[0]
    for (i in 1 until values.size) checksum += values[i] * i
    values.add(checksum % 103)
    values.add(STOP)

    val widths = mutableListOf<Int>()
    values.forEach { v -> CODE128_PATTERNS[v].forEach { ch -> widths.add(ch - '0') } }
    return widths.toIntArray()
}
