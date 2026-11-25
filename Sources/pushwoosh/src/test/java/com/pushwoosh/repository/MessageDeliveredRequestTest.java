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

import static com.pushwoosh.testutil.ReceiptTime.ON_MINUTE_RFC3339;
import static com.pushwoosh.testutil.ReceiptTime.ON_MINUTE_SECONDS;
import static com.pushwoosh.testutil.ReceiptTime.RECEIVED_AT_RFC3339;
import static com.pushwoosh.testutil.ReceiptTime.RECEIVED_AT_SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

import java.util.Locale;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class MessageDeliveredRequestTest {
    private Locale savedLocale;
    private TimeZone savedTimeZone;

    @Before
    public void saveDefaults() {
        savedLocale = Locale.getDefault();
        savedTimeZone = TimeZone.getDefault();
    }

    @After
    public void restoreDefaults() {
        Locale.setDefault(savedLocale);
        TimeZone.setDefault(savedTimeZone);
    }

    @Test
    public void buildParams_withReceivedAt_putsRfc3339String() throws Exception {
        JSONObject params = new JSONObject();

        new MessageDeliveredRequest("h", "md", RECEIVED_AT_SECONDS).buildParams(params);

        assertEquals(RECEIVED_AT_RFC3339, params.get("receivedAt"));
        assertFalse(params.has("timestampUTC"));
        assertEquals("h", params.getString("hash"));
        assertEquals("md", params.getString("metaData"));
        assertEquals(3, params.length());
    }

    @Test
    public void buildParams_withoutReceivedAt_omitsField() throws Exception {
        MessageDeliveredRequest[] requests = {
            new MessageDeliveredRequest("h", "md", null),
            new MessageDeliveredRequest("h", "md", 0L),
            new MessageDeliveredRequest("h", "md", -5L),
            new MessageDeliveredRequest("h", "md"),
        };

        for (MessageDeliveredRequest request : requests) {
            JSONObject params = new JSONObject();
            request.buildParams(params);

            assertFalse(params.has("receivedAt"));
            assertFalse(params.has("timestampUTC"));
            assertEquals(2, params.length());
        }
    }

    @Test
    public void receivedAt_keepsZeroSeconds() {
        assertEquals(ON_MINUTE_RFC3339, MessageDeliveredRequest.formatReceivedAt(ON_MINUTE_SECONDS));
    }

    @Test
    public void receivedAt_ignoresDeviceLocaleAndTimeZone() {
        Object[][] environments = {
            {Locale.forLanguageTag("ar-EG"), "Asia/Kathmandu"},
            {Locale.forLanguageTag("th-TH"), "America/St_Johns"},
            {Locale.forLanguageTag("ja-JP-u-ca-japanese"), "Pacific/Kiritimati"},
        };

        for (Object[] environment : environments) {
            Locale.setDefault((Locale) environment[0]);
            TimeZone.setDefault(TimeZone.getTimeZone((String) environment[1]));

            assertEquals(
                    environment[0] + " / " + environment[1],
                    RECEIVED_AT_RFC3339,
                    MessageDeliveredRequest.formatReceivedAt(RECEIVED_AT_SECONDS));
        }
    }
}
