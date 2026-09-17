package com.pushwoosh.liveupdates.internal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.pushwoosh.NotificationUpdateReceiver;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.preference.PreferenceClassValue;
import com.pushwoosh.internal.preference.PreferenceIntValue;
import com.pushwoosh.notification.NotificationIntentHelper;
import com.pushwoosh.notification.NotificationOpenActivity;
import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.notification.PushwooshNotificationFactory;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowPendingIntent;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(sdk = 36)
public class LiveUpdateIntentsTest {

    private static final String PW_LIVE = "{\"op\":\"OPERATION_START\",\"id\":\"order_1\"}";

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        // PushMessage/NotificationFactory fall back to AndroidPlatformModule's AppInfoProvider and
        // ResourceProvider when bundle keys are absent, and NotificationFactory reads its Context
        // from AndroidPlatformModule.getApplicationContext() — real init backs all of it consistently.
        AndroidPlatformModule.init(context, true);

        // PushMessage's constructor reads RepositoryModule.getNotificationPreferences().iconBackgroundColor()
        // unconditionally when the bundle carries no "ibc" key; stub it so livePushMessage() doesn't NPE.
        NotificationPrefs prefs = mock(NotificationPrefs.class);
        when(prefs.iconBackgroundColor()).thenReturn(mock(PreferenceIntValue.class));
        RepositoryModule.setNotificationPreferences(prefs);
    }

    @After
    public void tearDown() {
        RepositoryModule.setNotificationPreferences(null);
    }

    private static PushMessage livePushMessage() {
        Bundle bundle = new Bundle();
        bundle.putString("pw_live", PW_LIVE);
        return new PushMessage(bundle);
    }

    @Test
    public void contentIntent_targetsNotificationOpenActivityWithPushBundle() {
        PendingIntent pi = LiveUpdateIntents.contentIntent(context, livePushMessage(), "order_1");

        assertNotNull(pi);
        ShadowPendingIntent shadow = Shadows.shadowOf(pi);
        assertTrue(shadow.isActivityIntent());
        Intent saved = shadow.getSavedIntent();
        assertEquals(
                NotificationOpenActivity.class.getName(), saved.getComponent().getClassName());
        Bundle carried = saved.getBundleExtra(NotificationIntentHelper.EXTRA_NOTIFICATION_BUNDLE);
        assertNotNull(carried);
        assertEquals(PW_LIVE, carried.getString("pw_live"));
        assertFalse(saved.getBooleanExtra(NotificationIntentHelper.EXTRA_IS_DELETE_INTENT, false));
    }

    @Test
    public void deleteIntent_targetsNotificationUpdateReceiverMarkedAsDelete() {
        PendingIntent pi = LiveUpdateIntents.deleteIntent(context, livePushMessage(), "order_1");

        assertNotNull(pi);
        ShadowPendingIntent shadow = Shadows.shadowOf(pi);
        assertTrue(shadow.isBroadcastIntent());
        Intent saved = shadow.getSavedIntent();
        assertEquals(
                NotificationUpdateReceiver.class.getName(), saved.getComponent().getClassName());
        assertTrue(saved.getBooleanExtra(NotificationIntentHelper.EXTRA_IS_DELETE_INTENT, false));
        Bundle carried = saved.getBundleExtra(NotificationIntentHelper.EXTRA_NOTIFICATION_BUNDLE);
        assertNotNull(carried);
        assertEquals(PW_LIVE, carried.getString("pw_live"));
        // No row id: NotificationIntentHelper.handleDeleteIntent skips storage cleanup when rowId <= 0.
        assertEquals(0L, saved.getLongExtra(NotificationIntentHelper.EXTRA_NOTIFICATION_ROW_ID, 0L));
    }

    @Test
    public void deleteIntent_actionIsStablePerActivityId() {
        PendingIntent first = LiveUpdateIntents.deleteIntent(context, livePushMessage(), "order_1");
        PendingIntent second = LiveUpdateIntents.deleteIntent(context, livePushMessage(), "order_1");

        assertNotNull(first);
        assertNotNull(second);
        String firstAction = Shadows.shadowOf(first).getSavedIntent().getAction();
        String secondAction = Shadows.shadowOf(second).getSavedIntent().getAction();
        assertEquals("pw_live_delete:order_1", firstAction);
        assertEquals(firstAction, secondAction);
    }

    @Test
    public void contentIntent_honoursCustomNotificationFactoryFromPrefs() {
        // Built outside the mockStatic scope below: PushMessage's constructor itself reads
        // RepositoryModule, and that mock only stubs notificationFactoryClass().
        PushMessage message = livePushMessage();
        try (MockedStatic<RepositoryModule> repoMock = mockStatic(RepositoryModule.class)) {
            NotificationPrefs prefs = mock(NotificationPrefs.class);
            PreferenceClassValue classValue = mock(PreferenceClassValue.class);
            when(classValue.get()).thenReturn((Class) TestNotificationFactory.class);
            when(prefs.notificationFactoryClass()).thenReturn(classValue);
            repoMock.when(RepositoryModule::getNotificationPreferences).thenReturn(prefs);

            PendingIntent pi = LiveUpdateIntents.contentIntent(context, message, "order_1");

            assertNotNull(pi);
            assertEquals(
                    "custom-action-marker",
                    Shadows.shadowOf(pi).getSavedIntent().getAction());
        }
    }

    @Test
    public void contentIntent_fallsBackToDefaultFactoryWhenPrefsUnavailable() {
        // Built outside the mockStatic scope below, otherwise the RuntimeException stub would
        // fire during PushMessage construction instead of inside contentIntent().
        PushMessage message = livePushMessage();
        try (MockedStatic<RepositoryModule> repoMock = mockStatic(RepositoryModule.class)) {
            repoMock.when(RepositoryModule::getNotificationPreferences)
                    .thenThrow(new RuntimeException("not initialized"));

            PendingIntent pi = LiveUpdateIntents.contentIntent(context, message, "order_1");

            assertNotNull(pi);
            assertEquals(
                    NotificationOpenActivity.class.getName(),
                    Shadows.shadowOf(pi).getSavedIntent().getComponent().getClassName());
        }
    }

    public static class TestNotificationFactory extends PushwooshNotificationFactory {
        @Override
        public Intent getNotificationIntent(@NonNull PushMessage data) {
            Intent intent = super.getNotificationIntent(data);
            intent.setAction("custom-action-marker");
            return intent;
        }
    }
}
