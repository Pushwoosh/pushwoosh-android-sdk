package com.arellomobile.android.push.request;

import android.content.Context;

import java.util.Map;

public class PushStatRequest extends PushRequest
{
	private String hash;

	public PushStatRequest(String hash)
	{
		this.hash = hash;
	}

	public String getMethod()
	{
		return "pushStat";
	}

	@Override
	protected void buildParams(Context context, Map<String, Object> params)
	{
		if (hash != null)
		{
			params.put("hash", hash);
		}
	}
}
