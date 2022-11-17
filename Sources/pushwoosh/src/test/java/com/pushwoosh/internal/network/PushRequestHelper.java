package com.pushwoosh.internal.network;

import org.json.JSONObject;

/**
 * Created by etkachenko on 3/28/17.
 */

public class PushRequestHelper {
	public static JSONObject getParams(PushRequest request) throws Exception {
		return request.getParams();
	}
}
