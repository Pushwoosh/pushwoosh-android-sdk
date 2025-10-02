package com.pushwoosh;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;

import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushBundleDataProvider;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Helper class for scheduling WorkManager tasks to handle push notification statistics delivery.
 * <p>
 * This scheduler provides a reliable mechanism for queuing push notification statistics events
 * using Android's WorkManager framework. It addresses critical issues with statistics delivery
 * that can occur due to Process Death and Android's Doze Mode by leveraging WorkManager's
 * persistent task queue and system-level optimizations.
 * <p>
 * The scheduler uses a short jitter delay (0-2.5 seconds) to balance server load distribution
 * with Android's aggressive power management. This prevents devices from entering deep sleep
 * modes that would delay or block statistics delivery.
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

    /** Maximum initial delay in milliseconds to spread server load while avoiding device sleep. */
    private static final int MAX_INITIAL_DELAY_MILLIS = 2500;

    /** Random instance for generating jitter delays. */
    private static final Random RANDOM = new Random();
    
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
     * If the push hash is missing from the bundle, the method logs a warning and skips
     * scheduling the event, as the hash is required for proper statistics tracking.
     *
     * @param pushBundle the Bundle containing push notification data including hash and metadata
     *                   as provided by Firebase or other push notification services
     */
    public static void scheduleDeliveryEvent(Bundle pushBundle) {
        PWLog.noise(TAG, "scheduleDeliveryEvent()");
        String hash = PushBundleDataProvider.getPushHash(pushBundle);
        String metadata = PushBundleDataProvider.getPushMetadata(pushBundle);
        
        if (TextUtils.isEmpty(hash)) {
            PWLog.warn(TAG, "Push hash is null, skipping delivery event");
            return;
        }
        
        PWLog.debug(TAG, "Scheduling delivery event for hash: " + hash);
        scheduleStatisticsEvent(PushStatisticsWorker.EVENT_DELIVERY, hash, metadata);
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
        
        if (TextUtils.isEmpty(hash)) {
            PWLog.warn(TAG, "Push hash is null, skipping open event");
            return;
        }
        
        PWLog.debug(TAG, "Scheduling open event for hash: " + hash);
        scheduleStatisticsEvent(PushStatisticsWorker.EVENT_OPEN, hash, metadata);
    }
    
    /**
     * Public method for scheduling statistics events with WorkManager.
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
     */
    public static void scheduleStatisticsEvent(String eventType, String hash, String metadata) {
        PWLog.noise(TAG, "scheduleStatisticsEvent(), eventType: " + eventType + ", hash: " + hash + ", metadata: " + metadata);
        try {
            // Clean and concise Data creation using helper method
            Data inputData = PushStatisticsWorker.createInputData(eventType, hash, metadata);
                    
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PushStatisticsWorker.class)
                    .setInputData(inputData)
                    .setConstraints(getStatisticsConstraints())
                    .setInitialDelay(getJitterDelay(), TimeUnit.MILLISECONDS)
                    .setBackoffCriteria(getBackoffPolicy(eventType), getInitialBackoffDelay(eventType), TimeUnit.SECONDS)
                    .build();
                    
            String uniqueWorkName = eventType + "_" + hash;
            
            PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(
                request, 
                uniqueWorkName, 
                ExistingWorkPolicy.KEEP
            );
            
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
     * Generates a random jitter delay to spread server load while avoiding device sleep.
     * <p>
     * This method returns a random delay between 0 and {@link #MAX_INITIAL_DELAY_MILLIS}
     * to prevent all devices from sending statistics requests simultaneously to the server.
     * The delay is kept short (2.5 seconds) to prevent Android from putting the app into
     * deep sleep mode (Doze Mode) which would delay or block network requests.
     * <p>
     * The 2500ms range provides 25 different 100ms intervals for load distribution
     * while ensuring requests are sent before the device enters power saving modes.
     *
     * @return random delay in milliseconds between 0 and MAX_INITIAL_DELAY_MILLIS (inclusive)
     */
    private static int getJitterDelay() {
        return RANDOM.nextInt(MAX_INITIAL_DELAY_MILLIS + 1);
    }
}
