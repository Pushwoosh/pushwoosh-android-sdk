package com.pushwoosh.sample.messenger;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.text.TextUtils;

import com.pushwoosh.PushManager;
import com.pushwoosh.notification.DefaultNotificationFactory;
import com.pushwoosh.notification.PushData;
import com.pushwoosh.sample.R;

import org.json.JSONObject;

import java.util.List;

public class MessagingNotificationFactory extends DefaultNotificationFactory
{
    public static final String KEY_TEXT_REPLY = "key_text_reply";

    private String getSenderHwid(PushData pushData)
    {
        Bundle extras = pushData.getExtras();
        PushManager pushManager = PushManager.getInstance(getContext());
        String config = pushManager.getCustomData(extras);

        if (config == null)
        {
            return null;
        }

        try
        {
            JSONObject jsonConfig = new JSONObject(config);
            return jsonConfig.optString("from");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    private NotificationCompat.Style buildStyle(PushData pushData)
    {
        String senderHwid = getSenderHwid(pushData);

        if (TextUtils.equals(PushManager.getPushwooshHWID(getContext()), senderHwid))
        {
            MessageStorage.addMessage(getContext(), new Message(pushData.getMessage(), System.currentTimeMillis(), null));
        }
        else
        {
            MessageStorage.addMessage(getContext(), new Message(pushData.getMessage(), System.currentTimeMillis(), "Pushwoosh"));
        }
        List<Message> history = MessageStorage.getHistory(getContext());

        NotificationCompat.MessagingStyle notificationStyle = new NotificationCompat.MessagingStyle("User").setConversationTitle("Pushwoosh chat");
        for (Message message : history)
        {
            notificationStyle.addMessage(message.getText(), message.getTs(), message.getSender());
        }

        return notificationStyle;
    }

    private NotificationCompat.Action buildReplyAction()
    {
        Intent actionIntent = new Intent();
        actionIntent.setClass(getContext(), MessagingService.class);

        PendingIntent replyIntent = PendingIntent.getService(getContext(), 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_REPLY)
                .setLabel("Type message")
                .build();

        return new NotificationCompat.Action.Builder(0, "Reply", replyIntent)
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build();
    }

    @Override
    public Notification onGenerateNotification(PushData pushData)
    {
        // RemoteInput and Messaging style is supported only since Android N
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
        {
            return super.onGenerateNotification(pushData);
        }

        NotificationCompat.Style notificationStyle = buildStyle(pushData);
        NotificationCompat.Action replyAction = buildReplyAction();

        Notification notification = new NotificationCompat.Builder(getContext())
                .setContentTitle(getContentFromHtml(pushData.getHeader()))
                .setContentText(getContentFromHtml(pushData.getMessage()))
                .setSmallIcon(pushData.getSmallIcon())
                .setTicker(getContentFromHtml(pushData.getTicker()))
                .setWhen(System.currentTimeMillis())
                .setStyle(notificationStyle)
                .addAction(replyAction)
                .build();

        addSound(notification, pushData.getSound());

        return notification;
    }
}
