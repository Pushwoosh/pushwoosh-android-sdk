package com.pushwoosh.testingapp.layout;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.pushwoosh.GDPRManager;
import com.pushwoosh.Pushwoosh;
import com.pushwoosh.inapp.InAppManager;
import com.pushwoosh.tags.Tags;
import com.pushwoosh.tags.TagsBundle;
import com.pushwoosh.testingapp.AppData;
import com.pushwoosh.testingapp.AppPreferencesStrings;
import com.pushwoosh.testingapp.PlaceholderFragment;
import com.pushwoosh.testingapp.R;
import com.pushwoosh.testingapp.helpers.ShowMessageHelper;
import com.pushwoosh.testingapp.inline_inapp.InlineInAppActivity;
import com.pushwoosh.testingapp.proxy.PushwooshProxyController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ActionsFragment extends PlaceholderFragment {
	private View view;

	@BindView(R.id.messageText)
	TextView myTextView;

	@BindView(R.id.addBadgeNumberEditor)
	EditText addBadgeNumberEditor;

	@BindView(R.id.addBadgeNumberButton)
	Button addBadgeNumberButton;

	@BindView(R.id.setBadgeNumberEditor)
	EditText setBadgeNumberEditor;

	@BindView(R.id.setUserIdEditor)
	EditText setUserIdEditor;

	@BindView(R.id.setEmailEditor)
	EditText setEmailEditor;

	@BindView(R.id.postEventEditor)
	EditText postEventEditor;

	@BindView(R.id.sendTagsStringEditor)
	EditText sendTagsStringEditor;

	@BindView(R.id.sendTagsValueEditor)
	EditText sendTagsValueEditor;

	@OnClick(R.id.setBadgeNumberButton)
	public void setBadgeNumberButton() {
		if (setBadgeNumberEditor.getText().toString().length() != 0) {
			Integer badgeInt = Integer.parseInt(setBadgeNumberEditor.getText().toString());
			PushwooshProxyController.getPushwooshProxy().setBadgeNumber(badgeInt);
			String str = "setBadgeNumber: " + badgeInt + "\ngetBadgeNumber return: "
			             + PushwooshProxyController.getPushwooshProxy().getBadgeNumber();
			ShowMessageHelper.log(str);
			ShowMessageHelper.setMessage(str);
			hideSoftKeyBoardOnTabClicked(setBadgeNumberEditor);
		} else {
			myTextView.setText("Incorrect input!");
		}
	}

	@OnClick(R.id.addBadgeNumberButton)
	public void addBadgeNumberButton() {
		if (addBadgeNumberEditor.getText().toString().length() != 0) {
			Integer badgeInt = Integer.parseInt(addBadgeNumberEditor.getText().toString());
			PushwooshProxyController.getPushwooshProxy().addBadgeNumber(badgeInt);
			String str = "addBadgeNumber: " + badgeInt + "\ngetBadgeNumber return: "
			             + PushwooshProxyController.getPushwooshProxy().getBadgeNumber();
			ShowMessageHelper.log(str);
			ShowMessageHelper.setMessage(str);
			hideSoftKeyBoardOnTabClicked(addBadgeNumberEditor);
		} else {
			myTextView.setText("Incorrect input!");
		}
	}

	@OnClick(R.id.setUserIdButton)
	public void setUserIdButton() {
		if (setUserIdEditor.getText().toString().length() != 0) {
			String setUserIdString = setUserIdEditor.getText().toString();
			PushwooshProxyController.getPushwooshProxy().setUserId(setUserIdString);
			log("setUserId: " + setUserIdString);
			hideSoftKeyBoardOnTabClicked(setBadgeNumberEditor);
		} else {
			myTextView.setText("Incorrect input!");
		}
	}

	@OnClick(R.id.registerEmailButton)
	public void registerEmailButton() {
		if (setEmailEditor.getText().toString().length() != 0) {
			ArrayList list = new ArrayList();
			String setEmailString = setEmailEditor.getText().toString();
			list.add(setEmailString);
			PushwooshProxyController.getPushwooshProxy().setEmails(list);
			log("setEmail: " + setEmailString);
			hideSoftKeyBoardOnTabClicked(setBadgeNumberEditor);
		} else {
			myTextView.setText("Incorrect input!");
		}
	}

	@OnClick(R.id.postEventButton)
	public void postEventButton() {
		String postEventString = postEventEditor.getText().toString();
		if (postEventString.length() != 0) {
			TagsBundle attributes = Tags.empty();
			if (AppData.getInstance().getPostEventAttributes()) {
				attributes = AppData.getInstance().getAttributes();
			}
			InAppManager.getInstance().postEvent(postEventString, attributes);
			String str = "postEvent: " + postEventString + "\n attributes: " + attributes.toJson().toString();
			ShowMessageHelper.log(str);
			ShowMessageHelper.setMessage(str);
			hideSoftKeyBoardOnTabClicked(postEventEditor);
		} else {
			myTextView.setText("Incorrect input!");
		}
	}

	@OnClick(R.id.sendTagsButton)
	public void sendTagsButton() {
		String tagString = sendTagsStringEditor.getText().toString();
		String tagValue = sendTagsValueEditor.getText().toString();
		if ((tagString.length() != 0) && (tagValue.length() != 0)) {
			Integer tagIntValue = Integer.parseInt(tagValue);
			Map tag = new HashMap();
			tag.put(tagString, tagIntValue);
			PushwooshProxyController.getPushwooshProxy().sendTags(tagString, tagIntValue, (result) -> {
				String str;
				if (result.isSuccess()) {
					str = "onSentTagsSuccess";
				} else {
					str = "onSentTagsError, error: " + result.getException();
				}

				myTextView.append("\n" + str);
			});
			log("sendTags: " + tag);
			hideSoftKeyBoardOnTabClicked(setBadgeNumberEditor);
		} else {
			myTextView.setText("Incorrect input!");
		}
	}

	@OnClick(R.id.buyDeveloperButton)
	public void buyDeveloperButton() {
		String sku = "Developer";
		BigDecimal price = BigDecimal.valueOf(49.95);
		String currency = "USD";
		PushwooshProxyController.getPushwooshProxy().trackInAppRequest(sku, price, currency, new Date());
		String str = "trackInAppRequest, sku: " + sku + "\nprice: " + price + "\ncurrency: "
		             + currency + "\npurchaseTime: " + new Date();
		ShowMessageHelper.log(str);
		ShowMessageHelper.setMessage(str);
	}

	@OnClick(R.id.buyMarketingButton)
	public void buyMarketingButton() {
		String sku = "Marketing";
		BigDecimal price = BigDecimal.valueOf(149.95);
		String currency = "USD";
		PushwooshProxyController.getPushwooshProxy().trackInAppRequest(sku, price, currency, new Date());
		String str = "trackInAppRequest, sku: " + sku + "\nprice: " + price + "\ncurrency: "
		             + currency + "\npurchaseTime: " + new Date();
		ShowMessageHelper.log(str);
		ShowMessageHelper.setMessage(str);
	}

	@OnClick(R.id.scheduleLocalNotificationButton)
	public void scheduleLocalNotificationButton() {
		String message = "LocalNotification by TestingApp";
		Integer interval = 20;
		AppData.getInstance().getLocalNotificationIDList().
				add(PushwooshProxyController.getPushwooshProxy().scheduleLocalNotification(message, interval));
		String str = "scheduleLocalNotification, \nmessage: " + message + "\ninterval: " + interval;
		ShowMessageHelper.log(str);
		ShowMessageHelper.setMessage(str);
	}

	@OnClick(R.id.clearLocalNotificationButton)
	public void clearLocalNotificationButton() {
		ArrayList<Integer> idList = AppData.getInstance().getLocalNotificationIDList();
		Log.v(AppPreferencesStrings.TAG, "Scheduled local notifications number:" + idList.size());
		if (idList.size() > 0) {
			int localNotificationIndex = idList.size() - 1;
			PushwooshProxyController.getPushwooshProxy().clearLocalNotification(idList.get(localNotificationIndex));
			String str = "clearLocalNotification, ID: " + idList.get(localNotificationIndex);
			idList.remove(idList.size() - 1);
			Log.v(AppPreferencesStrings.TAG, "" + idList.size());
			ShowMessageHelper.log(str);
			ShowMessageHelper.setMessage(str);
		} else {
			myTextView.setText("There is no scheduled local notifications!");
		}
	}

	@OnClick(R.id.clearLocalNotificationsButton)
	public void clearLocalNotificationsButton() {
		PushwooshProxyController.getPushwooshProxy().clearLocalNotifications();
		AppData.getInstance().getLocalNotificationIDList().clear();
		log("clearLocalNotifications");
	}

	@OnClick(R.id.clearNotificationCenterButton)
	public void clearNotificationCenterButton() {
		PushwooshProxyController.getPushwooshProxy().clearNotificationCenter();
		log("clearNotificationCenterButton");
	}

	@OnClick(R.id.getPushHistoryButton)
	public void getPushHistoryButton() {
		List<String> pushHistory = PushwooshProxyController.getPushwooshProxy().getPushHistory();
		String str;
		if (pushHistory.size() > 0) {
			str = "getPushHistory: \n" + pushHistory.toString();
		} else {
			str = "getPushHistory: pushHistory is empty";
		}
		ShowMessageHelper.log(str);
		ShowMessageHelper.setMessage(str);
	}

	@OnClick(R.id.clearPushHistoryButton)
	public void clearPushHistoryButton() {
		PushwooshProxyController.getPushwooshProxy().clearPushHistory();
		log("clearPushHistory");
	}

	@OnClick({R.id.removeAllDeviceDataButton})
	public void removeAllDeviceDataButton(){
		GDPRManager.getInstance().removeAllDeviceData(null);
	}

	@OnClick({R.id.showCommunicationUIButton})
	public void showCommunicationUIButton(){
		GDPRManager.getInstance().showGDPRConsentUI();
	}

	@OnClick({R.id.showUiRemoveAllDeviceDataButton})
	public void showUiRemoveAllDeviceDataButton(){
		GDPRManager.getInstance().showGDPRDeletionUI();
	}

	private void log(String clearPushHistory) {
		String str = clearPushHistory;
		ShowMessageHelper.log(str);
		ShowMessageHelper.setMessage(str);
	}

	private void logAction(String str) {
		ShowMessageHelper.log(str);
		ShowMessageHelper.setMessage(str);
	}


	@OnClick(R.id.crashButton)
	public void crashApp() {
		throw new RuntimeException("Test");
	}

	@OnClick(R.id.openInlineInAppButton)
	public void openInlineInAppButton() {
		startActivity(new Intent(getContext(), InlineInAppActivity.class));
	}

	@OnClick(R.id.stopServerCommunicationButton)
	public void stopServerCommunication() {
		Pushwoosh.getInstance().stopServerCommunication();
	}

	@OnClick(R.id.startServerCommunicationButton)
	public void startServerCommunication() {
		Pushwoosh.getInstance().startServerCommunication();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		view = inflater.inflate(R.layout.fragment_actions, container, false);
		ButterKnife.bind(this, view);
		myTextView.setMovementMethod(new ScrollingMovementMethod());
		ShowMessageHelper.setTextView(myTextView);
		setMessage();
		return view;
	}

	public void setMessage() {
		myTextView.setText(AppData.getInstance().getMessage());
	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		if (isVisibleToUser) {
			if (view != null) {
				this.setMessage();
			}
		}
	}

	private void hideSoftKeyBoardOnTabClicked(View v) {
		if (v != null) {
			InputMethodManager imm = (InputMethodManager) getContext().
					getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(v.getApplicationWindowToken(),
					InputMethodManager.HIDE_NOT_ALWAYS);
		}
	}
}
