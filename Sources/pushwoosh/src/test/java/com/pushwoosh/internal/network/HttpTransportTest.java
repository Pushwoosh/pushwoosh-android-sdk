package com.pushwoosh.internal.network;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLog;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class HttpTransportTest {

    public static final int TIMEOUT_TEST = 10000;
    private static final String API_TOKEN = "Token test-api-token";
    private static final String METHOD = "testMethod";

    private MockWebServer server;
    private String endpointUrl;

    // class under test
    private HttpTransport transport;

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        server = new MockWebServer();
        server.start();
        endpointUrl = server.url("/").toString();
        transport = new HttpTransport();
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    private static JSONObject payload(String value) throws Exception {
        return new JSONObject().put("param", value);
    }

    private HttpResponse post(JSONObject data) throws Exception {
        return transport.makeRequest(endpointUrl, data, METHOD, Collections.emptyMap(), API_TOKEN);
    }

    // The URL on the wire is endpointUrl + methodName, and the verb is always POST.
    @Test(timeout = TIMEOUT_TEST)
    public void makeRequest_composesUrlFromEndpointAndMethod_andPostsPayload() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"status_code\":200}"));
        JSONObject data = payload("value");

        HttpResponse response = post(data);

        assertEquals(200, response.statusCode);
        RecordedRequest rec = server.takeRequest();
        assertEquals("POST", rec.getMethod());
        assertEquals("/" + METHOD, rec.getPath());
        assertEquals(data.toString(), rec.getBody().readUtf8());
    }

    // Caller headers are copied verbatim; Content-Type and Authorization are added by the transport.
    @Test(timeout = TIMEOUT_TEST)
    public void makeRequest_sendsCustomHeadersContentTypeAndAuthorization() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"status_code\":200}"));
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Custom-Auth", "abc123");
        headers.put("X-Tenant", "tenant-42");

        transport.makeRequest(endpointUrl, payload("v"), METHOD, headers, API_TOKEN);

        RecordedRequest rec = server.takeRequest();
        assertEquals("abc123", rec.getHeader("X-Custom-Auth"));
        assertEquals("tenant-42", rec.getHeader("X-Tenant"));
        assertEquals(API_TOKEN, rec.getHeader("Authorization"));
        assertThat(rec.getHeader("Content-Type"), containsString("application/json"));
        assertThat(rec.getHeader("Content-Type"), containsString("charset=utf-8"));
    }

    // Verifies that the transport's own Content-Type and Authorization win over same-named caller headers.
    // Caller headers are copied first and the transport's own are set after; swapping those two blocks
    // would ship a reverse proxy customer's credentials to Pushwoosh instead of the API token.
    @Test(timeout = TIMEOUT_TEST)
    public void makeRequest_callerHeadersCollideWithOwnHeaders_transportValuesWin() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"status_code\":200}"));
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic proxy-secret");
        headers.put("Content-Type", "text/plain");

        transport.makeRequest(endpointUrl, payload("v"), METHOD, headers, API_TOKEN);

        RecordedRequest rec = server.takeRequest();
        assertEquals(API_TOKEN, rec.getHeader("Authorization"));
        assertThat(rec.getHeader("Content-Type"), containsString("application/json"));
    }

    // payload.getBytes() encodes with the platform default charset, pinned to UTF-8 for the test JVM
    // in build.gradle. Pins that the declared length matches the bytes actually written, and that
    // multi-byte text survives intact. Subsumes the ASCII case: a wrong Content-Length breaks here too.
    @Test(timeout = TIMEOUT_TEST)
    public void makeRequest_nonAsciiPayload_bytesOnWireMatchDeclaredLength() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"status_code\":200}"));
        JSONObject data = payload("Привет");
        String rendered = data.toString();

        post(data);

        RecordedRequest rec = server.takeRequest();
        String contentLength = rec.getHeader("Content-Length");
        assertThat(contentLength, is(notNullValue()));
        assertEquals(Long.parseLong(contentLength), rec.getBodySize());
        assertEquals(rendered, rec.getBody().readUtf8());
    }

    // The read loop uses a 1024-byte buffer; a longer body must come back whole.
    @Test(timeout = TIMEOUT_TEST)
    public void makeRequest_success_readsWholeBodyPastBufferSize() throws Exception {
        StringBuilder filler = new StringBuilder();
        for (int i = 0; i < 3000; i++) {
            filler.append('x');
        }
        String body = "{\"status_code\":200,\"response\":{\"filler\":\"" + filler + "\"}}";
        server.enqueue(new MockResponse().setBody(body));

        HttpResponse response = post(payload("v"));

        assertEquals(200, response.statusCode);
        assertEquals(body, response.body);
    }

    // Verifies that a chunked response, which carries no Content-Length, is still read.
    // getContentLength() is -1 here and the read guard is "!= 0"; tightening it to "> 0" would turn
    // every chunked response into a silently empty body.
    @Test(timeout = TIMEOUT_TEST)
    public void makeRequest_chunkedResponse_readsBodyDespiteUnknownContentLength() throws Exception {
        String body = "{\"status_code\":200,\"response\":{\"result\":\"chunked output\"}}";
        server.enqueue(new MockResponse().setChunkedBody(body, 8));

        HttpResponse response = post(payload("v"));

        assertEquals(200, response.statusCode);
        assertEquals(body, response.body);
    }

    // Verifies that a multi-byte response body comes back intact across the 1024-byte read buffer.
    // The prefix is 39 ASCII bytes, so byte 1024 lands inside a two-byte Cyrillic character: decoding
    // each chunk on its own instead of accumulating bytes would corrupt exactly that character.
    @Test(timeout = TIMEOUT_TEST)
    public void makeRequest_nonAsciiResponseBody_readsWholeBodyIntact() throws Exception {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            text.append("Привет");
        }
        String body = "{\"status_code\":200,\"response\":{\"text\":\"" + text + "\"}}";
        server.enqueue(new MockResponse().setBody(body));

        HttpResponse response = post(payload("v"));

        assertEquals(200, response.statusCode);
        assertEquals(body, response.body);
    }

    // Error bodies must be read from getErrorStream(); getInputStream() would throw instead. 4xx and
    // 5xx share one isErrorCode() branch — its 400..599 boundaries are pinned in HttpResponseTest.
    @Test(timeout = TIMEOUT_TEST)
    public void makeRequest_errorStatus_readsBodyFromErrorStream() throws Exception {
        String[][] cases = {
            {"404", "Not Found", "{\"status_code\":210,\"status_message\":\"Quota\"}"},
            {"503", "Service Unavailable", "{\"status_code\":503}"},
        };

        for (String[] c : cases) {
            String label = "case " + c[0];
            server.enqueue(new MockResponse()
                    .setStatus("HTTP/1.1 " + c[0] + " " + c[1])
                    .setBody(c[2]));

            HttpResponse response = post(payload("v"));

            assertEquals(label, Integer.parseInt(c[0]), response.statusCode);
            assertEquals(label, c[1], response.statusMessage);
            assertEquals(label, c[2], response.body);
            assertTrue(label, response.isError());
        }
    }

    // 404 without a body: getErrorStream() can be null, so Content-Length 0 has to short-circuit
    // the read before anything touches the stream.
    @Test(timeout = TIMEOUT_TEST)
    public void makeRequest_clientErrorWithoutBody_returnsEmptyBody() throws Exception {
        server.enqueue(new MockResponse().setStatus("HTTP/1.1 404 Not Found"));

        HttpResponse response = post(payload("v"));

        assertEquals(404, response.statusCode);
        assertEquals("", response.body);
    }

    @Test(timeout = TIMEOUT_TEST)
    public void makeRequest_contentLengthZero_returnsEmptyBody() throws Exception {
        server.enqueue(new MockResponse());

        HttpResponse response = post(payload("v"));

        assertEquals(200, response.statusCode);
        assertEquals("", response.body);
    }

    @Test(timeout = TIMEOUT_TEST)
    public void makeRequest_connectFails_propagatesException() throws Exception {
        ServerSocket probe = new ServerSocket(0);
        int deadPort = probe.getLocalPort();
        probe.close();

        try {
            transport.makeRequest(
                    "http://127.0.0.1:" + deadPort + "/", payload("v"), METHOD, Collections.emptyMap(), API_TOKEN);
            fail("expected the connect failure to reach the caller");
        } catch (Exception expected) {
            assertThat(expected, instanceOf(IOException.class));
        }
    }

    // A ServerSocket we never accept() on: the OS completes the handshake so connect succeeds, but
    // read() never gets a response and must hit readTimeoutMs instead of pinning the network thread.
    @Test(timeout = TIMEOUT_TEST)
    public void makeRequest_serverSilent_hitsReadTimeout() throws Exception {
        int originalReadTimeout = HttpTransport.readTimeoutMs;
        ServerSocket silentServer = new ServerSocket(0);
        try {
            HttpTransport.readTimeoutMs = 200;

            transport.makeRequest(
                    "http://127.0.0.1:" + silentServer.getLocalPort() + "/",
                    payload("v"),
                    METHOD,
                    Collections.emptyMap(),
                    API_TOKEN);
            fail("expected a read timeout");
        } catch (SocketTimeoutException expected) {
            assertThat(expected.getMessage(), is(notNullValue()));
        } finally {
            HttpTransport.readTimeoutMs = originalReadTimeout;
            silentServer.close();
        }
    }
}
