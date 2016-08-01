package com.pushwoosh.sample.customcontent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.pushwoosh.PushManager;

public class NotificationButtonReceiver extends BroadcastReceiver
{
    public NotificationButtonReceiver()
    {
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d("Pushwoosh", "notificatoin button clicked");

        PushManager.clearNotificationCenter(context);

        Intent actionIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.pushwoosh.com/"));
        actionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(actionIntent);
    }
}
