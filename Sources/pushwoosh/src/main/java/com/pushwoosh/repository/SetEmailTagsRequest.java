package com.pushwoosh.repository;

import com.pushwoosh.internal.network.PushRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class SetEmailTagsRequest extends PushRequest<Void> {
    private JSONObject tags;
    private String email;

    public SetEmailTagsRequest(JSONObject tags, String email) {
        this.tags = tags;
        this.email = email;
    }

    @Override
    public String getMethod() {
        return "setEmailTags";
    }

    @Override
    protected void buildParams(JSONObject params) throws JSONException {
        Iterator<String> keys = tags.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            this.tags.put(key, tags.opt(key));
        }

        params.put("tags", tags);
        params.put("email", email);
    }
}
