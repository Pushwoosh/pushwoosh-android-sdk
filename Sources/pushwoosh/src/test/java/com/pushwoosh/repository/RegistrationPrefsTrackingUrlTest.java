package com.pushwoosh.repository;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class RegistrationPrefsTrackingUrlTest {

    private PlatformTestManager platformTestManager;
    private Config configMock;
    private RegistrationPrefs registrationPrefs;

    @Before
    public void setUp() {
        configMock = MockConfig.createMock();
        platformTestManager = new PlatformTestManager(configMock);
        platformTestManager.onApplicationCreated();
        registrationPrefs = platformTestManager.getRegistrationPrefs();
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    @Test
    public void returnsDefaultWhenNotConfigured() {
        when(configMock.getTrackingUrl()).thenReturn(null);
        assertEquals("https://tracking.svc-nue.pushwoosh.com/api/v2/device-api/", registrationPrefs.getTrackingUrl());
    }

    @Test
    public void returnsCustomUrlWithTrailingSlash() {
        when(configMock.getTrackingUrl()).thenReturn("https://tracking.api.wavesend.ru/json/1.3");
        assertEquals("https://tracking.api.wavesend.ru/json/1.3/", registrationPrefs.getTrackingUrl());
    }

    @Test
    public void fallsBackToDefaultForMissingScheme() {
        when(configMock.getTrackingUrl()).thenReturn("tracking.api.wavesend.ru/json/1.3");
        assertEquals("https://tracking.svc-nue.pushwoosh.com/api/v2/device-api/", registrationPrefs.getTrackingUrl());
    }

    @Test
    public void returnsDefaultForEmptyString() {
        when(configMock.getTrackingUrl()).thenReturn("");
        assertEquals("https://tracking.svc-nue.pushwoosh.com/api/v2/device-api/", registrationPrefs.getTrackingUrl());
    }
}
