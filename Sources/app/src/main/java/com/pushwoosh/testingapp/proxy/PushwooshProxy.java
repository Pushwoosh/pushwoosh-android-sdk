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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.os.Bundle;

import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.location.network.exception.LocationNotAvailableException;
import com.pushwoosh.notification.SoundType;
import com.pushwoosh.notification.VibrateType;
import com.pushwoosh.testingapp.helpers.GetTagsCallback;

public interface PushwooshProxy {
	void registerForPushNotifications();

	void unregisterForPushNotifications();

	String getCustomData(Bundle pushBoundle);

	void setBadgeNumber(int badgeNumber);

	void addBadgeNumber(int deltaBadge);

	int getBadgeNumber();

	void setUserId(String userId);

	void setEmails(ArrayList<String> emails);

	void sendTags(String tagName, Integer tagIntValue, Callback<Void, PushwooshException> callback);

	void trackInAppRequest(String sku, BigDecimal price, String currency, Date purchaseTime);

	int scheduleLocalNotification(String message, int seconds);

	void clearLocalNotification(int id);

	void clearLocalNotifications();

	void clearNotificationCenter();

	List<String> getPushHistory();

	void clearPushHistory();

	void setSoundNotificationType(SoundType soundNotificationType);

	void setVibrateNotificationType(VibrateType vibrateNotificationType);

	void setColorLED(int color);

	void setSimpleNotificationMode();

	void setMultiNotificationMode();

	void setLightScreenOnNotification(boolean lightsOn);

	void setEnableLED(boolean ledOn);

	void startTrackingGeoPushes(Callback<Void, LocationNotAvailableException> callback);

	void stopTrackingGeoPushes();

	String getPushToken();

	String getPushwooshHWID();

	String getLaunchNotification();

	void getTagsAsync(GetTagsCallback callback);

	void initPushwoosh(Context context);
}
