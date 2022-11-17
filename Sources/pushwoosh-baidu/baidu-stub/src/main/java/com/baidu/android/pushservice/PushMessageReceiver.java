package com.baidu.android.pushservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

public abstract class PushMessageReceiver extends BroadcastReceiver {
    public abstract void onBind(Context context, int i, String str, String str2, String str3, String str4);

    public abstract void onDelTags(Context context, int i, List<String> list, List<String> list2, String str);

    public abstract void onListTags(Context context, int i, List<String> list, String str);

    public abstract void onMessage(Context context, String str, String str2);

    public abstract void onNotificationArrived(Context context, String str, String str2, String str3);

    public abstract void onNotificationClicked(Context context, String str, String str2, String str3);

    public abstract void onSetTags(Context context, int i, List<String> list, List<String> list2, String str);

    public abstract void onUnbind(Context context, int i, String str);

    public final void onReceive(Context context, Intent intent) {
        throw new RuntimeException("Stub!");
    }
}
