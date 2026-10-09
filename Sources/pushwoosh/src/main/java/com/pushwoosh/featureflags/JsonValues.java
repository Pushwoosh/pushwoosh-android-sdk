package com.pushwoosh.featureflags;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

final class JsonValues {
    private JsonValues() {}

    @NonNull static JSONObject copy(@NonNull JSONObject source) {
        try {
            return new JSONObject(source.toString());
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    @NonNull static JSONArray copy(@NonNull JSONArray source) {
        try {
            return new JSONArray(source.toString());
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    static boolean equal(@Nullable Object a, @Nullable Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a instanceof JSONObject && b instanceof JSONObject) {
            JSONObject x = (JSONObject) a;
            JSONObject y = (JSONObject) b;
            if (x.length() != y.length()) {
                return false;
            }
            Iterator<String> keys = x.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!y.has(key) || !equal(x.opt(key), y.opt(key))) {
                    return false;
                }
            }
            return true;
        }
        if (a instanceof JSONArray && b instanceof JSONArray) {
            JSONArray x = (JSONArray) a;
            JSONArray y = (JSONArray) b;
            if (x.length() != y.length()) {
                return false;
            }
            for (int i = 0; i < x.length(); i++) {
                if (!equal(x.opt(i), y.opt(i))) {
                    return false;
                }
            }
            return true;
        }
        if (a instanceof Number && b instanceof Number) {
            return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue()) == 0;
        }
        return a.equals(b);
    }
}
