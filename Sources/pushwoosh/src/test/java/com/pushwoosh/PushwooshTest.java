package com.pushwoosh;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Bundle;

import androidx.work.Configuration;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.TestDriver;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.pushwoosh.exception.RegisterForPushNotificationsException;
import com.pushwoosh.exception.SetUserException;
import com.pushwoosh.exception.SetUserIdException;
import com.pushwoosh.featureflags.PushwooshFeatureFlags;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.network.InAppRepository;
import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.ReverseProxyReadyEvent;
import com.pushwoosh.internal.event.Subscription;
import com.pushwoosh.internal.network.ConnectionException;
import com.pushwoosh.internal.network.FakeRequestManager;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.registrar.RegisterDeviceWorker;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.testutil.CallbackWrapper;
import com.pushwoosh.testutil.PlatformTestManager;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class PushwooshTest {
    private PlatformTestManager platformTestManager;

    private static final String HASH = "test_hash";
    private static final String METADATA = "test_metadata";
    private PushwooshRepository pushwooshRepository;
    private RegistrationPrefs registrationPrefs;
    private PushwooshNotificationManager notificationManagerSpy;
    private WorkManager workManager;
    private TestDriver testDriver;
    private MockedStatic<PushwooshMessagingServiceHelper> messagingServiceHelperMock;

    private Method sendMessageDelivery;
    private Method sendPushStat;

    @Before
    public void setUp() throws Exception {
        Config configMock = MockConfig.createMock();

        platformTestManager = new PlatformTestManager(configMock);
        platformTestManager.onApplicationCreated();

        Context context = AndroidPlatformModule.getApplicationContext();

        Configuration config = new Configuration.Builder()
                .setExecutor(new SynchronousExecutor())
                .setTaskExecutor(new SynchronousExecutor())
                .build();

        WorkManagerTestInitHelper.initializeTestWorkManager(context, config);

        workManager = WorkManager.getInstance(context);
        testDriver = WorkManagerTestInitHelper.getTestDriver(context);

        pushwooshRepository = platformTestManager.getPushwooshRepositoryMock();
        registrationPrefs = platformTestManager.getRegistrationPrefs();
        notificationManagerSpy = Mockito.spy(platformTestManager.getNotificationManager());
        setField(PushwooshPlatform.getInstance(), "notificationManager", notificationManagerSpy);

        // Both singletons cache collaborators when first created, PlatformTestManager resets them
        // after every test. Re-point the cached fields at the current sandbox instances.
        setField(Pushwoosh.getInstance(), "registrationPrefs", platformTestManager.getRegistrationPrefs());
        setField(Pushwoosh.getInstance(), "notificationManager", notificationManagerSpy);
        setField(Pushwoosh.getInstance(), "inAppRepository", InAppModule.getInAppRepository());

        sendPushStat = Pushwoosh.class.getDeclaredMethod("sendPushStat", Bundle.class);
        sendMessageDelivery = Pushwoosh.class.getDeclaredMethod("sendMessageDelivery", Bundle.class);
        sendPushStat.setAccessible(true);
        sendMessageDelivery.setAccessible(true);

        // Mock PushwooshMessagingServiceHelper static methods
        messagingServiceHelperMock = Mockito.mockStatic(PushwooshMessagingServiceHelper.class);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @After
    public void tearDown() throws Exception {
        if (messagingServiceHelperMock != null) {
            messagingServiceHelperMock.close();
        }
        platformTestManager.tearDown();
    }

    private static InAppRepository mockInAppRepository() throws Exception {
        InAppRepository repository = Mockito.mock(InAppRepository.class);
        setField(Pushwoosh.getInstance(), "inAppRepository", repository);
        setField(Pushwoosh.getInstance(), "isInitialized", true);
        SdkStateProvider.getInstance().setReady();
        return repository;
    }

    private static void answerSetUserId(
            InAppRepository repository, String userId, Result<Boolean, SetUserIdException> result) {
        Mockito.doAnswer(invocation -> {
                    Callback<Boolean, SetUserIdException> callback = invocation.getArgument(1);
                    callback.process(result);
                    return null;
                })
                .when(repository)
                .setUserId(Mockito.eq(userId), Mockito.any());
    }

    private static void answerSetUserEmails(InAppRepository repository, Result<Boolean, SetUserException> result) {
        Mockito.doAnswer(invocation -> {
                    Callback<Boolean, SetUserException> callback = invocation.getArgument(1);
                    if (callback != null) {
                        callback.process(result);
                    }
                    return null;
                })
                .when(repository)
                .setUserEmails(Mockito.anyList(), Mockito.any());
    }

    // Verifies that setUser with the stored User ID only registers the emails: /registerUser would fire
    // UserIdUpdatedEvent, and feature flags refetch without etag on every such event.
    @Test
    public void setUser_sameUserId_registersEmailsWithoutRegisterUser() throws Exception {
        InAppRepository repository = mockInAppRepository();
        try {
            registrationPrefs.userId().set("u1");
            List<String> emails = Collections.singletonList("a@x.com");

            Pushwoosh.getInstance().setUser("u1", emails);

            Mockito.verify(repository).setUserEmails(Mockito.eq(emails), Mockito.isNull());
            Mockito.verify(repository, Mockito.never()).setUserId(Mockito.anyString(), Mockito.any());
        } finally {
            SdkStateProvider.getInstance().resetForTesting();
        }
    }

    // Verifies that setUser with a new User ID goes through /registerUser and registers the emails after it.
    @Test
    public void setUser_newUserId_registersUserThenEmails() throws Exception {
        InAppRepository repository = mockInAppRepository();
        try {
            registrationPrefs.userId().set("u1");
            List<String> emails = Collections.singletonList("a@x.com");
            answerSetUserId(repository, "u2", Result.fromData(true));

            Pushwoosh.getInstance().setUser("u2", emails);

            Mockito.verify(repository).setUserId(Mockito.eq("u2"), Mockito.any());
            Mockito.verify(repository).setUserEmails(Mockito.eq(emails), Mockito.isNull());
            assertThat(registrationPrefs.userId().get(), equalTo("u2"));
        } finally {
            SdkStateProvider.getInstance().resetForTesting();
        }
    }

    // Verifies that a failed /registerUser restores the previous User ID, so the next setUser with the same ID
    // goes through /registerUser again instead of the same-user short-circuit.
    @Test
    public void setUser_registerUserFails_restoresPreviousUserId() throws Exception {
        InAppRepository repository = mockInAppRepository();
        try {
            registrationPrefs.userId().set("u1");
            List<String> emails = Collections.singletonList("a@x.com");
            answerSetUserId(repository, "u2", Result.fromException(new SetUserIdException("offline")));
            List<Result<Boolean, SetUserException>> results = new ArrayList<>();

            Pushwoosh.getInstance().setUser("u2", emails, results::add);

            assertThat(registrationPrefs.userId().get(), equalTo("u1"));
            assertThat(results.size(), equalTo(1));
            assertThat(results.get(0).getException().getMessage(), equalTo("offline"));
            Mockito.verify(repository, Mockito.never()).setUserEmails(Mockito.anyList(), Mockito.any());

            Pushwoosh.getInstance().setUser("u2", emails);

            Mockito.verify(repository, Mockito.times(2)).setUserId(Mockito.eq("u2"), Mockito.any());
        } finally {
            SdkStateProvider.getInstance().resetForTesting();
        }
    }

    // Verifies that a /registerEmail failure after a successful /registerUser keeps the new User ID: the server
    // already knows it, and rolling back would make the next setUser re-register the user.
    @Test
    public void setUser_registerEmailFails_keepsNewUserId() throws Exception {
        InAppRepository repository = mockInAppRepository();
        try {
            registrationPrefs.userId().set("u1");
            List<String> emails = Collections.singletonList("a@x.com");
            answerSetUserId(repository, "u2", Result.fromData(true));
            answerSetUserEmails(repository, Result.fromException(new SetUserException("bad email")));
            List<Result<Boolean, SetUserException>> results = new ArrayList<>();

            Pushwoosh.getInstance().setUser("u2", emails, results::add);

            assertThat(registrationPrefs.userId().get(), equalTo("u2"));
            assertThat(results.size(), equalTo(1));
            assertThat(results.get(0).isSuccess(), equalTo(false));

            Pushwoosh.getInstance().setUser("u2", emails);

            Mockito.verify(repository, Mockito.times(1)).setUserId(Mockito.eq("u2"), Mockito.any());
            Mockito.verify(repository, Mockito.times(2)).setUserEmails(Mockito.eq(emails), Mockito.any());
        } finally {
            SdkStateProvider.getInstance().resetForTesting();
        }
    }

    // Verifies that a failed /registerUser from setUserId restores the previous User ID the same way.
    @Test
    public void setUserId_registerUserFails_restoresPreviousUserId() throws Exception {
        InAppRepository repository = mockInAppRepository();
        try {
            registrationPrefs.userId().set("u1");
            answerSetUserId(repository, "u2", Result.fromException(new SetUserIdException("offline")));
            List<Result<Boolean, SetUserIdException>> results = new ArrayList<>();

            Pushwoosh.getInstance().setUserId("u2", results::add);

            assertThat(registrationPrefs.userId().get(), equalTo("u1"));
            assertThat(results.size(), equalTo(1));
            assertThat(results.get(0).isSuccess(), equalTo(false));

            Pushwoosh.getInstance().setUserId("u2");

            Mockito.verify(repository, Mockito.times(2)).setUserId(Mockito.eq("u2"), Mockito.any());
        } finally {
            SdkStateProvider.getInstance().resetForTesting();
        }
    }

    @Test
    public void getFeatureFlags_isOneInstanceThatReadsThePlatformManager() throws Exception {
        PushwooshFeatureFlags flags = Pushwoosh.getInstance().getFeatureFlags();
        assertNull(flags.get("new_checkout"));
        RegistrationPrefs prefs = PushwooshPlatform.getInstance().getRegistrationPrefs();
        prefs.applicationId().set("70699-8A0F4");
        prefs.hwid().set("hwid-1");
        prefs.baseUrl().set("https://api.pushwoosh.com/json/1.3/");
        SdkStateProvider.getInstance().setReady();
        platformTestManager
                .getRequestManager()
                .respondWith(
                        "getFeatureFlags",
                        new JSONObject("{\"flags\":{\"new_checkout\":{\"enabled\":true,\"rule_id\":\"r_1\"}},"
                                + "\"etag\":\"e1\",\"not_modified\":false,\"partial\":false}"));

        flags.refresh(null);
        ShadowLooper.idleMainLooper();

        assertSame(flags, Pushwoosh.getInstance().getFeatureFlags());
        assertTrue(flags.isEnabled("new_checkout", false));
    }

    @Test
    public void shouldSendExistingTokenOnce() throws Exception {
        String testToken = "12345";
        FakeRequestManager fake = platformTestManager.getRequestManager();
        fake.respondWith("registerDevice", new JSONObject());
        Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback =
                CallbackWrapper.spy();

        Pushwoosh.getInstance().registerExistingToken(testToken, callback);
        List<WorkInfo> infos =
                workManager.getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG).get();
        assertThat(infos, hasSize(1));
        UUID id = infos.get(0).getId();
        testDriver.setAllConstraintsMet(id);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        Pushwoosh.getInstance().registerExistingToken(testToken, callback);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertThat(workManager.getWorkInfoById(id).get().getState(), equalTo(WorkInfo.State.SUCCEEDED));
        assertThat(
                workManager.getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG).get(), hasSize(1));
        assertThat(fake.count("registerDevice"), equalTo(1));
        assertThat(fake.last("registerDevice").params.getString("push_token"), equalTo(testToken));
        ArgumentCaptor<Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException>> captor =
                ArgumentCaptor.forClass(Result.class);
        Mockito.verify(callback, Mockito.times(2)).process(captor.capture());
        for (Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> r :
                captor.getAllValues()) {
            assertThat(r.isSuccess(), equalTo(true));
            assertThat(r.getData().getToken(), equalTo(testToken));
        }
    }

    // The token is saved at intake, before it ever reaches the server, so "same token" alone does not mean
    // the server knows it: after a terminal delivery failure the retry must schedule a new delivery.
    @Test
    public void registerExistingToken_afterTerminalFailure_schedulesDeliveryAgain() throws Exception {
        String testToken = "12345";
        FakeRequestManager fake = platformTestManager.getRequestManager();
        fake.failWith("registerDevice", new ConnectionException("Bad Request", 400, 0));
        Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> failedCallback =
                CallbackWrapper.spy();
        Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> retryCallback =
                CallbackWrapper.spy();

        Pushwoosh.getInstance().registerExistingToken(testToken, failedCallback);
        UUID failedId = pendingDeliveryId();
        testDriver.setAllConstraintsMet(failedId);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertThat(workManager.getWorkInfoById(failedId).get().getState(), equalTo(WorkInfo.State.FAILED));
        assertThat(resultOf(failedCallback).isSuccess(), equalTo(false));

        fake.respondWith("registerDevice", new JSONObject());
        Pushwoosh.getInstance().registerExistingToken(testToken, retryCallback);
        UUID retryId = pendingDeliveryId();
        testDriver.setAllConstraintsMet(retryId);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertThat(workManager.getWorkInfoById(retryId).get().getState(), equalTo(WorkInfo.State.SUCCEEDED));
        assertThat(fake.count("registerDevice"), equalTo(2));
        Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> retried =
                resultOf(retryCallback);
        assertThat(retried.isSuccess(), equalTo(true));
        assertThat(retried.getData().getToken(), equalTo(testToken));
    }

    private UUID pendingDeliveryId() throws Exception {
        for (WorkInfo info :
                workManager.getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG).get()) {
            if (!info.getState().isFinished()) {
                return info.getId();
            }
        }
        throw new AssertionError("no pending RegisterDeviceWorker");
    }

    private static Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> resultOf(
            Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback) {
        ArgumentCaptor<Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException>> captor =
                ArgumentCaptor.forClass(Result.class);
        Mockito.verify(callback).process(captor.capture());
        return captor.getValue();
    }

    @Test
    public void registerExistingToken_emptyToken_failsWithoutWorker() throws ExecutionException, InterruptedException {
        Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback =
                CallbackWrapper.spy();

        Pushwoosh.getInstance().registerExistingToken("", callback);

        ArgumentCaptor<Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException>> captor =
                ArgumentCaptor.forClass(Result.class);
        Mockito.verify(callback).process(captor.capture());
        assertThat(captor.getValue().isSuccess(), equalTo(false));
        List<WorkInfo> infos =
                workManager.getWorkInfosForUniqueWork(RegisterDeviceWorker.TAG).get();
        assertThat(infos, hasSize(0));
        Mockito.verify(notificationManagerSpy, Mockito.never()).onTokenReceived(Mockito.any(), Mockito.any());
    }

    private Pushwoosh spyWith(String appCode, String hwid) {
        Pushwoosh pushwoosh = Mockito.spy(Pushwoosh.getInstance());
        Mockito.doReturn(appCode).when(pushwoosh).getApplicationCode();
        Mockito.doReturn(hwid).when(pushwoosh).getHwid();
        return pushwoosh;
    }

    @Test
    public void shouldBuildSubscriptionAccountIdFromAppCodeAndHwid() {
        Pushwoosh pushwoosh = spyWith("XXXXX-XXXXX", "550e8400-e29b-41d4-a716-446655440000");

        assertThat(pushwoosh.getSubscriptionAccountId(), equalTo("XXXXX-XXXXX:550e8400-e29b-41d4-a716-446655440000"));
    }

    @Test
    public void shouldReturnEmptySubscriptionAccountIdWhenAppCodeEmpty() {
        Pushwoosh pushwoosh = spyWith("", "550e8400-e29b-41d4-a716-446655440000");

        assertThat(pushwoosh.getSubscriptionAccountId(), equalTo(""));
    }

    @Test
    public void shouldReturnEmptySubscriptionAccountIdWhenHwidEmpty() {
        Pushwoosh pushwoosh = spyWith("XXXXX-XXXXX", "");

        assertThat(pushwoosh.getSubscriptionAccountId(), equalTo(""));
    }

    @Test
    public void shouldReturnFullSubscriptionAccountIdWhenExceeds64Chars() {
        String appCode = "XXXXX-XXXXX";
        String longHwid = "550e8400-e29b-41d4-a716-446655440000-extra-long-suffix-overflow";
        Pushwoosh pushwoosh = spyWith(appCode, longHwid);

        assertThat(pushwoosh.getSubscriptionAccountId(), equalTo(appCode + ":" + longHwid));
    }

    // PushwooshStartWorker counts down its startup latch on ReverseProxyReadyEvent and then requires
    // a configured proxy, so announcing readiness after the URL was dropped blocks all traffic for good.
    @Test
    public void setReverseProxy_whenSdkNotInitialized_doesNotAnnounceReadiness() {
        Mockito.when(PushwooshPlatform.getInstance().getConfig().isReverseProxyAllowed())
                .thenReturn(true);
        NetworkModule.setRequestManager(null);
        List<ReverseProxyReadyEvent> events = new ArrayList<>();
        Subscription<ReverseProxyReadyEvent> subscription =
                EventBus.subscribe(ReverseProxyReadyEvent.class, events::add);

        Pushwoosh.getInstance().setReverseProxy("https://proxy.example.com/", null);
        ShadowLooper.idleMainLooper();
        subscription.unsubscribe();

        assertThat(events, hasSize(0));
    }

    @Test
    public void setReverseProxy_whenInitialized_announcesReadiness() {
        Mockito.when(PushwooshPlatform.getInstance().getConfig().isReverseProxyAllowed())
                .thenReturn(true);
        List<ReverseProxyReadyEvent> events = new ArrayList<>();
        Subscription<ReverseProxyReadyEvent> subscription =
                EventBus.subscribe(ReverseProxyReadyEvent.class, events::add);

        Pushwoosh.getInstance().setReverseProxy("https://proxy.example.com/", null);
        ShadowLooper.idleMainLooper();
        subscription.unsubscribe();

        assertThat(events, hasSize(1));
    }

    @Test
    public void setReverseProxy_whenNotAllowed_doesNotReachSeam() {
        Mockito.when(PushwooshPlatform.getInstance().getConfig().isReverseProxyAllowed())
                .thenReturn(false);
        List<ReverseProxyReadyEvent> events = new ArrayList<>();
        Subscription<ReverseProxyReadyEvent> subscription =
                EventBus.subscribe(ReverseProxyReadyEvent.class, events::add);

        Pushwoosh.getInstance().setReverseProxy("https://proxy.example.com/", null);
        ShadowLooper.idleMainLooper();
        subscription.unsubscribe();

        assertThat(platformTestManager.getRequestManager().reverseProxyCalls(), hasSize(0));
        assertThat(events, hasSize(0));
    }

    // "no URL given" is the facade's call: the seam reads null as "reset the proxy" and reports
    // success, which would announce readiness with no endpoint configured.
    @Test
    public void setReverseProxy_nullOrEmptyUrl_doesNotReachSeam() {
        Mockito.when(PushwooshPlatform.getInstance().getConfig().isReverseProxyAllowed())
                .thenReturn(true);
        List<ReverseProxyReadyEvent> events = new ArrayList<>();
        Subscription<ReverseProxyReadyEvent> subscription =
                EventBus.subscribe(ReverseProxyReadyEvent.class, events::add);

        Pushwoosh.getInstance().setReverseProxy(null, null);
        Pushwoosh.getInstance().setReverseProxy("", null);
        ShadowLooper.idleMainLooper();
        subscription.unsubscribe();

        assertThat(platformTestManager.getRequestManager().reverseProxyCalls(), hasSize(0));
        assertThat(events, hasSize(0));
    }

    // The facade used to reject this by shape: startsWith("https://") failed on the leading spaces.
    // Trimming is now the seam's verdict alone, so the facade must pass the URL through untouched.
    @Test
    public void setReverseProxy_urlWithSurroundingWhitespace_announcesReadiness() {
        Mockito.when(PushwooshPlatform.getInstance().getConfig().isReverseProxyAllowed())
                .thenReturn(true);
        List<ReverseProxyReadyEvent> events = new ArrayList<>();
        Subscription<ReverseProxyReadyEvent> subscription =
                EventBus.subscribe(ReverseProxyReadyEvent.class, events::add);

        Pushwoosh.getInstance().setReverseProxy("  https://proxy.example.com/  ", null);
        ShadowLooper.idleMainLooper();
        subscription.unsubscribe();

        assertThat(events, hasSize(1));
    }

    @Test
    public void setReverseProxy_urlRejectedBySeam_doesNotAnnounceReadiness() {
        Mockito.when(PushwooshPlatform.getInstance().getConfig().isReverseProxyAllowed())
                .thenReturn(true);
        platformTestManager.getRequestManager().setReverseProxyUrlReturns(false);
        List<ReverseProxyReadyEvent> events = new ArrayList<>();
        Subscription<ReverseProxyReadyEvent> subscription =
                EventBus.subscribe(ReverseProxyReadyEvent.class, events::add);

        Pushwoosh.getInstance().setReverseProxy("https://proxy.example.com/", null);
        ShadowLooper.idleMainLooper();
        subscription.unsubscribe();

        assertThat(events, hasSize(0));
    }

    // Verifies that getBaseUrl returns the effective endpoint derived from the application code at init.
    @Test
    public void getBaseUrl_afterInit_returnsUrlDerivedFromAppCode() {
        assertThat(
                Pushwoosh.getInstance().getBaseUrl(),
                equalTo("https://" + MockConfig.APP_ID + ".api.pushwoosh.com/json/1.3/"));
    }

    // Verifies that getBaseUrl reflects the custom URL from setAppId, normalized with a trailing slash.
    @Test
    public void getBaseUrl_afterSetAppIdWithCustomUrl_returnsNormalizedUrl() {
        Pushwoosh.getInstance().setAppId(MockConfig.APP_ID, "https://custom.example.com/json/1.3");

        assertThat(Pushwoosh.getInstance().getBaseUrl(), equalTo("https://custom.example.com/json/1.3/"));
    }

    // Verifies that getBaseUrl returns null, not an empty string, while the base URL is not computed yet.
    @Test
    public void getBaseUrl_whenBaseUrlNotComputed_returnsNull() {
        registrationPrefs.baseUrl().set("");

        assertThat(Pushwoosh.getInstance().getBaseUrl(), nullValue());
    }
}
