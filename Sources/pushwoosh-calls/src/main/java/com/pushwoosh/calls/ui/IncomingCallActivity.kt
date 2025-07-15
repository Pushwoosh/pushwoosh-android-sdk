package com.pushwoosh.calls.ui

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.pushwoosh.calls.PushwooshCallReceiver
import com.pushwoosh.calls.R

class IncomingCallActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val payload = intent.extras

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContentView(R.layout.activity_incoming_call)

        val callerNameView = findViewById<TextView>(R.id.caller_name)
        val acceptButton = findViewById<ImageButton>(R.id.btn_accept)
        val rejectButton = findViewById<ImageButton>(R.id.btn_reject)

        val callerName = intent.getStringExtra("callerName") ?: "Unknown Caller"
        callerNameView.text = callerName

        acceptButton.setOnClickListener {
            val acceptIntent = Intent(applicationContext, PushwooshCallReceiver::class.java).apply {
                action = "ACTION_ACCEPT_CALL"
            }
            if (payload != null) {
                acceptIntent.putExtras(payload)
            }
            sendBroadcast(acceptIntent)
            finish()
        }

        rejectButton.setOnClickListener {
            val rejectIntent = Intent(applicationContext, PushwooshCallReceiver::class.java).apply {
                action = "ACTION_REJECT_CALL"
            }
            if (payload != null) {
                rejectIntent.putExtras(payload)
            }
            sendBroadcast(rejectIntent)
            finish()
        }
    }
}
