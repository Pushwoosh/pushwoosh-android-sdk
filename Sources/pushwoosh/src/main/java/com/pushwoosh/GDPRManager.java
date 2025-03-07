/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh;

import com.pushwoosh.exception.GetTagsException;
import com.pushwoosh.exception.PostEventException;
import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.PushwooshInAppImpl;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.tags.TagsBundle;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GDPRManager {
    public static final String TAG = GDPRManager.class.getSimpleName();
    private static final String GDPR_DELETE = "GDPRDelete";
    private static final String STATUS = "status";
    private static final String GDPR_CONSENT = "GDPRConsent";
    private static final String CHANNEL = "channel";
    private static final String DEVICE_TYPE = "device_type";

    private static final String MESSAGE_ERROR_GDP_NOT_AVAILABLE = "The GDPR solution isn’t available for this account";

    private PushwooshRepository pushwooshRepository;
    private PushwooshNotificationManager notificationManager;
    private PushwooshInAppImpl pushwooshInApp;

    private Set<String> defaultTag = new HashSet<>(Arrays.asList("Application Version", "Language", "Last Application Open", "First Install"));

    public static GDPRManager getInstance() {
        return PushwooshPlatform.getInstance().getGdprManager();
    }


    GDPRManager(PushwooshRepository pushwooshRepository,
                PushwooshNotificationManager notificationManager,
                PushwooshInAppImpl pushwooshInApp) {

        this.pushwooshRepository = pushwooshRepository;
        this.notificationManager = notificationManager;
        this.pushwooshInApp = pushwooshInApp;
    }

    /**
     * Enable/disable all communication with Pushwoosh. Enabled by default.
     */
    public void setCommunicationEnabled(boolean enable, Callback<Void, PushwooshException> callback) {
        if (!isAvailable()) {
            proccessException(callback);
            return;
        }
        TagsBundle tagsBundle = new TagsBundle.Builder()
                .putBoolean(CHANNEL, enable)
                .putInt(DEVICE_TYPE, 3)
                .build();

        pushwooshInApp.postEvent(GDPR_CONSENT, tagsBundle,
                result -> onPostEventGDPRConsent(enable, result, callback), false);
    }

    private void proccessException(Callback<Void, PushwooshException> callback) {
        PWLog.debug(TAG, MESSAGE_ERROR_GDP_NOT_AVAILABLE);
        if (callback == null) {
            return;
        }
        PushwooshException pushwooshException = new PushwooshException(MESSAGE_ERROR_GDP_NOT_AVAILABLE);
        Result<Void, PushwooshException> result = Result.fromException(pushwooshException);
        callback.process(result);
    }

    private void onPostEventGDPRConsent(boolean enable,
                                        Result<Void, PostEventException> result,
                                        Callback<Void, PushwooshException> callback) {
        if (!result.isSuccess()) {
            PWLog.error(TAG, "cant set Communication Enable to " + enable, result.getException());
            if (callback != null) {
                callback.process(Result.fromException(result.getException()));
            }
            return;
        }

        pushwooshRepository.communicationEnabled(enable);
        if (enable) {
            notificationManager.registerForPushNotifications(resultRegister -> onPushRegistrationChangeResult(callback, resultRegister.getException()), true, null);
        } else {
            notificationManager.unregisterForPushNotifications(resultUnregister -> onPushRegistrationChangeResult(callback, resultUnregister.getException()));
        }
    }

    private void onPushRegistrationChangeResult(Callback<Void, PushwooshException> callback, PushwooshException exception) {
        if (callback == null) {
            return;
        }
        if (exception != null) {
            callback.process(Result.fromException(exception));
        } else {
            callback.process(Result.fromData(null));
        }
    }

    /**
     * Removes all device data from Pushwoosh and stops all interactions and communication permanently.
     */
    public void removeAllDeviceData(Callback<Void, PushwooshException> callback) {
        if (!isAvailable()) {
            proccessException(callback);
            return;
        }

        TagsBundle tagsBundle = new TagsBundle.Builder()
                .putBoolean(STATUS, true)
                .putInt(DEVICE_TYPE, 3)
                .build();

        if (pushwooshInApp != null) {
            pushwooshInApp.postEvent(GDPR_DELETE, tagsBundle,
                    result -> onPostEventGDPRDelete(result, callback), false);
        }
    }

    private void onPostEventGDPRDelete(Result<Void, PostEventException> result,
                                       Callback<Void, PushwooshException> callback) {

        if (result.isSuccess()) {
            pushwooshRepository.getTags(resultGetTag -> onGetTags(callback, resultGetTag));
        } else {
            if (callback != null) {
                callback.process(Result.fromException(result.getException()));
            }
            PWLog.error(TAG, "cant remove all device data", result.getException());
        }
    }

    private void onGetTags(Callback<Void, PushwooshException> callback, Result<TagsBundle, GetTagsException> resultGetTag) {
        if (resultGetTag.isSuccess()) {
            TagsBundle tagsBundle = resultGetTag.getData();
            TagsBundle tagBundleEmptyValue = buildEmptyTagsBundle(tagsBundle);
            pushwooshRepository
                    .sendTags(tagBundleEmptyValue, resultSendTag -> onSendTag(resultSendTag, callback));
        } else {
            if (callback != null) {
                callback.process(Result.fromException(resultGetTag.getException()));
            }
        }
    }

    private TagsBundle buildEmptyTagsBundle(TagsBundle tagsBundle) {
        TagsBundle.Builder builder = new TagsBundle.Builder();
        Map<String, Object> tagMap = tagsBundle.getMap();
        for (String tag : tagMap.keySet()) {
            builder.putString(tag, null);
        }
        return builder.build();
    }

    private void onSendTag(Result<Void, PushwooshException> result,
                           Callback<Void, PushwooshException> callback) {
        if (result.isSuccess()) {
            notificationManager.unregisterForPushNotifications(resultUnregister -> {
                onPushRegistrationChangeResult(callback, resultUnregister.getException());
               if(resultUnregister.isSuccess()){
                   pushwooshRepository.removeAllDeviceData();
               }
            });
        } else {
            if (callback != null) {
                callback.process(result);
            }
        }
    }

    /**
     * Indicates availability of the GDPR compliance solution.
     */
    public boolean isDeviceDataRemoved() {
        PWLog.debug(TAG, "isDeviceDataRemoved");
        return pushwooshRepository.isDeviceDataRemoved();
    }

    /**
     * Return flag is enable communication with server
     */
    public boolean isCommunicationEnabled() {
        PWLog.debug(TAG, "isCommunicationEnabled");
        return pushwooshRepository.isCommunicationEnabled();
    }

    /**
     * Return flag is enabled GDPR on server
     */
    public boolean isAvailable() {
        PWLog.debug(TAG, "isAvailable");
        return pushwooshRepository.isGdprEnable();
    }

    /**
     * Show inApp for all device data from Pushwoosh and stops all interactions and communication permanently.
     */
    public void showGDPRDeletionUI() {
        PWLog.debug(TAG, "showGDPRDeletionUI");
        pushwooshInApp.showGDPRDeletionInApp();
    }

    /**
     * Show inApp for change setting Enable/disable all communication with Pushwoosh
     */
    public void showGDPRConsentUI() {
        PWLog.debug(TAG, "showGDPRConsentUI");
        pushwooshInApp.showGDPRConsentInApp();
    }


}
