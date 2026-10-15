package com.pushwoosh.notification.handlers.message.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.notification.NoSummaryNotificationFactory;
import com.pushwoosh.notification.NotificationIntentHelper;
import com.pushwoosh.notification.NotificationOpenActivity;
import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.notification.PushwooshNotificationFactory;
import com.pushwoosh.notification.RecordingSummaryNotificationFactory;
import com.pushwoosh.notification.SummaryNotificationUtils;
import com.pushwoosh.notification.builder.NotificationBuilderManager;
import com.pushwoosh.repository.PushBundleStorage;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.repository.util.PushBundleDatabaseEntry;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Published by the real {@link ShowNotificationMessageHandler}, acted on through the real
 * {@link NotificationIntentHelper}; the only seam is the Robolectric notification shade.
 * Every test uses its own group name: the summary id storage and the push bundle storage live
 * in {@code RepositoryModule} for the whole test process.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = "AndroidManifest.xml")
public class GroupSummaryCountAfterChildActionTest {

    private static final String CUSTOM_BUTTON =
            "[{\"id\":\"cancel_order\",\"label\":\"Cancel\",\"action\":\"custom\"}]";

    private PlatformTestManager sdk;
    private Context context;
    private NotificationManager manager;
    private ShowNotificationMessageHandler handler;
    private int pushCounter;

    static final class Published {
        final int id;
        final Notification notification;

        Published(int id, Notification notification) {
            this.id = id;
            this.notification = notification;
        }
    }

    @Before
    public void setUp() throws InterruptedException {
        sdk = new PlatformTestManager();
        sdk.onApplicationCreated();
        awaitBackgroundExecutor();
        context = AndroidPlatformModule.getApplicationContext();
        manager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        sdk.getNotificationPrefs().multiMode().set(true);
        handler = new ShowNotificationMessageHandler();
    }

    @After
    public void tearDown() throws InterruptedException {
        awaitBackgroundExecutor();
        sdk.tearDown();
    }

    // A task queued on the serial BackgroundExecutor after processIntent runs once its work is done.
    private static void awaitBackgroundExecutor() throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        BackgroundExecutor.execute(done::countDown);
        assertTrue(done.await(2, TimeUnit.SECONDS));
    }

    private Bundle pushBundle(String group) {
        pushCounter++;
        Bundle bundle = new Bundle();
        bundle.putString("p", "hash-" + group + "-" + pushCounter);
        bundle.putString("header", "Order " + pushCounter);
        bundle.putString("title", "Order " + pushCounter + " is ready");
        bundle.putString("group_id", group);
        return bundle;
    }

    private Published publishBundle(Bundle bundle) {
        handler.handleNotification(new PushMessage(bundle));
        int id = sdk.getNotificationPrefs().messageId().get();
        Notification notification = Shadows.shadowOf(manager).getNotification(null, id);
        assertNotNull("child " + id + " must be in the shade", notification);
        return new Published(id, notification);
    }

    private Published publish(String group) {
        return publishBundle(pushBundle(group));
    }

    private Published publishWithButton(String group) {
        Bundle bundle = pushBundle(group);
        bundle.putString("pw_buttons", CUSTOM_BUTTON);
        return publishBundle(bundle);
    }

    private StatusBarNotification summaryOf(String group) {
        for (StatusBarNotification sbn : manager.getActiveNotifications()) {
            boolean isSummary = (sbn.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) != 0;
            if (isSummary && group.equals(sbn.getNotification().getGroup())) {
                return sbn;
            }
        }
        return null;
    }

    private List<StatusBarNotification> childrenOf(String group) {
        List<StatusBarNotification> children = new ArrayList<>();
        for (StatusBarNotification sbn : manager.getActiveNotifications()) {
            boolean isSummary = (sbn.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) != 0;
            if (!isSummary && group.equals(sbn.getNotification().getGroup())) {
                children.add(sbn);
            }
        }
        return children;
    }

    // Body tap and swipe: the system removes the notification before the SDK gets the intent.
    private void cancelFromShade(Published published) {
        manager.cancel(published.id);
    }

    private static Intent bodyTapIntent(Published published) {
        return Shadows.shadowOf(published.notification.contentIntent).getSavedIntent();
    }

    private static Intent buttonIntent(Published published) {
        return Shadows.shadowOf(published.notification.actions[0].actionIntent).getSavedIntent();
    }

    private static Intent swipeIntent(Published published) {
        return Shadows.shadowOf(published.notification.deleteIntent).getSavedIntent();
    }

    private void assertSummaryShows(String group, int number, Published... children) {
        StatusBarNotification summary = summaryOf(group);
        assertNotNull("summary of " + group + " must be in the shade", summary);
        for (Published child : children) {
            assertNotEquals("summary id collided with a child id", child.id, summary.getId());
        }
        assertEquals(number, summary.getNotification().number);
    }

    @Test
    public void bodyTapOnFirstChild_summaryShowsRemainingCount() throws Exception {
        String group = "as1-body-first";
        Published first = publish(group);
        Published second = publish(group);
        assertSummaryShows(group, 2, first, second);

        cancelFromShade(first);
        NotificationIntentHelper.processIntent(context, bodyTapIntent(first), () -> {});
        awaitBackgroundExecutor();

        List<StatusBarNotification> remaining = childrenOf(group);
        assertEquals(1, remaining.size());
        assertEquals(
                "the untouched child must survive", second.id, remaining.get(0).getId());
        assertSummaryShows(group, 1, first, second);
    }

    @Test
    public void buttonOnFirstChild_summaryShowsRemainingCount() throws Exception {
        String group = "as2-button-first";
        Published first = publishWithButton(group);
        Published second = publishWithButton(group);
        assertSummaryShows(group, 2, first, second);

        NotificationIntentHelper.processIntent(context, buttonIntent(first), () -> {});
        awaitBackgroundExecutor();

        assertNull(
                "the SDK cancels the notification on a button press",
                Shadows.shadowOf(manager).getNotification(null, first.id));
        List<StatusBarNotification> remaining = childrenOf(group);
        assertEquals(1, remaining.size());
        assertEquals(
                "the untouched child must survive", second.id, remaining.get(0).getId());
        assertSummaryShows(group, 1, first, second);
    }

    @Test
    public void swipeOnFirstChild_summaryShowsRemainingCount() throws Exception {
        String group = "as3-swipe-first";
        Published first = publish(group);
        Published second = publish(group);
        assertSummaryShows(group, 2, first, second);

        cancelFromShade(first);
        NotificationIntentHelper.processIntent(context, swipeIntent(first), () -> {});
        awaitBackgroundExecutor();

        List<StatusBarNotification> remaining = childrenOf(group);
        assertEquals(1, remaining.size());
        assertEquals(
                "the untouched child must survive", second.id, remaining.get(0).getId());
        assertSummaryShows(group, 1, first, second);
    }

    public static class GroupRenamingNotificationFactory extends PushwooshNotificationFactory {
        static final String SHADE_GROUP = "as8-shade-group";

        public GroupRenamingNotificationFactory() {}

        @Override
        public Notification onGenerateNotification(@NonNull PushMessage pushData) {
            Notification notification = super.onGenerateNotification(pushData);
            return NotificationBuilderManager.setNotificationGroup(notification, SHADE_GROUP);
        }
    }

    @Test
    public void customFactoryGroup_bodyTapOnFirstChild_summaryShowsRemainingCount() throws Exception {
        String pushGroup = "as8-push-group";
        sdk.getNotificationPrefs().notificationFactoryClass().set(GroupRenamingNotificationFactory.class);
        handler = new ShowNotificationMessageHandler();
        Published first = publish(pushGroup);
        Published second = publish(pushGroup);
        assertNull("no summary for the group named in the push", summaryOf(pushGroup));
        assertSummaryShows(GroupRenamingNotificationFactory.SHADE_GROUP, 2, first, second);

        cancelFromShade(first);
        NotificationIntentHelper.processIntent(context, bodyTapIntent(first), () -> {});
        awaitBackgroundExecutor();

        List<StatusBarNotification> remaining = childrenOf(GroupRenamingNotificationFactory.SHADE_GROUP);
        assertEquals(1, remaining.size());
        assertEquals(
                "the untouched child must survive", second.id, remaining.get(0).getId());
        assertSummaryShows(GroupRenamingNotificationFactory.SHADE_GROUP, 1, first, second);
    }

    @Test
    public void bodyTapIntentFromOldSdk_rowIdAndMinusOneGroupId_summaryShowsRemainingCount() throws Exception {
        String group = "as10-old-intent";
        Published first = publish(group);
        Published second = publish(group);
        assertSummaryShows(group, 2, first, second);
        Intent published = bodyTapIntent(first);
        assertEquals(-1, published.getIntExtra(NotificationIntentHelper.EXTRA_GROUP_ID, 0));

        Intent oldShape = new Intent(context, NotificationOpenActivity.class);
        oldShape.putExtra(
                NotificationIntentHelper.EXTRA_NOTIFICATION_BUNDLE,
                published.getBundleExtra(NotificationIntentHelper.EXTRA_NOTIFICATION_BUNDLE));
        oldShape.putExtra(
                NotificationIntentHelper.EXTRA_NOTIFICATION_ROW_ID,
                published.getLongExtra(NotificationIntentHelper.EXTRA_NOTIFICATION_ROW_ID, 0));
        oldShape.putExtra(NotificationIntentHelper.EXTRA_GROUP_ID, -1);

        cancelFromShade(first);
        NotificationIntentHelper.processIntent(context, oldShape, () -> {});
        awaitBackgroundExecutor();

        List<StatusBarNotification> remaining = childrenOf(group);
        assertEquals(1, remaining.size());
        assertEquals(
                "the untouched child must survive", second.id, remaining.get(0).getId());
        assertSummaryShows(group, 1, first, second);
    }

    @Test
    public void bodyTapOnSecondChild_summaryShowsRemainingCount() throws Exception {
        String group = "as4-body-second";
        Published first = publish(group);
        Published second = publish(group);
        assertSummaryShows(group, 2, first, second);

        cancelFromShade(second);
        NotificationIntentHelper.processIntent(context, bodyTapIntent(second), () -> {});
        awaitBackgroundExecutor();

        List<StatusBarNotification> remaining = childrenOf(group);
        assertEquals(1, remaining.size());
        assertEquals(
                "the untouched child must survive", first.id, remaining.get(0).getId());
        assertSummaryShows(group, 1, first, second);
    }

    @Test
    public void bodyTapOnSecondChild_rowIdMissing_summaryShowsRemainingCount() throws Exception {
        String group = "as12-row-missing";
        Published first = publish(group);
        Published second = publish(group);
        assertSummaryShows(group, 2, first, second);
        Intent published = bodyTapIntent(second);

        // Mirrors the intent a failed PushBundleStorage insert produces: row id missing, group id intact.
        Intent failedInsertShape = new Intent(context, NotificationOpenActivity.class);
        failedInsertShape.putExtra(
                NotificationIntentHelper.EXTRA_NOTIFICATION_BUNDLE,
                published.getBundleExtra(NotificationIntentHelper.EXTRA_NOTIFICATION_BUNDLE));
        failedInsertShape.putExtra(
                NotificationIntentHelper.EXTRA_GROUP_ID,
                published.getIntExtra(NotificationIntentHelper.EXTRA_GROUP_ID, 0));

        cancelFromShade(second);
        NotificationIntentHelper.processIntent(context, failedInsertShape, () -> {});
        awaitBackgroundExecutor();

        List<StatusBarNotification> remaining = childrenOf(group);
        assertEquals(1, remaining.size());
        assertEquals(
                "the untouched child must survive", first.id, remaining.get(0).getId());
        assertSummaryShows(group, 1, first, second);
    }

    @Test
    public void threeChildren_tapFirstThenSecond_summaryCountsDownTwoThenOne() throws Exception {
        String group = "as5-three";
        Published first = publish(group);
        Published second = publish(group);
        Published third = publish(group);
        assertSummaryShows(group, 3, first, second, third);

        cancelFromShade(first);
        NotificationIntentHelper.processIntent(context, bodyTapIntent(first), () -> {});
        awaitBackgroundExecutor();
        assertSummaryShows(group, 2, first, second, third);

        cancelFromShade(second);
        NotificationIntentHelper.processIntent(context, bodyTapIntent(second), () -> {});
        awaitBackgroundExecutor();
        assertSummaryShows(group, 1, first, second, third);
        List<StatusBarNotification> remaining = childrenOf(group);
        assertEquals(1, remaining.size());
        assertEquals(
                "the untouched child must survive", third.id, remaining.get(0).getId());
    }

    @Test
    public void bodyTapOnLastRemainingChild_summaryIsRemoved() throws Exception {
        String group = "as6-last";
        Published first = publish(group);
        Published second = publish(group);

        cancelFromShade(first);
        NotificationIntentHelper.processIntent(context, bodyTapIntent(first), () -> {});
        awaitBackgroundExecutor();
        assertSummaryShows(group, 1, first, second);
        List<StatusBarNotification> remainingAfterFirst = childrenOf(group);
        assertEquals(1, remainingAfterFirst.size());
        assertEquals(
                "the untouched child must survive",
                second.id,
                remainingAfterFirst.get(0).getId());

        cancelFromShade(second);
        NotificationIntentHelper.processIntent(context, bodyTapIntent(second), () -> {});
        awaitBackgroundExecutor();

        assertNull(summaryOf(group));
        assertTrue(childrenOf(group).isEmpty());
        assertEquals(-1, SummaryNotificationUtils.getNotificationIdForGroup(group));
    }

    @Test
    public void bodyTapOnTheOnlyChild_noSummaryAppears() throws Exception {
        String group = "as7-single";
        Published only = publish(group);
        assertNull(summaryOf(group));

        cancelFromShade(only);
        NotificationIntentHelper.processIntent(context, bodyTapIntent(only), () -> {});
        awaitBackgroundExecutor();

        assertNull("a lone child must not spawn a summary", summaryOf(group));
        assertEquals(-1, SummaryNotificationUtils.getNotificationIdForGroup(group));
        assertTrue(childrenOf(group).isEmpty());
    }

    @Test
    public void customSummaryFactory_bodyTapOnFirstChild_receivesRemainingCount() throws Exception {
        String group = "as9-custom-summary";
        sdk.getNotificationPrefs().summaryNotificationFactoryClass().set(RecordingSummaryNotificationFactory.class);
        RecordingSummaryNotificationFactory.lastAmount = -1;
        Published first = publish(group);
        Published second = publish(group);
        assertEquals(2, RecordingSummaryNotificationFactory.lastAmount);

        cancelFromShade(first);
        NotificationIntentHelper.processIntent(context, bodyTapIntent(first), () -> {});
        awaitBackgroundExecutor();

        assertEquals(1, RecordingSummaryNotificationFactory.lastAmount);
        assertSummaryShows(group, 1, first, second);
        assertEquals(
                "1 new orders",
                String.valueOf(
                        summaryOf(group).getNotification().extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)));
    }

    @Test
    public void summaryTap_cancelsLastChild_summaryShowsRemainingCount() throws Exception {
        String group = "as11-summary-tap";
        Published first = publish(group);
        Published second = publish(group);
        assertSummaryShows(group, 2, first, second);

        StatusBarNotification summary = summaryOf(group);
        assertNotNull(summary);
        Intent tap = Shadows.shadowOf(summary.getNotification().contentIntent).getSavedIntent();
        NotificationIntentHelper.processIntent(context, tap, () -> {});
        awaitBackgroundExecutor();

        // cancelLastStatusBarNotificationForGroup sends the last child's delete intent itself;
        // Robolectric does not auto-deliver it to the manifest receiver, so replay it here.
        NotificationIntentHelper.processIntent(context, swipeIntent(second), () -> {});
        awaitBackgroundExecutor();

        List<StatusBarNotification> remaining = childrenOf(group);
        assertEquals(1, remaining.size());
        assertEquals(
                "the untouched child must survive", first.id, remaining.get(0).getId());
        assertSummaryShows(group, 1, first, second);
    }

    @Test
    public void processIntent_rowAlreadyRemoved_doesNothingAndDoesNotThrow() throws Exception {
        String group = "rf1-row-gone";
        Published only = publish(group);
        Intent tap = bodyTapIntent(only);
        cancelFromShade(only);
        NotificationIntentHelper.processIntent(context, tap, () -> {});
        awaitBackgroundExecutor();

        NotificationIntentHelper.processIntent(context, tap, () -> {});
        awaitBackgroundExecutor();

        assertNull(summaryOf(group));
        assertEquals(-1, SummaryNotificationUtils.getNotificationIdForGroup(group));
    }

    @Test
    public void processIntent_bodyTapInOneGroup_leavesTheOtherGroupsSummaryAlone() throws Exception {
        String orders = "rf2-orders";
        String promos = "rf2-promos";
        Published order1 = publish(orders);
        Published promo1 = publish(promos);
        Published order2 = publish(orders);
        Published promo2 = publish(promos);
        assertSummaryShows(orders, 2, order1, order2);
        assertSummaryShows(promos, 2, promo1, promo2);

        cancelFromShade(order1);
        NotificationIntentHelper.processIntent(context, bodyTapIntent(order1), () -> {});
        awaitBackgroundExecutor();

        assertSummaryShows(orders, 1, order1, order2);
        assertSummaryShows(promos, 2, promo1, promo2);
        assertEquals(2, childrenOf(promos).size());
    }

    @Test
    public void processIntent_summaryDeleteIntent_stillWipesAllGroupRows() throws Exception {
        String group = "rf3-summary-swipe";
        Published first = publish(group);
        Published second = publish(group);
        StatusBarNotification summary = summaryOf(group);
        assertNotNull(summary);
        Intent summarySwipe =
                Shadows.shadowOf(summary.getNotification().deleteIntent).getSavedIntent();
        manager.cancel(summary.getId());

        NotificationIntentHelper.processIntent(context, summarySwipe, () -> {});
        awaitBackgroundExecutor();

        assertTrue(RepositoryModule.getPushBundleStorage().getGroupPushBundles().isEmpty());
        assertEquals(2, childrenOf(group).size());
    }

    @Test
    public void processIntent_summaryGenerationDisabled_doesNotCreateASummary() throws Exception {
        String group = "rf4-no-summary";
        sdk.getNotificationPrefs().summaryNotificationFactoryClass().set(NoSummaryNotificationFactory.class);
        Published first = publish(group);
        Published second = publish(group);
        assertNull(summaryOf(group));

        cancelFromShade(first);
        NotificationIntentHelper.processIntent(context, bodyTapIntent(first), () -> {});
        awaitBackgroundExecutor();

        assertNull(summaryOf(group));
        assertEquals(-1, SummaryNotificationUtils.getNotificationIdForGroup(group));
        List<StatusBarNotification> remaining = childrenOf(group);
        assertEquals(1, remaining.size());
        assertEquals(
                "the untouched child must survive", second.id, remaining.get(0).getId());
    }

    @Test
    @Config(sdk = 23)
    public void processIntent_apiBeforeN_stillRemovesTheGroupRow() throws Exception {
        String group = "rf5-api23";
        RepositoryModule.getPushBundleStorage().removeGroupPushBundles();
        Published first = publish(group);
        Published second = publish(group);
        assertEquals(
                2, RepositoryModule.getPushBundleStorage().getGroupPushBundles().size());
        long rowId = bodyTapIntent(first).getLongExtra(NotificationIntentHelper.EXTRA_NOTIFICATION_ROW_ID, 0);
        assertTrue(rowId > 0);

        cancelFromShade(first);
        NotificationIntentHelper.processIntent(context, bodyTapIntent(first), () -> {});
        awaitBackgroundExecutor();

        assertEquals(
                1, RepositoryModule.getPushBundleStorage().getGroupPushBundles().size());
        assertNull(RepositoryModule.getPushBundleStorage().getGroupPushBundleGroupId(rowId));
        long secondRowId = bodyTapIntent(second).getLongExtra(NotificationIntentHelper.EXTRA_NOTIFICATION_ROW_ID, 0);
        assertEquals(
                "the untouched child must survive",
                group,
                RepositoryModule.getPushBundleStorage().getGroupPushBundleGroupId(secondRowId));
    }

    /** Delegates everything but the row delete, which fails the way a locked or corrupt database does. */
    private static class FailingRemoveStorage implements PushBundleStorage {
        private final PushBundleStorage delegate;
        boolean removeAttempted;

        FailingRemoveStorage(PushBundleStorage delegate) {
            this.delegate = delegate;
        }

        @Override
        public long putPushBundle(Bundle pushBundle) throws Exception {
            return delegate.putPushBundle(pushBundle);
        }

        @Override
        public Bundle getPushBundle(long id) throws Exception {
            return delegate.getPushBundle(id);
        }

        @Override
        public void removePushBundle(long id) {
            delegate.removePushBundle(id);
        }

        @Override
        public long putGroupPushBundle(Bundle pushBundle, int id, String groupId) throws Exception {
            return delegate.putGroupPushBundle(pushBundle, id, groupId);
        }

        @Override
        public List<Bundle> getGroupPushBundles() {
            return delegate.getGroupPushBundles();
        }

        @Override
        public void removeGroupPushBundle(long id) {
            removeAttempted = true;
            throw new SQLiteException("database is locked");
        }

        @Override
        public void removeGroupPushBundles() {
            delegate.removeGroupPushBundles();
        }

        @Override
        public PushBundleDatabaseEntry getLastPushBundleEntryForGroup(String groupId) throws Exception {
            return delegate.getLastPushBundleEntryForGroup(groupId);
        }

        @Override
        @Nullable public String getGroupPushBundleGroupId(long rowId) {
            return delegate.getGroupPushBundleGroupId(rowId);
        }
    }

    private static void setPushBundleStorage(PushBundleStorage storage) throws Exception {
        Field field = RepositoryModule.class.getDeclaredField("pushBundleStorage");
        field.setAccessible(true);
        field.set(null, storage);
    }

    @Test
    public void rowDeleteFails_summaryStillShowsRemainingCount() throws Exception {
        String group = "rf6-remove-throws";
        Published first = publish(group);
        Published second = publish(group);
        assertSummaryShows(group, 2, first, second);

        PushBundleStorage real = RepositoryModule.getPushBundleStorage();
        FailingRemoveStorage failing = new FailingRemoveStorage(real);
        setPushBundleStorage(failing);
        try {
            cancelFromShade(first);
            NotificationIntentHelper.processIntent(context, bodyTapIntent(first), () -> {});
            awaitBackgroundExecutor();
        } finally {
            setPushBundleStorage(real);
        }

        assertTrue("the delete must have been attempted", failing.removeAttempted);
        List<StatusBarNotification> remaining = childrenOf(group);
        assertEquals(1, remaining.size());
        assertEquals(
                "the untouched child must survive", second.id, remaining.get(0).getId());
        assertSummaryShows(group, 1, first, second);
    }
}
