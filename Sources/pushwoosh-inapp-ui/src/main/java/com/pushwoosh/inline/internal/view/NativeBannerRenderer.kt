package com.pushwoosh.inline.internal.view

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.pushwoosh.inline.internal.model.InlineAlign
import com.pushwoosh.inline.internal.model.InlineAxis
import com.pushwoosh.inline.internal.model.InlineBanner
import com.pushwoosh.inline.internal.model.InlineColor
import com.pushwoosh.inline.internal.model.InlineDistribution
import com.pushwoosh.inline.internal.model.InlineInsets
import com.pushwoosh.inline.internal.model.InlineNode
import com.pushwoosh.inline.internal.model.InlineStackAlign
import com.pushwoosh.inline.internal.model.InlineStyle
import com.pushwoosh.inline.internal.model.InlineWeight
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Draws a native banner as views built in code: lengths in dp, font sizes in sp scaled by the
 * system font scale up to 1.6×, the `dark` style when [create] is asked for the dark theme. A tap
 * goes to the innermost node with an action; the banner `action` takes the rest. Ready at once.
 */
internal class NativeBannerRenderer : InlineContentFactory {

    override fun create(context: Context, banner: InlineBanner, dark: Boolean, host: InlineContentHost): InlineContent? {
        val root = banner.root ?: return null
        val builder = NativeTreeBuilder(context, dark, host)
        val bannerTap: (() -> Unit)? = banner.action?.let { action -> { host.onClick(null, action) } }
        val tree = builder.build(root, bannerTap)
        val content = if (bannerTap == null) tree else NodeBox(context, tree).apply { setOnClickListener { bannerTap() } }
        val frame = BannerFrame(context, content, banner.close?.takeIf { it.enabled }) { host.onClose() }
        return NativeContent(frame, builder.videos, builder.pagers)
    }

    private class NativeContent(
        override val view: View,
        private val videos: List<InlineVideoView>,
        private val pagers: List<InlinePagerView>,
    ) : InlineContent {
        override val isReady: Boolean = true

        override fun dispose() {
            videos.forEach { it.release() }
            pagers.forEach { it.stopTurning() }
        }
    }
}

private class NativeTreeBuilder(private val context: Context, private val dark: Boolean, private val host: InlineContentHost) {
    val videos = ArrayList<InlineVideoView>()
    val pagers = ArrayList<InlinePagerView>()

    private val density = context.resources.displayMetrics.density
    private val fontScale = min(context.resources.configuration.fontScale, MAX_FONT_SCALE)

    /**
     * The node's view, tagged with the node id. [enclosingTap] is the tap of the nearest ancestor
     * with an action, for areas that take touches themselves (pager pages).
     */
    fun build(node: InlineNode, enclosingTap: (() -> Unit)?): View {
        val tap: (() -> Unit)? = node.action?.let { action -> { host.onClick(node.id, action) } }
        val innerTap = tap ?: enclosingTap
        val style = effectiveStyle(node)
        val isButton = node is InlineNode.Button
        val content: View = when (node) {
            is InlineNode.Text -> text(node)
            is InlineNode.Image -> image(node)
            is InlineNode.Button -> button(node, style.padding)
            is InlineNode.Video -> InlineVideoView(context, node.aspect, node.url, node.poster).also { videos += it }
            is InlineNode.Stack -> if (node.axis == InlineAxis.OVERLAY) overlay(node, innerTap) else stack(node, innerTap)
            is InlineNode.Pager -> pager(node, innerTap)
            is InlineNode.Spacer -> FlexibleSpace(context)
            is InlineNode.Divider -> View(context).apply {
                setBackgroundColor(themed(node.color, node.dark?.color, SEPARATOR_LIGHT, SEPARATOR_DARK))
            }
        }
        val view = if (style == PLAIN && tap == null && !isButton) {
            content
        } else {
            box(content, style, if (isButton) InlineInsets.ZERO else style.padding).apply {
                if (isButton || tap != null) setOnClickListener { tap?.invoke() }
            }
        }
        view.tag = node.id
        return view
    }

    private fun effectiveStyle(node: InlineNode): InlineStyle {
        val o = node.dark?.takeIf { dark } ?: return node.style
        val s = node.style
        return s.copy(
            background = o.background ?: s.background,
            gradient = o.gradient ?: s.gradient,
            backgroundImage = o.backgroundImage ?: s.backgroundImage,
            shadow = o.shadow ?: s.shadow,
            border = o.border ?: s.border,
            glass = o.glass ?: s.glass,
        )
    }

    /** A light color without a dark variant stays in the dark theme; no color at all = the theme default. */
    private fun themed(color: InlineColor?, darkOnly: Int?, light: Int, darkDefault: Int): Int =
        if (dark) color?.dark ?: darkOnly ?: color?.light ?: darkDefault else color?.light ?: light

    private fun box(content: View, style: InlineStyle, padding: InlineInsets): NodeBox {
        val box = NodeBox(context, content)
        box.setPadding(px(padding.left), px(padding.top), px(padding.right), px(padding.bottom))
        val radius = style.cornerRadius * density
        box.background = GradientDrawable().apply {
            cornerRadius = radius
            val gradient = style.gradient
            if (gradient != null) {
                orientation = orientation(gradient.angle)
                colors = gradient.colors.toIntArray()
            } else {
                setColor(style.background ?: Color.TRANSPARENT)
            }
            style.border?.let { setStroke(max(1, px(it.width)), it.color) }
        }
        if (radius > 0) box.clipToOutline = true
        style.backgroundImage?.let { url ->
            box.addDecor(ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                Glide.with(this).load(url).into(this)
            })
        }
        if (style.glass) {
            box.addDecor(View(context).apply {
                background = GradientDrawable().apply {
                    cornerRadius = radius
                    setColor(if (dark) GLASS_DARK else GLASS_LIGHT)
                }
            })
        }
        style.shadow?.takeIf { Color.alpha(it.color) > 0 }?.let { shadow ->
            box.elevation = shadow.radius * density
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                box.outlineSpotShadowColor = shadow.color
                box.outlineAmbientShadowColor = shadow.color
            }
        }
        return box
    }

    private fun text(node: InlineNode.Text): TextView = TextView(context).apply {
        text = node.text
        setTextSize(TypedValue.COMPLEX_UNIT_PX, node.size * density * fontScale)
        typeface = typeface(node.weight)
        setTextColor(themed(node.color, node.dark?.color, LABEL_LIGHT, LABEL_DARK))
        val horizontal = when (node.align) {
            InlineAlign.LEFT -> Gravity.LEFT
            InlineAlign.CENTER -> Gravity.CENTER_HORIZONTAL
            InlineAlign.RIGHT -> Gravity.RIGHT
            null -> Gravity.START
        }
        gravity = horizontal or Gravity.CENTER_VERTICAL
        if (node.lines > 0) {
            maxLines = node.lines
            ellipsize = TextUtils.TruncateAt.END
        }
        if (node.strike) paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
    }

    /** The node padding becomes the title's insets; the box around it takes the press. */
    private fun button(node: InlineNode.Button, insets: InlineInsets): TextView = TextView(context).apply {
        text = node.title
        setTextSize(TypedValue.COMPLEX_UNIT_PX, node.size * density * fontScale)
        typeface = typeface(node.weight)
        setTextColor(themed(node.color, node.dark?.color, LABEL_LIGHT, LABEL_DARK))
        gravity = Gravity.CENTER
        setPadding(px(insets.left), px(insets.top), px(insets.right), px(insets.bottom))
    }

    /** Ratio `aspect`, or 2:1 until the image loads and then its own ratio. */
    private fun image(node: InlineNode.Image): View {
        val frame = AspectFrame(context, node.aspect ?: DEFAULT_IMAGE_ASPECT)
        val image = ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        frame.addView(image, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        var request = Glide.with(image).load(node.url).dontTransform()
        if (node.aspect == null) {
            request = request.listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean = false

                override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                    val width = resource.intrinsicWidth
                    val height = resource.intrinsicHeight
                    if (width > 0 && height > 0) {
                        frame.ratio = (width.toFloat() / height).coerceIn(MIN_ASPECT, MAX_ASPECT)
                        host.onContentSizeChanged()
                    }
                    return false
                }
            })
        }
        request.into(image)
        return frame
    }

    private fun stack(node: InlineNode.Stack, enclosingTap: (() -> Unit)?): LinearLayout {
        val row = node.axis == InlineAxis.ROW
        val equal = node.distribution == InlineDistribution.EQUAL
        val hasFlexibleSpacer = node.children.any { it is InlineNode.Spacer && it.size == null }
        val layout = LinearLayout(context)
        layout.orientation = if (row) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        layout.isMeasureWithLargestChildEnabled = equal
        val spacing = px(node.spacing)
        node.children.forEachIndexed { i, child ->
            val params = if (row) rowParams(child, node.align, equal, hasFlexibleSpacer) else columnParams(child, node.align, equal)
            if (i > 0) {
                if (row) params.marginStart = spacing else params.topMargin = spacing
            }
            layout.addView(build(child, enclosingTap), params)
        }
        return layout
    }

    private fun columnParams(child: InlineNode, align: InlineStackAlign, equal: Boolean): LinearLayout.LayoutParams = when (child) {
        is InlineNode.Spacer -> LinearLayout.LayoutParams(MATCH, child.size?.let { px(it) } ?: 0, if (child.size == null) 1f else 0f)
        is InlineNode.Divider -> LinearLayout.LayoutParams(MATCH, max(1, px(child.thickness)))
        else -> LinearLayout.LayoutParams(if (align == InlineStackAlign.FILL) MATCH else WRAP, if (equal) 0 else WRAP, if (equal) 1f else 0f).apply {
            gravity = when (align) {
                InlineStackAlign.CENTER -> Gravity.CENTER_HORIZONTAL
                InlineStackAlign.END -> Gravity.END
                else -> Gravity.START
            }
        }
    }

    /** Without a flexible spacer, nodes without a natural width (media, stacks, wrapping text) share the free width. */
    private fun rowParams(child: InlineNode, align: InlineStackAlign, equal: Boolean, hasFlexibleSpacer: Boolean): LinearLayout.LayoutParams =
        when (child) {
            is InlineNode.Spacer -> LinearLayout.LayoutParams(child.size?.let { px(it) } ?: 0, WRAP, if (child.size == null) 1f else 0f)
            is InlineNode.Divider -> LinearLayout.LayoutParams(max(1, px(child.thickness)), MATCH)
            else -> {
                val stretch = equal || (!hasFlexibleSpacer && stretchesInRow(child))
                LinearLayout.LayoutParams(if (stretch) 0 else WRAP, if (align == InlineStackAlign.FILL) MATCH else WRAP, if (stretch) 1f else 0f).apply {
                    gravity = when (align) {
                        InlineStackAlign.CENTER -> Gravity.CENTER_VERTICAL
                        InlineStackAlign.END -> Gravity.BOTTOM
                        else -> Gravity.TOP
                    }
                }
            }
        }

    private fun stretchesInRow(node: InlineNode): Boolean = when (node) {
        is InlineNode.Image, is InlineNode.Video, is InlineNode.Stack, is InlineNode.Pager -> true
        is InlineNode.Text -> node.lines != 1
        else -> false
    }

    private fun overlay(node: InlineNode.Stack, enclosingTap: (() -> Unit)?): OverlayLayout {
        val layout = OverlayLayout(context, px(node.spacing))
        node.children.forEach { layout.addChild(build(it, enclosingTap), it.position) }
        return layout
    }

    private fun pager(node: InlineNode.Pager, enclosingTap: (() -> Unit)?): InlinePagerView {
        val pages = node.pages.map { build(it, enclosingTap) }
        val color = themed(node.indicatorColor, node.dark?.indicatorColor, LABEL_LIGHT, LABEL_DARK)
        val intervalMs = node.intervalSec?.let { (it * 1000).toLong() }
        return InlinePagerView(context, pages, intervalMs, color, enclosingTap).also { pagers += it }
    }

    private fun typeface(weight: InlineWeight): Typeface {
        val value = when (weight) {
            InlineWeight.REGULAR -> 400
            InlineWeight.MEDIUM -> 500
            InlineWeight.SEMIBOLD -> 600
            InlineWeight.BOLD -> 700
            InlineWeight.HEAVY -> 800
            InlineWeight.BLACK -> 900
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Typeface.create(Typeface.DEFAULT, value, false)
        } else {
            Typeface.create(Typeface.DEFAULT, if (value >= 600) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    /** GradientDrawable draws only 45° steps: the angle is rounded to the nearest one. */
    private fun orientation(angle: Float): GradientDrawable.Orientation {
        val normalized = ((angle % 360f) + 360f) % 360f
        return ORIENTATIONS[(normalized / 45f).roundToInt() % ORIENTATIONS.size]
    }

    private fun px(dp: Float): Int = (dp * density).roundToInt()

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
        const val MAX_FONT_SCALE = 1.6f
        const val DEFAULT_IMAGE_ASPECT = 2f
        const val MIN_ASPECT = 0.1f
        const val MAX_ASPECT = 20f
        const val LABEL_LIGHT = 0xFF000000.toInt()
        const val LABEL_DARK = 0xFFFFFFFF.toInt()
        const val SEPARATOR_LIGHT = 0x4A3C3C43
        const val SEPARATOR_DARK = 0x99545458.toInt()
        const val GLASS_LIGHT = 0x59FFFFFF
        const val GLASS_DARK = 0x59000000
        val PLAIN = InlineStyle()

        /** Clockwise from left-to-right in 45° steps. */
        val ORIENTATIONS = arrayOf(
            GradientDrawable.Orientation.LEFT_RIGHT,
            GradientDrawable.Orientation.TL_BR,
            GradientDrawable.Orientation.TOP_BOTTOM,
            GradientDrawable.Orientation.TR_BL,
            GradientDrawable.Orientation.RIGHT_LEFT,
            GradientDrawable.Orientation.BR_TL,
            GradientDrawable.Orientation.BOTTOM_TOP,
            GradientDrawable.Orientation.BL_TR,
        )
    }
}
