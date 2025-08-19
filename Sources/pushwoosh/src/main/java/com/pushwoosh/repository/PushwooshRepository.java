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
import com.pushwoosh.internal.event.EventListener;
import com.pushwoosh.internal.event.ServerCommunicationStartedEvent;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.network.RequestStorage;
import com.pushwoosh.internal.network.ServerCommunicationManager;
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
    private final ServerCommunicationManager serverCommunicationManager;
    private String currentSessionHash;
    private String currentRichMediaCode;
    private String currentInAppCode;
    private EventListener<ServerCommunicationStartedEvent> sendAppOpenWhenServerCommunicationStartsEvent;

    public PushwooshRepository(RequestManager requestManager,
                               SendTagsProcessor sendTagsProcessor,
                               RegistrationPrefs registrationPrefs,
                               NotificationPrefs notificationPrefs,
                               RequestStorage requestStorage,
                               ServerCommunicationManager serverCommunicationManager) {
        this.requestManager = requestManager;
        this.sendTagsProcessor = sendTagsProcessor;
        this.registrationPrefs = registrationPrefs;
        this.notificationPrefs = notificationPrefs;
        this.requestStorage = requestStorage;
        this.serverCommunicationManager = serverCommunicationManager;

        if (requestManager == null) {
            PWLog.error(TAG, "requestManager can't be null");
            return;
        }

        if (registrationPrefs.setTagsFailed().get()) {
            JSONObject tags = notificationPrefs.tags().get();
            if (tags == null) {
                return;
            }

            PWLog.debug(TAG, "Resending application tags");

            sendTagsProcessor.sendTags(tags, result -> {
                if (result.isSuccess()) {
                    registrationPrefs.setTagsFailed().set(false);
                }
            });
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
//        if (serverCommunicationManager != null && !serverCommunicationManager.isServerCommunicationAllowed()) {
//            subscribeSendAppOpenWhenServerCommunicationStartsEvent();
//            return;
//        }
        PWLog.noise(TAG, "sendAppOpen()");
        AppOpenRequest request = new AppOpenRequest();
        requestManager.sendRequest(
                request,
                new CacheFailedRequestCallback<>(request, requestStorage)
                );

//        BusinessCasesManager businessCasesManager = PushwooshPlatform.getInstance().getBusinessCasesManager();
//        businessCasesManager.triggerCase(BusinessCasesManager.WELCOME_CASE, null);
//        businessCasesManager.triggerCase(BusinessCasesManager.APP_UPDATE_CASE, null);

    }

//    private void subscribeSendAppOpenWhenServerCommunicationStartsEvent() {
//        if (sendAppOpenWhenServerCommunicationStartsEvent != null) {
//            return;
//        }
//        sendAppOpenWhenServerCommunicationStartsEvent = new EventListener<ServerCommunicationStartedEvent>() {
//            @Override
//            public void onReceive(ServerCommunicationStartedEvent event) {
//                EventBus.unsubscribe(ServerCommunicationStartedEvent.class, this);
//                sendAppOpen();
//            }
//        };
//        EventBus.subscribe(ServerCommunicationStartedEvent.class, sendAppOpenWhenServerCommunicationStartsEvent);
//    }

    public void sendTags(@NonNull TagsBundle tags, Callback<Void, PushwooshException> listener) {
        JSONObject jsonTags = tags.toJson();
        try {
            notificationPrefs.tags().merge(jsonTags);
        } catch (Exception e) {
            // cache failure shouldn't affect request
            PWLog.exception(e);
        }
        sendTagsProcessor.sendTags(jsonTags, listener);
    }

    public void sendEmailTags(@NonNull TagsBundle tags, String email, Callback<Void, PushwooshException> listener) {
        JSONObject jsonTags = tags.toJson();

        RequestManager requestManager = NetworkModule.getRequestManager();
        if (requestManager == null) {
            NetworkException exception = new NetworkException("Request manager is null");
            PWLog.warn(TAG, "Cannot send email tags", exception);
            return;
        }

        SetEmailTagsRequest request = new SetEmailTagsRequest(jsonTags, email);
        requestManager.sendRequest(request, new CacheFailedRequestCallback<Void>(request, RepositoryModule.getRequestStorage()) {
            @Override
            public void process(@NonNull Result<Void, NetworkException> result) {
                super.process(result);
                if (result.isSuccess()) {
                    listener.process(Result.fromData(result.getData()));
                } else {
                    listener.process(Result.fromException(result.getException()));
                }
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

    public void sendPushDelivered(String hash, String metaData) {
        PWLog.info(TAG,"Sending MessageDeliveredRequest, hash: " + hash);
        MessageDeliveredRequest request = new MessageDeliveredRequest(hash, metaData);
        if (requestManager == null) {
            PWLog.error(TAG, "Request manager is null");
            return;
        }
        requestManager.sendRequest(request, null, new CacheFailedRequestCallback<>(request, requestStorage));
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

    public boolean isGdprEnable() {
        return registrationPrefs.gdprEnable().get();
    }

    public String getHwid() {
        return registrationPrefs.hwid().get();
    }
}
