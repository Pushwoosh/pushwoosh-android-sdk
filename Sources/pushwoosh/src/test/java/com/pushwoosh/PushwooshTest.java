package com.pushwoosh;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Configuration;
import androidx.work.ListenableWorker;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.TestDriver;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.pushwoosh.exception.RegisterForPushNotificationsException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.ReverseProxyReadyEvent;
import com.pushwoosh.internal.event.Subscription;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.registrar.ExistingTokenRegistrarWorker;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.testutil.CallbackWrapper;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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

    private final WorkerFactory testWorkerFactory = new WorkerFactory() {
        @Nullable @Override
        public ListenableWorker createWorker(
                @NonNull Context appContext, @NonNull String workerClassName, @NonNull WorkerParameters params) {
            if (workerClassName.equals(ExistingTokenRegistrarWorker.class.getName())) {
                // Modify ctor to accept deps, or use a Service Locator inside worker.
                return new ExistingTokenRegistrarWorker(appContext, params);
            }
            return null;
        }
    };

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
                .setWorkerFactory(testWorkerFactory)
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

    @Test
    public void shouldSendExistingTokenOnce() throws ExecutionException, InterruptedException {
        String testToken = "12345";
        Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback =
                CallbackWrapper.spy();

        Pushwoosh.getInstance().registerExistingToken(testToken, callback);

        List<WorkInfo> infos = workManager
                .getWorkInfosForUniqueWork(ExistingTokenRegistrarWorker.TAG)
                .get();
        assertThat(infos, hasSize(1));
        WorkInfo info = infos.get(0);
        testDriver.setAllConstraintsMet(info.getId());

        Pushwoosh.getInstance().registerExistingToken(testToken, callback);

        infos = workManager
                .getWorkInfosForUniqueWork(ExistingTokenRegistrarWorker.TAG)
                .get();
        assertThat(infos, hasSize(1));
        info = infos.get(0);
        testDriver.setAllConstraintsMet(info.getId());

        Mockito.verify(callback, Mockito.times(2)).process(Mockito.any());
        Mockito.verify(notificationManagerSpy, Mockito.times(1)).onExistingTokenReceived(Mockito.any(), Mockito.any());
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
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
