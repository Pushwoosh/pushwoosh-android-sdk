package com.pushwoosh.internal.network;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by etkachenko on 3/28/17.
 */
public class PushRequestHelper {
    public static JSONObject getParams(PushRequest request) throws Exception {
        return request.getParams();
    }

    public static List<String> sortedKeys(JSONObject params) {
        List<String> keys = new ArrayList<>();
        Iterator<String> iterator = params.keys();
        while (iterator.hasNext()) {
            keys.add(iterator.next());
        }
        Collections.sort(keys);
        return keys;
    }
}
