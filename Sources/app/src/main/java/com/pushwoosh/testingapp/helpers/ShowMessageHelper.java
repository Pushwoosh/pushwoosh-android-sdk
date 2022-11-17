package com.pushwoosh.testingapp.helpers;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.pushwoosh.testingapp.AppData;
import com.pushwoosh.testingapp.AppPreferencesStrings;
import com.pushwoosh.testingapp.proxy.PushwooshProxyController;

/**
 * Created by etkachenko on 5/18/17.
 */

public class ShowMessageHelper {
	private static ShowMessageHelper showMessageHelper;
	private static Context context;
	private static Toast toast;
	private static TextView textView;

	public static void initShowMessageHelper(Context appContext) {
		if (showMessageHelper == null) {
			showMessageHelper = new ShowMessageHelper();
			context = appContext;
		}
	}

	private static void toast(String str) {
		cancelToast();
		toast = Toast.makeText(context, str, Toast.LENGTH_SHORT);
		toast.show();
	}

	public static void log(String str) {
		Log.v(AppPreferencesStrings.TAG, str);
	}

	public static void setMessage(String str) {
		AppData.getInstance().setMessage(str);
		if (textView != null) {
			textView.setText(str);
		}
	}

	public static void showMessage(String message) {
		setMessage(message);
		//        toast(message);
		log(message);
	}

	public static void cancelToast() {
		if (toast != null) {
			toast.cancel();
		}
	}

	public static void setTextView(TextView textView) {
		ShowMessageHelper.textView = textView;
	}
}
