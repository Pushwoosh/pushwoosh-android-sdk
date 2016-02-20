package com.pushwoosh.sample.quickstart;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.pushwoosh.fragment.PushEventListener;
import com.pushwoosh.fragment.PushFragment;


public class MainActivity extends FragmentActivity implements PushEventListener
{
    private String TAG = "PushwooshSample";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Init Pushwoosh fragment
        PushFragment.init(this);
    }

    @Override
    public void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        //Check if we've got new intent with a push notification
        PushFragment.onNewIntent(this, intent);
    }

    @Override
    public void doOnRegistered(String registrationId)
    {
        Log.i(TAG, "Registered for pushes: " + registrationId);
    }

    @Override
    public void doOnRegisteredError(String errorId)
    {
        Log.e(TAG, "Failed to register for pushes: " + errorId);
    }

    @Override
    public void doOnMessageReceive(String message)
    {
        Log.i(TAG, "Notification opened: " + message);
    }

    @Override
    public void doOnUnregistered(final String message)
    {
        Log.i(TAG, "Unregistered from pushes: " + message);
    }

    @Override
    public void doOnUnregisteredError(String errorId)
    {
        Log.e(TAG, "Failed to unregister from pushes: " + errorId);
    }
}
