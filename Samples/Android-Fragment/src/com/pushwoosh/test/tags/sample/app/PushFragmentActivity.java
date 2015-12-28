package com.pushwoosh.test.tags.sample.app;

import com.pushwoosh.fragment.PushEventListener;
import com.pushwoosh.fragment.PushFragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;

/**
 * Date: 2/9/2015
 * Time: 12:23 PM
 *
 * @author xanderblinov
 */
public class PushFragmentActivity extends FragmentActivity implements PushEventListener
{
	private TextView mGeneralStatus;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		//Init Pushwoosh fragment
		PushFragment.init(this);

		setContentView(R.layout.act_pushfrag);

		mGeneralStatus = (TextView) findViewById(R.id.general_status);
	}

	@Override
	public void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);

		//Check if we've got new intent with a push notification
		PushFragment.onNewIntent(this, intent);
	}

	public void doOnRegistered(String registrationId)
	{
		mGeneralStatus.setText(getString(R.string.registered, registrationId));
	}

	public void doOnRegisteredError(String errorId)
	{
		mGeneralStatus.setText(getString(R.string.registered_error, errorId));
	}

	public void doOnUnregistered(final String message)
	{
		mGeneralStatus.setText(message);
	}

	public void doOnUnregisteredError(String errorId)
	{
		mGeneralStatus.setText(getString(R.string.unregistered_error, errorId));
	}

	public void doOnMessageReceive(String message)
	{
		mGeneralStatus.setText(getString(R.string.on_message, message));
	}

}