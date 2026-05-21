package com.pushwoosh.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.pushwoosh.exception.UnregisterForPushNotificationException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.notification.event.DeregistrationErrorEvent;
import com.pushwoosh.notification.event.DeregistrationSuccessEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE)
public class UnregistrationCallbackHolderTest {

    private AutoCloseable mocks;

    @Mock
    private Callback<String, UnregisterForPushNotificationException> cb1;

    @Mock
    private Callback<String, UnregisterForPushNotificationException> cb2;

    @Before
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        resetStaticHolder();
        EventBus.clearSubscribersMap();
    }

    @After
    public void tearDown() throws Exception {
        resetStaticHolder();
        EventBus.clearSubscribersMap();
        mocks.close();
    }

    private void resetStaticHolder() throws Exception {
        Field field = UnregistrationCallbackHolder.class.getDeclaredField("currentCallbackHolder");
        field.setAccessible(true);
        field.set(null, null);
    }

    private Object readStaticHolder() throws Exception {
        Field field = UnregistrationCallbackHolder.class.getDeclaredField("currentCallbackHolder");
        field.setAccessible(true);
        return field.get(null);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Result<String, UnregisterForPushNotificationException>> resultCaptor() {
        return ArgumentCaptor.forClass(Result.class);
    }

    // Verifies that a DeregistrationSuccessEvent delivers a success Result whose data equals the event payload.
    @Test
    public void deliversSuccessResultWithEventData() {
        UnregistrationCallbackHolder.setCallback(cb1);

        EventBus.sendEvent(new DeregistrationSuccessEvent("old-hwid"));
        ShadowLooper.idleMainLooper();

        ArgumentCaptor<Result<String, UnregisterForPushNotificationException>> captor = resultCaptor();
        verify(cb1, times(1)).process(captor.capture());
        Result<String, UnregisterForPushNotificationException> result = captor.getValue();
        assertTrue(result.isSuccess());
        assertEquals("old-hwid", result.getData());
        assertNull(result.getException());
    }

    // Verifies that a DeregistrationErrorEvent delivers a failure Result wrapping the message in
    // UnregisterForPushNotificationException.
    @Test
    public void deliversFailureResultWrappingMessageInException() {
        UnregistrationCallbackHolder.setCallback(cb1);

        EventBus.sendEvent(new DeregistrationErrorEvent("server refused"));
        ShadowLooper.idleMainLooper();

        ArgumentCaptor<Result<String, UnregisterForPushNotificationException>> captor = resultCaptor();
        verify(cb1, times(1)).process(captor.capture());
        Result<String, UnregisterForPushNotificationException> result = captor.getValue();
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertNotNull(result.getException());
        assertTrue(result.getException() instanceof UnregisterForPushNotificationException);
        assertEquals("server refused", result.getException().getMessage());
    }

    // Verifies that setCallback(null) is a no-op and leaves the singleton free for a later registration.
    @Test
    public void setCallbackNullIsNoOpAndDoesNotLockSingleton() throws Exception {
        UnregistrationCallbackHolder.setCallback(null);

        assertNull(readStaticHolder());
        assertFalse(EventBus.getSubscribersMap().containsKey(DeregistrationSuccessEvent.class));
        assertFalse(EventBus.getSubscribersMap().containsKey(DeregistrationErrorEvent.class));

        UnregistrationCallbackHolder.setCallback(cb1);
        EventBus.sendEvent(new DeregistrationSuccessEvent("x"));
        ShadowLooper.idleMainLooper();

        ArgumentCaptor<Result<String, UnregisterForPushNotificationException>> captor = resultCaptor();
        verify(cb1, times(1)).process(captor.capture());
        assertEquals("x", captor.getValue().getData());
    }

    // Verifies that a second setCallback while a holder is active is silently dropped; only the first callback receives
    // the event.
    @Test
    public void secondSetCallbackWhileActiveIsDropped() {
        UnregistrationCallbackHolder.setCallback(cb1);
        UnregistrationCallbackHolder.setCallback(cb2);

        EventBus.sendEvent(new DeregistrationSuccessEvent("hwid"));
        ShadowLooper.idleMainLooper();

        ArgumentCaptor<Result<String, UnregisterForPushNotificationException>> captor = resultCaptor();
        verify(cb1, times(1)).process(captor.capture());
        assertTrue(captor.getValue().isSuccess());
        assertEquals("hwid", captor.getValue().getData());
        verify(cb2, never()).process(org.mockito.ArgumentMatchers.any());
    }

    // Verifies that after a success event the singleton is freed, late duplicates are ignored, and a fresh callback can
    // register.
    @Test
    public void successEventFreesSingletonAndIgnoresLateDuplicates() throws Exception {
        UnregistrationCallbackHolder.setCallback(cb1);

        EventBus.sendEvent(new DeregistrationSuccessEvent("a"));
        ShadowLooper.idleMainLooper();

        assertNull(readStaticHolder());

        EventBus.sendEvent(new DeregistrationSuccessEvent("late"));
        ShadowLooper.idleMainLooper();

        UnregistrationCallbackHolder.setCallback(cb2);
        EventBus.sendEvent(new DeregistrationSuccessEvent("b"));
        ShadowLooper.idleMainLooper();

        ArgumentCaptor<Result<String, UnregisterForPushNotificationException>> firstCaptor = resultCaptor();
        verify(cb1, times(1)).process(firstCaptor.capture());
        assertEquals("a", firstCaptor.getValue().getData());

        ArgumentCaptor<Result<String, UnregisterForPushNotificationException>> secondCaptor = resultCaptor();
        verify(cb2, times(1)).process(secondCaptor.capture());
        assertEquals("b", secondCaptor.getValue().getData());
    }

    // Verifies that the error path symmetrically detaches both subscriptions and frees the singleton, so later success
    // events are ignored.
    @Test
    public void errorEventDetachesSuccessSubscriptionAndFreesSingleton() throws Exception {
        UnregistrationCallbackHolder.setCallback(cb1);

        EventBus.sendEvent(new DeregistrationErrorEvent("nope"));
        ShadowLooper.idleMainLooper();

        EventBus.sendEvent(new DeregistrationSuccessEvent("late"));
        ShadowLooper.idleMainLooper();

        ArgumentCaptor<Result<String, UnregisterForPushNotificationException>> captor = resultCaptor();
        verify(cb1, times(1)).process(captor.capture());
        assertFalse(captor.getValue().isSuccess());
        assertNotNull(captor.getValue().getException());
        assertEquals("nope", captor.getValue().getException().getMessage());
        assertNull(readStaticHolder());
    }
}
