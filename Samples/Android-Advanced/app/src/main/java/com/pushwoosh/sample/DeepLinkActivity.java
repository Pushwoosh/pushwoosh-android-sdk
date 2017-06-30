package com.pushwoosh.sample;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class DeepLinkActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.deep_link);
		setTitle("Deep link activity");
		
		Intent intent = getIntent();
	    String action = intent.getAction();
	    Uri data = intent.getData();
	    
	    if (action == Intent.ACTION_VIEW)
	    {
	    	openUrl(data);
	    }
	}
	
	private void openUrl(Uri uri) {
		String path = uri.getPath();
		Toast.makeText(getApplicationContext(), path, Toast.LENGTH_LONG).show();
	}
}
