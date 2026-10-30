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

package com.pushwoosh.location.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.pushwoosh.location.geofencer.GeofenceReceiver;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.List;

/**
 * Regression guard for SDK-1127: the geofence {@code PendingIntent} carried {@code FLAG_IMMUTABLE}
 * since 6.3.0, which drops the fill-in extras GMS attaches at send time, so
 * {@code GeofencingEvent.fromIntent()} always saw an empty intent.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 30)
@LooperMode(LooperMode.Mode.LEGACY)
public class GeofencePendingIntentMutabilityTest {

    private static final String PROBE_KEY = "pw_fill_in_probe";

    private void assertFillInExtraReachesReceiver() throws PendingIntent.CanceledException {
        Context context = RuntimeEnvironment.getApplication();
        PendingIntent pendingIntent = LocationModule.createGeofencePendingIntent(context);

        Intent fillIn = new Intent();
        fillIn.putExtra(PROBE_KEY, "transition");
        pendingIntent.send(context, 0, fillIn);

        List<Intent> broadcasts = shadowOf(RuntimeEnvironment.getApplication()).getBroadcastIntents();
        Intent delivered = broadcasts.get(broadcasts.size() - 1);
        assertEquals("transition", delivered.getStringExtra(PROBE_KEY));
        assertEquals(GeofenceReceiver.class.getName(), delivered.getComponent().getClassName());
    }

    @Test
    @Config(sdk = 30)
    public void testSenderExtrasReachReceiverWhenSdkBelowS() throws Exception {
        assertFillInExtraReachesReceiver();
    }

    @Test
    @Config(sdk = 35)
    public void testSenderExtrasReachReceiverWhenSdkAtLeastS() throws Exception {
        assertFillInExtraReachesReceiver();
    }

    @Test
    @Config(sdk = 35)
    public void testMutableFlagIsSetWhenSdkAtLeastS() {
        PendingIntent pendingIntent = LocationModule.createGeofencePendingIntent(RuntimeEnvironment.getApplication());

        assertTrue((shadowOf(pendingIntent).getFlags() & PendingIntent.FLAG_MUTABLE) != 0);
    }

    @Test
    public void testFactoryReturnsNullWhenContextIsNull() {
        assertNull(LocationModule.createGeofencePendingIntent(null));
    }
}
