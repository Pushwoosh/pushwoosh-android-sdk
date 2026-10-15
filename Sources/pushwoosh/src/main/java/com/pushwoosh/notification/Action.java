package com.pushwoosh.notification;

import android.text.TextUtils;

import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Legacy Android-only notification action, described by the {@code pw_actions} payload field.
 * <p>
 * Its tap fires a {@code PendingIntent} straight into an application component: the SDK does not
 * see it, the notification is not dismissed and no open event is reported.
 *
 * @deprecated Use {@link ActionButton} and {@link PushMessage#getButtons()} instead. A push that
 *             carries {@code pw_buttons} never renders {@code pw_actions}. Support for
 *             {@code pw_actions} in regular pushes will be removed in the next major release.
 */
@Deprecated
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
            } catch (ClassNotFoundException | LinkageError e) {
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
