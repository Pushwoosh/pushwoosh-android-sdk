package com.pushwoosh.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;

import com.pushwoosh.NotificationUpdateReceiver;
import com.pushwoosh.PushStatisticsWorker;
import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.PushwooshWorkManagerHelper;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.notification.handlers.notification.NotificationOpenHandlerChainProvider;
import com.pushwoosh.notification.handlers.notification.PushNotificationOpenHandler;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.richmedia.RichMediaController;
import com.pushwoosh.testutil.NoUrlNotificationServiceMock;
import com.pushwoosh.testutil.NotificationServiceMock;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.LooperMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class NotificationIntentHelperButtonTest {

    private static final String BUTTONS = "["
            + "{\"id\":\"open_order\",\"label\":\"Open order\",\"action\":\"open\"},"
            + "{\"id\":\"site\",\"label\":\"Site\",\"action\":\"url\",\"url\":\"https://shop.com/42\"},"
            + "{\"id\":\"cancel_order\",\"label\":\"Cancel\",\"action\":\"custom\"}]";

    private PlatformTestManager sdk;
    private Context context;

    @Before
    public void setUp() {
        sdk = new PlatformTestManager();
        sdk.onApplicationCreated();
        context = AndroidPlatformModule.getApplicationContext();
    }

    @After
    public void tearDown() {
        sdk.tearDown();
    }

    private static Bundle pushBundle() {
        Bundle bundle = new Bundle();
        bundle.putString("p", "hash-42");
        bundle.putString("pw_msg_tag", "order-42");
        bundle.putString("pw_buttons", BUTTONS);
        return bundle;
    }

    private static Intent buttonIntent(String buttonId, Class<?> target) {
        return buttonIntent(pushBundle(), buttonId, target, 7);
    }

    private static Intent buttonIntent(String buttonId, Class<?> target, int notificationId) {
        return buttonIntent(pushBundle(), buttonId, target, notificationId);
    }

    private static Intent buttonIntent(Bundle bundle, String buttonId, Class<?> target, int notificationId) {
        Intent intent = new Intent(AndroidPlatformModule.getApplicationContext(), target);
        intent.putExtra(NotificationIntentHelper.EXTRA_NOTIFICATION_BUNDLE, bundle);
        intent.putExtra(ActionButtonIntents.EXTRA_BUTTON_ID, buttonId);
        intent.putExtra(ActionButtonIntents.EXTRA_NOTIFICATION_TAG, "order-42");
        intent.putExtra(ActionButtonIntents.EXTRA_NOTIFICATION_ID, notificationId);
        return intent;
    }

    private static Intent bodyTapIntent(Bundle bundle) {
        Intent intent = new Intent(AndroidPlatformModule.getApplicationContext(), NotificationOpenActivity.class);
        intent.putExtra(NotificationIntentHelper.EXTRA_NOTIFICATION_BUNDLE, bundle);
        return intent;
    }

    private static void registerLauncherActivity(Context context, String packageName) {
        Intent launcher = new Intent(Intent.ACTION_MAIN);
        launcher.addCategory(Intent.CATEGORY_LAUNCHER);
        launcher.setPackage(packageName);
        ResolveInfo info = new ResolveInfo();
        info.activityInfo = new ActivityInfo();
        info.activityInfo.packageName = packageName;
        info.activityInfo.name = "LauncherActivity";
        Shadows.shadowOf(context.getPackageManager()).addResolveInfoForIntent(launcher, info);
    }

    private static boolean launches(Intent intent, String packageName) {
        return intent.getComponent() != null
                && packageName.equals(intent.getComponent().getPackageName());
    }

    // Group rows are removed on BackgroundExecutor's serial executor: a task queued after
    // processIntent runs once everything queued before it has finished.
    private static void awaitBackgroundExecutor() throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        BackgroundExecutor.execute(done::countDown);
        assertTrue(done.await(2, TimeUnit.SECONDS));
    }

    private static PushMessage capturedOpenedMessage() {
        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(NotificationServiceMock.callbackMock()).onMessageOpened(captor.capture());
        return captor.getValue();
    }

    @Test
    public void processIntent_openButton_startsActivityAndReportsTheButtonOnTheMessage() throws Exception {
        NotificationIntentHelper.processIntent(
                context, buttonIntent("open_order", NotificationOpenActivity.class), () -> {});

        verify(NotificationServiceMock.callbackMock(), times(1)).startActivityForPushMessage(any());
        PushMessage opened = capturedOpenedMessage();
        assertEquals("open_order", opened.getTappedButtonId());
        assertEquals("open_order", opened.toJson().getString("tapped_button_id"));
        assertSame(opened, sdk.getNotificationManager().getLaunchNotification());
    }

    @Test
    public void processIntent_openButton_doesNotOpenThePushLink() {
        sdk.getNotificationPrefs().allowedExternalHosts().add("external.com");
        Bundle bundle = pushBundle();
        bundle.putString("l", "https://external.com/promo");

        NotificationIntentHelper.processIntent(
                context, buttonIntent(bundle, "open_order", NotificationOpenActivity.class, 7), () -> {});

        // Had the push link been opened, the SDK would have returned before raising the app.
        verify(NotificationServiceMock.callbackMock(), times(1)).startActivityForPushMessage(any());
    }

    @Test
    public void processIntent_bodyTap_opensThePushLinkInsteadOfTheApp() {
        sdk.getNotificationPrefs().allowedExternalHosts().add("external.com");
        Mockito.clearInvocations(context);
        Bundle bundle = pushBundle();
        bundle.putString("l", "https://external.com/promo");

        NotificationIntentHelper.processIntent(context, bodyTapIntent(bundle), () -> {});

        verify(NotificationServiceMock.callbackMock(), never()).startActivityForPushMessage(any());
        assertNull(sdk.getNotificationManager().getLaunchNotification());
        ArgumentCaptor<Intent> startedCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(context).startActivity(startedCaptor.capture());
        assertEquals("https://external.com/promo", startedCaptor.getValue().getDataString());
    }

    @Test
    public void processIntent_urlButton_openedOutsideTheApp_leavesTheLaunchNotificationUnset() {
        sdk.getNotificationPrefs().allowedExternalHosts().add("shop.com");

        NotificationIntentHelper.processIntent(context, buttonIntent("site", NotificationOpenActivity.class), () -> {});

        verify(NotificationServiceMock.callbackMock(), never()).startActivityForPushMessage(any());
        assertNull(sdk.getNotificationManager().getLaunchNotification());
        assertEquals("site", capturedOpenedMessage().getTappedButtonId());
    }

    @Test
    public void processIntent_urlButton_linkUnresolvable_fallsBackToLaunchingTheApp() {
        // shop.com is not an allowed external host and no activity in the test manifest resolves https links.
        NotificationIntentHelper.processIntent(context, buttonIntent("site", NotificationOpenActivity.class), () -> {});

        verify(NotificationServiceMock.callbackMock(), times(1)).startActivityForPushMessage(any());
        PushMessage opened = capturedOpenedMessage();
        assertSame(opened, sdk.getNotificationManager().getLaunchNotification());
        assertEquals("site", opened.getTappedButtonId());
    }

    @Test
    public void processIntent_customButton_doesNotStartAnyActivity() {
        NotificationIntentHelper.processIntent(
                context, buttonIntent("cancel_order", NotificationUpdateReceiver.class), () -> {});

        verify(NotificationServiceMock.callbackMock(), never()).startActivityForPushMessage(any());
        assertEquals("cancel_order", capturedOpenedMessage().getTappedButtonId());
        assertNull(sdk.getNotificationManager().getLaunchNotification());
    }

    @Test
    public void processIntent_anyButton_cancelsTheNotificationByTagAndId() {
        NotificationManager manager = (NotificationManager)
                RuntimeEnvironment.getApplication().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify("order-42", 7, new Notification());

        NotificationIntentHelper.processIntent(
                context, buttonIntent("cancel_order", NotificationUpdateReceiver.class), () -> {});

        assertNull(Shadows.shadowOf(manager).getNotification("order-42", 7));
    }

    @Test
    public void processIntent_taggedPushWithNotificationIdZero_stillCancelsTheNotification() {
        // ShowNotificationMessageHandler publishes every tagged push as notify(tag, 0): here 0 is
        // the real notification id, not a missing extra.
        NotificationManager manager = (NotificationManager)
                RuntimeEnvironment.getApplication().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify("order-42", 0, new Notification());

        NotificationIntentHelper.processIntent(
                context, buttonIntent("cancel_order", NotificationUpdateReceiver.class, 0), () -> {});

        assertNull(Shadows.shadowOf(manager).getNotification("order-42", 0));
    }

    @Test
    public void processIntent_buttonOnGroupedNotification_removesTheGroupRow() throws Exception {
        RepositoryModule.getPushBundleStorage().removeGroupPushBundles();
        long rowId = RepositoryModule.getPushBundleStorage().putGroupPushBundle(pushBundle(), 7, "orders");
        assertEquals(
                1, RepositoryModule.getPushBundleStorage().getGroupPushBundles().size());
        Intent intent = buttonIntent("cancel_order", NotificationUpdateReceiver.class);
        intent.putExtra(NotificationIntentHelper.EXTRA_NOTIFICATION_ROW_ID, rowId);

        NotificationIntentHelper.processIntent(context, intent, () -> {});
        awaitBackgroundExecutor();

        assertTrue(RepositoryModule.getPushBundleStorage().getGroupPushBundles().isEmpty());
    }

    @Test
    public void processIntent_bodyTap_leavesTappedButtonIdNull() throws Exception {
        NotificationIntentHelper.processIntent(context, bodyTapIntent(pushBundle()), () -> {});

        PushMessage opened = capturedOpenedMessage();
        assertNull(opened.getTappedButtonId());
        assertFalse(opened.toJson().has("tapped_button_id"));
    }

    @Test
    public void processIntent_anyButton_schedulesAnOpenEventCarryingTheButtonId() {
        // The scheduler is the seam: a work request named open_hash-42|cancel_order proves that the
        // button id travelled from the intent through the bundle into the statistics event.
        try (MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock =
                Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {
            workManagerHelperMock
                    .when(PushwooshWorkManagerHelper::getStatisticsConstraints)
                    .thenReturn(mock(Constraints.class));

            NotificationIntentHelper.processIntent(
                    context, buttonIntent("cancel_order", NotificationUpdateReceiver.class), () -> {});

            ArgumentCaptor<OneTimeWorkRequest> requestCaptor = ArgumentCaptor.forClass(OneTimeWorkRequest.class);
            ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
            workManagerHelperMock.verify(
                    () -> PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(
                            requestCaptor.capture(), nameCaptor.capture(), any(ExistingWorkPolicy.class)),
                    times(1));

            assertEquals("open_hash-42|cancel_order", nameCaptor.getValue());
            assertEquals(
                    "cancel_order",
                    requestCaptor.getValue().getWorkSpec().input.getString(PushStatisticsWorker.DATA_BUTTON_ID));
        }
    }

    @Test
    public void processIntent_anyButton_doesNotShowRichMedia() {
        RichMediaController richMediaController = mock(RichMediaController.class);
        WhiteboxHelper.setInternalState(PushwooshPlatform.getInstance(), "richMediaController", richMediaController);

        Bundle bundle = pushBundle();
        bundle.putString("rm", "{\"code\":\"RM-1\"}");

        NotificationIntentHelper.processIntent(
                context, buttonIntent(bundle, "cancel_order", NotificationUpdateReceiver.class, 7), () -> {});

        verify(richMediaController, never()).showResourceWrapper(any());
    }

    @Test
    public void processIntent_bodyTapOnTheSamePush_stillShowsRichMedia() {
        RichMediaController richMediaController = mock(RichMediaController.class);
        WhiteboxHelper.setInternalState(PushwooshPlatform.getInstance(), "richMediaController", richMediaController);

        Bundle bundle = pushBundle();
        bundle.putString("rm", "{\"code\":\"RM-1\"}");

        NotificationIntentHelper.processIntent(context, bodyTapIntent(bundle), () -> {});

        verify(richMediaController, times(1)).showResourceWrapper(any());
    }

    @Test
    public void processIntent_anyButton_doesNotLaunchThePackageFromTheLaunchKey() {
        registerLauncherActivity(context, "com.other.app");
        Mockito.clearInvocations(context);
        Bundle bundle = pushBundle();
        bundle.putString("launch", "com.other.app");

        NotificationIntentHelper.processIntent(
                context, buttonIntent(bundle, "cancel_order", NotificationUpdateReceiver.class, 7), () -> {});

        verify(context, never()).startActivity(argThat(started -> launches(started, "com.other.app")));
    }

    @Test
    public void processIntent_bodyTapOnTheSamePush_launchesThePackageFromTheLaunchKey() {
        registerLauncherActivity(context, "com.other.app");
        Mockito.clearInvocations(context);
        Bundle bundle = pushBundle();
        bundle.putString("launch", "com.other.app");

        NotificationIntentHelper.processIntent(context, bodyTapIntent(bundle), () -> {});

        verify(context).startActivity(argThat(started -> launches(started, "com.other.app")));
    }

    @Test
    public void processIntent_anyButton_runsTheHandlersAppsAddedToTheChain() {
        List<Bundle> seen = new ArrayList<>();
        PushNotificationOpenHandler recorder = seen::add;
        NotificationOpenHandlerChainProvider.getNotificationOpenHandlerChain().addItem(recorder);
        try {
            NotificationIntentHelper.processIntent(
                    context, buttonIntent("cancel_order", NotificationUpdateReceiver.class), () -> {});
            assertEquals(1, seen.size());
            assertEquals("cancel_order", PushBundleDataProvider.getTappedButtonId(seen.get(0)));
        } finally {
            NotificationOpenHandlerChainProvider.getNotificationOpenHandlerChain()
                    .removeItem(recorder);
        }
    }

    @Test
    public void processIntent_buttonIdNotInTheMessage_onlyClosesReportsAndNotifies() {
        NotificationIntentHelper.processIntent(
                context, buttonIntent("ghost", NotificationOpenActivity.class), () -> {});

        verify(NotificationServiceMock.callbackMock(), never()).startActivityForPushMessage(any());
        assertEquals("ghost", capturedOpenedMessage().getTappedButtonId());
    }

    @Test
    public void processIntent_urlButton_appDisabledSdkUrlHandling_behavesLikeOpen() {
        sdk.tearDown();
        Config config = MockConfig.createMock();
        sdk = new PlatformTestManager(config);
        // After PlatformTestManager's own stub, otherwise it wins and the plain mock is installed.
        when(config.getNotificationService()).thenAnswer(invocation -> NoUrlNotificationServiceMock.class);
        sdk.onApplicationCreated();
        context = AndroidPlatformModule.getApplicationContext();
        // The lever: with SDK url handling on, this host would be opened outside the app instead.
        sdk.getNotificationPrefs().allowedExternalHosts().add("shop.com");

        NotificationIntentHelper.processIntent(context, buttonIntent("site", NotificationOpenActivity.class), () -> {});

        verify(NotificationServiceMock.callbackMock(), times(1)).startActivityForPushMessage(any());
        PushMessage opened = capturedOpenedMessage();
        assertSame(opened, sdk.getNotificationManager().getLaunchNotification());
        assertEquals("site", opened.getTappedButtonId());
        assertEquals("https://shop.com/42", opened.getButtons().get(1).getUrl());
    }
}
