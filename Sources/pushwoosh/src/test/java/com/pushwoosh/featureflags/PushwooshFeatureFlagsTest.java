package com.pushwoosh.featureflags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.pushwoosh.function.Result;
import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.UserIdUpdatedEvent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class PushwooshFeatureFlagsTest extends FeatureFlagsManagerFixture {

    private FeatureFlagsManager platformManager;
    private final PushwooshFeatureFlags api = new PushwooshFeatureFlags(() -> platformManager);

    @Test
    public void isEnabled_usesCacheOrDefault() throws Exception {
        platformManager = manager;
        assertTrue(api.isEnabled("new_checkout", true));
        assertFalse(api.isEnabled("new_checkout", false));

        cacheV1();

        assertTrue(api.isEnabled("new_checkout", false));
        assertTrue(api.get("new_checkout").isEnabled());
    }

    @Test
    public void notInitialized_isSafe() {
        List<Result<Void, FeatureFlagsException>> results = new ArrayList<>();

        assertNull(api.get("k"));
        assertTrue(api.isEnabled("k", true));
        api.addUpdateListener(() -> {});
        api.logImpression("k");
        api.refresh(results::add);
        ShadowLooper.idleMainLooper();

        assertEquals(1, results.size());
        assertFalse(results.get(0).isSuccess());
        assertEquals(0, results.get(0).getException().getHttpStatus());
    }

    @Test
    public void listenerAddedBeforeRebuild_firesForNewManager_andGetSeesItsFlags() throws Exception {
        network.alwaysRespondWith(METHOD, body(FLAGS_V1, "e1", false, false));
        platformManager = manager;
        manager.start();
        List<String> updates = new ArrayList<>();
        api.addUpdateListener(() -> updates.add("updated"));

        platformManager = newManager(false);
        platformManager.start();
        prefs.userId().set("alice");
        EventBus.sendEvent(new UserIdUpdatedEvent());
        ShadowLooper.idleMainLooper();

        assertEquals(1, updates.size());
        assertTrue(api.get("new_checkout").isEnabled());
    }

    @Test
    public void refreshAfterRebuild_joinsNewManagersRequest() throws Exception {
        network.captureOnly(METHOD);
        platformManager = manager;
        manager.start();
        assertNull(api.get("new_checkout"));
        platformManager = newManager(false);
        platformManager.start();
        prefs.userId().set("alice");
        EventBus.sendEvent(new UserIdUpdatedEvent());
        ShadowLooper.idleMainLooper();
        assertEquals(1, network.count(METHOD));

        List<Result<Void, FeatureFlagsException>> results = new ArrayList<>();
        api.refresh(results::add);
        ShadowLooper.idleMainLooper();
        assertEquals(1, network.count(METHOD));

        network.deliver(network.last(METHOD), success(FLAGS_V1, "e1"));
        ShadowLooper.idleMainLooper();
        assertEquals(1, results.size());
        assertTrue(results.get(0).isSuccess());
        assertEquals(1, network.count(METHOD));
    }

    @Test
    public void refreshQueuedOnOldManagerBeforeRebuild_isSentAndAppliedByNewManager() throws Exception {
        network.alwaysRespondWith(METHOD, body(FLAGS_V1, "e1", false, false));
        SdkStateProvider.getInstance().resetForTesting();
        platformManager = manager;
        manager.start();
        List<String> updates = new ArrayList<>();
        api.addUpdateListener(() -> updates.add("updated"));
        List<Result<Void, FeatureFlagsException>> results = new ArrayList<>();
        api.refresh(results::add);

        platformManager = newManager(false);
        platformManager.start();
        SdkStateProvider.getInstance().setReady();
        ShadowLooper.idleMainLooper();

        assertEquals(1, network.count(METHOD));
        assertEquals(1, results.size());
        assertTrue(results.get(0).isSuccess());
        assertEquals(1, updates.size());
        assertTrue(api.get("new_checkout").isEnabled());
        assertNotNull(new FeatureFlagsStorage(disk).load());
    }

    @Test
    public void oldManagersResponseAfterRebuild_isNotApplied_andItsCallbackCompletesViaNewManager() throws Exception {
        network.captureOnly(METHOD);
        platformManager = manager;
        manager.start();
        List<String> updates = new ArrayList<>();
        api.addUpdateListener(() -> updates.add("updated"));
        List<Result<Void, FeatureFlagsException>> results = new ArrayList<>();
        api.refresh(results::add);
        ShadowLooper.idleMainLooper();
        assertEquals(1, network.count(METHOD));

        platformManager = newManager(false);
        platformManager.start();
        network.deliver(network.last(METHOD), success(FLAGS_V1, "e1"));
        ShadowLooper.idleMainLooper();

        assertEquals(2, network.count(METHOD));
        assertTrue(results.isEmpty());
        assertTrue(updates.isEmpty());
        assertNull(new FeatureFlagsStorage(disk).load());

        network.deliver(network.last(METHOD), success(FLAGS_V1, "e1"));
        ShadowLooper.idleMainLooper();

        assertEquals(1, results.size());
        assertTrue(results.get(0).isSuccess());
        assertEquals(1, updates.size());
        assertTrue(api.get("new_checkout").isEnabled());
    }

    @Test
    public void listenerChangesOnStoppedManager_reachTheCurrentOne() throws Exception {
        network.alwaysRespondWith(METHOD, body(FLAGS_V1, "e1", false, false));
        manager.start();
        FeatureFlagsManager second = newManager(true);
        second.start();
        List<String> kept = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        FeatureFlagsUpdateListener removedListener = () -> removed.add("updated");
        manager.addUpdateListener(() -> kept.add("updated"));
        manager.addUpdateListener(removedListener);
        manager.removeUpdateListener(removedListener);

        second.refresh(null);
        ShadowLooper.idleMainLooper();

        assertEquals(1, kept.size());
        assertTrue(removed.isEmpty());
    }

    @Test
    public void responseAppliedBeforeRebuild_isVisibleToNewManager() throws Exception {
        manager.start();
        FeatureFlagsManager second = newManager(false);
        cacheV1();

        second.start();

        assertTrue(second.get("new_checkout").isEnabled());
    }
}
