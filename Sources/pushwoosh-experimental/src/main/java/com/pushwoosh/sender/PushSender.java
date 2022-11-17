//
//  PushSender.java
//
// Pushwoosh Push Notifications SDK
// www.pushwoosh.com
//
// MIT Licensed

package com.pushwoosh.sender;

import android.content.Context;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;

public class PushSender {
	private String accessToken = "";
	private String appId = "";

	public PushSender(Context context, String accessToken) {
		this.accessToken = accessToken;
		appId = Pushwoosh.getInstance().getApplicationCode();
	}

	public void sendPush(PushMessage message) {
		CreateMessageRequest request = new CreateMessageRequest(appId, accessToken, message);
		RequestManager requestManager = NetworkModule.getRequestManager();
		if(requestManager == null){
			return;
		}
		requestManager.sendRequest(request);
	}
}
