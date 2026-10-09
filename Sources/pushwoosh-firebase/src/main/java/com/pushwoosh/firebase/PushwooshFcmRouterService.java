package com.pushwoosh.firebase;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.pushwoosh.internal.utils.PWLog;

/**
 * FCM service base class for apps that need their own push routing next to Pushwoosh.
 * <p>
 * Extend this class instead of {@link FirebaseMessagingService} and register your subclass
 * in the manifest as the FCM service. Forwarding to Pushwoosh is built in and cannot be
 * bypassed: both callbacks and the FCM dispatcher {@code handleIntent} are {@code final}.
 * <p>
 * Message dispatch is exclusive. A Pushwoosh message goes to the Pushwoosh SDK and never
 * reaches {@link #onAppMessageReceived}; any other message goes to
 * {@link #onAppMessageReceived} only. To observe Pushwoosh pushes as they arrive, override
 * {@code NotificationServiceExtension#onMessageReceived(PushMessage)}: it fires for every
 * Pushwoosh push whichever FCM service received it, and its return value decides whether the
 * notification is shown. To route Pushwoosh pushes to screens, handle the <b>open</b>, not the
 * receive: use {@code NotificationServiceExtension} and the notification open handler chain.
 * <p>
 * Contract:
 * <ul>
 * <li>on a new token the Pushwoosh SDK is notified first, then {@link #onAppTokenRefresh};</li>
 * <li>both callbacks run on the FCM background executor, like the native
 * {@code onMessageReceived} and {@code onNewToken};</li>
 * <li>an exception thrown by the Pushwoosh SDK on a Pushwoosh message is logged and
 * swallowed; an exception inside {@link #onAppMessageReceived} is the app's own.</li>
 * </ul>
 * Recommended manifest registration — the priority keeps the service winning the resolve
 * even if another library ships its own FCM service:
 * <pre>{@code
 * <service android:name=".YourRouterService" android:exported="false">
 *     <intent-filter android:priority="1">
 *         <action android:name="com.google.firebase.MESSAGING_EVENT" />
 *     </intent-filter>
 * </service>
 * }</pre>
 * Other {@link FirebaseMessagingService} callbacks ({@code onDeletedMessages},
 * {@code onMessageSent}, {@code onSendError}) are not sealed — override them as usual.
 */
public abstract class PushwooshFcmRouterService extends FirebaseMessagingService {
    private static final String TAG = "PushwooshFcmRouterService";

    // handleIntent is @hide in firebase-messaging, yet apps override it by a well-known recipe to catch
    // background notification messages; sealed so a missing super() cannot skip both callbacks at once.
    @Override
    public final void handleIntent(@NonNull Intent intent) {
        super.handleIntent(intent);
    }

    @Override
    @WorkerThread
    public final void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (PushwooshFcmHelper.isPushwooshMessage(remoteMessage)) {
            try {
                PushwooshFcmHelper.onMessageReceived(getApplicationContext(), remoteMessage);
            } catch (Exception e) {
                PWLog.error(TAG, "Failed to handle Pushwoosh message", e);
            }
            return;
        }
        onAppMessageReceived(remoteMessage);
    }

    @Override
    @WorkerThread
    public final void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        try {
            PushwooshFcmHelper.onTokenRefresh(token);
        } catch (Exception e) {
            PWLog.error(TAG, "Failed to handle new token", e);
        }
        onAppTokenRefresh(token);
    }

    /**
     * Called for every FCM message that is not a Pushwoosh message. Runs on the FCM
     * background executor.
     */
    @WorkerThread
    protected abstract void onAppMessageReceived(@NonNull RemoteMessage message);

    /**
     * Called after the new FCM token has been passed to the Pushwoosh SDK. Runs on the
     * FCM background executor. Default implementation does nothing.
     */
    @WorkerThread
    protected void onAppTokenRefresh(@NonNull String token) {}
}
