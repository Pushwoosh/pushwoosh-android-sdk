package com.pushwoosh.sample.quickstart;

import android.os.Bundle;

import com.pushwoosh.Pushwoosh;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Pushwoosh.getInstance().registerForPushNotifications();
	}
}
