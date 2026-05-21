package com.pushwoosh.internal.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HashDecoderTest {

    @Test
    public void parseMessageHash_tooFewSections_returnsDefaultTriple() {
        String[] threeParts = HashDecoder.parseMessageHash("_abc_def");
        String[] singlePart = HashDecoder.parseMessageHash("nodelimiters");
        String[] emptyInput = HashDecoder.parseMessageHash("");

        assertArrayEquals(new String[] {"0", "", "0"}, threeParts);
        assertArrayEquals(new String[] {"0", "", "0"}, singlePart);
        assertArrayEquals(new String[] {"0", "", "0"}, emptyInput);
    }

    @Test
    public void parseMessageHash_wellFormedHash_decodesAllThreeParts() {
        // "_1_2_a": campaignID="1"->1, messageID="2"->2, messageCode="a" (single part, returned as-is)
        String[] result = HashDecoder.parseMessageHash("_1_2_a");

        assertEquals("2", result[0]);
        assertEquals("a", result[1]);
        assertEquals("1", result[2]);
    }

    @Test
    public void decodeMessageCode_noDash_returnsInputAsIs() {
        String result = HashDecoder.decodeMessageCode("abc");

        assertEquals("abc", result);
    }

    @Test
    public void decodeMessageCode_multipleSections_decodesToHexWithPadding() {
        // "1-1": each section decodes to 1 -> hex "1"; first padded to 4 chars, others to 8.
        String result = HashDecoder.decodeMessageCode("1-1");

        assertEquals("0001-00000001", result);
    }

    @Test
    public void decodeMessageCode_sectionsAlreadyAtLeastMinimumLength_doesNotPad() {
        // "zzz" -> 136745 -> hex "21629" (5 chars >= 4, no padding for first part).
        // "zzzzz" -> 525649985 -> hex "1F54C841" (8 chars >= 8, no padding for other parts).
        String result = HashDecoder.decodeMessageCode("zzz-zzzzz");

        assertEquals("21629-1F54C841", result);
    }

    @Test
    public void alphabetDecode_nullOrEmpty_returnsZero() {
        assertEquals(0L, HashDecoder.alphabetDecode(null));
        assertEquals(0L, HashDecoder.alphabetDecode(""));
    }

    @Test
    public void alphabetDecode_singleCharacter_returnsAlphabetIndex() {
        String[][] cases = {
            {"0", "0"},
            {"9", "9"},
            {"a", "10"},
            {"z", "35"},
            {"A", "36"},
            {"Z", "61"},
        };

        for (String[] pair : cases) {
            String input = pair[0];
            long expected = Long.parseLong(pair[1]);
            assertEquals("alphabetDecode(\"" + input + "\")", expected, HashDecoder.alphabetDecode(input));
        }
    }

    @Test
    public void alphabetDecode_multipleCharacters_returnsBase62Value() {
        assertEquals(62L, HashDecoder.alphabetDecode("10"));
        assertEquals(123L, HashDecoder.alphabetDecode("1Z"));
    }
}
