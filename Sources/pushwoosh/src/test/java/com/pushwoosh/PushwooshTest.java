package com.pushwoosh;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

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
        @Nullable
        @Override
        public ListenableWorker createWorker(
                @NonNull Context appContext,
                @NonNull String workerClassName,
                @NonNull WorkerParameters params
        ) {
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
        Field f = PushwooshPlatform.class.getDeclaredField("notificationManager");
        f.setAccessible(true);
        f.set(PushwooshPlatform.getInstance(), notificationManagerSpy);

        // Pushwoosh singleton caches registration prefs when first created, PlatformTestManager resets
        // prefs after every test is ran. Need to manually reset prefs in singleton
        Field reg = Pushwoosh.class.getDeclaredField("registrationPrefs");
        reg.setAccessible(true);
        reg.set(Pushwoosh.getInstance(), platformTestManager.getRegistrationPrefs());

        sendPushStat = Pushwoosh.class.getDeclaredMethod("sendPushStat", Bundle.class);
        sendMessageDelivery = Pushwoosh.class.getDeclaredMethod("sendMessageDelivery", Bundle.class);
        sendPushStat.setAccessible(true);
        sendMessageDelivery.setAccessible(true);

        // Mock PushwooshMessagingServiceHelper static methods
        messagingServiceHelperMock = Mockito.mockStatic(PushwooshMessagingServiceHelper.class);
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

            List<WorkInfo> infos = workManager.getWorkInfosForUniqueWork(ExistingTokenRegistrarWorker.TAG).get();
            assertThat(infos, hasSize(1));
            WorkInfo info = infos.get(0);
            testDriver.setAllConstraintsMet(info.getId());

            Pushwoosh.getInstance().registerExistingToken(testToken, callback);

            infos = workManager.getWorkInfosForUniqueWork(ExistingTokenRegistrarWorker.TAG).get();
            assertThat(infos, hasSize(1));
            info = infos.get(0);
            testDriver.setAllConstraintsMet(info.getId());

            Mockito.verify(callback, Mockito.times(2)).process(Mockito.any());
            Mockito.verify(notificationManagerSpy, Mockito.times(1))
                    .onExistingTokenReceived(Mockito.any(), Mockito.any());
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
    }
}
