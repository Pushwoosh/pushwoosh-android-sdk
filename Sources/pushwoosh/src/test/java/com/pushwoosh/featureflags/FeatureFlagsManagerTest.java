package com.pushwoosh.featureflags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.network.ConnectionException;
import com.pushwoosh.internal.network.FakeRequestManager;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.PushRequest;
import com.pushwoosh.internal.network.RequestBlockedException;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class FeatureFlagsManagerTest extends FeatureFlagsManagerFixture {

    @Test
    public void get_noCache_returnsNull() {
        assertNull(manager.get("new_checkout"));
    }

    @Test
    public void refresh_success_cachesFlagsAndSendsFlatBody() throws Exception {
        network.respondWith(METHOD, body(FLAGS_V1, "e1", false, false));

        List<Result<Void, FeatureFlagsException>> results = refresh();

        assertTrue(results.get(0).isSuccess());
        FeatureFlag flag = manager.get("new_checkout");
        assertTrue(flag.isEnabled());
        assertEquals("green", flag.getString("color", "x"));
        FakeRequestManager.Sent sent = network.last(METHOD);
        assertEquals(APP, sent.params.getString("application"));
        assertEquals(HWID, sent.params.getString("hwid"));
        assertFalse(sent.params.has("etag"));
    }

    @Test
    public void refresh_notModified_sendsEtagKeepsCacheAndDoesNotNotify() throws Exception {
        int[] notified = {0};
        manager.addUpdateListener(() -> notified[0]++);
        cacheV1();
        assertEquals(1, notified[0]);
        pastThrottle();
        network.respondWith(METHOD, body("{}", "e1", true, false));

        refresh();

        assertEquals("e1", network.last(METHOD).params.getString("etag"));
        assertTrue(manager.get("new_checkout").isEnabled());
        assertEquals(1, notified[0]);
    }

    @Test
    public void refresh_notModified_extendsCacheAge() throws Exception {
        cacheV1();
        time.now += FeatureFlagsManager.MAX_CACHE_AGE_MS - 1000;
        network.respondWith(METHOD, body("{}", "e1", true, false));
        refresh();

        time.now += 2000;

        assertNotNull(manager.get("new_checkout"));
        assertNotNull(newManager(false).get("new_checkout"));
    }

    @Test
    public void refresh_notModifiedOnExpiredCache_notifiesAndServesFlagsAgain() throws Exception {
        int[] notified = {0};
        manager.addUpdateListener(() -> notified[0]++);
        cacheV1();
        time.now += FeatureFlagsManager.MAX_CACHE_AGE_MS + 1;
        assertNull(manager.get("new_checkout"));
        network.respondWith(METHOD, body("{}", "e1", true, false));

        refresh();

        assertEquals("e1", network.last(METHOD).params.getString("etag"));
        assertTrue(manager.get("new_checkout").isEnabled());
        assertEquals(2, notified[0]);
    }

    @Test
    public void refresh_fullResponse_dropsMissingKeysAndNotifies() throws Exception {
        int[] notified = {0};
        manager.addUpdateListener(() -> notified[0]++);
        cacheV1();
        pastThrottle();
        network.respondWith(METHOD, body("{}", "e2", false, false));

        refresh();

        assertNull(manager.get("new_checkout"));
        assertEquals(2, notified[0]);
    }

    @Test
    public void refresh_sameContentNewEtag_doesNotNotify() throws Exception {
        int[] notified = {0};
        manager.addUpdateListener(() -> notified[0]++);
        cacheV1();
        pastThrottle();
        network.respondWith(METHOD, body(FLAGS_V1, "e2", false, false));

        refresh();

        assertEquals(1, notified[0]);
        pastThrottle();
        network.respondWith(METHOD, body(FLAGS_V2, "e3", false, false));
        refresh();
        assertEquals(2, notified[0]);
    }

    @Test
    public void refresh_partialWithoutCache_servesFromMemoryOnly() throws Exception {
        network.respondWith(METHOD, body(FLAGS_V1, "", false, true));

        refresh();

        assertNotNull(manager.get("new_checkout"));
        assertNull(newManager(false).get("new_checkout"));
        pastThrottle();
        network.respondWith(METHOD, body("{}", "", true, false));
        refresh();
        assertFalse(network.last(METHOD).params.has("etag"));
    }

    @Test
    public void refresh_partialWithValidCache_isIgnored() throws Exception {
        cacheV1();
        pastThrottle();
        network.respondWith(METHOD, body(FLAGS_V2, "", false, true));

        refresh();

        assertTrue(manager.get("new_checkout").isEnabled());
    }

    @Test
    public void refresh_partialOverInMemoryPartial_replacesIt() throws Exception {
        network.respondWith(METHOD, body(FLAGS_V1, "", false, true));
        refresh();
        pastThrottle();
        network.respondWith(METHOD, body(FLAGS_V2, "", false, true));

        refresh();

        assertFalse(manager.get("new_checkout").isEnabled());
    }

    @Test
    public void refresh_errors_keepCacheAndReportStatus() throws Exception {
        cacheV1();
        int[] statuses = {400, 401, 429, 500, 0};
        for (int status : statuses) {
            pastThrottle();
            network.failWith(METHOD, new ConnectionException("failed", status, status));

            List<Result<Void, FeatureFlagsException>> results = refresh();

            assertFalse(results.get(0).isSuccess());
            assertEquals(status, results.get(0).getException().getHttpStatus());
            assertTrue("status " + status, manager.get("new_checkout").isEnabled());
        }
    }

    @Test
    public void refresh_responseWithoutFlags_keepsCacheAndReportsError() throws Exception {
        cacheV1();
        pastThrottle();
        network.respondWith(METHOD, new JSONObject("{\"etag\":\"x\",\"not_modified\":false,\"partial\":false}"));

        List<Result<Void, FeatureFlagsException>> results = refresh();

        assertFalse(results.get(0).isSuccess());
        assertTrue(manager.get("new_checkout").isEnabled());
    }

    @Test
    public void refresh_emptyFlagsWithoutEtag_isSuccessWithNullFlags() throws Exception {
        network.respondWith(METHOD, body("{}", "", false, false));

        List<Result<Void, FeatureFlagsException>> results = refresh();

        assertTrue(results.get(0).isSuccess());
        assertNull(manager.get("new_checkout"));
    }

    @Test
    public void refresh_networkFailure_failsWithStatusZeroAndThrottlesNextRefresh() throws Exception {
        cacheV1();
        pastThrottle();
        network.failWith(METHOD, new NetworkException("socket closed"));

        List<Result<Void, FeatureFlagsException>> failed = refresh();
        long lastRequestAt = new FeatureFlagsStorage(disk).lastRequestAt();
        List<Result<Void, FeatureFlagsException>> throttled = refresh();

        assertFalse(failed.get(0).isSuccess());
        assertEquals(0, failed.get(0).getException().getHttpStatus());
        assertEquals(time.now, lastRequestAt);
        assertTrue(throttled.get(0).isSuccess());
        assertEquals(2, network.count(METHOD)); // throttling suppresses the third send (the throttled refresh)
    }

    // A request the manager refused before the transport (bad proxy root, no usable base URL, a closed
    // gate) must not consume the throttle window: otherwise the misconfiguration is reported once and then
    // hidden behind "success" with stale flags until the throttle expires.
    @Test
    public void refresh_blockedBeforeTransport_doesNotConsumeTheThrottleWindow() throws Exception {
        cacheV1();
        pastThrottle();
        long lastRequestAt = new FeatureFlagsStorage(disk).lastRequestAt();
        network.alwaysFailWith(METHOD, new RequestBlockedException("Reverse proxy URL does not end with json/1.3/"));

        List<Result<Void, FeatureFlagsException>> first = refresh();
        List<Result<Void, FeatureFlagsException>> second = refresh();

        assertFalse(first.get(0).isSuccess());
        assertFalse(second.get(0).isSuccess());
        assertEquals(lastRequestAt, new FeatureFlagsStorage(disk).lastRequestAt());
        assertEquals(3, network.count(METHOD)); // cacheV1 + both refreshes actually reached the manager
        assertTrue(manager.get("new_checkout").isEnabled());
    }

    @Test
    public void refresh_missingHwid_failsWithoutSending() {
        prefs.hwid().set("");

        List<Result<Void, FeatureFlagsException>> results = refresh();

        assertFalse(results.get(0).isSuccess());
        assertEquals(0, results.get(0).getException().getHttpStatus());
        assertEquals(0, network.count(METHOD));
    }

    @Test
    public void get_cacheOlderThan24h_returnsNull() throws Exception {
        cacheV1();

        time.now += FeatureFlagsManager.MAX_CACHE_AGE_MS + 1;

        assertNull(manager.get("new_checkout"));
    }

    @Test
    public void get_fetchedAtInFuture_returnsNull() throws Exception {
        cacheV1();

        time.now -= 60_000;

        assertNull(manager.get("new_checkout"));
    }

    @Test
    public void get_afterRestartWithFreshCache_servesCachedFlags() throws Exception {
        cacheV1();

        assertTrue(newManager(false).get("new_checkout").isEnabled());
    }

    @Test
    public void get_deviceDataRemoved_returnsNull() throws Exception {
        cacheV1();

        prefs.removeAllDeviceData().set(true);

        assertNull(manager.get("new_checkout"));
    }

    @Test
    public void constructor_corruptedDiskCache_startsEmptyAndClearsDisk() {
        new FeatureFlagsStorage(disk).save("{not json", "e1", time.now, HWID);

        FeatureFlagsManager fresh = newManager(false);

        assertNull(fresh.get("new_checkout"));
        assertNull(new FeatureFlagsStorage(disk).load());
    }

    @Test
    public void refresh_within5Minutes_isThrottledAndSucceeds() throws Exception {
        cacheV1();
        time.now += FeatureFlagsManager.THROTTLE_MS - 1;

        List<Result<Void, FeatureFlagsException>> results = refresh();

        assertTrue(results.get(0).isSuccess());
        assertEquals(1, network.count(METHOD));
    }

    @Test
    public void refresh_afterFailedRequestWithoutCache_isNotThrottled() throws Exception {
        network.failWith(METHOD, new ConnectionException("offline", 0, 0));
        refresh();
        time.now += 60_000;
        network.respondWith(METHOD, body(FLAGS_V1, "e1", false, false));

        List<Result<Void, FeatureFlagsException>> results = refresh();

        assertEquals(2, network.count(METHOD));
        assertTrue(results.get(0).isSuccess());
        assertTrue(manager.get("new_checkout").isEnabled());
    }

    @Test
    public void refresh_throttleSurvivesRestart() throws Exception {
        cacheV1();
        manager = newManager(false);

        refresh();

        assertEquals(1, network.count(METHOD));
    }

    @Test
    public void refresh_after5Minutes_isSent() throws Exception {
        cacheV1();
        pastThrottle();
        network.respondWith(METHOD, body(FLAGS_V1, "e1", true, false));

        refresh();

        assertEquals(2, network.count(METHOD));
    }

    @Test
    public void refresh_throttleDisabled_alwaysSends() throws Exception {
        manager = newManager(true);
        network.alwaysRespondWith(METHOD, body(FLAGS_V1, "e1", false, false));

        refresh();
        refresh();

        assertEquals(2, network.count(METHOD));
    }

    @Test
    public void refresh_lastRequestInFuture_isNotThrottled() throws Exception {
        new FeatureFlagsStorage(disk).saveLastRequestAt(time.now + TimeUnit.HOURS.toMillis(3));
        network.respondWith(METHOD, body(FLAGS_V1, "e1", false, false));

        refresh();

        assertEquals(1, network.count(METHOD));
    }

    @Test
    public void refresh_whileInFlight_joinsTheSameRequest() throws Exception {
        network.captureOnly(METHOD);
        List<Result<Void, FeatureFlagsException>> results = new ArrayList<>();
        manager.refresh(results::add);
        manager.refresh(results::add);
        ShadowLooper.idleMainLooper();
        assertEquals(1, network.count(METHOD));

        network.deliver(
                network.last(METHOD),
                Result.<FeatureFlagsResponse, NetworkException>fromData(
                        FeatureFlagsResponse.from(body(FLAGS_V1, "e1", false, false))));
        ShadowLooper.idleMainLooper();

        assertEquals(2, results.size());
        assertTrue(results.get(0).isSuccess());
        assertTrue(results.get(1).isSuccess());
        assertNotNull(manager.get("new_checkout"));
    }

    @Test
    public void refresh_userChangedWhileInFlight_dropsResponse() throws Exception {
        network.captureOnly(METHOD);
        manager.refresh(null);
        ShadowLooper.idleMainLooper();
        prefs.userId().set("alice");

        network.deliver(
                network.last(METHOD),
                Result.<FeatureFlagsResponse, NetworkException>fromData(
                        FeatureFlagsResponse.from(body(FLAGS_V1, "e1", false, false))));
        ShadowLooper.idleMainLooper();

        assertNull(manager.get("new_checkout"));
        assertNull(new FeatureFlagsStorage(disk).load());
        prefs.userId().set("");
        assertNull(manager.get("new_checkout"));
    }

    @Test
    public void refresh_sendThrowsSynchronously_failsCallbackAndAllowsNextRequest() throws Exception {
        boolean[] thrown = {false};
        FakeRequestManager throwingOnce = new FakeRequestManager() {
            @Override
            public <Response> void sendRequest(
                    PushRequest<Response> request, Callback<Response, NetworkException> callback) {
                if (!thrown[0]) {
                    thrown[0] = true;
                    throw new IllegalStateException("broken transport");
                }
                super.sendRequest(request, callback);
            }
        };
        throwingOnce.respondWith(METHOD, body(FLAGS_V1, "e1", false, false));
        manager = new FeatureFlagsManager(
                prefs, throwingOnce, new FeatureFlagsStorage(disk), (event, attributes, done) -> {}, time, false);

        List<Result<Void, FeatureFlagsException>> failed = refresh();
        pastThrottle();
        List<Result<Void, FeatureFlagsException>> next = refresh();

        assertEquals(1, failed.size());
        assertEquals(0, failed.get(0).getException().getHttpStatus());
        assertTrue(next.get(0).isSuccess());
        assertEquals(1, throwingOnce.count(METHOD));
        assertNotNull(manager.get("new_checkout"));
    }

    @Test
    public void refresh_sdkNotReady_isQueuedUntilReady() throws Exception {
        SdkStateProvider.getInstance().resetForTesting();
        network.respondWith(METHOD, body(FLAGS_V1, "e1", false, false));

        manager.refresh(null);
        assertEquals(0, network.count(METHOD));

        SdkStateProvider.getInstance().setReady();
        ShadowLooper.idleMainLooper();
        assertEquals(1, network.count(METHOD));
    }

    @Test
    public void refresh_sdkInError_failsCallbackImmediately() {
        SdkStateProvider.getInstance().resetForTesting();
        SdkStateProvider.getInstance().setError();

        List<Result<Void, FeatureFlagsException>> results = refresh();

        assertEquals(1, results.size());
        assertFalse(results.get(0).isSuccess());
        assertEquals(0, results.get(0).getException().getHttpStatus());
    }

    @Test
    public void listeners_throwingListenerDoesNotBlockOthers() throws Exception {
        int[] notified = {0};
        manager.addUpdateListener(() -> {
            throw new IllegalStateException("app bug");
        });
        manager.addUpdateListener(() -> notified[0]++);
        network.respondWith(METHOD, body(FLAGS_V1, "e1", false, false));

        List<Result<Void, FeatureFlagsException>> results = refresh();

        assertEquals(1, notified[0]);
        assertTrue(results.get(0).isSuccess());
    }

    @Test
    public void listeners_removedListenerIsNotCalled() throws Exception {
        int[] notified = {0};
        FeatureFlagsUpdateListener listener = () -> notified[0]++;
        manager.addUpdateListener(listener);
        manager.removeUpdateListener(listener);
        network.respondWith(METHOD, body(FLAGS_V1, "e1", false, false));

        refresh();

        assertEquals(0, notified[0]);
    }
}
