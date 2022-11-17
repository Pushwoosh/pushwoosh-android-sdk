package com.pushwoosh.internal.utils;

import static org.junit.Assert.*;

import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import androidx.annotation.Nullable;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.resource.ResourceProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AndroidPlatformModule.class, PushwooshPlatform.class})
public class NotificationUtilsTest {
    @Before
    public void setUp() {
        PowerMockito.mockStatic(AndroidPlatformModule.class);
        PowerMockito.when(AndroidPlatformModule.getResourceProvider()).thenReturn(new ResourceProvider() {
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
        PowerMockito.mockStatic(PushwooshPlatform.class);
        PowerMockito.when(PushwooshPlatform.getInstance()).thenReturn(null);
    }

    //test case for opening a closed app with existing notifications in status bar
    @Test
    public void tryToGetIconFormStringOrGetFromApplicationWhenPushwooshNotInitializedTest() throws Exception {
        NotificationUtils.tryToGetIconFormStringOrGetFromApplication(null);
    }
}