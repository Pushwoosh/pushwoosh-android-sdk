package com.pushwoosh.liveupdates.internal;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.manager.ManagerProvider;
import com.pushwoosh.internal.utils.NotificationUtils;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.PendingIntentUtils;
import com.pushwoosh.liveupdates.LiveUpdateProgressStyleProvider;
import com.pushwoosh.liveupdates.LiveUpdateState;
import com.pushwoosh.notification.Action;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * SDK-owned renderer that owns the entire live-update notification pipeline.
 * <p>
 * Turns a {@link LiveUpdateState} into a posted notification: it sets up the dedicated channel,
 * builds an ongoing, promoted, alert-once {@link Notification}, wires title / subtitle / header-time
 * / large icon / actions, and posts via {@link NotificationManager#notify} keyed by the
 * {@code activityId} (used as both the notification tag and a stable numeric id). The only piece it
 * delegates is the {@link Notification.ProgressStyle}, obtained from the pluggable
 * {@link LiveUpdateProgressStyleProvider} with a fall back to {@link DefaultProgressStyleProvider}
 * when the custom provider throws.
 * <p>
 * Requires API 36+; instantiated only when {@link com.pushwoosh.liveupdates.LiveUpdatesPlugin}
 * activates on a supported device.
 */
@RequiresApi(36)
public class LiveUpdateNotificationRenderer {

    private static final String TAG = "LiveUpdateNotificationRenderer";
    static final String CHANNEL_ID = "pushwoosh_live_updates";
    private static final String CHANNEL_NAME = "Live Updates";
    private static final String EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing";

    @NonNull private final LiveUpdateProgressStyleProvider provider;

    @NonNull private final DefaultProgressStyleProvider defaultProvider = new DefaultProgressStyleProvider();

    public LiveUpdateNotificationRenderer(@NonNull LiveUpdateProgressStyleProvider provider) {
        this.provider = provider;
    }

    /** Test hook: returns the configured style provider. */
    @VisibleForTesting
    @NonNull public LiveUpdateProgressStyleProvider getProviderForTest() {
        return provider;
    }

    /**
     * Builds and posts (or refreshes in place) the live-update notification for the given state.
     * <p>
     * Posting reuses the {@code activityId} as the notification tag and {@code activityId.hashCode()}
     * as the id, so a later {@code update} push with the same id replaces the existing notification
     * silently ({@code setOnlyAlertOnce}). Failures in optional steps (icon download, individual
     * actions, the style provider) are logged and degrade gracefully; the notification still posts.
     * A missing context or {@link NotificationManager} drops the render with an error log.
     */
    @WorkerThread
    public void render(@NonNull LiveUpdateState state) {
        PWLog.noise(TAG, "render(activityId=" + state.getActivityId() + ", op=" + state.getOperation() + ")");
        Context context = AndroidPlatformModule.getApplicationContext();
        if (context == null) {
            PWLog.error(TAG, "context is null, dropping render for " + state.getActivityId());
            return;
        }

        NotificationManager nm = notificationManager();
        if (nm == null) {
            PWLog.error(TAG, "NotificationManager unavailable");
            return;
        }
        ensureChannel(nm);

        int smallIcon = NotificationUtils.tryToGetIconFormStringOrGetFromApplication(null);
        if (smallIcon == -1) {
            smallIcon = context.getApplicationInfo().icon;
        }

        Notification.Builder builder = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setOngoing(true)
                .setOnlyAlertOnce(true);

        // Raw key, not setRequestPromotedOngoing(): that API is 36.1, we compile against 36.
        android.os.Bundle promotedExtras = new android.os.Bundle();
        promotedExtras.putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true);
        builder.addExtras(promotedExtras);

        if (state.getTitle() != null) builder.setContentTitle(state.getTitle());
        if (state.getSubtitle() != null) builder.setContentText(state.getSubtitle());

        // Header-time contract documented on LiveUpdateState. The three booleans are passed
        // through unconditionally — the platform defaults (false/false/true) are a visual no-op.
        if (state.getWhen() != null) {
            builder.setWhen(state.getWhen());
        }
        builder.setUsesChronometer(state.isChronometer());
        builder.setChronometerCountDown(state.isChronometerCountDown());
        builder.setShowWhen(state.isShowWhen());

        if (state.showProgressBar()) {
            builder.setStyle(styleFor(state));
        }

        if (state.getIconUrl() != null) {
            int dimension = (int) AndroidPlatformModule.getResourceProvider()
                    .getDimension(android.R.dimen.notification_large_icon_height);
            Bitmap icon = NotificationUtils.tryGetBitmap(state.getIconUrl(), dimension);
            if (icon != null) {
                builder.setLargeIcon(icon);
            } else {
                PWLog.warn(TAG, "icon load failed for " + state.getActivityId() + ", rendering without");
            }
        }

        List<Action> actions = state.getActions();
        for (int i = 0; i < actions.size(); i++) {
            try {
                int requestCode = state.getActivityId().hashCode() * 31 + i;
                String intentId = state.getActivityId() + "#" + i;
                Notification.Action platformAction = buildAction(context, actions.get(i), requestCode, intentId);
                if (platformAction != null) {
                    builder.addAction(platformAction);
                }
            } catch (Throwable t) {
                PWLog.warn(TAG, "action build failed, skipping: " + t.getMessage());
            }
        }

        try {
            nm.notify(state.getActivityId(), state.getActivityId().hashCode(), builder.build());
        } catch (Throwable t) {
            PWLog.error(TAG, "notify failed for " + state.getActivityId(), t);
        }
    }

    /** Asks the custom provider for the style, falling back to the default if it throws. */
    @NonNull private Notification.ProgressStyle styleFor(@NonNull LiveUpdateState state) {
        try {
            return provider.createStyle(state);
        } catch (Throwable t) {
            PWLog.error(TAG, "style provider threw, falling back to default", t);
            try {
                return defaultProvider.createStyle(state);
            } catch (Throwable t2) {
                PWLog.error(TAG, "default style provider also threw, rendering without progress style", t2);
                return new Notification.ProgressStyle();
            }
        }
    }

    /** Cancels the live-update notification with the given {@code activityId}; no-op if not shown. */
    @AnyThread
    public void dismiss(@NonNull String activityId) {
        PWLog.noise(TAG, "dismiss(activityId=" + activityId + ")");
        NotificationManager nm = notificationManager();
        if (nm != null) {
            nm.cancel(activityId, activityId.hashCode());
        }
    }

    /**
     * Returns the {@code activityId}s of live updates currently on screen, identified by the
     * module's channel. Reads {@link NotificationManager#getActiveNotifications()}; returns an empty
     * list if the context or manager is unavailable, or if the binder call fails.
     */
    @AnyThread
    @NonNull public List<String> getActiveIds() {
        PWLog.noise(TAG, "getActiveIds()");
        NotificationManager nm = notificationManager();
        if (nm == null) return Collections.emptyList();

        List<String> ids = new ArrayList<>();
        try {
            for (StatusBarNotification sbn : nm.getActiveNotifications()) {
                if (CHANNEL_ID.equals(sbn.getNotification().getChannelId()) && sbn.getTag() != null) {
                    ids.add(sbn.getTag());
                }
            }
        } catch (Throwable t) {
            PWLog.warn(TAG, "getActiveNotifications failed: " + t.getMessage());
        }
        return ids;
    }

    /** Resolves the platform {@link NotificationManager}, or {@code null} if it is unavailable. */
    @Nullable private static NotificationManager notificationManager() {
        ManagerProvider managerProvider = AndroidPlatformModule.getManagerProvider();
        return managerProvider == null ? null : managerProvider.getNotificationManager();
    }

    /** Creates the module's notification channel on first use; idempotent. */
    private void ensureChannel(NotificationManager nm) {
        if (nm.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        // IMPORTANCE_DEFAULT, not LOW, is intentional. Promoted-ongoing needs at least LOW, so both
        // LOW and DEFAULT qualify; DEFAULT is chosen because it keeps the live update more visible
        // in the collapsed shade. The noise downside is removed by muting the channel by hand below
        // (setSound(null) + enableVibration(false)) — don't "fix" this back to LOW.
        NotificationChannel ch =
                new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("Pushwoosh Live Updates");
        ch.setSound(null, null);
        ch.enableVibration(false);
        nm.createNotificationChannel(ch);
    }

    /**
     * Converts a Pushwoosh {@link Action} into a platform {@link Notification.Action} backed by a
     * {@link PendingIntent} of the matching type (activity / broadcast / service). The
     * {@code requestCode} is unique per action within a notification; {@code intentId} additionally
     * makes the intent {@code filterEquals}-distinct across live updates, so pending intents of
     * different activities never collide under {@code FLAG_UPDATE_CURRENT} even on a hash clash.
     *
     * @return the built action, or {@code null} if the action has no title
     */
    @SuppressWarnings("deprecation")
    private static Notification.Action buildAction(Context ctx, Action action, int requestCode, String intentId) {
        if (action.getTitle() == null) {
            PWLog.warn(TAG, "action has no title, skipping");
            return null;
        }
        String intentAction = action.getIntentAction();
        String url = action.getUrl();
        Intent intent = new Intent().setIdentifier(intentId);
        if (!TextUtils.isEmpty(intentAction)) {
            intent.setAction(intentAction);
        } else if (!TextUtils.isEmpty(url) && action.getType() == Action.Type.ACTIVITY) {
            intent.setAction(Intent.ACTION_VIEW);
        }
        if (!TextUtils.isEmpty(url)) {
            intent.setData(Uri.parse(url));
        }
        if (action.getActionClass() != null) {
            intent.setClass(ctx, action.getActionClass());
        }
        JSONObject extras = action.getExtras();
        if (extras != null) {
            // Intentionally stringified to match core SDK behaviour (NotificationBuilderManager.addAction).
            // Apps usually share one receiver between regular and live-update pushes, so the extras
            // contract must be identical — typed values here would silently diverge from regular pushes.
            Iterator<String> keys = extras.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                intent.putExtra(k, extras.optString(k));
            }
        }

        int flags = PendingIntentUtils.addImmutableFlag(PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent pi;
        switch (action.getType()) {
            case ACTIVITY:
                pi = PendingIntent.getActivity(ctx, requestCode, intent, flags);
                break;
            case BROADCAST:
                pi = PendingIntent.getBroadcast(ctx, requestCode, intent, flags);
                break;
            default:
                pi = PendingIntent.getService(ctx, requestCode, intent, flags);
        }

        return new Notification.Action.Builder(0, action.getTitle(), pi).build();
    }
}
