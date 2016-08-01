package com.pushwoosh.sample.customcontent;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.util.Log;
import android.widget.RemoteViews;

import com.pushwoosh.notification.AbsNotificationFactory;
import com.pushwoosh.notification.DefaultNotificationFactory;
import com.pushwoosh.notification.PushData;
import com.pushwoosh.sample.R;

public class CustomContentNotificationFactory extends DefaultNotificationFactory
{
    private RemoteViews buildContentView(PushData pushData)
    {
        RemoteViews contentView = new RemoteViews(getContext().getPackageName(), R.layout.notification);

        Intent intent = new Intent();
        intent.setAction(getContext().getPackageName() + ".action.NOTIFICATION_BUTTON");
        contentView.setOnClickPendingIntent(R.id.notification_button, PendingIntent.getBroadcast(getContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
        contentView.setImageViewResource(R.id.notification_button, R.drawable.ic_notification);

        contentView.setTextViewText(R.id.notification_text, pushData.getMessage());

        return contentView;
    }

    @Override
    public Notification onGenerateNotification(PushData pushData)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
        {
            // fallback to default
            return super.onGenerateNotification(pushData);
        }

        RemoteViews contentView = buildContentView(pushData);

        Notification.Builder notificationBuilder = new Notification.Builder(getContext())
                .setContentTitle(getContentFromHtml(pushData.getHeader()))
                .setContentText(getContentFromHtml(pushData.getMessage()))
                .setSmallIcon(pushData.getSmallIcon())
                .setTicker(getContentFromHtml(pushData.getTicker()))
                .setWhen(System.currentTimeMillis())
                .setColor(0xFF0EA7ED)
                .setPriority(Notification.PRIORITY_HIGH);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        {
            notificationBuilder.setCustomContentView(contentView);
            notificationBuilder.setCustomBigContentView(contentView);
            notificationBuilder.setStyle(new Notification.DecoratedCustomViewStyle());
        }
        else
        {
            notificationBuilder.setContent(contentView);
        }

        final Notification notification = notificationBuilder.build();

        addSound(notification, pushData.getSound());
        addVibration(notification, pushData.getVibration());
        addCancel(notification);

        return notification;
    }
}
