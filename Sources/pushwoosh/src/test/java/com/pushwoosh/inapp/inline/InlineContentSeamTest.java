package com.pushwoosh.inapp.inline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.exception.PostEventException;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.network.InAppExecutorServiceHelper;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.internal.network.FakeRequestManager;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class InlineContentSeamTest {

    private static final class Call {
        final JSONObject inline;
        final String userId;
        final String appCode;

        Call(JSONObject inline, String userId, String appCode) {
            this.inline = inline;
            this.userId = userId;
            this.appCode = appCode;
        }
    }

    private static final class RecordingHandler implements InlineContentHandler {
        final List<Call> calls = new ArrayList<>();

        @Override
        public void onInlineContent(@NonNull JSONObject inline, @Nullable String userId, @Nullable String appCode) {
            calls.add(new Call(inline, userId, appCode));
        }
    }

    private PlatformTestManager platformTestManager;
    private FakeRequestManager fake;
    private RecordingHandler handler;

    @Before
    public void setUp() throws Exception {
        platformTestManager = new PlatformTestManager(MockConfig.createMock());
        platformTestManager.setUp();
        fake = platformTestManager.getRequestManager();
        handler = new RecordingHandler();
        InlineContentHandlerProvider.set(handler);
    }

    @After
    public void tearDown() throws Exception {
        InlineContentHandlerProvider.set(null);
        platformTestManager.tearDown();
    }

    // Verifies that the inline block of a postEvent answer reaches the module with the user and app the request
    // carried.
    @Test
    public void testPostEventInlineBlockReachesHandlerWithSentUser() throws Exception {
        platformTestManager.getRegistrationPrefs().userId().set("seam-user");
        fake.respondWith("postEvent", new JSONObject("{\"inline\":{\"slots\":{\"home\":null}}}"));
        // null callback on purpose: the answer is otherwise ignored by handlePostEventResponse
        platformTestManager.getInAppRepository().postEvent("buy", null, null);
        JSONObject sent = fake.awaitLast("postEvent").params;
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals(1, handler.calls.size());
        assertTrue(handler.calls.get(0).inline.getJSONObject("slots").has("home"));
        assertEquals("seam-user", sent.optString("userId", null));
        assertEquals("seam-user", handler.calls.get(0).userId);
        assertEquals(sent.getString("application"), handler.calls.get(0).appCode);
    }

    // Verifies that a throwing inline handler does not keep the postEvent answer from the caller's callback.
    @Test
    public void testThrowingHandlerStillDeliversPostEventCallback() throws Exception {
        WhiteboxHelper.setInternalState(
                platformTestManager.getInAppRepository(), "io", InAppExecutorServiceHelper.createExecutorService());
        InlineContentHandlerProvider.set((inline, userId, appCode) -> {
            throw new IllegalStateException("module bug");
        });
        fake.respondWith("postEvent", new JSONObject("{\"inline\":{\"slots\":{}}}"));
        List<Result<Resource, PostEventException>> results = new ArrayList<>();
        platformTestManager.getInAppRepository().postEvent("buy", null, results::add);
        fake.awaitCount("postEvent", 1);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals(1, results.size());
        assertTrue(results.get(0).isSuccess());
        assertNull(results.get(0).getData());
    }

    // Verifies that an answer without an inline object does not call the module.
    @Test
    public void testPostEventWithoutInlineBlockSkipsHandler() throws Exception {
        fake.respondWith("postEvent", new JSONObject("{}"));
        fake.respondWith("postEvent", new JSONObject("{\"inline\":\"x\"}"));
        platformTestManager.getInAppRepository().postEvent("buy", null, null);
        platformTestManager.getInAppRepository().postEvent("buy", null, null);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals(2, fake.count("postEvent"));
        assertTrue(handler.calls.isEmpty());
    }

    // Verifies that a purchase answer (PW_InAppPurchase through postEvent) reaches the module.
    @Test
    public void testPurchaseAnswerInlineBlockReachesHandler() throws Exception {
        fake.respondWith("postEvent", new JSONObject("{\"inline\":{\"slots\":{}}}"));
        platformTestManager.getPushwooshRepository().sendInappPurchase("sku", BigDecimal.ONE, "USD", new Date());
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals(1, handler.calls.size());
    }

    // Verifies that a failed postEvent does not call the module.
    @Test
    public void testFailedPostEventSkipsHandler() {
        fake.failWith("postEvent", new NetworkException("boom"));
        platformTestManager.getInAppRepository().postEvent("buy", null, null);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals(1, fake.count("postEvent"));
        assertTrue(handler.calls.isEmpty());
    }
}
