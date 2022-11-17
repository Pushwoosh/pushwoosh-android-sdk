package com.pushwoosh.inbox.ui.presentation.view.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.text.TextUtils
import android.view.View
import com.bumptech.glide.Glide
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.inbox.ui.presentation.view.style.ColorSchemeProvider
import com.pushwoosh.inbox.ui.presentation.view.style.ColorSchemeProviderFactory

open class AttachmentActivity : AppCompatActivity() {
    companion object {
        const val attachmentUrlExtra : String = "ATTACHMENT_URL_EXTRA"
    }

    private lateinit var colorSchemeProvider: ColorSchemeProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pw_activity_attachment)

        if (intent != null) {
            val url = intent.getStringExtra(attachmentUrlExtra)
            if (!TextUtils.isEmpty(url)) {
                Glide.with(this)
                        .load(url)
                        .into(findViewById(R.id.attachment))
            }
        }

        colorSchemeProvider = ColorSchemeProviderFactory.generateColorScheme(this)

        val containerView = findViewById<View>(R.id.container)
        containerView.setBackgroundColor(colorSchemeProvider.backgroundColor)
        containerView.setOnClickListener {
            super.onBackPressed()
        }
    }
}
