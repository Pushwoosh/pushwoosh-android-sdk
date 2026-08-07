package com.pushwoosh.internal.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import com.pushwoosh.function.Result;
import com.pushwoosh.repository.RegistrationPrefs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.LEGACY)
public class NotInitializedRequestManagerTest {

    private static class TestRequest extends PushRequest<Void> {
        @Override
        public String getMethod() {
            return "testMethod";
        }
    }

    private final TestRequest request = new TestRequest();

    @Before
    public void setUp() {
        NetworkModule.setRequestManager(null);
    }

    @After
    public void tearDown() {
        NetworkModule.setRequestManager(null);
    }

    // Verifies that the getter never returns null before NetworkModule.init() has run.
    @Test
    public void getRequestManager_notInitialized_returnsNonNullStub() {
        assertNotNull(NetworkModule.getRequestManager());
    }

    // Verifies that a dropped request fails with a bare NetworkException, never a retriable ConnectionException.
    @Test
    public void sendRequestSync_notInitialized_returnsTerminalNetworkException() {
        Result<Void, NetworkException> result =
                NetworkModule.getRequestManager().sendRequestSync(request);

        assertFalse(result.isSuccess());
        assertEquals("SDK is not initialized", result.getException().getMessage());
        assertFalse(result.getException() instanceof ConnectionException);
    }

    // Verifies that the error reaches the callback synchronously on the calling thread, as the removed guards did.
    @Test
    public void sendRequest_notInitialized_deliversErrorSynchronouslyOnCallerThread() {
        List<Result<Void, NetworkException>> delivered = new ArrayList<>();
        List<Thread> callbackThread = new ArrayList<>();
        Thread callerThread = Thread.currentThread();

        NetworkModule.getRequestManager().sendRequest(request, result -> {
            delivered.add(result);
            callbackThread.add(Thread.currentThread());
        });

        assertEquals(1, delivered.size());
        assertFalse(delivered.get(0).isSuccess());
        assertEquals("SDK is not initialized", delivered.get(0).getException().getMessage());
        assertSame(callerThread, callbackThread.get(0));
    }

    // Verifies that the base-url overload drops the request the same way.
    @Test
    public void sendRequestWithBaseUrl_notInitialized_deliversError() {
        List<Result<Void, NetworkException>> delivered = new ArrayList<>();

        NetworkModule.getRequestManager().sendRequest(request, "https://example.com/", delivered::add);

        assertEquals(1, delivered.size());
        assertFalse(delivered.get(0).isSuccess());
    }

    // Verifies that fire-and-forget senders survive a dropped request.
    @Test
    public void sendRequest_notInitialized_withoutCallbackDoesNotThrow() {
        NetworkModule.getRequestManager().sendRequest(request);
    }

    // Verifies that an explicit null callback is tolerated instead of dereferenced.
    @Test
    public void sendRequest_notInitialized_withNullCallbackDoesNotThrow() {
        NetworkModule.getRequestManager().sendRequest(request, "https://example.com/", null);
    }

    // Verifies that updateBaseUrl reports failure, which set_base_url turns into a false command result.
    @Test
    public void updateBaseUrl_notInitialized_returnsFalse() {
        assertFalse(NetworkModule.getRequestManager().updateBaseUrl("https://example.com/"));
    }

    // Verifies that setReverseProxyUrl reports failure, which keeps Pushwoosh.setReverseProxy() from
    // announcing a readiness the dropped URL cannot back.
    @Test
    public void setReverseProxyUrl_notInitialized_returnsFalse() {
        assertFalse(NetworkModule.getRequestManager().setReverseProxyUrl("https://proxy.example.com/", null));
    }

    // Verifies that a throwing callback is contained, as PushwooshRequestManager.safeProcessCallback does.
    @Test
    public void sendRequest_notInitialized_containsThrowingCallback() {
        List<Result<Void, NetworkException>> delivered = new ArrayList<>();

        NetworkModule.getRequestManager().sendRequest(request, result -> {
            delivered.add(result);
            throw new IllegalStateException("callback boom");
        });

        assertEquals(1, delivered.size());
    }

    // Verifies that the stub is only a fallback: once a real manager is installed, the getter hands it out.
    @Test
    public void getRequestManager_afterSetRequestManager_returnsRealManager() {
        RequestManager real = mock(RequestManager.class);

        NetworkModule.setRequestManager(real);

        assertSame(real, NetworkModule.getRequestManager());
    }

    // The field now holds the stub instead of null, so init()'s idempotence guard tests for the stub.
    // A second init() (a platform rebuilt in the same process) must not swap the manager under live callers.
    @Test
    public void init_calledTwice_keepsFirstManager() {
        RequestManager stub = NetworkModule.getRequestManager();

        NetworkModule.init(mock(RegistrationPrefs.class), mock(ServerCommunicationManager.class), false);
        RequestManager first = NetworkModule.getRequestManager();
        assertNotSame(stub, first);

        NetworkModule.init(mock(RegistrationPrefs.class), mock(ServerCommunicationManager.class), false);

        assertSame(first, NetworkModule.getRequestManager());
    }

    // A manager installed by a test must survive a later init(), as it did when the guard tested for null.
    @Test
    public void init_afterSetRequestManager_doesNotOverwrite() {
        RequestManager installed = mock(RequestManager.class);
        NetworkModule.setRequestManager(installed);

        NetworkModule.init(mock(RegistrationPrefs.class), mock(ServerCommunicationManager.class), false);

        assertSame(installed, NetworkModule.getRequestManager());
    }

    // Verifies that setRequestManager(null) still works as a reset, now via normalization to the stub.
    @Test
    public void getRequestManager_afterResetToNull_returnsStubAgain() {
        NetworkModule.setRequestManager(mock(RequestManager.class));
        NetworkModule.setRequestManager(null);

        Result<Void, NetworkException> result =
                NetworkModule.getRequestManager().sendRequestSync(request);

        assertFalse(result.isSuccess());
        assertEquals("SDK is not initialized", result.getException().getMessage());
    }
}
