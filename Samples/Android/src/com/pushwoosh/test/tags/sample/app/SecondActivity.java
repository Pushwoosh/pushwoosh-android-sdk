package com.pushwoosh.test.tags.sample.app;

import com.pushwoosh.PushManager;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Date: 04.09.13
 * Time: 15:21
 *
 * @author Yuri Shmakov
 */
public class SecondActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.second);

		((TextView) findViewById(R.id.text_push)).setText(getIntent().getStringExtra(PushManager.PUSH_RECEIVE_EVENT));
	}
}
