package com.pushwoosh.internal.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.InitHwidEvent;
import com.pushwoosh.internal.event.Subscription;
import com.pushwoosh.internal.platform.utils.DeviceUtils;
import com.pushwoosh.internal.platform.utils.GeneralUtils;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.repository.RepositoryModule;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class PushRequest<S> {

    public abstract String getMethod();

    JSONObject getParams() throws JSONException, InterruptedException {
        final JSONObject baseParams = new JSONObject();

        baseParams.put("application", getApplicationId());
        String hwid = getHwid();
        baseParams.put("hwid", hwid);
        baseParams.put("v", GeneralUtils.SDK_VERSION);

        //check for Amazon (Kindle) or Google device
        baseParams.put("device_type", DeviceSpecificProvider.getInstance().deviceType());

        String currentUserId = getUserId();
        if (!TextUtils.isEmpty(currentUserId)) {
            baseParams.put("userId", currentUserId);
        }

        buildParams(baseParams);

        return baseParams;
    }

    @NonNull
    protected String getHwid() throws InterruptedException {
        String hwid = Pushwoosh.getInstance().getHwid();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Subscription<InitHwidEvent> subscriber =
                EventBus.subscribe(InitHwidEvent.class, event -> countDownLatch.countDown());
        if (hwid.isEmpty()) {
            countDownLatch.await(5, TimeUnit.SECONDS);
        }
        subscriber.unsubscribe();

        hwid = Pushwoosh.getInstance().getHwid();
        if (hwid.isEmpty()) {
            hwid = DeviceUtils.getDeviceUUID();
        }
        return hwid;
    }

    protected String getUserId() {
        return RepositoryModule.getRegistrationPreferences().userId().get();
    }

    protected String getApplicationId() {
        return RepositoryModule.getRegistrationPreferences().applicationId().get();
    }

    /**
     * Add params to request
     *
     * @param params - current params
     */
    protected void buildParams(JSONObject params) throws JSONException {
        // subclass
    }

    /**
     * Method for parsing response which returning from service for current request
     *
     * @param response - JSONObject response
     * @return Response model which connected with this request.
     */
    @Nullable
    public S parseResponse(@NonNull JSONObject response) throws JSONException {
        return null;
    }
}
