/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.testingapp.proxy;

import android.content.Context;
import android.os.Bundle;
import androidx.core.app.NotificationManagerCompat;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.RegisterForPushNotificationsResultData;
import com.pushwoosh.badge.PushwooshBadge;
import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.exception.RegisterForPushNotificationsException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.location.PushwooshLocation;
import com.pushwoosh.location.network.exception.LocationNotAvailableException;
import com.pushwoosh.notification.LocalNotification;
import com.pushwoosh.notification.LocalNotificationReceiver;
import com.pushwoosh.notification.LocalNotificationRequest;
import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.notification.PushwooshNotificationSettings;
import com.pushwoosh.notification.SoundType;
import com.pushwoosh.notification.VibrateType;
import com.pushwoosh.tags.TagsBundle;
import com.pushwoosh.testingapp.helpers.GetTagsCallback;
import com.pushwoosh.testingapp.helpers.ShowMessageHelper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CurrentPushwoosh implements PushwooshProxy {

	private Context context;
	private static final Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> CALLBACK = result -> {
		if (result.isSuccess()) {
			ShowMessageHelper.showMessage("Registration success:" + "\n" + result.getData().getToken() + " " + result.getData().isEnabled());
		} else {
			ShowMessageHelper.showMessage("Registration error:" + "\n" + result.getException());
		}
	};

	CurrentPushwoosh(Context context) {
		this.context = context;
	}

	@Override
	public void registerForPushNotifications() {
		//noinspection unchecked
		Pushwoosh.getInstance().registerForPushNotifications(CALLBACK);
	}

	@Override
	public void unregisterForPushNotifications() {
		Pushwoosh.getInstance().unregisterForPushNotifications();
	}

	@Override
	public String getCustomData(Bundle pushBoundle) {
		PushMessage message = new PushMessage(pushBoundle);
		return message.getCustomData();
	}

	@Override
	public void setBadgeNumber(int badgeNumber) {
		PushwooshBadge.setBadgeNumber(badgeNumber);
	}

	@Override
	public void addBadgeNumber(int deltaBadge) {
		PushwooshBadge.addBadgeNumber(deltaBadge);
	}

	@Override
	public int getBadgeNumber() {
		return PushwooshBadge.getBadgeNumber();
	}

	@Override
	public void setUserId(String userId) {
		Pushwoosh.getInstance().setUserId(userId);
	}

	@Override
	public void setEmails(ArrayList<String> emails) { Pushwoosh.getInstance().setEmail(emails); }

	@Override
	public void sendTags(String tagName, Integer tagIntValue, Callback<Void, PushwooshException> callback) {
		TagsBundle tag = new TagsBundle.Builder().putInt(tagName, tagIntValue)
				.build();
		Pushwoosh.getInstance().setTags(tag, callback);
	}

	@Override
	public void trackInAppRequest(String sku, BigDecimal price, String currency, Date purchaseTime) {
		Pushwoosh.getInstance().sendInappPurchase(sku, price, currency);
	}

	@Override
	public int scheduleLocalNotification(String message, int seconds) {
		LocalNotification notification = new LocalNotification.Builder().setMessage(message)
				.setDelay(seconds)
				.build();
		LocalNotificationRequest request = Pushwoosh.getInstance().scheduleLocalNotification(notification);
		return request.getRequestId();
	}

	@Override
	public void clearLocalNotification(int id) {
		LocalNotificationRequest request = new LocalNotificationRequest(id);
		request.cancel();
	}

	@Override
	public void clearLocalNotifications() {
		LocalNotificationReceiver.cancelAll();
	}

	@Override
	public void clearNotificationCenter() {
		NotificationManagerCompat.from(context).cancelAll();
	}

	@Override
	public List<String> getPushHistory() {
		List<PushMessage> messages = Pushwoosh.getInstance().getPushHistory();
		ArrayList<String> result = new ArrayList<>();
		for (PushMessage message : messages) {
			result.add(message.toJson().toString());
		}
		return result;
	}

	@Override
	public void clearPushHistory() {
		Pushwoosh.getInstance().clearPushHistory();
	}

	@Override
	public void setSoundNotificationType(SoundType soundNotificationType) {
		PushwooshNotificationSettings.setSoundNotificationType(soundNotificationType);
	}

	@Override
	public void setVibrateNotificationType(VibrateType vibrateNotificationType) {
		PushwooshNotificationSettings.setVibrateNotificationType(vibrateNotificationType);
	}

	@Override
	public void setColorLED(int color) {
		PushwooshNotificationSettings.setColorLED(color);
	}

	@Override
	public void setSimpleNotificationMode() {
		PushwooshNotificationSettings.setMultiNotificationMode(false);
	}

	@Override
	public void setMultiNotificationMode() {
		PushwooshNotificationSettings.setMultiNotificationMode(true);
	}

	@Override
	public void setLightScreenOnNotification(boolean lightsOn) {
		PushwooshNotificationSettings.setLightScreenOnNotification(lightsOn);
	}

	@Override
	public void setEnableLED(boolean ledOn) {
		PushwooshNotificationSettings.setEnableLED(ledOn);
	}

	@Override
	public void startTrackingGeoPushes(Callback<Void, LocationNotAvailableException> callback) {
		PushwooshLocation.startLocationTracking(result -> {
			if (result.isSuccess()) {
				ShowMessageHelper.showMessage("Success change location tracking.");
				PushwooshLocation.requestBackgroundLocationPermission();
			} else {
				ShowMessageHelper.showMessage("Failed change location tracking.");
			}
			callback.process(result);
		});
	}

	@Override
	public void stopTrackingGeoPushes() {
		PushwooshLocation.stopLocationTracking();
	}

	@Override
	public String getPushToken() {
		return Pushwoosh.getInstance().getPushToken();
	}

	@Override
	public String getPushwooshHWID() {
		return Pushwoosh.getInstance().getHwid();
	}

	@Override
	public String getLaunchNotification() {
		PushMessage msg = Pushwoosh.getInstance().getLaunchNotification();
		if (msg != null) {
			return msg.toJson().toString();
		}
		return "";
	}

	@Override
	public void getTagsAsync(GetTagsCallback getTagsCallback) {
		Pushwoosh.getInstance().getTags(result -> {
			if (result.isSuccess()) {
				TagsBundle tags = result.getData();
				if (tags != null) {
					getTagsCallback.process(tags.toJson().toString());
				}
			} else {
				if (result.getException() != null) {
					getTagsCallback.process(result.getException().getMessage());
				}
			}
		});
	}

	@Override
	public void initPushwoosh(Context context) {
		//stub
	}
}
