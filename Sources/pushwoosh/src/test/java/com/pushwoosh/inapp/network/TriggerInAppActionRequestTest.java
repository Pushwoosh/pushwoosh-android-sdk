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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class TriggerInAppActionRequestTest {

    // Restored from cross-check: pins the wire-level endpoint name. Consistent with sibling PushRequest
    // tests (RegisterDeviceRequestTest, RegisterEmailUserRequestTest, RichMediaActionRequestTest).
    // A typo / rename here silently routes to a different backend method.
    @Test
    public void getMethod_returnsTriggerInAppAction() {
        TriggerInAppActionRequest request = new TriggerInAppActionRequest("inapp-1", "hash", "media");

        assertEquals("triggerInAppAction", request.getMethod());
    }

    // Verifies that for a plain inAppCode with messageHash and non-r- richMediaCode, params contain
    // action/code/messageHash without richMediaCode and include both timestamps.
    @Test
    public void buildParams_plainInAppWithHashAndPlainRichMedia_putsActionCodeAndMessageHashOnly()
            throws JSONException {
        TriggerInAppActionRequest request = new TriggerInAppActionRequest("inapp-123", "hash-abc", "media-xyz");

        JSONObject params = new JSONObject();
        request.buildParams(params);

        assertEquals("show", params.getString("action"));
        assertEquals("inapp-123", params.getString("code"));
        assertEquals("hash-abc", params.getString("messageHash"));
        assertFalse(params.has("richMediaCode"));
        assertTrue(params.has("timestampUTC"));
        assertTrue(params.has("timestampCurrent"));
    }

    // Verifies that when inAppCode starts with "r-" the code field is emitted as an empty string and null messageHash
    // is skipped.
    @Test
    public void buildParams_inAppCodeStartsWithRPrefix_emitsEmptyCodeAndSkipsMessageHash() throws JSONException {
        TriggerInAppActionRequest request = new TriggerInAppActionRequest("r-abc123", null, "non-rich-code");

        JSONObject params = new JSONObject();
        request.buildParams(params);

        assertEquals("", params.getString("code"));
        assertFalse(params.has("messageHash"));
    }

    // Verifies that when richMediaCode starts with "r-" the field is added with the "r-" prefix stripped.
    @Test
    public void buildParams_richMediaCodeStartsWithRPrefix_stripsRPrefixAndPutsRichMediaCode() throws JSONException {
        TriggerInAppActionRequest request = new TriggerInAppActionRequest("inapp-1", "h", "r-richcode");

        JSONObject params = new JSONObject();
        request.buildParams(params);

        assertEquals("richcode", params.getString("richMediaCode"));
    }
}
