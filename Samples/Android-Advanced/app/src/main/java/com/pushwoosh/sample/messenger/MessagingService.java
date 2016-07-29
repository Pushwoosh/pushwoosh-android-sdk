package com.pushwoosh.sample.messenger;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import com.pushwoosh.PushManager;
import com.pushwoosh.sample.R;
import com.pushwoosh.sender.PushMessage;
import com.pushwoosh.sender.PushSender;

import java.util.HashMap;
import java.util.Map;

public class MessagingService extends IntentService
{
    public MessagingService()
    {
        super(MessagingService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null)
        {
            CharSequence message = remoteInput.getCharSequence(MessagingNotificationFactory.KEY_TEXT_REPLY);
            Log.d("PushwooshSample", "Posted message: " + message);

            if (message == null)
            {
                return;
            }

            // Broadcast message to all subscribers
            // NOTE: the following code is not a recommended way of implementing upstream messages
            // Please use https://developers.google.com/cloud-messaging/upstream for this
            PushSender sender = new PushSender(this, getString(R.string.api_access_token));
            PushMessage pushMessage = new PushMessage();
            pushMessage.content = message.toString();
            Map<String, Object> notificationParams = new HashMap<String, Object>();
            Map<String, Object> customData = new HashMap<String, Object>();

            // hwid as additional data will allow to determine if received notification was originally posted be the same device
            customData.put("from", PushManager.getPushwooshHWID(this));
            notificationParams.put("data", customData);
            pushMessage.notificationParams = notificationParams;

            sender.sendPush(pushMessage);
        }
    }
}
