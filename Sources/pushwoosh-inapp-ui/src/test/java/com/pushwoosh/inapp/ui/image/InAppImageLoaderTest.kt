package com.pushwoosh.inapp.ui.image

import android.view.View
import android.widget.ImageView
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class InAppImageLoaderTest {

    // A blank url hits the early return (never reaching Glide), hiding the image slot instead of
    // leaving a visible empty box — the contract every template leans on when imageUrl is absent.
    @Test
    fun loadHidesViewWhenUrlBlank() {
        val iv = ImageView(RuntimeEnvironment.getApplication())
        for (blank in listOf(null, "")) {
            iv.visibility = View.VISIBLE
            InAppImageLoader.load(blank, iv)
            assertEquals("url=<$blank> should hide the view", View.GONE, iv.visibility)
        }
    }
}
