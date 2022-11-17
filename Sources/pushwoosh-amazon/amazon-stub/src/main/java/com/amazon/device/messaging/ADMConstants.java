//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.amazon.device.messaging;

public final class ADMConstants {
	public static final String ERROR_AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
	public static final String ERROR_SERVICE_NOT_AVAILABLE = "SERVICE_NOT_AVAILABLE";
	public static final String ERROR_INVALID_SENDER = "INVALID_SENDER";
	public static final String EXTRA_MD5 = "adm_message_md5";

	private ADMConstants() {
		throw new RuntimeException("Stub! You are bundling a stubbed jar in the apk! Please move it to the classpath instead.");
	}

	public static final class LowLevel {
		public static final String ACTION_REGISTER = "com.amazon.device.messaging.intent.REGISTER";
		public static final String ACTION_UNREGISTER = "com.amazon.device.messaging.intent.UNREGISTER";
		public static final String ACTION_SYNC = "com.amazon.device.messaging.intent.SYNC";
		public static final String EXTRA_APPLICATION_PENDING_INTENT = "app";
		public static final String ACTION_APP_REGISTRATION_EVENT = "com.amazon.device.messaging.intent.REGISTRATION";
		public static final String EXTRA_REGISTRATION_ID = "registration_id";
		public static final String EXTRA_UNREGISTERED = "unregistered";
		public static final String EXTRA_SENDER = "sender";
		public static final String EXTRA_ERROR = "error";
		public static final String EXTRA_ERROR_DESCRIPTION = "error_description";
		public static final String EXTRA_SYNC_STATE = "is_sync";
		public static final String ACTION_RECEIVE_ADM_MESSAGE = "com.amazon.device.messaging.intent.RECEIVE";
		public static final String PERMISSION_SEND_ADM_MESSAGE = "com.amazon.device.messaging.permission.SEND";
		public static final String PERMISSION_TO_RECEIVE = "com.amazon.device.messaging.permission.RECEIVE";
		public static final String ADM_PACKAGE_NAME = "com.amazon.device.messaging";

		private LowLevel() {
			throw new RuntimeException("Stub! You are bundling a stubbed jar in the apk! Please move it to the classpath instead.");
		}
	}
}
