package com.arellomobile.android.push.request;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GetTagsRequest extends PushRequest {
	
	private Map<String, Object> tags;

	public String getMethod() {
		return "getTags";
	}
	
	@Override
	public void parseResponse(JSONObject resultData) throws JSONException {
		Map<String, Object> result = new HashMap<String, Object>();

		JSONObject response = resultData.getJSONObject("response");
		JSONObject jsonResult = response.getJSONObject("result");
		
		@SuppressWarnings("unchecked")
		Iterator<String> keys = jsonResult.keys();
		while(keys.hasNext()) {
			String key = keys.next();
			result.put(key, jsonResult.get(key));
		}
		
		tags = result;
	}
	
	public Map<String, Object> getTags() {
		return tags;
	}
}
