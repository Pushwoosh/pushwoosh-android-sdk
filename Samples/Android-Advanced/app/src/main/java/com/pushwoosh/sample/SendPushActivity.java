package com.pushwoosh.sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.pushwoosh.PushManager;
import com.pushwoosh.sender.PushMessage;
import com.pushwoosh.sender.PushSender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SendPushActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_push);

        final CheckBox broadcastCheck = (CheckBox) findViewById(R.id.check_broadcast);
        final EditText pushText = (EditText) findViewById(R.id.text_push);

        findViewById(R.id.butt_push).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pushToken = PushManager.getPushToken(SendPushActivity.this);
                boolean broadcast = broadcastCheck.isChecked();

                PushSender sender = new PushSender(SendPushActivity.this, getString(R.string.api_access_token));
                PushMessage message = new PushMessage();
                message.content = pushText.getText().toString();

                if (!broadcast) {
                    List<String> devices = new ArrayList<String>();
                    devices.add(pushToken);
                    Map<String, Object> notification = new HashMap<String, Object>();
                    notification.put("devices", devices);
                    message.notificationParams = notification;
                }

                sender.sendPush(message);
            }
        });
    }
}
