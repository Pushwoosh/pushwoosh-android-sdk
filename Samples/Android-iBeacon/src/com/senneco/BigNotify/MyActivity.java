package com.senneco.BigNotify;

import java.util.HashMap;
import java.util.Map;

import com.pushwoosh.PushManager;
import com.pushwoosh.BasePushMessageReceiver;
import com.pushwoosh.BaseRegistrationReceiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MyActivity extends Activity
{
	private String mLog = "";
	private TextView mLogText;

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		//Register receivers for push notifications
		registerReceivers();

		//Create and start push manager
		PushManager.initializePushManager(this, "4FC89B6D14A655.46488481", "60756016005");

		PushManager pushManager = PushManager.getInstance(this);

		PushManager.setMultiNotificationMode(this);

		try
		{
			pushManager.onStartup(this);
			pushManager.startTrackingBeaconPushes();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("push_enabled", true))
		{
			pushManager.registerForPushNotifications();
		}
		else
		{
			pushManager.unregisterForPushNotifications();
		}

		Map<String, Object> tags = new HashMap<String, Object>();

		tags.put("List", new String[]{"red", "green", "blue"});

		PushManager.sendTags(this, tags, null);

		PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("push_enabled", true).commit();

		checkMessage(getIntent());

		setContentView(R.layout.main);

		mLogText = (TextView) findViewById(R.id.text_log);

		findViewById(R.id.butt_set_background).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				PushManager.setBeaconBackgroundMode(MyActivity.this, true);
			}
		});

		findViewById(R.id.butt_set_foreground).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				PushManager.setBeaconBackgroundMode(MyActivity.this, false);
			}
		});

		LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				mLog += intent.getStringExtra("Status") + "\n";
				mLogText.setText(mLog);
			}
		}, new IntentFilter("beacon"));
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
		unregisterReceivers();

		IntentFilter intentFilter = new IntentFilter("PUSH_MESSAGE_RECEIVE");
		LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, intentFilter);

		registerReceiver(mBroadcastReceiver, new IntentFilter(getPackageName() + "." + PushManager.REGISTER_BROAD_CAST_ACTION));
	}

	public void unregisterReceivers()
	{
		//Unregister receivers on pause
		try
		{
			LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
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
				showMessage("register");
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

