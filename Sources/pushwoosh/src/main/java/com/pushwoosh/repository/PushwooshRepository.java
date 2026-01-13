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

package com.pushwoosh.repository;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.exception.GetTagsException;
import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.CacheFailedRequestCallback;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.network.InAppRepository;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.network.RequestStorage;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.tags.Tags;
import com.pushwoosh.tags.TagsBundle;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class PushwooshRepository {
    private static final String TAG = "PushwooshRepository";

    private final RequestManager requestManager;
    private final SendTagsProcessor sendTagsProcessor;
    private final RegistrationPrefs registrationPrefs;
    private final NotificationPrefs notificationPrefs;
    private final RequestStorage requestStorage;
    private String currentSessionHash;
    private String currentRichMediaCode;
    private String currentInAppCode;

    public PushwooshRepository(RequestManager requestManager,
                               SendTagsProcessor sendTagsProcessor,
                               RegistrationPrefs registrationPrefs,
                               NotificationPrefs notificationPrefs,
                               RequestStorage requestStorage) {
        this.requestManager = requestManager;
        this.sendTagsProcessor = sendTagsProcessor;
        this.registrationPrefs = registrationPrefs;
        this.notificationPrefs = notificationPrefs;
        this.requestStorage = requestStorage;
    }

    private <T, E extends PushwooshException> void safeProcessCallback(Callback<T, E> callback, Result<T, E> result) {
        if (callback != null) {
            callback.process(result);
        }
    }

    public String getCurrentSessionHash() {
        return currentSessionHash;
    }

    public void setCurrentSessionHash(String currentSessionHash) {
        this.currentSessionHash = currentSessionHash;
    }

    public String getCurrentRichMediaCode() {
        return currentRichMediaCode;
    }

    public void setCurrentRichMediaCode(String currentRichMediaCode) {
        this.currentRichMediaCode = currentRichMediaCode;
    }

    public String getCurrentInAppCode() {
        return currentInAppCode;
    }

    public void setCurrentInAppCode(String currentInAppCode) {
        this.currentInAppCode = currentInAppCode;
    }

    public void sendAppOpen() {
        PWLog.noise(TAG, "sendAppOpen()");
        AppOpenRequest request = new AppOpenRequest();
        requestManager.sendRequest(request);
    }

    public void sendTags(@NonNull TagsBundle tags, Callback<Void, PushwooshException> listener) {
        PWLog.noise(TAG, "sendTags()");
        JSONObject jsonTags = tags.toJson();
        try {
            notificationPrefs.tags().merge(jsonTags);
        } catch (Exception e) {
            PWLog.error(TAG, "Failed to cache tags", e);
        }
        sendTagsProcessor.sendTags(jsonTags, listener);
    }

    public void sendEmailTags(@NonNull TagsBundle tags, String email, Callback<Void, PushwooshException> listener) {
        JSONObject jsonTags = tags.toJson();

        SetEmailTagsRequest request = new SetEmailTagsRequest(jsonTags, email);
        requestManager.sendRequest(request, result -> {
            if (result.isSuccess()) {
                safeProcessCallback(listener, Result.fromData(result.getData()));
            } else {
                safeProcessCallback(listener, Result.fromException(result.getException()));
            }
        });
    }

    public void getTags(@Nullable final Callback<TagsBundle, GetTagsException> callback) {
        GetTagsRequest request = new GetTagsRequest();
        if (requestManager == null) {
            if (callback != null) {
                callback.process(Result.fromException(new GetTagsException("Request Manager is null")));
            }
            return;
        }
        requestManager.sendRequest(request, result -> {
            if (callback != null) {
                if (result.isSuccess()) {
                    TagsBundle tags = result.getData() == null ? Tags.empty() : result.getData();
                    notificationPrefs.tags().set(tags.toJson());
                    callback.process(Result.fromData(tags));
                } else {
                    JSONObject josnTags = notificationPrefs.tags().get();

                    if (josnTags != null) {
                        TagsBundle tags = Tags.fromJson(josnTags);
                        callback.process(Result.fromData(tags));
                    } else {
                        callback.process(Result.fromException(new GetTagsException(result.getException() == null ? "" : result.getException().getMessage())));
                    }
                }
            }
        });
    }

    public void sendInappPurchase(String sku, BigDecimal price, String currency, Date purchaseTime) {
        InAppRepository inAppRepository = InAppModule.getInAppRepository();
        if (inAppRepository == null) {
            return;
        }

        TagsBundle attributes = new TagsBundle.Builder()
                .putString("productIdentifier", sku)
                .putInt("quantity", 1)
                .putString("amount", price.toPlainString())
                .putDate("transactionDate", purchaseTime)
                .putString("currency", currency)
                .putString("status", "success")
                .build();

        inAppRepository.postEvent("PW_InAppPurchase", attributes, result -> {
            if (result.isSuccess()) {
                PWLog.noise(TAG, "In-app purchase data sent successfully");
            } else if (result.getException() != null) {
                PWLog.error(TAG, "Failed to send in-app purchase data", result.getException());
            }
        });
    }

    /**
     * Legacy method for sending push notification open statistics.
     * <p>
     * This method sends push open events using direct HTTP requests, which are not reliable
     * in scenarios involving process death or Android's Doze Mode. The method has been
     * superseded by {@link com.pushwoosh.PushStatisticsScheduler} which provides reliable
     * delivery using WorkManager.
     * <p>
     * <strong>Deprecation Notice:</strong> This method is deprecated and scheduled for removal.
     * It can be safely removed after January 1, 2026. Use
     * {@link com.pushwoosh.PushStatisticsScheduler#scheduleOpenEvent(android.os.Bundle)} or
     * {@link com.pushwoosh.PushStatisticsScheduler#scheduleStatisticsEvent(String, String, String)}
     * instead.
     *
     * @param hash the unique hash identifier of the push notification
     * @param metadata additional metadata associated with the push notification, may be null
     * @deprecated Use {@link com.pushwoosh.PushStatisticsScheduler} for reliable statistics delivery.
     *             Scheduled for removal after January 1, 2026.
     */
    @Deprecated
    public void sendPushOpened(String hash, String metadata) {
        PWLog.info(TAG, "Sending PushStatRequest, hash: " + hash);
        if (hash != null && TextUtils.equals(hash, notificationPrefs.lastNotificationHash().get())) {
            PWLog.warn(TAG,"Push stat for (" + hash + ") already sent");
            return;
        }

        notificationPrefs.lastNotificationHash().set(hash);

        PushStatRequest request = new PushStatRequest(hash, metadata);
        if (requestManager == null) {
            PWLog.error(TAG,"Request manager is null");
            return;
        }
        requestManager.sendRequest(request, new CacheFailedRequestCallback<>(request, requestStorage));
    }

    /**
     * Legacy method for sending push notification delivery statistics.
     * <p>
     * This method sends push delivery events using direct HTTP requests, which are not reliable
     * in scenarios involving process death or Android's Doze Mode. The method has been
     * superseded by {@link com.pushwoosh.PushStatisticsScheduler} which provides reliable
     * delivery using WorkManager.
     * <p>
     * <strong>Deprecation Notice:</strong> This method is deprecated and scheduled for removal.
     * It can be safely removed after January 1, 2026. Use
     * {@link com.pushwoosh.PushStatisticsScheduler#scheduleDeliveryEvent(android.os.Bundle)} or
     * {@link com.pushwoosh.PushStatisticsScheduler#scheduleStatisticsEvent(String, String, String)}
     * instead.
     *
     * @param hash the unique hash identifier of the push notification
     * @param metaData additional metadata associated with the push notification, may be null
     * @deprecated Use {@link com.pushwoosh.PushStatisticsScheduler} for reliable statistics delivery.
     *             Scheduled for removal after January 1, 2026.
     */
    @Deprecated
    public void sendPushDelivered(String hash, String metaData) {
        PWLog.info(TAG,"Sending MessageDeliveredRequest, hash: " + hash);
        MessageDeliveredRequest request = new MessageDeliveredRequest(hash, metaData);
        if (requestManager == null) {
            PWLog.error(TAG, "Request manager is null");
            return;
        }
        requestManager.sendRequest(request, null, new CacheFailedRequestCallback<>(request, requestStorage));
    }

    public Result<Void, NetworkException> sendPushOpenedSync(String hash, String metadata) {
        PWLog.info(TAG, "Sending PushStatRequest sync, hash: " + hash);

        if (hash != null && TextUtils.equals(hash, notificationPrefs.lastNotificationHash().get())) {
            PWLog.warn(TAG,"Push stat for (" + hash + ") already sent");
            return Result.fromData(null); // Already sent - success
        }

        PushStatRequest request = new PushStatRequest(hash, metadata);
        if (requestManager == null) {
            PWLog.error(TAG,"Request manager is null");
            return Result.fromException(new NetworkException("Request manager is null"));
        }

        try {
            Result<Void, NetworkException> result = requestManager.sendRequestSync(request);

            // Only save hash after successful request
            if (result.isSuccess() && hash != null) {
                notificationPrefs.lastNotificationHash().set(hash);
            }

            return result;
        } catch (Exception e) {
            PWLog.error(TAG, "Push stat exception for hash: " + hash, e);
            return Result.fromException(new NetworkException(e.getMessage()));
        }
    }

    public Result<Void, NetworkException> sendPushDeliveredSync(String hash, String metaData) {
        PWLog.info(TAG,"Sending MessageDeliveredRequest sync, hash: " + hash);

        MessageDeliveredRequest request = new MessageDeliveredRequest(hash, metaData);
        if (requestManager == null) {
            PWLog.error(TAG, "Request manager is null");
            return Result.fromException(new NetworkException("Request manager is null"));
        }

        try {
            return requestManager.sendRequestSync(request);
        } catch (Exception e) {
            PWLog.error(TAG, "Message delivered exception for hash: " + hash, e);
            return Result.fromException(new NetworkException(e.getMessage()));
        }
    }

    public void prefetchTags() {
        GetTagsRequest request = new GetTagsRequest();
        if (requestManager == null) {
            return;
        }

        Result<TagsBundle, NetworkException> result = requestManager.sendRequestSync(request);
        if (result.isSuccess() && result.getData() != null) {
            JSONObject jsonTags = result.getData().toJson();
            if (jsonTags.length() > 0) {
                notificationPrefs.tags().set(jsonTags);
            }
        }
    }

    public List<PushMessage> getPushHistory() {
        List<String> pushHistoryStrings = notificationPrefs.pushHistory().get();
        List<PushMessage> result = new ArrayList<>();
        for (String pushString : pushHistoryStrings) {
            Bundle pushBundle = new Bundle();

            try {
                JSONObject object = new JSONObject(pushString);
                Iterator<?> keys = object.keys();

                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    if (object.get(key) instanceof String) {
                        pushBundle.putString(key, object.getString(key));
                    }
                }
                PushMessage data = new PushMessage(pushBundle);
                result.add(data);
            } catch (Exception e) {
                PWLog.exception(e);
            }
        }
        return result;
    }

    public void removeAllDeviceData() {
        notificationPrefs.tags().set(null);
        registrationPrefs.removeAllDeviceData().set(true);
    }

    public boolean isDeviceDataRemoved() {
        return registrationPrefs.removeAllDeviceData().get();
    }

    public void communicationEnabled(boolean enable) {
        registrationPrefs.communicationEnable().set(enable);
    }

    public boolean isCommunicationEnabled(){
        return registrationPrefs.communicationEnable().get();
    }

    public String getHwid() {
        return registrationPrefs.hwid().get();
    }
}
