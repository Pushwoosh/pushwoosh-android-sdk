package com.pushwoosh.notification;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.repository.DbLocalNotification;
import com.pushwoosh.repository.RepositoryModule;

import java.io.Serializable;

/**
 * Represents a handle to a scheduled local notification that allows you to manage and cancel it.
 * <p>
 * This class is returned by {@link com.pushwoosh.Pushwoosh#scheduleLocalNotification(LocalNotification)}
 * and provides methods to control the notification's lifecycle after it has been scheduled.
 * <p>
 * <b>Key Features:</b>
 * <ul>
 * <li>Cancel scheduled notifications before they are displayed</li>
 * <li>Remove notifications that have already been displayed</li>
 * <li>Track scheduled notifications using unique request IDs</li>
 * </ul>
 * <p>
 * <b>Quick Start:</b>
 * <pre>
 * {@code
 *   // Schedule a notification and save the request
 *   LocalNotification notification = new LocalNotification.Builder()
 *       .setMessage("Cart reminder")
 *       .setDelay(3600)
 *       .build();
 *
 *   LocalNotificationRequest request = Pushwoosh.getInstance()
 *       .scheduleLocalNotification(notification);
 *
 *   // Save request ID for later use
 *   int requestId = request.getRequestId();
 *   SharedPreferences.Editor editor = prefs.edit();
 *   editor.putInt("reminder_request_id", requestId);
 *   editor.apply();
 *
 *   // Later, cancel the notification
 *   int savedRequestId = prefs.getInt("reminder_request_id", -1);
 *   if (savedRequestId != -1) {
 *       LocalNotificationRequest savedRequest = new LocalNotificationRequest(savedRequestId);
 *       savedRequest.cancel(); // Removes from schedule and notification tray
 *   }
 * }
 * </pre>
 * <p>
 * <b>Important Notes:</b>
 * <ul>
 * <li>Request IDs are unique integers assigned by the SDK</li>
 * <li>Scheduled notifications persist across app restarts</li>
 * <li>Use {@link #cancel()} to remove both scheduled and displayed notifications</li>
 * <li>Use {@link #unschedule()} to only cancel scheduled notifications (doesn't remove displayed ones)</li>
 * </ul>
 *
 * @see LocalNotification
 * @see com.pushwoosh.Pushwoosh#scheduleLocalNotification(LocalNotification)
 * @see #cancel()
 * @see #unschedule()
 */
public class LocalNotificationRequest implements Serializable {
    private int requestId;

    /**
     * Returns the unique request ID for this scheduled notification.
     * <p>
     * The request ID can be used to recreate a LocalNotificationRequest object later
     * for cancellation or management purposes. Store this ID if you need to cancel
     * the notification in the future.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Schedule notification and store request ID
     *   LocalNotification notification = new LocalNotification.Builder()
     *       .setMessage("Daily reminder")
     *       .setDelay(86400)
     *       .build();
     *
     *   LocalNotificationRequest request = Pushwoosh.getInstance()
     *       .scheduleLocalNotification(notification);
     *
     *   // Save request ID in database or SharedPreferences
     *   int requestId = request.getRequestId();
     *   database.saveNotificationRequestId("daily_reminder", requestId);
     *
     *   // Later, retrieve and cancel
     *   int savedId = database.getNotificationRequestId("daily_reminder");
     *   new LocalNotificationRequest(savedId).cancel();
     * }
     * </pre>
     *
     * @return unique request ID for this notification
     */
    public int getRequestId() {
        return requestId;
    }

    /**
     * Creates a LocalNotificationRequest instance with the specified request ID.
     * <p>
     * This constructor is typically used to recreate a request object from a previously
     * saved request ID, allowing you to cancel or manage notifications scheduled in
     * previous app sessions.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Retrieve saved request ID from storage
     *   SharedPreferences prefs = context.getSharedPreferences("app_prefs", MODE_PRIVATE);
     *   int savedRequestId = prefs.getInt("cart_reminder_id", -1);
     *
     *   if (savedRequestId != -1) {
     *       // Recreate the request and cancel it
     *       LocalNotificationRequest request = new LocalNotificationRequest(savedRequestId);
     *       request.cancel();
     *
     *       // Remove from storage
     *       prefs.edit().remove("cart_reminder_id").apply();
     *   }
     * }
     * </pre>
     *
     * @param requestId the unique request ID of the scheduled notification
     */
    public LocalNotificationRequest(int requestId) {
        this.requestId = requestId;
    }

    /**
     * Cancels and removes the local notification associated with this request.
     * <p>
     * This method performs a complete cancellation:
     * <ul>
     * <li>If the notification is <b>scheduled but not yet displayed</b>: unschedules it from AlarmManager</li>
     * <li>If the notification is <b>already displayed</b>: removes it from the notification tray</li>
     * <li>Removes the notification from Pushwoosh's internal database</li>
     * </ul>
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // E-commerce: Cancel cart reminder when user completes purchase
     *   public void onPurchaseComplete() {
     *       SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
     *       int cartReminderId = prefs.getInt("cart_reminder_id", -1);
     *
     *       if (cartReminderId != -1) {
     *           LocalNotificationRequest request = new LocalNotificationRequest(cartReminderId);
     *           request.cancel(); // Remove scheduled cart reminder
     *
     *           // Clean up stored ID
     *           prefs.edit().remove("cart_reminder_id").apply();
     *           Log.d("App", "Cart reminder cancelled after purchase");
     *       }
     *   }
     *
     *   // Fitness app: Cancel workout reminder if user completes workout early
     *   public void onWorkoutComplete() {
     *       if (scheduledWorkoutRequest != null) {
     *           scheduledWorkoutRequest.cancel();
     *           scheduledWorkoutRequest = null;
     *           Toast.makeText(this, "Workout reminder cancelled", Toast.LENGTH_SHORT).show();
     *       }
     *   }
     *
     *   // News app: Cancel notification when user reads article
     *   public void onArticleRead(int articleId) {
     *       String requestKey = "article_" + articleId + "_notification";
     *       int requestId = prefs.getInt(requestKey, -1);
     *
     *       if (requestId != -1) {
     *           new LocalNotificationRequest(requestId).cancel();
     *           prefs.edit().remove(requestKey).apply();
     *       }
     *   }
     *
     *   // Cancel all reminders when user logs out
     *   public void onUserLogout() {
     *       List<Integer> requestIds = database.getAllNotificationRequestIds();
     *       for (int requestId : requestIds) {
     *           new LocalNotificationRequest(requestId).cancel();
     *       }
     *       database.clearAllNotificationRequestIds();
     *   }
     * }
     * </pre>
     * <p>
     * <b>Important Notes:</b>
     * <ul>
     * <li>This method is idempotent - safe to call multiple times for the same request</li>
     * <li>If notification was already dismissed by user, this cleans up internal state</li>
     * <li>If request ID is invalid or notification doesn't exist, method silently succeeds</li>
     * </ul>
     *
     * @see #unschedule()
     */
    public void cancel() {
        unschedule();

        DbLocalNotification dbLocalNotification = RepositoryModule.getLocalNotificationStorage().getLocalNotificationShown(requestId);
        if (dbLocalNotification != null) {
            int notificationId = dbLocalNotification.getNotificationId();
            String notificationTag = dbLocalNotification.getNotificationTag();
            android.app.NotificationManager manager = AndroidPlatformModule.getManagerProvider().getNotificationManager();

            if (manager == null) {
                return;
            }

            manager.cancel(notificationTag, notificationId);
        }
    }

    /**
     * Unschedules the local notification, preventing it from being displayed.
     * <p>
     * This method only cancels the scheduled notification from Android's AlarmManager.
     * Unlike {@link #cancel()}, it does <b>not</b> remove notifications that have already
     * been displayed from the notification tray.
     * <p>
     * <b>Use Cases:</b>
     * <ul>
     * <li>You want to prevent a scheduled notification from appearing</li>
     * <li>You want to reschedule a notification with different content</li>
     * <li>You don't care about notifications that are already in the notification tray</li>
     * </ul>
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Reschedule a notification with updated content
     *   public void updateReminder(int oldRequestId, String newMessage, int newDelay) {
     *       // Unschedule old notification
     *       LocalNotificationRequest oldRequest = new LocalNotificationRequest(oldRequestId);
     *       oldRequest.unschedule();
     *
     *       // Schedule new notification with updated content
     *       LocalNotification newNotification = new LocalNotification.Builder()
     *           .setMessage(newMessage)
     *           .setDelay(newDelay)
     *           .build();
     *
     *       LocalNotificationRequest newRequest = Pushwoosh.getInstance()
     *           .scheduleLocalNotification(newNotification);
     *
     *       // Store new request ID
     *       prefs.edit().putInt("reminder_id", newRequest.getRequestId()).apply();
     *   }
     *
     *   // Cancel scheduled notification but leave displayed ones
     *   public void stopFutureReminders() {
     *       int requestId = prefs.getInt("future_reminder_id", -1);
     *       if (requestId != -1) {
     *           new LocalNotificationRequest(requestId).unschedule();
     *           Log.d("App", "Future reminders stopped");
     *       }
     *   }
     * }
     * </pre>
     * <p>
     * <b>Comparison with {@link #cancel()}:</b>
     * <table border="1">
     * <tr>
     * <th>Method</th>
     * <th>Unschedules Future Notification</th>
     * <th>Removes Displayed Notification</th>
     * </tr>
     * <tr>
     * <td>{@link #unschedule()}</td>
     * <td>✓ Yes</td>
     * <td>✗ No</td>
     * </tr>
     * <tr>
     * <td>{@link #cancel()}</td>
     * <td>✓ Yes</td>
     * <td>✓ Yes</td>
     * </tr>
     * </table>
     *
     * @see #cancel()
     * @see com.pushwoosh.Pushwoosh#scheduleLocalNotification(LocalNotification)
     */
    public void unschedule() {
        LocalNotificationReceiver.cancelNotification(requestId);
    }
}
