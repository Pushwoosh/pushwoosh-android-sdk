package com.pushwoosh;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.pushwoosh.internal.event.Event;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.utils.PWLog;

public class BootReceiver extends BroadcastReceiver {

	public static class DeviceBootedEvent implements Event {
		private DeviceBootedEvent() {/*do nothing*/}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			if (intent == null || !TextUtils.equals(Intent.ACTION_BOOT_COMPLETED, intent.getAction())) {
				PWLog.warn("BootReceiver", "Received unexpected action");
				return;
			}

			EventBus.sendEvent(new DeviceBootedEvent());
		} catch (Exception e) {
			PWLog.exception(e);
		}
	}
}
