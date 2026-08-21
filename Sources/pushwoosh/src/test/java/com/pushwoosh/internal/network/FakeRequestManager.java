package com.pushwoosh.internal.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The test adapter behind {@link RequestManager}: no socket, no Mockito, one supported way to walk a
 * component past the network.
 *
 * <p>Outcomes are scripted per API method name ({@link PushRequest#getMethod()} — the wire contract,
 * which outlives class names), FIFO by default, sticky via the {@code always*} overloads:
 *
 * <pre>
 * FakeRequestManager fake = FakeRequestManager.install();
 * fake.respondWith("postEvent", body);
 * fake.awaitLast("postEvent").params;   // what the component actually sent
 * fake.assertAllScripted();             // in @After of tests that script
 * </pre>
 *
 * <p><b>An unscripted request answers with a failure and remembers the miss.</b> It never answers with
 * {@code Result.fromData(null)}: {@link Result#isSuccess()} is {@code exception == null}, so that
 * would disguise a forgotten stub as a successful call — the defect {@code RequestManagerMock} shipped.
 * The miss is not thrown on the spot (unlike {@code FakeHttpTransport}) because
 * {@code PlatformTestManager} installs this globally for ~45 test files, most of which only boot the
 * platform; {@link #assertAllScripted()} is the opt-in strict mode.
 *
 * <p>Callbacks are delivered through the production barrier
 * ({@link PushwooshRequestManager#deliverOnMain}), so a regression there breaks tests instead of being
 * masked by a hand-written copy. Recording and outcome resolution are inline — no
 * {@code BackgroundExecutor.network} hop — so a test does not need {@code Thread.sleep} to steer the
 * async path.
 */
public class FakeRequestManager implements RequestManager {

    private static final String TAG = "FakeRequestManager";

    private static final long AWAIT_MS = 2000;
    private static final long POLL_MS = 10;

    /** One recorded call. {@code params} is {@code null} when {@code getParams()} threw. */
    public static final class Sent {
        public final PushRequest<?> request;

        @Nullable public final String baseUrl;

        @Nullable public final JSONObject params;

        @Nullable public final Callback<?, NetworkException> callback;

        Sent(
                PushRequest<?> request,
                @Nullable String baseUrl,
                @Nullable JSONObject params,
                @Nullable Callback<?, NetworkException> callback) {
            this.request = request;
            this.baseUrl = baseUrl;
            this.params = params;
            this.callback = callback;
        }
    }

    /** One recorded {@link #setReverseProxyUrl} call. */
    public static final class ProxyCall {
        @Nullable public final String url;

        @Nullable public final Map<String, String> headers;

        ProxyCall(@Nullable String url, @Nullable Map<String, String> headers) {
            this.url = url;
            this.headers = headers == null ? null : new HashMap<>(headers);
        }
    }

    private static final class Outcome {
        @Nullable final JSONObject body;

        @Nullable final NetworkException failure;

        final boolean captureOnly;

        Outcome(@Nullable JSONObject body, @Nullable NetworkException failure, boolean captureOnly) {
            this.body = body;
            this.failure = failure;
            this.captureOnly = captureOnly;
        }
    }

    private static final Outcome CAPTURE_ONLY = new Outcome(null, null, true);

    private final Map<String, Deque<Outcome>> scripts = new HashMap<>();
    private final Map<String, Outcome> sticky = new HashMap<>();
    private final Map<String, List<Sent>> sent = new HashMap<>();
    private final List<String> baseUrlUpdates = Collections.synchronizedList(new ArrayList<>());
    private final List<ProxyCall> reverseProxyCalls = Collections.synchronizedList(new ArrayList<>());
    private final AtomicReference<AssertionError> missedScript = new AtomicReference<>();

    private volatile boolean updateBaseUrlAnswer = true;
    private volatile boolean setReverseProxyUrlAnswer = true;

    /** Installs a fresh fake behind {@link NetworkModule} and hands it back. */
    public static FakeRequestManager install() {
        FakeRequestManager fake = new FakeRequestManager();
        NetworkModule.setRequestManager(fake);
        return fake;
    }

    // --- scripting -------------------------------------------------------------------------------

    public synchronized FakeRequestManager respondWith(String method, @NonNull JSONObject body) {
        scripts.computeIfAbsent(method, k -> new ArrayDeque<>())
                .addLast(new Outcome(requireBody(method, body), null, false));
        return this;
    }

    public synchronized FakeRequestManager failWith(String method, @NonNull NetworkException failure) {
        scripts.computeIfAbsent(method, k -> new ArrayDeque<>())
                .addLast(new Outcome(null, requireFailure(method, failure), false));
        return this;
    }

    public synchronized FakeRequestManager alwaysRespondWith(String method, @NonNull JSONObject body) {
        sticky.put(method, new Outcome(requireBody(method, body), null, false));
        return this;
    }

    public synchronized FakeRequestManager alwaysFailWith(String method, @NonNull NetworkException failure) {
        sticky.put(method, new Outcome(null, requireFailure(method, failure), false));
        return this;
    }

    /** A null body would reach {@code parseResponse} and NPE there, blaming the SDK for a typo in the script. */
    private static JSONObject requireBody(String method, JSONObject body) {
        return Objects.requireNonNull(body, "FakeRequestManager: null body scripted for " + method);
    }

    /**
     * A null failure is worse than a crash: {@link Result#isSuccess()} is {@code exception == null}, so it
     * would script a "failure" that reads as success — the very defect this class exists to remove.
     */
    private static NetworkException requireFailure(String method, NetworkException failure) {
        return Objects.requireNonNull(failure, "FakeRequestManager: null failure scripted for " + method);
    }

    /**
     * Records calls to {@code method} and never invokes their callback — the test drives delivery
     * itself via {@link #deliver}. A {@code sendRequestSync} for such a method is a test bug and is
     * answered like an unscripted request.
     */
    public synchronized FakeRequestManager captureOnly(String method) {
        sticky.put(method, CAPTURE_ONLY);
        return this;
    }

    public FakeRequestManager updateBaseUrlReturns(boolean value) {
        updateBaseUrlAnswer = value;
        return this;
    }

    public FakeRequestManager setReverseProxyUrlReturns(boolean value) {
        setReverseProxyUrlAnswer = value;
        return this;
    }

    // --- RequestManager --------------------------------------------------------------------------

    @Override
    public <Response> void sendRequest(PushRequest<Response> request) {
        sendRequest(request, null, null);
    }

    @Override
    public <Response> void sendRequest(PushRequest<Response> request, Callback<Response, NetworkException> callback) {
        sendRequest(request, null, callback);
    }

    @Override
    public <Response> void sendRequest(
            PushRequest<Response> request, String baseUrl, Callback<Response, NetworkException> callback) {
        Outcome outcome = recordAndTakeOutcome(request, baseUrl, callback);
        if (outcome != null && outcome.captureOnly) {
            return;
        }
        PushwooshRequestManager.deliverOnMain(callback, resolve(request, outcome));
    }

    @Override
    @NonNull public <Response> Result<Response, NetworkException> sendRequestSync(PushRequest<Response> request) {
        Outcome outcome = recordAndTakeOutcome(request, null, null);
        return resolve(request, outcome != null && outcome.captureOnly ? null : outcome);
    }

    @Override
    public boolean updateBaseUrl(String baseUrl) {
        baseUrlUpdates.add(baseUrl);
        return updateBaseUrlAnswer;
    }

    @Override
    public boolean setReverseProxyUrl(String url, Map<String, String> headers) {
        reverseProxyCalls.add(new ProxyCall(url, headers));
        return setReverseProxyUrlAnswer;
    }

    // --- inspection ------------------------------------------------------------------------------

    public synchronized int count(String method) {
        return sent.containsKey(method) ? sent.get(method).size() : 0;
    }

    public synchronized List<Sent> all(String method) {
        return sent.containsKey(method) ? new ArrayList<>(sent.get(method)) : Collections.emptyList();
    }

    public synchronized Sent last(String method) {
        List<Sent> calls = sent.get(method);
        if (calls == null || calls.isEmpty()) {
            throw new AssertionError("FakeRequestManager: no request was sent for " + method);
        }
        return calls.get(calls.size() - 1);
    }

    /**
     * Waits (up to 2s) for at least one call to {@code method} and returns the most recent one.
     *
     * <p>This blocks the calling thread, which under {@code @LooperMode(LEGACY)} is the main thread: it
     * waits for a send made from <i>another</i> thread, never for one queued on the main looper. For the
     * latter, drain the looper first ({@code ShadowLooper.runUiThreadTasksIncludingDelayedTasks()}) and
     * assert with {@link #count}.
     */
    public Sent awaitLast(String method) {
        awaitAtLeast(method, 1);
        return last(method);
    }

    /**
     * Waits (up to 2s) for {@code expected} calls to {@code method}, then asserts there are exactly that many.
     * Same main-thread caveat as {@link #awaitLast}.
     */
    public void awaitCount(String method, int expected) {
        awaitAtLeast(method, expected);
        int actual = count(method);
        if (actual != expected) {
            throw new AssertionError(
                    "FakeRequestManager: expected " + expected + " request(s) for " + method + ", got " + actual);
        }
    }

    private void awaitAtLeast(String method, int expected) {
        long deadline = System.currentTimeMillis() + AWAIT_MS;
        while (count(method) < expected && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (count(method) < expected) {
            throw new AssertionError("FakeRequestManager: timed out waiting for " + expected + " request(s) for "
                    + method + ", got " + count(method)
                    + " — if the send is queued on the main looper, this loop holds that thread and can never"
                    + " see it: drain the looper and assert with count() instead");
        }
    }

    public List<String> baseUrlUpdates() {
        synchronized (baseUrlUpdates) {
            return new ArrayList<>(baseUrlUpdates);
        }
    }

    public List<ProxyCall> reverseProxyCalls() {
        synchronized (reverseProxyCalls) {
            return new ArrayList<>(reverseProxyCalls);
        }
    }

    public synchronized void assertNoRequests() {
        if (!sent.isEmpty()) {
            throw new AssertionError("FakeRequestManager: expected no requests, got " + sent.keySet());
        }
    }

    /**
     * Rethrows the first unscripted request as an {@code AssertionError}, then fails on FIFO scripts
     * nobody consumed — a request the test believed it was making but never made. Sticky outcomes are
     * reusable by design and are not counted as unused.
     */
    public void assertAllScripted() {
        AssertionError miss = missedScript.get();
        if (miss != null) {
            throw miss;
        }
        synchronized (this) {
            for (Map.Entry<String, Deque<Outcome>> entry : scripts.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    throw new AssertionError("FakeRequestManager: "
                            + entry.getValue().size() + " scripted outcome(s) unused for " + entry.getKey());
                }
            }
        }
    }

    /** Delivers {@code result} to a captured call through the production main-thread barrier. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void deliver(Sent sentCall, Result<?, NetworkException> result) {
        if (sentCall.callback == null) {
            throw new AssertionError("FakeRequestManager: the recorded call to " + sentCall.request.getMethod()
                    + " carries no callback, there is nothing to deliver to");
        }
        PushwooshRequestManager.deliverOnMain((Callback) sentCall.callback, (Result) result);
    }

    // --- internals -------------------------------------------------------------------------------

    /**
     * A send and the script it consumes are published together: an {@code awaitLast} that returned
     * between the two would let {@code assertAllScripted()} fail "unused" on a healthy test.
     */
    @Nullable private Outcome recordAndTakeOutcome(
            PushRequest<?> request, @Nullable String baseUrl, @Nullable Callback<?, NetworkException> callback) {
        String method = request.getMethod();
        // getParams() runs SDK code (buildParams, the platform singleton) — never under our monitor.
        Sent sentCall = new Sent(request, baseUrl, paramsOf(request), callback);
        synchronized (this) {
            sent.computeIfAbsent(method, k -> new ArrayList<>()).add(sentCall);
            return takeOutcome(method);
        }
    }

    /**
     * Params are captured eagerly, the way {@code RequestManagerMock} did: {@code getParams()} reaches
     * for prefs and the platform, which some fixtures do not build — a throw means {@code null} params,
     * not a failed test.
     */
    @Nullable private static JSONObject paramsOf(PushRequest<?> request) {
        try {
            return request.getParams();
        } catch (Exception e) {
            // Dormant by default (noise level, showStandardStreams=false): this is for the developer who
            // hit a bare NPE on Sent.params and turned logging up to find out why it was null.
            PWLog.noise(TAG, "getParams() threw for " + request.getMethod() + ", recording null params", e);
            return null;
        }
    }

    @Nullable private synchronized Outcome takeOutcome(String method) {
        Deque<Outcome> queue = scripts.get(method);
        if (queue != null && !queue.isEmpty()) {
            return queue.pollFirst();
        }
        return sticky.get(method);
    }

    @NonNull private <Response> Result<Response, NetworkException> resolve(
            PushRequest<Response> request, @Nullable Outcome outcome) {
        if (outcome == null) {
            String message = "FakeRequestManager: nothing scripted for " + request.getMethod();
            missedScript.compareAndSet(null, new AssertionError(message));
            return Result.fromException(new NetworkException(message));
        }
        if (outcome.failure != null) {
            return Result.fromException(outcome.failure);
        }
        try {
            // The body is handed to parseResponse verbatim: the fake sits above the envelope layer,
            // exactly where RequestManagerMock sat.
            return Result.fromData(request.parseResponse(outcome.body));
        } catch (JSONException e) {
            return Result.fromException(new NetworkException(e.getMessage()));
        }
    }
}
