package com.pushwoosh;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.pushwoosh.function.Result;
import com.pushwoosh.internal.network.CachedRequest;
import com.pushwoosh.internal.network.ConnectionException;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.network.RequestStorage;
import com.pushwoosh.repository.RepositoryModule;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE)
public class SendCachedRequestWorkerTest {

    private static final long REQ_ID = 42L;
    private static final String REQ_KEY = "k42";

    @Mock
    private WorkerParameters workerParametersMock;

    @Mock
    private RequestStorage requestStorageMock;

    @Mock
    private RequestManager requestManagerMock;

    @Mock
    private CachedRequest cachedRequestMock;

    private Context context;
    private AutoCloseable mocks;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        context = spy(RuntimeEnvironment.application);
        when(context.getApplicationContext()).thenReturn(context);
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    private Data inputDataWithId(long id) {
        return new Data.Builder()
                .putLong(SendCachedRequestWorker.DATA_CACHED_REQUEST_ID, id)
                .build();
    }

    private Data emptyInputData() {
        return new Data.Builder().build();
    }

    private SendCachedRequestWorker workerWith(Data data, int attemptCount) {
        when(workerParametersMock.getInputData()).thenReturn(data);
        when(workerParametersMock.getRunAttemptCount()).thenReturn(attemptCount);
        return new SendCachedRequestWorker(context, workerParametersMock);
    }

    @Test
    public void doWork_validIdAndSendSucceeds_removesAndReturnsSuccess() {
        when(cachedRequestMock.getKey()).thenReturn(REQ_KEY);

        try (MockedStatic<RepositoryModule> repoMock = mockStatic(RepositoryModule.class);
                MockedStatic<NetworkModule> netMock = mockStatic(NetworkModule.class)) {
            repoMock.when(RepositoryModule::getRequestStorage).thenReturn(requestStorageMock);
            when(requestStorageMock.get(REQ_ID)).thenReturn(cachedRequestMock);
            netMock.when(NetworkModule::getRequestManager).thenReturn(requestManagerMock);
            when(requestManagerMock.sendRequestSync(cachedRequestMock)).thenReturn(Result.fromData(null));

            SendCachedRequestWorker worker = workerWith(inputDataWithId(REQ_ID), 0);

            ListenableWorker.Result result = worker.doWork();

            assertEquals(ListenableWorker.Result.success(), result);
            verify(requestManagerMock).sendRequestSync(cachedRequestMock);
            verify(requestStorageMock).remove(REQ_KEY);
        }
    }

    @Test
    public void doWork_sendThrowsConnectionException_returnsRetryAndKeepsRequest() {
        try (MockedStatic<RepositoryModule> repoMock = mockStatic(RepositoryModule.class);
                MockedStatic<NetworkModule> netMock = mockStatic(NetworkModule.class)) {
            repoMock.when(RepositoryModule::getRequestStorage).thenReturn(requestStorageMock);
            when(requestStorageMock.get(REQ_ID)).thenReturn(cachedRequestMock);
            netMock.when(NetworkModule::getRequestManager).thenReturn(requestManagerMock);
            when(requestManagerMock.sendRequestSync(cachedRequestMock))
                    .thenReturn(Result.fromException(new ConnectionException("no net", 0, 0)));

            SendCachedRequestWorker worker = workerWith(inputDataWithId(REQ_ID), 0);

            ListenableWorker.Result result = worker.doWork();

            assertEquals(ListenableWorker.Result.retry(), result);
            verify(requestStorageMock, never()).remove(anyString());
        }
    }

    @Test
    public void doWork_sendFailsWithNonConnectionException_dropsAndReturnsSuccess() {
        when(cachedRequestMock.getKey()).thenReturn(REQ_KEY);

        try (MockedStatic<RepositoryModule> repoMock = mockStatic(RepositoryModule.class);
                MockedStatic<NetworkModule> netMock = mockStatic(NetworkModule.class)) {
            repoMock.when(RepositoryModule::getRequestStorage).thenReturn(requestStorageMock);
            when(requestStorageMock.get(REQ_ID)).thenReturn(cachedRequestMock);
            netMock.when(NetworkModule::getRequestManager).thenReturn(requestManagerMock);
            when(requestManagerMock.sendRequestSync(cachedRequestMock))
                    .thenReturn(Result.fromException(new NetworkException("bad payload")));

            SendCachedRequestWorker worker = workerWith(inputDataWithId(REQ_ID), 0);

            ListenableWorker.Result result = worker.doWork();

            assertEquals(ListenableWorker.Result.success(), result);
            verify(requestStorageMock).remove(REQ_KEY);
        }
    }

    @Test
    public void onFail_runAttemptCountAtOrAboveLimit_returnsSuccess() {
        List<Integer> exhaustedAttempts = Arrays.asList(3, 4, 5);
        for (Integer attempt : exhaustedAttempts) {
            try (MockedStatic<RepositoryModule> repoMock = mockStatic(RepositoryModule.class);
                    MockedStatic<NetworkModule> netMock = mockStatic(NetworkModule.class)) {

                SendCachedRequestWorker worker = workerWith(emptyInputData(), attempt);

                ListenableWorker.Result result = worker.doWork();

                assertEquals("attempt=" + attempt, ListenableWorker.Result.success(), result);
                repoMock.verifyNoInteractions();
                netMock.verifyNoInteractions();
            }
        }
    }

    @Test
    public void onFail_runAttemptCountBelowLimit_returnsRetry() {
        List<Integer> attempts = Arrays.asList(0, 1, 2);
        for (Integer attempt : attempts) {
            try (MockedStatic<RepositoryModule> repoMock = mockStatic(RepositoryModule.class);
                    MockedStatic<NetworkModule> netMock = mockStatic(NetworkModule.class)) {

                SendCachedRequestWorker worker = workerWith(emptyInputData(), attempt);

                ListenableWorker.Result result = worker.doWork();

                assertEquals("attempt=" + attempt, ListenableWorker.Result.retry(), result);
                repoMock.verifyNoInteractions();
                netMock.verifyNoInteractions();
            }
        }
    }
}
