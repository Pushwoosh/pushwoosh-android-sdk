package com.pushwoosh.featureflags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.LEGACY)
public class FeatureFlagsUrlTest {
    private static final String APP = "70699-8A0F4";

    @Test
    public void nueBase() {
        assertEquals(
                "https://api.pushwoosh.com/api/v2/device-api/",
                FeatureFlagsUrl.endpoint(null, "https://api.pushwoosh.com/json/1.3/", APP));
    }

    @Test
    public void appCodeHostBase() {
        assertEquals(
                "https://70699-8A0F4.api.pushwoosh.com/api/v2/device-api/",
                FeatureFlagsUrl.endpoint(null, "https://70699-8A0F4.api.pushwoosh.com/json/1.3/", APP));
    }

    @Test
    public void usAndRuBases() {
        assertEquals(
                "https://api.pushwoosh.us/api/v2/device-api/",
                FeatureFlagsUrl.endpoint(null, "https://api.pushwoosh.us/json/1.3/", APP));
        assertEquals(
                "https://api.wavesend.ru/api/v2/device-api/",
                FeatureFlagsUrl.endpoint(null, "https://api.wavesend.ru/json/1.3/", APP));
    }

    @Test
    public void proxyWithPath_winsOverBase() {
        assertEquals(
                "https://proxy.example/pw/api/v2/device-api/",
                FeatureFlagsUrl.endpoint(
                        "https://proxy.example/pw/json/1.3/", "https://api.pushwoosh.com/json/1.3/", APP));
    }

    @Test
    public void baseWithoutLegacyPath_fallsBackToAppCodeHost() {
        assertEquals(
                "https://70699-8A0F4.api.pushwoosh.com/api/v2/device-api/",
                FeatureFlagsUrl.endpoint(null, "https://www.google.com/", APP));
    }

    @Test
    public void noBase_fallsBackToAppCodeHost() {
        assertEquals(
                "https://70699-8A0F4.api.pushwoosh.com/api/v2/device-api/", FeatureFlagsUrl.endpoint(null, "", APP));
    }

    @Test
    public void proxyWithoutLegacyPath_returnsNullInsteadOfBypassingProxy() {
        assertNull(FeatureFlagsUrl.endpoint(
                "https://proxy.example/pushwoosh/", "https://api.pushwoosh.com/json/1.3/", APP));
    }
}
