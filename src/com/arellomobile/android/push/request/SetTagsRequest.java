package com.arellomobile.android.push.request;

import android.content.Context;

import com.arellomobile.android.push.PushManager;
import com.arellomobile.android.push.utils.JsonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class SetTagsRequest extends PushRequest
{

	private Map<String, Object> tags;
	private JSONArray skippedTags;

	public SetTagsRequest(Map<String, Object> tags)
	{
		this.tags = tags;
	}

	@Override
	public String getMethod()
	{
		return "setTags";
	}

	@Override
	protected void buildParams(Context context, Map<String, Object> params) throws JSONException
	{
		JSONObject tagsObject = jsonObjectFromTagMap(tags);
		params.put("tags", tagsObject);
	}

	private JSONObject jsonObjectFromTagMap(Map<String, Object> tags) throws JSONException
	{
		// prepare strange #pwinc# key
		for (String key : tags.keySet())
		{
			Object value = tags.get(key);
			if (value instanceof String)
			{
				String valString = (String) value;
				if (valString.startsWith("#pwinc#"))
				{
					valString = valString.substring(7);
					Integer intValue = Integer.parseInt(valString);
					tags.put(key, PushManager.incrementalTag(intValue));
				}
			}
		}

		return JsonUtils.mapToJson(tags);
	}

	public void parseResponse(JSONObject response) throws JSONException
	{
		JSONObject resp = response.getJSONObject("response");
		skippedTags = resp.getJSONArray("skipped");
	}

	public JSONArray getSkippedTags()
	{
		return skippedTags;
	}
}
