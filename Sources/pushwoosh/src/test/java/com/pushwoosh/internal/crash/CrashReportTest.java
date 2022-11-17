package com.pushwoosh.internal.crash;

import com.pushwoosh.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class CrashReportTest {
    private final String testMessageString = "Test";
    private final Throwable throwable = new RuntimeException(testMessageString);

    private final String applicationTestString = "XXXXX-XXXXX";
    private final String hwidTestString = "test_hwid";
    private final String frameworkTestString = "Native";

    @Test
    public void testCrashReport() throws Exception {
        JSONObject crashJson = CrashReport.createCrashReport(
                throwable,
                applicationTestString,
                hwidTestString,
                frameworkTestString,
                true,
                true
        );

        JSONObject data = crashJson.getJSONObject("data");
        String environment = data.getString("environment");
        String framework = data.getString("framework");
        String level = data.getString("level");
        String codeVersion = data.getString("code_version");
        String language = data.getString("language");
        String platform = data.getString("platform");

        Assert.assertEquals("production", environment);
        Assert.assertEquals(frameworkTestString, framework);
        Assert.assertEquals("error", level);
        Assert.assertEquals(BuildConfig.VERSION_NAME, codeVersion);
        Assert.assertEquals("java", language);
        Assert.assertEquals("android", platform);

        JSONObject custom = data.getJSONObject("custom");
        String application = custom.getString("application");
        String hwid = custom.getString("hwid");

        Assert.assertEquals(applicationTestString, application);
        Assert.assertEquals(hwidTestString, hwid);

        JSONObject client = data.getJSONObject("client");
        long timestamp = client.getLong("timestamp");

        Assert.assertNotEquals(0, timestamp);

        JSONObject android = client.getJSONObject("android");
        String androidVersion = android.getString("android_version");
        String phoneModel = android.getString("device_model");

        Assert.assertNotNull(androidVersion);
        Assert.assertNotEquals("", androidVersion);
        Assert.assertNotNull(phoneModel);
        Assert.assertNotEquals("", phoneModel);

        JSONObject body = data.getJSONObject("body");
        JSONArray traceChainArray = body.getJSONArray("trace_chain");
        JSONObject traceChain = traceChainArray.getJSONObject(0);
        JSONObject exception = traceChain.getJSONObject("exception");
        String message = exception.getString("message");
        String classString = exception.getString("class");

        Assert.assertEquals(testMessageString, message);
        Assert.assertEquals("java.lang.RuntimeException", classString);

        JSONArray frames = traceChain.getJSONArray("frames");
        Assert.assertNotEquals(0, frames.length());

        String raw = traceChain.getString("raw");
        Assert.assertNotNull(raw);
        Assert.assertNotEquals("", raw);
    }

    @Test
    public void testDeviceModelOsVersionNotPresent() throws Exception {
        JSONObject crashJson = CrashReport.createCrashReport(
                throwable,
                applicationTestString,
                hwidTestString,
                frameworkTestString,
                false,
                false
        );

        JSONObject data = crashJson.getJSONObject("data");
        JSONObject client = data.getJSONObject("client");
        JSONObject android = client.getJSONObject("android");

        Assert.assertFalse(android.has("android_version"));
        Assert.assertFalse(android.has("phone_model"));
    }

}
