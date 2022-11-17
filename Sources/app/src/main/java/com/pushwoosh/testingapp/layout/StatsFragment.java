package com.pushwoosh.testingapp.layout;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pushwoosh.testingapp.AppData;
import com.pushwoosh.testingapp.PlaceholderFragment;
import com.pushwoosh.testingapp.R;
import com.pushwoosh.testingapp.helpers.GetTagsCallback;
import com.pushwoosh.testingapp.helpers.ShowMessageHelper;
import com.pushwoosh.testingapp.proxy.PushwooshProxyController;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class StatsFragment extends PlaceholderFragment {
	private View view;

	@BindView(R.id.getPushTokenTextView)
	TextView getPushTokenTextView;

	@BindView(R.id.getPushwooshHWIDTextView)
	TextView getPushwooshHWIDTextView;

	@BindView(R.id.getCustomDataTextView)
	TextView getCustomDataTextView;

	@BindView(R.id.getLaunchNotificationTextView)
	TextView getLaunchNotificationTextView;

	@BindView(R.id.getTagsAsyncTextView)
	TextView getTagsAsyncTextView;

	@BindView(R.id.getBadgeNumberTextView)
	TextView getBadgeNumberTextView;

	@OnClick(R.id.refreshStatsButton)
	public void refreshStatsButton() {
		updateStats();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		view = inflater.inflate(R.layout.fragment_stats, container, false);
		ButterKnife.bind(this, view);
		updateStats();
		return view;
	}

	private void updateStats() {
		updateGetPushTokenTextView();
		updateGetPushwooshHWIDTextView();
		updateGetCustomDataTextView();
		updateGetLaunchNotificationTextView();
		updateGetTagsAsyncTextView();
		updateGetBadgeNumberTextView();
	}

	private void updateGetPushTokenTextView() {
		String str = PushwooshProxyController.getPushwooshProxy().getPushToken();
		if (str == "" || str == null) {
			str = "No token obtained";
		}
		getPushTokenTextView.setText(str);
		ShowMessageHelper.log("getPushToken: " + str);
	}

	private void updateGetPushwooshHWIDTextView() {
		String str = PushwooshProxyController.getPushwooshProxy().getPushwooshHWID();
		if (str == "" || str == null) {
			str = "No HWID obtained";
		}
		getPushwooshHWIDTextView.setText(str);
		ShowMessageHelper.log("getHwid: " + str);
	}

	private void updateGetCustomDataTextView() {
		Bundle pushBundle = AppData.getInstance().getPushBundle();
		String str = "";
		if (pushBundle != null) {
			str = PushwooshProxyController.getPushwooshProxy().getCustomData(pushBundle);
		}
		if (str == null || str == "") {
			str = "No customData obtained";
		}
		getCustomDataTextView.setText(str);
		ShowMessageHelper.log("getCustomData: " + str);
	}

	private void updateGetLaunchNotificationTextView() {
		String str = "" + PushwooshProxyController.getPushwooshProxy().getLaunchNotification();
		if (str.length() == 0) {
			str = "No launchNotification obtained";
		}
		getLaunchNotificationTextView.setText(str);
		ShowMessageHelper.log("getLaunchNotification: " + str);
	}

	private void updateGetTagsAsyncTextView() {
		PushwooshProxyController.getPushwooshProxy().getTagsAsync(new GetTagsCallbackImpl());
		String str = "request sent";
		getTagsAsyncTextView.setText(str);
		ShowMessageHelper.log("getTagsAsync: " + str);
	}

	private void updateGetBadgeNumberTextView() {
		String str = "" + PushwooshProxyController.getPushwooshProxy().getBadgeNumber();
		if (str.length() == 0) {
			str = "No badgeNumber obtained";
		}
		getBadgeNumberTextView.setText(str);
		ShowMessageHelper.log("getBadgeNumber: " + str);
	}

	private class GetTagsCallbackImpl implements GetTagsCallback {
		@Override
		public void process(String getTagsResponseMessage) {
			getTagsAsyncTextView.setText(getTagsResponseMessage);
			ShowMessageHelper.log("tagsReceived: " + getTagsResponseMessage);
		}
	}
}
