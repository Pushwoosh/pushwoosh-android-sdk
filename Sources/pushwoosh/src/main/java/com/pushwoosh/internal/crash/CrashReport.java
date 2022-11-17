package com.pushwoosh.internal.crash;

import com.pushwoosh.internal.platform.utils.GeneralUtils;
import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class CrashReport {
    private static final String TAG = CrashReport.class.getSimpleName();
    public static JSONObject createCrashReport(Throwable throwable,
                                        String appCode,
                                        String hwid,
                                        String framework,
                                        boolean isCollectingDeviceModelAllowed,
                                        boolean isCollectingDeviceOsVersionAllowed) throws JSONException {
        JSONObject crashReport = new JSONObject();
        JSONObject data = new JSONObject();
        data.put("environment", "production");
        data.put("body", getBodyJson(throwable));
        data.put("level", "error");
        data.put("code_version", GeneralUtils.SDK_VERSION);
        data.put("platform", "android");
        data.put("language", "java");
        data.put("framework", framework);
        data.put("client", getClientJson(isCollectingDeviceModelAllowed,
                isCollectingDeviceOsVersionAllowed));
        data.put("custom", getCustomJson(appCode, hwid));
        crashReport.put("data", data);
        return crashReport;
    }

    private static JSONObject getCustomJson(String appCode, String hwid) throws JSONException {
        JSONObject custom = new JSONObject();
        custom.put("application", appCode);
        custom.put("hwid", hwid);
        return custom;
    }

    private static JSONObject getClientJson(boolean isCollectingDeviceModelAllowed, boolean isCollectingDeviceOsVersionAllowed) throws JSONException {
        JSONObject client = new JSONObject();
        client.put("timestamp", System.currentTimeMillis() / 1000);
        JSONObject androidJson = new JSONObject();
        if (isCollectingDeviceModelAllowed) {
            androidJson.put("device_model", android.os.Build.MODEL);
        }
        if (isCollectingDeviceOsVersionAllowed) {
            androidJson.put("android_version", android.os.Build.VERSION.RELEASE);
        }
        client.put("android", androidJson);
        return client;
    }

    private static JSONObject getBodyJson(Throwable throwable) {
        try {
            JSONObject body = new JSONObject();
            body.put("trace_chain", getTraceChainJsonArray(throwable));
            return body;
        } catch (JSONException e) {
            PWLog.warn(TAG, e.getMessage());
            return null;
        }
    }

    private static JSONArray getTraceChainJsonArray(Throwable throwable) throws JSONException {
        List<JSONObject> traceJsonList = new ArrayList<>();
        do {
            traceJsonList.add(0, getTraceJson(throwable));
            throwable = throwable.getCause();
        } while (throwable != null);
        return new JSONArray(traceJsonList);
    }

    private static JSONObject getTraceJson(Throwable throwable) throws JSONException {
        JSONObject trace = new JSONObject();
        JSONArray frames = new JSONArray();
        StackTraceElement[] elements = throwable.getStackTrace();
        for (int i = elements.length - 1; i >= 0; --i) {
            frames.put(getFrameJson(elements[i]));
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);

            throwable.printStackTrace(ps);
            ps.close();
            baos.close();
            trace.put("raw", baos.toString("UTF-8"));
        } catch (Exception e) {
            PWLog.warn(TAG, e.getMessage());
        }
        trace.put("frames", frames);
        trace.put("exception", getExceptionJson(throwable));
        return trace;
    }

    private static JSONObject getFrameJson(StackTraceElement element) throws JSONException {
        JSONObject frame = new JSONObject();
        frame.put("class_name", element.getClassName());
        frame.put("filename", element.getFileName());
        frame.put("method", element.getMethodName());
        if (element.getLineNumber() > 0) {
            frame.put("lineno", element.getLineNumber());
        }
        return frame;
    }

    private static JSONObject getExceptionJson(Throwable throwable) throws JSONException {
        JSONObject exception = new JSONObject();
        exception.put("class", throwable.getClass().getName());
        exception.put("message", throwable.getMessage());
        return exception;
    }
}
