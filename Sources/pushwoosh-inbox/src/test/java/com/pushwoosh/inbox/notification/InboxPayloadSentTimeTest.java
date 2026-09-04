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

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.concurrent.TimeUnit;

/**
 * Pins the timescale the inbox stores in {@code send_date}, which the fresh-push grace in
 * {@code InboxDbHelper.selectNotFromList} compares against the device clock.
 *
 * FCM does stamp a send time, but on the RemoteMessage object — the SDK's mapper copies only
 * {@code getData()} into the bundle, so {@code google.sent_time} never arrives, and even a
 * server-supplied string under that key cannot be read as a long. Both ends of the grace
 * therefore run on the device clock and no clock skew is involved.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class InboxPayloadSentTimeTest {

    private static final long SERVER_TIME_MILLIS = 1_700_000_000_000L;

    @Test
    public void getSentTime_bundleWithoutSentTime_isDeviceClock() {
        long before = System.currentTimeMillis();
        long sentTime = InboxPayloadDataProvider.getSentTime(new Bundle());
        long after = System.currentTimeMillis();

        assertTrue("expected the device clock, got " + sentTime, sentTime >= before && sentTime <= after);
    }

    @Test
    public void getSentTime_stringSentTimeInPayload_staysDeviceClock() {
        // A data payload can only carry strings; Bundle.getLong refuses to read one and
        // returns the default, so a server value under this key cannot leak into send_date.
        Bundle pushBundle = new Bundle();
        pushBundle.putString("google.sent_time", String.valueOf(SERVER_TIME_MILLIS));

        long sentTime = InboxPayloadDataProvider.getSentTime(pushBundle);

        assertNotEquals(SERVER_TIME_MILLIS, sentTime);
        assertTrue(Math.abs(System.currentTimeMillis() - sentTime) < TimeUnit.SECONDS.toMillis(5));
    }
}
