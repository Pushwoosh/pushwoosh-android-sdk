package com.pushwoosh.featureflags;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.exception.PostEventException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.PushwooshInAppImpl;
import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.event.DeviceDataRemovedEvent;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.Subscription;
import com.pushwoosh.internal.event.UserIdUpdatedEvent;
import com.pushwoosh.internal.network.ConnectionException;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.ApplicationOpenDetector;
import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.TimeProvider;
import com.pushwoosh.notification.event.RegistrationSuccessEvent;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.tags.TagsBundle;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Owns the feature flags state: cache, refresh throttle, in-flight request, triggers, listeners and impressions.
 */
public final class FeatureFlagsManager {
    private static final String TAG = "FeatureFlagsManager";
    static final long THROTTLE_MS = TimeUnit.MINUTES.toMillis(5);
    static final long MAX_CACHE_AGE_MS = TimeUnit.HOURS.toMillis(24);
    static final long SESSION_GAP_MS = TimeUnit.MINUTES.toMillis(30);
    static final long PERIODIC_REFRESH_MS = TimeUnit.MINUTES.toMillis(60);
    static final String IMPRESSION_EVENT = "PW_FeatureFlagImpression";
    private static final PushwooshFeatureFlags PUBLIC_API =
            new PushwooshFeatureFlags(FeatureFlagsManager::platformManager);
    private static final AtomicReference<FeatureFlagsManager> STARTED = new AtomicReference<>();

    interface EventPoster {
        void post(
                @NonNull String event,
                @NonNull TagsBundle attributes,
                @NonNull Callback<Void, PostEventException> callback);
    }

    private final RegistrationPrefs registrationPrefs;
    private final RequestManager requestManager;
    private final FeatureFlagsStorage storage;
    private final EventPoster eventPoster;
    private final TimeProvider time;
    private final boolean throttleDisabled;
    private final List<FeatureFlagsUpdateListener> listeners = new CopyOnWriteArrayList<>();
    private final List<Subscription<?>> subscriptions = new CopyOnWriteArrayList<>();
    private final Object lock = new Object();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable periodicRefresh = new Runnable() {
        @Override
        public void run() {
            if (stopped) {
                return;
            }
            long due = untilPeriodicRefreshDue();
            if (due > 0) {
                mainHandler.postDelayed(this, due);
                return;
            }
            requestRefresh(null, false);
            mainHandler.postDelayed(this, PERIODIC_REFRESH_MS);
        }
    };

    private volatile boolean stopped;
    private Map<String, FeatureFlag> flags = Collections.emptyMap();
    private String etag = "";
    private long fetchedAt;

    @Nullable private String boundUserId;

    private boolean persisted;
    private boolean inFlight;
    private List<Callback<Void, FeatureFlagsException>> waiting = new ArrayList<>();
    private boolean followUpQueued;
    private List<Callback<Void, FeatureFlagsException>> followUpWaiting = new ArrayList<>();
    private boolean lastSentUnregistered;
    private int cacheGeneration;
    private boolean sessionStarted;
    private long backgroundedAt = -1;
    private volatile ImpressionLog impressions = new ImpressionLog();

    FeatureFlagsManager(
            @NonNull RegistrationPrefs registrationPrefs,
            @NonNull RequestManager requestManager,
            @NonNull FeatureFlagsStorage storage,
            @NonNull EventPoster eventPoster,
            @NonNull TimeProvider time,
            boolean throttleDisabled) {
        this.registrationPrefs = registrationPrefs;
        this.requestManager = requestManager;
        this.storage = storage;
        this.eventPoster = eventPoster;
        this.time = time;
        this.throttleDisabled = throttleDisabled;
        loadCache();
    }

    @NonNull public static FeatureFlagsManager create(
            @NonNull Config config,
            @NonNull RegistrationPrefs registrationPrefs,
            @NonNull RequestManager requestManager,
            @NonNull PushwooshInAppImpl inApp) {
        return new FeatureFlagsManager(
                registrationPrefs,
                requestManager,
                new FeatureFlagsStorage(
                        AndroidPlatformModule.getPrefsProvider().providePrefs(FeatureFlagsStorage.PREFS_NAME)),
                inApp::postEvent,
                new TimeProvider(),
                config.isFeatureFlagsThrottleDisabled());
    }

    /**
     * @return the process-wide public API; it resolves the manager of the current platform on every call
     */
    @NonNull public static PushwooshFeatureFlags publicApi() {
        return PUBLIC_API;
    }

    @Nullable private static FeatureFlagsManager platformManager() {
        PushwooshPlatform platform = PushwooshPlatform.getInstance();
        if (platform == null) {
            PushwooshPlatform.notifyNotInitialized();
            return null;
        }
        return platform.featureFlags();
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static void resetForTests() {
        FeatureFlagsManager started = STARTED.getAndSet(null);
        if (started != null) {
            started.stop();
        }
    }

    @Nullable FeatureFlag get(@NonNull String key) {
        synchronized (lock) {
            return visibleFlagsLocked().get(key);
        }
    }

    void refresh(@Nullable Callback<Void, FeatureFlagsException> callback) {
        requestRefresh(callback, false);
    }

    void addUpdateListener(@NonNull FeatureFlagsUpdateListener listener) {
        FeatureFlagsManager successor = successor();
        if (successor != null) {
            successor.addUpdateListener(listener);
            return;
        }
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    void removeUpdateListener(@NonNull FeatureFlagsUpdateListener listener) {
        listeners.remove(listener);
        FeatureFlagsManager successor = successor();
        if (successor != null) {
            successor.removeUpdateListener(listener);
        }
    }

    /**
     * Subscribes to the SDK events that drive refreshes. Must run before the first activity starts.
     * <p>
     * Only one manager per process is started: starting this one detaches the previously started manager, takes over
     * its update listeners and its cache (lazy initialization builds the platform twice); a repeated call is a no-op.
     */
    public void start() {
        FeatureFlagsManager previous = STARTED.getAndSet(this);
        if (previous == this) {
            return;
        }
        if (previous != null) {
            previous.stop();
            for (FeatureFlagsUpdateListener listener : previous.listeners) {
                addUpdateListener(listener);
            }
            takeOverCacheOf(previous);
        }
        subscriptions.add(EventBus.subscribe(
                ApplicationOpenDetector.ApplicationMovedToForegroundEvent.class, event -> onForeground()));
        subscriptions.add(EventBus.subscribe(
                ApplicationOpenDetector.ApplicationMovedToBackgroundEvent.class, event -> onBackground()));
        subscriptions.add(EventBus.subscribe(RegistrationSuccessEvent.class, event -> onRegistered()));
        subscriptions.add(EventBus.subscribe(UserIdUpdatedEvent.class, event -> onUserIdUpdated()));
        subscriptions.add(EventBus.subscribe(DeviceDataRemovedEvent.class, event -> onDeviceDataRemoved()));
    }

    private void stop() {
        stopped = true;
        for (Subscription<?> subscription : subscriptions) {
            subscription.unsubscribe();
        }
        subscriptions.clear();
        mainHandler.post(() -> mainHandler.removeCallbacks(periodicRefresh));
    }

    private void takeOverCacheOf(@NonNull FeatureFlagsManager previous) {
        Map<String, FeatureFlag> previousFlags;
        String previousEtag;
        long previousFetchedAt;
        String previousUserId;
        boolean previousPersisted;
        synchronized (previous.lock) {
            previousFlags = previous.flags;
            previousEtag = previous.etag;
            previousFetchedAt = previous.fetchedAt;
            previousUserId = previous.boundUserId;
            previousPersisted = previous.persisted;
        }
        synchronized (lock) {
            flags = previousFlags;
            etag = previousEtag;
            fetchedAt = previousFetchedAt;
            boundUserId = previousUserId;
            persisted = previousPersisted;
        }
        impressions = previous.impressions;
    }

    /**
     * @return the manager that replaced this stopped one, or {@code null} while this one is current
     */
    @Nullable private FeatureFlagsManager successor() {
        if (!stopped) {
            return null;
        }
        FeatureFlagsManager current = STARTED.get();
        return current != this ? current : null;
    }

    private void forwardRefresh(@Nullable Callback<Void, FeatureFlagsException> callback, boolean bypassThrottle) {
        FeatureFlagsManager successor = successor();
        if (successor != null) {
            successor.requestRefresh(callback, bypassThrottle);
            return;
        }
        deliver(
                callback,
                Result.<Void, FeatureFlagsException>fromException(
                        new FeatureFlagsException("Feature flags manager is stopped", 0)));
    }

    void onForeground() {
        if (stopped) {
            return;
        }
        boolean newSession;
        synchronized (lock) {
            long now = time.getCurrentTime();
            boolean renewed = sessionStarted && backgroundedAt >= 0 && now - backgroundedAt >= SESSION_GAP_MS;
            newSession = !sessionStarted || renewed;
            sessionStarted = true;
            backgroundedAt = -1;
            if (renewed) {
                impressions.reset();
            }
        }
        if (newSession) {
            requestRefresh(null, false);
        }
        mainHandler.removeCallbacks(periodicRefresh);
        mainHandler.postDelayed(periodicRefresh, untilPeriodicRefreshDue());
    }

    private long untilPeriodicRefreshDue() {
        long sinceLast = time.getCurrentTime() - storage.lastRequestAt();
        if (sinceLast < 0 || sinceLast >= PERIODIC_REFRESH_MS) {
            return 0;
        }
        return PERIODIC_REFRESH_MS - sinceLast;
    }

    void onBackground() {
        synchronized (lock) {
            backgroundedAt = time.getCurrentTime();
        }
        mainHandler.removeCallbacks(periodicRefresh);
    }

    void onRegistered() {
        synchronized (lock) {
            if (!lastSentUnregistered) {
                return;
            }
            lastSentUnregistered = false;
        }
        requestRefresh(null, true);
    }

    void onUserIdUpdated() {
        boolean userChanged;
        synchronized (lock) {
            userChanged = !effectiveUserId().equals(boundUserId);
            if (userChanged) {
                clearCacheLocked();
                PWLog.debug(TAG, "User changed, feature flags cache cleared");
            } else {
                distrustEtagLocked();
                PWLog.debug(TAG, "User confirmed, feature flags revalidated without etag");
            }
        }
        if (userChanged) {
            impressions.reset();
        }
        requestRefresh(null, true);
    }

    void onDeviceDataRemoved() {
        synchronized (lock) {
            clearCacheLocked();
        }
        impressions.reset();
    }

    void logImpression(@NonNull String key) {
        FeatureFlag flag = get(key);
        if (flag == null) {
            return;
        }
        String dedupKey = key + '\u0000' + flag.getRuleId() + '\u0000' + flag.isEnabled();
        ImpressionLog log = impressions;
        int session = log.begin(dedupKey);
        if (session < 0) {
            return;
        }
        // PW_FF_ assignment tags are written server-side from this event; the SDK never sends them through
        // setTags, so there is nothing to filter on the client.
        eventPoster.post(
                IMPRESSION_EVENT,
                new TagsBundle.Builder()
                        .putString("flag_key", key)
                        .putString("rule_id", flag.getRuleId())
                        .putBoolean("enabled", flag.isEnabled())
                        .build(),
                result -> log.finish(dedupKey, session, result.isSuccess()));
    }

    private void loadCache() {
        FeatureFlagsStorage.Snapshot snapshot = storage.load();
        if (snapshot == null) {
            return;
        }
        try {
            Map<String, FeatureFlag> loaded = FeatureFlagsParser.parse(new JSONObject(snapshot.flagsJson));
            synchronized (lock) {
                flags = loaded;
                etag = snapshot.etag;
                fetchedAt = snapshot.fetchedAt;
                boundUserId = snapshot.userId;
                persisted = true;
            }
        } catch (JSONException e) {
            PWLog.error(TAG, "Cached feature flags are unreadable, dropping them");
            storage.clear();
        }
    }

    private void requestRefresh(@Nullable Callback<Void, FeatureFlagsException> callback, boolean bypassThrottle) {
        SdkStateProvider sdkState = SdkStateProvider.getInstance();
        if (sdkState.getCurrentState() == SdkStateProvider.SdkState.ERROR) {
            deliver(
                    callback,
                    Result.<Void, FeatureFlagsException>fromException(
                            new FeatureFlagsException("SDK initialization failed", 0)));
            return;
        }
        sdkState.executeOrQueue(() -> startOrJoin(callback, bypassThrottle));
    }

    private void startOrJoin(@Nullable Callback<Void, FeatureFlagsException> callback, boolean bypassThrottle) {
        if (stopped) {
            forwardRefresh(callback, bypassThrottle);
            return;
        }
        Runnable send;
        synchronized (lock) {
            if (inFlight) {
                if (bypassThrottle) {
                    followUpQueued = true;
                    addIfNotNull(followUpWaiting, callback);
                } else {
                    addIfNotNull(waiting, callback);
                }
                return;
            }
            if (!bypassThrottle && isThrottledLocked()) {
                PWLog.debug(TAG, "Feature flags refresh throttled");
                deliver(callback, Result.<Void, FeatureFlagsException>fromData(null));
                return;
            }
            addIfNotNull(waiting, callback);
            send = prepareSendLocked();
        }
        runIfNotNull(send);
    }

    private boolean isThrottledLocked() {
        if (throttleDisabled || !hasValidCacheLocked(effectiveUserId())) {
            return false;
        }
        long sinceLast = time.getCurrentTime() - storage.lastRequestAt();
        return sinceLast >= 0 && sinceLast < THROTTLE_MS;
    }

    @Nullable private Runnable prepareSendLocked() {
        String application = registrationPrefs.applicationId().get();
        String hwid = registrationPrefs.hwid().get();
        if (TextUtils.isEmpty(application) || TextUtils.isEmpty(hwid)) {
            failWaitingLocked("Application code or HWID is not set");
            return null;
        }
        String endpoint = FeatureFlagsUrl.endpoint(
                requestManager.getReverseProxyUrl(), registrationPrefs.baseUrl().get(), application);
        if (endpoint == null) {
            failWaitingLocked("Reverse proxy URL does not end with json/1.3/");
            return null;
        }
        inFlight = true;
        final String userId = effectiveUserId();
        final int generation = cacheGeneration;
        String requestEtag = persisted && userId.equals(boundUserId) ? etag : "";
        lastSentUnregistered = !registrationPrefs.registeredOnServer().get();
        storage.saveLastRequestAt(time.getCurrentTime());
        PWLog.debug(TAG, "Feature flags refresh sent" + (requestEtag.isEmpty() ? "" : " with etag"));
        GetFeatureFlagsRequest request = new GetFeatureFlagsRequest(application, hwid, requestEtag);
        return () -> {
            try {
                requestManager.sendRequestToEndpoint(
                        request, endpoint, result -> onResponse(userId, generation, result));
            } catch (RuntimeException e) {
                PWLog.error(TAG, "Feature flags request could not be sent", e);
                onResponse(
                        userId,
                        generation,
                        Result.<FeatureFlagsResponse, NetworkException>fromException(
                                new NetworkException(e.getMessage())));
            }
        };
    }

    private void failWaitingLocked(@NonNull String message) {
        List<Callback<Void, FeatureFlagsException>> failed = waiting;
        waiting = new ArrayList<>();
        Result<Void, FeatureFlagsException> outcome =
                Result.fromException(new FeatureFlagsException("Feature flags refresh skipped: " + message, 0));
        for (Callback<Void, FeatureFlagsException> callback : failed) {
            deliver(callback, outcome);
        }
    }

    private void onResponse(
            @NonNull String requestUserId,
            int requestGeneration,
            @NonNull Result<FeatureFlagsResponse, NetworkException> result) {
        List<Callback<Void, FeatureFlagsException>> done;
        Result<Void, FeatureFlagsException> outcome = null;
        boolean changed = false;
        Runnable followUp = null;
        boolean handedOver;
        synchronized (lock) {
            inFlight = false;
            done = waiting;
            waiting = new ArrayList<>();
            handedOver = stopped;
            if (handedOver) {
                done.addAll(followUpWaiting);
                followUpWaiting = new ArrayList<>();
                followUpQueued = false;
            } else {
                if (!result.isSuccess() || result.getData() == null) {
                    outcome = Result.fromException(toFeatureFlagsException(result.getException()));
                } else {
                    outcome = Result.fromData(null);
                    if (requestGeneration != cacheGeneration) {
                        PWLog.debug(TAG, "Feature flags cache cleared while in flight, response dropped");
                    } else if (requestUserId.equals(effectiveUserId())) {
                        changed = applyLocked(result.getData(), requestUserId);
                    } else {
                        PWLog.debug(TAG, "User changed while feature flags were in flight, response dropped");
                    }
                }
                if (followUpQueued) {
                    followUpQueued = false;
                    waiting = followUpWaiting;
                    followUpWaiting = new ArrayList<>();
                    followUp = prepareSendLocked();
                }
            }
        }
        if (handedOver) {
            forwardRefresh(done.isEmpty() ? null : joined(done), true);
            return;
        }
        for (Callback<Void, FeatureFlagsException> callback : done) {
            deliver(callback, outcome);
        }
        if (changed) {
            notifyListeners();
        }
        runIfNotNull(followUp);
    }

    private boolean applyLocked(@NonNull FeatureFlagsResponse response, @NonNull String userId) {
        long now = time.getCurrentTime();
        Map<String, FeatureFlag> before = visibleFlagsLocked();
        if (response.notModified) {
            if (persisted && userId.equals(boundUserId)) {
                fetchedAt = now;
                storage.saveFetchedAt(now);
            }
            PWLog.debug(TAG, "Feature flags not modified");
        } else if (response.partial) {
            PWLog.warn(TAG, "Feature flags response is partial, not cached");
            if (!persisted || !hasValidCacheLocked(userId)) {
                setCacheLocked(response.flags, "", now, userId, false);
            }
        } else {
            setCacheLocked(response.flags, response.etag, now, userId, true);
            storage.save(response.flagsJson.toString(), response.etag, now, userId);
            PWLog.debug(TAG, "Feature flags updated: " + response.flags.size() + " flags");
        }
        return !before.equals(visibleFlagsLocked());
    }

    private void setCacheLocked(
            @NonNull Map<String, FeatureFlag> newFlags,
            @NonNull String newEtag,
            long newFetchedAt,
            @NonNull String userId,
            boolean isPersisted) {
        flags = newFlags;
        etag = newEtag;
        fetchedAt = newFetchedAt;
        boundUserId = userId;
        persisted = isPersisted;
    }

    private void clearCacheLocked() {
        flags = Collections.emptyMap();
        etag = "";
        fetchedAt = 0L;
        boundUserId = null;
        persisted = false;
        cacheGeneration++;
        storage.clear();
    }

    private void distrustEtagLocked() {
        etag = "";
        cacheGeneration++;
        storage.saveEtag("");
    }

    private boolean hasValidCacheLocked(@NonNull String userId) {
        if (boundUserId == null || !boundUserId.equals(userId)) {
            return false;
        }
        long age = time.getCurrentTime() - fetchedAt;
        return age >= 0 && age <= MAX_CACHE_AGE_MS;
    }

    @NonNull private Map<String, FeatureFlag> visibleFlagsLocked() {
        if (registrationPrefs.removeAllDeviceData().get()) {
            return Collections.emptyMap();
        }
        return hasValidCacheLocked(effectiveUserId()) ? flags : Collections.<String, FeatureFlag>emptyMap();
    }

    @NonNull private String effectiveUserId() {
        String userId = registrationPrefs.userId().get();
        if (!TextUtils.isEmpty(userId)) {
            return userId;
        }
        String hwid = registrationPrefs.hwid().get();
        return hwid != null ? hwid : "";
    }

    @NonNull private static FeatureFlagsException toFeatureFlagsException(@Nullable NetworkException e) {
        int status = e instanceof ConnectionException ? ((ConnectionException) e).getStatusCode() : 0;
        if (status == 200) {
            PWLog.error(TAG, "Feature flags response could not be used");
        } else {
            PWLog.debug(TAG, "Feature flags refresh failed, HTTP " + status);
        }
        return new FeatureFlagsException(e != null ? e.getMessage() : "Feature flags request failed", status);
    }

    private void notifyListeners() {
        BackgroundExecutor.main(() -> {
            for (FeatureFlagsUpdateListener listener : listeners) {
                try {
                    listener.onFeatureFlagsUpdated();
                } catch (Exception e) {
                    PWLog.error(TAG, "Feature flags listener threw", e);
                }
            }
        });
    }

    private static void deliver(
            @Nullable Callback<Void, FeatureFlagsException> callback,
            @NonNull Result<Void, FeatureFlagsException> result) {
        if (callback == null) {
            return;
        }
        BackgroundExecutor.main(() -> callback.process(result));
    }

    @NonNull private static Callback<Void, FeatureFlagsException> joined(
            @NonNull List<Callback<Void, FeatureFlagsException>> callbacks) {
        return result -> {
            for (Callback<Void, FeatureFlagsException> callback : callbacks) {
                callback.process(result);
            }
        };
    }

    /**
     * Impression dedup state of one session: a key is consumed only once its post succeeded, and a key whose post is
     * in flight is not posted again. Shared with the manager that takes over after lazy initialization.
     */
    private static final class ImpressionLog {
        private final Set<String> sent = new HashSet<>();
        private final Set<String> inFlight = new HashSet<>();
        private int session;

        /**
         * @return the session the post belongs to, or {@code -1} if the key is already sent or being sent
         */
        synchronized int begin(@NonNull String key) {
            if (sent.contains(key) || !inFlight.add(key)) {
                return -1;
            }
            return session;
        }

        synchronized void finish(@NonNull String key, int postSession, boolean success) {
            if (postSession != session) {
                return;
            }
            inFlight.remove(key);
            if (success) {
                sent.add(key);
            }
        }

        synchronized void reset() {
            sent.clear();
            inFlight.clear();
            session++;
        }
    }

    private static void runIfNotNull(@Nullable Runnable task) {
        if (task != null) {
            task.run();
        }
    }

    private static void addIfNotNull(
            @NonNull List<Callback<Void, FeatureFlagsException>> list,
            @Nullable Callback<Void, FeatureFlagsException> callback) {
        if (callback != null) {
            list.add(callback);
        }
    }
}
