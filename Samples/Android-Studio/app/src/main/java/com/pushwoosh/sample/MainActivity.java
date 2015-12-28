package com.pushwoosh.sample;



import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.pushwoosh.PushManager;
import com.pushwoosh.PushManager.GetTagsListener;
import com.pushwoosh.PushManager.RichPageListener;
import com.pushwoosh.BasePushMessageReceiver;
import com.pushwoosh.BaseRegistrationReceiver;


public class MainActivity extends Activity
{
    private static final String LTAG = "PushwooshSample";

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
            Log.e(LTAG, "Success: get Tags " + tags.toString());
        }

        @Override
        public void onError(Exception e)
        {
            Log.e(LTAG, "ERROR: get Tags " + e.getMessage());
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

        setContentView(R.layout.activity_main);

        //NetworkUtils.useSSL = true;

        //Register receivers for push notifications
        registerReceivers();

        final PushManager pushManager = PushManager.getInstance(this);

        class RichPageListenerImpl implements RichPageListener
        {
            @Override
            public void onRichPageAction(String actionParams)
            {
                Log.d(LTAG, "Rich page action: " + actionParams);
            }

            @Override
            public void onRichPageClosed()
            {
                Log.d(LTAG, "Rich page closed");
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
            Log.e(LTAG, e.getLocalizedMessage());
        }

        //Register for push!
        pushManager.registerForPushNotifications();

        //check launch notification (optional)
        String launchNotificatin = pushManager.getLaunchNotification();
        if (launchNotificatin != null)
        {
            Log.d(LTAG, "Launch notification received: " + launchNotificatin);
        }
        else
        {
            Log.d(LTAG, "No launch notification received");
        }

        //Clear application icon badge number
        pushManager.setBadgeNumber(0);

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
        Log.d(LTAG, "registered: " + registrationId);
    }

    public void doOnRegisteredError(String errorId)
    {
        Log.d(LTAG, "registration error: " + errorId);
    }

    public void doOnUnregistered(String registrationId)
    {
        Log.d(LTAG, "unregistered: " + registrationId);
    }

    public void doOnUnregisteredError(String errorId)
    {
        Log.d(LTAG, "deregistration error: " + errorId);
    }

    public void doOnMessageReceive(String message)
    {
        Log.d(LTAG, "received push: " + message);
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
