package com.pushwoosh.internal.network;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Scripted stand-in for {@link HttpTransport}: no socket, no MockWebServer.
 *
 * <p>Enqueue outcomes with {@link #respondWith} / {@link #failWith}, inspect what the manager sent
 * with {@link #last()}. A request with nothing scripted for it raises {@code AssertionError} — the
 * trap {@code RequestManagerMock} falls into is answering an unscripted request with something that
 * looks like a legitimate result.
 */
class FakeHttpTransport extends HttpTransport {

    static final class Call {
        final String url;
        final String method;
        final String payload;
        final Map<String, String> headers;
        final String apiToken;

        Call(String url, String method, String payload, Map<String, String> headers, String apiToken) {
            this.url = url;
            this.method = method;
            this.payload = payload;
            this.headers = new HashMap<>(headers);
            this.apiToken = apiToken;
        }
    }

    private final Deque<Object> scripted = new ArrayDeque<>();
    private final List<Call> calls = Collections.synchronizedList(new ArrayList<>());
    private final AtomicReference<AssertionError> missedStub = new AtomicReference<>();

    FakeHttpTransport respondWith(int statusCode, String body) {
        return respondWith(statusCode, "OK", body);
    }

    synchronized FakeHttpTransport respondWith(int statusCode, String statusMessage, String body) {
        scripted.addLast(new HttpResponse(statusCode, statusMessage, body));
        return this;
    }

    synchronized FakeHttpTransport failWith(Exception failure) {
        scripted.addLast(failure);
        return this;
    }

    @NonNull @Override
    HttpResponse makeRequest(
            @NonNull String endpointUrl,
            @NonNull JSONObject data,
            @NonNull String methodName,
            @NonNull Map<String, String> headers,
            @NonNull String apiToken)
            throws Exception {
        calls.add(new Call(endpointUrl, methodName, data.toString(), headers, apiToken));
        Object outcome;
        synchronized (this) {
            outcome = scripted.pollFirst();
        }
        // AssertionError, not Exception: the manager narrows every Exception into ConnectionException,
        // which would disguise a missing stub as a network failure.
        if (outcome == null) {
            AssertionError miss = new AssertionError("FakeHttpTransport: nothing scripted for request " + methodName);
            missedStub.compareAndSet(null, miss);
            throw miss;
        }
        if (outcome instanceof Exception) {
            throw (Exception) outcome;
        }
        return (HttpResponse) outcome;
    }

    int count() {
        return calls.size();
    }

    Call last() {
        synchronized (calls) {
            if (calls.isEmpty()) {
                throw new AssertionError("FakeHttpTransport: no request was made");
            }
            return calls.get(calls.size() - 1);
        }
    }

    /**
     * Asserts the script and the requests match exactly. Rethrows a missed-stub failure swallowed on
     * the async path (the manager narrows only {@code Exception}, and {@code BackgroundExecutor}
     * catches whatever is left), then fails on outcomes nobody consumed — a request the test believed
     * it was making but never made.
     */
    void assertAllScripted() {
        AssertionError miss = missedStub.get();
        if (miss != null) {
            throw miss;
        }
        synchronized (this) {
            if (!scripted.isEmpty()) {
                throw new AssertionError("FakeHttpTransport: " + scripted.size() + " scripted outcome(s) unused");
            }
        }
    }
}
