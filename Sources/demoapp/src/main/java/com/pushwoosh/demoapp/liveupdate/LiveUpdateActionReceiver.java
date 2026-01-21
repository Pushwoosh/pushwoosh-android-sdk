package com.pushwoosh.demoapp.liveupdate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * Handles action button clicks from Live Update notifications.
 */
public class LiveUpdateActionReceiver extends BroadcastReceiver {
    private static final String TAG = "LiveUpdateAction";

    public static final String ACTION_CANCEL = "com.pushwoosh.demoapp.ACTION_CANCEL_ORDER";
    public static final String ACTION_TIP = "com.pushwoosh.demoapp.ACTION_ADD_TIP";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Action received: " + action);

        if (ACTION_CANCEL.equals(action)) {
            handleCancel(context);
        } else if (ACTION_TIP.equals(action)) {
            handleTip(context);
        }
    }

    private void handleCancel(Context context) {
        Toast.makeText(context, "Order cancelled!", Toast.LENGTH_SHORT).show();
        LiveUpdateDemo.getInstance(context).cancel();
    }

    private void handleTip(Context context) {
        Toast.makeText(context, "Thanks for the tip! 🎉", Toast.LENGTH_SHORT).show();
    }
}
