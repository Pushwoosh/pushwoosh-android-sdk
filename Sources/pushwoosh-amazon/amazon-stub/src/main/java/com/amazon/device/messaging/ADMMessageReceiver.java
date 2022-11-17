//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.amazon.device.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public abstract class ADMMessageReceiver extends BroadcastReceiver {
	protected ADMMessageReceiver() {
		throw new RuntimeException("Stub! You are bundling a stubbed jar in the apk! Please move it to the classpath instead.");
	}

	protected ADMMessageReceiver(Class<? extends ADMMessageHandlerBase> serviceClass) {
		throw new RuntimeException("Stub! You are bundling a stubbed jar in the apk! Please move it to the classpath instead.");
	}

	protected ADMMessageReceiver(Class<? extends ADMMessageHandlerJobBase> serviceClass, int jobId) {
		throw new RuntimeException("Stub! You are bundling a stubbed jar in the apk! Please move it to the classpath instead.");
	}

	protected void registerIntentServiceClass(Class<? extends ADMMessageHandlerBase> serviceClass) {
		throw new RuntimeException("Stub! You are bundling a stubbed jar in the apk! Please move it to the classpath instead.");
	}

	protected void registerJobServiceClass(Class<? extends ADMMessageHandlerJobBase> serviceClass, int jobId) {
		throw new RuntimeException("Stub! You are bundling a stubbed jar in the apk! Please move it to the classpath instead.");
	}

	public final void onReceive(Context context, Intent intent) {
		throw new RuntimeException("Stub! You are bundling a stubbed jar in the apk! Please move it to the classpath instead.");
	}
}
