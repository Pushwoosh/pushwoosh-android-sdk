package com.pushwoosh;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;

import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest= org.robolectric.annotation.Config.NONE)

public class PushwooshLazyInitializationTest {

    private PlatformTestManager platformTestManager;

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
        if (platformTestManager != null) {
            platformTestManager.tearDown();
        }
    }

    @Test
    public void onMessageReceived() {
        platformTestManager = new PlatformTestManager();
        Config config = MockConfig.createMock();
        when(config.isLazySdkInitialization()).thenReturn(true);
        when(config.getLogLevel()).thenReturn("NOISE");
        Context context = RuntimeEnvironment.getApplication();
        Bundle pushBundle = Mockito.mock(Bundle.class);
        PushwooshMessagingServiceHelper.onMessageReceived(context, pushBundle);
    }
}