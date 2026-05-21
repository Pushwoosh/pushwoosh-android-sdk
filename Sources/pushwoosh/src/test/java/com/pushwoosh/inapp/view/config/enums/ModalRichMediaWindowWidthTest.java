package com.pushwoosh.inapp.view.config.enums;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ModalRichMediaWindowWidthTest {

    // Verifies that compare uses reversed argument order vs Integer.compare(this, that).
    @Test
    public void compare_differentValues_usesReversedArgumentOrder() {
        assertTrue(
                "FULL_SCREEN.compare(WRAP_CONTENT) must be positive (source.code=1 > this.code=0)",
                ModalRichMediaWindowWidth.FULL_SCREEN.compare(ModalRichMediaWindowWidth.WRAP_CONTENT) > 0);
        assertTrue(
                "WRAP_CONTENT.compare(FULL_SCREEN) must be negative (source.code=0 < this.code=1)",
                ModalRichMediaWindowWidth.WRAP_CONTENT.compare(ModalRichMediaWindowWidth.FULL_SCREEN) < 0);
    }
}
