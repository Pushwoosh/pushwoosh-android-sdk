/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.notification.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;

import com.pushwoosh.exception.GroupIdNotFoundException;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.manager.ManagerProvider;
import com.pushwoosh.internal.platform.resource.ResourceProvider;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.preference.PreferenceIntValue;
import com.pushwoosh.notification.Action;
import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.notification.SummaryNotificationUtils;
import com.pushwoosh.repository.InboxNotificationStorage;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.PushBundleStorage;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.repository.SummaryNotificationStorage;
import com.pushwoosh.repository.util.PushBundleDatabaseEntry;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = "AndroidManifest.xml")
public class NotificationBuilderManagerTest {

    // Pre-Oreo test: on API >= 26 createNotificationBuilder returns NotificationBuilderApi26.
    @Test
    @Config(sdk = 23)
    public void createNotificationBuilder() throws Exception {
        Context context = RuntimeEnvironment.application;

        NotificationBuilder notificationBuilder = NotificationBuilderManager.createNotificationBuilder(context, "id");

        Assert.assertTrue(notificationBuilder instanceof NotificationBuilderApi14);
    }

    @Test
    public void addAction() throws Exception {
        Context context = RuntimeEnvironment.application;
        AndroidPlatformModule.init(context, true);
        NotificationBuilder notificationBuilder = Mockito.mock(NotificationBuilder.class);

        Action action = getActionMock();

        JSONObject extras = new JSONObject();
        Mockito.when(action.getExtras()).thenReturn(extras);

        NotificationBuilderManager.addAction(context, notificationBuilder, action);

        Mockito.verify(notificationBuilder).addAction(eq(17301540), eq("title"), Mockito.any(PendingIntent.class));
    }

    @NonNull private Action getActionMock() {
        Action action = Mockito.mock(Action.class);
        Mockito.when(action.getIcon()).thenReturn("android.R.drawable.ic_media_play");
        Mockito.when(action.getType()).thenReturn(Action.Type.ACTIVITY);
        Mockito.when(action.getTitle()).thenReturn("title");
        Mockito.when(action.getUrl()).thenReturn("http://url");
        Mockito.when(action.getActionClass()).thenReturn(Object.class);
        return action;
    }

    // ===========================================================================
    // addLED
    // ===========================================================================

    // Verifies that with LED globally enabled and an explicit color, setLed is called with the explicit color.
    @Test
    public void addLED_ledEnabledWithExplicitColor_setsExplicitColor() {
        NotificationBuilder builder = mock(NotificationBuilder.class);
        try (MockedStatic<RepositoryModule> repo = Mockito.mockStatic(RepositoryModule.class)) {
            stubLedPrefs(repo, true, 0xFF0000);

            NotificationBuilderManager.addLED(builder, 0x00FF00, 500, 1000);
        }

        verify(builder).setLed(0x00FF00, 500, 1000);
    }

    // Verifies that with LED globally enabled and null color, setLed falls back to the prefs default color.
    @Test
    public void addLED_ledEnabledWithNullColor_fallsBackToDefaultColor() {
        NotificationBuilder builder = mock(NotificationBuilder.class);
        try (MockedStatic<RepositoryModule> repo = Mockito.mockStatic(RepositoryModule.class)) {
            stubLedPrefs(repo, true, 0xABCDEF);

            NotificationBuilderManager.addLED(builder, null, 200, 300);
        }

        verify(builder).setLed(0xABCDEF, 200, 300);
    }

    // Verifies that with LED globally disabled but an explicit color, the explicit color still wins.
    @Test
    public void addLED_ledDisabledButExplicitColor_setsExplicitColor() {
        NotificationBuilder builder = mock(NotificationBuilder.class);
        try (MockedStatic<RepositoryModule> repo = Mockito.mockStatic(RepositoryModule.class)) {
            stubLedPrefs(repo, false, 0x111111);

            NotificationBuilderManager.addLED(builder, 0x222222, 1, 2);
        }

        verify(builder).setLed(0x222222, 1, 2);
    }

    // Verifies that with LED globally disabled and no color override, setLed is not called at all.
    @Test
    public void addLED_ledDisabledAndNoColor_doesNotSetLed() {
        NotificationBuilder builder = mock(NotificationBuilder.class);
        try (MockedStatic<RepositoryModule> repo = Mockito.mockStatic(RepositoryModule.class)) {
            stubLedPrefs(repo, false, 0x111111);

            NotificationBuilderManager.addLED(builder, null, 10, 20);
        }

        verify(builder, never()).setLed(anyInt(), anyInt(), anyInt());
    }

    private void stubLedPrefs(MockedStatic<RepositoryModule> repo, boolean enabled, int defaultColor) {
        NotificationPrefs prefs = mock(NotificationPrefs.class);
        PreferenceBooleanValue ledEnabledPref = mock(PreferenceBooleanValue.class);
        PreferenceIntValue ledColorPref = mock(PreferenceIntValue.class);
        when(ledEnabledPref.get()).thenReturn(enabled);
        when(ledColorPref.get()).thenReturn(defaultColor);
        when(prefs.ledEnabled()).thenReturn(ledEnabledPref);
        when(prefs.ledColor()).thenReturn(ledColorPref);
        repo.when(RepositoryModule::getNotificationPreferences).thenReturn(prefs);
    }

    // ===========================================================================
    // addAction (gaps)
    // ===========================================================================

    // Verifies that an app drawable name is resolved via ResourceProvider.getIdentifier.
    @Test
    public void addAction_appDrawableIcon_resolvesViaResourceProvider() {
        Context context = RuntimeEnvironment.application;
        AndroidPlatformModule.init(context, true);
        NotificationBuilder builder = mock(NotificationBuilder.class);
        Action action = mock(Action.class);
        when(action.getIcon()).thenReturn("my_icon");
        when(action.getType()).thenReturn(Action.Type.ACTIVITY);
        when(action.getTitle()).thenReturn("t");
        when(action.getUrl()).thenReturn(null);
        when(action.getActionClass()).thenReturn(null);

        ResourceProvider rp = mock(ResourceProvider.class);
        when(rp.getIdentifier("my_icon", "drawable")).thenReturn(42);

        try (MockedStatic<AndroidPlatformModule> platform =
                Mockito.mockStatic(AndroidPlatformModule.class, Mockito.CALLS_REAL_METHODS)) {
            platform.when(AndroidPlatformModule::getResourceProvider).thenReturn(rp);

            NotificationBuilderManager.addAction(context, builder, action);
        }

        verify(builder).addAction(eq(42), eq("t"), any(PendingIntent.class));
    }

    // Verifies that Action.Type.BROADCAST routes through PendingIntent.getBroadcast.
    @Test
    public void addAction_broadcastType_usesGetBroadcast() {
        Context context = RuntimeEnvironment.application;
        AndroidPlatformModule.init(context, true);
        NotificationBuilder builder = mock(NotificationBuilder.class);
        Action action = mock(Action.class);
        when(action.getIcon()).thenReturn("android.R.drawable.ic_media_play");
        when(action.getType()).thenReturn(Action.Type.BROADCAST);
        when(action.getTitle()).thenReturn("t");
        when(action.getUrl()).thenReturn(null);
        when(action.getActionClass()).thenReturn(null);

        try (MockedStatic<PendingIntent> piMock = Mockito.mockStatic(PendingIntent.class)) {
            PendingIntent fakeIntent = mock(PendingIntent.class);
            piMock.when(() -> PendingIntent.getBroadcast(eq(context), eq(0), any(Intent.class), anyInt()))
                    .thenReturn(fakeIntent);

            NotificationBuilderManager.addAction(context, builder, action);

            piMock.verify(() -> PendingIntent.getBroadcast(eq(context), eq(0), any(Intent.class), anyInt()));
            piMock.verify(() -> PendingIntent.getActivity(any(), anyInt(), any(), anyInt()), never());
            piMock.verify(() -> PendingIntent.getService(any(), anyInt(), any(), anyInt()), never());
        }
    }

    // Verifies that Action.Type.SERVICE (default branch) routes through PendingIntent.getService.
    @Test
    public void addAction_serviceType_usesGetService() {
        Context context = RuntimeEnvironment.application;
        AndroidPlatformModule.init(context, true);
        NotificationBuilder builder = mock(NotificationBuilder.class);
        Action action = mock(Action.class);
        when(action.getIcon()).thenReturn("android.R.drawable.ic_media_play");
        when(action.getType()).thenReturn(Action.Type.SERVICE);
        when(action.getTitle()).thenReturn("t");
        when(action.getUrl()).thenReturn(null);
        when(action.getActionClass()).thenReturn(null);

        try (MockedStatic<PendingIntent> piMock = Mockito.mockStatic(PendingIntent.class)) {
            PendingIntent fakeIntent = mock(PendingIntent.class);
            piMock.when(() -> PendingIntent.getService(eq(context), eq(0), any(Intent.class), anyInt()))
                    .thenReturn(fakeIntent);

            NotificationBuilderManager.addAction(context, builder, action);

            piMock.verify(() -> PendingIntent.getService(eq(context), eq(0), any(Intent.class), anyInt()));
            piMock.verify(() -> PendingIntent.getActivity(any(), anyInt(), any(), anyInt()), never());
            piMock.verify(() -> PendingIntent.getBroadcast(any(), anyInt(), any(), anyInt()), never());
        }
    }

    // Verifies that JSON extras from Action are copied as string extras to the action Intent.
    @Test
    public void addAction_jsonExtras_copiedIntoIntentExtras() throws Exception {
        Context context = RuntimeEnvironment.application;
        AndroidPlatformModule.init(context, true);
        NotificationBuilder builder = mock(NotificationBuilder.class);
        Action action = mock(Action.class);
        when(action.getIcon()).thenReturn("android.R.drawable.ic_media_play");
        when(action.getType()).thenReturn(Action.Type.ACTIVITY);
        when(action.getTitle()).thenReturn("t");
        when(action.getUrl()).thenReturn(null);
        when(action.getActionClass()).thenReturn(null);
        JSONObject extras = new JSONObject();
        extras.put("k1", "v1");
        extras.put("k2", "v2");
        when(action.getExtras()).thenReturn(extras);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        try (MockedStatic<PendingIntent> piMock = Mockito.mockStatic(PendingIntent.class)) {
            PendingIntent fakeIntent = mock(PendingIntent.class);
            piMock.when(() -> PendingIntent.getActivity(eq(context), eq(0), intentCaptor.capture(), anyInt()))
                    .thenReturn(fakeIntent);

            NotificationBuilderManager.addAction(context, builder, action);
        }

        Intent captured = intentCaptor.getValue();
        assertEquals("v1", captured.getStringExtra("k1"));
        assertEquals("v2", captured.getStringExtra("k2"));
    }

    // ===========================================================================
    // getActiveNotifications
    // ===========================================================================

    // Verifies that getActiveNotifications returns only non-summary group notifications.
    @Test
    @Config(sdk = 24)
    public void getActiveNotifications_filtersOutSummaryAndNonGroup() {
        StatusBarNotification groupNonSummary = sbn(false, true);
        StatusBarNotification groupSummary = sbn(true, true);
        StatusBarNotification noGroup = sbn(false, false);
        NotificationManager nm = mock(NotificationManager.class);
        when(nm.getActiveNotifications())
                .thenReturn(new StatusBarNotification[] {groupNonSummary, groupSummary, noGroup});
        ManagerProvider mp = mock(ManagerProvider.class);
        when(mp.getNotificationManager()).thenReturn(nm);

        List<StatusBarNotification> result;
        try (MockedStatic<AndroidPlatformModule> platform =
                Mockito.mockStatic(AndroidPlatformModule.class, Mockito.CALLS_REAL_METHODS)) {
            platform.when(AndroidPlatformModule::getManagerProvider).thenReturn(mp);

            result = NotificationBuilderManager.getActiveNotifications();
        }

        assertEquals(1, result.size());
        assertEquals(groupNonSummary, result.get(0));
    }

    // Verifies that when NotificationManager.getActiveNotifications throws, an empty list is returned.
    @Test
    @Config(sdk = 24)
    public void getActiveNotifications_managerThrows_returnsEmptyList() {
        NotificationManager nm = mock(NotificationManager.class);
        when(nm.getActiveNotifications()).thenThrow(new RuntimeException("boom"));
        ManagerProvider mp = mock(ManagerProvider.class);
        when(mp.getNotificationManager()).thenReturn(nm);

        List<StatusBarNotification> result;
        try (MockedStatic<AndroidPlatformModule> platform =
                Mockito.mockStatic(AndroidPlatformModule.class, Mockito.CALLS_REAL_METHODS)) {
            platform.when(AndroidPlatformModule::getManagerProvider).thenReturn(mp);

            result = NotificationBuilderManager.getActiveNotifications();
        }

        assertTrue(result.isEmpty());
    }

    // Verifies that when NotificationManager is null, getActiveNotifications returns an empty list.
    @Test
    @Config(sdk = 24)
    public void getActiveNotifications_notificationManagerNull_returnsEmptyList() {
        ManagerProvider mp = mock(ManagerProvider.class);
        when(mp.getNotificationManager()).thenReturn(null);

        List<StatusBarNotification> result;
        try (MockedStatic<AndroidPlatformModule> platform =
                Mockito.mockStatic(AndroidPlatformModule.class, Mockito.CALLS_REAL_METHODS)) {
            platform.when(AndroidPlatformModule::getManagerProvider).thenReturn(mp);

            result = NotificationBuilderManager.getActiveNotifications();
        }

        assertTrue(result.isEmpty());
    }

    // ===========================================================================
    // getActiveNotificationsForGroup
    // ===========================================================================

    // Verifies that getActiveNotificationsForGroup filters by Notification.getGroup() equality.
    @Test
    @Config(sdk = 24)
    public void getActiveNotificationsForGroup_filtersByGroupId() {
        StatusBarNotification g1a = sbn(false, true, "g1");
        StatusBarNotification g2 = sbn(false, true, "g2");
        StatusBarNotification g1b = sbn(false, true, "g1");
        NotificationManager nm = mock(NotificationManager.class);
        when(nm.getActiveNotifications()).thenReturn(new StatusBarNotification[] {g1a, g2, g1b});
        ManagerProvider mp = mock(ManagerProvider.class);
        when(mp.getNotificationManager()).thenReturn(nm);

        List<StatusBarNotification> matching;
        List<StatusBarNotification> missing;
        try (MockedStatic<AndroidPlatformModule> platform =
                Mockito.mockStatic(AndroidPlatformModule.class, Mockito.CALLS_REAL_METHODS)) {
            platform.when(AndroidPlatformModule::getManagerProvider).thenReturn(mp);

            matching = NotificationBuilderManager.getActiveNotificationsForGroup("g1");
            missing = NotificationBuilderManager.getActiveNotificationsForGroup("missing");
        }

        assertEquals(Arrays.asList(g1a, g1b), matching);
        assertTrue(missing.isEmpty());
    }

    // ===========================================================================
    // isGroupSummary
    // ===========================================================================

    // Verifies that isGroupSummary reads the FLAG_GROUP_SUMMARY bit (table-driven).
    @Test
    public void isGroupSummary_reflectsFlagGroupSummaryBit() {
        Object[][] cases = new Object[][] {
            {Notification.FLAG_GROUP_SUMMARY, true},
            {0, false},
            {Notification.FLAG_AUTO_CANCEL, false},
            {Notification.FLAG_GROUP_SUMMARY | Notification.FLAG_AUTO_CANCEL, true}
        };
        for (Object[] c : cases) {
            int flags = (int) c[0];
            boolean expected = (boolean) c[1];
            Notification notification = new Notification();
            notification.flags = flags;
            StatusBarNotification sbn = mock(StatusBarNotification.class);
            when(sbn.getNotification()).thenReturn(notification);

            assertEquals("flags=" + flags, expected, NotificationBuilderManager.isGroupSummary(sbn));
        }
    }

    // ===========================================================================
    // isReplacingMessage
    // ===========================================================================

    // Verifies that isReplacingMessage returns false when activeNotifications is null.
    @Test
    public void isReplacingMessage_activeNotificationsNull_returnsFalse() {
        PushMessage pm = mock(PushMessage.class);
        when(pm.getTag()).thenReturn("t");

        assertFalse(NotificationBuilderManager.isReplacingMessage(pm, null));
    }

    // Verifies that isReplacingMessage returns false when the active list is empty.
    @Test
    public void isReplacingMessage_activeNotificationsEmpty_returnsFalse() {
        PushMessage pm = mock(PushMessage.class);
        when(pm.getTag()).thenReturn("t");

        assertFalse(NotificationBuilderManager.isReplacingMessage(pm, Collections.emptyList()));
    }

    // Verifies that isReplacingMessage returns false when the push tag is null or empty (table-driven).
    @Test
    public void isReplacingMessage_pushTagNullOrEmpty_returnsFalse() {
        StatusBarNotification sbn = mock(StatusBarNotification.class);
        when(sbn.getId()).thenReturn(0);
        when(sbn.getTag()).thenReturn("any");
        List<StatusBarNotification> active = Collections.singletonList(sbn);

        String[] tags = {null, ""};
        for (String tag : tags) {
            PushMessage pm = mock(PushMessage.class);
            when(pm.getTag()).thenReturn(tag);

            assertFalse("tag=" + tag, NotificationBuilderManager.isReplacingMessage(pm, active));
        }
    }

    // Verifies that isReplacingMessage returns true when an active notification has id==0 and matching tag.
    @Test
    public void isReplacingMessage_idZeroAndTagMatches_returnsTrue() {
        PushMessage pm = mock(PushMessage.class);
        when(pm.getTag()).thenReturn("abc");
        StatusBarNotification sbn = mock(StatusBarNotification.class);
        when(sbn.getId()).thenReturn(0);
        when(sbn.getTag()).thenReturn("abc");

        assertTrue(NotificationBuilderManager.isReplacingMessage(pm, Collections.singletonList(sbn)));
    }

    // Verifies that isReplacingMessage returns false when id is non-zero even though the tag matches.
    @Test
    public void isReplacingMessage_idNonZero_returnsFalse() {
        PushMessage pm = mock(PushMessage.class);
        when(pm.getTag()).thenReturn("abc");
        StatusBarNotification sbn = mock(StatusBarNotification.class);
        when(sbn.getId()).thenReturn(7);
        when(sbn.getTag()).thenReturn("abc");

        assertFalse(NotificationBuilderManager.isReplacingMessage(pm, Collections.singletonList(sbn)));
    }

    // Verifies that isReplacingMessage returns false when id==0 but the tag differs.
    @Test
    public void isReplacingMessage_idZeroButTagDiffers_returnsFalse() {
        PushMessage pm = mock(PushMessage.class);
        when(pm.getTag()).thenReturn("abc");
        StatusBarNotification sbn = mock(StatusBarNotification.class);
        when(sbn.getId()).thenReturn(0);
        when(sbn.getTag()).thenReturn("xyz");

        assertFalse(NotificationBuilderManager.isReplacingMessage(pm, Collections.singletonList(sbn)));
    }

    // ===========================================================================
    // removeInboxNotification
    // ===========================================================================

    // Verifies that with a stored id and non-empty tag, NotificationManager.cancel(tag, id) is called.
    @Test
    public void removeInboxNotification_withTag_callsCancelTagId() {
        NotificationManager nm = mock(NotificationManager.class);
        ManagerProvider mp = mock(ManagerProvider.class);
        when(mp.getNotificationManager()).thenReturn(nm);
        InboxNotificationStorage storage = mock(InboxNotificationStorage.class);
        when(storage.getNotificationId("inboxA")).thenReturn(42);
        when(storage.getNotificationTag("inboxA")).thenReturn("tagA");

        try (MockedStatic<AndroidPlatformModule> platform =
                        Mockito.mockStatic(AndroidPlatformModule.class, Mockito.CALLS_REAL_METHODS);
                MockedStatic<RepositoryModule> repo = Mockito.mockStatic(RepositoryModule.class)) {
            platform.when(AndroidPlatformModule::getManagerProvider).thenReturn(mp);
            repo.when(RepositoryModule::getInboxNotificationStorage).thenReturn(storage);

            NotificationBuilderManager.removeInboxNotification("inboxA");
        }

        verify(nm).cancel("tagA", 42);
        verify(nm, never()).cancel(anyInt());
    }

    // Verifies that with an empty/null tag, the cancel(int) overload is used (table-driven).
    @Test
    public void removeInboxNotification_emptyOrNullTag_callsCancelIdOnly() {
        String[] tags = {null, ""};
        for (String tag : tags) {
            NotificationManager nm = mock(NotificationManager.class);
            ManagerProvider mp = mock(ManagerProvider.class);
            when(mp.getNotificationManager()).thenReturn(nm);
            InboxNotificationStorage storage = mock(InboxNotificationStorage.class);
            when(storage.getNotificationId("inboxA")).thenReturn(42);
            when(storage.getNotificationTag("inboxA")).thenReturn(tag);

            try (MockedStatic<AndroidPlatformModule> platform =
                            Mockito.mockStatic(AndroidPlatformModule.class, Mockito.CALLS_REAL_METHODS);
                    MockedStatic<RepositoryModule> repo = Mockito.mockStatic(RepositoryModule.class)) {
                platform.when(AndroidPlatformModule::getManagerProvider).thenReturn(mp);
                repo.when(RepositoryModule::getInboxNotificationStorage).thenReturn(storage);

                NotificationBuilderManager.removeInboxNotification("inboxA");
            }

            verify(nm).cancel(42);
            verify(nm, never()).cancel(anyString(), anyInt());
        }
    }

    // Verifies that with no stored notificationId, NotificationManager is not invoked.
    @Test
    public void removeInboxNotification_storageReturnsNullId_noManagerInteraction() {
        NotificationManager nm = mock(NotificationManager.class);
        ManagerProvider mp = mock(ManagerProvider.class);
        when(mp.getNotificationManager()).thenReturn(nm);
        InboxNotificationStorage storage = mock(InboxNotificationStorage.class);
        when(storage.getNotificationId("inboxX")).thenReturn(null);

        try (MockedStatic<AndroidPlatformModule> platform =
                        Mockito.mockStatic(AndroidPlatformModule.class, Mockito.CALLS_REAL_METHODS);
                MockedStatic<RepositoryModule> repo = Mockito.mockStatic(RepositoryModule.class)) {
            platform.when(AndroidPlatformModule::getManagerProvider).thenReturn(mp);
            repo.when(RepositoryModule::getInboxNotificationStorage).thenReturn(storage);

            NotificationBuilderManager.removeInboxNotification("inboxX");
        }

        verifyNoInteractions(nm);
    }

    // Verifies that an exception from storage is swallowed (no propagation, no cancel).
    @Test
    public void removeInboxNotification_storageThrows_swallowsException() {
        NotificationManager nm = mock(NotificationManager.class);
        ManagerProvider mp = mock(ManagerProvider.class);
        when(mp.getNotificationManager()).thenReturn(nm);
        InboxNotificationStorage storage = mock(InboxNotificationStorage.class);
        when(storage.getNotificationId("x")).thenThrow(new RuntimeException("db"));

        try (MockedStatic<AndroidPlatformModule> platform =
                        Mockito.mockStatic(AndroidPlatformModule.class, Mockito.CALLS_REAL_METHODS);
                MockedStatic<RepositoryModule> repo = Mockito.mockStatic(RepositoryModule.class)) {
            platform.when(AndroidPlatformModule::getManagerProvider).thenReturn(mp);
            repo.when(RepositoryModule::getInboxNotificationStorage).thenReturn(storage);

            NotificationBuilderManager.removeInboxNotification("x");
        }

        verifyNoInteractions(nm);
    }

    // ===========================================================================
    // cancelGroupSummary
    // ===========================================================================

    // Verifies that with a valid summary id, NotificationManager.cancel(id) is called and the summary entry is removed.
    @Test
    public void cancelGroupSummary_validId_cancelsAndRemoves() throws Exception {
        SummaryNotificationStorage summaryStorage = mock(SummaryNotificationStorage.class);
        NotificationManager nm = mock(NotificationManager.class);
        Context context = contextWithNotificationManager(nm);

        try (MockedStatic<AndroidPlatformModule> platform =
                        Mockito.mockStatic(AndroidPlatformModule.class, Mockito.CALLS_REAL_METHODS);
                MockedStatic<RepositoryModule> repo = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<SummaryNotificationUtils> summaryUtils =
                        Mockito.mockStatic(SummaryNotificationUtils.class)) {
            platform.when(AndroidPlatformModule::getApplicationContext).thenReturn(context);
            repo.when(RepositoryModule::getSummaryNotificationStorage).thenReturn(summaryStorage);
            summaryUtils
                    .when(() -> SummaryNotificationUtils.getNotificationIdForGroup("g1"))
                    .thenReturn(99);

            NotificationBuilderManager.cancelGroupSummary("g1");
        }

        verify(nm).cancel(99);
        verify(summaryStorage).remove("g1");
    }

    // Verifies that when getNotificationIdForGroup returns -1, no cancel or storage interaction occurs.
    @Test
    public void cancelGroupSummary_idMinusOne_noInteractions() throws Exception {
        SummaryNotificationStorage summaryStorage = mock(SummaryNotificationStorage.class);
        NotificationManager nm = mock(NotificationManager.class);
        Context context = contextWithNotificationManager(nm);

        try (MockedStatic<AndroidPlatformModule> platform =
                        Mockito.mockStatic(AndroidPlatformModule.class, Mockito.CALLS_REAL_METHODS);
                MockedStatic<RepositoryModule> repo = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<SummaryNotificationUtils> summaryUtils =
                        Mockito.mockStatic(SummaryNotificationUtils.class)) {
            platform.when(AndroidPlatformModule::getApplicationContext).thenReturn(context);
            repo.when(RepositoryModule::getSummaryNotificationStorage).thenReturn(summaryStorage);
            summaryUtils
                    .when(() -> SummaryNotificationUtils.getNotificationIdForGroup("g2"))
                    .thenReturn(-1);

            NotificationBuilderManager.cancelGroupSummary("g2");
        }

        verify(nm, never()).cancel(anyInt());
        verifyNoInteractions(summaryStorage);
    }

    // Verifies that a GroupIdNotFoundException from storage is swallowed, while cancel was still invoked.
    @Test
    public void cancelGroupSummary_storageThrowsGroupIdNotFound_swallowsExceptionButCancels() throws Exception {
        SummaryNotificationStorage summaryStorage = mock(SummaryNotificationStorage.class);
        Mockito.doThrow(new GroupIdNotFoundException("nope"))
                .when(summaryStorage)
                .remove("g3");
        NotificationManager nm = mock(NotificationManager.class);
        Context context = contextWithNotificationManager(nm);

        try (MockedStatic<AndroidPlatformModule> platform =
                        Mockito.mockStatic(AndroidPlatformModule.class, Mockito.CALLS_REAL_METHODS);
                MockedStatic<RepositoryModule> repo = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<SummaryNotificationUtils> summaryUtils =
                        Mockito.mockStatic(SummaryNotificationUtils.class)) {
            platform.when(AndroidPlatformModule::getApplicationContext).thenReturn(context);
            repo.when(RepositoryModule::getSummaryNotificationStorage).thenReturn(summaryStorage);
            summaryUtils
                    .when(() -> SummaryNotificationUtils.getNotificationIdForGroup("g3"))
                    .thenReturn(7);

            NotificationBuilderManager.cancelGroupSummary("g3");
        }

        verify(nm).cancel(7);
        verify(summaryStorage).remove("g3");
    }

    // ===========================================================================
    // cancelLastStatusBarNotificationForGroup
    // ===========================================================================

    // Verifies that the matching active notification is dismissed, deleteIntent is sent, and the push bundle row is
    // removed.
    @Test
    @Config(sdk = 24)
    public void cancelLastStatusBarNotificationForGroup_matchingActive_cancelsAndRemovesBundle() throws Exception {
        PushBundleStorage pushBundleStorage = mock(PushBundleStorage.class);
        PushBundleDatabaseEntry entry = new PushBundleDatabaseEntry(5, 11L, new Bundle());
        when(pushBundleStorage.getLastPushBundleEntryForGroup("g1")).thenReturn(entry);

        PendingIntent deleteIntent = mock(PendingIntent.class);
        Notification notification = Mockito.spy(new Notification());
        notification.deleteIntent = deleteIntent;
        when(notification.getGroup()).thenReturn("g1");
        StatusBarNotification active = mock(StatusBarNotification.class);
        when(active.getNotification()).thenReturn(notification);
        when(active.getId()).thenReturn(5);
        when(active.isGroup()).thenReturn(true);

        NotificationManager nm = mock(NotificationManager.class);
        // not empty -> no cascade to cancelGroupSummary
        when(nm.getActiveNotifications()).thenReturn(new StatusBarNotification[] {active});
        ManagerProvider mp = mock(ManagerProvider.class);
        when(mp.getNotificationManager()).thenReturn(nm);
        SummaryNotificationStorage summaryStorage = mock(SummaryNotificationStorage.class);
        Context context = contextWithNotificationManager(nm);

        try (MockedStatic<AndroidPlatformModule> platform =
                        Mockito.mockStatic(AndroidPlatformModule.class, Mockito.CALLS_REAL_METHODS);
                MockedStatic<RepositoryModule> repo = Mockito.mockStatic(RepositoryModule.class)) {
            platform.when(AndroidPlatformModule::getApplicationContext).thenReturn(context);
            platform.when(AndroidPlatformModule::getManagerProvider).thenReturn(mp);
            repo.when(RepositoryModule::getPushBundleStorage).thenReturn(pushBundleStorage);
            repo.when(RepositoryModule::getSummaryNotificationStorage).thenReturn(summaryStorage);

            NotificationBuilderManager.cancelLastStatusBarNotificationForGroup("g1");
        }

        verify(deleteIntent).send();
        verify(nm).cancel(5);
        verify(pushBundleStorage).removeGroupPushBundle(11L);
        verifyNoInteractions(summaryStorage);
    }

    // Verifies that when the active list becomes empty after the cancel, cancelGroupSummary is invoked for the group.
    @Test
    @Config(sdk = 24)
    public void cancelLastStatusBarNotificationForGroup_activeListEmptyAfterCancel_cascadesToCancelGroupSummary()
            throws Exception {
        PushBundleStorage pushBundleStorage = mock(PushBundleStorage.class);
        PushBundleDatabaseEntry entry = new PushBundleDatabaseEntry(5, 11L, new Bundle());
        when(pushBundleStorage.getLastPushBundleEntryForGroup("g1")).thenReturn(entry);

        PendingIntent deleteIntent = mock(PendingIntent.class);
        Notification notification = Mockito.spy(new Notification());
        notification.deleteIntent = deleteIntent;
        when(notification.getGroup()).thenReturn("g1");
        StatusBarNotification active = mock(StatusBarNotification.class);
        when(active.getNotification()).thenReturn(notification);
        when(active.getId()).thenReturn(5);
        when(active.isGroup()).thenReturn(true);

        NotificationManager nm = mock(NotificationManager.class);
        // first call: for getActiveNotificationsForGroup, returns the matching active.
        // second call: for getActiveNotifications().isEmpty() check, returns empty -> cascade.
        when(nm.getActiveNotifications())
                .thenReturn(new StatusBarNotification[] {active})
                .thenReturn(new StatusBarNotification[] {});
        ManagerProvider mp = mock(ManagerProvider.class);
        when(mp.getNotificationManager()).thenReturn(nm);
        SummaryNotificationStorage summaryStorage = mock(SummaryNotificationStorage.class);
        Context context = contextWithNotificationManager(nm);

        try (MockedStatic<AndroidPlatformModule> platform =
                        Mockito.mockStatic(AndroidPlatformModule.class, Mockito.CALLS_REAL_METHODS);
                MockedStatic<RepositoryModule> repo = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<SummaryNotificationUtils> summaryUtils =
                        Mockito.mockStatic(SummaryNotificationUtils.class)) {
            platform.when(AndroidPlatformModule::getApplicationContext).thenReturn(context);
            platform.when(AndroidPlatformModule::getManagerProvider).thenReturn(mp);
            repo.when(RepositoryModule::getPushBundleStorage).thenReturn(pushBundleStorage);
            repo.when(RepositoryModule::getSummaryNotificationStorage).thenReturn(summaryStorage);
            summaryUtils
                    .when(() -> SummaryNotificationUtils.getNotificationIdForGroup("g1"))
                    .thenReturn(77);

            NotificationBuilderManager.cancelLastStatusBarNotificationForGroup("g1");
        }

        verify(nm).cancel(5);
        verify(nm).cancel(77);
        verify(summaryStorage).remove("g1");
        verify(pushBundleStorage).removeGroupPushBundle(11L);
    }

    // ===========================================================================
    // Helpers
    // ===========================================================================

    private static StatusBarNotification sbn(boolean isSummary, boolean isGroup) {
        return sbn(isSummary, isGroup, null);
    }

    private static StatusBarNotification sbn(boolean isSummary, boolean isGroup, String group) {
        Notification notification = Mockito.spy(new Notification());
        if (isSummary) {
            notification.flags |= Notification.FLAG_GROUP_SUMMARY;
        }
        if (group != null) {
            when(notification.getGroup()).thenReturn(group);
        }
        StatusBarNotification sbn = mock(StatusBarNotification.class);
        when(sbn.getNotification()).thenReturn(notification);
        when(sbn.isGroup()).thenReturn(isGroup);
        return sbn;
    }

    private static Context contextWithNotificationManager(NotificationManager nm) {
        Context context = mock(Context.class);
        when(context.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(nm);
        return context;
    }
}
