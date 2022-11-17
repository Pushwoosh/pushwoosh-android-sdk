package com.pushwoosh.testingapp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import android.os.Bundle;

import com.pushwoosh.tags.TagsBundle;

/**
 * Created by etkachenko on 1/18/17.
 */

public class AppData {
	private static AppData instance;
	private String message;
	private ArrayList<Integer> localNotificationIDList = new ArrayList<Integer>();
	private Bundle pushBundle;
	private Boolean handleInForeground;
	private Boolean customNotifications;
	private Boolean postEventAttributes;
	private Boolean firstRun;
	private final TagsBundle attributes;

	private AppData() {
		attributes = new TagsBundle.Builder()
				.putInt("testInt", 17)
				.putString("testString", "str")
				.putDate("testDate", new Date())
				.putBoolean("testBoolean", true)
				.putList("testList", Arrays.asList("item1", "item2", "item3"))
				.build();
		message = "PW_message: device not registered";
	}

	public static synchronized AppData getInstance() {
		if (instance == null) {
			instance = new AppData();
		}
		return instance;
	}

	public Boolean getHandleInForeground() {
		return handleInForeground;
	}

	public void setHandleInForeground(Boolean handleInForeground) {
		this.handleInForeground = handleInForeground;
	}

	public void setMessage(String msg) {
		message = msg;
	}

	public String getMessage() {
		return message;
	}

	public Bundle getPushBundle() {
		return pushBundle;
	}

	public void setPushBundle(Bundle pushBundle) {
		this.pushBundle = pushBundle;
	}

	public Boolean getPostEventAttributes() {
		return postEventAttributes;
	}

	public void setPostEventAttributes(Boolean setPostEventAttributes) {
		this.postEventAttributes = setPostEventAttributes;
	}

	public Boolean getCustomNotifications() {
		return customNotifications;
	}

	public void setCustomNotifications(Boolean customNotifications) {
		this.customNotifications = customNotifications;
	}

	public TagsBundle getAttributes() {
		return attributes;
	}

	public ArrayList<Integer> getLocalNotificationIDList() {
		return localNotificationIDList;
	}

	public Boolean getFirstRun() {
		return firstRun;
	}

	public void setFirstRun(Boolean firstRun) {
		this.firstRun = firstRun;
	}
}
