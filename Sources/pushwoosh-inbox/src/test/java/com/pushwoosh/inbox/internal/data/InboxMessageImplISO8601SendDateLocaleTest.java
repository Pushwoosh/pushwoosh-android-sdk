package com.pushwoosh.inbox.internal.data;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

import java.util.Locale;
import java.util.TimeZone;

// Regression guard for SDK-977: getISO8601SendDate() must produce the same valid ISO8601
// string regardless of the device locale. Without a pinned Locale, SimpleDateFormat picks up
// the device default: arabic-indic digits on ar/fa locales, buddhist year (+543) on a
// buddhist-calendar locale — downstream JS Date parsing then yields NaN or a wrong instant.
// JVM caveat (from the spec): desktop JDK gives ar_SA a gregorian calendar, so the calendar
// dimension is exercised via an explicit ca=buddhist extension instead.
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class InboxMessageImplISO8601SendDateLocaleTest {

    // Zone is pinned so the expectation is absolute instead of host-dependent; Tokyo puts the
    // local hour past noon, which keeps an HH -> hh mask regression visible too.
    private static final TimeZone ZONE = TimeZone.getTimeZone("Asia/Tokyo");
    private static final long SEND_DATE_UNIX = 1788930610L;
    private static final String EXPECTED = "2026-09-09T14:10:10+0900";

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
        InboxMessageInternal internal = mock(InboxMessageInternal.class);
        when(internal.getSendDate()).thenReturn(SEND_DATE_UNIX);
        return new InboxMessageImpl(internal).getISO8601SendDate();
    }

    @Test
    public void iso8601SendDate_isLocaleIndependent() {
        Locale[] deviceLocales = {
            Locale.US, new Locale("ar", "OM"), new Locale("fa", "IR"), Locale.forLanguageTag("th-TH-u-ca-buddhist"),
        };
        for (Locale locale : deviceLocales) {
            assertEquals(locale.toString(), EXPECTED, formatUnder(locale));
        }
    }
}
