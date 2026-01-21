package com.pushwoosh.demoapp.liveupdate;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Build;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.pushwoosh.demoapp.MainActivity;
import com.pushwoosh.demoapp.R;

/**
 * Manages Live Update (promoted ongoing) notifications for Android 15+.
 * <p>
 * Live Updates are high-visibility notifications displayed as status bar chips,
 * on the lock screen, and expanded by default in the notification drawer.
 * This manager handles the full lifecycle: start, update, finish, and cancel.
 * <p>
 * Requirements for Live Update promotion:
 * <ul>
 *   <li>{@code POST_PROMOTED_NOTIFICATIONS} permission in manifest</li>
 *   <li>{@code setOngoing(true)} — notification must be ongoing</li>
 *   <li>{@code setRequestPromotedOngoing(true)} — requests promotion (API 35+)</li>
 *   <li>Must use Standard, BigTextStyle, CallStyle, or ProgressStyle</li>
 *   <li>Must include a {@code contentTitle}</li>
 * </ul>
 * <p>
 * On devices below API 35, notifications will display as regular ongoing notifications.
 *
 * @see <a href="https://developer.android.com/develop/ui/views/notifications/live-update">Live Updates Documentation</a>
 */
public class LiveUpdateNotificationManager {
    private static final String TAG = "LiveUpdateManager";

    private static final String CHANNEL_ID = "live_update_channel";
    private static final String CHANNEL_NAME = "Live Updates";
    private static final int NOTIFICATION_ID = 9001;

    private final Context context;
    private final NotificationManagerCompat notificationManager;

    private int currentProgress = 0;
    private String currentStatus = "";
    private boolean isActive = false;

    // Customization options
    @DrawableRes
    private int smallIconRes = R.drawable.ic_notifications_black_24dp;
    @ColorInt
    private int accentColor = 0;
    @Nullable
    private Bitmap largeIcon = null;
    @Nullable
    private String subText = null;

    // Action buttons
    private final java.util.List<NotificationCompat.Action> actions = new java.util.ArrayList<>();

    public LiveUpdateNotificationManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = NotificationManagerCompat.from(this.context);
        createNotificationChannel();
    }

    /**
     * Checks if the app has permission to post promoted Live Update notifications.
     * <p>
     * Uses reflection to call {@code NotificationManager.canPostPromotedNotifications()}
     * which is only available on API 35+. Returns {@code false} on older Android versions.
     *
     * @return {@code true} if Live Updates are supported and permitted, {@code false} otherwise
     */
    public boolean canPostLiveUpdates() {
        if (Build.VERSION.SDK_INT >= 35) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                try {
                    // Use reflection since this API is new in API 35
                    Object result = nm.getClass()
                            .getMethod("canPostPromotedNotifications")
                            .invoke(nm);
                    return Boolean.TRUE.equals(result);
                } catch (Exception e) {
                    Log.w(TAG, "canPostPromotedNotifications check failed", e);
                }
            }
        }
        return false;
    }

    /**
     * Opens system settings for the promoted notifications permission.
     * <p>
     * Attempts to open {@code MANAGE_APP_PROMOTED_NOTIFICATIONS} settings.
     * Falls back to general app notification settings if unavailable.
     */
    public void openLiveUpdateSettings() {
        // Using string literals to avoid API 26 requirement for Settings constants
        final String ACTION_APP_NOTIFICATION_SETTINGS = "android.settings.APP_NOTIFICATION_SETTINGS";
        final String EXTRA_APP_PACKAGE = "android.provider.extra.APP_PACKAGE";

        try {
            Intent intent = new Intent("android.settings.MANAGE_APP_PROMOTED_NOTIFICATIONS");
            intent.putExtra(EXTRA_APP_PACKAGE, context.getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "Failed to open Live Update settings", e);
            // Fallback to app notification settings
            Intent intent = new Intent(ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(EXTRA_APP_PACKAGE, context.getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    /**
     * Starts a Live Update notification with progress bar.
     * <p>
     * Creates an ongoing notification configured for Live Update promotion.
     * The notification displays a title, status text, and optional progress indicator.
     *
     * @param title    notification title (required for Live Update promotion)
     * @param status   status text displayed below the title
     * @param progress progress value 0-100, or -1 for indeterminate spinner
     */
    public void start(@NonNull String title, @NonNull String status, int progress) {
        this.currentProgress = progress;
        this.currentStatus = status;
        this.isActive = true;

        NotificationCompat.Builder builder = createBaseBuilder(title)
                .setContentText(status)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(status));

        if (progress >= 0) {
            builder.setProgress(100, progress, false);
        } else {
            builder.setProgress(0, 0, true); // Indeterminate
        }

        showNotification(builder);
        Log.d(TAG, "Live Update started: " + title);
    }

    /**
     * Starts a Live Update notification with countdown timer.
     * <p>
     * Displays a chronometer counting down from the specified duration.
     * Useful for time-limited events like delivery ETA or appointment reminders.
     *
     * @param title           notification title
     * @param status          status text displayed below the title
     * @param countdownMillis countdown duration in milliseconds
     */
    public void startWithCountdown(@NonNull String title, @NonNull String status, long countdownMillis) {
        this.currentStatus = status;
        this.isActive = true;

        long futureTime = System.currentTimeMillis() + countdownMillis;

        NotificationCompat.Builder builder = createBaseBuilder(title)
                .setContentText(status)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(status))
                .setWhen(futureTime)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true);

        showNotification(builder);
        Log.d(TAG, "Live Update started with countdown: " + title);
    }

    /**
     * Updates an active Live Update notification.
     * <p>
     * Modifies the notification content without dismissing it. Uses {@code setOnlyAlertOnce(true)}
     * to prevent repeated sounds on each update.
     *
     * @param title    updated title, or {@code null} to use default "Live Update"
     * @param status   updated status text
     * @param progress updated progress 0-100, -1 for indeterminate, or {@code null} to hide progress bar
     */
    public void update(@Nullable String title, @NonNull String status, @Nullable Integer progress) {
        if (!isActive) {
            Log.w(TAG, "Cannot update: Live Update is not active");
            return;
        }

        this.currentStatus = status;
        if (progress != null) {
            this.currentProgress = progress;
        }

        String notificationTitle = title != null ? title : "Live Update";

        NotificationCompat.Builder builder = createBaseBuilder(notificationTitle)
                .setContentText(status)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(status));

        if (progress != null) {
            if (progress >= 0) {
                builder.setProgress(100, progress, false);
            } else {
                builder.setProgress(0, 0, true); // Indeterminate
            }
        }

        showNotification(builder);
        Log.d(TAG, "Live Update updated: " + status + " (" + progress + "%)");
    }

    /**
     * Finishes the Live Update with a completion state.
     * <p>
     * Converts the ongoing notification to a dismissible one with {@code setAutoCancel(true)}.
     * The user can tap to dismiss or swipe it away.
     *
     * @param title  final title (e.g., "Delivered!")
     * @param status final status message
     */
    public void finish(@NonNull String title, @NonNull String status) {
        if (!isActive) {
            Log.w(TAG, "Cannot finish: Live Update is not active");
            return;
        }

        this.isActive = false;

        // Make title bold
        SpannableString boldTitle = new SpannableString(title);
        boldTitle.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), 0);

        // Show final state as non-ongoing notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(smallIconRes)
                .setContentTitle(boldTitle)
                .setContentText(status)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(status))
                .setAutoCancel(true)
                .setContentIntent(createContentIntent());

        // Apply customization
        if (accentColor != 0) {
            builder.setColor(accentColor);
        }
        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon);
        }

        showNotification(builder);
        Log.d(TAG, "Live Update finished: " + status);
    }

    /**
     * Cancels the Live Update notification immediately.
     */
    public void cancel() {
        isActive = false;
        notificationManager.cancel(NOTIFICATION_ID);
        Log.d(TAG, "Live Update cancelled");
    }

    /**
     * Checks if a Live Update notification is currently active.
     *
     * @return {@code true} if a Live Update is showing, {@code false} otherwise
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Returns the current progress value.
     *
     * @return progress 0-100, or -1 if indeterminate
     */
    public int getCurrentProgress() {
        return currentProgress;
    }

    /**
     * Returns the current status text.
     *
     * @return last status string passed to {@link #start} or {@link #update}
     */
    public String getCurrentStatus() {
        return currentStatus;
    }

    // ==================== Customization Methods ====================

    /**
     * Sets the small icon displayed in the status bar and notification header.
     *
     * @param iconRes drawable resource ID for the small icon
     * @return this manager for chaining
     */
    public LiveUpdateNotificationManager setSmallIcon(@DrawableRes int iconRes) {
        this.smallIconRes = iconRes;
        return this;
    }

    /**
     * Sets the accent color for the notification.
     * <p>
     * This color tints the small icon and affects the notification appearance.
     *
     * @param color ARGB color value (e.g., {@code 0xFF6200EE} for purple)
     * @return this manager for chaining
     */
    public LiveUpdateNotificationManager setAccentColor(@ColorInt int color) {
        this.accentColor = color;
        return this;
    }

    /**
     * Sets the large icon displayed on the right side of the notification.
     * <p>
     * Use this to show a product image, user avatar, or contextual graphic.
     *
     * @param bitmap the large icon bitmap, or {@code null} to remove
     * @return this manager for chaining
     */
    public LiveUpdateNotificationManager setLargeIcon(@Nullable Bitmap bitmap) {
        this.largeIcon = bitmap;
        return this;
    }

    /**
     * Sets the sub-text displayed below the notification content.
     * <p>
     * Use this for secondary info like "ETA 11:48 am" or category labels.
     *
     * @param text sub-text string, or {@code null} to remove
     * @return this manager for chaining
     */
    public LiveUpdateNotificationManager setSubText(@Nullable String text) {
        this.subText = text;
        return this;
    }

    /**
     * Adds an action button to the notification.
     * <p>
     * Maximum 3 actions are displayed. Actions appear as buttons below the content.
     *
     * @param iconRes drawable resource for the action icon
     * @param title   button text
     * @param intent  PendingIntent triggered when button is clicked
     * @return this manager for chaining
     */
    public LiveUpdateNotificationManager addAction(@DrawableRes int iconRes, @NonNull String title, @NonNull PendingIntent intent) {
        actions.add(new NotificationCompat.Action.Builder(iconRes, title, intent).build());
        return this;
    }

    /**
     * Clears all action buttons.
     *
     * @return this manager for chaining
     */
    public LiveUpdateNotificationManager clearActions() {
        actions.clear();
        return this;
    }

    private NotificationCompat.Builder createBaseBuilder(@NonNull String title) {
        // Make title bold
        SpannableString boldTitle = new SpannableString(title);
        boldTitle.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(smallIconRes)
                .setContentTitle(boldTitle)
                .setContentIntent(createContentIntent())
                .setOngoing(true) // Required for Live Update
                .setOnlyAlertOnce(true); // Don't make sound on every update

        // Apply customization
        if (accentColor != 0) {
            builder.setColor(accentColor);
        }
        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon);
        }
        if (subText != null) {
            builder.setSubText(subText);
        }

        // Add action buttons
        for (NotificationCompat.Action action : actions) {
            builder.addAction(action);
        }

        // Request promotion to Live Update (API 35+)
        if (Build.VERSION.SDK_INT >= 35) {
            try {
                // setRequestPromotedOngoing is available via NotificationCompat on newer versions
                builder.getClass()
                        .getMethod("setRequestPromotedOngoing", boolean.class)
                        .invoke(builder, true);
            } catch (Exception e) {
                // Fallback: set the extra directly
                builder.getExtras().putBoolean("android.requestPromotedOngoing", true);
            }
        }

        return builder;
    }

    private PendingIntent createContentIntent() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void showNotification(NotificationCompat.Builder builder) {
        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to show notification: missing permission", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT // DEFAULT for proper visibility
            );
            channel.setDescription("Live Update notifications for real-time status tracking");
            channel.setShowBadge(false);

            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }
}
