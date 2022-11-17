package com.pushwoosh.repository.config;

import androidx.annotation.NonNull;

import com.pushwoosh.internal.network.PushRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class GetConfigRequest extends PushRequest<Config> {

    @Override
    public String getMethod() {
        return "getConfig";
    }

    @Override
    protected void buildParams(JSONObject params) throws JSONException {
        JSONArray features = new JSONArray();
        features.put("channels");
        features.put("events");
        features.put("public_key");
        params.put("features", features);
    }

    @NonNull
    @Override
    public Config parseResponse(@NonNull JSONObject response) throws JSONException {
        JSONObject features = response.optJSONObject("features");
        ArrayList<Channel> channels = new ArrayList<>();
        ArrayList<Event> events = new ArrayList<>();
        String publicKey = "";
        int logger = 1;
        if (features != null) {
            JSONArray channelsJSON = features.optJSONArray("channels");
            JSONArray eventsJSON = features.optJSONArray("events");
            if (channelsJSON != null) {
                for (int i = 0; i < channelsJSON.length(); ++i) {
                    JSONObject channelJSON = channelsJSON.getJSONObject(i);
                    Channel channel = new Channel(channelJSON);
                    channels.add(channel);
                }
            }
            if (eventsJSON != null) {
                for (int i = 0; i < eventsJSON.length(); ++i) {
                    String eventName = eventsJSON.optString(i);
                    if (!eventName.isEmpty()) {
                        events.add(new Event(eventName));
                    }
                }
            }
            publicKey = features.getString("public_key");
            try {
                logger = features.getInt("logger");
            } catch (JSONException e) {
                // ignore
            }
        }
        return new Config(channels, events, publicKey, logger);
    }
}
