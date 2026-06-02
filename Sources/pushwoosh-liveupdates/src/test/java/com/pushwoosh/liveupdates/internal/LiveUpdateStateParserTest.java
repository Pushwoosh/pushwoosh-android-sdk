package com.pushwoosh.liveupdates.internal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.os.Bundle;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.liveupdates.LiveUpdateOperation;
import com.pushwoosh.liveupdates.LiveUpdateState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class LiveUpdateStateParserTest {

    @Before
    public void setUp() {
        // PushBundleDataProvider.getHeader() falls back to AppInfoProvider when "header"
        // is absent, so the platform module must be initialized for unit tests.
        AndroidPlatformModule.init(RuntimeEnvironment.getApplication(), true);
    }

    @Test
    public void happyPath_parsesAllFields() {
        Bundle b = new Bundle();
        b.putString("pw_live_op", "update");
        b.putString("pw_live_id", "order_4521");
        b.putString("header", "Order #4521");
        b.putString("title", "Cooking");
        b.putString("ci", "https://example.com/icon.png");
        b.putString("pw_live_progress", "35");
        b.putString("pw_live_progress_indeterminate", "false");
        b.putString("pw_live_segments", "[{\"color\":\"#00FF00\",\"label\":\"Cooking\"}]");

        LiveUpdateState s = LiveUpdateStateParser.parse(b);

        assertNotNull(s);
        assertEquals("order_4521", s.getActivityId());
        assertEquals(LiveUpdateOperation.UPDATE, s.getOperation());
        assertEquals("Order #4521", s.getTitle());
        assertEquals("Cooking", s.getSubtitle());
        assertEquals(Integer.valueOf(35), s.getProgress());
        assertFalse(s.isProgressIndeterminate());
        assertEquals(1, s.getSegments().size());
        // "label" key is silently ignored; length defaults to 1 when absent.
        assertEquals(1, s.getSegments().get(0).getLength());
        assertEquals("https://example.com/icon.png", s.getIconUrl());
    }

    @Test
    public void missingPwLiveId_returnsNull() {
        Bundle b = new Bundle();
        b.putString("pw_live_op", "start");
        assertNull(LiveUpdateStateParser.parse(b));
    }

    @Test
    public void missingPwLiveOp_returnsNull() {
        Bundle b = new Bundle();
        b.putString("pw_live_id", "x");
        assertNull(LiveUpdateStateParser.parse(b));
    }

    @Test
    public void unknownOperation_returnsNull() {
        Bundle b = new Bundle();
        b.putString("pw_live_op", "pause");
        b.putString("pw_live_id", "x");
        assertNull(LiveUpdateStateParser.parse(b));
    }

    @Test
    public void degenerateCase_onlyRequiredFields_parses() {
        Bundle b = new Bundle();
        b.putString("pw_live_op", "start");
        b.putString("pw_live_id", "x");

        LiveUpdateState s = LiveUpdateStateParser.parse(b);

        assertNotNull(s);
        assertEquals("x", s.getActivityId());
        assertEquals(LiveUpdateOperation.START, s.getOperation());
        assertNull(s.getProgress());
        assertFalse(s.isProgressIndeterminate());
        assertTrue(s.getSegments().isEmpty());
        assertTrue(s.getActions().isEmpty());
    }

    @Test
    public void malformedSegmentsJson_returnsEmptyList() {
        Bundle b = new Bundle();
        b.putString("pw_live_op", "update");
        b.putString("pw_live_id", "x");
        b.putString("pw_live_segments", "not-a-json");

        LiveUpdateState s = LiveUpdateStateParser.parse(b);

        assertNotNull(s);
        assertTrue(s.getSegments().isEmpty());
    }

    @Test
    public void progressIndeterminate_acceptsTrueOneYes() {
        for (String v : new String[] {"true", "1", "yes", "TRUE", "Yes"}) {
            Bundle b = new Bundle();
            b.putString("pw_live_op", "update");
            b.putString("pw_live_id", "x");
            b.putString("pw_live_progress_indeterminate", v);
            assertTrue("for value: " + v, LiveUpdateStateParser.parse(b).isProgressIndeterminate());
        }
    }

    @Test
    public void nullBundle_returnsNull() {
        assertNull(LiveUpdateStateParser.parse(null));
    }

    private static Bundle baseBundle() {
        Bundle b = new Bundle();
        b.putString("pw_live_op", "update");
        b.putString("pw_live_id", "x");
        return b;
    }

    @Test
    public void when_absent_isNull() {
        assertNull(LiveUpdateStateParser.parse(baseBundle()).getWhen());
    }

    @Test
    public void when_numeric_parsedToLong() {
        Bundle b = baseBundle();
        b.putString("pw_live_when", "1779976320000");
        assertEquals(
                Long.valueOf(1779976320000L), LiveUpdateStateParser.parse(b).getWhen());
    }

    @Test
    public void when_nonNumeric_fallsBackToNullSilently() {
        Bundle b = baseBundle();
        b.putString("pw_live_when", "foo");
        // A type mismatch on `when` is silent, like `progress` — no warn, no throw.
        // A minimal bundle has no other warn source, so verifying "never warn" is safe.
        try (MockedStatic<PWLog> log = mockStatic(PWLog.class)) {
            LiveUpdateState s = LiveUpdateStateParser.parse(b);
            assertNull(s.getWhen());
            log.verify(() -> PWLog.warn(anyString(), anyString()), never());
        }
    }

    @Test
    public void chronometer_defaultsFalse_acceptsTruthy() {
        assertFalse(LiveUpdateStateParser.parse(baseBundle()).isChronometer());
        for (String v : new String[] {"true", "1", "yes"}) {
            Bundle b = baseBundle();
            b.putString("pw_live_chronometer", v);
            assertTrue("for value: " + v, LiveUpdateStateParser.parse(b).isChronometer());
        }
    }

    @Test
    public void chronometerCountDown_defaultsFalse_acceptsTruthy() {
        assertFalse(LiveUpdateStateParser.parse(baseBundle()).isChronometerCountDown());
        for (String v : new String[] {"true", "1", "yes"}) {
            Bundle b = baseBundle();
            b.putString("pw_live_chronometer_count_down", v);
            assertTrue("for value: " + v, LiveUpdateStateParser.parse(b).isChronometerCountDown());
        }
    }

    @Test
    public void showWhen_defaultsTrue_falsyTurnsOff() {
        assertTrue(LiveUpdateStateParser.parse(baseBundle()).isShowWhen());
        for (String v : new String[] {"false", "0", "no"}) {
            Bundle b = baseBundle();
            b.putString("pw_live_show_when", v);
            assertFalse("for value: " + v, LiveUpdateStateParser.parse(b).isShowWhen());
        }
    }

    @Test
    public void isLiveUpdatePush_recognizesByPwLiveOpKey() {
        Bundle yes = new Bundle();
        yes.putString("pw_live_op", "start");
        assertTrue(LiveUpdateStateParser.isLiveUpdatePush(yes));

        Bundle no = new Bundle();
        no.putString("title", "hi");
        assertFalse(LiveUpdateStateParser.isLiveUpdatePush(no));

        assertFalse(LiveUpdateStateParser.isLiveUpdatePush(null));
    }
}
