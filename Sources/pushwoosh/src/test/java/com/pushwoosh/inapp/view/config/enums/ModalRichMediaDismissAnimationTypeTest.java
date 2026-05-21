package com.pushwoosh.inapp.view.config.enums;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModalRichMediaDismissAnimationTypeTest {

    // Verifies that fromString() maps documented aliases case-insensitively.
    @Test
    public void fromString_mapsAliasesCaseInsensitively() {
        Map<String, ModalRichMediaDismissAnimationType> expected = new LinkedHashMap<>();
        expected.put("fade_out", ModalRichMediaDismissAnimationType.FADE_OUT);
        expected.put("up", ModalRichMediaDismissAnimationType.SLIDE_UP);
        expected.put("right", ModalRichMediaDismissAnimationType.SLIDE_RIGHT);
        expected.put("down", ModalRichMediaDismissAnimationType.SLIDE_DOWN);
        expected.put("left", ModalRichMediaDismissAnimationType.SLIDE_LEFT);
        expected.put("none", ModalRichMediaDismissAnimationType.NONE);
        expected.put("Up", ModalRichMediaDismissAnimationType.SLIDE_UP);
        expected.put("FADE_Out", ModalRichMediaDismissAnimationType.FADE_OUT);

        for (Map.Entry<String, ModalRichMediaDismissAnimationType> entry : expected.entrySet()) {
            assertEquals(
                    "fromString(\"" + entry.getKey() + "\")",
                    entry.getValue(),
                    ModalRichMediaDismissAnimationType.fromString(entry.getKey()));
        }
    }

    // Verifies that compare() returns sign of (other.code - this.code) — reversed ordering relative to receiver.
    @Test
    public void compare_returnsReversedSignOfCodeDifference() {
        int greater = ModalRichMediaDismissAnimationType.FADE_OUT.compare(ModalRichMediaDismissAnimationType.NONE);
        int lesser = ModalRichMediaDismissAnimationType.NONE.compare(ModalRichMediaDismissAnimationType.FADE_OUT);
        int equal = ModalRichMediaDismissAnimationType.SLIDE_UP.compare(ModalRichMediaDismissAnimationType.SLIDE_UP);

        assertTrue("FADE_OUT.compare(NONE) must be positive, was " + greater, greater > 0);
        assertTrue("NONE.compare(FADE_OUT) must be negative, was " + lesser, lesser < 0);
        assertEquals("SLIDE_UP.compare(SLIDE_UP) must be 0", 0, equal);
    }
}
