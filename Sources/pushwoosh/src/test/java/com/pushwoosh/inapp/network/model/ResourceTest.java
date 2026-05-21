/*
 *
 * Copyright (c) 2026. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.inapp.network.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.pushwoosh.inapp.exception.ResourceParseException;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class ResourceTest {

    // Verifies that JSONObject constructor applies defaults when optional fields are missing.
    @Test
    public void jsonConstructor_missingOptionalFields_appliesDefaults() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("code", "X");
        json.put("url", "https://example.com");
        json.put("updated", 0L);

        Resource resource = new Resource(json);

        assertEquals("", resource.getHash());
        assertFalse(resource.isRequired());
        assertEquals(0, resource.getPriority());
    }

    // Verifies that url-only constructor initializes Resource with FULLSCREEN layout and default fields.
    @Test
    public void urlConstructor_setsFullscreenLayoutAndDefaults() {
        Resource resource = new Resource("http://x");

        assertEquals("", resource.getCode());
        assertEquals("http://x", resource.getUrl());
        assertEquals(InAppLayout.FULLSCREEN, resource.getLayout());
        assertFalse(resource.isRequired());
        assertEquals(-1, resource.getPriority());
        assertTrue(resource.getTags().isEmpty());
    }

    // Verifies that the full constructor converts Map<String,Object> tags to strings and drops null values.
    @Test
    public void fullConstructor_convertsTags_filtersNullValues() {
        Map<String, Object> tags = new HashMap<>();
        tags.put("a", 1);
        tags.put("b", "x");
        tags.put("c", null);

        Resource resource = new Resource("code", "url", "hash", 0L, InAppLayout.TOP, tags, false, 0);

        Map<String, String> result = resource.getTags();
        assertEquals(2, result.size());
        assertEquals("1", result.get("a"));
        assertEquals("x", result.get("b"));
        assertFalse(result.containsKey("c"));
    }

    // Verifies that getTags returns a defensive copy so external mutation does not affect Resource state.
    @Test
    public void getTags_returnsDefensiveCopy() {
        Map<String, Object> tags = new HashMap<>();
        tags.put("a", "1");
        Resource resource = new Resource("code", "url", "hash", 0L, InAppLayout.TOP, tags, false, 0);

        Map<String, String> snapshot = resource.getTags();
        snapshot.clear();

        Map<String, String> reread = resource.getTags();
        assertEquals(1, reread.size());
        assertEquals("1", reread.get("a"));
    }

    // Verifies that null tags passed to the constructor result in an empty tags map.
    @Test
    public void fullConstructor_nullTags_yieldsEmptyMap() {
        Resource resource = new Resource("code", "url", "hash", 0L, InAppLayout.TOP, null, false, 0);

        assertTrue(resource.getTags().isEmpty());
    }

    // Verifies that isInApp classifies codes correctly based on presence and the "r-" rich-media prefix.
    @Test
    public void isInApp_classifiesByPrefixAndEmptiness() {
        List<Object[]> cases = Arrays.asList(
                new Object[] {"abc123", true}, new Object[] {"r-CAF38-1F50B", false}, new Object[] {"", false});

        for (Object[] row : cases) {
            String code = (String) row[0];
            boolean expected = (Boolean) row[1];
            Resource resource = code.isEmpty() ? new Resource("http://x") : new Resource(code, false);
            assertEquals("isInApp for code='" + code + "'", expected, resource.isInApp());
        }
    }

    // Verifies that compareTo orders resources by required-first, then priority DESC, then code ASC with null-code
    // last.
    @Test
    public void compareTo_orderingRules() {
        Resource r1;
        Resource r2;

        r1 = new Resource("a", true);
        r2 = new Resource("b", false);
        assertTrue("required ahead of non-required", r1.compareTo(r2) < 0);
        assertTrue("non-required behind required", r2.compareTo(r1) > 0);

        r1 = new Resource("code", "url", "hash", 0L, InAppLayout.TOP, null, false, 10);
        r2 = new Resource("code", "url", "hash", 0L, InAppLayout.TOP, null, false, 5);
        assertTrue("higher priority first", r1.compareTo(r2) < 0);

        r1 = new Resource("aaa", "url", "hash", 0L, InAppLayout.TOP, null, true, 1);
        r2 = new Resource("bbb", "url", "hash", 0L, InAppLayout.TOP, null, true, 1);
        assertTrue("same priority, code ASC", r1.compareTo(r2) < 0);

        r1 = new Resource(null, "url", "hash", 0L, InAppLayout.TOP, null, false, 1);
        r2 = new Resource("x", "url", "hash", 0L, InAppLayout.TOP, null, false, 1);
        assertEquals("null code goes after", 1, r1.compareTo(r2));

        r1 = new Resource("same", "url", "hash", 0L, InAppLayout.TOP, null, false, 1);
        r2 = new Resource("same", "url", "hash", 0L, InAppLayout.TOP, null, false, 1);
        assertEquals("equal sort order", 0, r1.compareTo(r2));
    }

    // Verifies that parseRichMedia delegates to ResourceParseUtils and yields a Resource with the "r-" prefix.
    @Test
    public void parseRichMedia_delegatesToParseUtils() throws ResourceParseException {
        String json = "{\"url\":\"https://richmedia.pushwoosh.com/8/5/8516C-47D37.zip?ts=1761804213\","
                + "\"ts\":1761804213}";

        Resource resource = Resource.parseRichMedia(json);

        assertNotNull(resource);
        assertTrue(
                "code must start with r-: " + resource.getCode(),
                resource.getCode().startsWith("r-"));
        assertEquals("https://richmedia.pushwoosh.com/8/5/8516C-47D37.zip?ts=1761804213", resource.getUrl());
        assertEquals(1761804213L, resource.getUpdated());
        assertEquals(InAppLayout.TOP, resource.getLayout());
    }

    // Verifies that parseRichMedia throws ResourceParseException on malformed input.
    @Test
    public void parseRichMedia_invalidInput_throws() {
        assertThrows(ResourceParseException.class, () -> Resource.parseRichMedia("not a json"));
    }
}
