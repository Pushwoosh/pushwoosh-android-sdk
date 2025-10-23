package com.pushwoosh;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.network.ConnectionException;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.PushwooshRepository;

/**
 * WorkManager Worker that handles reliable delivery of push notification statistics events.
 * <p>
 * This worker addresses issues with push notification statistics delivery that can occur
 * due to Process Death and Android's Doze Mode by using WorkManager's persistent task queue.
 * It ensures that critical push delivery and open events are reliably sent to the Pushwoosh
 * servers even when the app process is terminated or the device is in low-power state.
 * <p>
 * The worker supports two types of events:
 * <ul>
 * <li>Delivery events - sent when a push notification is successfully delivered</li>
 * <li>Open events - sent when a user opens/clicks a push notification</li>
 * </ul>
 * <p>
 * The worker uses {@link SdkStateProvider} to handle SDK initialization state management,
 * ensuring that statistics are only sent when the SDK is properly initialized. This prevents
 * issues with sending statistics before the SDK is ready or after it has been shut down.
 *
 * @see PushStatisticsScheduler for scheduling these workers
 * @see SdkStateProvider for SDK state management
 * @see PushwooshRepository for the actual statistics delivery
 */
public class PushStatisticsWorker extends Worker {
    /** Tag used for logging purposes. */
    public static final String TAG = "PushStatisticsWorker";
    
    // Data keys for WorkManager input data
    /** Key for the event type in WorkManager input data. */
    public static final String DATA_EVENT_TYPE = "DATA_EVENT_TYPE";
    /** Key for the push notification hash in WorkManager input data. */
    public static final String DATA_PUSH_HASH = "DATA_PUSH_HASH";
    /** Key for the push notification metadata in WorkManager input data. */
    public static final String DATA_METADATA = "DATA_METADATA";
    
    // Event types supported by this worker
    /** Event type constant for push notification delivery events. */
    public static final String EVENT_DELIVERY = "delivery";
    /** Event type constant for push notification open events. */
    public static final String EVENT_OPEN = "open";

    /** Maximum number of retry attempts for both delivery and open events. */
    private static final int MAX_RETRY_ATTEMPTS = 5;

    /**
     * Creates input data for statistics WorkManager task.
     * <p>
     * This helper method encapsulates the Data.Builder logic and ensures
     * consistent parameter naming across all statistics scheduling. It provides
     * a centralized way to create WorkManager input data for both delivery
     * and open events, making the code more maintainable and less prone to errors.
     *
     * @param eventType the type of statistics event (EVENT_DELIVERY or EVENT_OPEN)
     * @param pushHash the unique hash identifier of the push notification
     * @param metadata additional metadata associated with the push notification, may be null
     * @return Data object ready to be used with WorkManager requests
     */
    public static Data createInputData(String eventType, String pushHash, String metadata) {
        return new Data.Builder()
                .putString(DATA_EVENT_TYPE, eventType)
                .putString(DATA_PUSH_HASH, pushHash)
                .putString(DATA_METADATA, metadata)
                .build();
    }
    
    /**
     * Constructs a new PushStatisticsWorker.
     * <p>
     * This constructor is called by the WorkManager framework when the worker is instantiated
     * to process a queued statistics task.
     *
     * @param context the application context provided by WorkManager
     * @param workerParams the parameters containing input data and other worker configuration
     */
    public PushStatisticsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * Executes the statistics delivery work.
     * <p>
     * This method processes the input data to extract event type, push hash, and metadata,
     * then synchronously sends the appropriate statistics event. The method first checks
     * that the SDK is properly initialized using {@link SdkStateProvider#isReady()}.
     * <p>
     * The method validates required input parameters and returns failure if critical
     * data (event type or push hash) is missing. If the SDK is not ready, it returns
     * retry to allow WorkManager to reschedule the task. For valid input and ready SDK,
     * it executes the statistics request synchronously and returns the actual result.
     * <p>
     * Thread safety: This method is called by WorkManager's background thread and
     * executes the statistics request directly, allowing WorkManager's retry mechanism
     * to work correctly based on the actual HTTP request result.
     *
     * @return {@link Result#success()} if the statistics request completed successfully,
     *         {@link Result#retry()} if the SDK is not ready or the request failed,
     *         {@link Result#failure()} if required input data is missing or event type is unknown
     */
    @NonNull
    @Override
    public Result doWork() {
        PWLog.noise(TAG, String.format("doWork(), %s attempt", getRunAttemptCount()));
        String eventType = getInputData().getString(DATA_EVENT_TYPE);
        String pushHash = getInputData().getString(DATA_PUSH_HASH);
        String metadata = getInputData().getString(DATA_METADATA);

        // Check eventType and pushHash is not empty
        if (TextUtils.isEmpty(eventType) || TextUtils.isEmpty(pushHash)) {
            PWLog.error(TAG, "Cannot send statistics with null eventType or hash");
            return Result.failure();
        }

        // Check retry limit
        if (getRunAttemptCount() >= MAX_RETRY_ATTEMPTS) {
            PWLog.warn(TAG, "Max retry attempts reached for " + eventType + " event, giving up");
            return Result.failure();
        }

        // Check if SDK is ready - if not, let WorkManager retry later
        if (!SdkStateProvider.getInstance().isReady()) {
            return Result.retry();
        }

        // Execute statistics request synchronously with smart retry logic
        try {
            if (!EVENT_DELIVERY.equals(eventType) && !EVENT_OPEN.equals(eventType)) {
                PWLog.warn(TAG, "Unknown event type: " + eventType);
                return Result.failure();
            }

            com.pushwoosh.function.Result<Void, NetworkException> result =
                sendStatisticsEventSync(eventType, pushHash, metadata);

            if (result.isSuccess()) {
                return Result.success();
            }

            NetworkException exception = result.getException();
            if (exception == null) {
                PWLog.error(TAG, String.format("Failed to send %s event due to null exception", eventType));;
                return Result.failure();
            }

            if (shouldRetryException(exception)) {
                PWLog.debug(TAG, String.format("Will retry %s event due to: %s", eventType, exception.getMessage()));
                return Result.retry();
            } else {
                PWLog.warn(TAG, String.format("Failed to send %s event due to: %s", eventType, exception.getMessage()));
                return Result.failure();
            }

        } catch (Throwable e) {
            PWLog.error(TAG, "Failed to send statistics event: " + eventType, e);
            return Result.retry();
        }
    }
    
    /**
     * Sends a push notification statistics event synchronously.
     * <p>
     * This unified method handles both delivery and open events using the synchronous API
     * to send statistics, returning the actual result which allows WorkManager to make
     * informed retry decisions based on the specific error type.
     *
     * @param eventType the type of statistics event (EVENT_DELIVERY or EVENT_OPEN)
     * @param hash the unique hash identifier of the push notification
     * @param metadata additional metadata associated with the push notification, may be null
     * @return Result containing success or NetworkException with error details
     */
    private com.pushwoosh.function.Result<Void, NetworkException> sendStatisticsEventSync(String eventType, String hash, String metadata) {
        PWLog.noise(TAG, "sendStatisticsEventSync(), eventType: " + eventType);
        try {
            PushwooshRepository repository = getPushwooshRepository();
            if (repository == null) {
                PWLog.error(TAG, "Repository is null, " + eventType + " event not sent");
                return com.pushwoosh.function.Result.fromException(new NetworkException("Repository not available"));
            }

            return EVENT_DELIVERY.equals(eventType)
                ? repository.sendPushDeliveredSync(hash, metadata)
                : repository.sendPushOpenedSync(hash, metadata);

        } catch (Exception e) {
            PWLog.error(TAG, "Failed to send " + eventType + " event", e);
            return com.pushwoosh.function.Result.fromException(new NetworkException(e.getMessage()));
        }
    }

    /**
     * Determines whether a network exception should trigger a retry.
     * <p>
     * This method implements smart retry logic adapted from RetriableRequestCallback:
     * - Connection errors (network unavailable): retry with network constraints
     * - Server errors (5xx, 408, 429): retry with backoff
     * - Client errors (4xx): don't retry to save battery and server load
     * - Unknown errors: default to retry (better safe than sorry)
     *
     * @param exception the NetworkException that occurred
     * @return true if the request should be retried, false otherwise
     */
    static boolean shouldRetryException(NetworkException exception) {
        if (!(exception instanceof ConnectionException)) {
            return false; // Only retry ConnectionExceptions
        }

        ConnectionException connEx = (ConnectionException) exception;
        int pushwooshStatus = connEx.getPushwooshStatusCode();
        int networkStatus = connEx.getStatusCode();

        // Connection errors (no network) - let WorkManager retry with network constraints
        if (pushwooshStatus == 0 && networkStatus == 0) {
            return true;
        }

        // Server errors - should retry (same logic as RetriableRequestCallback)
        return isRetriableServerError(networkStatus);
    }

    /**
     * Determines if an HTTP status code represents a server error that should be retried.
     * <p>
     * Based on RetriableRequestCallback.isRetriableStatus(), this method identifies
     * temporary server issues that are likely to resolve on retry:
     * - 408 Request Timeout
     * - 429 Too Many Requests
     * - 5xx Server Errors (500, 502, 503, 504)
     *
     * @param statusCode the HTTP status code
     * @return true if the status code indicates a retriable server error
     */
    private static boolean isRetriableServerError(int statusCode) {
        switch (statusCode) {
            case 408: // Request Timeout
            case 429: // Too Many Requests
            case 500: // Internal Server Error
            case 502: // Bad Gateway
            case 503: // Service Unavailable
            case 504: // Gateway Timeout
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Retrieves the PushwooshRepository instance for sending statistics.
     * <p>
     * This method safely accesses the {@link PushwooshPlatform} singleton and extracts
     * the repository instance. It includes defensive error handling to prevent crashes
     * if the platform is not initialized or is in an invalid state.
     *
     * @return the PushwooshRepository instance, or null if the platform is not available
     *         or not properly initialized
     */
    private PushwooshRepository getPushwooshRepository() {
        try {
            PushwooshPlatform platform = PushwooshPlatform.getInstance();
            return platform != null ? platform.pushwooshRepository() : null;
        } catch (Exception e) {
            PWLog.debug(TAG, "Failed to get repository", e);
            return null;
        }
    }
}
