package com.pushwoosh;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;

import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushBundleDataProvider;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for scheduling WorkManager tasks to handle push notification statistics delivery.
 * <p>
 * This scheduler provides a reliable mechanism for queuing push notification statistics events
 * using Android's WorkManager framework. It addresses critical issues with statistics delivery
 * that can occur due to Process Death and Android's Doze Mode by leveraging WorkManager's
 * persistent task queue and system-level optimizations.
 * <p>
 * Delivery events spread their initial delay over a window requested by the push payload itself
 * (see {@link #getDeliveryDelayWindowMillis(Bundle)}): with no window in the payload the delay
 * defaults to 0-2.5 seconds, and a window the payload does carry is capped at 5 minutes. Open
 * events always use the fixed 0-2.5 second jitter, regardless of what the push payload carries,
 * to balance server load distribution with Android's aggressive power management: this prevents
 * devices from entering deep sleep modes that would delay or block statistics delivery.
 * <p>
 * The window bounds the delay handed to WorkManager, not the moment the request leaves the
 * device: while the app is in the background the platform fires delayed work through an inexact
 * alarm and may run it later than the window.
 * <p>
 * The scheduler supports two types of statistics events:
 * <ul>
 * <li>Delivery events - scheduled when a push notification is successfully delivered to the device</li>
 * <li>Open events - scheduled when a user opens or clicks on a push notification</li>
 * </ul>
 * <p>
 * Each event type has different retry policies optimized for their importance:
 * <ul>
 * <li>Delivery events use linear backoff with faster retry intervals (10s initial delay)</li>
 * <li>Open events use exponential backoff with slower retry intervals (30s initial delay)</li>
 * </ul>
 * <p>
 * The scheduler integrates with {@link PushBundleDataProvider} to extract push notification
 * metadata and with {@link PushwooshWorkManagerHelper} to manage WorkManager constraints
 * and task scheduling policies.
 *
 * @see PushStatisticsWorker for the actual statistics delivery implementation
 * @see PushBundleDataProvider for push notification data extraction
 * @see PushwooshWorkManagerHelper for WorkManager configuration
 */
public class PushStatisticsScheduler {
    /** Tag used for logging purposes. */
    private static final String TAG = "PushStatisticsScheduler";

    /**
     * Default jitter window in milliseconds, chosen to spread server load while avoiding device sleep.
     * <p>
     * Open events always draw their initial delay from this window. Delivery events use it only
     * when the push payload carries no valid window of its own.
     */
    private static final int DEFAULT_DELAY_WINDOW_MILLIS = 2500;

    /** Push payload key carrying the server-requested delivery statistics delay window, in seconds. */
    private static final String KEY_DELIVERY_DELAY = "pw_delivery_delay";

    /** Upper bound for the delivery delay window requested by the push payload, in seconds. */
    private static final int MAX_DELIVERY_DELAY_SECONDS = 300;

    /**
     * Whole seconds with an optional fraction. ASCII digits are spelled out because {@code \d}
     * matches any Unicode digit on Android, and the payload contract is plain ASCII.
     */
    private static final Pattern WHOLE_SECONDS = Pattern.compile("([0-9]+)(\\.[0-9]*)?");

    /** Longest digit run that always fits a long; a longer one is far above the cap anyway. */
    private static final int MAX_PARSEABLE_DIGITS = 18;

    /** Random instance for generating jitter delays. */
    private static final Random RANDOM = new Random();

    /**
     * Separates the tapped button id from the push hash in a unique work name. A push hash is
     * base62 plus {@code _} and {@code -}, so this character cannot occur in it and the boundary
     * stays unambiguous however the server names its buttons; {@code _} would not, and a collision
     * under {@link ExistingWorkPolicy#KEEP} drops a click silently.
     */
    private static final String BUTTON_ID_SEPARATOR = "|";

    /**
     * Schedules a push notification delivery event for reliable statistics reporting.
     * <p>
     * This method extracts the push hash and metadata from the provided bundle using
     * {@link PushBundleDataProvider} and schedules a {@link PushStatisticsWorker} task
     * to handle the delivery event. The task will be executed by WorkManager with
     * appropriate retry policies and network constraints.
     * <p>
     * Delivery events are considered critical for push notification analytics and use
     * linear backoff retry policy with faster retry intervals to ensure timely delivery.
     * <p>
     * The initial delay is chosen uniformly at random within the window requested by the push
     * payload (see {@link #getDeliveryDelayWindowMillis(Bundle)}), defaulting to 0-2.5 seconds
     * and capped at 5 minutes.
     * <p>
     * If the push hash is missing from the bundle, the method logs a warning and skips
     * scheduling the event, as the hash is required for proper statistics tracking.
     *
     * @param pushBundle the Bundle containing push notification data including hash and metadata
     *                   as provided by Firebase or other push notification services
     */
    public static void scheduleDeliveryEvent(Bundle pushBundle) {
        scheduleDeliveryEvent(pushBundle, 0L);
    }

    /**
     * Schedules a delivery event that carries the moment the SDK received the push.
     *
     * @param pushBundle the Bundle containing push notification data including hash and metadata
     * @param receivedAtMillis receipt time in epoch milliseconds; zero or negative means unknown
     *                         and no receipt time is sent
     * @see #scheduleDeliveryEvent(Bundle)
     */
    public static void scheduleDeliveryEvent(Bundle pushBundle, long receivedAtMillis) {
        PWLog.noise(TAG, "scheduleDeliveryEvent()");
        String hash = PushBundleDataProvider.getPushHash(pushBundle);
        String metadata = PushBundleDataProvider.getPushMetadata(pushBundle);

        if (TextUtils.isEmpty(hash)) {
            PWLog.warn(TAG, "Push hash is null, skipping delivery event");
            return;
        }

        PWLog.debug(TAG, "Scheduling delivery event for hash: " + hash);
        long delayWindowMillis = getDeliveryDelayWindowMillis(pushBundle);
        long delayMillis = getRandomDelayMillis(delayWindowMillis);
        PWLog.debug(
                TAG, "Delivery event delay window: " + delayWindowMillis + "ms, drawn delay: " + delayMillis + "ms");
        Long receivedAtUtcSeconds = receivedAtMillis > 0 ? receivedAtMillis / 1000L : null;
        scheduleStatisticsEvent(
                PushStatisticsWorker.EVENT_DELIVERY, hash, metadata, null, delayMillis, receivedAtUtcSeconds);
    }

    /**
     * Schedules a push notification open event for reliable statistics reporting.
     * <p>
     * This method extracts the push hash and metadata from the provided bundle using
     * {@link PushBundleDataProvider} and schedules a {@link PushStatisticsWorker} task
     * to handle the open event. The task will be executed by WorkManager with
     * appropriate retry policies and network constraints.
     * <p>
     * Open events use exponential backoff retry policy with longer initial delays
     * since they are less time-critical than delivery events but still important
     * for user engagement analytics.
     * <p>
     * If the push hash is missing from the bundle, the method logs a warning and skips
     * scheduling the event, as the hash is required for proper statistics tracking.
     *
     * @param pushBundle the Bundle containing push notification data including hash and metadata
     *                   as provided by Firebase or other push notification services
     */
    public static void scheduleOpenEvent(Bundle pushBundle) {
        PWLog.noise(TAG, "scheduleOpenEvent()");
        String hash = PushBundleDataProvider.getPushHash(pushBundle);
        String metadata = PushBundleDataProvider.getPushMetadata(pushBundle);
        String buttonId = PushBundleDataProvider.getTappedButtonId(pushBundle);

        if (TextUtils.isEmpty(hash)) {
            PWLog.warn(TAG, "Push hash is null, skipping open event");
            return;
        }

        PWLog.debug(TAG, "Scheduling open event for hash: " + hash);
        scheduleStatisticsEvent(PushStatisticsWorker.EVENT_OPEN, hash, metadata, buttonId);
    }

    /**
     * Schedules a statistics event with no tapped button id and the default 0-2.5 second jitter.
     *
     * @see #scheduleStatisticsEvent(String, String, String, String)
     */
    public static void scheduleStatisticsEvent(String eventType, String hash, String metadata) {
        scheduleStatisticsEvent(eventType, hash, metadata, null);
    }

    /**
     * Schedules a statistics event with the default 0-2.5 second jitter as its initial delay.
     *
     * @param buttonId the id of the tapped action button, may be null
     */
    public static void scheduleStatisticsEvent(
            String eventType, String hash, String metadata, @Nullable String buttonId) {
        scheduleStatisticsEvent(
                eventType, hash, metadata, buttonId, getRandomDelayMillis(DEFAULT_DELAY_WINDOW_MILLIS), null);
    }

    /**
     * Schedules a statistics event with WorkManager.
     * <p>
     * This method creates a {@link OneTimeWorkRequest} for the {@link PushStatisticsWorker}
     * with the provided event data, applies appropriate constraints and retry policies,
     * and enqueues the work using {@link PushwooshWorkManagerHelper}.
     * The method generates a work name combining event type, hash.
     * <p>
     * Work constraints and backoff policies are configured based on event type:
     * <ul>
     * <li>Network connectivity requirements are applied via {@link #getStatisticsConstraints()}</li>
     * <li>Retry policies differ between delivery (linear) and open (exponential) events</li>
     * <li>Initial backoff delays are optimized per event type importance</li>
     * </ul>
     *
     * @param eventType the type of statistics event (use {@link PushStatisticsWorker#EVENT_DELIVERY} or {@link PushStatisticsWorker#EVENT_OPEN})
     * @param hash the unique hash identifier of the push notification
     * @param metadata additional metadata associated with the push notification, may be null
     * @param buttonId the id of the tapped action button, may be null
     * @param initialDelayMillis the initial delay before WorkManager runs the task, in milliseconds
     * @param receivedAtUtcSeconds the push receipt time in unix seconds, null when unknown
     */
    private static void scheduleStatisticsEvent(
            String eventType,
            String hash,
            String metadata,
            @Nullable String buttonId,
            long initialDelayMillis,
            @Nullable Long receivedAtUtcSeconds) {
        PWLog.noise(
                TAG,
                "scheduleStatisticsEvent(), eventType: " + eventType + ", hash: " + hash + ", metadata: " + metadata
                        + ", buttonId: " + buttonId);
        try {
            // Clean and concise Data creation using helper method
            Data inputData =
                    PushStatisticsWorker.createInputData(eventType, hash, metadata, buttonId, receivedAtUtcSeconds);

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PushStatisticsWorker.class)
                    .setInputData(inputData)
                    .setConstraints(getStatisticsConstraints())
                    .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                    .setBackoffCriteria(
                            getBackoffPolicy(eventType), getInitialBackoffDelay(eventType), TimeUnit.SECONDS)
                    .build();

            // The button id is part of the work name so a queued open for the same hash cannot swallow the click.
            String uniqueWorkName = TextUtils.isEmpty(buttonId)
                    ? eventType + "_" + hash
                    : eventType + "_" + hash + BUTTON_ID_SEPARATOR + buttonId;

            PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(request, uniqueWorkName, ExistingWorkPolicy.KEEP);

            PWLog.debug(TAG, "Successfully scheduled " + eventType + " event for hash: " + hash);

        } catch (Exception e) {
            PWLog.error(TAG, "Failed to schedule " + eventType + " event", e);
        }
    }

    /**
     * Retrieves WorkManager constraints for statistics delivery tasks.
     * <p>
     * These constraints ensure that statistics are only sent when appropriate
     * network conditions are met, helping to preserve battery life and data usage
     * while ensuring reliable delivery.
     *
     * @return Constraints object configured for statistics delivery requirements
     */
    private static Constraints getStatisticsConstraints() {
        return PushwooshWorkManagerHelper.getStatisticsConstraints();
    }

    /**
     * Determines the appropriate backoff policy based on event type.
     * <p>
     * Different event types have different criticality levels and thus use
     * different retry strategies:
     * <ul>
     * <li>Delivery events use {@link BackoffPolicy#LINEAR} for consistent, frequent retries</li>
     * <li>Open events use {@link BackoffPolicy#EXPONENTIAL} for progressively longer delays</li>
     * </ul>
     * <p>
     * This differentiation ensures that critical delivery events are retried more
     * aggressively while open events, though important, don't overwhelm the system
     * with excessive retry attempts.
     *
     * @param eventType the type of statistics event (delivery or open)
     * @return the appropriate BackoffPolicy for the event type
     */
    private static BackoffPolicy getBackoffPolicy(String eventType) {
        if (PushStatisticsWorker.EVENT_DELIVERY.equals(eventType)) {
            // Delivery events are more critical - use linear backoff
            return BackoffPolicy.LINEAR;
        } else {
            // Open events can use exponential backoff
            return BackoffPolicy.EXPONENTIAL;
        }
    }

    /**
     * Determines the initial backoff delay based on event type.
     * <p>
     * The initial delay is optimized for each event type's criticality:
     * <ul>
     * <li>Delivery events: 10 seconds - faster retry for critical analytics</li>
     * <li>Open events: 30 seconds - longer delay for less time-critical events</li>
     * </ul>
     * <p>
     * These delays balance the need for timely statistics delivery with system
     * resource conservation and network efficiency.
     *
     * @param eventType the type of statistics event (delivery or open)
     * @return the initial backoff delay in seconds
     */
    private static long getInitialBackoffDelay(String eventType) {
        if (PushStatisticsWorker.EVENT_DELIVERY.equals(eventType)) {
            // Delivery events retry faster - 10 seconds
            return 10;
        } else {
            // Open events retry slower - 30 seconds
            return 30;
        }
    }

    /**
     * Picks a uniformly random delay within a caller-provided window.
     * <p>
     * Spreading the delay prevents all devices from sending statistics requests to the server
     * simultaneously. Callers pass {@link #DEFAULT_DELAY_WINDOW_MILLIS} for the default jitter, kept
     * short so the request goes out before Android puts the app into deep sleep (Doze Mode).
     *
     * @param windowMillis the upper bound of the window, in milliseconds (inclusive)
     * @return a random delay in milliseconds between 0 and {@code windowMillis} (inclusive)
     */
    private static long getRandomDelayMillis(long windowMillis) {
        return (long) (RANDOM.nextDouble() * (windowMillis + 1));
    }

    /**
     * Reads the delivery statistics delay window requested by the push payload.
     * <p>
     * A missing key falls back to the default jitter window ({@link #DEFAULT_DELAY_WINDOW_MILLIS}),
     * and so does a value that is not a whole number of seconds or is less than one second. A
     * value above {@link #MAX_DELIVERY_DELAY_SECONDS} is capped at that bound so a misconfigured
     * or malicious payload cannot hold statistics delivery back indefinitely. A valid window below
     * {@link #DEFAULT_DELAY_WINDOW_MILLIS} is honoured as-is, with no floor at the default.
     *
     * @param pushBundle the Bundle containing push notification data
     * @return the delivery delay window in milliseconds
     */
    static long getDeliveryDelayWindowMillis(Bundle pushBundle) {
        Object value = pushBundle.get(KEY_DELIVERY_DELAY);
        if (value == null) {
            return DEFAULT_DELAY_WINDOW_MILLIS;
        }
        long seconds = getWholeSeconds(value);
        if (seconds < 1) {
            PWLog.warn(TAG, "Ignoring " + KEY_DELIVERY_DELAY + "=" + value + ": not a whole number of seconds >= 1");
            return DEFAULT_DELAY_WINDOW_MILLIS;
        }
        if (seconds > MAX_DELIVERY_DELAY_SECONDS) {
            PWLog.warn(
                    TAG,
                    KEY_DELIVERY_DELAY + "=" + value + " is above the cap, using " + MAX_DELIVERY_DELAY_SECONDS + "s");
            return MAX_DELIVERY_DELAY_SECONDS * 1000L;
        }
        return seconds * 1000L;
    }

    /**
     * Converts a raw {@link #KEY_DELIVERY_DELAY} value to whole seconds, dropping any fraction.
     * <p>
     * FCM delivers the value as a string while locally built or test pushes may carry a numeric
     * value, so both are accepted. A string is trimmed and must be unsigned decimal digits with an
     * optional fraction ({@link #WHOLE_SECONDS}).
     *
     * @param value the raw payload value
     * @return the requested window in whole seconds, or 0 when the value is not such a number
     */
    private static long getWholeSeconds(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (!(value instanceof String)) {
            return 0;
        }
        Matcher matcher = WHOLE_SECONDS.matcher(((String) value).trim());
        if (!matcher.matches()) {
            return 0;
        }
        String digits = matcher.group(1);
        return digits.length() > MAX_PARSEABLE_DIGITS ? Long.MAX_VALUE : Long.parseLong(digits);
    }
}
