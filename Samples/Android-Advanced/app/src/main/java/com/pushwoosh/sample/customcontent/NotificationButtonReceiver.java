package com.pushwoosh.sample.customcontent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

public class NotificationButtonReceiver extends BroadcastReceiver {
    public NotificationButtonReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Pushwoosh", "notificatoin button clicked");

        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
        managerCompat.cancelAll();

        Intent actionIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.pushwoosh.com/"));
        actionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(actionIntent);
    }
}
