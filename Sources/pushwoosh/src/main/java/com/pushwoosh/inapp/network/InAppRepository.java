/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.inapp.network;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.exception.MergeUserException;
import com.pushwoosh.exception.PostEventException;
import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.exception.RichMediaActionException;
import com.pushwoosh.exception.SetEmailException;
import com.pushwoosh.exception.SetUserException;
import com.pushwoosh.exception.SetUserIdException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.exception.ResourceParseException;
import com.pushwoosh.inapp.mapper.ResourceMapper;
import com.pushwoosh.inapp.model.HtmlData;
import com.pushwoosh.inapp.network.downloader.DownloadResult;
import com.pushwoosh.inapp.network.downloader.InAppDownloader;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppFolderProvider;
import com.pushwoosh.inapp.storage.InAppStorage;
import com.pushwoosh.inapp.view.InAppViewEvent;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.UserIdUpdatedEvent;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.preference.PreferenceStringValue;
import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.tags.TagsBundle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class InAppRepository {
    private static final String TAG = "[InApp]InAppRepository";

    private final InAppStorage inAppStorage;
    private final InAppDownloader inAppDownloader;
    private final InAppDeployedChecker inAppDeployedChecker;
    private final ResourceMapper resourceMapper;
    private final AtomicBoolean inAppLoaded = new AtomicBoolean(false);

    /**
     * Single-threaded lane for in-app resource work: downloading resources for postEvent
     * responses and (since SDK-957) receive-time rich media prefetch + tags refresh.
     * Contract: postEvent response handling (including showing a code-based in-app already
     * on disk) is the only work whose latency may depend on this queue. Tap-driven shows,
     * notification tap handling and API requests must never wait on it.
     */
    private final ExecutorService io;

    private final ConcurrentHashMap<String, CountDownLatch> inFlightDownloads = new ConcurrentHashMap<>();

    @VisibleForTesting
    static long downloadJoinTimeoutMs = 60_000;

    public InAppRepository(
            InAppStorage inAppStorage,
            InAppDownloader inAppDownloader,
            ResourceMapper resourceMapper,
            InAppFolderProvider inAppFolderProvider) {
        this(inAppStorage, inAppDownloader, resourceMapper, inAppFolderProvider, Executors.newSingleThreadExecutor());
    }

    @VisibleForTesting
    InAppRepository(
            InAppStorage inAppStorage,
            InAppDownloader inAppDownloader,
            ResourceMapper resourceMapper,
            InAppFolderProvider inAppFolderProvider,
            ExecutorService io) {

        this.inAppStorage = inAppStorage;
        this.inAppDownloader = inAppDownloader;
        this.resourceMapper = resourceMapper;
        this.io = io;

        inAppDeployedChecker = new InAppDeployedChecker(inAppStorage, inAppFolderProvider);
        EventBus.subscribe(InAppViewEvent.class, (event) -> {
            PreferenceStringValue preferenceValue =
                    RepositoryModule.getNotificationPreferences().messageHash();
            String slotHash = preferenceValue.get();
            String msgHash = event.hasOwnMessageHash() ? event.getMessageHash() : slotHash;

            PWLog.noise(
                    TAG,
                    String.format(
                            "Sending show analytics for: %s",
                            event.getResource().getCode()));
            TriggerInAppActionRequest request = new TriggerInAppActionRequest(
                    event.getResource().getCode(), msgHash, event.getResource().getCode());
            NetworkModule.getRequestManager().sendRequest(request);

            // Consume only our own hash: a message still waiting to be shown must keep its attribution.
            if (!event.hasOwnMessageHash() || Objects.equals(preferenceValue.get(), msgHash)) {
                RepositoryModule.getNotificationPreferences().messageHash().set(null);
            }
        });
    }

    @SuppressWarnings("UnusedReturnValue")
    @WorkerThread
    public Result<Void, NetworkException> loadInApps() {
        List<Resource> data = null;
        try {
            data = getInAppsList();

            if (data == null || data.isEmpty()) {
                return Result.fromData(null);
            }

            updateInAppStorage(data);
            downloadOrUpdate(data);

            return Result.fromData(null);
        } finally {
            inAppLoaded.set(true);
        }
    }

    @WorkerThread
    private void downloadOrUpdate(List<Resource> inapps) {
        // One code per downloader call on purpose: a show during the batch waits for its own ZIP through
        // the in-flight map, not for the whole batch behind the downloader mutex.
        List<Resource> ordered = new ArrayList<>(inapps);
        Collections.sort(ordered);
        for (Resource resource : ordered) {
            downloadIfNeeded(resource);
        }
    }

    @WorkerThread
    private boolean downloadIfNeeded(Resource resource) {
        String code = resource.getCode();
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch other = inFlightDownloads.putIfAbsent(code, latch);
        if (other != null) {
            return joinInFlightDownload(resource, other);
        }

        boolean success = false;
        try {
            // Checked under the claim, not before it: a leader finishing in between would otherwise
            // leave a second leader to wipe and re-download its files.
            if (inAppDeployedChecker.check(resource)) {
                return true;
            }
            PWLog.noise(TAG, String.format("Starting download for resource: %s", code));
            DownloadResult downloadResult = inAppDownloader.downloadAndDeploy(Collections.singletonList(resource));
            success = !downloadResult.getSuccess().isEmpty();
            // Row lands after the download, and without updateInAppStorage's wipe: written before, it
            // makes the check pass on stale files; the download already wiped them.
            if (success && resource.getUrl() != null) {
                inAppStorage.saveOrUpdateResources(Collections.singletonList(resource));
            }
        } finally {
            // remove before countDown: a woken joiner re-entering must see a fresh row or a live leader
            inFlightDownloads.remove(code, latch);
            latch.countDown();
        }

        if (success) {
            PWLog.info(TAG, String.format("Successfully downloaded resource: %s", code));
        } else {
            PWLog.error(TAG, String.format("Failed to download resource: %s", code));
        }
        return success;
    }

    @WorkerThread
    private boolean joinInFlightDownload(Resource resource, CountDownLatch other) {
        PWLog.noise(TAG, String.format("Joining in-flight download: %s", resource.getCode()));
        boolean released;
        try {
            released = other.await(downloadJoinTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            PWLog.error(TAG, "Deploy interrupted", e);
            return false;
        }
        // Truth comes from check, not from the await flag: the leader may have fetched another ts
        // of this code, and a timed-out joiner has no leader outcome to trust at all.
        boolean deployed = inAppDeployedChecker.check(resource);
        String msg = String.format(
                "Joined in-flight download of %s: deployed=%b, timedOut=%b", resource.getCode(), deployed, !released);
        PWLog.noise(TAG, msg);
        return deployed;
    }

    public void setUserId(String userId) {
        setUserId(userId, null);
    }

    public void setUserId(String userId, Callback<Boolean, SetUserIdException> callback) {
        RegisterUserRequest request = new RegisterUserRequest(userId);
        NetworkModule.getRequestManager().sendRequest(request, result -> {
            if (result.isSuccess()) {
                PWLog.info("User ID \"" + userId + "\" successfully set");
                EventBus.sendEvent(new UserIdUpdatedEvent());

                if (callback != null) {
                    callback.process(Result.fromData(true));
                }
                return;
            }

            String errorMessage = getRegisterUserErrorMessage(result);
            if (callback != null) {
                callback.process(Result.fromException(new SetUserIdException(errorMessage)));
            }
        });
    }

    private String getRegisterUserErrorMessage(Result result) {
        return getResultErrorMessage(result, "an error occurred during /registerUser request");
    }

    public void setUser(String userId, @NonNull List<String> emails, Callback<Boolean, SetUserException> callback) {
        if (!TextUtils.isEmpty(userId)) {
            setUserId(userId, result -> {
                if (result.isSuccess()) {
                    RepositoryModule.getRegistrationPreferences().userId().set(userId);
                    EventBus.sendEvent(new UserIdUpdatedEvent());
                    setEmail(emails, setEmailResult -> {
                        if (callback == null) {
                            return;
                        }
                        if (setEmailResult.isSuccess()) {
                            callback.process(Result.fromData(true));
                        } else if (!setEmailResult.isSuccess()) {
                            String errorMessage = getRegisterEmailErrorMessage(setEmailResult);
                            callback.process(Result.fromException(new SetUserException(errorMessage)));
                        }
                    });
                } else {
                    String errorMessage = getRegisterUserErrorMessage(result);
                    if (callback != null) {
                        callback.process(Result.fromException(new SetUserException(errorMessage)));
                    }
                }
            });
        } else {
            PWLog.warn("userId cannot be empty");
        }
    }

    private String getRegisterEmailErrorMessage(Result result) {
        return getResultErrorMessage(result, "an error occurred during /registerEmail request");
    }

    public void setEmail(@NonNull List<String> emails, Callback<Boolean, SetEmailException> callback) {
        if (emails.isEmpty()) {
            PWLog.warn("emails array list is empty or null");
            return;
        }
        SetEmailListSuccessCallbackCounter counter = new SetEmailListSuccessCallbackCounter(emails.size());
        for (String email : emails) {
            setEmail(email, result -> {
                if (callback == null) {
                    return;
                }
                if (result.isSuccess()) {
                    counter.incrementSuccessCallbacksCount();
                } else {
                    String errorMessage = getSetEmailErrorMessage(result, email);
                    callback.process(Result.fromException(new SetEmailException(errorMessage)));
                }
                if (counter.isAllCallbacksSucceeded()) {
                    callback.process(Result.fromData(true));
                }
            });
        }
    }

    private String getSetEmailErrorMessage(Result result, String email) {
        return getResultErrorMessage(result, "an error occurred during registration of " + email);
    }

    public void setEmail(String email, Callback<Boolean, PushwooshException> callback) {
        registerEmail(email, result -> {
            if (result.isSuccess()) {
                String userId =
                        RepositoryModule.getRegistrationPreferences().userId().get();
                registerEmailUser(email, userId, registerEmailUserResult -> {
                    if (callback == null) {
                        return;
                    }
                    if (registerEmailUserResult.isSuccess()) {
                        callback.process(Result.fromData(true));
                    } else {
                        String errorMessage = getRegisterEmailUserErrorMessage(registerEmailUserResult);
                        callback.process(Result.fromException(new PushwooshException(errorMessage)));
                    }
                });
            } else {
                String errorMessage = getRegisterEmailErrorMessage(result);
                if (callback != null) {
                    callback.process(Result.fromException(new PushwooshException(errorMessage)));
                }
            }
        });
    }

    private String getRegisterEmailUserErrorMessage(Result result) {
        return getResultErrorMessage(result, "an error occurred during /registerEmailUser request");
    }

    private void registerEmail(@NonNull String email, @NonNull Callback<Boolean, PushwooshException> callback) {
        RegisterEmailRequest request = new RegisterEmailRequest(email);
        NetworkModule.getRequestManager().sendRequest(request, result -> {
            if (result.isSuccess()) {
                callback.process(Result.fromData(true));
            } else {
                callback.process(Result.fromException(result.getException()));
            }
        });
    }

    private void registerEmailUser(
            @NonNull String email, String userId, @NonNull Callback<Boolean, PushwooshException> callback) {
        RegisterEmailUserRequest request = new RegisterEmailUserRequest(userId, email);
        NetworkModule.getRequestManager().sendRequest(request, result -> {
            if (result.isSuccess()) {
                callback.process(Result.fromData(true));
            } else {
                callback.process(Result.fromException(result.getException()));
            }
        });
    }

    public void richMediaAction(
            String richmediaCode,
            String inappCode,
            String messageHash,
            String actionAttributes,
            int actionType,
            Callback<Void, RichMediaActionException> callback) {
        RichMediaActionRequest request =
                new RichMediaActionRequest(richmediaCode, inappCode, messageHash, actionAttributes, actionType);
        NetworkModule.getRequestManager().sendRequest(request, result -> {
            if (callback == null) {
                return;
            }

            if (result.isSuccess()) {
                callback.process(Result.fromData(result.getData()));
            } else {
                if (result.getException() != null) {
                    callback.process(Result.fromException(
                            new RichMediaActionException(result.getException().getMessage())));
                    PWLog.warn(TAG, result.getException().getMessage(), result.getException());
                }
            }
        });
    }

    /**
     * Sends event to server. If response contains Rich Media or In-App code, triggers display.
     */
    public void postEvent(
            String event, TagsBundle attributes, @Nullable Callback<Resource, PostEventException> callback) {
        String currentSessionHash =
                PushwooshPlatform.getInstance().pushwooshRepository().getCurrentSessionHash();

        PostEventRequest request = new PostEventRequest(event, currentSessionHash, attributes);
        NetworkModule.getRequestManager().sendRequest(request, result -> {
            handlePostEventResponse(result, callback);
        });
    }

    public void mergeUserId(
            String oldUserId,
            String newUserId,
            boolean doMerge,
            @Nullable Callback<Void, MergeUserException> callback) {
        MergeUserRequest request = new MergeUserRequest(oldUserId, newUserId, doMerge);
        NetworkModule.getRequestManager().sendRequest(request, result -> {
            if (callback != null) {
                if (result.isSuccess()) {
                    callback.process(Result.fromData(null));
                } else {
                    if (result.getException() != null) {
                        callback.process(Result.fromException(
                                new MergeUserException(result.getException().getMessage())));
                    }
                }
            }
        });
    }

    /**
     * Downloads and extracts Rich Media ZIP to local storage.
     */
    @SuppressWarnings("UnusedReturnValue")
    @WorkerThread
    public Result<Resource, ResourceParseException> prefetchRichMedia(String richMedia) {
        PWLog.noise(TAG, String.format("prefetchRichMedia(), rich media: %s", richMedia));
        try {
            Resource resource = Resource.parseRichMedia(richMedia);
            boolean downloaded = downloadIfNeeded(resource);

            if (!downloaded) {
                String msg = "Can't download or update richMedia: " + resource.getCode();
                return Result.fromException(new ResourceParseException(msg));
            }

            return Result.fromData(resource);
        } catch (ResourceParseException e) {
            return Result.fromException(e);
        }
    }

    /**
     * Prefetches push Rich Media ZIP and refreshes the tags snapshot off the caller's thread.
     * Fire-and-forget: runs on {@link #io} (see the field contract), never throws.
     * ZIP first — a tap joins its in-flight download; tags second — they fall back to the
     * prefs snapshot if a show wins the race.
     */
    public void prefetchRichMediaAndTags(String richMedia) {
        io.submit(() -> {
            try {
                prefetchRichMedia(richMedia);
                PushwooshPlatform.getInstance().pushwooshRepository().prefetchTags();
            } catch (Throwable t) {
                // Bare submit would bury this in an unread Future and kill the tags fetch silently.
                PWLog.error(TAG, "Receive-time prefetch failed", t);
            }
        });
    }

    /**
     * Blocking: resolves a code-only Resource to the full one and guarantees the ZIP is
     * downloaded and deployed. Single source of truth for both the HTML path
     * (mapToHtmlData) and the native detect path (ResourceViewStrategyFactory).
     */
    @WorkerThread
    @NonNull public Result<Resource, ResourceParseException> ensureResolvedAndDeployed(Resource inapp) {
        PWLog.noise(
                TAG,
                String.format(
                        "ensureResolvedAndDeployed: code=%s, inAppListReady=%s", inapp.getCode(), inAppLoaded.get()));
        if (inapp.isNotDownload()) {
            try {
                if (inAppLoaded.get()) {
                    Resource resource = inAppStorage.getResource(inapp.getCode());
                    if (resource != null) {
                        inapp = resource;
                    } else {
                        return Result.fromException(new ResourceParseException(
                                String.format("Rich media with code %s does not exist.", inapp.getCode())));
                    }
                }
            } catch (Exception e) {
                return Result.fromException(new ResourceParseException(
                        String.format("Can't download or update richMedia: %s", inapp.getCode()), e));
            }
        }

        if (!inAppDeployedChecker.check(inapp)) {
            boolean downloaded = downloadIfNeeded(inapp);
            if (!downloaded) {
                return Result.fromException(
                        new ResourceParseException("Can't download or update richMedia: " + inapp.getCode()));
            }
        }

        return Result.fromData(inapp);
    }

    @WorkerThread
    public Result<HtmlData, ResourceParseException> mapToHtmlData(Resource inapp) {
        Result<Resource, ResourceParseException> ensured = ensureResolvedAndDeployed(inapp);
        if (!ensured.isSuccess()) {
            return Result.fromException(ensured.getException());
        }

        Resource resolved = ensured.getData();
        try {
            return Result.fromData(resourceMapper.map(resolved));
        } catch (IOException e) {
            return Result.fromException(new ResourceParseException(
                    String.format("Can't mapping resource %s to htmlData", resolved.getCode()), e));
        }
    }

    private String getResultErrorMessage(Result result, String defaultErrorMessage) {
        return result.getException() == null
                        || TextUtils.isEmpty(result.getException().getMessage())
                ? defaultErrorMessage
                : result.getException().getMessage();
    }

    /**
     * Parses postEvent response: richmedia JSON → Rich Media, code → In-App (fetches via getInApps).
     */
    @WorkerThread
    @Nullable private Resource getResourceFromPostEvent(PostEventResponse response) {
        PWLog.noise(TAG, "getResourceFromPostEvent()");

        try {
            String code = response.getCode();
            String richMediaJson = response.getRichMediaJson();

            Resource fromStorage = inAppStorage.getResource(code);
            if (fromStorage != null) {
                PWLog.noise(TAG, String.format("get inapp resource %s from local storage", code));
                return fromStorage;
            }

            if (code != null && !code.isEmpty()) {
                List<Resource> list = this.getInAppsList();

                for (Resource r : list) {
                    if (code.equals(r.getCode())) {
                        updateInAppStorage(Collections.singletonList(r));
                        this.downloadIfNeeded(r);
                        PWLog.noise(TAG, String.format("get inapp resource %s from server", code));
                        return r;
                    }
                }
                PWLog.error(TAG, String.format("can't load inapp resource, code %s not found on server", code));
                return null;
            } else if (richMediaJson != null && !richMediaJson.isEmpty()) {
                PWLog.noise(TAG, String.format("get rich media resource from string: %s", richMediaJson));
                this.prefetchRichMedia(richMediaJson);
                return Resource.parseRichMedia(richMediaJson);
            }
        } catch (Exception e) {
            PWLog.error(TAG, "getResourceFromPostEvent failed", e);
            return null;
        }
        return null;
    }

    @WorkerThread
    List<Resource> getInAppsList() {
        GetInAppsRequest request = new GetInAppsRequest();
        Result<List<Resource>, NetworkException> getInAppsResult =
                NetworkModule.getRequestManager().sendRequestSync(request);
        if (!getInAppsResult.isSuccess()) {
            PWLog.error(TAG, "Failed to get rich media resource: getInApps request failed");
            return Collections.emptyList();
        }

        List<Resource> resultData = getInAppsResult.getData();
        if (resultData == null || resultData.isEmpty()) {
            PWLog.noise(TAG, "GetInApps response has no inapp data");
            return Collections.emptyList();
        }
        return resultData;
    }

    @WorkerThread
    private void updateInAppStorage(List<Resource> data) {
        // Get list of codes for resources that were actually updated
        List<String> updatedResourceCodes = inAppStorage.saveOrUpdateResources(data);

        if (updatedResourceCodes.isEmpty()) {
            return;
        }

        PWLog.info(
                TAG,
                String.format(
                        Locale.US,
                        "Removing old files for %d updated resources: %s",
                        updatedResourceCodes.size(),
                        updatedResourceCodes));

        for (String code : updatedResourceCodes) {
            inAppDownloader.removeResourceFiles(code);
        }
    }

    private void handlePostEventResponse(
            Result<PostEventResponse, NetworkException> result, Callback<Resource, PostEventException> callback) {
        if (callback == null) {
            return;
        }

        if (result.isSuccess()) {
            PostEventResponse data = result.getData();
            if (data != null) {
                // downloading missing resources requires network operation, should be done in
                // worker thread
                io.submit(() -> {
                    Resource postEventResource = getResourceFromPostEvent(result.getData());
                    if (postEventResource != null && !TextUtils.isEmpty(data.getMessageHash())) {
                        PWLog.info(TAG, "Setting messageHash from postEvent response: " + data.getMessageHash());
                        RepositoryModule.getNotificationPreferences()
                                .messageHash()
                                .set(data.getMessageHash());
                    }
                    // presenting rich media is UI operation, should be done in main thread
                    BackgroundExecutor.main(() -> {
                        callback.process(Result.fromData(postEventResource));
                    });
                });
            }
        } else {
            final NetworkException exception = result.getException();

            if (exception == null) {
                return;
            }
            callback.process(Result.fromException(new PostEventException(exception.getMessage())));
            PWLog.warn(TAG, exception.getMessage(), exception);
        }
    }

    private class SetEmailListSuccessCallbackCounter {
        private int emailListSize;
        private int successCallbacksCount;

        public SetEmailListSuccessCallbackCounter(int emailListSize) {
            this.emailListSize = emailListSize;
        }

        public void incrementSuccessCallbacksCount() {
            ++this.successCallbacksCount;
        }

        public boolean isAllCallbacksSucceeded() {
            return successCallbacksCount == emailListSize;
        }
    }
}
