package com.pushwoosh.sample;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;

import com.pushwoosh.PushManager;
import com.pushwoosh.PushManager.RichPageListener;
import com.pushwoosh.BasePushMessageReceiver;
import com.pushwoosh.BaseRegistrationReceiver;

public class MainActivity extends Activity
{
	private TextView mGeneralStatus;
	private Button mSendPushButton;
	private ToggleButton mGeoPushButton;
	private ToggleButton mBeaconPushButton;

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		mGeneralStatus = (TextView) findViewById(R.id.general_status);
		mGeoPushButton = (ToggleButton) findViewById(R.id.butt_geo_pushes);
		mBeaconPushButton = (ToggleButton) findViewById(R.id.butt_beacon_pushes);
		mSendPushButton = (Button) findViewById(R.id.butt_send_push);
		mSendPushButton.setEnabled(false);

		findViewById(R.id.butt_set_tags).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(MainActivity.this, SetTagsActivity.class);
				startActivity(intent);
			}
		});

		mSendPushButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, SendPushActivity.class);
				startActivity(intent);
			}
		});

		initPushwoosh();
	}

	private void initPushwoosh()
	{
		//Register receivers for push notifications
		registerReceivers();

		final PushManager pushManager = PushManager.getInstance(this);

		class RichPageListenerImpl implements RichPageListener
		{
			@Override
			public void onRichPageAction(String actionParams)
			{
				Log.d("Pushwoosh", "Rich page action: " + actionParams);
			}

			@Override
			public void onRichPageClosed()
			{
				Log.d("Pushwoosh", "Rich page closed");
			}
		}

		pushManager.setRichPageListener(new RichPageListenerImpl());

		//pushManager.setNotificationFactory(new NotificationFactorySample());

		//Start push manager, this will count app open for Pushwoosh stats as well
		try
		{
			pushManager.onStartup(this);
		}
		catch (Exception e)
		{
			Log.e("Pushwoosh", e.getLocalizedMessage());
		}

		//Register for push!
		pushManager.registerForPushNotifications();

		//check launch notification (optional)
		String launchNotificatin = pushManager.getLaunchNotification();
		if (launchNotificatin != null)
		{
			Log.d("Pushwoosh", "Launch notification received: " + launchNotificatin);
		}
		else
		{
			Log.d("Pushwoosh", "No launch notification received");
		}

		//Check for start push notification in the Intent payload
		checkMessage(getIntent());

		//The commented code below shows how to use geo pushes
		//pushManager.startTrackingGeoPushes();

		//The commented code below shows how to use local notifications
		//PushManager.clearLocalNotifications(this);

		//easy way
		//PushManager.scheduleLocalNotification(this, "Your pumpkins are ready!", 30);

		//expert mode
		//Bundle extras = new Bundle();
		//extras.putString("b", "https://cp.pushwoosh.com/img/arello-logo.png");
		//extras.putString("u", "50");
		//PushManager.scheduleLocalNotification(this, "Your pumpkins are ready!", extras, 30);

		//Clear application badge number
		//pushManager.setBadgeNumber(0);

		// Start/stop geo pushes
		mGeoPushButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					pushManager.startTrackingGeoPushes();
				} else {
					pushManager.stopTrackingGeoPushes();
				}
			}
		});

		// Start/stop beacon pushes
		mBeaconPushButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					pushManager.startTrackingBeaconPushes();
				} else {
					pushManager.stopTrackingBeaconPushes();
				}
			}
		});
	}

	/**
	 * Called when the activity receives a new intent.
	 */
	public void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);

		//have to check if we've got new intent as a part of push notification
		checkMessage(intent);
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
			doOnMessageReceive(intent.getExtras().getString(JSON_DATA_KEY));
		}
	};

	//Registration of the receivers
	public void registerReceivers()
	{
		IntentFilter intentFilter = new IntentFilter(getPackageName() + ".action.PUSH_MESSAGE_RECEIVE");
		registerReceiver(mReceiver, intentFilter, getPackageName() +".permission.C2D_MESSAGE", null);
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

	/**
	 * Will check PushWoosh extras in this intent, and fire actual method
	 *
	 * @param intent activity intent
	 */
	private void checkMessage(Intent intent)
	{
		if (null != intent)
		{
			if (intent.hasExtra(PushManager.PUSH_RECEIVE_EVENT))
			{
				doOnMessageReceive(intent.getExtras().getString(PushManager.PUSH_RECEIVE_EVENT));
			}
			else if (intent.hasExtra(PushManager.REGISTER_EVENT))
			{
				doOnRegistered(intent.getExtras().getString(PushManager.REGISTER_EVENT));
			}
			else if (intent.hasExtra(PushManager.UNREGISTER_EVENT))
			{
				doOnUnregistered(intent.getExtras().getString(PushManager.UNREGISTER_EVENT));
			}
			else if (intent.hasExtra(PushManager.REGISTER_ERROR_EVENT))
			{
				doOnRegisteredError(intent.getExtras().getString(PushManager.REGISTER_ERROR_EVENT));
			}
			else if (intent.hasExtra(PushManager.UNREGISTER_ERROR_EVENT))
			{
				doOnUnregisteredError(intent.getExtras().getString(PushManager.UNREGISTER_ERROR_EVENT));
			}

			resetIntentValues();
		}
	}

	public void doOnRegistered(String registrationId)
	{
		mGeneralStatus.setText(getString(R.string.registered, registrationId));
		mSendPushButton.setEnabled(true);
	}

	public void doOnRegisteredError(String errorId)
	{
		mGeneralStatus.setText(getString(R.string.registered_error, errorId));
	}

	public void doOnUnregistered(String registrationId)
	{
		mGeneralStatus.setText(getString(R.string.unregistered, registrationId));
	}

	public void doOnUnregisteredError(String errorId)
	{
		mGeneralStatus.setText(getString(R.string.unregistered_error, errorId));
	}

	public void doOnMessageReceive(String message)
	{
		mGeneralStatus.setText(getString(R.string.on_message, message));

		// Parse custom JSON data string.
		// You can set background color with custom JSON data in the following format: { "r" : "10", "g" : "200", "b" : "100" }
		// Or open specific screen of the app with custom page ID (set ID in the { "id" : "2" } format)
		try
		{
			JSONObject messageJson = new JSONObject(message);
			JSONObject customJson = new JSONObject(messageJson.getString("u"));

			if (customJson.has("r") && customJson.has("g") && customJson.has("b"))
			{
				int r = customJson.getInt("r");
				int g = customJson.getInt("g");
				int b = customJson.getInt("b");
				View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
				rootView.setBackgroundColor(Color.rgb(r, g, b));
			}
			if (customJson.has("id"))
			{
				Intent intent = new Intent(this, SecondActivity.class);
				intent.putExtra(PushManager.PUSH_RECEIVE_EVENT, messageJson.toString());
				startActivity(intent);
			}
		}
		catch (JSONException e)
		{
			// No custom JSON. Pass this exception
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
}
