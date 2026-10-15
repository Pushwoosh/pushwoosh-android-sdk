package com.pushwoosh.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.NotificationUpdateReceiver;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.PendingIntentUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the platform actions for the {@link ActionButton}s of a push and the intents behind them.
 * <p>
 * The button intent extras are written and read in this one file. The tap target is always an SDK
 * component: an activity for {@code open}/{@code url}/{@code deeplink} and a broadcast receiver for
 * {@code custom}, which is also what keeps a {@code custom} tap from starting an activity on
 * Android 12+ (notification trampoline restriction).
 */
public final class ActionButtonIntents {

    private static final String TAG = "ActionButtonIntents";

    public static final String EXTRA_BUTTON_ID = "pushwoosh_button_id";
    public static final String EXTRA_NOTIFICATION_TAG = "pushwoosh_button_notification_tag";
    public static final String EXTRA_NOTIFICATION_ID = "pushwoosh_button_notification_id";

    private ActionButtonIntents() {
        /*do nothing*/
    }

    /**
     * Appends one platform action per {@link ActionButton} of the message to an already built
     * notification, preserving any actions the notification factory added itself.
     *
     * @param context        used to target the intents at this app's own components
     * @param notification   the notification about to be published; its {@code actions} array may be null
     * @param message        the push the notification was built from; its buttons are rendered here
     * @param contentIntent  the content intent of the same notification, carrying the group row id
     *                       and the summary notification id
     * @param tag            the notification tag the notification is published under
     * @param notificationId the notification id the notification is published under
     */
    public static void attachActionButtons(
            @NonNull Context context,
            @NonNull Notification notification,
            @NonNull PushMessage message,
            @NonNull Intent contentIntent,
            @Nullable String tag,
            int notificationId) {
        List<ActionButton> buttons = message.getButtons();
        if (buttons.isEmpty()) {
            return;
        }

        List<Notification.Action> built = new ArrayList<>();
        for (ActionButton button : buttons) {
            Notification.Action action = buildAction(context, button, message, contentIntent, tag, notificationId);
            if (action != null) {
                built.add(action);
            }
        }
        if (built.isEmpty()) {
            return;
        }

        Notification.Action[] existing = notification.actions;
        int existingCount = existing == null ? 0 : existing.length;
        Notification.Action[] merged = new Notification.Action[existingCount + built.size()];
        for (int i = 0; i < existingCount; i++) {
            merged[i] = existing[i];
        }
        for (int i = 0; i < built.size(); i++) {
            merged[existingCount + i] = built.get(i);
        }
        notification.actions = merged;
    }

    @Nullable static String getTappedButtonId(@NonNull Intent intent) {
        return intent.getStringExtra(EXTRA_BUTTON_ID);
    }

    @Nullable static String getNotificationTag(@NonNull Intent intent) {
        return intent.getStringExtra(EXTRA_NOTIFICATION_TAG);
    }

    static int getNotificationId(@NonNull Intent intent) {
        return intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0);
    }

    @SuppressWarnings("deprecation")
    @Nullable private static Notification.Action buildAction(
            Context context,
            ActionButton button,
            PushMessage message,
            Intent contentIntent,
            String tag,
            int notificationId) {
        try {
            Intent intent = button.getAction() == ActionButton.ActionType.CUSTOM
                    ? new Intent(context, NotificationUpdateReceiver.class)
                    : new Intent(context, NotificationOpenActivity.class);

            Bundle contentExtras = contentIntent.getExtras();
            if (contentExtras != null) {
                intent.putExtras(contentExtras);
            }
            // Fallback only: a factory that skips the bundle must not strand a button tap without it.
            if (contentExtras == null
                    || contentExtras.getBundle(NotificationIntentHelper.EXTRA_NOTIFICATION_BUNDLE) == null) {
                intent.putExtra(NotificationIntentHelper.EXTRA_NOTIFICATION_BUNDLE, message.toBundle());
            }
            intent.putExtra(EXTRA_BUTTON_ID, button.getId());
            intent.putExtra(EXTRA_NOTIFICATION_TAG, tag);
            intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);

            String key = intentKey(tag, notificationId, button.getId());
            // Deterministic, not currentTimeMillis: same-pass buttons must not collapse into one PendingIntent.
            intent.setAction("pw_button:" + key);

            int flags = PendingIntentUtils.addImmutableFlag(PendingIntent.FLAG_CANCEL_CURRENT);
            PendingIntent pendingIntent = button.getAction() == ActionButton.ActionType.CUSTOM
                    ? PendingIntent.getBroadcast(context, key.hashCode(), intent, flags)
                    : PendingIntent.getActivity(context, key.hashCode(), intent, flags);

            return new Notification.Action.Builder(0, button.getLabel(), pendingIntent).build();
        } catch (Exception e) {
            PWLog.error(TAG, "failed to build action button " + button.getId() + ", skipping it", e);
            return null;
        }
    }

    private static String intentKey(@Nullable String tag, int notificationId, String buttonId) {
        return (tag == null ? "" : tag) + "#" + notificationId + "#" + buttonId;
    }
}
