package com.pushwoosh.inapp.network;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.pushwoosh.internal.network.PushRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class RichMediaActionRequest extends PushRequest<Void> {

    private String richmediaCode;
    private String inappCode;
    private String messageHash;
    private String actionAttributes;
    private int actionType;

    public RichMediaActionRequest(String richmediaCode, String inappCode, String messageHash, String actionAttributes, int actionType) {
        this.richmediaCode = richmediaCode;
        this.inappCode = inappCode;
        this.messageHash = messageHash;
        this.actionAttributes = actionAttributes;
        this.actionType = actionType;
    }

    @Override
    public String getMethod() {
        return "richMediaAction";
    }

    @Override
    protected void buildParams(JSONObject params) throws JSONException {
        addStringToParamsIfNotEmpty( "rich_media_code", richmediaCode, params);
        addStringToParamsIfNotEmpty("inapp_code", inappCode, params);
        addStringToParamsIfNotEmpty("message_hash", messageHash, params);
        addStringToParamsIfNotEmpty("action_attributes", actionAttributes, params);
        params.put("action_type", actionType);
    }

    private void addStringToParamsIfNotEmpty(String key, String value, @NonNull JSONObject params) throws JSONException {
        if (!TextUtils.isEmpty(value)) {
            params.put(key, value);
        }
    }
}
