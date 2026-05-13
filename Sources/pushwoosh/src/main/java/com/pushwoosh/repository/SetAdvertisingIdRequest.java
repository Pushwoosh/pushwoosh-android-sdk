package com.pushwoosh.repository;

import androidx.annotation.Nullable;

import com.pushwoosh.internal.network.PushRequest;

import org.json.JSONException;
import org.json.JSONObject;

class SetAdvertisingIdRequest extends PushRequest<Void> {

    private final String advertisingId;

    SetAdvertisingIdRequest(@Nullable String advertisingId) {
        this.advertisingId = advertisingId;
    }

    @Override
    public String getMethod() {
        return "setMADID";
    }

    @Override
    public boolean shouldWrapRequest() {
        return false;
    }

    @Override
    protected JSONObject getParams() throws JSONException, InterruptedException {
        JSONObject params = new JSONObject();
        params.put("application", getApplicationId());
        params.put("hwid", getHwid());
        params.put("madid", advertisingId != null ? advertisingId : JSONObject.NULL);
        return params;
    }
}
