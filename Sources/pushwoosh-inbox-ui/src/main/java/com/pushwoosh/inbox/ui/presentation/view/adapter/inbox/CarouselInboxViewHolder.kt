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

package com.pushwoosh.inbox.ui.presentation.view.adapter.inbox

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.ui.InboxCarouselSlide
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.inbox.ui.presentation.view.style.ColorSchemeProvider
import kotlin.math.abs

/**
 * Carousel rich card: a swipeable gallery of slides with page dots, over the
 * message title / body. Mirrors the iOS PushwooshInboxKit carousel cell —
 * tapping a slide opens the slide's own url, and a slide without one falls
 * through to the message's default row action. The text block collapses when
 * the message carries neither title nor body (pure gallery).
 */
class CarouselInboxViewHolder(adapter: InboxAdapter,
                              itemView: View,
                              colorSchemeProvider: ColorSchemeProvider) : RichCardViewHolder(adapter, itemView, colorSchemeProvider) {

    private val galleryView: RecyclerView = itemView.findViewById(R.id.inboxCarouselGallery)
    private val dotsView: LinearLayout = itemView.findViewById(R.id.inboxCarouselDots)
    private val textBlockView: View = itemView.findViewById(R.id.inboxCarouselTextBlock)
    private val titleRowView: View = itemView.findViewById(R.id.inboxCarouselTitleRow)
    private val titleTextView: TextView = itemView.findViewById(R.id.inboxCarouselTitle)
    private val bodyTextView: TextView = itemView.findViewById(R.id.inboxCarouselBody)
    private val dateTextView: TextView = itemView.findViewById(R.id.inboxCarouselDate)
    private val unreadDotView: View = itemView.findViewById(R.id.inboxCarouselUnreadDot)
    private val pinChipView: View = itemView.findViewById(R.id.inboxCarouselPinChip)

    private val slideAdapter = SlideAdapter()
    private val snapHelper = PagerSnapHelper()

    private val dotSize = context.resources.getDimensionPixelSize(R.dimen.pw_carousel_dot_size)
    private val dotActiveWidth = context.resources.getDimensionPixelSize(R.dimen.pw_carousel_dot_active_width)
    private val dotGap = context.resources.getDimensionPixelSize(R.dimen.pw_carousel_dot_gap)

    private var boundCode: String? = null
    private var boundSlideCount = 0
    private var activePage = 0

    init {
        clipToRoundedCorners(galleryView, R.dimen.pw_carousel_gallery_corner_radius)
        galleryView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        galleryView.adapter = slideAdapter
        // The gallery lives inside a vertically scrolling list — without a fixed size every
        // slide bind would ask the whole inbox to re-measure.
        galleryView.setHasFixedSize(true)
        snapHelper.attachToRecyclerView(galleryView)
        galleryView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateActiveDot(currentPage())
            }
        })
        // The list's swipe-to-delete wins the touch-slop race for horizontal gestures, so claim
        // the touch on down and hand it back only once the gesture turns out vertical.
        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        galleryView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            private var downX = 0f
            private var downY = 0f

            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = e.x
                        downY = e.y
                        rv.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = abs(e.x - downX)
                        val dy = abs(e.y - downY)
                        if (dy > touchSlop && dy > dx) {
                            rv.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                }
                return false
            }
        })
    }

    override fun fillView(model: InboxMessage?, position: Int) {
        if (model == null) {
            return
        }

        val params = InboxCardKind.parseActionParams(model)
        val slides = InboxCarouselSlide.decode(params)

        // Any list update rebinds every visible row (setCollection -> notifyDataSetChanged).
        // Rewinding the gallery then would yank a half-swiped carousel back to slide one.
        val sameMessage = boundCode == model.code
        val sameSlides = boundSlideCount == slides.size
        boundCode = model.code
        boundSlideCount = slides.size
        slideAdapter.submit(slides, model)
        if (!sameMessage) {
            galleryView.scrollToPosition(0)
            activePage = 0
            buildDots(slides.size)
        } else if (!sameSlides) {
            // Same row, edited payload: the gallery and the dot strip must follow the new slide set.
            activePage = activePage.coerceIn(0, maxOf(slides.size - 1, 0))
            galleryView.scrollToPosition(activePage)
            buildDots(slides.size)
        }

        val hasText = bindTextBlock(model, titleRowView, titleTextView, bodyTextView, dateTextView, textBlockView)

        bindUnreadDot(unreadDotView, model)
        // With no text block there is no gutter to hold the dot — iOS hides it the same way.
        if (!hasText) {
            unreadDotView.visibility = View.GONE
        }
        bindPinChip(pinChipView, InboxCardKind.isPinned(params))
    }

    private fun currentPage(): Int {
        val layoutManager = galleryView.layoutManager as? LinearLayoutManager ?: return 0
        val snapView = snapHelper.findSnapView(layoutManager) ?: return 0
        return layoutManager.getPosition(snapView)
    }

    private fun buildDots(count: Int) {
        dotsView.removeAllViews()
        // A single slide needs no indicator, same as the iOS page control.
        if (count <= 1) {
            dotsView.visibility = View.GONE
            return
        }
        dotsView.visibility = View.VISIBLE
        repeat(count) { index ->
            val dot = View(context)
            dot.setBackgroundResource(R.drawable.pw_bg_carousel_dot)
            val params = LinearLayout.LayoutParams(dotSize, dotSize)
            if (index > 0) {
                params.marginStart = dotGap
            }
            dotsView.addView(dot, params)
        }
        applyActiveDot()
    }

    /**
     * Resizing the dots walks a requestLayout up to the inbox list, so it must happen on a
     * page change — not on every onScrolled callback, which fires once per dragged pixel.
     */
    private fun updateActiveDot(page: Int) {
        if (page == activePage) {
            return
        }
        activePage = page
        applyActiveDot()
    }

    private fun applyActiveDot() {
        for (index in 0 until dotsView.childCount) {
            val dot = dotsView.getChildAt(index)
            val active = index == activePage
            val params = dot.layoutParams
            val width = if (active) dotActiveWidth else dotSize
            if (params.width != width) {
                params.width = width
                dot.layoutParams = params
            }
            dot.alpha = if (active) 1f else 0.45f
        }
    }

    /** Backs the horizontal gallery; one entry per decoded slide. */
    private inner class SlideAdapter : RecyclerView.Adapter<SlideViewHolder>() {

        private var slides: List<InboxCarouselSlide> = emptyList()
        private var message: InboxMessage? = null

        fun submit(slides: List<InboxCarouselSlide>, message: InboxMessage) {
            this.slides = slides
            this.message = message
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.pw_item_inbox_carousel_slide, parent, false)
            return SlideViewHolder(view)
        }

        override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
            holder.bind(slides[position], message)
        }

        override fun getItemCount(): Int = slides.size
    }

    private inner class SlideViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val imageView: ImageView = view.findViewById(R.id.inboxCarouselSlideImage)
        private val scrimView: View = view.findViewById(R.id.inboxCarouselSlideScrim)
        private val captionView: TextView = view.findViewById(R.id.inboxCarouselSlideCaption)

        fun bind(slide: InboxCarouselSlide, message: InboxMessage?) {
            Glide.with(itemView.context).load(slide.imageUrl).into(imageView)

            if (slide.title != null) {
                captionView.text = slide.title
                captionView.visibility = View.VISIBLE
                scrimView.visibility = View.VISIBLE
            } else {
                captionView.text = null
                captionView.visibility = View.GONE
                scrimView.visibility = View.GONE
            }

            itemView.contentDescription = slide.title
            itemView.setOnClickListener {
                val model = message ?: return@setOnClickListener
                val url = slide.url
                if (url != null) {
                    openCardUrl(url, model)
                } else {
                    // No destination of its own — behave like a tap on the card itself.
                    this@CarouselInboxViewHolder.itemView.performClick()
                }
            }
        }
    }
}
