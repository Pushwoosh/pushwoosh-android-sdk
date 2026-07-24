package com.pushwoosh.inapp.ui.parser

import androidx.annotation.ColorInt

/**
 * Parses the CSS hex forms `#RGB`, `#RGBA`, `#RRGGBB`, `#RRGGBBAA` into an ARGB [Int]. The
 * leading `#` is mandatory (matches what the native-rich-media preview renders); anything else
 * — a bare hex string, a bad length, non-hex characters, empty or null — returns `null` so the
 * strict parser can reject the whole config.
 *
 * Not `Color.parseColor`: that throws on `#RGB`/`#RGBA`, rejects `#RRGGBBAA`, and is awkward to
 * guard. `toLong(16)` here requires the whole string to be valid hex.
 */
object InAppColorParser {

    @ColorInt
    fun parse(hex: String?): Int? {
        val raw = hex?.trim() ?: return null
        if (!raw.startsWith("#")) return null
        val digits = raw.substring(1)
        // toLong(16) would otherwise accept a leading sign ("#-FF"); require pure hex digits.
        if (digits.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) return null
        val value = digits.toLongOrNull(16) ?: return null
        return when (digits.length) {
            3 -> argb(0xFF, nibble(value, 2), nibble(value, 1), nibble(value, 0))
            4 -> argb(nibble(value, 0), nibble(value, 3), nibble(value, 2), nibble(value, 1))
            6 -> argb(0xFF, (value shr 16) and 0xFF, (value shr 8) and 0xFF, value and 0xFF)
            8 -> argb(value and 0xFF, (value shr 24) and 0xFF, (value shr 16) and 0xFF, (value shr 8) and 0xFF)
            else -> null
        }
    }

    /** Expands the [index]-th hex nibble (0 = least significant) to a 0x00..0xFF byte (F -> FF). */
    private fun nibble(value: Long, index: Int): Long = ((value shr (index * 4)) and 0xF) * 0x11

    private fun argb(a: Long, r: Long, g: Long, b: Long): Int =
        (((a and 0xFF) shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)).toInt()
}
