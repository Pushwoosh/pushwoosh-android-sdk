package com.pushwoosh.internal.utils;

import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import androidx.annotation.Nullable;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.resource.ResourceProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class NotificationUtilsTest {
    @Before
    public void setUp() {

    }

    //test case for opening a closed app with existing notifications in status bar
    @Test
    public void tryToGetIconFormStringOrGetFromApplicationWhenPushwooshNotInitializedTest() throws Exception {
        try (
                MockedStatic<AndroidPlatformModule> platformModuleMockedStatic = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<PushwooshPlatform> pushwooshPlatformMockedStatic = Mockito.mockStatic(PushwooshPlatform.class)
        ) {
            pushwooshPlatformMockedStatic.when(PushwooshPlatform::getInstance).thenReturn(null);
            platformModuleMockedStatic.when(AndroidPlatformModule::getResourceProvider).thenReturn(new ResourceProvider() {
                @Override
                public String getString(int id, Object... params) {
                    return null;
                }

                @Override
                public int getIdentifier(String resourceName, String defType) {
                    return 0;
                }

                @Nullable
                @Override
                public Configuration getConfiguration() {
                    return null;
                }

                @Override
                public void getValue(int res, TypedValue value, boolean resolveRefs) {

                }

                @Nullable
                @Override
                public DisplayMetrics getDisplayMetrics() {
                    return null;
                }

                @Override
                public float getDimension(int res) {
                    return 0;
                }
            });

            NotificationUtils.tryToGetIconFormStringOrGetFromApplication(null);
        }
    }
}