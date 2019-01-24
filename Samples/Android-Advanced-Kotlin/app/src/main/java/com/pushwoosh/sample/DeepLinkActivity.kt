package com.pushwoosh.sample

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast

class DeepLinkActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.deep_link)
        title = "Deep link activity"

        val intent = intent
        val action = intent.action
        val data = intent.data

        data?.let {
            if (TextUtils.equals(action, Intent.ACTION_VIEW)) {
                openUrl(data)
            }
        } ?: run {
            Log.d(PushwooshSampleApp.LTAG, "Intent data is null")
        }
    }

    private fun openUrl(uri: Uri) {
        val path = uri.path
        Toast.makeText(applicationContext, path, Toast.LENGTH_LONG).show()
    }
}
