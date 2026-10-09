package com.pushwoosh.firebase;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.RemoteMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class PushwooshFcmRouterServiceTest {

    public static class TestRouter extends PushwooshFcmRouterService {
        final List<String> calls = new ArrayList<>();
        RemoteMessage lastAppMessage;
        String lastToken;

        @Override
        protected void onAppMessageReceived(@NonNull RemoteMessage message) {
            calls.add("onAppMessageReceived");
            lastAppMessage = message;
        }

        @Override
        protected void onAppTokenRefresh(@NonNull String token) {
            calls.add("onAppTokenRefresh");
            lastToken = token;
        }
    }

    private TestRouter router;

    @Before
    public void setUp() {
        router = Robolectric.setupService(TestRouter.class);
    }

    private static RemoteMessage pushwooshMessage() {
        Map<String, String> data = new HashMap<>();
        data.put("pw_msg", "1");
        return new RemoteMessage.Builder("123").clearData().setData(data).build();
    }

    private static RemoteMessage foreignMessage() {
        return new RemoteMessage.Builder("123").build();
    }

    @Test
    public void pushwooshMessage_forwardedToHelper_notToApp() {
        try (MockedStatic<PushwooshFcmHelper> helper = mockStatic(PushwooshFcmHelper.class)) {
            helper.when(() -> PushwooshFcmHelper.isPushwooshMessage(any(RemoteMessage.class)))
                    .thenReturn(true);
            RemoteMessage message = pushwooshMessage();

            router.onMessageReceived(message);

            helper.verify(() -> PushwooshFcmHelper.onMessageReceived(any(Context.class), eq(message)));
            assertTrue("app callback must not see a Pushwoosh message", router.calls.isEmpty());
        }
    }

    @Test
    public void foreignMessage_goesToApp_notToHelper() {
        try (MockedStatic<PushwooshFcmHelper> helper = mockStatic(PushwooshFcmHelper.class)) {
            helper.when(() -> PushwooshFcmHelper.isPushwooshMessage(any(RemoteMessage.class)))
                    .thenReturn(false);
            RemoteMessage message = foreignMessage();

            router.onMessageReceived(message);

            assertEquals(singletonList("onAppMessageReceived"), router.calls);
            assertSame(message, router.lastAppMessage);
            helper.verify(
                    () -> PushwooshFcmHelper.onMessageReceived(any(Context.class), any(RemoteMessage.class)), never());
        }
    }

    @Test
    public void newToken_helperFirst_thenAppCallback() {
        try (MockedStatic<PushwooshFcmHelper> helper = mockStatic(PushwooshFcmHelper.class)) {
            helper.when(() -> PushwooshFcmHelper.onTokenRefresh(anyString())).thenAnswer(inv -> {
                router.calls.add("helper.onTokenRefresh");
                return null;
            });

            router.onNewToken("token-1");

            assertEquals(asList("helper.onTokenRefresh", "onAppTokenRefresh"), router.calls);
            assertEquals("token-1", router.lastToken);
        }
    }

    @Test
    public void helperThrowsOnPushwooshMessage_routerSurvives_appNotCalled() {
        try (MockedStatic<PushwooshFcmHelper> helper = mockStatic(PushwooshFcmHelper.class)) {
            helper.when(() -> PushwooshFcmHelper.isPushwooshMessage(any(RemoteMessage.class)))
                    .thenReturn(true);
            helper.when(() -> PushwooshFcmHelper.onMessageReceived(any(Context.class), any(RemoteMessage.class)))
                    .thenThrow(new RuntimeException("boom"));

            router.onMessageReceived(pushwooshMessage());

            assertTrue(router.calls.isEmpty());
        }
    }
}
