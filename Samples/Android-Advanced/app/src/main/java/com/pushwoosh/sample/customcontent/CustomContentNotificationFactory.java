package com.pushwoosh.sample.customcontent;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.notification.PushwooshNotificationFactory;
import com.pushwoosh.sample.R;

public class CustomContentNotificationFactory extends PushwooshNotificationFactory
{
    private RemoteViews buildContentView(PushMessage pushData)
    {
        Context context = getApplicationContext();
        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification);

        Intent intent = new Intent();
        intent.setAction(context.getPackageName() + ".action.NOTIFICATION_BUTTON");
        contentView.setOnClickPendingIntent(R.id.notification_button, PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
        contentView.setImageViewResource(R.id.notification_button, R.drawable.ic_notification);

        contentView.setTextViewText(R.id.notification_text, pushData.getMessage());

        return contentView;
    }

    @Override
    public Notification onGenerateNotification(PushMessage pushData)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
        {
            // fallback to default
            return super.onGenerateNotification(pushData);
        }

        RemoteViews contentView = buildContentView(pushData);

        Notification.Builder notificationBuilder = new Notification.Builder(getApplicationContext())
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
