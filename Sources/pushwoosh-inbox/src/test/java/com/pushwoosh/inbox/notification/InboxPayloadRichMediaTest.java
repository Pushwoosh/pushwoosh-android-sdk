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

package com.pushwoosh.inbox.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.pushwoosh.inbox.data.InboxMessageType;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class InboxPayloadRichMediaTest {

    private static final String ZIP = "https://richmedia.pushwoosh.com/0/6/06883-1BC29.zip";

    /** Verifies that an rm object from the push builder is returned as it is. */
    @Test
    public void getRichMedia_object_isReturnedAsIs() throws Exception {
        JSONObject payload = new JSONObject("{\"rm\":{\"url\":\"" + ZIP + "\",\"ts\":\"1784034924\"}}");

        JSONObject richMedia = InboxPayloadDataProvider.getRichMedia(payload);

        assertEquals(ZIP, richMedia.getString("url"));
        assertEquals(1784034924L, richMedia.getLong("ts"));
    }

    /** Verifies that a bare zip URL set through root params becomes a descriptor with its ts query. */
    @Test
    public void getRichMedia_urlString_takesTheTimestampFromTheQuery() throws Exception {
        JSONObject payload = new JSONObject("{\"rm\":\"" + ZIP + "?ts=1784034924\"}");

        JSONObject richMedia = InboxPayloadDataProvider.getRichMedia(payload);

        assertEquals(ZIP + "?ts=1784034924", richMedia.getString("url"));
        assertEquals(1784034924L, richMedia.getLong("ts"));
    }

    /** Verifies that a zip URL without a ts query gets timestamp zero. */
    @Test
    public void getRichMedia_urlStringWithoutTs_getsZeroTimestamp() throws Exception {
        JSONObject payload = new JSONObject("{\"rm\":\"" + ZIP + "\"}");

        assertEquals(0L, InboxPayloadDataProvider.getRichMedia(payload).getLong("ts"));
    }

    /** Verifies that a message whose rm is a bare URL is typed as rich media, not plain. */
    @Test
    public void getInboxType_urlStringRm_isRichMedia() {
        String actionParams = "{\"b\":\"https://picsum.photos/700/350\",\"rm\":\"" + ZIP + "?ts=1784034924\"}";

        assertEquals(InboxMessageType.RICH_MEDIA, InboxPayloadDataProvider.getInboxType(actionParams));
    }

    /** Verifies that a payload without rm has no rich media. */
    @Test
    public void getRichMedia_absent_isNull() throws Exception {
        assertNull(InboxPayloadDataProvider.getRichMedia(new JSONObject("{\"l\":\"https://example.com\"}")));
    }
}
