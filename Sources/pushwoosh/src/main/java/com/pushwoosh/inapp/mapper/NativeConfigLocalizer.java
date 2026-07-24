/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.inapp.mapper;

import androidx.annotation.NonNull;

import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Localizes a native-config.json by substituting placeholders inside every string VALUE of the JSON
 * tree; object keys and non-string values are left untouched. Mirrors Rich Media localization: it
 * reuses {@link PlaceholderSubstitutor} with the language dictionary, while the tag passes get an
 * empty map for the MVP — so any nested dynamic-content placeholder collapses to its default.
 *
 * <p>Tree-walk (not a raw-text replace over the JSON, as Rich Media does over HTML) is intentional:
 * values are substituted into already-parsed strings, so quotes / newlines in a translation cannot
 * break the JSON or need escaping.
 */
public final class NativeConfigLocalizer {
    private static final String TAG = "[InApp]NativeConfigLocalizer";

    private NativeConfigLocalizer() {}

    @NonNull public static String localize(@NonNull String configJson, @NonNull Map<String, String> strings) {
        try {
            JSONObject root = new JSONObject(configJson);
            localizeObject(root, strings);
            return root.toString();
        } catch (Throwable t) {
            // Any failure hands the input back untouched, never dropping the show: malformed JSON
            // (JSONException) or pathological nesting blowing the stack (StackOverflowError from the parser
            // or the localizeObject/localizeArray recursion). Catching Throwable is deliberate here — the
            // module parser then rejects the raw config downstream (present() returns false), exactly as if
            // localization had never run.
            PWLog.warn(TAG, "native-config.json could not be localized, passing through unchanged", t);
            return configJson;
        }
    }

    private static void localizeObject(JSONObject object, Map<String, String> strings) throws JSONException {
        List<String> keys = new ArrayList<>();
        Iterator<String> it = object.keys();
        while (it.hasNext()) {
            keys.add(it.next());
        }
        for (String key : keys) {
            Object value = object.get(key);
            if (value instanceof String) {
                object.put(key, PlaceholderSubstitutor.substitute((String) value, strings, Collections.emptyMap()));
            } else if (value instanceof JSONObject) {
                localizeObject((JSONObject) value, strings);
            } else if (value instanceof JSONArray) {
                localizeArray((JSONArray) value, strings);
            }
        }
    }

    private static void localizeArray(JSONArray array, Map<String, String> strings) throws JSONException {
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof String) {
                array.put(i, PlaceholderSubstitutor.substitute((String) value, strings, Collections.emptyMap()));
            } else if (value instanceof JSONObject) {
                localizeObject((JSONObject) value, strings);
            } else if (value instanceof JSONArray) {
                localizeArray((JSONArray) value, strings);
            }
        }
    }
}
