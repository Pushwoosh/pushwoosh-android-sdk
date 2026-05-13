package com.pushwoosh.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.testutil.PlatformTestManager;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class SetAdvertisingIdRequestTest {

    private PlatformTestManager platformTestManager;

    @Before
    public void setUp() {
        Config configMock = MockConfig.createMock();
        platformTestManager = new PlatformTestManager(configMock);
        platformTestManager.onApplicationCreated();
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    @Test
    public void getMethodReturnsSetMADID() {
        SetAdvertisingIdRequest request = new SetAdvertisingIdRequest("test-gaid");
        assertEquals("setMADID", request.getMethod());
    }

    @Test
    public void shouldWrapRequestReturnsFalse() {
        SetAdvertisingIdRequest request = new SetAdvertisingIdRequest("test-gaid");
        assertFalse(request.shouldWrapRequest());
    }

    @Test
    public void getParamsContainsMadid() throws Exception {
        SetAdvertisingIdRequest request = new SetAdvertisingIdRequest("test-gaid-123");
        JSONObject params = request.getParams();
        assertEquals("test-gaid-123", params.getString("madid"));
    }

    @Test
    public void getParamsContainsJsonNullWhenNull() throws Exception {
        SetAdvertisingIdRequest request = new SetAdvertisingIdRequest(null);
        JSONObject params = request.getParams();
        assertEquals(JSONObject.NULL, params.get("madid"));
    }

    @Test
    public void getParamsContainsOnlyRequiredFields() throws Exception {
        SetAdvertisingIdRequest request = new SetAdvertisingIdRequest("test-gaid");
        JSONObject params = request.getParams();
        assertEquals(3, params.length());
        assertEquals("test-gaid", params.getString("madid"));
        params.getString("hwid");
        params.getString("application");
    }
}
