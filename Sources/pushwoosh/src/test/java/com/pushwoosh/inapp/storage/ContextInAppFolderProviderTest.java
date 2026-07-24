package com.pushwoosh.inapp.storage;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ContextInAppFolderProviderTest {

    // Verifies that getNativeConfigFile resolves to "native-config.json" inside the same
    // per-code folder the deployer unzips into (getInAppFolder). The file name is the server
    // ZIP contract; every other test mocks InAppFolderProvider, so it is pinned only here.
    @Test
    public void getNativeConfigFileResolvesToNativeConfigJsonInsideInAppFolder() {
        Context context = ApplicationProvider.getApplicationContext();
        ContextInAppFolderProvider provider = new ContextInAppFolderProvider(context);

        File file = provider.getNativeConfigFile("code1");

        assertEquals("native-config.json", file.getName());
        assertEquals(provider.getInAppFolder("code1"), file.getParentFile());
    }
}
