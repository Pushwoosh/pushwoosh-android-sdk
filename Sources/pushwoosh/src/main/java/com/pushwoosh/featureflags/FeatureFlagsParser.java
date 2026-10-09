package com.pushwoosh.featureflags;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

final class FeatureFlagsParser {
    private FeatureFlagsParser() {}

    @NonNull static Map<String, FeatureFlag> parse(@NonNull JSONObject flags) throws JSONException {
        Map<String, FeatureFlag> result = new HashMap<>();
        Iterator<String> keys = flags.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject flag = flags.getJSONObject(key);
            Object ruleId = flag.opt("rule_id");
            JSONObject properties = flag.optJSONObject("properties");
            result.put(
                    key,
                    new FeatureFlag(
                            key,
                            flag.getBoolean("enabled"),
                            ruleId instanceof String ? (String) ruleId : "",
                            properties != null ? properties : new JSONObject()));
        }
        return Collections.unmodifiableMap(result);
    }
}
