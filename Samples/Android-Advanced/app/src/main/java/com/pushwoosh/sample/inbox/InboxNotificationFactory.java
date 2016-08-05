package com.pushwoosh.sample.inbox;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.pushwoosh.notification.DefaultNotificationFactory;
import com.pushwoosh.notification.PushData;
import com.pushwoosh.sample.inbox.Message;
import com.pushwoosh.sample.inbox.MessageStorage;

import java.util.ArrayList;

public class InboxNotificationFactory extends DefaultNotificationFactory
{
    public static final String GROUP_KEY = "PushwooshGroup";

    private NotificationCompat.Style generateInboxStyle(PushData pushData)
    {
        MessageStorage.addMessage(getContext(), new Message(pushData.getMessage(), System.currentTimeMillis(), pushData.getHeader()));
        ArrayList<Message> messages = MessageStorage.getHistory(getContext());

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle().
                setBigContentTitle(pushData.getHeader() + " Details");

        for (Message message : messages)
        {
            style.addLine(getContentFromHtml("<b>" + message.getSender() + "</b> " + message.getText()));
        }

        if (messages.size() > 7)
        {
            style.setSummaryText("+ " + (messages.size() - 7) + " more");
        }

        return style;
    }

    private void generateSummary(PushData pushData)
    {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getContext())
                .setContentTitle(getContentFromHtml(pushData.getHeader()))
                .setContentText(getContentFromHtml(pushData.getMessage()))
                .setSmallIcon(pushData.getSmallIcon())
                .setTicker(getContentFromHtml(pushData.getTicker()))
                .setWhen(System.currentTimeMillis())
                .setStyle(generateInboxStyle(pushData))
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(getContext(), 0, getNotifyIntent(), PendingIntent.FLAG_CANCEL_CURRENT));

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getContext());
        notificationManager.notify(GROUP_KEY, 0, notificationBuilder.build());
    }

    @Override
    public Notification onGenerateNotification(PushData pushData)
    {
        // Summary will be displayed only on devices that do not support grouped notifications
        generateSummary(pushData);

        // This notification will be displayed only on devices that support grouped notifications
        Notification notification = new NotificationCompat.Builder(getContext())
                .setContentTitle(getContentFromHtml(pushData.getHeader()))
                .setContentText(getContentFromHtml(pushData.getMessage()))
                .setSmallIcon(pushData.getSmallIcon())
                .setTicker(getContentFromHtml(pushData.getTicker()))
                .setWhen(System.currentTimeMillis())
                .setGroup(GROUP_KEY)
                .setAutoCancel(true)
                .build();

        addSound(notification, pushData.getSound());

        return notification;
    }

    @Override
    public void onPushHandle(Activity activity)
    {
        MessageStorage.clear(getContext());
    }
}
