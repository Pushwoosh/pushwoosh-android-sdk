package com.pushwoosh;

/**
 * Result data returned after successful push notification registration.
 * <p>
 * This class contains the push token and notification permission status received when
 * registering the device for push notifications via {@link Pushwoosh#registerForPushNotifications(com.pushwoosh.function.Callback)}.
 * The data is provided in the success callback and can be used to verify registration status
 * and retrieve the push token for server-side operations.
 * <p>
 * <b>Usage Example:</b>
 * <pre>
 * {@code
 * Pushwoosh.getInstance().registerForPushNotifications((result) -> {
 *     if (result.isSuccess()) {
 *         RegisterForPushNotificationsResultData data = result.getData();
 *
 *         // Get the push token
 *         String pushToken = data.getToken();
 *         Log.d("App", "Push token: " + pushToken);
 *
 *         // Check if notifications are enabled
 *         boolean notificationsEnabled = data.isEnabled();
 *         if (notificationsEnabled) {
 *             Log.d("App", "User has granted notification permission");
 *         } else {
 *             Log.w("App", "Notifications are disabled by user");
 *             showEnableNotificationsPrompt();
 *         }
 *
 *         // Send token to your backend server
 *         sendTokenToServer(pushToken);
 *     }
 * });
 * }
 * </pre>
 *
 * @see Pushwoosh#registerForPushNotifications(com.pushwoosh.function.Callback)
 * @see Pushwoosh#registerForPushNotificationsWithTags(com.pushwoosh.function.Callback, com.pushwoosh.tags.TagsBundle)
 */
public class RegisterForPushNotificationsResultData {
    private final String token;
    private final boolean enabled;

    public RegisterForPushNotificationsResultData(String token, boolean enabled) {
        this.token = token;
        this.enabled = enabled;
    }

    /**
     * Returns the push notification token (FCM/GCM token).
     * <p>
     * This is the unique device token assigned by Firebase Cloud Messaging (FCM) or Google Cloud
     * Messaging (GCM). The token is used by Pushwoosh to send push notifications to this specific
     * device. You can also send this token to your own backend server if you need to send pushes
     * directly through FCM.
     * <p>
     * <b>Example:</b>
     * <pre>
     * {@code
     * String token = resultData.getToken();
     * // Send to your backend
     * apiService.updateDeviceToken(userId, token);
     * // Or log for debugging
     * Log.d("Pushwoosh", "Device token: " + token);
     * }
     * </pre>
     *
     * @return FCM/GCM push token string
     * @see Pushwoosh#getPushToken()
     */
    public String getToken() {
        return token;
    }

    /**
     * Returns whether push notifications are currently enabled for this app.
     * <p>
     * This indicates if the user has granted notification permission to the app. On Android 13+
     * (API level 33+), users must explicitly grant notification permission. On earlier versions,
     * notifications are enabled by default but users can disable them in system settings.
     * <p>
     * Returns {@code false} if:
     * <ul>
     * <li>User denied notification permission (Android 13+)</li>
     * <li>User disabled notifications in system settings</li>
     * <li>App's notification channel is blocked</li>
     * </ul>
     * <p>
     * <b>Example:</b>
     * <pre>
     * {@code
     * if (!resultData.isEnabled()) {
     *     // Notifications are disabled, show explanation to user
     *     new AlertDialog.Builder(this)
     *         .setTitle("Enable Notifications")
     *         .setMessage("Please enable notifications to receive important updates")
     *         .setPositiveButton("Settings", (dialog, which) -> {
     *             // Open app notification settings
     *             Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
     *             intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
     *             startActivity(intent);
     *         })
     *         .show();
     * }
     * }
     * </pre>
     *
     * @return {@code true} if notifications are enabled, {@code false} otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }
}
