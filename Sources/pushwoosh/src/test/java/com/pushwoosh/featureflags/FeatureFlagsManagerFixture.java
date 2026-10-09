package com.pushwoosh.featureflags;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import android.content.SharedPreferences;

import com.pushwoosh.exception.PostEventException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.network.FakeRequestManager;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.prefs.TestPrefsProvider;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.internal.utils.TimeProvider;
import com.pushwoosh.repository.DeviceRegistrar;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryTestManager;
import com.pushwoosh.tags.TagsBundle;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public abstract class FeatureFlagsManagerFixture {
    static final String APP = "70699-8A0F4";
    static final String HWID = "hwid-1";
    static final String ENDPOINT = "https://api.pushwoosh.com/api/v2/device-api/";
    static final String METHOD = GetFeatureFlagsRequest.METHOD;
    static final String FLAGS_V1 =
            "{\"new_checkout\":{\"enabled\":true,\"rule_id\":\"r_1\",\"properties\":{\"color\":\"green\"}}}";
    static final String FLAGS_V2 =
            "{\"new_checkout\":{\"enabled\":false,\"rule_id\":\"\",\"properties\":{\"color\":\"blue\"}}}";

    static final class FakeTime extends TimeProvider {
        long now = 1_700_000_000_000L;

        @Override
        public long getCurrentTime() {
            return now;
        }
    }

    RegistrationPrefs prefs;
    FakeRequestManager network;
    SharedPreferences disk;
    FakeTime time;
    List<String> postedEvents;
    List<TagsBundle> postedAttributes;
    List<Callback<Void, PostEventException>> pendingPosts;
    boolean holdPosts;
    FeatureFlagsManager manager;

    @Before
    public void setUp() {
        AndroidPlatformModule.init(RuntimeEnvironment.application, true);
        prefs = RepositoryTestManager.createRegistrationPrefs(MockConfig.createMock(APP), mock(DeviceRegistrar.class));
        prefs.applicationId().set(APP);
        prefs.hwid().set(HWID);
        prefs.userId().set("");
        prefs.baseUrl().set("https://api.pushwoosh.com/json/1.3/");
        prefs.registeredOnServer().set(true);
        network = FakeRequestManager.install();
        disk = new TestPrefsProvider().providePrefs(FeatureFlagsStorage.PREFS_NAME);
        time = new FakeTime();
        postedEvents = new ArrayList<>();
        postedAttributes = new ArrayList<>();
        pendingPosts = new ArrayList<>();
        holdPosts = false;
        SdkStateProvider.getInstance().resetForTesting();
        SdkStateProvider.getInstance().setReady();
        manager = newManager(false);
    }

    @After
    public void tearDown() {
        FeatureFlagsManager.resetForTests();
        SdkStateProvider.getInstance().resetForTesting();
        NetworkModule.setRequestManager(null);
        EventBus.clearSubscribersMap();
        prefs.removeAllDeviceData().set(false);
        RepositoryTestManager.destroyRegistrationPrefs(prefs);
    }

    FeatureFlagsManager newManager(boolean throttleDisabled) {
        return new FeatureFlagsManager(
                prefs,
                network,
                new FeatureFlagsStorage(disk),
                (event, attributes, done) -> {
                    postedEvents.add(event);
                    postedAttributes.add(attributes);
                    if (holdPosts) {
                        pendingPosts.add(done);
                    } else {
                        done.process(Result.fromData(null));
                    }
                },
                time,
                throttleDisabled);
    }

    static JSONObject body(String flags, String etag, boolean notModified, boolean partial) throws JSONException {
        return new JSONObject()
                .put("flags", new JSONObject(flags))
                .put("etag", etag)
                .put("not_modified", notModified)
                .put("partial", partial);
    }

    static Result<FeatureFlagsResponse, NetworkException> success(String flags, String etag) throws Exception {
        return Result.fromData(FeatureFlagsResponse.from(body(flags, etag, false, false)));
    }

    List<Result<Void, FeatureFlagsException>> refresh() {
        List<Result<Void, FeatureFlagsException>> results = new ArrayList<>();
        manager.refresh(results::add);
        ShadowLooper.idleMainLooper();
        return results;
    }

    void cacheV1() throws Exception {
        network.respondWith(METHOD, body(FLAGS_V1, "e1", false, false));
        refresh();
        assertNotNull(manager.get("new_checkout"));
    }

    void completePost(int index, boolean success) {
        pendingPosts
                .get(index)
                .process(
                        success
                                ? Result.<Void, PostEventException>fromData(null)
                                : Result.<Void, PostEventException>fromException(new PostEventException("offline")));
    }

    void pastThrottle() {
        time.now += FeatureFlagsManager.THROTTLE_MS + 1;
    }
}
