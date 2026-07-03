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

package com.pushwoosh.firebase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.firebase.messaging.RemoteMessage;
import com.pushwoosh.firebase.internal.RemoteMessageUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

/**
 * Regression guard for crash candidate #15 (crash-fcmhelper-ispushwooshmessage-null).
 *
 * A host with a custom FirebaseMessagingService can pass a null RemoteMessage straight into the
 * public, unannotated helper PushwooshFcmHelper.isPushwooshMessage(RemoteMessage), which forwards to
 * RemoteMessageUtils.isPushwooshMessage. That used to dereference the null receiver at
 * remoteMessage.getData() -> NullPointerException. The fix guards the receiver: a null message is, by
 * definition, not a Pushwoosh message, so the call now returns false gracefully (same result as a
 * non-Pushwoosh message) instead of crashing the host.
 *
 * These tests assert that graceful behaviour through both the public helper and the internal util,
 * and pin the necessary condition (null receiver) with negative controls so the guard cannot silently
 * widen to also reject valid non-null messages.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class PushwooshFcmHelperIsPushwooshMessageNullTest {

    // Verifies that the public host-facing helper returns false (no throw) for a null message.
    @Test
    public void publicHelper_nullMessage_returnsFalseGracefully() {
        assertFalse(
                "null message through the public helper must return false, not throw",
                PushwooshFcmHelper.isPushwooshMessage(null));
    }

    // Verifies that the internal crash point itself returns false (no throw) for a null message.
    @Test
    public void internalUtil_nullMessage_returnsFalseGracefully() {
        assertFalse(
                "null message at the internal crash point must return false, not throw",
                RemoteMessageUtils.isPushwooshMessage(null));
    }

    // ---- negative controls: the guard must reject ONLY null, not valid non-null messages ----

    @Test
    public void negativeControl_pushwooshMessage_noThrow() {
        Map<String, String> data = new HashMap<>();
        data.put("pw_msg", "1");
        RemoteMessage remoteMessage = Mockito.mock(RemoteMessage.class);
        Mockito.when(remoteMessage.getData()).thenReturn(data);

        assertTrue(
                "non-null pushwoosh message must return true, not throw",
                PushwooshFcmHelper.isPushwooshMessage(remoteMessage));
    }

    @Test
    public void negativeControl_nonPushwooshMessage_noThrow() {
        Map<String, String> data = new HashMap<>();
        data.put("other_key", "1");
        RemoteMessage remoteMessage = Mockito.mock(RemoteMessage.class);
        Mockito.when(remoteMessage.getData()).thenReturn(data);

        assertFalse(
                "non-null non-pushwoosh message must return false, not throw",
                PushwooshFcmHelper.isPushwooshMessage(remoteMessage));
    }
}
