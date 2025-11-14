package com.pushwoosh.calls.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.pushwoosh.calls.PushwooshCallReceiver
import com.pushwoosh.calls.R
import com.pushwoosh.calls.util.Constants
import com.pushwoosh.internal.utils.PWLog

class IncomingCallActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "IncomingCallActivity"
    }

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            PWLog.noise(TAG, "Received broadcast to close activity")
            finish()

        }
    }

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

        registerCloseReceiver()

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

    private fun registerCloseReceiver() {
        val filter = IntentFilter(Constants.ACTION_FINISH_CALL_ACTIVITY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(closeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(closeReceiver, filter)
        }
        PWLog.debug(TAG, "Registered closeReceiver for action: ${Constants.ACTION_FINISH_CALL_ACTIVITY}")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(closeReceiver)
            PWLog.debug(TAG, "Unregistered closeReceiver")
        } catch (e: IllegalArgumentException) {
            PWLog.debug(TAG, "closeReceiver was not registered")
        }
    }
}
