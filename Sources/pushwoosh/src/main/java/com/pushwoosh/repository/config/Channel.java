package com.pushwoosh.repository.config;

import org.json.JSONException;
import org.json.JSONObject;

public class Channel {
    private String code;
    private String name;
    private int position;

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getPosition() {
        return position;
    }

    public Channel(JSONObject json) {
        code = json.optString("code");
        name = json.optString("name");
        position = json.optInt("position", Integer.MAX_VALUE);
    }

    public JSONObject jsonValue() {
        JSONObject result = new JSONObject();
        try {
            result.put("code", code);
            result.put("name", name);
            result.put("position", position);
        } catch (Exception ignore) {}
        return result;
    }
}
