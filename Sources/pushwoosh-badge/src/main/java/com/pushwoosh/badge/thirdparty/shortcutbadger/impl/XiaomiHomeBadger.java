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

package com.pushwoosh.badge.thirdparty.shortcutbadger.impl;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import com.pushwoosh.badge.thirdparty.shortcutbadger.Badger;
import com.pushwoosh.badge.thirdparty.shortcutbadger.ShortcutBadgeException;
import com.pushwoosh.badge.thirdparty.shortcutbadger.util.BroadcastHelper;


/**
 * @author leolin
 */
@Deprecated
public class XiaomiHomeBadger implements Badger {

    public static final String INTENT_ACTION = "android.intent.action.APPLICATION_MESSAGE_UPDATE";
    public static final String EXTRA_UPDATE_APP_COMPONENT_NAME = "android.intent.extra.update_application_component_name";
    public static final String EXTRA_UPDATE_APP_MSG_TEXT = "android.intent.extra.update_application_message_text";
    private ResolveInfo resolveInfo;

    @Override
    public void executeBadge(Context context, ComponentName componentName, int badgeCount) throws ShortcutBadgeException {
        try {
            Class miuiNotificationClass = Class.forName("android.app.MiuiNotification");
            Object miuiNotification = miuiNotificationClass.newInstance();
            Field field = miuiNotification.getClass().getDeclaredField("messageCount");
            field.setAccessible(true);
            try {
                field.set(miuiNotification, String.valueOf(badgeCount == 0 ? "" : badgeCount));
            } catch (Exception e) {
                field.set(miuiNotification, badgeCount);
            }
        } catch (Exception e) {
            Intent localIntent = new Intent(
                    INTENT_ACTION);
            localIntent.putExtra(EXTRA_UPDATE_APP_COMPONENT_NAME, componentName.getPackageName() + "/" + componentName.getClassName());
            localIntent.putExtra(EXTRA_UPDATE_APP_MSG_TEXT, String.valueOf(badgeCount == 0 ? "" : badgeCount));
            if (BroadcastHelper.canResolveBroadcast(context, localIntent)) {
                context.sendBroadcast(localIntent);
            }
        }
        if (Build.MANUFACTURER.equalsIgnoreCase("Xiaomi")) {
            tryNewMiuiBadge(context, badgeCount);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void tryNewMiuiBadge(Context context, int badgeCount) throws ShortcutBadgeException {
        if (resolveInfo == null) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            resolveInfo = context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        }

        if (resolveInfo != null) {
            NotificationManager mNotificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            Notification.Builder builder = new Notification.Builder(context)
                    .setContentTitle("")
                    .setContentText("")
                    .setSmallIcon(resolveInfo.getIconResource());
            Notification notification = builder.build();
            try {
                Field field = notification.getClass().getDeclaredField("extraNotification");
                Object extraNotification = field.get(notification);
                Method method = extraNotification.getClass().getDeclaredMethod("setMessageCount", int.class);
                method.invoke(extraNotification, badgeCount);
                mNotificationManager.notify(0, notification);
            } catch (Exception e) {
                throw new ShortcutBadgeException("not able to set badge", e);
            }
        }
    }

    @Override
    public List<String> getSupportLaunchers() {
        return Arrays.asList(
                "com.miui.miuilite",
                "com.miui.home",
                "com.miui.miuihome",
                "com.miui.miuihome2",
                "com.miui.mihome",
                "com.miui.mihome2",
                "com.i.miui.launcher"
        );
    }
}
