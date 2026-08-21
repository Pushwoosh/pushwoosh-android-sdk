package com.pushwoosh.internal.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class FakeRequestManagerTest {

    /** Minimal request: no prefs, no platform — getParams() is irrelevant to these assertions. */
    private static final class ProbeRequest extends PushRequest<String> {
        private final String method;

        ProbeRequest(String method) {
            this.method = method;
        }

        @Override
        public String getMethod() {
            return method;
        }

        @Override
        public String parseResponse(JSONObject response) {
            return response.optString("value");
        }
    }

    private FakeRequestManager fake;

    @Before
    public void setUp() {
        fake = FakeRequestManager.install();
    }

    @After
    public void tearDown() {
        NetworkModule.setRequestManager(null);
    }

    // The defect this class exists to remove: an unscripted request used to look like success.
    @Test
    public void unscriptedRequest_isFailureNotEmptySuccess() {
        Result<String, NetworkException> result = fake.sendRequestSync(new ProbeRequest("getTags"));

        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertTrue(result.getException().getMessage().contains("getTags"));
    }

    @Test
    public void assertAllScripted_rethrowsTheMiss() {
        fake.sendRequestSync(new ProbeRequest("getTags"));

        try {
            fake.assertAllScripted();
            fail("assertAllScripted must rethrow the recorded miss");
        } catch (AssertionError expected) {
            assertTrue(expected.getMessage().contains("getTags"));
        }
    }

    @Test
    public void assertAllScripted_failsOnScriptNobodyUsed() {
        fake.respondWith("getTags", new JSONObject());

        try {
            fake.assertAllScripted();
            fail("assertAllScripted must fail on an unused script");
        } catch (AssertionError expected) {
            assertTrue(expected.getMessage().contains("unused"));
        }
    }

    @Test
    public void scriptsAreConsumedInOrder() throws Exception {
        fake.respondWith("getTags", new JSONObject().put("value", "first"));
        fake.respondWith("getTags", new JSONObject().put("value", "second"));

        assertEquals("first", fake.sendRequestSync(new ProbeRequest("getTags")).getData());
        assertEquals("second", fake.sendRequestSync(new ProbeRequest("getTags")).getData());
        fake.assertAllScripted();
    }

    @Test
    public void scriptsAreKeyedByMethodName() throws Exception {
        fake.respondWith("getTags", new JSONObject().put("value", "tags"));
        fake.respondWith("setTags", new JSONObject().put("value", "saved"));

        assertEquals("saved", fake.sendRequestSync(new ProbeRequest("setTags")).getData());
        assertEquals("tags", fake.sendRequestSync(new ProbeRequest("getTags")).getData());
        fake.assertAllScripted();
    }

    @Test
    public void alwaysFailWith_isSticky() {
        NetworkException boom = new NetworkException("boom");
        fake.alwaysFailWith("getTags", boom);

        for (int i = 0; i < 3; i++) {
            assertSame(boom, fake.sendRequestSync(new ProbeRequest("getTags")).getException());
        }
        assertEquals(3, fake.count("getTags"));
        fake.assertAllScripted();
    }

    @Test
    public void fifoScriptWinsOverStickyOne() throws Exception {
        fake.alwaysRespondWith("getTags", new JSONObject().put("value", "sticky"));
        fake.respondWith("getTags", new JSONObject().put("value", "queued"));

        assertEquals("queued", fake.sendRequestSync(new ProbeRequest("getTags")).getData());
        assertEquals("sticky", fake.sendRequestSync(new ProbeRequest("getTags")).getData());
    }

    @Test
    public void failWith_deliversTheSameExceptionInstanceToTheCallback() {
        NetworkException boom = new NetworkException("boom");
        fake.failWith("getTags", boom);
        AtomicReference<Result<String, NetworkException>> seen = new AtomicReference<>();

        fake.sendRequest(new ProbeRequest("getTags"), (Callback<String, NetworkException>) seen::set);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertSame(boom, seen.get().getException());
        fake.assertAllScripted();
    }

    @Test
    public void captureOnly_recordsWithoutCallingBack() {
        fake.captureOnly("getTags");
        List<Result<String, NetworkException>> delivered = new ArrayList<>();

        fake.sendRequest(new ProbeRequest("getTags"), (Callback<String, NetworkException>) delivered::add);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals(1, fake.count("getTags"));
        assertEquals(Collections.emptyList(), delivered);
        fake.assertAllScripted();
    }

    // deliver() must go through the production barrier: a paused looper holds the delivery.
    @Test
    public void deliver_goesThroughTheMainLooper() throws Exception {
        fake.captureOnly("getTags");
        AtomicReference<Result<String, NetworkException>> seen = new AtomicReference<>();
        fake.sendRequest(new ProbeRequest("getTags"), (Callback<String, NetworkException>) seen::set);

        ShadowLooper.pauseMainLooper();
        fake.deliver(fake.last("getTags"), Result.fromData("late"));
        assertNull("delivery must be queued on the main looper, not inline", seen.get());

        ShadowLooper.idleMainLooper();
        assertEquals("late", seen.get().getData());
    }

    // A throwing host callback must be absorbed by the barrier, exactly as in production.
    @Test
    public void deliver_swallowsAThrowingCallback() {
        fake.captureOnly("getTags");
        AtomicInteger invocations = new AtomicInteger();
        fake.sendRequest(new ProbeRequest("getTags"), (Callback<String, NetworkException>) result -> {
            invocations.incrementAndGet();
            throw new RuntimeException("host boom");
        });

        fake.deliver(fake.last("getTags"), Result.fromData("value"));
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals(1, invocations.get());
    }

    // A captureOnly method has no answer to give: sync must miss, not hand back Result.fromData(null).
    @Test
    public void sendRequestSync_onCaptureOnlyMethod_isAnsweredAsUnscripted() {
        fake.captureOnly("getTags");

        Result<String, NetworkException> result = fake.sendRequestSync(new ProbeRequest("getTags"));

        assertFalse(result.isSuccess());
        assertTrue(result.getException().getMessage().contains("getTags"));
        try {
            fake.assertAllScripted();
            fail("a sync call to a captureOnly method must be remembered as a miss");
        } catch (AssertionError expected) {
            assertTrue(expected.getMessage().contains("getTags"));
        }
    }

    @Test
    public void countAllLast_reportWhatWasSent() {
        fake.captureOnly("getTags");
        fake.sendRequest(new ProbeRequest("getTags"));
        fake.sendRequest(new ProbeRequest("getTags"), "https://custom.example.com/", null);

        assertEquals(2, fake.count("getTags"));
        assertEquals(2, fake.all("getTags").size());
        assertEquals("https://custom.example.com/", fake.last("getTags").baseUrl);
        assertNull(fake.all("getTags").get(0).baseUrl);
    }

    @Test
    public void last_withoutAnyRequest_fails() {
        try {
            fake.last("getTags");
            fail("last() must fail loudly when nothing was sent");
        } catch (AssertionError expected) {
            assertTrue(expected.getMessage().contains("getTags"));
        }
    }

    @Test
    public void assertNoRequests_failsAfterARequest() {
        fake.captureOnly("getTags");
        fake.assertNoRequests();

        fake.sendRequest(new ProbeRequest("getTags"));
        try {
            fake.assertNoRequests();
            fail("assertNoRequests must fail once a request was sent");
        } catch (AssertionError expected) {
            assertTrue(expected.getMessage().contains("getTags"));
        }
    }

    @Test
    public void baseUrlIsRecordedAndItsAnswerIsScriptable() {
        assertTrue(fake.updateBaseUrl("https://a.example.com/"));

        fake.updateBaseUrlReturns(false);
        assertFalse(fake.updateBaseUrl("https://b.example.com/"));

        assertEquals(
                java.util.Arrays.asList("https://a.example.com/", "https://b.example.com/"), fake.baseUrlUpdates());
    }

    @Test
    public void reverseProxyIsRecordedAndItsAnswerIsScriptable() {
        assertTrue(fake.setReverseProxyUrl("https://proxy.example.com/", Collections.singletonMap("k", "v")));

        fake.setReverseProxyUrlReturns(false);
        assertFalse(fake.setReverseProxyUrl(null, null));

        assertEquals(2, fake.reverseProxyCalls().size());
        assertEquals("https://proxy.example.com/", fake.reverseProxyCalls().get(0).url);
        assertEquals("v", fake.reverseProxyCalls().get(0).headers.get("k"));
        assertNull(fake.reverseProxyCalls().get(1).url);
    }

    @Test
    public void awaitLast_returnsARequestSentFromAnotherThread() throws Exception {
        fake.captureOnly("getTags");
        Thread sender = new Thread(() -> fake.sendRequest(new ProbeRequest("getTags")));
        sender.start();

        assertEquals("getTags", fake.awaitLast("getTags").request.getMethod());
        sender.join();
    }

    @Test
    public void awaitCount_failsWhenMoreArrivedThanExpected() {
        fake.captureOnly("getTags");
        fake.sendRequest(new ProbeRequest("getTags"));
        fake.sendRequest(new ProbeRequest("getTags"));

        try {
            fake.awaitCount("getTags", 1);
            fail("awaitCount must fail when the count overshoots");
        } catch (AssertionError expected) {
            assertTrue(expected.getMessage().contains("getTags"));
        }
    }
}
