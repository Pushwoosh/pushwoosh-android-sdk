package com.pushwoosh.richmedia;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RichMediaColorSchemeTest {

    @Test
    public void fromString_exactNames() {
        assertEquals(RichMediaColorScheme.APP, RichMediaColorScheme.fromString("APP"));
        assertEquals(RichMediaColorScheme.SYSTEM, RichMediaColorScheme.fromString("SYSTEM"));
        assertEquals(RichMediaColorScheme.LIGHT, RichMediaColorScheme.fromString("LIGHT"));
        assertEquals(RichMediaColorScheme.DARK, RichMediaColorScheme.fromString("DARK"));
    }

    @Test
    public void fromString_isCaseInsensitive() {
        assertEquals(RichMediaColorScheme.DARK, RichMediaColorScheme.fromString("dark"));
        assertEquals(RichMediaColorScheme.SYSTEM, RichMediaColorScheme.fromString("SyStEm"));
        assertEquals(RichMediaColorScheme.LIGHT, RichMediaColorScheme.fromString("Light"));
        assertEquals(RichMediaColorScheme.APP, RichMediaColorScheme.fromString("app"));
    }

    @Test
    public void fromString_unknownOrNull_fallsBackToApp() {
        assertEquals(RichMediaColorScheme.APP, RichMediaColorScheme.fromString("blue"));
        assertEquals(RichMediaColorScheme.APP, RichMediaColorScheme.fromString(""));
        assertEquals(RichMediaColorScheme.APP, RichMediaColorScheme.fromString(null));
    }
}
