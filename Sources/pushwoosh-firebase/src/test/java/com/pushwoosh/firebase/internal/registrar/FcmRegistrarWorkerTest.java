package com.pushwoosh.firebase.internal.registrar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.pushwoosh.firebase.internal.utils.FirebaseTokenHelper;
import com.pushwoosh.internal.preference.PreferenceStringValue;
import com.pushwoosh.internal.utils.NotificationRegistrarHelper;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.concurrent.ExecutionException;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class FcmRegistrarWorkerTest {
    private static final String TOKEN = "fcm-token-abc";
    private static final String TAGS_JSON = "{\"test_tag\":\"test_value\"}";

    @Mock
    private WorkerParameters workerParameters;

    @Mock
    private RegistrationPrefs registrationPrefs;

    @Mock
    private PreferenceStringValue savedPushToken;

    private AutoCloseable mocks;
    private FcmRegistrarWorker worker;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        when(savedPushToken.get()).thenReturn("");
        when(registrationPrefs.pushToken()).thenReturn(savedPushToken);
        setInputData(null);

        Context context = spy(RuntimeEnvironment.application);
        when(context.getApplicationContext()).thenReturn(context);
        worker = new FcmRegistrarWorker(context, workerParameters);
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    private void setInputData(String tagsJson) {
        Data input = new Data.Builder()
                .putBoolean(FcmRegistrarWorker.DATA_REGISTER, true)
                .putString(FcmRegistrarWorker.DATA_TAGS, tagsJson)
                .build();
        when(workerParameters.getInputData()).thenReturn(input);
    }

    private MockedStatic<RepositoryModule> mockRepository() {
        MockedStatic<RepositoryModule> repository = Mockito.mockStatic(RepositoryModule.class);
        repository.when(RepositoryModule::getRegistrationPreferences).thenReturn(registrationPrefs);
        return repository;
    }

    @Test
    public void register_tokenFetched_forwardsTokenWithTags() {
        setInputData(TAGS_JSON);
        try (MockedStatic<RepositoryModule> repository = mockRepository();
                MockedStatic<FirebaseTokenHelper> tokenHelper = Mockito.mockStatic(FirebaseTokenHelper.class);
                MockedStatic<NotificationRegistrarHelper> registrar =
                        Mockito.mockStatic(NotificationRegistrarHelper.class)) {
            tokenHelper.when(FirebaseTokenHelper::getFirebaseToken).thenReturn(TOKEN);

            ListenableWorker.Result result = worker.doWork();

            assertEquals(ListenableWorker.Result.success(), result);
            registrar.verify(() -> NotificationRegistrarHelper.onRegisteredForRemoteNotifications(TOKEN, TAGS_JSON));
            registrar.verify(
                    () -> NotificationRegistrarHelper.onFailedToRegisterForRemoteNotifications(any()), never());
        }
    }

    @Test
    public void register_nullToken_reportsFailure() {
        try (MockedStatic<RepositoryModule> repository = mockRepository();
                MockedStatic<FirebaseTokenHelper> tokenHelper = Mockito.mockStatic(FirebaseTokenHelper.class);
                MockedStatic<NotificationRegistrarHelper> registrar =
                        Mockito.mockStatic(NotificationRegistrarHelper.class)) {
            tokenHelper.when(FirebaseTokenHelper::getFirebaseToken).thenReturn(null);

            worker.doWork();

            registrar.verify(() -> NotificationRegistrarHelper.onFailedToRegisterForRemoteNotifications(anyString()));
            registrar.verify(
                    () -> NotificationRegistrarHelper.onRegisteredForRemoteNotifications(any(), any()), never());
        }
    }

    @Test
    public void register_tokenFetchThrows_reportsFailureWithMessage() {
        try (MockedStatic<RepositoryModule> repository = mockRepository();
                MockedStatic<FirebaseTokenHelper> tokenHelper = Mockito.mockStatic(FirebaseTokenHelper.class);
                MockedStatic<NotificationRegistrarHelper> registrar =
                        Mockito.mockStatic(NotificationRegistrarHelper.class)) {
            tokenHelper
                    .when(FirebaseTokenHelper::getFirebaseToken)
                    .thenThrow(new ExecutionException(new Throwable("SERVICE_NOT_AVAILABLE")));

            worker.doWork();

            ArgumentCaptor<String> error = ArgumentCaptor.forClass(String.class);
            registrar.verify(
                    () -> NotificationRegistrarHelper.onFailedToRegisterForRemoteNotifications(error.capture()));
            assertTrue(error.getValue().contains("SERVICE_NOT_AVAILABLE"));
            registrar.verify(
                    () -> NotificationRegistrarHelper.onRegisteredForRemoteNotifications(any(), any()), never());
        }
    }

    @Test
    public void register_firebaseNotConfigured_reportsFailureWithMessage() {
        try (MockedStatic<RepositoryModule> repository = mockRepository();
                MockedStatic<FirebaseTokenHelper> tokenHelper = Mockito.mockStatic(FirebaseTokenHelper.class);
                MockedStatic<NotificationRegistrarHelper> registrar =
                        Mockito.mockStatic(NotificationRegistrarHelper.class)) {
            tokenHelper
                    .when(FirebaseTokenHelper::getFirebaseToken)
                    .thenThrow(new IllegalStateException("Default FirebaseApp is not initialized"));

            worker.doWork();

            ArgumentCaptor<String> error = ArgumentCaptor.forClass(String.class);
            registrar.verify(
                    () -> NotificationRegistrarHelper.onFailedToRegisterForRemoteNotifications(error.capture()));
            assertFalse(error.getValue().isEmpty());
        }
    }
}
