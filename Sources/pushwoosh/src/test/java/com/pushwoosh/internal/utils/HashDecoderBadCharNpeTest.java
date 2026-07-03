package com.pushwoosh.internal.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HashDecoderBadCharNpeTest {

    // Verifies that any hash carrying an out-of-alphabet char degrades to 0 (the same value
    // alphabetDecode already returns for null/empty), regardless of where the bad char sits.
    @Test
    public void alphabetDecode_badChar_returnsZero() {
        String[] cases = {"!", "a!", "ab-cd"}; // '-' is outside the base-62 alphabet too
        for (String input : cases) {
            assertEquals("case: " + input, 0L, HashDecoder.alphabetDecode(input));
        }
    }

    // Verifies that a >3-section hash with a bad char in the campaign section decodes gracefully
    // through the public parseMessageHash entry: campaign id falls back to 0, clean sections decode.
    @Test
    public void parseMessageHash_badCharInCampaignSection_decodesGracefully() {
        // "_!_2_3" -> parts ["","!","2","3"] (len 4 > 3) -> campaign "!" -> 0, message id "2" -> 2,
        // message code "3" (single section, as-is).
        String[] result = HashDecoder.parseMessageHash("_!_2_3");

        assertEquals("2", result[0]); // message id
        assertEquals("3", result[1]); // message code
        assertEquals("0", result[2]); // campaign id (undecodable -> 0)
    }

    // Verifies that the message-id section degrades the same way, proving every decoded section is
    // guarded (clean campaign section still decodes).
    @Test
    public void parseMessageHash_badCharInMessageIdSection_decodesGracefully() {
        // "_a_!_3" -> campaign "a" -> 10, message id "!" -> 0, message code "3" (as-is).
        String[] result = HashDecoder.parseMessageHash("_a_!_3");

        assertEquals("0", result[0]); // message id (undecodable -> 0)
        assertEquals("3", result[1]); // message code
        assertEquals("10", result[2]); // campaign id
    }

    // Verifies that the message-code section (decoded only when it contains a '-') degrades
    // gracefully: the inner bad char decodes to 0 rather than throwing.
    @Test
    public void parseMessageHash_badCharInMessageCodeSection_decodesGracefully() {
        // "_1_2_x-!" -> campaign "1" -> 1, message id "2" -> 2, message code "x-!" splits on '-':
        // "x" -> 33 -> hex "21" -> "0021" (first part), "!" -> 0 -> hex "0" -> "00000000".
        String[] result = HashDecoder.parseMessageHash("_1_2_x-!");

        assertEquals("2", result[0]); // message id
        assertEquals("0021-00000000", result[1]); // message code (bad inner char -> 0)
        assertEquals("1", result[2]); // campaign id
    }

    // Verifies that the guard is not over-broad: a >3-section hash whose every decoded section is
    // pure base-62 still decodes normally with no fallback to 0.
    @Test
    public void parseMessageHash_allBase62Sections_decodesNormally() {
        // "_1_2_a": campaign "1"->1, message id "2"->2, message code "a" (single section, as-is).
        String[] result = HashDecoder.parseMessageHash("_1_2_a");

        assertEquals("2", result[0]); // message id
        assertEquals("a", result[1]); // message code
        assertEquals("1", result[2]); // campaign id
    }

    // Verifies that a pure base-62 string still decodes to its real value, not the guard's 0.
    @Test
    public void alphabetDecode_allBase62_decodesNormally() {
        assertEquals(123L, HashDecoder.alphabetDecode("1Z"));
    }
}
