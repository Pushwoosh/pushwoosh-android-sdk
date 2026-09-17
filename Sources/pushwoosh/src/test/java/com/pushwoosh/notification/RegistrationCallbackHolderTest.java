package com.pushwoosh.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    }

    @After
    public void tearDown() throws Exception {
        EventBus.clearSubscribersMap();
        mocks.close();
    }

    private static int subscribersCountFor(Class<? extends Event> eventClass) {
        Map<Class<? extends Event>, List<EventListener<?>>> map = EventBus.getSubscribersMap();
        List<EventListener<?>> list = map.get(eventClass);
        return list == null ? 0 : list.size();
    }

    // Verifies that setCallback with null callback does not subscribe.
    @Test
    public void setCallback_nullCallback_doesNothing() {
        RegistrationCallbackHolder.setCallback(null);

        assertEquals(0, subscribersCountFor(RegistrationSuccessEvent.class));
        assertEquals(0, subscribersCountFor(RegistrationErrorEvent.class));
    }

    // Verifies that on success the callback receives success Result and both subscriptions are removed.
    @Test
    public void setCallback_successEvent_callbackInvokedAndUnsubscribed() {
        RegisterForPushNotificationsResultData data = new RegisterForPushNotificationsResultData("token-123", true);

        RegistrationCallbackHolder.setCallback(callback);
        EventBus.sendEvent(new RegistrationSuccessEvent(data));
        ShadowLooper.idleMainLooper();

        ArgumentCaptor<Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException>> captor =
                ArgumentCaptor.forClass(Result.class);
        verify(callback).process(captor.capture());
        assertTrue(captor.getValue().isSuccess());
        assertEquals(data, captor.getValue().getData());
        assertEquals(0, subscribersCountFor(RegistrationSuccessEvent.class));
        assertEquals(0, subscribersCountFor(RegistrationErrorEvent.class));
    }

    // Verifies that on error the callback receives a RegisterForPushNotificationsException with the event message.
    @Test
    public void setCallback_errorEvent_callbackInvokedWithException() {
        RegistrationCallbackHolder.setCallback(callback);
        EventBus.sendEvent(new RegistrationErrorEvent("registration_failed"));
        ShadowLooper.idleMainLooper();

        ArgumentCaptor<Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException>> captor =
                ArgumentCaptor.forClass(Result.class);
        verify(callback).process(captor.capture());
        assertNull(captor.getValue().getData());
        assertEquals("registration_failed", captor.getValue().getException().getMessage());
        assertEquals(0, subscribersCountFor(RegistrationSuccessEvent.class));
    }

    // Spec test 6: two waiting callbacks, one event - both invoked once, no subscriptions left,
    // a later error event does not re-invoke.
    @Test
    public void setCallback_twoWaitingCallbacks_bothResolvedByNearestEventOnce() {
        RegistrationCallbackHolder.setCallback(callback);
        RegistrationCallbackHolder.setCallback(callback2);

        RegisterForPushNotificationsResultData data = new RegisterForPushNotificationsResultData("token-x", true);
        EventBus.sendEvent(new RegistrationSuccessEvent(data));
        ShadowLooper.idleMainLooper();

        verify(callback, times(1)).process(any());
        verify(callback2, times(1)).process(any());
        assertEquals(0, subscribersCountFor(RegistrationSuccessEvent.class));
        assertEquals(0, subscribersCountFor(RegistrationErrorEvent.class));

        EventBus.sendEvent(new RegistrationErrorEvent("late_error"));
        ShadowLooper.idleMainLooper();

        verify(callback, times(1)).process(any());
        verify(callback2, times(1)).process(any());
    }
}
