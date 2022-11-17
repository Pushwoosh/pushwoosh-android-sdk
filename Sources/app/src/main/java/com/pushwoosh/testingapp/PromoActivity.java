package com.pushwoosh.testingapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class PromoActivity extends Activity {
	private TextView textView;
	private Button toMainActivityButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_promo);
		setTitle("Deep link activity");

		Intent intent = getIntent();
		String action = intent.getAction();
		Uri data = intent.getData();
		if (TextUtils.equals(action, Intent.ACTION_VIEW)) {
			openUrl(data);
		}
	}

	private void openUrl(Uri uri) {
		String promoId = uri.getQueryParameter("id");
		textView = (TextView) findViewById(R.id.deepLinkTextView);
		toMainActivityButton = (Button) findViewById(R.id.toMainActivityButton);
		toMainActivityButton.setOnClickListener(toMainActivityListener);
		textView.append("\npromoID = " + promoId);
		Toast.makeText(getApplicationContext(), promoId, Toast.LENGTH_LONG).show();
	}

	private View.OnClickListener toMainActivityListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			onBackPressed();
		}
	};
}