package com.pushwoosh.tags;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

// Regression guard for SDK-977: putDate() must store the same "yyyy-MM-dd HH:mm" string
// regardless of the device locale — the value is sent to the backend as-is. Without a pinned
// Locale, an arabic/farsi device writes arabic-indic digits into the tag, and a
// buddhist-calendar device shifts the year by +543.
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class TagsBundlePutDateLocaleTest {

    // Zone is pinned so the expectation is absolute instead of host-dependent; Tokyo puts the
    // local hour past noon, which keeps an HH -> hh mask regression visible too.
    private static final TimeZone ZONE = TimeZone.getTimeZone("Asia/Tokyo");
    private static final Date DATE = new Date(1788930610000L);
    private static final String EXPECTED = "2026-09-09 14:10";

    private Locale originalLocale;
    private TimeZone originalTimeZone;

    @Before
    public void pinEnvironment() {
        originalLocale = Locale.getDefault();
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(ZONE);
    }

    @After
    public void restoreEnvironment() {
        Locale.setDefault(originalLocale);
        TimeZone.setDefault(originalTimeZone);
    }

    private String formatUnder(Locale deviceLocale) {
        Locale.setDefault(deviceLocale);
        return new TagsBundle.Builder().putDate("Last_Purchase", DATE).build().getString("Last_Purchase");
    }

    @Test
    public void putDate_isLocaleIndependent() {
        Locale[] deviceLocales = {
            Locale.US, new Locale("ar", "OM"), new Locale("fa", "IR"), Locale.forLanguageTag("th-TH-u-ca-buddhist"),
        };
        for (Locale locale : deviceLocales) {
            assertEquals(locale.toString(), EXPECTED, formatUnder(locale));
        }
    }
}
