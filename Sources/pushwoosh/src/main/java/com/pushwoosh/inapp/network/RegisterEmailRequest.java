package com.pushwoosh.inapp.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.network.PushRequest;
import com.pushwoosh.repository.RepositoryModule;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

class RegisterEmailRequest extends PushRequest<Map<String, Object>> {

    // user email
    private String email;

    RegisterEmailRequest(String email) {
        this.email = email;
    }

    public String getMethod() {
        return "registerEmail";
    }

    @Override
    protected void buildParams(JSONObject params) throws JSONException {
        params.put("email", email);
        params.put(
                "tz_offset",
                TimeUnit.SECONDS.convert(TimeZone.getDefault().getOffset(new Date().getTime()), TimeUnit.MILLISECONDS));
        if (!RepositoryModule.getNotificationPreferences()
                .isCollectingDeviceLocaleAllowed()
                .get()) {
            return;
        }
        params.put("language", Locale.getDefault().getDisplayLanguage());
    }

    @Nullable @Override
    public Map<String, Object> parseResponse(@NonNull JSONObject response) throws JSONException {
        return super.parseResponse(response);
    }
}
