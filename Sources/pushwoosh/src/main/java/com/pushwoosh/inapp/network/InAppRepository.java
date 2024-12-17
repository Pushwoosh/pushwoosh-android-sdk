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
import com.pushwoosh.inapp.businesscases.BusinessCasesManager;
import com.pushwoosh.inapp.event.InAppEvent;
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
import com.pushwoosh.internal.event.Subscription;
import com.pushwoosh.internal.event.UserIdUpdatedEvent;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.preference.PreferenceStringValue;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.tags.TagsBundle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class InAppRepository {
    private static final String TAG = "[InApp]InAppRepository";
    //Time while wait required inApp
    private static final int REQUIRED_TIMEOUT_SECONDS = 5;

    @Nullable
    private RequestManager requestManager;
    private final InAppStorage inAppStorage;
    private final InAppDownloader inAppDownloader;
    private final InAppDeployedChecker inAppDeployedChecker;
    private final ResourceMapper resourceMapper;
    private final AtomicBoolean inAppLoaded = new AtomicBoolean(false);
    private final RegistrationPrefs registrationPrefs;


    public InAppRepository(@Nullable RequestManager requestManager,
                           InAppStorage inAppStorage,
                           InAppDownloader inAppDownloader,
                           ResourceMapper resourceMapper,
                           InAppFolderProvider inAppFolderProvider,
                           RegistrationPrefs registrationPrefs) {

        this.requestManager = requestManager;
        this.inAppStorage = inAppStorage;
        this.inAppDownloader = inAppDownloader;
        this.resourceMapper = resourceMapper;
        this.registrationPrefs = registrationPrefs;

        inAppDeployedChecker = new InAppDeployedChecker(inAppStorage, inAppFolderProvider);
        EventBus.subscribe(InAppViewEvent.class, (event) -> {
            PreferenceStringValue preferenceValue = RepositoryModule.getNotificationPreferences().messageHash();
            String msgHash = preferenceValue.get();

            TriggerInAppActionRequest request = new TriggerInAppActionRequest(event.getResource().getCode(), msgHash, event.getResource().getCode());
            requestManager.sendRequest(request);

            RepositoryModule.getNotificationPreferences().messageHash().set(null);
        });
    }

    @SuppressWarnings("UnusedReturnValue")
    @WorkerThread
    public Result<Void, NetworkException> loadInApps() {
        List<Resource> data = null;
        try {
            GetInAppsRequest request = new GetInAppsRequest();

            if (!updateRequestManagerIfNeeded() || requestManager == null) {
                return Result.fromException(new NetworkException("Request Manager is null"));
            }

            Result<List<Resource>, NetworkException> getInAppsResult = requestManager.sendRequestSync(request);

            data = getInAppsResult.getData();
            if (!getInAppsResult.isSuccess()) {
                return Result.fromException(getInAppsResult.getException());
            }

            if (data == null || data.isEmpty()) {
                return Result.fromData(null);
            }

            List<String> updateResource = new ArrayList<>();
            updateResource.addAll(inAppStorage.saveOrUpdateResources(data));

            BusinessCasesManager.processInAppsData(data);


            for (String code : updateResource) {
                inAppDownloader.removeResourceFiles(code);
            }
            checkEnableGDPR(data);

            downloadOrUpdate(data);
            return Result.fromData(null);
        } finally {
            inAppLoaded.set(true);
        }
    }


    private void checkEnableGDPR(List<Resource> resourceList) {
        boolean result = false;
        for (Resource resource : resourceList) {
            String gdpr = resource.getGdpr();
            if (gdpr != null && !gdpr.isEmpty()) {
                result = true;
                break;
            }
        }
        registrationPrefs.gdprEnable().set(result);
    }

    private boolean updateRequestManagerIfNeeded() {
        if (requestManager == null) {
            requestManager = NetworkModule.getRequestManager();

            if (requestManager == null) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("UnusedReturnValue")
    @WorkerThread
    private DownloadResult downloadOrUpdate(List<Resource> inapps) {
        List<Resource> needDeploy = new ArrayList<>();

        for (Resource resource : inapps) {
            if (!inAppDeployedChecker.check(resource)) {
                needDeploy.add(resource);
            }
        }

        if (needDeploy.isEmpty()) {
            return DownloadResult.empty();
        }

        return inAppDownloader.downloadAndDeploy(needDeploy);
    }

    private boolean downloadIfNeeded(Resource resource) {
        if (!inAppDeployedChecker.check(resource)) {
            if (inAppDownloader.isDownloading(resource)) {
                return waitUntilDeploying(resource);
            } else {
                DownloadResult downloadResult = inAppDownloader.downloadAndDeploy(Collections.singletonList(resource));
                return !downloadResult.getSuccess().isEmpty();
            }
        }

        return true;
    }

    //if resource downloading now wait until it deploying or not
    private boolean waitUntilDeploying(Resource resource) {
        CountDownLatch latch = new CountDownLatch(1);

        final InAppEvent.EventType[] eventType = {InAppEvent.EventType.DEPLOY_FAILED};
        Subscription<InAppEvent> subscribe = EventBus.subscribe(InAppEvent.class, event -> {
            if (event == null || (!event.getType().equals(InAppEvent.EventType.DEPLOY_FAILED) && !event.getType().equals(InAppEvent.EventType.DEPLOYED))) {
                return;
            }

            if (event.getCode().equals(resource.getCode())) {
                eventType[0] = event.getType();
                latch.countDown();
            }
        });

        try {
            latch.await();
            subscribe.unsubscribe();
            return eventType[0].equals(InAppEvent.EventType.DEPLOYED);
        } catch (InterruptedException e) {
            PWLog.error("Deploy interrupted", e);
            return false;
        }
    }

    public void setUserId(String userId) {
        setUserId(userId, null);
    }

    public void setUserId(String userId, Callback<Boolean, SetUserIdException> callback) {
        RegisterUserRequest request = new RegisterUserRequest(userId);
        if (!updateRequestManagerIfNeeded() || requestManager == null) {
            return;
        }

        requestManager.sendRequest(request, result -> {
            if (callback == null) {
                return;
            }
            if (result.isSuccess()) {
                EventBus.sendEvent(new UserIdUpdatedEvent());
                callback.process(Result.fromData(true));
            } else {
                String errorMessage = getRegisterUserErrorMessage(result);
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
                    callback.process(Result.fromException(new SetUserException(errorMessage)));
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
                String userId = RepositoryModule.getRegistrationPreferences().userId().get();
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
            }
        });
    }

    private String getRegisterEmailUserErrorMessage(Result result) {
        return getResultErrorMessage(result, "an error occurred during /registerEmailUser request");
    }


    private void registerEmail(@NonNull String email, @NonNull Callback<Boolean, PushwooshException> callback) {
        RegisterEmailRequest request = new RegisterEmailRequest(email);
        if (!updateRequestManagerIfNeeded() || requestManager == null) {
            return;
        }
        requestManager.sendRequest(request, result -> {
            if (result.isSuccess()) {
                callback.process(Result.fromData(true));
            } else {
                callback.process(Result.fromException(result.getException()));
            }
        });
    }

    private void registerEmailUser(@NonNull String email, String userId, @NonNull Callback<Boolean, PushwooshException> callback) {
        RegisterEmailUserRequest request = new RegisterEmailUserRequest(userId, email);
        if (!updateRequestManagerIfNeeded() || requestManager == null) {
            return;
        }
        requestManager.sendRequest(request, result -> {
            if (result.isSuccess()) {
                callback.process(Result.fromData(true));
            } else {
                callback.process(Result.fromException(result.getException()));
            }
        });
    }

    public void richMediaAction(String richmediaCode, String inappCode, String messageHash, String actionAttributes, int actionType, Callback<Void, RichMediaActionException> callback) {
        RichMediaActionRequest request = new RichMediaActionRequest(richmediaCode, inappCode, messageHash, actionAttributes, actionType);
        if (!updateRequestManagerIfNeeded() || requestManager == null) {
            if (callback != null) {
                callback.process(Result.fromException(new RichMediaActionException("Request Manager is null")));
            }
            return;
        }
        requestManager.sendRequest(request, result -> {
            if (callback == null) {
                return;
            }

            if (result.isSuccess()) {
                callback.process(Result.fromData(result.getData()));
            } else {
                if (result.getException() != null) {
                    callback.process(Result.fromException(new RichMediaActionException(result.getException().getMessage())));
                    PWLog.warn(TAG, result.getException().getMessage(), result.getException());
                }
            }
        });
    }
    public void postEvent(String event, TagsBundle attributes, @Nullable Callback<Resource, PostEventException> callback) {
        String currentSessionHash = PushwooshPlatform.getInstance().pushwooshRepository().getCurrentSessionHash();

        PostEventRequest request = new PostEventRequest(event, currentSessionHash, attributes);
        if (!updateRequestManagerIfNeeded() || requestManager == null) {
            if (callback != null) {
                callback.process(Result.fromException(new PostEventException("Request Manager is null")));
            }
            return;
        }
        requestManager.sendRequest(request, result -> {
            if (callback == null) {
                return;
            }

            PostEventResponse data = result.getData();
            if (data != null) {
                if (data.getResource() != null || !data.isRequired()) {
                    callback.process(Result.fromData(data.getResource()));
                } else {
                    callback.process(Result.fromData(new Resource(data.getCode(), data.isRequired())));
                }
            } else {
                final NetworkException exception = result.getException();

                if (exception == null) {
                    return;
                }
                callback.process(Result.fromException(new PostEventException(exception.getMessage())));
                PWLog.warn(TAG, exception.getMessage(), exception);
            }
        });
    }

    public void mergeUserId(String oldUserId, String newUserId, boolean doMerge, @Nullable Callback<Void, MergeUserException> callback) {
        MergeUserRequest request = new MergeUserRequest(oldUserId, newUserId, doMerge);
        if (!updateRequestManagerIfNeeded() || requestManager == null) {
            if (callback != null) {
                callback.process(Result.fromException(new MergeUserException("Request Manager is null")));
            }
            return;
        }

        requestManager.sendRequest(request, result -> {
            if (callback != null) {
                if (result.isSuccess()) {
                    callback.process(Result.fromData(null));
                } else {
                    if (result.getException() != null) {
                        callback.process(Result.fromException(new MergeUserException(result.getException().getMessage())));
                    }
                }
            }
        });
    }

    @SuppressWarnings("UnusedReturnValue")
    @WorkerThread
    public Result<Resource, ResourceParseException> prefetchRichMedia(String richMedia) {
        try {
            Resource inapp = Resource.parseRichMedia(richMedia);
            boolean downloaded = downloadIfNeeded(inapp);

            if (!downloaded) {
                return Result.fromException(new ResourceParseException("Can't download or update richMedia: " + inapp.getCode()));
            }

            return Result.fromData(inapp);
        } catch (ResourceParseException e) {
            return Result.fromException(e);
        }
    }

    @WorkerThread
    public Result<HtmlData, ResourceParseException> mapToHtmlData(Resource inapp) {
        PWLog.noise("mapToHtmlData for resource " + inapp.getCode() + " inApp is required: " + inapp.isRequired() + " inAppLoaded: " + inAppLoaded.get());
        if (inapp.isNotDownload()) {
            try {
                if (inAppLoaded.get() || (inapp.isRequired() && waitUntilObtainInApps())) {
                    Resource resource = inAppStorage.getResource(inapp.getCode());
                    if (resource != null) {
                        inapp = resource;
                    } else {
                        return Result.fromException(new ResourceParseException(String.format("Rich media with code %s does not exist.", inapp.getCode())));
                    }
                }
            } catch (Exception e) {
                return Result.fromException(new ResourceParseException(String.format("Can't download or update richMedia: %s", inapp.getCode()), e));
            }
        }

        if (!inAppDeployedChecker.check(inapp)) {
            boolean downloaded = downloadIfNeeded(inapp);
            if (!downloaded) {
                return Result.fromException(new ResourceParseException("Can't download or update richMedia: " + inapp.getCode()));
            }
        }

        try {
            return Result.fromData(resourceMapper.map(inapp));
        } catch (IOException e) {
            return Result.fromException(new ResourceParseException(String.format("Can't mapping resource %s to htmlData", inapp.getCode()), e));
        }
    }

    private boolean waitUntilObtainInApps() throws Exception {
        PWLog.noise("Wait until getInApps finished");
        int waitCounter = 0;
        while (!inAppLoaded.get() && waitCounter < REQUIRED_TIMEOUT_SECONDS * 5) {
            Thread.sleep(200);
            waitCounter++;
        }
        if (!inAppLoaded.get()) {
            throw new TimeoutException("InApp wait timeout");
        }
        return true;
    }

    public Resource getGDPRConsentInAppResource() {
        return inAppStorage.getResourceGDPRConsent();
    }

    public Resource getGDPRDeletionInApp() {
        return inAppStorage.getResourceGDPRDeletion();
    }

    private String getResultErrorMessage(Result result, String defaultErrorMessage) {
        return result.getException() == null || TextUtils.isEmpty(result.getException().getMessage())
                ? defaultErrorMessage
                : result.getException().getMessage();
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
