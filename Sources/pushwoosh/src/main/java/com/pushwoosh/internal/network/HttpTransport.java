package com.pushwoosh.internal.network;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

class HttpTransport {
    private static final String TAG = "HttpTransport";

    @VisibleForTesting
    static int connectTimeoutMs = 30_000;

    @VisibleForTesting
    static int readTimeoutMs = 60_000;

    @NonNull HttpResponse makeRequest(
            @NonNull String endpointUrl,
            @NonNull JSONObject data,
            @NonNull String methodName,
            @NonNull Map<String, String> headers,
            @NonNull String apiToken)
            throws Exception {
        try {
            URL url = new URL(endpointUrl + methodName);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);

            connection.setRequestMethod("POST");
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Authorization", apiToken);
            connection.setDoOutput(true);

            String payload = data.toString();
            connection.setRequestProperty("Content-Length", String.valueOf(payload.getBytes().length));
            connection.setUseCaches(false);
            try (OutputStream connectionOutput = connection.getOutputStream()) {
                connectionOutput.write(payload.getBytes());
                connectionOutput.flush();
            }

            HttpResponse response = readResponse(connection);
            PWLog.debug(
                    TAG,
                    "\n"
                            + "| Pushwoosh request: " + methodName + "\n"
                            + "| - URL: " + url.toString() + "\n"
                            + "| - Payload: " + payload + "\n"
                            + "| - Response: " + response.body + "\n");

            return response;
        } catch (Exception e) {
            PWLog.error(TAG, "Request failed: " + e.getMessage(), e);
            throw e;
        }
    }

    private HttpResponse readResponse(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        InputStream inputStream;
        if (isErrorResponseCode(status)) {
            inputStream = new BufferedInputStream(connection.getErrorStream());
        } else {
            inputStream = new BufferedInputStream(connection.getInputStream());
        }
        String body = "";
        try {
            if (connection.getContentLength() != 0) {
                try (ByteArrayOutputStream dataCache = new ByteArrayOutputStream()) {
                    byte[] buff = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buff)) >= 0) {
                        dataCache.write(buff, 0, len);
                    }
                    body = dataCache.toString().trim();
                } catch (Exception e) {
                    PWLog.error(TAG, "Failed to read response body", e);
                }
            }
        } finally {
            inputStream.close();
        }
        return new HttpResponse(status, connection.getResponseMessage(), body);
    }

    private boolean isErrorResponseCode(int code) {
        return code >= 400 && code < 600;
    }
}
