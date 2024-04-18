package com.pushwoosh.inapp.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.UserIdUpdatedEvent;
import com.pushwoosh.internal.network.PushRequest;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;
import java.util.TimeZone;
import java.util.Date;

import java.util.Map;
import android.text.TextUtils;

class RegisterEmailUserRequest extends PushRequest<Map<String, Object>> {

    // user email
    private String email;
    // user userId
    private String userId;

    RegisterEmailUserRequest(String userId, String email) {
        this.email = email;
        this.userId = userId;
    }

    public String getMethod() {
        return "registerEmailUser";
    }

    public boolean shouldUseJitter(){ return false; }

    @Override
    protected void buildParams(JSONObject params) throws JSONException {

        params.put("email", email);
        params.put("tz_offset", TimeUnit.SECONDS.convert(TimeZone.getDefault().getOffset(new Date().getTime()), TimeUnit.MILLISECONDS));
        params.put("userId", userId);
    }

    @Nullable
    @Override
    public Map<String, Object> parseResponse(@NonNull JSONObject response) throws JSONException {
        EventBus.sendEvent(new UserIdUpdatedEvent());
        return super.parseResponse(response);
    }
}
