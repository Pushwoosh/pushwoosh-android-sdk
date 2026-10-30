package com.pushwoosh.inapp.view.config.enums;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

// Turkish default locale lowercases "I" to dotless "ı": "RIGHT" became "rıght" and matched no case.
public class ModalRichMediaEnumsTurkishLocaleTest {

    private final Locale defaultLocale = Locale.getDefault();

    @Before
    public void setUp() {
        Locale.setDefault(new Locale("tr", "TR"));
    }

    @After
    public void tearDown() {
        Locale.setDefault(defaultLocale);
    }

    @Test
    public void dismissAnimationType_parsesUppercaseRight() {
        assertEquals(
                ModalRichMediaDismissAnimationType.SLIDE_RIGHT, ModalRichMediaDismissAnimationType.fromString("RIGHT"));
    }

    @Test
    public void presentAnimationType_parsesUppercaseFadeIn() {
        assertEquals(
                ModalRichMediaPresentAnimationType.FADE_IN, ModalRichMediaPresentAnimationType.fromString("FADE_IN"));
    }

    @Test
    public void swipeGesture_parsesUppercaseRight() {
        assertEquals(ModalRichMediaSwipeGesture.RIGHT, ModalRichMediaSwipeGesture.fromString("RIGHT"));
    }
}
