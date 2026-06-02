package com.pushwoosh.notification;

import android.text.TextUtils;

import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONException;
import org.json.JSONObject;

public class Action {
    public enum Type {
        ACTIVITY,
        SERVICE,
        BROADCAST
    }

    private final Type mType;
    private final String mIntentAction;
    private final String mTitle;
    private final String mIcon;
    private final String mUrl;

    @SuppressWarnings("rawtypes")
    private Class mClass;

    private JSONObject mExtras;

    public Action(JSONObject json) throws JSONException {
        // mandatory
        try {
            mType = Type.valueOf(json.getString("type"));
        } catch (Exception e) {
            throw new JSONException(e.getMessage());
        }

        mTitle = json.getString("title");

        // optional
        mIcon = json.optString("icon");
        mIntentAction = json.optString("action");
        mUrl = json.optString("url");
        String className = json.optString("class");
        if (!TextUtils.isEmpty(className)) {
            try {
                mClass = Class.forName(className);
            } catch (ClassNotFoundException e) {
                PWLog.exception(e);
            }
        }

        try {
            mExtras = json.getJSONObject("extras");
        } catch (JSONException e) {
            // ignore
        }
    }

    public Type getType() {
        return mType;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getIcon() {
        return mIcon;
    }

    public String getUrl() {
        return mUrl;
    }

    @SuppressWarnings("rawtypes")
    public Class getActionClass() {
        return mClass;
    }

    public JSONObject getExtras() {
        return mExtras;
    }

    public String getIntentAction() {
        return mIntentAction;
    }
}
