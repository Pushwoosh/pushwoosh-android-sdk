package com.pushwoosh.inapp.ui.image

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide

/** Thin Glide wrapper so image loading lives behind one swappable call site. */
internal object InAppImageLoader {

    /** Loads [url] into [imageView]; hides the view when the url is blank. A neutral grey
     *  placeholder holds the frame while loading and on error, so a broken image never leaves
     *  a black/collapsed gap (iOS uses a fixed-aspect placeholder box; we skip its spinner by design). */
    fun load(url: String?, imageView: ImageView) {
        if (url.isNullOrEmpty()) {
            imageView.visibility = View.GONE
            return
        }
        imageView.visibility = View.VISIBLE
        val placeholder = placeholderDrawable()
        Glide.with(imageView).load(url).placeholder(placeholder).error(placeholder).into(imageView)
    }

    /** Warms Glide's cache for each of [urls] so images (incl. off-screen carousel/stories
     *  slides) are ready before the view is built. Called on the main thread from route();
     *  Glide loads async, so this never blocks the show. Capped to the screen size so a heavy
     *  multi-slide message can't spike memory decoding every image at full resolution. */
    fun prefetch(context: Context, urls: List<String>) {
        val metrics = context.resources.displayMetrics
        val w = metrics.widthPixels
        val h = metrics.heightPixels
        for (url in urls) {
            Glide.with(context).load(url).override(w, h).preload()
        }
    }

    private fun placeholderDrawable(): GradientDrawable =
        GradientDrawable().apply { setColor(Color.parseColor("#E0E0E0")) }
}
