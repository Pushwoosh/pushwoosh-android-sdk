package com.pushwoosh.inapp.ui.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InAppColorParserTest {

    /** #RRGGBB parses to an opaque ARGB int. */
    @Test
    fun parsesSixDigitHex() {
        assertEquals(0xFFFF0000.toInt(), InAppColorParser.parse("#FF0000"))
    }

    /** #RGB shorthand expands each nibble (F -> FF). */
    @Test
    fun parsesThreeDigitHex() {
        assertEquals(0xFFFFFFFF.toInt(), InAppColorParser.parse("#FFF"))
    }

    /** #RGBA shorthand expands each nibble; the 4th is alpha (A -> AA). */
    @Test
    fun parsesFourDigitRgba() {
        // #F00A -> R=FF G=00 B=00 A=AA
        assertEquals(0xAAFF0000.toInt(), InAppColorParser.parse("#F00A"))
    }

    /** #RRGGBBAA puts the trailing pair into the alpha channel. */
    @Test
    fun parsesEightDigitHexWithAlpha() {
        assertEquals(0x80FF0000.toInt(), InAppColorParser.parse("#FF000080"))
    }

    /** The leading '#' is now mandatory: a bare hex string is rejected. */
    @Test
    fun requiresLeadingHash() {
        assertNull(InAppColorParser.parse("00FF00"))
        assertNull(InAppColorParser.parse("FFF"))
    }

    /** Partly-invalid, unsupported-length, or empty input returns null, never a wrong color. */
    @Test
    fun returnsNullForMalformedInput() {
        assertNull(InAppColorParser.parse("#abcXYZ"))
        assertNull(InAppColorParser.parse("0x1234"))
        assertNull(InAppColorParser.parse("#12"))
        assertNull(InAppColorParser.parse("#12345"))
        assertNull(InAppColorParser.parse(""))
        assertNull(InAppColorParser.parse("#"))
        assertNull(InAppColorParser.parse(null))
        // A leading sign is not a hex digit — toLong(16) would accept "#-FF" otherwise.
        assertNull(InAppColorParser.parse("#-FF"))
        assertNull(InAppColorParser.parse("#+FFF"))
    }

    /** The leading '#' is mandatory even when the tail would itself be a valid hex color. */
    @Test
    fun rejectsMissingHashEvenWhenTailIsValidHex() {
        // Without the '#' guard, substring(1) of "7123456" is "123456" — a valid 6-digit hex.
        assertNull(InAppColorParser.parse("7123456"))
    }

    /** Every hex-digit range edge (0,9,a,f,A,F) must count as a valid digit and map to its channel. */
    @Test
    fun acceptsAllHexDigitRangeBoundaries() {
        // #09afAF touches the low/high edge of each accepted range: '0'..'9', 'a'..'f', 'A'..'F'.
        assertEquals(0xFF09AFAF.toInt(), InAppColorParser.parse("#09afAF"))
    }
}
