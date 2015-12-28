package com.pushwoosh.test.tags.sample.app;

import com.pushwoosh.BasePushMessageReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SilentPushReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent)
	{
		Log.d("Pushwoosh", "[SilentPushReceiver] " + intent.getStringExtra(BasePushMessageReceiver.JSON_DATA_KEY));
	}
}
