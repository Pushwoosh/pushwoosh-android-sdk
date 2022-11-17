package com.pushwoosh.internal.utils;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("WeakerAccess")
public final class JsonUtils {
    @NonNull
    public static Map<String, Object> jsonToMap(@Nullable JSONObject jsonObject, boolean convertSubobjectToString) throws JSONException {
        Map<String, Object> map = new HashMap<>();

        if (jsonObject == null) {
            return map;
        }
        JSONObject object;
        try {
            //cloning to avoid ConcurrentModificationException for keysIterator
            object = new JSONObject(jsonObject.toString());
        } catch (Throwable t) {
            // avoid ConcurrentModificationException in jsonObject.toString() method
            return map;
        }

        Iterator<String> keysItr = object.keys();
        while (keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if (value instanceof JSONArray) {
                value = convertSubobjectToString ? value.toString() : jsonToList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = convertSubobjectToString ? value.toString() : jsonToMap((JSONObject) value, false);
            } else if (JSONObject.NULL.equals(value)) {
                value = null;
            }

            map.put(key, value);
        }
        return map;
    }

    @NonNull
    public static Map<String, Object> jsonToMap(@Nullable JSONObject jsonObject) throws JSONException {
        return jsonToMap(jsonObject, false);
    }

    @NonNull
    public static List<Object> jsonToList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);

            if (value instanceof JSONArray) {
                value = jsonToList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = jsonToMap((JSONObject) value);
            } else if (JSONObject.NULL.equals(value)) {
                value = null;
            }

            list.add(value);
        }
        return list;
    }

    @NonNull
    public static JSONObject mapToJson(@Nullable Map<?, ?> data) {
        JSONObject object = new JSONObject();

        if (data == null) {
            return new JSONObject();
        }

        for (Map.Entry<?, ?> entry : data.entrySet()) {
            /*
             * Deviate from the original by checking that keys are non-null and
             * of the proper type. (We still defer validating the values).
             */
            String key = (String) entry.getKey();
            if (key == null) {
                throw new IllegalArgumentException("key == null");
            }
            try {
                object.put(key, wrap(entry.getValue()));
            } catch (JSONException e) {
                PWLog.exception(e);
            }
        }

        return object;
    }

    public static JSONArray collectionToJson(Collection data) {
        JSONArray jsonArray = new JSONArray();
        if (data != null) {
            for (Object aData : data) {
                jsonArray.put(wrap(aData));
            }
        }
        return jsonArray;
    }

    public static <T> JSONArray arrayToJson(T[] data) throws JSONException {
        final int length = Array.getLength(data);
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < length; ++i) {
            jsonArray.put(wrap(Array.get(data, i)));
        }

        return jsonArray;
    }

    private static Object wrap(Object o) {
        if (o == null) {
            return JSONObject.NULL;
        }
        if (o instanceof JSONArray || o instanceof JSONObject) {
            return o;
        }
        try {
            if (o instanceof Collection) {
                return collectionToJson((Collection) o);
            } else if (o.getClass().isArray()) {
                return arrayToJson((Object[]) o);
            }
            if (o instanceof Map) {
                return mapToJson((Map) o);
            }
            if (o instanceof Boolean
                    || o instanceof Byte
                    || o instanceof Character
                    || o instanceof Double
                    || o instanceof Float
                    || o instanceof Integer
                    || o instanceof Long
                    || o instanceof Short
                    || o instanceof String) {
                return o;
            }
            if (o.getClass().getPackage().getName().startsWith("java.")) {
                return o.toString();
            }
        } catch (Exception e) {
            PWLog.exception(e);
        }
        return JSONObject.NULL;
    }

    public static void mergeJson(@NonNull JSONObject from, @NonNull JSONObject to) {
        Iterator<String> keys = from.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                to.put(key, from.opt(key));
            } catch (JSONException e) {
                PWLog.exception(e);
            }
        }
    }

    public static void clearJsonObject(@NonNull JSONObject json) {
        Iterator<String> iter = json.keys();
        List<String> keys = new ArrayList<>();

        while (iter.hasNext()) {
            keys.add(iter.next());
        }

        for (String key : keys) {
            json.remove(key);
        }
    }

    @NonNull
    public static JSONObject bundleToJson(final Bundle pushBundle) {
        JSONObject dataObject = new JSONObject();
        if (pushBundle == null) {
            return dataObject;
        }
        Set<String> keys = pushBundle.keySet();
        for (String key : keys) {
            try {
                Object value = pushBundle.get(key);
                if (value instanceof char[]) {
                    dataObject.put(key, Arrays.toString((char[]) value));
                } else if (value instanceof float[]) {
                    dataObject.put(key, Arrays.toString((float[]) value));
                } else if (value instanceof double[]) {
                    dataObject.put(key, Arrays.toString((double[]) value));
                } else if (value instanceof int[]) {
                    dataObject.put(key, Arrays.toString((int[]) value));
                } else if (value instanceof long[]) {
                    dataObject.put(key, Arrays.toString((long[]) value));
                } else if (value instanceof byte[]) {
                    dataObject.put(key, Arrays.toString((byte[]) value));
                } else if (value instanceof Object[]) {
                    dataObject.put(key, Arrays.deepToString((Object[]) value));
                } else {
                    dataObject.put(key, value);
                }
            } catch (JSONException ignore) {
                // pass
            }
        }
        return dataObject;
    }

    @NonNull
    public static JSONObject bundleToJsonWithUserData(final Bundle pushBundle) {
        JSONObject dataObject = new JSONObject();
        if (pushBundle == null) {
            return dataObject;
        }

        Set<String> keys = pushBundle.keySet();
        for (String key : keys) {
            //backward compatibility
            if (key.equals("u")) {
                try {
                    String userDataString = pushBundle.getString("u");
                    if (userDataString == null) {
                        continue;
                    }

                    if (isObject(userDataString)) {
                        dataObject.put("userdata", new JSONObject(userDataString));
                    } else if (isArray(userDataString)) {
                        dataObject.put("userdata", new JSONArray(userDataString));
                    }
                } catch (JSONException ignore) {
                    // pass
                }
            }

            try {
                dataObject.put(key, pushBundle.get(key));
            } catch (JSONException ignore) {
                // pass
            }
        }

        return dataObject;
    }

    private static boolean isArray(@NonNull final String userDataString) {
        return userDataString.trim().startsWith("[");
    }

    private static boolean isObject(@NonNull final String userDataString) {
        return userDataString.trim().startsWith("{");
    }

    public static Bundle jsonStringToBundle(String jsonString) {
        return jsonStringToBundle(jsonString, false);
    }

    public static Bundle jsonStringToBundle(String jsonString, boolean convertSubobjectsToString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            Map<String, Object> map = jsonToMap(jsonObject, convertSubobjectsToString);
            return mapToBundle(map);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        return new Bundle();
    }

    private static Bundle mapToBundle(Map<String, Object> map) {
        Bundle bundle = new Bundle();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                bundle.putString(key, (String) value);
            } else if (value instanceof Integer) {
                bundle.putInt(key, (Integer) value);
            } else if (value instanceof Float) {
                bundle.putFloat(key, (Float) value);
            } else if (value instanceof Double) {
                bundle.putDouble(key, (Double) value);
            } else if (value instanceof Boolean) {
                bundle.putBoolean(key, (Boolean) value);
            } else if (value instanceof Byte) {
                bundle.putByte(key, (Byte) value);
            } else if (value instanceof Long) {
                bundle.putLong(key, (Long) value);
            } else if (value instanceof Character) {
                bundle.putChar(key, (Character) value);
            } else if (value instanceof char[]) {
                bundle.putCharArray(key, (char[]) value);
            } else if (value instanceof float[]) {
                bundle.putFloatArray(key, (float[]) value);
            } else if (value instanceof double[]) {
                bundle.putDoubleArray(key, (double[]) value);
            } else if (value instanceof int[]) {
                bundle.putIntArray(key, (int[]) value);
            } else if (value instanceof long[]) {
                bundle.putLongArray(key, (long[]) value);
            } else if (value instanceof byte[]) {
                bundle.putByteArray(key, (byte[]) value);
            } else if (value instanceof Map) {
                bundle.putBundle(key, mapToBundle((Map<String, Object>) value));
            } else {
                throw new IllegalArgumentException("cant parse value by key:" + key + " value:" + value);
            }
        }
        return bundle;
    }

    private JsonUtils() { /* do nothing */ }
}
