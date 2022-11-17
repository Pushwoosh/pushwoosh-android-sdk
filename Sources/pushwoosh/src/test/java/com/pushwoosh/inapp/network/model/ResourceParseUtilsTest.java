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

import com.pushwoosh.BuildConfig;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by aevstefeev on 07/03/2018.
 */
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(constants = BuildConfig.class)
public class ResourceParseUtilsTest {

    @Test
    public void parseRichMedia() throws Exception {
        String richMediaString = "{\"url\":\"https:\\/\\/inapp.pushwoosh.com\\/json\\/1.3\\/getInApp\\/FE293-BA62E-1\",\"ts\":1000, \"hash\":\"hash_test\","
                + "\"code\":\"FE293-BA62E\",\"layout\":\"centerbox\",\"updated\":1465292957,\"closeButtonType\":\"1\", " +
                "\"tags\": {\"key1\":1, \"key2\":true, \"key3\":\"value\"}}";
        Resource resource = ResourceParseUtils.parseRichMedia(richMediaString);

        Assert.assertEquals("r-FE293-BA62E-1",resource.getCode());
        Assert.assertEquals("https://inapp.pushwoosh.com/json/1.3/getInApp/FE293-BA62E-1", resource.getUrl());
        Assert.assertEquals("hash_test",resource.getHash());
        InAppLayout layout = resource.getLayout();
        Assert.assertEquals("topbanner",layout.getCode());
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

}