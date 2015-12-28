package com.pushwoosh.test.tags.sample.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;

import com.pushwoosh.notification.AbsNotificationFactory;
import com.pushwoosh.notification.PushData;

import android.net.Uri;
import android.support.v4.app.NotificationCompat;

/**
 * Notification factory sample for remote notification actions
 * Android root params example : { "my_actions" : [ { "title" : "Pushwoosh", "url" : "https://www.pushwoosh.com"  } ] }
 */
public class NotificationFactorySample extends AbsNotificationFactory
{
	@Override
	public Notification onGenerateNotification(PushData pushData)
	{
		//create notification builder
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getContext());
		
		//set title of notification
		 notificationBuilder.setContentTitle(getContentFromHtml(pushData.getHeader()));
		
		//set content of notification
		  notificationBuilder.setContentText(getContentFromHtml(pushData.getMessage()));
		
		//set small icon (usually app icon)
		notificationBuilder.setSmallIcon(pushData.getSmallIcon());
		
		//set ticket text
		notificationBuilder.setTicker(getContentFromHtml(pushData.getTicker()));
		
		//display notification now
		notificationBuilder.setWhen(System.currentTimeMillis());
		
		//add actions to the notification
		addRemoteActions(notificationBuilder, pushData);
		
		if (pushData.getBigPicture() != null)
		{
		    //set big image if available
			notificationBuilder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(pushData.getBigPicture()).setSummaryText(getContentFromHtml(pushData.getMessage())));
		}
		else
		{
		    //otherwise it's big text style
		    notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getContentFromHtml(pushData.getMessage())));
		}
		
		//support icon background color
		if (pushData.getIconBackgroundColor() != null)
		{
		      notificationBuilder.setColor(pushData.getIconBackgroundColor());
		}
		
		//support custom icon
		if (null != pushData.getLargeIcon())
		{
		    notificationBuilder.setLargeIcon(pushData.getLargeIcon());
		}
		
		//build the notification
		final Notification notification = notificationBuilder.build();
		
		//add sound
		addSound(notification, pushData.getSound());
		
		//add vibration
		addVibration(notification, pushData.getVibration());
		
		//make it cancelable
		addCancel(notification);
		
		//all done!
		return notification;
	}
	
	private void addRemoteActions(NotificationCompat.Builder notificationBuilder, PushData pushData)
	{
		String actions = pushData.getExtras().getString("my_actions");
		if (actions != null)
		{
			try
			{
				JSONArray jsonArray = new JSONArray(actions);
				for (int i = 0; i < jsonArray.length(); ++i)
				{
					JSONObject json = jsonArray.getJSONObject(i);
					String title = json.getString("title");
					String url = json.getString("url");
					Intent actionIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					notificationBuilder.addAction(new NotificationCompat.Action(0, title, PendingIntent.getActivity(getContext(), 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT)));
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onPushReceived(PushData pushData)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPushHandle(Activity activity)
	{
		// TODO Auto-generated method stub
		
	}
}
