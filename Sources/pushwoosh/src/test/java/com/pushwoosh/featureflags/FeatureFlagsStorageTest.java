package com.pushwoosh.featureflags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.SharedPreferences;

import com.pushwoosh.internal.prefs.TestPrefsProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.LEGACY)
public class FeatureFlagsStorageTest {
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        prefs = new TestPrefsProvider().providePrefs(FeatureFlagsStorage.PREFS_NAME);
    }

    @Test
    public void load_nothingSaved_returnsNull() {
        assertNull(new FeatureFlagsStorage(prefs).load());
    }

    @Test
    public void clear_dropsCacheButKeepsLastRequestAt() {
        FeatureFlagsStorage storage = new FeatureFlagsStorage(prefs);
        storage.save("{}", "e1", 1000L, "alice");
        storage.saveLastRequestAt(777L);

        storage.clear();

        FeatureFlagsStorage reopened = new FeatureFlagsStorage(prefs);
        assertNull(reopened.load());
        assertEquals(777L, reopened.lastRequestAt());
    }
}
