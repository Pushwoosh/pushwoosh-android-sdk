
package com.pushwoosh.inapp.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.UserIdUpdatedEvent;
import com.pushwoosh.internal.network.PushRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.TimeZone;
import java.util.Date;

class RegisterEmailRequest extends PushRequest<Map<String, Object>> {

    // user email
    private String email;

    RegisterEmailRequest(String email) {
        this.email = email;
    }

    public String getMethod() {
        return "registerEmail";
    }

    public boolean shouldUseJitter(){ return false; }

    @Override
    protected void buildParams(JSONObject params) throws JSONException {
        String language = Locale.getDefault().getDisplayLanguage();

        params.put("email", email);
        params.put("tz_offset", TimeUnit.SECONDS.convert(TimeZone.getDefault().getOffset(new Date().getTime()), TimeUnit.MILLISECONDS));
        params.put("language", language);
    }

    @Nullable
    @Override
    public Map<String, Object> parseResponse(@NonNull JSONObject response) throws JSONException {
        EventBus.sendEvent(new UserIdUpdatedEvent());
        return super.parseResponse(response);
    }
}
