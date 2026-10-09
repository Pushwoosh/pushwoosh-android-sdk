package com.pushwoosh.featureflags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.pushwoosh.internal.network.PushRequestHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class GetFeatureFlagsRequestTest {

    private static Set<String> keysOf(JSONObject json) {
        Set<String> keys = new HashSet<>();
        Iterator<String> it = json.keys();
        while (it.hasNext()) {
            keys.add(it.next());
        }
        return keys;
    }

    @Test
    public void body_withoutEtag_hasOnlyApplicationAndHwid() throws Exception {
        JSONObject params = PushRequestHelper.getParams(new GetFeatureFlagsRequest("70699-8A0F4", "hwid-1", ""));
        assertEquals(new HashSet<>(Arrays.asList("application", "hwid")), keysOf(params));
        assertEquals("70699-8A0F4", params.getString("application"));
        assertEquals("hwid-1", params.getString("hwid"));
    }

    @Test
    public void body_withEtag_hasExactlyThreeFields() throws Exception {
        JSONObject params = PushRequestHelper.getParams(new GetFeatureFlagsRequest("70699-8A0F4", "hwid-1", "e1"));
        assertEquals(new HashSet<>(Arrays.asList("application", "hwid", "etag")), keysOf(params));
        assertEquals("e1", params.getString("etag"));
    }

    @Test
    public void parse_liveProdEmptyAnswer() throws Exception {
        FeatureFlagsResponse r = new GetFeatureFlagsRequest("a", "h", "")
                .parseResponse(
                        new JSONObject(
                                "{\"etag\":\"44136fa355b3678a1146ad16f7e8649e\",\"flags\":{},\"not_modified\":false,\"partial\":false}"));
        assertTrue(r.flags.isEmpty());
        assertEquals("44136fa355b3678a1146ad16f7e8649e", r.etag);
        assertFalse(r.notModified);
        assertFalse(r.partial);
    }

    @Test
    public void parse_flagsAndRawJsonKept() throws Exception {
        FeatureFlagsResponse r = FeatureFlagsResponse.from(new JSONObject(
                "{\"flags\":{\"new_checkout\":{\"enabled\":true,\"rule_id\":\"r_784227d4\",\"properties\":{\"color\":\"green\"}}},"
                        + "\"etag\":\"9f2c\",\"not_modified\":false,\"partial\":false}"));
        assertTrue(r.flags.get("new_checkout").isEnabled());
        assertTrue(r.flagsJson.has("new_checkout"));
    }

    @Test
    public void parse_notModified_withEmptyFlags() throws Exception {
        FeatureFlagsResponse r = FeatureFlagsResponse.from(
                new JSONObject("{\"flags\":{},\"etag\":\"e1\",\"not_modified\":true,\"partial\":false}"));
        assertTrue(r.notModified);
    }

    @Test
    public void parse_partialWithoutEtag() throws Exception {
        FeatureFlagsResponse r =
                FeatureFlagsResponse.from(new JSONObject("{\"flags\":{},\"not_modified\":false,\"partial\":true}"));
        assertTrue(r.partial);
        assertEquals("", r.etag);
    }

    @Test(expected = JSONException.class)
    public void from_missingFlags_throws() throws Exception {
        FeatureFlagsResponse.from(new JSONObject("{\"etag\":\"e\",\"not_modified\":false,\"partial\":false}"));
    }
}
