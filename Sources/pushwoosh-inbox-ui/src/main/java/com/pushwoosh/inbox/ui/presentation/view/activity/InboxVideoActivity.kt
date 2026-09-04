/*
 *
 * Copyright (c) 2026. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.inbox.ui.presentation.view.activity

import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.internal.utils.PWLog

/**
 * Full-screen player for a video inbox card, opened when the card's poster is
 * tapped. Mirrors the iOS full-screen AVPlayer: sound on, transport controls,
 * playback starts by itself.
 */
open class InboxVideoActivity : AppCompatActivity() {

    companion object {
        const val VIDEO_URL_EXTRA: String = "PW_INBOX_VIDEO_URL_EXTRA"
        private const val TAG = "InboxVideoActivity"
        private const val PLAYBACK_POSITION_KEY = "pw_inbox_video_position"
    }

    private var playbackPosition = 0
    private var resumed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pw_activity_video)

        val url = intent?.getStringExtra(VIDEO_URL_EXTRA)
        if (TextUtils.isEmpty(url)) {
            PWLog.warn(TAG, "No video URL in the intent, closing the player")
            finish()
            return
        }

        playbackPosition = savedInstanceState?.getInt(PLAYBACK_POSITION_KEY, 0) ?: 0

        val player = findViewById<VideoView>(R.id.inboxVideoPlayer)
        player.setMediaController(MediaController(this).apply { setAnchorView(player) })
        player.setOnPreparedListener {
            if (playbackPosition > 0) {
                player.seekTo(playbackPosition)
            }
            // Buffering can outlive the visible activity — starting then would play sound
            // behind whatever the user switched to.
            if (resumed) {
                player.start()
            }
        }
        player.setOnCompletionListener { finish() }
        player.setOnErrorListener { _, what, extra ->
            PWLog.error(TAG, "Video playback failed: what=$what extra=$extra url=$url")
            Toast.makeText(this, R.string.pw_inbox_video_failed, Toast.LENGTH_SHORT).show()
            finish()
            // Handled here — returning false would also pop the system's own error dialog.
            true
        }
        player.setVideoURI(Uri.parse(url))
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        val player = findViewById<VideoView>(R.id.inboxVideoPlayer) ?: return
        if (playbackPosition > 0) {
            player.seekTo(playbackPosition)
        }
        player.start()
    }

    override fun onPause() {
        super.onPause()
        resumed = false
        val player = findViewById<VideoView>(R.id.inboxVideoPlayer) ?: return
        playbackPosition = player.currentPosition
        player.pause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(PLAYBACK_POSITION_KEY, playbackPosition)
    }
}
