package com.pushwoosh.notification;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;

public class NotificationOpenActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NotificationIntentHelper.processIntent(this, getIntent());
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        NotificationIntentHelper.processIntent(this, intent);
        finish();
    }
}
