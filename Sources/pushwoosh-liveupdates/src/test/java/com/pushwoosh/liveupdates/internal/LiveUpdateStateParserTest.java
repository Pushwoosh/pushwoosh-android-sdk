package com.pushwoosh.liveupdates.internal;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import android.os.Bundle;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.liveupdates.LiveUpdateOperation;
import com.pushwoosh.liveupdates.LiveUpdateSegment;
import com.pushwoosh.liveupdates.LiveUpdateState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class LiveUpdateStateParserTest {

    @Before
    public void setUp() {
        // PushBundleDataProvider.getHeader() falls back to AppInfoProvider when "header"
        // is absent, so the platform module must be initialized for unit tests.
        AndroidPlatformModule.init(RuntimeEnvironment.getApplication(), true);
    }

    // Verifies that a fully populated pw_live object parses every field. progress/when/length arrive as
    // strings — that is the device wire shape (the backend re-serializes those proto int64 fields to JSON
    // strings; the send-live-push.bash request sends JSON numbers, the backend converts them). The parser
    // coerces them back: progress "42" → 42, when "1733740800000" → 1733740800000L, segment length "3" → 3.
    @Test
    public void testParsePopulatesAllFieldsAndCoercesNumericStrings() {
        Bundle b = new Bundle();
        b.putString("header", "Order #4521");
        b.putString("title", "Cooking");
        b.putString("ci", "https://example.com/icon.png");
        b.putString(
                "pw_live",
                "{"
                        + "\"op\":\"OPERATION_UPDATE\","
                        + "\"id\":\"order_4521\","
                        + "\"progress\":\"42\","
                        + "\"progress_indeterminate\":true,"
                        + "\"progress_bar\":false,"
                        + "\"segments\":[{\"color\":\"#00FF00\",\"length\":\"3\"}],"
                        + "\"extras\":{\"eta\":\"15:30\"},"
                        + "\"when\":\"1733740800000\","
                        + "\"chronometer\":true,"
                        + "\"chronometer_count_down\":true,"
                        + "\"show_when\":false"
                        + "}");

        LiveUpdateState s = LiveUpdateStateParser.parse(b);

        assertNotNull(s);
        assertEquals("order_4521", s.getActivityId());
        assertEquals(LiveUpdateOperation.UPDATE, s.getOperation());
        assertEquals("Order #4521", s.getTitle());
        assertEquals("Cooking", s.getSubtitle());
        assertEquals("https://example.com/icon.png", s.getIconUrl());
        assertEquals(Integer.valueOf(42), s.getProgress());
        assertTrue(s.isProgressIndeterminate());
        assertFalse(s.showProgressBar());
        assertEquals(1, s.getSegments().size());
        assertEquals(3, s.getSegments().get(0).getLength());
        assertNotNull(s.getExtras());
        assertEquals("15:30", s.getExtras().optString("eta"));
        assertEquals(Long.valueOf(1733740800000L), s.getWhen());
        assertTrue(s.isChronometer());
        assertTrue(s.isChronometerCountDown());
        assertFalse(s.isShowWhen());
    }

    // Verifies that a minimal pw_live (op + id only) yields contract defaults: progress/when null
    // (not 0), progress_bar/show_when true, other bools false, segments and actions empty.
    @Test
    public void testParseMinimalAppliesDefaults() {
        Bundle b = pwLive("{\"op\":\"OPERATION_UPDATE\",\"id\":\"order_4521\"}");

        LiveUpdateState s = LiveUpdateStateParser.parse(b);

        assertNotNull(s);
        assertEquals("order_4521", s.getActivityId());
        assertEquals(LiveUpdateOperation.UPDATE, s.getOperation());
        assertNull(s.getProgress());
        assertNull(s.getWhen());
        assertTrue(s.showProgressBar());
        assertTrue(s.isShowWhen());
        assertFalse(s.isProgressIndeterminate());
        assertFalse(s.isChronometer());
        assertFalse(s.isChronometerCountDown());
        assertTrue(s.getSegments().isEmpty());
        assertTrue(s.getActions().isEmpty());
    }

    // Verifies that each full OPERATION_* enum name maps to its operation, while a short form
    // ("start") or garbage ("pause") makes parse return null (push consumed, no crash).
    @Test
    public void testParseMapsOperationOnlyForFullEnumNames() {
        assertEquals(
                LiveUpdateOperation.START,
                LiveUpdateStateParser.parse(pwLive("{\"op\":\"OPERATION_START\",\"id\":\"x\"}"))
                        .getOperation());
        assertEquals(
                LiveUpdateOperation.UPDATE,
                LiveUpdateStateParser.parse(pwLive("{\"op\":\"OPERATION_UPDATE\",\"id\":\"x\"}"))
                        .getOperation());
        assertEquals(
                LiveUpdateOperation.END,
                LiveUpdateStateParser.parse(pwLive("{\"op\":\"OPERATION_END\",\"id\":\"x\"}"))
                        .getOperation());

        assertNull(LiveUpdateStateParser.parse(pwLive("{\"op\":\"start\",\"id\":\"x\"}")));
        assertNull(LiveUpdateStateParser.parse(pwLive("{\"op\":\"pause\",\"id\":\"x\"}")));
    }

    // Verifies that a missing op, a literal-null op, a missing id, an empty id, or a literal-null id
    // all make parse return null. The literal-null cases matter because optString coerces a JSON null
    // to the string "null", which would otherwise pass the checks below (op "null" matches no
    // OPERATION_*; id "null" would yield an activityId of "null"); optStringOrNull treats null as absent.
    @Test
    public void testParseReturnsNullWhenRequiredFieldsMissing() {
        assertNull(LiveUpdateStateParser.parse(pwLive("{\"id\":\"x\"}")));
        assertNull(LiveUpdateStateParser.parse(pwLive("{\"op\":null,\"id\":\"x\"}")));
        assertNull(LiveUpdateStateParser.parse(pwLive("{\"op\":\"OPERATION_START\"}")));
        assertNull(LiveUpdateStateParser.parse(pwLive("{\"op\":\"OPERATION_START\",\"id\":\"\"}")));
        assertNull(LiveUpdateStateParser.parse(pwLive("{\"op\":\"OPERATION_START\",\"id\":null}")));
    }

    // Verifies that a literal JSON null for an optional numeric field is treated as absent: progress
    // and when come back null (not 0 / 0L), so downstream the bar stays unset and no timestamp is set
    // rather than a 0% bar / Jan-1970 stamp. Guards the has(key) && !isNull(key) branch.
    @Test
    public void testParseTreatsLiteralNullNumericAsAbsent() {
        Bundle b = pwLive("{\"op\":\"OPERATION_UPDATE\",\"id\":\"x\",\"progress\":null,\"when\":null}");

        LiveUpdateState s = LiveUpdateStateParser.parse(b);

        assertNotNull(s);
        assertNull(s.getProgress());
        assertNull(s.getWhen());
    }

    // Verifies that malformed JSON in pw_live returns null without throwing and logs an error.
    @Test
    public void testParseReturnsNullAndLogsOnMalformedJson() {
        Bundle b = pwLive("not-a-json");

        try (MockedStatic<PWLog> log = mockStatic(PWLog.class)) {
            LiveUpdateState s = LiveUpdateStateParser.parse(b);

            assertNull(s);
            log.verify(() -> PWLog.error(anyString(), anyString()));
        }
    }

    // Verifies that a null bundle and an empty pw_live string both return null.
    @Test
    public void testParseReturnsNullForNullBundleAndEmptyPayload() {
        assertNull(LiveUpdateStateParser.parse(null));

        Bundle empty = new Bundle();
        empty.putString("pw_live", "");
        assertNull(LiveUpdateStateParser.parse(empty));
    }

    // Verifies that a segment element missing its required "color" is skipped while the rest of the
    // array still parses successfully.
    @Test
    public void testParseSkipsSegmentMissingColorAndKeepsRest() {
        Bundle b = pwLive("{\"op\":\"OPERATION_UPDATE\",\"id\":\"x\",\"segments\":["
                + "{\"length\":\"2\"},"
                + "{\"color\":\"#00FF00\",\"length\":\"3\"}"
                + "]}");

        LiveUpdateState s = LiveUpdateStateParser.parse(b);

        assertNotNull(s);
        List<LiveUpdateSegment> segments = s.getSegments();
        assertEquals(1, segments.size());
        assertEquals(
                android.graphics.Color.parseColor("#00FF00"), segments.get(0).getColor());
        assertEquals(3, segments.get(0).getLength());
    }

    // Verifies that a segments value that is not an array yields an empty list, parse still succeeds.
    @Test
    public void testParseTreatsNonArraySegmentsAsEmpty() {
        Bundle b = pwLive("{\"op\":\"OPERATION_UPDATE\",\"id\":\"x\",\"segments\":\"not-an-array\"}");

        LiveUpdateState s = LiveUpdateStateParser.parse(b);

        assertNotNull(s);
        assertTrue(s.getSegments().isEmpty());
    }

    // Verifies that progress present as "0" parses to Integer 0 — a present value, not treated as
    // absent (which would be null). Guards the has(key) ? optInt : null branch.
    @Test
    public void testParseProgressZeroIsPresentValueNotNull() {
        Bundle b = pwLive("{\"op\":\"OPERATION_UPDATE\",\"id\":\"x\",\"progress\":\"0\"}");

        LiveUpdateState s = LiveUpdateStateParser.parse(b);

        assertNotNull(s);
        assertEquals(Integer.valueOf(0), s.getProgress());
    }

    // Verifies that isLiveUpdatePush keys off the pw_live marker: true with pw_live, false for a
    // bundle carrying only the OLD flat pw_live_op, false for an unrelated bundle, false for null.
    @Test
    public void testIsLiveUpdatePushRecognizesByPwLiveKey() {
        Bundle yes = pwLive("{\"op\":\"OPERATION_START\",\"id\":\"x\"}");
        assertTrue(LiveUpdateStateParser.isLiveUpdatePush(yes));

        Bundle oldFlat = new Bundle();
        oldFlat.putString("pw_live_op", "start");
        assertFalse(LiveUpdateStateParser.isLiveUpdatePush(oldFlat));

        Bundle unrelated = new Bundle();
        unrelated.putString("title", "hi");
        assertFalse(LiveUpdateStateParser.isLiveUpdatePush(unrelated));

        assertFalse(LiveUpdateStateParser.isLiveUpdatePush(null));
    }

    private static Bundle pwLive(String json) {
        Bundle b = new Bundle();
        b.putString("pw_live", json);
        return b;
    }
}
