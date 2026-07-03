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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;

import com.google.firebase.messaging.RemoteMessage;
import com.pushwoosh.firebase.internal.mapper.RemoteMessageMapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

/**
 * Regression guard for crash candidate #16 (crash-fcmhelper-messagetobundle-null).
 *
 * A host with custom message-processing logic can pass a null RemoteMessage straight into the public,
 * unannotated helper PushwooshFcmHelper.messageToBundle(RemoteMessage), which forwards to
 * RemoteMessageMapper.mapToBundle. That used to dereference the null receiver at
 * remoteMessage.getData() -> NullPointerException. The fix guards the receiver: a null message now
 * yields an empty Bundle gracefully (same result as a message with no data) instead of crashing the
 * host.
 *
 * These tests assert that graceful behaviour through both the public helper and the internal crash
 * point, and pin the necessary condition (null receiver) with negative controls so the guard cannot
 * silently widen to also drop data from valid non-null messages.
 *
 * Direct sibling of #15 (crash-fcmhelper-ispushwooshmessage-null): same unguarded-delegate defect on
 * the other thin public helper of PushwooshFcmHelper.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class PushwooshFcmHelperMessageToBundleNullTest {

    // Verifies that the public host-facing helper returns an empty Bundle (no throw) for a null message.
    @Test
    public void publicHelper_nullMessage_returnsEmptyBundleGracefully() {
        Bundle bundle = PushwooshFcmHelper.messageToBundle(null);
        assertNotNull("null message through the public helper must return a Bundle, not throw", bundle);
        assertTrue("null message must produce an empty Bundle", bundle.isEmpty());
    }

    // Verifies that the internal crash point itself returns an empty Bundle (no throw) for a null message.
    @Test
    public void internalMapper_nullMessage_returnsEmptyBundleGracefully() {
        Bundle bundle = RemoteMessageMapper.mapToBundle(null);
        assertNotNull("null message at the internal crash point must return a Bundle, not throw", bundle);
        assertTrue("null message must produce an empty Bundle", bundle.isEmpty());
    }

    // ---- negative controls: the guard must affect ONLY null, not valid non-null messages ----

    @Test
    public void negativeControl_messageWithData_noThrow() {
        Map<String, String> data = new HashMap<>();
        data.put("pw_msg", "1");
        data.put("title", "hello");
        RemoteMessage remoteMessage = Mockito.mock(RemoteMessage.class);
        Mockito.when(remoteMessage.getData()).thenReturn(data);

        Bundle bundle = PushwooshFcmHelper.messageToBundle(remoteMessage);
        assertNotNull("non-null message must produce a Bundle, not throw", bundle);
        assertTrue("data keys must be carried into the bundle", bundle.containsKey("pw_msg"));
    }

    @Test
    public void negativeControl_emptyData_noThrow() {
        RemoteMessage remoteMessage = Mockito.mock(RemoteMessage.class);
        Mockito.when(remoteMessage.getData()).thenReturn(new HashMap<>());

        Bundle bundle = PushwooshFcmHelper.messageToBundle(remoteMessage);
        assertNotNull("empty-data message must produce an empty Bundle, not throw", bundle);
    }

    @Test
    public void negativeControl_nullData_noThrow() {
        RemoteMessage remoteMessage = Mockito.mock(RemoteMessage.class);
        Mockito.when(remoteMessage.getData()).thenReturn(null);

        Bundle bundle = PushwooshFcmHelper.messageToBundle(remoteMessage);
        assertNotNull("null data (but non-null receiver) is handled by the if(data!=null) guard, not throw", bundle);
    }
}
