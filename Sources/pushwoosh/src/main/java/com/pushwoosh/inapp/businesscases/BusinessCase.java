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

package com.pushwoosh.inapp.businesscases;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.event.RichMediaCloseEvent;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppStorage;
import com.pushwoosh.inapp.view.InAppViewEvent;
import com.pushwoosh.inapp.view.InAppViewFailedEvent;
import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;
import com.pushwoosh.internal.event.Event;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.EventListener;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.TimeProvider;
import com.pushwoosh.richmedia.RichMediaController;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by kai on 31.01.2018.
 */

public class BusinessCase {
    public static final int ONE_HOUR = 1000 * 60 * 60;
    public static final long ONE_DAY = 24 * ONE_HOUR;
    public static final String TAG = "BusinessCase";


    private interface CheckedEventListener<T extends Event> {
        boolean onReceive(T event);
    }

    private class OneTimeEventListener<T extends Event> implements EventListener<T> {
        private CheckedEventListener<T> eventListener;
        private Class<T> eventType;
        private OneTimeEventListener anotherListener;

        OneTimeEventListener(Class<T> event, CheckedEventListener<T> eventListener) {
            this.eventListener = eventListener;
            eventType = event;
            EventBus.subscribe(eventType, this);
        }

        void setAnotherListener(OneTimeEventListener anotherListener) {
            this.anotherListener = anotherListener;
        }

        @Override
        public void onReceive(T event) {
            if (eventListener.onReceive(event)) {
                unsubscribe();
            }
        }

        void unsubscribe() {
            if (anotherListener != null) {
                if (anotherListener.anotherListener == this) {
                    anotherListener.setAnotherListener(null);
                }
                anotherListener.unsubscribe();
            }
            EventBus.unsubscribe(eventType, this);
        }
    }

    public interface BusinessCaseCallback {
        void onShowFail(BusinessCaseResult result);
    }

    public interface Condition {
        boolean check();
    }

    interface Waiter<T> {
        void onValue(T val);
    }

    private class Future<T> {
        private T val;
        private boolean isCanceled;
        private Waiter<T> waiter;

        void getVal(Waiter<T> waiter) {
            synchronized (this) {
                if (val != null) {
                    waiter.onValue(val);
                } else {
                    this.waiter = waiter;
                }
            }
        }

        public T getValOrNull() {
            return val;
        }

        public void provide(T val) {
            synchronized (this) {
                if (waiter != null) {
                    waiter.onValue(val);
                    waiter = null;
                }
                this.val = val;
            }
        }
    }

    private String uid;
    private long cappingTime;
    private Condition condition;

    private SharedPreferences prefs;

    private Future<String> inAppId = new Future<>();

    private Date lastTriggerDate;
    private String resourceId;
    private TimeProvider timeProvider;
    private BusinessCaseCallback savedCallback;

    public BusinessCase(String uid, float capCountDay, SharedPreferences prefs, Condition condition, TimeProvider timeProvider) {
        this.uid = uid;
        this.cappingTime = (long) (capCountDay * ONE_DAY);
        this.prefs = prefs;
        this.condition = condition;
        this.timeProvider = timeProvider;
    }

    public void trigger(BusinessCaseCallback callback) {
        PWLog.debug("[BusinessCase]", "trigger " + uid);
        if (!condition.check()) {
            if (callback != null) {
                callback.onShowFail(BusinessCaseResult.CONDITION_NOT_SATISFIED);
            } else {
                PWLog.debug(TAG, uid + " condition not satisfied");
            }
            return;
        }
        if (checkTriggerCapExceeded()) {
            if (callback != null) {
                callback.onShowFail(BusinessCaseResult.TRIGGER_CAP_EXCEEDED);
            } else {
                PWLog.debug(TAG, uid + " trigger cap exceeded");
            }
            return;
        }

        AtomicBoolean timeoutExceeded = new AtomicBoolean(false);
        AtomicBoolean valueReceived = new AtomicBoolean(false);
        Looper myLooper = Looper.myLooper();
        if (myLooper == null) {
            PWLog.debug(TAG,"Looper is null. Using MainLooper instead, which will cause StrictMode policy violation");
        }
        Handler handler = new Handler(myLooper != null ? myLooper : Looper.getMainLooper());
        handler.postDelayed(() -> {
            if (valueReceived.get()) {
                return;
            }
            if (callback != null)
                callback.onShowFail(BusinessCaseResult.LOADING_FAILED);
            timeoutExceeded.set(true);
        }, 4000);
        inAppId.getVal((val) -> {
            handler.post(() -> {
                if (timeoutExceeded.get()) {
                    PWLog.debug(TAG, uid + " timeout Exceeded");
                    return;
                }
                valueReceived.set(true);
                Resource resource = null;
                InAppStorage inAppStorage = InAppModule.getInAppStorage();
                if (inAppStorage != null) {
                    resource = inAppStorage.getResource(val);
                }
                if (resource != null) {
                    if (callback != null) {
                        OneTimeEventListener<InAppViewEvent> successListener = new OneTimeEventListener<>(InAppViewEvent.class, (event) -> event.getResource().getCode().equals(val));
                        OneTimeEventListener<InAppViewFailedEvent> failListener = new OneTimeEventListener<>(InAppViewFailedEvent.class, (event) -> {
                            if (event.getResource().getCode().equals(val)) {
                                callback.onShowFail(BusinessCaseResult.LOADING_FAILED);
                                return true;
                            }
                            return false;
                        });
                        successListener.setAnotherListener(failListener);
                        failListener.setAnotherListener(successListener);
                    }

                    ResourceWrapper resourceWrapper = new ResourceWrapper.Builder()
                            .setResource(resource)
                            .build();

                    resourceId = resource.getCode();
                    savedCallback = callback;
                    EventBus.subscribe(RichMediaCloseEvent.class, this::onCloseRichMedia);

                    RichMediaController richMediaController = PushwooshPlatform.getInstance().getRichMediaController();
                    if (richMediaController != null)
                        richMediaController.showResourceWrapper(resourceWrapper);

                    setTriggered();
                } else {
                    if (callback != null)
                        callback.onShowFail(BusinessCaseResult.LOADING_FAILED);
                }
            });
        });
    }

    private void onCloseRichMedia(RichMediaCloseEvent event) {
        Resource resource = event.getResource();

        if (resource != null) {
            if (TextUtils.equals(resource.getCode(), resourceId)) {
                EventBus.unsubscribe(RichMediaCloseEvent.class, this::onCloseRichMedia);

                //to ensure that callback called after registerForPushNotifications
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (savedCallback != null) {
                            savedCallback.onShowFail(BusinessCaseResult.RICHMEDIA_CLOSED);
                            savedCallback = null;
                        }
                    }
                }, 1000);

            }
        } else {
            PWLog.error(TAG, "resource in event is null");
        }
    }

    private boolean checkTriggerCapExceeded() {
        if (cappingTime == 0) {
            return false;
        }

        long timeValue = prefs.getLong(uid, Long.MIN_VALUE);

        if (timeValue != Long.MIN_VALUE) {
            lastTriggerDate = new Date(timeValue);
        }

        if (lastTriggerDate == null) {
            return false;
        }

        long timeBeforeListTriggering = timeProvider.getCurrentTime() - lastTriggerDate.getTime();

        return timeBeforeListTriggering < cappingTime;
    }

    public String getUid() {
        return uid;
    }

    public void setInAppId(String inAppId) {
        this.inAppId.provide(inAppId);
    }

    private void setTriggered() {
        prefs.edit().putLong(uid, timeProvider.getCurrentTime()).apply();
    }
}
