package com.pushwoosh.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.pushwoosh.RegisterForPushNotificationsResultData;
import com.pushwoosh.exception.RegisterForPushNotificationsException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.event.Event;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.EventListener;
import com.pushwoosh.notification.event.RegistrationErrorEvent;
import com.pushwoosh.notification.event.RegistrationSuccessEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class RegistrationCallbackHolderTest {

    private AutoCloseable mocks;

    @Mock
    private Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback;

    @Mock
    private Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback2;

    @Before
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        EventBus.clearSubscribersMap();
        resetStaticHolder();
    }

    @After
    public void tearDown() throws Exception {
        EventBus.clearSubscribersMap();
        resetStaticHolder();
        mocks.close();
    }

    private static void resetStaticHolder() throws Exception {
        Field f = RegistrationCallbackHolder.class.getDeclaredField("currentCallbackHolder");
        f.setAccessible(true);
        f.set(null, null);
    }

    private static Object readStaticHolder() throws Exception {
        Field f = RegistrationCallbackHolder.class.getDeclaredField("currentCallbackHolder");
        f.setAccessible(true);
        return f.get(null);
    }

    private static int subscribersCountFor(Class<? extends Event> eventClass) {
        Map<Class<? extends Event>, List<EventListener<?>>> map = EventBus.getSubscribersMap();
        List<EventListener<?>> list = map.get(eventClass);
        return list == null ? 0 : list.size();
    }

    // Verifies that setCallback with null callback and public flag does not subscribe or change state.
    @Test
    public void setCallback_nullCallbackPublicFlag_doesNothing() throws Exception {
        RegistrationCallbackHolder.setCallback(null, true);

        assertEquals(0, subscribersCountFor(RegistrationSuccessEvent.class));
        assertEquals(0, subscribersCountFor(RegistrationErrorEvent.class));
        assertNull(readStaticHolder());
    }

    // Verifies that setCallback with null callback and internal flag does not subscribe or change state.
    @Test
    public void setCallback_nullCallbackInternalFlag_doesNothing() throws Exception {
        RegistrationCallbackHolder.setCallback(null, false);

        assertEquals(0, subscribersCountFor(RegistrationSuccessEvent.class));
        assertEquals(0, subscribersCountFor(RegistrationErrorEvent.class));
        assertNull(readStaticHolder());
    }

    // Verifies that on RegistrationSuccessEvent the public-path callback receives success Result and the singleton
    // holder is cleared.
    @Test
    public void setCallback_publicPathSuccessEvent_callbackInvokedAndHolderCleared() throws Exception {
        RegisterForPushNotificationsResultData data = new RegisterForPushNotificationsResultData("token-123", true);

        RegistrationCallbackHolder.setCallback(callback, true);
        assertNotNull(readStaticHolder());

        EventBus.sendEvent(new RegistrationSuccessEvent(data));
        ShadowLooper.idleMainLooper();

        ArgumentCaptor<Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException>> captor =
                ArgumentCaptor.forClass(Result.class);
        verify(callback).process(captor.capture());

        Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> result =
                captor.getValue();
        assertTrue(result.isSuccess());
        assertEquals(data, result.getData());
        assertNull(readStaticHolder());
        assertEquals(0, subscribersCountFor(RegistrationSuccessEvent.class));
        assertEquals(0, subscribersCountFor(RegistrationErrorEvent.class));
    }

    // Verifies that on RegistrationErrorEvent the public-path callback receives failure Result wrapped in
    // RegisterForPushNotificationsException.
    @Test
    public void setCallback_publicPathErrorEvent_callbackInvokedWithExceptionAndHolderCleared() throws Exception {
        RegistrationCallbackHolder.setCallback(callback, true);
        assertNotNull(readStaticHolder());

        EventBus.sendEvent(new RegistrationErrorEvent("registration_failed"));
        ShadowLooper.idleMainLooper();

        ArgumentCaptor<Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException>> captor =
                ArgumentCaptor.forClass(Result.class);
        verify(callback).process(captor.capture());

        Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> result =
                captor.getValue();
        assertNull(result.getData());
        assertNotNull(result.getException());
        assertEquals("registration_failed", result.getException().getMessage());
        assertNull(readStaticHolder());
        assertEquals(0, subscribersCountFor(RegistrationSuccessEvent.class));
        assertEquals(0, subscribersCountFor(RegistrationErrorEvent.class));
    }

    // Verifies that a second public-path setCallback while holder is active is silently ignored and only the first
    // callback receives the event.
    @Test
    public void setCallback_secondPublicCallWhileActive_silentlyIgnoresSecond() throws Exception {
        RegistrationCallbackHolder.setCallback(callback, true);
        Object holderAfterFirst = readStaticHolder();
        assertNotNull(holderAfterFirst);

        RegistrationCallbackHolder.setCallback(callback2, true);
        assertEquals("holder must not be replaced", holderAfterFirst, readStaticHolder());

        RegisterForPushNotificationsResultData data = new RegisterForPushNotificationsResultData("token-x", true);
        EventBus.sendEvent(new RegistrationSuccessEvent(data));
        ShadowLooper.idleMainLooper();

        verify(callback, times(1)).process(any());
        verify(callback2, never()).process(any());
        assertNull(readStaticHolder());
    }

    // Verifies that internal-path setCallback allows multiple registrations and each receives the event independently.
    @Test
    public void setCallback_internalPathMultipleRegistrations_allCallbacksInvoked() throws Exception {
        RegistrationCallbackHolder.setCallback(callback, false);
        RegistrationCallbackHolder.setCallback(callback2, false);
        assertNull(readStaticHolder());

        RegisterForPushNotificationsResultData data = new RegisterForPushNotificationsResultData("token-y", false);
        EventBus.sendEvent(new RegistrationSuccessEvent(data));
        ShadowLooper.idleMainLooper();

        verify(callback, times(1)).process(any());
        verify(callback2, times(1)).process(any());
    }

    // Verifies that internal-path unsubscribe does not write to the static singleton holder.
    @Test
    public void setCallback_internalPath_doesNotTouchStaticHolder() throws Exception {
        RegistrationCallbackHolder.setCallback(callback, false);
        assertNull("internal path must not set holder", readStaticHolder());

        RegisterForPushNotificationsResultData data = new RegisterForPushNotificationsResultData("token-z", true);
        EventBus.sendEvent(new RegistrationSuccessEvent(data));
        ShadowLooper.idleMainLooper();

        verify(callback).process(any());
        assertNull(readStaticHolder());
        assertEquals(0, subscribersCountFor(RegistrationSuccessEvent.class));
        assertEquals(0, subscribersCountFor(RegistrationErrorEvent.class));
    }

    // Verifies that triggering success unsubscribes BOTH success and error listeners so a later error event does not
    // reach the callback.
    @Test
    public void setCallback_successEventThenErrorEvent_errorIgnoredAfterSuccess() throws Exception {
        RegistrationCallbackHolder.setCallback(callback, true);

        RegisterForPushNotificationsResultData data = new RegisterForPushNotificationsResultData("token-q", true);
        EventBus.sendEvent(new RegistrationSuccessEvent(data));
        ShadowLooper.idleMainLooper();

        EventBus.sendEvent(new RegistrationErrorEvent("late_error"));
        ShadowLooper.idleMainLooper();

        verify(callback, times(1)).process(any());
        assertEquals(0, subscribersCountFor(RegistrationSuccessEvent.class));
        assertEquals(0, subscribersCountFor(RegistrationErrorEvent.class));
    }

    // Verifies that a repeated event after the callback fired does not invoke the callback a second time.
    @Test
    public void setCallback_repeatedSuccessEvent_callbackInvokedOnlyOnce() throws Exception {
        RegistrationCallbackHolder.setCallback(callback, false);

        RegisterForPushNotificationsResultData data = new RegisterForPushNotificationsResultData("token-r", true);
        EventBus.sendEvent(new RegistrationSuccessEvent(data));
        ShadowLooper.idleMainLooper();

        EventBus.sendEvent(new RegistrationSuccessEvent(data));
        ShadowLooper.idleMainLooper();

        verify(callback, times(1)).process(any());
    }
}
