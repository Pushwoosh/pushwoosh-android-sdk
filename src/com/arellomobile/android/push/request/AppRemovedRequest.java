package com.arellomobile.android.push.request;

import android.content.Context;

import java.util.Map;

public class AppRemovedRequest extends PushRequest {
	
	private String packageName;
	
	public AppRemovedRequest(String packageName) {
		this.packageName = packageName;
	}

	@Override
	public String getMethod() {
		return "androidPackageRemoved";
	}

	@Override
	public void buildParams(Context context, Map<String, Object> params)
	{
		params.put("android_package", packageName);
	}
}
