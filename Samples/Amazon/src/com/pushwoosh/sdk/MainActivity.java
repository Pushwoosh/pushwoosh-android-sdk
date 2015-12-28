/*
 * [ADMMessenger]
 *
 * (c) 2012, Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.pushwoosh.sdk;

import com.pushwoosh.sdk.R;
import com.pushwoosh.PushManager;
import com.pushwoosh.BasePushMessageReceiver;
import com.pushwoosh.BaseRegistrationReceiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Main activity of the sample app.
 * Provides a minimal UI to display messages coming from the server.
 *
 * @version Revision: 1, Date: 11/11/2012
 */
public class MainActivity extends Activity
{
    /** Tag for logs. */
    private final static String TAG = "ADMMessenger";

    /** {@inheritDoc} */
    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

	    //Register receivers for push notifications
	    registerReceivers();

	    //Get push manager instance
	    PushManager pushManager = PushManager.getInstance(this);
		
		//Start push manager, this will count app open for Pushwoosh stats as well
		try {
			pushManager.onStartup(this);
		}
		catch(Exception e)
		{
			//push notifications are not available or AndroidManifest.xml is not configured properly
		}

	    PushManager.setSimpleNotificationMode(this);

		//Register for push!
		pushManager.registerForPushNotifications();

	    checkMessage(getIntent());
    }

	@Override
	public void onResume()
	{
		super.onResume();

		//Re-register receivers on resume
		registerReceivers();
	}

	@Override
	public void onPause()
	{
		super.onPause();

		//Unregister receivers on pause
		unregisterReceivers();
	}

	//Registration receiver
	BroadcastReceiver mBroadcastReceiver = new BaseRegistrationReceiver()
	{
		@Override
		public void onRegisterActionReceive(Context context, Intent intent)
		{
			checkMessage(intent);
		}
	};

	//Push message receiver
	private BroadcastReceiver mReceiver = new BasePushMessageReceiver()
	{
		@Override
		protected void onMessageReceive(Intent intent)
		{
			//JSON_DATA_KEY contains JSON payload of push notification.
			showMessage("push message is " + intent.getExtras().getString(JSON_DATA_KEY));
		}
	};

	//Registration of the receivers
	public void registerReceivers()
	{
		IntentFilter intentFilter = new IntentFilter(getPackageName() + ".action.PUSH_MESSAGE_RECEIVE");

		registerReceiver(mReceiver, intentFilter, getPackageName() +".permission.RECEIVE_ADM_MESSAGE", null);

		registerReceiver(mBroadcastReceiver, new IntentFilter(getPackageName() + "." + PushManager.REGISTER_BROAD_CAST_ACTION));
	}

	public void unregisterReceivers()
	{
		//Unregister receivers on pause
		try
		{
			unregisterReceiver(mReceiver);
		}
		catch (Exception e)
		{
			// pass.
		}

		try
		{
			unregisterReceiver(mBroadcastReceiver);
		}
		catch (Exception e)
		{
			//pass through
		}
	}

	private void checkMessage(Intent intent)
	{
		if (null != intent)
		{
			if (intent.hasExtra(PushManager.PUSH_RECEIVE_EVENT))
			{
				showMessage("push message is " + intent.getExtras().getString(PushManager.PUSH_RECEIVE_EVENT));
			}
			else if (intent.hasExtra(PushManager.REGISTER_EVENT))
			{
				//showMessage("register");

			}
			else if (intent.hasExtra(PushManager.UNREGISTER_EVENT))
			{
				showMessage("unregister");
			}
			else if (intent.hasExtra(PushManager.REGISTER_ERROR_EVENT))
			{
				showMessage("register error");
			}
			else if (intent.hasExtra(PushManager.UNREGISTER_ERROR_EVENT))
			{
				showMessage("unregister error");
			}

			resetIntentValues();
		}
	}

	/**
	 * Will check main Activity intent and if it contains any PushWoosh data, will clear it
	 */
	private void resetIntentValues()
	{
		Intent mainAppIntent = getIntent();

		if (mainAppIntent.hasExtra(PushManager.PUSH_RECEIVE_EVENT))
		{
			mainAppIntent.removeExtra(PushManager.PUSH_RECEIVE_EVENT);
		}
		else if (mainAppIntent.hasExtra(PushManager.REGISTER_EVENT))
		{
			mainAppIntent.removeExtra(PushManager.REGISTER_EVENT);
		}
		else if (mainAppIntent.hasExtra(PushManager.UNREGISTER_EVENT))
		{
			mainAppIntent.removeExtra(PushManager.UNREGISTER_EVENT);
		}
		else if (mainAppIntent.hasExtra(PushManager.REGISTER_ERROR_EVENT))
		{
			mainAppIntent.removeExtra(PushManager.REGISTER_ERROR_EVENT);
		}
		else if (mainAppIntent.hasExtra(PushManager.UNREGISTER_ERROR_EVENT))
		{
			mainAppIntent.removeExtra(PushManager.UNREGISTER_ERROR_EVENT);
		}

		setIntent(mainAppIntent);
	}

	private void showMessage(String message)
	{
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		setIntent(intent);

		checkMessage(intent);

		setIntent(new Intent());
	}
}
