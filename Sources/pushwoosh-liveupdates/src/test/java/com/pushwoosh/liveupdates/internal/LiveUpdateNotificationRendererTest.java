package com.pushwoosh.liveupdates.internal;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.manager.ManagerProvider;
import com.pushwoosh.internal.platform.resource.ResourceProvider;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.liveupdates.LiveUpdateOperation;
import com.pushwoosh.liveupdates.LiveUpdateProgressStyleProvider;
import com.pushwoosh.liveupdates.LiveUpdateSegment;
import com.pushwoosh.liveupdates.LiveUpdateState;

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
public class LiveUpdateNotificationRendererTest {

    private MockedStatic<AndroidPlatformModule> platformMock;
    private Context context;
    private NotificationManager nm;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        platformMock = mockStatic(AndroidPlatformModule.class);
        platformMock.when(AndroidPlatformModule::getApplicationContext).thenReturn(context);
        ResourceProvider resourceProvider = mock(ResourceProvider.class);
        when(resourceProvider.getIdentifier(anyString(), anyString())).thenReturn(0);
        platformMock.when(AndroidPlatformModule::getResourceProvider).thenReturn(resourceProvider);
        ManagerProvider managerProvider = mock(ManagerProvider.class);
        when(managerProvider.getNotificationManager()).thenReturn(nm);
        platformMock.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProvider);
    }

    @After
    public void tearDown() {
        platformMock.close();
    }

    @Test
    public void render_createsChannelWithImportanceDefaultAndNoSound() {
        LiveUpdateState state = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.START)
                .title("Order")
                .build();

        new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider()).render(state);

        NotificationChannel ch = nm.getNotificationChannel("pushwoosh_live_updates");
        assertNotNull(ch);
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, ch.getImportance());
        assertNull(ch.getSound());
        assertFalse(ch.shouldVibrate());
    }

    @Test
    public void render_postsNotificationWithActivityIdAsTag() {
        LiveUpdateState state = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.START)
                .title("Order")
                .build();

        new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider()).render(state);

        assertEquals(1, nm.getActiveNotifications().length);
        assertEquals("order_1", nm.getActiveNotifications()[0].getTag());
        assertEquals("order_1".hashCode(), nm.getActiveNotifications()[0].getId());
    }

    @Test
    public void render_appliesProgressStyle() {
        LiveUpdateState state = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.UPDATE)
                .title("Order")
                .progress(35)
                .build();

        new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider()).render(state);

        Notification n = nm.getActiveNotifications()[0].getNotification();
        // Notification has no getStyle(), but the EXTRA_TEMPLATE extra records the style class name.
        assertEquals(Notification.ProgressStyle.class.getName(), n.extras.getString(Notification.EXTRA_TEMPLATE));
    }

    @Test
    public void render_showProgressBarFalse_postsWithoutProgressStyle() {
        LiveUpdateState state = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.UPDATE)
                .title("Order")
                .progress(35)
                .showProgressBar(false)
                .build();

        new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider()).render(state);

        Notification n = nm.getActiveNotifications()[0].getNotification();
        // No setStyle() call → the ProgressStyle template marker is absent.
        assertNotEquals(Notification.ProgressStyle.class.getName(), n.extras.getString(Notification.EXTRA_TEMPLATE));
        // Everything else is still wired: title and the promoted-ongoing extra survive.
        assertEquals("Order", n.extras.getString(Notification.EXTRA_TITLE));
        assertTrue(n.extras.getBoolean("android.requestPromotedOngoing", false));
    }

    @Test
    public void render_addsActionsFromState() throws Exception {
        org.json.JSONObject json =
                new org.json.JSONObject("{\"type\":\"BROADCAST\",\"title\":\"Cancel\",\"action\":\"com.app.CANCEL\"}");
        com.pushwoosh.notification.Action a = new com.pushwoosh.notification.Action(json);

        LiveUpdateState state = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.START)
                .title("Order")
                .actions(java.util.Collections.singletonList(a))
                .build();

        new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider()).render(state);

        Notification n = nm.getActiveNotifications()[0].getNotification();
        assertNotNull(n.actions);
        assertEquals(1, n.actions.length);
        assertEquals("Cancel", n.actions[0].title.toString());
    }

    @Test
    public void render_actionWithoutUrl_intentHasNoEmptyUriData() throws Exception {
        // Action.optString("url") returns "" when the key is absent. The renderer must treat ""
        // as missing, otherwise Uri.parse("") leaks an empty URI into the intent data.
        org.json.JSONObject json =
                new org.json.JSONObject("{\"type\":\"BROADCAST\",\"title\":\"Cancel\",\"action\":\"com.app.CANCEL\"}");
        com.pushwoosh.notification.Action a = new com.pushwoosh.notification.Action(json);

        LiveUpdateState state = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.START)
                .title("Order")
                .actions(java.util.Collections.singletonList(a))
                .build();

        new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider()).render(state);

        Notification n = nm.getActiveNotifications()[0].getNotification();
        ShadowPendingIntent pi = Shadows.shadowOf(n.actions[0].actionIntent);
        Intent saved = pi.getSavedIntent();
        assertEquals("com.app.CANCEL", saved.getAction());
        assertNull("Uri must not be parsed from missing url (\"\" from optString)", saved.getData());
    }

    @Test
    public void render_actionWithoutUrlAndAction_intentHasNullActionNotEmptyString() throws Exception {
        // Both "url" and "action" keys missing — Action.optString returns "" for both.
        // The renderer must not call setAction("") which would leave intent.getAction() == "".
        org.json.JSONObject json = new org.json.JSONObject("{\"type\":\"ACTIVITY\",\"title\":\"Open\"}");
        com.pushwoosh.notification.Action a = new com.pushwoosh.notification.Action(json);

        LiveUpdateState state = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.START)
                .title("Order")
                .actions(java.util.Collections.singletonList(a))
                .build();

        new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider()).render(state);

        Notification n = nm.getActiveNotifications()[0].getNotification();
        ShadowPendingIntent pi = Shadows.shadowOf(n.actions[0].actionIntent);
        Intent saved = pi.getSavedIntent();
        assertNotEquals("intent.getAction() must not be empty string", "", saved.getAction());
        assertNull(saved.getData());
    }

    @Test
    public void render_actionWithUrlButNoIntentAction_defaultsToActionView() throws Exception {
        org.json.JSONObject json = new org.json.JSONObject(
                "{\"type\":\"ACTIVITY\",\"title\":\"Track order\",\"url\":\"https://pushwoosh.com\"}");
        com.pushwoosh.notification.Action a = new com.pushwoosh.notification.Action(json);

        LiveUpdateState state = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.START)
                .title("Order")
                .actions(java.util.Collections.singletonList(a))
                .build();

        new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider()).render(state);

        Notification n = nm.getActiveNotifications()[0].getNotification();
        ShadowPendingIntent pi = Shadows.shadowOf(n.actions[0].actionIntent);
        Intent saved = pi.getSavedIntent();
        assertEquals(Intent.ACTION_VIEW, saved.getAction());
        assertEquals("https://pushwoosh.com", saved.getData().toString());
    }

    @Test
    public void render_multipleActions_haveUniqueRequestCodes() throws Exception {
        // Same action/data on both buttons — without unique requestCodes, FLAG_UPDATE_CURRENT
        // would collapse them into a single PendingIntent and the first button would fire
        // the second's extras.
        org.json.JSONObject json1 = new org.json.JSONObject(
                "{\"type\":\"BROADCAST\",\"title\":\"Accept\",\"action\":\"com.app.HANDLE\",\"extras\":{\"verdict\":\"accept\"}}");
        org.json.JSONObject json2 = new org.json.JSONObject(
                "{\"type\":\"BROADCAST\",\"title\":\"Cancel\",\"action\":\"com.app.HANDLE\",\"extras\":{\"verdict\":\"cancel\"}}");

        LiveUpdateState state = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.START)
                .title("Order")
                .actions(java.util.Arrays.asList(
                        new com.pushwoosh.notification.Action(json1), new com.pushwoosh.notification.Action(json2)))
                .build();

        new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider()).render(state);

        Notification n = nm.getActiveNotifications()[0].getNotification();
        assertEquals(2, n.actions.length);
        int rc1 = Shadows.shadowOf(n.actions[0].actionIntent).getRequestCode();
        int rc2 = Shadows.shadowOf(n.actions[1].actionIntent).getRequestCode();
        assertNotEquals("each action must have a unique requestCode", rc1, rc2);
    }

    @Test
    public void render_iconLoadFailure_doesNotCrash() {
        LiveUpdateState state = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.START)
                .title("Order")
                .iconUrl("http://example.invalid/will-fail.png")
                .build();

        new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider()).render(state); // must not throw

        assertEquals(1, nm.getActiveNotifications().length);
    }

    @Test
    public void dismiss_cancelsNotificationByTagAndId() {
        LiveUpdateState state = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.START)
                .title("Order")
                .build();
        LiveUpdateNotificationRenderer renderer =
                new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider());

        renderer.render(state);
        assertEquals(1, nm.getActiveNotifications().length);

        renderer.dismiss("order_1");
        assertEquals(0, nm.getActiveNotifications().length);
    }

    @Test
    public void getActiveIds_excludesNonLiveUpdateNotifications() {
        LiveUpdateNotificationRenderer renderer =
                new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider());

        renderer.render(new LiveUpdateState.Builder("order_1", LiveUpdateOperation.START)
                .title("Order")
                .build());

        // Foreign notification on a different channel, WITH a non-null tag. The tag is what makes
        // this test meaningful: it isolates the CHANNEL_ID filter as the only reason for exclusion.
        // Drop that filter and endAllLiveUpdates() would cancel unrelated app notifications.
        NotificationChannel other =
                new NotificationChannel("other_app_channel", "Other", NotificationManager.IMPORTANCE_DEFAULT);
        nm.createNotificationChannel(other);
        Notification foreign = new Notification.Builder(context, "other_app_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Newsletter")
                .build();
        nm.notify("foreign_tag", 42, foreign);

        java.util.List<String> ids = renderer.getActiveIds();

        assertTrue("live update must be reported", ids.contains("order_1"));
        assertFalse(
                "foreign-channel notification must be excluded by the CHANNEL_ID filter", ids.contains("foreign_tag"));
        assertEquals(1, ids.size());
    }

    @Test
    public void render_addsPromotedOngoingExtra() {
        LiveUpdateState state = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.START)
                .title("Order")
                .build();

        new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider()).render(state);

        Notification n = nm.getActiveNotifications()[0].getNotification();
        assertTrue(n.extras.getBoolean("android.requestPromotedOngoing", false));
    }

    @Test
    public void render_progressValueAppliedToStyle() {
        LiveUpdateState state = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.UPDATE)
                .title("Order")
                .progress(35)
                .build();

        new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider()).render(state);

        Notification n = nm.getActiveNotifications()[0].getNotification();
        assertEquals(35, n.extras.getInt(Notification.EXTRA_PROGRESS));
    }

    @Test
    public void render_existingChannelWithSameImportance_notRecreated() {
        NotificationChannel preExisting = new NotificationChannel(
                "pushwoosh_live_updates", "Live Updates", NotificationManager.IMPORTANCE_DEFAULT);
        preExisting.setDescription("custom-sentinel");
        preExisting.enableVibration(true);
        nm.createNotificationChannel(preExisting);

        LiveUpdateState state = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.START)
                .title("Order")
                .build();

        new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider()).render(state);

        NotificationChannel ch = nm.getNotificationChannel("pushwoosh_live_updates");
        assertEquals("custom-sentinel", ch.getDescription());
        assertTrue("channel must NOT be recreated when importance already matches", ch.shouldVibrate());
    }

    @Test
    public void render_segmentLengthsAppliedToPlatformStyle() {
        // Mixed lengths must reach the platform Segment one-to-one; the renderer no longer
        // hard-codes length=1, and progress is interpreted against the sum of lengths
        // (here: 2+10=12), not the segment count.
        LiveUpdateSegment s1 = new LiveUpdateSegment(android.graphics.Color.RED, 2);
        LiveUpdateSegment s2 = new LiveUpdateSegment(android.graphics.Color.GREEN, 10);
        LiveUpdateState state = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.UPDATE)
                .title("Order")
                .segments(java.util.Arrays.asList(s1, s2))
                .progress(5)
                .build();

        new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider()).render(state);

        Notification n = nm.getActiveNotifications()[0].getNotification();
        // ProgressStyle persists segments in extras as ArrayList<Bundle> under
        // "android.progressSegments"; each bundle keys length/colorInt/id. progressMax
        // is the sum of lengths — the contract that proves per-segment length reached
        // the platform.
        assertEquals(12, n.extras.getInt("android.progressMax"));

        java.util.ArrayList<android.os.Bundle> segs =
                n.extras.getParcelableArrayList("android.progressSegments", android.os.Bundle.class);
        assertNotNull(segs);
        assertEquals(2, segs.size());
        assertEquals(2, segs.get(0).getInt("length"));
        assertEquals(10, segs.get(1).getInt("length"));

        assertEquals(5, n.extras.getInt(Notification.EXTRA_PROGRESS));
    }

    @Test
    public void render_existingChannelWithUserRaisedImportance_preserved() {
        NotificationChannel preExisting =
                new NotificationChannel("pushwoosh_live_updates", "Live Updates", NotificationManager.IMPORTANCE_HIGH);
        preExisting.setDescription("user-modified");
        preExisting.enableVibration(true);
        nm.createNotificationChannel(preExisting);

        LiveUpdateState state = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.START)
                .title("Order")
                .build();

        new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider()).render(state);

        NotificationChannel ch = nm.getNotificationChannel("pushwoosh_live_updates");
        assertEquals(
                "user-raised importance must be respected, not overwritten",
                NotificationManager.IMPORTANCE_HIGH,
                ch.getImportance());
        assertEquals("user-modified", ch.getDescription());
        assertTrue("user vibration choice must be preserved", ch.shouldVibrate());
    }

    @Test
    public void render_whenSet_appliedToNotification() {
        LiveUpdateState state = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.UPDATE)
                .title("Order")
                .when(1779976320000L)
                .build();

        new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider()).render(state);

        Notification n = nm.getActiveNotifications()[0].getNotification();
        assertEquals(1779976320000L, n.when);
    }

    @Test
    public void render_chronometerCountDown_setOnExtras() {
        LiveUpdateState state = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.UPDATE)
                .title("Order")
                .when(1779976320000L)
                .chronometer(true)
                .chronometerCountDown(true)
                .build();

        new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider()).render(state);

        Notification n = nm.getActiveNotifications()[0].getNotification();
        assertTrue(n.extras.getBoolean("android.showChronometer"));
        assertTrue(n.extras.getBoolean("android.chronometerCountDown"));
    }

    @Test
    public void render_showWhenFalse_setOnExtras() {
        LiveUpdateState state = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.UPDATE)
                .title("Order")
                .showWhen(false)
                .build();

        new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider()).render(state);

        Notification n = nm.getActiveNotifications()[0].getNotification();
        assertFalse(n.extras.getBoolean("android.showWhen"));
    }

    @Test
    public void render_headerTimeDefaults_matchTodaysBehaviour() {
        LiveUpdateState state = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.START)
                .title("Order")
                .build();

        new LiveUpdateNotificationRenderer(new DefaultProgressStyleProvider()).render(state);

        Notification n = nm.getActiveNotifications()[0].getNotification();
        assertFalse(n.extras.getBoolean("android.showChronometer"));
        assertFalse(n.extras.getBoolean("android.chronometerCountDown"));
        assertTrue(n.extras.getBoolean("android.showWhen", false));
    }

    @Test
    public void render_customProviderStyleReachesNotification() {
        // A provider that forces a sentinel progress (77) different from the state's progress (35);
        // seeing 77 on the posted notification proves the provider's style — not a default style
        // derived from state — reached notify().
        LiveUpdateProgressStyleProvider custom = state -> {
            Notification.ProgressStyle s = new Notification.ProgressStyle();
            s.setProgress(77);
            return s;
        };
        LiveUpdateState state = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.UPDATE)
                .title("Order")
                .progress(35)
                .build();

        new LiveUpdateNotificationRenderer(custom).render(state);

        Notification n = nm.getActiveNotifications()[0].getNotification();
        assertEquals(77, n.extras.getInt(Notification.EXTRA_PROGRESS));
    }

    @Test
    public void render_providerThrows_fallsBackToDefaultStyleAndLogsError() {
        LiveUpdateProgressStyleProvider boom = state -> {
            throw new RuntimeException("boom");
        };
        LiveUpdateState state = new LiveUpdateState.Builder("order_1", LiveUpdateOperation.UPDATE)
                .title("Order")
                .progress(35)
                .build();

        try (MockedStatic<PWLog> log = mockStatic(PWLog.class)) {
            new LiveUpdateNotificationRenderer(boom).render(state);
            log.verify(() -> PWLog.error(
                    eq("LiveUpdateNotificationRenderer"), contains("style provider threw"), any(Throwable.class)));
        }

        // Notification still posts, using the default style derived from state (progress 35).
        Notification n = nm.getActiveNotifications()[0].getNotification();
        assertEquals(35, n.extras.getInt(Notification.EXTRA_PROGRESS));
    }
}
