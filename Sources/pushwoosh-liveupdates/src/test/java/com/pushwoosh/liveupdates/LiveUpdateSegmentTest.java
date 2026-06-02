package com.pushwoosh.liveupdates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class LiveUpdateSegmentTest {

    @Test
    public void fromJson_parsesColor() throws JSONException {
        LiveUpdateSegment s = LiveUpdateSegment.fromJson(new JSONObject("{\"color\":\"#FF8800\"}"));
        assertEquals(android.graphics.Color.parseColor("#FF8800"), s.getColor());
    }

    @Test
    public void fromJson_lengthVariants() throws JSONException {
        // Each row: [json, expected length, label]. Parsing must never throw — the worst case
        // is a silent fallback to 1.
        String[][] cases = {
            {"{\"color\":\"#FF0000\"}", "1", "absent → 1"},
            {"{\"color\":\"#FF0000\",\"length\":2}", "2", "number → 2"},
            {"{\"color\":\"#FF0000\",\"length\":\"2\"}", "2", "numeric string → 2"},
            {"{\"color\":\"#FF0000\",\"length\":\"foo\"}", "1", "non-numeric string → 1 silently"},
            {"{\"color\":\"#FF0000\",\"length\":{\"a\":1}}", "1", "object → 1 silently"},
            {"{\"color\":\"#FF0000\",\"length\":0}", "1", "zero → 1 (clamp)"},
            {"{\"color\":\"#FF0000\",\"length\":-5}", "1", "negative → 1 (clamp)"},
            {"{\"color\":\"#FF0000\",\"length\":7}", "7", "positive → 7"},
        };
        for (String[] tc : cases) {
            LiveUpdateSegment s = LiveUpdateSegment.fromJson(new JSONObject(tc[0]));
            assertEquals(tc[2], Integer.parseInt(tc[1]), s.getLength());
        }
    }

    @Test
    public void fromJson_labelKey_silentlyIgnored() throws JSONException {
        // Old payloads may still carry the dead "label" field — parser must not choke on it.
        LiveUpdateSegment s =
                LiveUpdateSegment.fromJson(new JSONObject("{\"color\":\"#FF0000\",\"label\":\"Cooking\"}"));
        assertEquals(1, s.getLength());
    }

    @Test
    public void fromJson_missingColor_throws() {
        // color is required; absence is a programmer error and must surface, not be swallowed.
        try {
            LiveUpdateSegment.fromJson(new JSONObject("{}"));
            fail("expected JSONException for missing color");
        } catch (JSONException expected) {
            // ok
        }
    }

    @Test
    public void constructor_negativeLength_clampedToOne() {
        LiveUpdateSegment s = new LiveUpdateSegment(android.graphics.Color.RED, -3);
        assertEquals(1, s.getLength());
    }
}
