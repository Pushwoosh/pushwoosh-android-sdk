package com.pushwoosh.featureflags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.pushwoosh.function.Result;
import com.pushwoosh.internal.event.DeviceDataRemovedEvent;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.UserIdUpdatedEvent;
import com.pushwoosh.internal.network.ConnectionException;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.platform.ApplicationOpenDetector;
import com.pushwoosh.notification.event.RegistrationSuccessEvent;
import com.pushwoosh.tags.TagsBundle;

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
public class FeatureFlagsTriggersTest extends FeatureFlagsManagerFixture {

    @Test
    public void foreground_first_refreshes() throws Exception {
        network.respondWith(METHOD, body(FLAGS_V1, "e1", false, false));

        manager.onForeground();
        ShadowLooper.idleMainLooper();

        assertEquals(1, network.count(METHOD));
    }

    @Test
    public void foreground_afterShortBackground_isSameSession() throws Exception {
        network.respondWith(METHOD, body(FLAGS_V1, "e1", false, false));
        manager.onForeground();
        manager.onBackground();
        time.now += FeatureFlagsManager.SESSION_GAP_MS - 1;

        manager.onForeground();
        ShadowLooper.idleMainLooper();

        assertEquals(1, network.count(METHOD));
    }

    @Test
    public void foreground_after30MinutesInBackground_isNewSession() throws Exception {
        network.alwaysRespondWith(METHOD, body(FLAGS_V1, "e1", false, false));
        manager.onForeground();
        manager.onBackground();
        time.now += FeatureFlagsManager.SESSION_GAP_MS;

        manager.onForeground();
        ShadowLooper.idleMainLooper();

        assertEquals(2, network.count(METHOD));
    }

    @Test
    public void foreground_refreshesEvery60Minutes_andStopsInBackground() throws Exception {
        network.alwaysRespondWith(METHOD, body(FLAGS_V1, "e1", true, false));
        manager.onForeground();
        ShadowLooper.idleMainLooper();
        assertEquals(1, network.count(METHOD));

        time.now += FeatureFlagsManager.PERIODIC_REFRESH_MS;
        ShadowLooper.idleMainLooper(FeatureFlagsManager.PERIODIC_REFRESH_MS, TimeUnit.MILLISECONDS);
        assertEquals(2, network.count(METHOD));

        manager.onBackground();
        time.now += 3 * FeatureFlagsManager.PERIODIC_REFRESH_MS;
        ShadowLooper.idleMainLooper(3 * FeatureFlagsManager.PERIODIC_REFRESH_MS, TimeUnit.MILLISECONDS);
        assertEquals(2, network.count(METHOD));
    }

    @Test
    public void foreground_shortVisits_refreshAtMostAnHourAfterLastRequest() throws Exception {
        network.alwaysRespondWith(METHOD, body(FLAGS_V1, "e1", true, false));
        long start = time.now;
        List<Long> sentAt = new ArrayList<>();

        for (int cycle = 0; cycle < 5; cycle++) {
            manager.onForeground();
            ShadowLooper.idleMainLooper();
            sentAt.add(new FeatureFlagsStorage(disk).lastRequestAt() - start);
            advance(TimeUnit.MINUTES.toMillis(50));
            manager.onBackground();
            advance(TimeUnit.MINUTES.toMillis(10));
        }

        assertEquals(5, network.count(METHOD));
        List<Long> expected = new ArrayList<>();
        for (int cycle = 0; cycle < 5; cycle++) {
            expected.add(cycle * FeatureFlagsManager.PERIODIC_REFRESH_MS);
        }
        assertEquals(expected, sentAt);
    }

    @Test
    public void foreground_shortVisits_timerFiresAnHourAfterLastRequestNotAfterReturn() throws Exception {
        network.alwaysRespondWith(METHOD, body(FLAGS_V1, "e1", true, false));
        manager.onForeground();
        ShadowLooper.idleMainLooper();
        advance(TimeUnit.MINUTES.toMillis(45));
        manager.onBackground();
        advance(TimeUnit.MINUTES.toMillis(10));
        manager.onForeground();
        ShadowLooper.idleMainLooper();
        assertEquals(1, network.count(METHOD));

        advance(TimeUnit.MINUTES.toMillis(5) - 1);
        assertEquals(1, network.count(METHOD));
        advance(1);
        assertEquals(2, network.count(METHOD));

        advance(FeatureFlagsManager.PERIODIC_REFRESH_MS - 1);
        assertEquals(2, network.count(METHOD));
        advance(1);
        assertEquals(3, network.count(METHOD));
    }

    @Test
    public void foreground_returnAfterPeriodElapsedInBackground_refreshesPromptly() throws Exception {
        network.alwaysRespondWith(METHOD, body(FLAGS_V1, "e1", true, false));
        manager.onForeground();
        ShadowLooper.idleMainLooper();
        advance(TimeUnit.MINUTES.toMillis(40));
        manager.onBackground();
        advance(TimeUnit.MINUTES.toMillis(25));

        manager.onForeground();
        ShadowLooper.idleMainLooper();

        assertEquals(2, network.count(METHOD));
    }

    @Test
    public void foreground_repeatedReturns_armSingleTimer() throws Exception {
        network.alwaysRespondWith(METHOD, body(FLAGS_V1, "e1", true, false));
        manager.onForeground();
        manager.onForeground();
        manager.onForeground();
        ShadowLooper.idleMainLooper();

        assertEquals(1, ShadowLooper.getShadowMainLooper().getScheduler().size());
    }

    @Test
    public void foreground_requestByOtherTrigger_postponesPeriodicRefresh() throws Exception {
        network.alwaysRespondWith(METHOD, body(FLAGS_V1, "e1", true, false));
        manager.onForeground();
        ShadowLooper.idleMainLooper();
        advance(TimeUnit.MINUTES.toMillis(30));
        refresh();
        assertEquals(2, network.count(METHOD));

        advance(TimeUnit.MINUTES.toMillis(30));
        assertEquals(2, network.count(METHOD));

        advance(TimeUnit.MINUTES.toMillis(30) - 1);
        assertEquals(2, network.count(METHOD));
        advance(1);
        assertEquals(3, network.count(METHOD));
    }

    private void advance(long ms) {
        time.now += ms;
        ShadowLooper.idleMainLooper(ms, TimeUnit.MILLISECONDS);
    }

    @Test
    public void registration_afterUnregisteredRequest_refreshesOnceBypassingThrottle() throws Exception {
        prefs.registeredOnServer().set(false);
        network.alwaysRespondWith(METHOD, body(FLAGS_V1, "e1", false, false));
        refresh();
        time.now += 60_000;
        prefs.registeredOnServer().set(true);

        manager.onRegistered();
        ShadowLooper.idleMainLooper();
        manager.onRegistered();
        ShadowLooper.idleMainLooper();

        assertEquals(2, network.count(METHOD));
    }

    @Test
    public void registration_afterRegisteredRequest_doesNothing() throws Exception {
        cacheV1();

        manager.onRegistered();
        ShadowLooper.idleMainLooper();

        assertEquals(1, network.count(METHOD));
    }

    @Test
    public void userChange_clearsCacheAndEtag_refreshesBypassingThrottle() throws Exception {
        cacheV1();
        time.now += 60_000;
        prefs.userId().set("alice");
        assertNull(manager.get("new_checkout"));
        network.captureOnly(METHOD);

        manager.onUserIdUpdated();
        ShadowLooper.idleMainLooper();

        assertEquals(2, network.count(METHOD));
        assertFalse(network.last(METHOD).params.has("etag"));
        assertNull(new FeatureFlagsStorage(disk).load());
        prefs.userId().set("");
        assertNull(manager.get("new_checkout"));
    }

    @Test
    public void userChange_newUserFlagsServedAfterResponse() throws Exception {
        cacheV1();
        time.now += 60_000;
        prefs.userId().set("alice");
        network.respondWith(METHOD, body(FLAGS_V2, "e2", false, false));

        manager.onUserIdUpdated();
        ShadowLooper.idleMainLooper();

        assertFalse(manager.get("new_checkout").isEnabled());
        assertEquals("alice", new FeatureFlagsStorage(disk).load().userId);
    }

    @Test
    public void userIdUpdated_sameEffectiveUser_keepsCacheAndRefreshesWithoutEtag() throws Exception {
        cacheV1();
        prefs.userId().set(HWID);
        network.captureOnly(METHOD);

        manager.onUserIdUpdated();
        ShadowLooper.idleMainLooper();

        assertEquals(2, network.count(METHOD));
        assertFalse(network.last(METHOD).params.has("etag"));
        assertTrue(manager.get("new_checkout").isEnabled());
    }

    @Test
    public void userIdUpdated_afterRefreshInSetUserIdWindow_replacesOldUserFlags() throws Exception {
        prefs.userId().set("u2");
        cacheV1();
        assertEquals("u2", new FeatureFlagsStorage(disk).load().userId);
        time.now += 60_000;
        network.respondWith(METHOD, body(FLAGS_V2, "e2", false, false));

        manager.onUserIdUpdated();
        ShadowLooper.idleMainLooper();

        assertEquals(2, network.count(METHOD));
        assertFalse(network.last(METHOD).params.has("etag"));
        assertFalse(manager.get("new_checkout").isEnabled());
        FeatureFlagsStorage.Snapshot saved = new FeatureFlagsStorage(disk).load();
        assertEquals("u2", saved.userId);
        assertEquals("e2", saved.etag);
    }

    @Test
    public void userIdUpdated_sameUserWhileInFlight_dropsWindowResponse_followUpWithoutEtag() throws Exception {
        prefs.userId().set("u2");
        cacheV1();
        pastThrottle();
        network.captureOnly(METHOD);
        manager.refresh(null);
        ShadowLooper.idleMainLooper();
        assertEquals("e1", network.last(METHOD).params.getString("etag"));

        manager.onUserIdUpdated();
        ShadowLooper.idleMainLooper();
        network.deliver(network.last(METHOD), success(FLAGS_V1, "e1-old-user"));
        ShadowLooper.idleMainLooper();

        assertEquals(3, network.count(METHOD));
        assertFalse(network.last(METHOD).params.has("etag"));
        assertTrue(manager.get("new_checkout").isEnabled());
        network.deliver(network.last(METHOD), success(FLAGS_V2, "e2"));
        ShadowLooper.idleMainLooper();
        assertFalse(manager.get("new_checkout").isEnabled());
        assertEquals("e2", new FeatureFlagsStorage(disk).load().etag);
    }

    @Test
    public void deviceDataRemoved_wipesMemoryAndDisk() throws Exception {
        cacheV1();

        manager.onDeviceDataRemoved();

        assertNull(manager.get("new_checkout"));
        assertNull(new FeatureFlagsStorage(disk).load());
    }

    @Test
    public void deviceDataRemovedWhileInFlight_responseIsNotCached() throws Exception {
        network.captureOnly(METHOD);
        manager.refresh(null);
        ShadowLooper.idleMainLooper();

        manager.onDeviceDataRemoved();
        network.deliver(network.last(METHOD), success(FLAGS_V1, "e1"));
        ShadowLooper.idleMainLooper();

        assertNull(new FeatureFlagsStorage(disk).load());
        assertNull(manager.get("new_checkout"));
    }

    @Test
    public void impression_dedupedPerSession_withTypedAttributes() throws Exception {
        cacheV1();

        manager.logImpression("new_checkout");
        manager.logImpression("new_checkout");
        manager.logImpression("new_checkout");

        assertEquals(1, postedEvents.size());
        assertEquals("PW_FeatureFlagImpression", postedEvents.get(0));
        TagsBundle attributes = postedAttributes.get(0);
        assertEquals("new_checkout", attributes.getString("flag_key"));
        assertEquals("r_1", attributes.getString("rule_id"));
        assertTrue(attributes.getBoolean("enabled", false));
    }

    @Test
    public void impression_killedFlag_sendsEmptyRuleIdAndFalse() throws Exception {
        network.respondWith(METHOD, body(FLAGS_V2, "e2", false, false));
        refresh();

        manager.logImpression("new_checkout");

        assertEquals("", postedAttributes.get(0).getString("rule_id"));
        assertFalse(postedAttributes.get(0).getBoolean("enabled", true));
    }

    @Test
    public void impression_unknownFlag_sendsNothing() {
        manager.logImpression("missing");

        assertTrue(postedEvents.isEmpty());
    }

    @Test
    public void impression_changedFlagValue_isNewDedupKey() throws Exception {
        cacheV1();
        manager.logImpression("new_checkout");
        pastThrottle();
        network.respondWith(METHOD, body(FLAGS_V2, "e2", false, false));
        refresh();

        manager.logImpression("new_checkout");

        assertEquals(2, postedEvents.size());
    }

    @Test
    public void impression_newSession_resetsDedup() throws Exception {
        network.alwaysRespondWith(METHOD, body(FLAGS_V1, "e1", false, false));
        manager.onForeground();
        ShadowLooper.idleMainLooper();
        manager.logImpression("new_checkout");
        manager.onBackground();
        time.now += FeatureFlagsManager.SESSION_GAP_MS;
        manager.onForeground();
        ShadowLooper.idleMainLooper();

        manager.logImpression("new_checkout");

        assertEquals(2, postedEvents.size());
    }

    @Test
    public void impression_loggedBeforeFirstForeground_isNotResentInSameSession() throws Exception {
        cacheV1();
        manager.logImpression("new_checkout");

        manager.onForeground();
        ShadowLooper.idleMainLooper();
        manager.logImpression("new_checkout");

        assertEquals(1, postedEvents.size());
    }

    @Test
    public void impression_shortBackground_keepsDedup() throws Exception {
        cacheV1();
        manager.logImpression("new_checkout");
        manager.onForeground();
        manager.onBackground();
        time.now += FeatureFlagsManager.SESSION_GAP_MS - 1;

        manager.onForeground();
        ShadowLooper.idleMainLooper();
        manager.logImpression("new_checkout");

        assertEquals(1, postedEvents.size());
    }

    @Test
    public void impression_coldStartThen30MinutesInBackground_resetsDedup() throws Exception {
        cacheV1();
        network.alwaysRespondWith(METHOD, body(FLAGS_V1, "e1", false, false));
        manager.logImpression("new_checkout");
        manager.onForeground();
        manager.onBackground();
        time.now += FeatureFlagsManager.SESSION_GAP_MS;

        manager.onForeground();
        ShadowLooper.idleMainLooper();
        manager.logImpression("new_checkout");

        assertEquals(2, postedEvents.size());
    }

    @Test
    public void impression_afterDeviceDataRemoved_isSentAgain() throws Exception {
        cacheV1();
        manager.logImpression("new_checkout");

        manager.onDeviceDataRemoved();
        pastThrottle();
        cacheV1();
        manager.logImpression("new_checkout");

        assertEquals(2, postedEvents.size());
    }

    @Test
    public void impression_afterUserChange_isSentAgain() throws Exception {
        cacheV1();
        manager.logImpression("new_checkout");
        prefs.userId().set("alice");
        network.respondWith(METHOD, body(FLAGS_V1, "e1", false, false));

        manager.onUserIdUpdated();
        ShadowLooper.idleMainLooper();
        manager.logImpression("new_checkout");

        assertEquals(2, postedEvents.size());
    }

    @Test
    public void impression_failedPost_isSentAgainByLaterCall() throws Exception {
        cacheV1();
        holdPosts = true;
        manager.logImpression("new_checkout");
        completePost(0, false);

        manager.logImpression("new_checkout");

        assertEquals(2, postedEvents.size());
    }

    @Test
    public void impression_succeededPost_laterCallsSendNothing() throws Exception {
        cacheV1();
        holdPosts = true;
        manager.logImpression("new_checkout");
        completePost(0, true);

        manager.logImpression("new_checkout");
        manager.logImpression("new_checkout");

        assertEquals(1, postedEvents.size());
    }

    @Test
    public void impression_callsWhilePostInFlight_sendOnePost() throws Exception {
        cacheV1();
        holdPosts = true;

        manager.logImpression("new_checkout");
        manager.logImpression("new_checkout");
        manager.logImpression("new_checkout");

        assertEquals(1, postedEvents.size());
    }

    @Test
    public void impression_postInFlightAcrossNewSession_doesNotConsumeNewSessionKey() throws Exception {
        network.alwaysRespondWith(METHOD, body(FLAGS_V1, "e1", false, false));
        holdPosts = true;
        manager.onForeground();
        ShadowLooper.idleMainLooper();
        manager.logImpression("new_checkout");
        manager.onBackground();
        time.now += FeatureFlagsManager.SESSION_GAP_MS;
        manager.onForeground();
        ShadowLooper.idleMainLooper();
        manager.logImpression("new_checkout");
        assertEquals(2, postedEvents.size());

        completePost(0, true);
        completePost(1, false);
        manager.logImpression("new_checkout");

        assertEquals(3, postedEvents.size());
    }

    @Test
    public void impression_loggedBeforeLazyRebuild_isNotResentByNewManager() throws Exception {
        manager.start();
        cacheV1();
        manager.logImpression("new_checkout");
        FeatureFlagsManager rebuilt = newManager(false);
        rebuilt.start();

        rebuilt.logImpression("new_checkout");

        assertEquals(1, postedEvents.size());
    }

    @Test
    public void impression_inFlightAtLazyRebuild_isOwnedByNewManager() throws Exception {
        manager.start();
        cacheV1();
        holdPosts = true;
        manager.logImpression("new_checkout");
        FeatureFlagsManager rebuilt = newManager(false);
        rebuilt.start();

        rebuilt.logImpression("new_checkout");
        assertEquals(1, postedEvents.size());

        completePost(0, false);
        rebuilt.logImpression("new_checkout");
        assertEquals(2, postedEvents.size());

        completePost(1, true);
        rebuilt.logImpression("new_checkout");
        assertEquals(2, postedEvents.size());
    }

    @Test
    public void start_subscribesToLifecycleRegistrationUserAndWipeEvents() throws Exception {
        network.alwaysRespondWith(METHOD, body(FLAGS_V1, "e1", false, false));
        prefs.registeredOnServer().set(false);
        manager.start();

        EventBus.sendEvent(new ApplicationOpenDetector.ApplicationMovedToForegroundEvent());
        ShadowLooper.idleMainLooper();
        assertEquals(1, network.count(METHOD));

        prefs.registeredOnServer().set(true);
        EventBus.sendEvent(new RegistrationSuccessEvent(null));
        ShadowLooper.idleMainLooper();
        assertEquals(2, network.count(METHOD));

        prefs.userId().set("alice");
        EventBus.sendEvent(new UserIdUpdatedEvent());
        ShadowLooper.idleMainLooper();
        assertEquals(3, network.count(METHOD));

        EventBus.sendEvent(new ApplicationOpenDetector.ApplicationMovedToBackgroundEvent());
        EventBus.sendEvent(new DeviceDataRemovedEvent());
        ShadowLooper.idleMainLooper();
        assertNull(new FeatureFlagsStorage(disk).load());
    }

    @Test
    public void start_twice_reactsToEachEventOnce() throws Exception {
        network.alwaysRespondWith(METHOD, body(FLAGS_V1, "e1", false, false));
        manager.start();
        manager.start();

        prefs.userId().set("alice");
        EventBus.sendEvent(new UserIdUpdatedEvent());
        ShadowLooper.idleMainLooper();

        assertEquals(1, network.count(METHOD));
    }

    @Test
    public void start_ofNewerManager_detachesPreviousOne() throws Exception {
        network.alwaysRespondWith(METHOD, body(FLAGS_V1, "e1", false, false));
        manager.start();
        EventBus.sendEvent(new ApplicationOpenDetector.ApplicationMovedToForegroundEvent());
        ShadowLooper.idleMainLooper();
        assertEquals(1, network.count(METHOD));

        newManager(false).start();
        time.now += FeatureFlagsManager.PERIODIC_REFRESH_MS;
        ShadowLooper.idleMainLooper(FeatureFlagsManager.PERIODIC_REFRESH_MS, TimeUnit.MILLISECONDS);
        assertEquals(1, network.count(METHOD));

        prefs.userId().set("alice");
        EventBus.sendEvent(new UserIdUpdatedEvent());
        ShadowLooper.idleMainLooper();
        assertEquals(2, network.count(METHOD));
    }

    @Test
    public void userChangesWhileInFlight_sendOneFollowUp_whoseResultReachesItsCallbacks() throws Exception {
        network.captureOnly(METHOD);
        List<Result<Void, FeatureFlagsException>> first = new ArrayList<>();
        manager.refresh(first::add);
        ShadowLooper.idleMainLooper();

        prefs.userId().set("alice");
        manager.onUserIdUpdated();
        prefs.userId().set("bob");
        manager.onUserIdUpdated();
        ShadowLooper.idleMainLooper();
        assertEquals(1, network.count(METHOD));

        network.deliver(network.last(METHOD), success(FLAGS_V1, "e1"));
        ShadowLooper.idleMainLooper();
        assertEquals(2, network.count(METHOD));
        List<Result<Void, FeatureFlagsException>> second = new ArrayList<>();
        manager.refresh(second::add);
        ShadowLooper.idleMainLooper();
        assertEquals(2, network.count(METHOD));

        network.deliver(
                network.last(METHOD),
                Result.<FeatureFlagsResponse, NetworkException>fromException(
                        new ConnectionException("failed", 500, 500)));
        ShadowLooper.idleMainLooper();

        assertEquals(2, network.count(METHOD));
        assertTrue(first.get(0).isSuccess());
        assertEquals(1, second.size());
        assertEquals(500, second.get(0).getException().getHttpStatus());
    }

    @Test
    public void userChangeWhileInFlight_dropsOldUserResponse_cachesFollowUpForNewUser() throws Exception {
        network.captureOnly(METHOD);
        manager.refresh(null);
        ShadowLooper.idleMainLooper();
        prefs.userId().set("alice");
        manager.onUserIdUpdated();
        ShadowLooper.idleMainLooper();

        network.deliver(network.last(METHOD), success(FLAGS_V1, "e1"));
        ShadowLooper.idleMainLooper();
        assertNull(manager.get("new_checkout"));
        assertNull(new FeatureFlagsStorage(disk).load());

        network.deliver(network.last(METHOD), success(FLAGS_V2, "e2"));
        ShadowLooper.idleMainLooper();

        assertFalse(manager.get("new_checkout").isEnabled());
        FeatureFlagsStorage.Snapshot saved = new FeatureFlagsStorage(disk).load();
        assertEquals("alice", saved.userId);
        assertEquals("e2", saved.etag);
    }
}
