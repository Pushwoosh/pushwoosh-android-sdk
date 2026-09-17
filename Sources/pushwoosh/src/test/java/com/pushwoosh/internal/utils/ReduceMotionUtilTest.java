package com.pushwoosh.internal.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.provider.Settings;

import com.pushwoosh.internal.platform.AndroidPlatformModule;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

/**
 * SDK-984: "animations off" detector for the modal rich media show path. The detector must be
 * fail-safe: any inability to read the setting (null context, throwing resolver) reports false so
 * the SDK keeps its current animated behavior instead of breaking the show.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = "AndroidManifest.xml")
public class ReduceMotionUtilTest {

    private static void setScale(Context context, float scale) {
        Settings.Global.putFloat(context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, scale);
    }

    @Test
    public void animatorScaleZero_reportsReduceMotionEnabled() {
        Context context = RuntimeEnvironment.getApplication();
        setScale(context, 0f);
        try (MockedStatic<AndroidPlatformModule> platform = Mockito.mockStatic(AndroidPlatformModule.class)) {
            platform.when(AndroidPlatformModule::getApplicationContext).thenReturn(context);
            assertTrue(ReduceMotionUtil.isReduceMotionEnabled());
        }
    }

    @Test
    public void animatorScaleOne_reportsReduceMotionDisabled() {
        Context context = RuntimeEnvironment.getApplication();
        setScale(context, 1f);
        try (MockedStatic<AndroidPlatformModule> platform = Mockito.mockStatic(AndroidPlatformModule.class)) {
            platform.when(AndroidPlatformModule::getApplicationContext).thenReturn(context);
            assertFalse(ReduceMotionUtil.isReduceMotionEnabled());
        }
    }

    // Setting absent -> Settings.Global.getFloat falls back to the default 1f -> animations on.
    @Test
    public void unsetSetting_reportsReduceMotionDisabled() {
        Context context = RuntimeEnvironment.getApplication();
        try (MockedStatic<AndroidPlatformModule> platform = Mockito.mockStatic(AndroidPlatformModule.class)) {
            platform.when(AndroidPlatformModule::getApplicationContext).thenReturn(context);
            assertFalse(ReduceMotionUtil.isReduceMotionEnabled());
        }
    }

    // AndroidPlatformModule.getApplicationContext() is @Nullable; a null context must not disable
    // the show path — detector reports false (current behavior preserved).
    @Test
    public void nullContext_reportsReduceMotionDisabled() {
        try (MockedStatic<AndroidPlatformModule> platform = Mockito.mockStatic(AndroidPlatformModule.class)) {
            platform.when(AndroidPlatformModule::getApplicationContext).thenReturn(null);
            assertFalse(ReduceMotionUtil.isReduceMotionEnabled());
        }
    }

    // Any exception while reading the setting must be swallowed into false, never propagated
    // into onPageLoaded.
    @Test
    public void throwingContentResolver_reportsReduceMotionDisabled() {
        Context throwing = Mockito.mock(Context.class);
        Mockito.when(throwing.getContentResolver()).thenThrow(new RuntimeException("settings unavailable"));
        try (MockedStatic<AndroidPlatformModule> platform = Mockito.mockStatic(AndroidPlatformModule.class)) {
            platform.when(AndroidPlatformModule::getApplicationContext).thenReturn(throwing);
            assertFalse(ReduceMotionUtil.isReduceMotionEnabled());
        }
    }
}
