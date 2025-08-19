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

package com.pushwoosh.inapp.network;

import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppStorage;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PostEventResponseTest {

    public static final String CODE_1 = "code1";
    @Mock
    private InAppStorage inAppStorageMock;
    @Mock
    Resource resourceMock;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        InAppModule.setInAppStorage(inAppStorageMock);
    }

    @Test
    public void postEventResponseTest() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", CODE_1);
        jsonObject.put("required", true);

        PostEventResponse postEventResponse = new PostEventResponse(jsonObject);

        Assert.assertEquals("code1", postEventResponse.getCode());
        Assert.assertEquals(true, postEventResponse.isRequired());
    }

    @Test
    public void postEventResponseEmptyCode() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("required", true);

        PostEventResponse postEventResponse = new PostEventResponse(jsonObject);

        Assert.assertEquals("",postEventResponse.getCode());
        Assert.assertEquals(true, postEventResponse.isRequired());
    }

    @Test
    public void postEventResponseEmptyStorage() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", CODE_1);
        jsonObject.put("required", true);
        InAppModule.setInAppStorage(null);

        PostEventResponse postEventResponse = new PostEventResponse(jsonObject);

        Assert.assertEquals("code1", postEventResponse.getCode());
        Assert.assertEquals(true, postEventResponse.isRequired());
    }
}