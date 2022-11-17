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

package com.pushwoosh.repository;

import com.pushwoosh.tags.TagsBundle;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class SetTagsRequestTest {

    @Test
    public void buildParams() throws JSONException {
        JSONObject json = new JSONObject("{\"key1\":\"1\", \"key2\":\"#pwinc#111\"}");
        SetTagsRequest setTagsRequest = new SetTagsRequest(json);

        JSONObject result = new JSONObject();
        setTagsRequest.buildParams(result);
        JSONObject tags = (JSONObject) result.get("tags");
        Assert.assertEquals("1", tags.getString("key1"));
        TagsBundle tagsBundle = (TagsBundle) tags.get("key2");
        Assert.assertEquals("{\"key2\":{\"operation\":\"increment\",\"value\":111}}", tagsBundle.toJson().toString());
    }
}