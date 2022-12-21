package com.pushwoosh;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;

import com.pushwoosh.appevents.PushwooshDefaultEvents;
import com.pushwoosh.inapp.PushwooshInAppImpl;
import com.pushwoosh.inapp.event.RichMediaCloseEvent;
import com.pushwoosh.internal.Plugin;
import com.pushwoosh.internal.PushRegistrarHelper;
import com.pushwoosh.internal.event.ConfigLoadedEvent;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.network.ServerCommunicationManager;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.AppVersionProvider;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.repository.DeviceRegistrar;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.config.GetConfigRequest;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class PushwooshStartWorkerTest {
    private static final String TEST_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCjBrqGZf/eMiLBmvhQ5c7CtPAvwmey5jDz0RJqJcwchxp3oPSxjmrfh5vT4o7uFf1NIJG3m+vVfOcNqBniUSI6CTAfRd/8aoT5dqFszEShAVGxchSsP94kmiQh/Fm16tSrVw6g9EMemjbWLnotbNK8DzWyHahVz1LFiS2DUze1QIDAQAB";
    private PushwooshStartWorker pushwooshStartWorker;

    @Mock
    private Config config;

    private RegistrationPrefs registrationPrefs;
    @Mock
    private AppVersionProvider appVersionProvider;
    @Mock
    private PushwooshRepository pushwooshRepository;
    @Mock
    private PushwooshNotificationManager notificationManager;
    @Mock
    private PushwooshInAppImpl pushwooshInApp;
    @Mock
    private DeviceRegistrar deviceRegistrar;
    @Mock
    private PushwooshDefaultEvents pushwooshDefaultEvents;
    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;

    private PlatformTestManager platformTestManager;

    private Intent launchIntent;

    private List<Plugin> pluginList = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        injectLaunchIntent();

        platformTestManager = new PlatformTestManager();

        registrationPrefs = Mockito.spy(platformTestManager.getRegistrationPrefs());
        PushRegistrarHelper pushRegistrarHelper = new PushRegistrarHelper(config.getPluginProvider(), notificationManager);
        ServerCommunicationManager serverCommunicationManager = mock(ServerCommunicationManager.class);
        when(serverCommunicationManager.isServerCommunicationAllowed()).thenReturn(true);
        pushwooshStartWorker = new PushwooshStartWorker(
                config,
                registrationPrefs,
                appVersionProvider,
                pushwooshRepository,
                notificationManager,
                pushwooshInApp,
                deviceRegistrar,
                pushwooshDefaultEvents,
                pushRegistrarHelper,
                serverCommunicationManager);

        AndroidPlatformModule.getApplicationOpenDetector().onApplicationCreated(false);


        pluginList.add(mock(Plugin.class));
        pluginList.add(mock(Plugin.class));
        pluginList.add(mock(Plugin.class));
        when(config.getPlugins()).thenReturn(pluginList);
        when(pushwooshRepository.getPublicKey()).thenReturn(TEST_PUBLIC_KEY);
    }

    private void injectLaunchIntent() {
        launchIntent = new Intent(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launchIntent.setPackage("com.pushwoosh");

        ResolveInfo resolveInfo = new ResolveInfo();
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = "com.pushwoosh";
        activityInfo.name = "android.app.Activity";
        resolveInfo.activityInfo = activityInfo;

        Context context = RuntimeEnvironment.application;
        ShadowPackageManager packageManager = shadowOf(context.getPackageManager());
        packageManager.addResolveInfoForIntent(launchIntent, resolveInfo);
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();

    }

    class LooperThread extends Thread {
        public Handler mHandler;
        private Runnable runnable;

        public LooperThread(Runnable runnable) {
            super();
            this.runnable =runnable;
        }

        public void run() {
            Looper.prepare();
            mHandler = new Handler();
            mHandler.post(runnable);
            Looper.loop();
        }
    }

    @Test(timeout = 20000)
    @Ignore
    public void sameTime() throws InterruptedException {
        pushwooshStartWorker.onApplicationCreated();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        CountDownLatch countDownLatchStart = new CountDownLatch(3);
        CountDownLatch countDownLatchFinish = new CountDownLatch(3);

        ExecutorService executorService = Executors.newFixedThreadPool(3, LooperThread::new);
        executorService.submit(() -> {
            countDownLatchStart.countDown();
            await(countDownLatch);
            Pushwoosh.getInstance().setAppId("123");
            countDownLatchFinish.countDown();
        });
        executorService.submit(() -> {
            countDownLatchStart.countDown();
            await(countDownLatch);
            openActivity();
            countDownLatchFinish.countDown();
        });
        executorService.submit(() -> {
            countDownLatchStart.countDown();
            await(countDownLatch);
            countDownLatchFinish.countDown();
        });

        await(countDownLatchStart);
        countDownLatch.countDown();
        PWLog.debug("countDown");
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        PWLog.debug("assert");
        await(countDownLatchFinish);
        assertNormalStart();
        PWLog.debug("assert maked");
        executorService.shutdownNow();
    }

    private void await(CountDownLatch countDownLatch) {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void setAppIdOpenActivityGetHwid() throws InterruptedException {
        pushwooshStartWorker.onApplicationCreated();

        PushwooshPlatform.getInstance().notificationManager().setAppId("123");
        openActivity();

        EventBus.sendEvent(new ConfigLoadedEvent());
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertNormalStart();
    }

    @Test
    public void setAppIdGetHwidOpenActivity() throws InterruptedException {
        pushwooshStartWorker.onApplicationCreated();

        PushwooshPlatform.getInstance().notificationManager().setAppId("123");
        EventBus.sendEvent(new ConfigLoadedEvent());
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        openActivity();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertNormalStart();
    }

    @Test
    public void openActivitySetAppIdGetHwid() throws InterruptedException {
        pushwooshStartWorker.onApplicationCreated();

        openActivity();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        PushwooshPlatform.getInstance().notificationManager().setAppId("123");
        EventBus.sendEvent(new ConfigLoadedEvent());
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertNormalStart();
    }

    @Test
    public void openActivityGetHwidSetAppId() throws InterruptedException {
        pushwooshStartWorker.onApplicationCreated();

        openActivity();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        PushwooshPlatform.getInstance().notificationManager().setAppId("123");
        EventBus.sendEvent(new ConfigLoadedEvent());
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertNormalStart();
    }

    @Test
    public void getHwidSetAppIdOpenActivity() throws InterruptedException {
        pushwooshStartWorker.onApplicationCreated();

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        PushwooshPlatform.getInstance().notificationManager().setAppId("123");
        EventBus.sendEvent(new ConfigLoadedEvent());
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        openActivity();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertNormalStart();
    }

    @Test
    public void getHwidOpenActivitySetAppId() throws InterruptedException {
        pushwooshStartWorker.onApplicationCreated();

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        openActivity();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        PushwooshPlatform.getInstance().notificationManager().setAppId("123");
        EventBus.sendEvent(new ConfigLoadedEvent());
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertNormalStart();
    }

    private void assertNormalStart() {
        //String hwid = Pushwoosh.getInstance().getHwid();
        //Assert.assertFalse(hwid.isEmpty());
        // hwid initialisation is asynchronous from 5.14.1

        InOrder inOrder = Mockito.inOrder(appVersionProvider, pushwooshRepository);
        inOrder.verify(appVersionProvider).handleLaunch();
        inOrder.verify(pushwooshRepository).sendAppOpen();

        verify(pushwooshInApp).setUserId(anyString());
        verify(pushwooshInApp).checkForUpdates();
        verify(notificationManager).initialize();

        verify(pluginList.get(0)).init();
        verify(pluginList.get(1)).init();
        verify(pluginList.get(2)).init();
    }

    private void openActivity() {
        injectLaunchIntent();
        Robolectric.buildActivity(Activity.class, launchIntent).create().start().resume().visible().get();
    }

    @Test
    public void reset() {
        AtomicBoolean started = (AtomicBoolean) Whitebox.getInternalState(pushwooshStartWorker, "started");
        started.set(true);

        pushwooshStartWorker.reset();

        Assert.assertFalse(started.get());
    }
}