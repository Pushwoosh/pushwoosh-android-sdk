package com.pushwoosh;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.app.AppInfoProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class AndroidManifestConfigTest {

    private static final String BASE_API_URL_FORMAT = "https://%s.api.pushwoosh.com/json/1.3/";

    @Mock
    private AppInfoProvider appInfoProvider;

    private AutoCloseable mocks;
    private MockedStatic<AndroidPlatformModule> platformMock;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        platformMock = mockStatic(AndroidPlatformModule.class);
        platformMock.when(AndroidPlatformModule::getAppInfoProvider).thenReturn(appInfoProvider);
        when(appInfoProvider.getPackageName()).thenReturn("com.pushwoosh.test");
    }

    @After
    public void tearDown() throws Exception {
        platformMock.close();
        mocks.close();
    }

    private AndroidManifestConfig configWithMetaData(Bundle metaData) {
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.metaData = metaData;
        when(appInfoProvider.getApplicationInfo()).thenReturn(applicationInfo);
        return new AndroidManifestConfig();
    }

    @Test
    public void appIdWithTrailingNewlineIsSanitized() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.appid", "XXXXX-XXXXX\n");

        AndroidManifestConfig config = configWithMetaData(metaData);

        assertEquals("XXXXX-XXXXX", config.getAppId());
    }

    @Test
    public void apiTokenWithCrlfIsSanitized() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.apitoken", "TOKEN_VALUE\r\n");

        AndroidManifestConfig config = configWithMetaData(metaData);

        assertEquals("TOKEN_VALUE", config.getApiToken());
    }

    @Test
    public void requestUrlWithWhitespaceIsTrimmed() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.base_url", "  https://api.example.com/\n");

        AndroidManifestConfig config = configWithMetaData(metaData);

        assertEquals("https://api.example.com/", config.getRequestUrl());
    }

    @Test
    public void logLevelWithSurroundingSpacesIsTrimmed() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.log_level", "  DEBUG  ");

        AndroidManifestConfig config = configWithMetaData(metaData);

        assertEquals("DEBUG", config.getLogLevel());
    }

    @Test
    public void cleanAppIdIsReturnedUntouched() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.appid", "AAAAA-BBBBB");

        AndroidManifestConfig config = configWithMetaData(metaData);

        assertEquals("AAAAA-BBBBB", config.getAppId());
    }

    @Test
    public void sanitizedAppIdProducesValidApiUrl() {
        Bundle metaData = new Bundle();
        metaData.putString("com.pushwoosh.appid", "XXXXX-XXXXX\n");

        AndroidManifestConfig config = configWithMetaData(metaData);
        String url = String.format(BASE_API_URL_FORMAT, config.getAppId());

        assertEquals("https://XXXXX-XXXXX.api.pushwoosh.com/json/1.3/", url);
    }
}
