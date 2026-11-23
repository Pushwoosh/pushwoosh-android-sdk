/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
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

import static com.pushwoosh.testutil.ReceiptTime.RECEIVED_AT_SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class MessageDeliveredRequestTest {

    @Test
    public void buildParams_withTimestamp_putsIntegerSeconds() throws Exception {
        MessageDeliveredRequest request = new MessageDeliveredRequest("h", "md", RECEIVED_AT_SECONDS);
        JSONObject params = new JSONObject();

        request.buildParams(params);

        assertTrue(params.get("timestampUTC") instanceof Long);
        assertEquals(RECEIVED_AT_SECONDS, params.getLong("timestampUTC"));
        assertEquals("h", params.getString("hash"));
        assertEquals("md", params.getString("metaData"));
        assertEquals(3, params.length());
    }

    @Test
    public void buildParams_withoutTimestamp_omitsField() throws Exception {
        JSONObject withNull = new JSONObject();
        JSONObject twoArg = new JSONObject();

        new MessageDeliveredRequest("h", "md", null).buildParams(withNull);
        new MessageDeliveredRequest("h", "md").buildParams(twoArg);

        assertFalse(withNull.has("timestampUTC"));
        assertEquals(2, withNull.length());
        assertFalse(twoArg.has("timestampUTC"));
        assertEquals(2, twoArg.length());
    }
}
