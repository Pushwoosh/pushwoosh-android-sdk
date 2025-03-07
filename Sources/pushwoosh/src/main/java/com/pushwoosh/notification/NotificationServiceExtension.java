package com.pushwoosh.notification;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.utils.DeviceUtils;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.handlers.message.system.MessageSystemHandleChainProvider;
import com.pushwoosh.notification.handlers.message.user.MessageHandleChainProvider;
import com.pushwoosh.notification.handlers.notification.NotificationOpenHandlerChainProvider;
import com.pushwoosh.notification.handlers.notification.PushStatNotificationOpenHandler;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import java.util.List;

/**
 * NotificationServiceExtension allows to customize push notification behaviour.
 * All NotificationServiceExtension ancestors must be public and must contain public constructor without parameters.
 * Application will crash on startup if this requirement is not met.
 * Custom NotificationServiceExtension should be registered in AndroidManifest.xml metadata as follows:
 * <p>
 * <pre>
 *     {@code
 *         <meta-data
 *             android:name="com.pushwoosh.notification_service_extension"
 *             android:value="com.your.package.YourNotificationServiceExtension" />
 *     }
 * </pre>
 */
public class NotificationServiceExtension {
    private static final String TAG = "NotificationService";

    private NotificationOpenHandler notificationOpenHandler;
    private PushMessageHandler pushMessageHandler;
    private PushMessageFactory pushMessageFactory;
    private PushwooshNotificationManager pushNotificationManager;
    private Config config;
    @Nullable
    private Context applicationContext;
    private PushStatNotificationOpenHandler pushStatNotificationOpenHandler;

    public NotificationServiceExtension() {
        pushMessageFactory = PushwooshPlatform.getInstance().getPushMessageFactory();
        applicationContext = AndroidPlatformModule.getApplicationContext();
        pushNotificationManager = PushwooshPlatform.getInstance().notificationManager();
        notificationOpenHandler = new NotificationOpenHandler(NotificationOpenHandlerChainProvider.getNotificationOpenHandlerChain());
        pushMessageHandler = new PushMessageHandler(MessageSystemHandleChainProvider.getMessageSystemChain(), MessageHandleChainProvider.getHandleProcessor());
        config = PushwooshPlatform.getInstance().getConfig();
        pushStatNotificationOpenHandler = PushwooshPlatform.getInstance().pushStatNotificationOpenHandler();

    }

    /**
     * Handles push arrival.
     *
     * @param pushBundle push notification payload as Bundle
     */
    @WorkerThread

    public final void handleMessage(Bundle pushBundle) {
        handleMessageInternal(pushBundle);
    }

    void handleMessageInternal(Bundle pushBundle) {
        if (pushBundle == null) {
            PWLog.info("handle null message");
            return;
        }

        PWLog.debug(TAG, "handleMessage: " + pushBundle.toString());

        if (pushMessageHandler.preHandleMessage(pushBundle)) {
            return;
        }

        PushMessage message = pushMessageFactory.createPushMessage(pushBundle);


        boolean isHandled = onMessageReceived(message);

        boolean isNeedSendPushStat = isHandled && config.getSendPushStatIfShowForegroundDisabled();
        if (isNeedSendPushStat) {
            pushStatNotificationOpenHandler.postHandleNotification(pushBundle);
        }

        pushMessageHandler.handlePushMessage(message, isHandled);
    }

    /**
     * Handles notification open.
     *
     * @param pushBundle push notification payload as Bundle
     */
    public final void handleNotification(Bundle pushBundle) {
        if (pushBundle == null) {
            PWLog.info("open null notification");
            return;
        }
        PushMessage message = new PushMessage(pushBundle);

        try {
            if (preHandleNotificationsWithUrl()) {
                if (notificationOpenHandler.preHandleNotification(pushBundle)) {
                    return;
                }
            }

            pushNotificationManager.setLaunchNotification(message);

            startActivityForPushMessage(message);
        } finally {
            notificationOpenHandler.postHandleNotification(pushBundle);
            onMessageOpened(message);
        }

    }

    /**
     * Handles notifications group open.
     *
     * @param messages list of push messages of the group which was opened
     */
    public final void handleNotificationGroup(List<PushMessage> messages) {
        onMessagesGroupOpened(messages);
    }

    /**
     * Handles notification cancel.
     *
     * @param pushBundle push notification payload as Bundle
     */
    public final void handleNotificationCanceled(Bundle pushBundle) {
        if (pushBundle == null) {
            PWLog.info("cancel null notification");
            return;
        }
        PushMessage message = new PushMessage(pushBundle);
        onMessageCanceled(message);
    }

    /**
     * Callback method which is fired when single push notification opened
     *
     * @param message push message which was opened
     */

    @SuppressWarnings({"WeakerAccess", "unused"})
    protected void onMessageOpened(final PushMessage message) {

    }

    /**
     * Callback method that is triggered when the user deletes a push notification from the Notification Center.
     *
     * @param message push message which was canceled
     */
    protected void onMessageCanceled(final PushMessage message) {

    }


    /**
     * Callback method which is fired when push notifications group opened
     *
     * @param messages list of push messages of the group which was opened
     */
    protected void onMessagesGroupOpened(final List<PushMessage> messages) {
        handleNotification(messages.get(messages.size() - 1).toBundle());
    }

    /**
     * Extension method for push notification receive handling
     *
     * @param data notification data
     * @return false if notification should be generated for this data
     */
    @WorkerThread
    protected boolean onMessageReceived(PushMessage data) {
        if (RepositoryModule.getNotificationPreferences() != null) {
            return (RepositoryModule.getNotificationPreferences().showPushnotificationAlert().get() && isAppOnForeground());
        } else {
            return false;
        }
    }

    /**
     * Extension method for push notification open handling.
     * By default starts Launcher Activity or Activity marked with @{applicationId}.MESSAGE intent filter.
     *
     * @param message notification data
     */
    @MainThread
    protected void startActivityForPushMessage(PushMessage message) {
        notificationOpenHandler.startPushLauncherActivity(message);
    }

    /**
     * Extension method for push notification open handling.
     *
     * Pushwoosh is handling notifications containing url or deeplink by default. If there is an
     * activity which can handle url or deeplink provided it will be started and the method
     * {@link #startActivityForPushMessage(PushMessage message)} will not be called.
     *
     * @return true if you want Pushwoosh to handle notifications containing url or deeplink,
     * false if you want to handle such notifications using {@link #startActivityForPushMessage(PushMessage message)} method.
     */
    protected boolean preHandleNotificationsWithUrl() {
        return true;
    }

    /**
     * @return true if application is currently in focus.
     */
    protected boolean isAppOnForeground() {
        return DeviceUtils.isAppOnForeground();
    }

    /**
     * @return Application context.
     */
    @Nullable
    protected final Context getApplicationContext() {
        return applicationContext;
    }
}
