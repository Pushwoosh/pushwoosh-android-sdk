package com.arellomobile.android.push;

import com.arellomobile.android.push.exception.PushWooshException;

import java.util.Map;

/**
 * Date: 27.08.12
 * Time: 14:37
 *
 * @author MiG35
 */
public interface SendPushTagsCallBack
{
	void taskStarted();

	void onSentTagsSuccess(Map<String, String> skippedTags);

	void onSentTagsError(PushWooshException error);
}
