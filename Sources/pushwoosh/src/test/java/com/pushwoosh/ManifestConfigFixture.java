package com.pushwoosh;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.app.AppInfoProvider;
import com.pushwoosh.internal.platform.resource.ResourceProvider;
import com.pushwoosh.internal.utils.Config;

import org.mockito.MockedStatic;

/**
 * Builds the production manifest config from the given meta-data, so tests outside this package can
 * drive the SDK with real manifest flag resolution instead of a stubbed {@link Config}.
 */
public final class ManifestConfigFixture {

    private ManifestConfigFixture() {}

    /**
     * @return a Mockito spy of the real config, as {@code PlatformTestManager} stubs a few getters on it
     */
    public static Config fromMetaData(Bundle metaData) {
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.metaData = metaData;
        AppInfoProvider appInfoProvider = mock(AppInfoProvider.class);
        when(appInfoProvider.getApplicationInfo()).thenReturn(applicationInfo);
        when(appInfoProvider.getPackageName()).thenReturn("com.pushwoosh.test");

        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class)) {
            platform.when(AndroidPlatformModule::getAppInfoProvider).thenReturn(appInfoProvider);
            platform.when(AndroidPlatformModule::getResourceProvider).thenReturn(mock(ResourceProvider.class));
            return spy(new AndroidManifestConfig());
        }
    }
}
