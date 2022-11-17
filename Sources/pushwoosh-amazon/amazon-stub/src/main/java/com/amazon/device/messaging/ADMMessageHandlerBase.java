//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.amazon.device.messaging;

import android.app.IntentService;
import android.content.Intent;

public abstract class ADMMessageHandlerBase extends IntentService {
	protected ADMMessageHandlerBase(String className) {
		super((String)null);
		throw new RuntimeException("Stub! You are bundling a stubbed jar in the apk! Please move it to the classpath instead.");
	}

	protected final void onHandleIntent(Intent intent) {
		throw new RuntimeException("Stub! You are bundling a stubbed jar in the apk! Please move it to the classpath instead.");
	}

	protected abstract void onMessage(Intent var1);

	protected abstract void onRegistrationError(String var1);

	protected abstract void onRegistered(String var1);

	protected abstract void onUnregistered(String var1);
}
