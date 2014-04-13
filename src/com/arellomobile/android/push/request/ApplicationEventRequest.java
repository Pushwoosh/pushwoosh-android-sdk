package com.arellomobile.android.push.request;

import android.content.Context;

import java.util.Map;

public class ApplicationEventRequest extends PushRequest {
	
	private String goal;
	private Integer count;
	
	public ApplicationEventRequest(String goal, Integer count) {
		this.goal = goal;
		this.count = count;
	}

	@Override
	public String getMethod() {
		return "applicationEvent";
	}

	@Override
	protected void buildParams(Context context, Map<String, Object> params)
	{
		params.put("goal", goal);
		
		if(count != null) {
			params.put("count", count);
		}
	}
}
