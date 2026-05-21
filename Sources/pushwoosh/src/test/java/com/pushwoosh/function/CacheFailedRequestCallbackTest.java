package com.pushwoosh.function;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;

import com.pushwoosh.PushwooshWorkManagerHelper;
import com.pushwoosh.SendCachedRequestWorker;
import com.pushwoosh.internal.network.ConnectionException;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.PushRequest;
import com.pushwoosh.internal.network.RequestStorage;
import com.pushwoosh.repository.RepositoryModule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class CacheFailedRequestCallbackTest {

    @Mock
    private RequestStorage requestStorage;

    @Mock
    private PushRequest<String> request;

    @Mock
    private Callback<String, NetworkException> delegate;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        ShadowLog.stream = System.out;
    }

    @Test
    public void needToRetryTest() {
        CacheFailedRequestCallback callback = new CacheFailedRequestCallback(null, null);

        // pushwoosh status 200
        ConnectionException exception = new ConnectionException(null, 200, 200);
        Assert.assertFalse(callback.needToRetry(exception));

        // pushwoosh status 300
        exception = new ConnectionException(null, 200, 300);
        Assert.assertFalse(callback.needToRetry(exception));

        // pushwoosh status 400
        exception = new ConnectionException(null, 200, 400);
        Assert.assertFalse(callback.needToRetry(exception));

        // pushwoosh status 429
        exception = new ConnectionException(null, 200, 429);
        Assert.assertFalse(callback.needToRetry(exception));

        // pushwoosh status 500
        exception = new ConnectionException(null, 200, 500);
        Assert.assertFalse(callback.needToRetry(exception));

        // pushwoosh status 600
        exception = new ConnectionException(null, 200, 600);
        Assert.assertFalse(callback.needToRetry(exception));

        // pushwoosh status 0
        exception = new ConnectionException(null, 200, 0);
        Assert.assertFalse(callback.needToRetry(exception));

        // pushwoosh status 0 network status 0 (no internet)
        exception = new ConnectionException(null, 0, 0);
        Assert.assertTrue(callback.needToRetry(exception));

        // pushwoosh status -100
        exception = new ConnectionException(null, 200, -100);
        Assert.assertFalse(callback.needToRetry(exception));
    }

    // ---------- Group A: process(Result) ----------

    @Test
    public void processForwardsSuccessResultToDelegateAndSkipsWorker() {
        CacheFailedRequestCallback<String> callback =
                new CacheFailedRequestCallback<>(delegate, request, requestStorage);
        Result<String, NetworkException> result = Result.fromData("ok");

        try (MockedStatic<RepositoryModule> repoMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<PushwooshWorkManagerHelper> wmMock =
                        Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            callback.process(result);

            verify(delegate).process(result);
            repoMock.verifyNoInteractions();
            wmMock.verifyNoInteractions();
        }
    }

    @Test
    public void processForwardsNetworkExceptionFailureAndSkipsWorker() {
        CacheFailedRequestCallback<String> callback =
                new CacheFailedRequestCallback<>(delegate, request, requestStorage);
        Result<String, NetworkException> result = Result.fromException(new NetworkException("boom"));

        try (MockedStatic<RepositoryModule> repoMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<PushwooshWorkManagerHelper> wmMock =
                        Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            callback.process(result);

            verify(delegate).process(result);
            repoMock.verifyNoInteractions();
            wmMock.verifyNoInteractions();
        }
    }

    @Test
    public void processForwardsHttpErrorAndSkipsWorker_whenServerReturnsRealStatus() {
        CacheFailedRequestCallback<String> callback =
                new CacheFailedRequestCallback<>(delegate, request, requestStorage);
        Result<String, NetworkException> result = Result.fromException(new ConnectionException("server", 500, 200));

        try (MockedStatic<RepositoryModule> repoMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<PushwooshWorkManagerHelper> wmMock =
                        Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            callback.process(result);

            verify(delegate).process(result);
            repoMock.verifyNoInteractions();
            wmMock.verifyNoInteractions();
        }
    }

    @Test
    public void processSchedulesWorkerAndForwardsResult_whenConnectionErrorAndZeroStatuses() {
        CacheFailedRequestCallback<String> callback =
                new CacheFailedRequestCallback<>(delegate, request, requestStorage);
        Result<String, NetworkException> result = Result.fromException(new ConnectionException("offline", 0, 0));

        RequestStorage repoStorage = mock(RequestStorage.class);
        when(repoStorage.add(request)).thenReturn(42L);

        try (MockedStatic<RepositoryModule> repoMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<PushwooshWorkManagerHelper> wmMock =
                        Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {
            repoMock.when(RepositoryModule::getRequestStorage).thenReturn(repoStorage);
            stubConstraints(wmMock);

            callback.process(result);

            verify(repoStorage).add(request);
            wmMock.verify(
                    () -> PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(
                            any(OneTimeWorkRequest.class),
                            eq(SendCachedRequestWorker.TAG),
                            eq(ExistingWorkPolicy.APPEND)),
                    times(1));
            verify(delegate).process(result);
        }
    }

    @Test
    public void processSchedulesWorker_whenConnectionErrorAndNullDelegate() {
        CacheFailedRequestCallback<String> callback = new CacheFailedRequestCallback<>(request, requestStorage);
        Result<String, NetworkException> result = Result.fromException(new ConnectionException("offline", 0, 0));

        RequestStorage repoStorage = mock(RequestStorage.class);
        when(repoStorage.add(request)).thenReturn(7L);

        try (MockedStatic<RepositoryModule> repoMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<PushwooshWorkManagerHelper> wmMock =
                        Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {
            repoMock.when(RepositoryModule::getRequestStorage).thenReturn(repoStorage);
            stubConstraints(wmMock);

            callback.process(result);

            wmMock.verify(
                    () -> PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(
                            any(OneTimeWorkRequest.class),
                            eq(SendCachedRequestWorker.TAG),
                            eq(ExistingWorkPolicy.APPEND)),
                    times(1));
        }
    }

    // ---------- Group B: scheduleSendCachedRequestWorker(PushRequest) ----------

    @Test
    public void scheduleSendCachedRequestWorkerEnqueuesWorkWithRowIdPayload() {
        RequestStorage repoStorage = mock(RequestStorage.class);
        when(repoStorage.add(request)).thenReturn(123L);

        try (MockedStatic<RepositoryModule> repoMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<PushwooshWorkManagerHelper> wmMock =
                        Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {
            repoMock.when(RepositoryModule::getRequestStorage).thenReturn(repoStorage);
            stubConstraints(wmMock);

            CacheFailedRequestCallback.scheduleSendCachedRequestWorker(request);

            ArgumentCaptor<OneTimeWorkRequest> captor = ArgumentCaptor.forClass(OneTimeWorkRequest.class);
            wmMock.verify(() -> PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(
                    captor.capture(), eq(SendCachedRequestWorker.TAG), eq(ExistingWorkPolicy.APPEND)));
            long capturedId =
                    captor.getValue().getWorkSpec().input.getLong(SendCachedRequestWorker.DATA_CACHED_REQUEST_ID, -1);
            assertEquals(123L, capturedId);
        }
    }

    @Test
    public void scheduleSendCachedRequestWorkerSkipsEnqueue_whenStorageReturnsNegativeOne() {
        RequestStorage repoStorage = mock(RequestStorage.class);
        when(repoStorage.add(request)).thenReturn(-1L);

        try (MockedStatic<RepositoryModule> repoMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<PushwooshWorkManagerHelper> wmMock =
                        Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {
            repoMock.when(RepositoryModule::getRequestStorage).thenReturn(repoStorage);

            CacheFailedRequestCallback.scheduleSendCachedRequestWorker(request);

            wmMock.verify(
                    () -> PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(
                            any(OneTimeWorkRequest.class), any(String.class), any(ExistingWorkPolicy.class)),
                    never());
        }
    }

    private static void stubConstraints(MockedStatic<PushwooshWorkManagerHelper> wmMock) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        wmMock.when(PushwooshWorkManagerHelper::getNetworkAvailableConstraints).thenReturn(constraints);
    }
}
