//
//  CreateMessageRequest.java
//
// Pushwoosh Push Notifications SDK
// www.pushwoosh.com
//
// MIT Licensed

package com.pushwoosh.sender;

import com.pushwoosh.internal.network.PushRequest;
import com.pushwoosh.internal.utils.JsonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

class CreateMessageRequest extends PushRequest<Void> {
    private final String application;
    private final String authToken;
    private final PushMessage message;

    CreateMessageRequest(String application, String authToken, PushMessage message) {
        this.application = application;
        this.authToken = authToken;
        this.message = message;
    }

    @Override
    public String getMethod() {
        return "createMessage";
    }

    @Override
    protected void buildParams(JSONObject params) throws JSONException {
        JsonUtils.clearJsonObject(params);
        params.put("application", application);
        params.put("auth", authToken);

        JSONObject notification = new JSONObject();
        notification.put("content", message.content);
        notification.put("send_date", "now");
        notification.put("ignore_user_timezone", true);
        Map<String, Object> notificationParams = message.notificationParams;
        if (notificationParams != null) {
            for (String key : notificationParams.keySet()) {
                notification.put(key, notificationParams.get(key));
            }
        }

        JSONArray notificaitons = new JSONArray();
        notificaitons.put(notification);
        params.put("notifications", notificaitons);

        System.out.print(params.toString());
    }
}
