package com.pushwoosh.test.tags.sample.app;

/**
 * Date: 01.11.12
 * Time: 12:22
 *
 * @author MiG35
 */
public interface SendTagsCallBack
{
	void onStatusChange(int sendTagsStatus);

	void onTaskEnds();

	void onTaskStarts();
}
