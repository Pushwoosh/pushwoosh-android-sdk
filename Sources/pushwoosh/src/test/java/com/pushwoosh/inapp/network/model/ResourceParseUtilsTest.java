/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
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

import static org.junit.Assert.assertThrows;

import com.pushwoosh.inapp.exception.ResourceParseException;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by aevstefeev on 07/03/2018.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class ResourceParseUtilsTest {

    @Test
    public void parseRichMedia() throws Exception {
        String richMediaString =
                "{\"url\":\"https:\\/\\/inapp.pushwoosh.com\\/json\\/1.3\\/getInApp\\/FE293-BA62E-1\",\"ts\":1000, \"hash\":\"hash_test\","
                        + "\"code\":\"FE293-BA62E\",\"layout\":\"centerbox\",\"updated\":1465292957,\"closeButtonType\":\"1\", "
                        + "\"tags\": {\"key1\":1, \"key2\":true, \"key3\":\"value\"}}";
        Resource resource = ResourceParseUtils.parseRichMedia(richMediaString);

        Assert.assertEquals("r-FE293-BA62E-1", resource.getCode());
        Assert.assertEquals("https://inapp.pushwoosh.com/json/1.3/getInApp/FE293-BA62E-1", resource.getUrl());
        Assert.assertEquals("hash_test", resource.getHash());
        InAppLayout layout = resource.getLayout();
        Assert.assertEquals("topbanner", layout.getCode());
        Assert.assertEquals("TOP", layout.name());
        Assert.assertEquals(1000, resource.getUpdated());
        Assert.assertEquals(0, resource.getPriority());

        Map<String, String> tags = resource.getTags();
        Assert.assertEquals("1", tags.get("key1"));
        Assert.assertEquals("true", tags.get("key2"));
        Assert.assertEquals("value", tags.get("key3"));
    }

    @Test
    public void convertTags() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", 1);
        map.put("key2", 2L);
        map.put("key3", "string");
        map.put("key4", true);

        Map<String, String> resultMap = ResourceParseUtils.convertTags(map);

        Assert.assertEquals("1", resultMap.get("key1"));
        Assert.assertEquals("2", resultMap.get("key2"));
        Assert.assertEquals("string", resultMap.get("key3"));
        Assert.assertEquals("true", resultMap.get("key4"));
    }

    // Verifies that parseRichMedia strips file extension from URL last path segment and prefixes code with "r-".
    @Test
    public void parseRichMedia_stripsFileExtensionFromCode() throws Exception {
        String richMedia = "{\"url\":\"https://richmedia.pushwoosh.com/C/A/CAF38-1F50B.zip?ts=1730321891\","
                + "\"ts\":1730321891,\"hash\":\"d16cfc240fdbb9530acdb9908f4c0204\"}";

        Resource resource = ResourceParseUtils.parseRichMedia(richMedia);

        Assert.assertEquals("r-CAF38-1F50B", resource.getCode());
        Assert.assertEquals("https://richmedia.pushwoosh.com/C/A/CAF38-1F50B.zip?ts=1730321891", resource.getUrl());
        Assert.assertEquals("d16cfc240fdbb9530acdb9908f4c0204", resource.getHash());
        Assert.assertEquals(1730321891L, resource.getUpdated());
        Assert.assertTrue(resource.getTags().isEmpty());
        Assert.assertFalse(resource.isRequired());
        Assert.assertEquals(0, resource.getPriority());
    }

    // Verifies that parseRichMedia propagates explicit required and priority fields into Resource.
    @Test
    public void parseRichMedia_propagatesRequiredAndPriority() throws Exception {
        String richMedia = "{\"url\":\"https://richmedia.pushwoosh.com/A/B/code\","
                + "\"ts\":1000,\"required\":true,\"priority\":7}";

        Resource resource = ResourceParseUtils.parseRichMedia(richMedia);

        Assert.assertTrue(resource.isRequired());
        Assert.assertEquals(7, resource.getPriority());
    }

    // Verifies that parseRichMedia returns an empty tags map when tags field is missing from payload.
    @Test
    public void parseRichMedia_missingTagsField_returnsEmptyTags() throws Exception {
        String richMedia =
                "{\"ts\":1761804213,\"url\":\"https://richmedia.pushwoosh.com/8/5/8516C-47D37.zip?ts=1761804213\"}";

        Resource resource = ResourceParseUtils.parseRichMedia(richMedia);

        Assert.assertTrue(resource.getTags().isEmpty());
        Assert.assertEquals("r-8516C-47D37", resource.getCode());
    }

    // Verifies that parseRichMedia wraps invalid JSON into ResourceParseException with non-null cause.
    @Test
    public void parseRichMedia_invalidJson_throwsResourceParseException() {
        ResourceParseException ex =
                assertThrows(ResourceParseException.class, () -> ResourceParseUtils.parseRichMedia("not a json"));

        Assert.assertTrue(ex.getMessage().contains("Can't parse richMedia"));
        Assert.assertNotNull(ex.getCause());
    }

    // Verifies that parseRichMedia throws ResourceParseException with JSONException cause when ts is missing.
    @Test
    public void parseRichMedia_missingTimestamp_throwsResourceParseException() {
        String richMedia = "{\"url\":\"https://richmedia.pushwoosh.com/A/B/code\"}";

        ResourceParseException ex =
                assertThrows(ResourceParseException.class, () -> ResourceParseUtils.parseRichMedia(richMedia));

        Assert.assertTrue(ex.getCause() instanceof JSONException);
    }

    // Verifies that parseRichMedia throws ResourceParseException when required url field is missing.
    @Test
    public void parseRichMedia_missingUrl_throwsResourceParseException() {
        String richMedia = "{\"ts\":1000}";

        ResourceParseException ex =
                assertThrows(ResourceParseException.class, () -> ResourceParseUtils.parseRichMedia(richMedia));

        Assert.assertNotNull(ex.getCause());
    }

    // Verifies that parseRichMedia wraps IllegalArgumentException when URL has no last path segment.
    @Test
    public void parseRichMedia_urlWithoutLastPathSegment_throwsResourceParseException() {
        String url = "https://richmedia.pushwoosh.com/";
        String richMedia = "{\"ts\":1000,\"url\":\"" + url + "\"}";

        ResourceParseException ex =
                assertThrows(ResourceParseException.class, () -> ResourceParseUtils.parseRichMedia(richMedia));

        Assert.assertTrue(ex.getCause() instanceof IllegalArgumentException);
        Assert.assertTrue(ex.getCause().getMessage().contains(url));
    }

    // Verifies that convertTags returns empty map for null input instead of throwing NPE.
    @Test
    public void convertTags_nullInput_returnsEmptyMap() {
        Map<String, String> result = ResourceParseUtils.convertTags(null);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    // Verifies that convertTags skips entries whose values are null and converts remaining values to strings.
    @Test
    public void convertTags_skipsNullValues() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("a", "x");
        input.put("b", null);
        input.put("c", 42);

        Map<String, String> result = ResourceParseUtils.convertTags(input);

        Assert.assertEquals(2, result.size());
        Assert.assertFalse(result.containsKey("b"));
        Assert.assertEquals("x", result.get("a"));
        Assert.assertEquals("42", result.get("c"));
    }
}
