package com.pushwoosh.test.tags.sample.app;

import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import com.pushwoosh.PushManager;
import com.pushwoosh.PushManager.GetTagsListener;
import com.pushwoosh.PushManager.RichPageListener;
import com.pushwoosh.BasePushMessageReceiver;
import com.pushwoosh.BaseRegistrationReceiver;

public class MainActivity extends FragmentActivity implements SendTagsCallBack
{
	private static final String SEND_TAGS_STATUS_FRAGMENT_TAG = "send_tags_status_fragment_tag";

	private TextView mTagsStatus;
	private EditText mIntTags;
	private EditText mStringTags;
	private Button mSubmitTagsButton;
	private TextView mGeneralStatus;

	boolean broadcastPush = true;

	public class GetTagsListenerImpl implements GetTagsListener
	{
		@Override
		public void onTagsReceived(Map<String, Object> tags)
		{
			Log.e("Pushwoosh", "Success: get Tags " + tags.toString());
		}

		@Override
		public void onError(Exception e)
		{
			Log.e("Pushwoosh", "ERROR: get Tags " + e.getMessage());
		}
	}

	GetTagsListenerImpl tagsListener = new GetTagsListenerImpl();

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		//NetworkUtils.useSSL = true;

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
		
		// send local notification
		PushManager.scheduleLocalNotification(getApplicationContext(), "PushwooshSample started", 5);

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

		mGeneralStatus = (TextView) findViewById(R.id.general_status);
		mTagsStatus = (TextView) findViewById(R.id.status);
		mIntTags = (EditText) findViewById(R.id.tag_int);
		mStringTags = (EditText) findViewById(R.id.tag_string);

		//Check for start push notification in the Intent payload
		checkMessage(getIntent());

		mSubmitTagsButton = (Button) findViewById(R.id.submit_tags);
		mSubmitTagsButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				//PushManager.getTagsAsync(MainActivity.this, tagsListener);
				checkAndSendTagsIfWeCan();
			}
		});

		SendTagsFragment sendTagsFragment = getSendTagsFragment();
		mTagsStatus.setText(sendTagsFragment.getSendTagsStatus());
		mSubmitTagsButton.setEnabled(sendTagsFragment.canSendTags());

		// Start/stop geo pushes
		findViewById(R.id.butt_start_goe_pushes).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				pushManager.startTrackingGeoPushes();
			}
		});
		findViewById(R.id.butt_stop_goe_pushes).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				pushManager.stopTrackingGeoPushes();
			}
		});
		
		//Clear application badge number
		//pushManager.setBadgeNumber(0);
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

		if (broadcastPush)
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

	@Override
	public void onStatusChange(int sendTagsStatus)
	{
		mTagsStatus.setText(sendTagsStatus);
	}

	@Override
	public void onTaskEnds()
	{
		mSubmitTagsButton.setEnabled(true);
	}

	@Override
	public void onTaskStarts()
	{
		mSubmitTagsButton.setEnabled(false);
	}

	private void checkAndSendTagsIfWeCan()
	{
		SendTagsFragment sendTagsFragment = getSendTagsFragment();

		if (sendTagsFragment.canSendTags())
		{
			sendTagsFragment.submitTags(this, mIntTags.getText().toString().trim(), mStringTags.getText().toString().trim());
		}
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

	private SendTagsFragment getSendTagsFragment()
	{
		FragmentManager fragmentManager = getSupportFragmentManager();
		SendTagsFragment sendTagsFragment =
				(SendTagsFragment) fragmentManager.findFragmentByTag(SEND_TAGS_STATUS_FRAGMENT_TAG);

		if (null == sendTagsFragment)
		{
			sendTagsFragment = new SendTagsFragment();
			sendTagsFragment.setRetainInstance(true);
			fragmentManager.beginTransaction().add(sendTagsFragment, SEND_TAGS_STATUS_FRAGMENT_TAG).commit();
			fragmentManager.executePendingTransactions();
		}

		return sendTagsFragment;
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		mIntTags = null;
		mStringTags = null;
		mTagsStatus = null;
		mSubmitTagsButton = null;
	}
}
