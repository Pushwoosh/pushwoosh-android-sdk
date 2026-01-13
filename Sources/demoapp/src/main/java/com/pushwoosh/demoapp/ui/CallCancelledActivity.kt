package com.pushwoosh.demoapp.ui

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.pushwoosh.demoapp.R
import com.pushwoosh.internal.utils.PWLog

class CallCancelledActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "CallCancelledActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_cancelled)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val callerName = intent.getStringExtra("caller_name") ?: "Unknown"
        val callId = intent.getStringExtra("call_id") ?: "N/A"
        val hasVideo = intent.getBooleanExtra("has_video", false)

        PWLog.info(TAG, "Showing call cancelled screen: caller=$callerName, callId=$callId, video=$hasVideo")

        // Setup views
        val textCallerName = findViewById<TextView>(R.id.textCallerName)
        val textCallType = findViewById<TextView>(R.id.textCallType)
        val textCallId = findViewById<TextView>(R.id.textCallId)
        val iconCallType = findViewById<ImageView>(R.id.iconCallType)
        val buttonOk = findViewById<MaterialButton>(R.id.buttonOk)

        textCallerName.text = callerName
        textCallId.text = callId

        if (hasVideo) {
            textCallType.text = "Video Call"
            iconCallType.setImageResource(R.drawable.ic_videocam)
        } else {
            textCallType.text = "Voice Call"
            iconCallType.setImageResource(R.drawable.ic_call)
        }

        buttonOk.setOnClickListener {
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
