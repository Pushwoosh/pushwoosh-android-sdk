package com.pushwoosh.liveupdates.internal;

import static org.junit.Assert.*;

import android.app.Notification;
import android.content.Context;

import com.pushwoosh.liveupdates.LiveUpdateOperation;
import com.pushwoosh.liveupdates.LiveUpdateState;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(sdk = 36)
public class DefaultProgressStyleProviderTest {

    private Notification build(Notification.ProgressStyle style) {
        Context ctx = RuntimeEnvironment.getApplication();
        return new Notification.Builder(ctx, "ch")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setStyle(style)
                .build();
    }

    @Test
    public void progressApplied() {
        LiveUpdateState state = new LiveUpdateState.Builder("id", LiveUpdateOperation.UPDATE)
                .progress(35)
                .build();

        Notification n = build(new DefaultProgressStyleProvider().createStyle(state));

        assertEquals(35, n.extras.getInt(Notification.EXTRA_PROGRESS));
    }

    @Test
    public void indeterminateApplied() {
        LiveUpdateState state = new LiveUpdateState.Builder("id", LiveUpdateOperation.UPDATE)
                .progressIndeterminate(true)
                .build();

        Notification n = build(new DefaultProgressStyleProvider().createStyle(state));

        // ProgressStyle serializes into the legacy progress extras (EXTRA_PROGRESS / progressMax
        // are already asserted elsewhere); the indeterminate flag lands under the sibling key.
        assertTrue(n.extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE));
    }
}
