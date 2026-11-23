package com.pushwoosh.inline.internal.html

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import com.pushwoosh.inapp.ui.R
import com.pushwoosh.inline.internal.CoreInlineIdentity
import com.pushwoosh.inline.internal.InlineModule
import com.pushwoosh.inline.internal.MainInlineScheduler
import com.pushwoosh.inline.internal.model.InlineBanner
import com.pushwoosh.inline.internal.view.BannerFrame
import com.pushwoosh.inline.internal.view.InlineContent
import com.pushwoosh.inline.internal.view.InlineContentFactory
import com.pushwoosh.inline.internal.view.InlineContentHost
import com.pushwoosh.inline.internal.view.InlineLinkOpener
import com.pushwoosh.internal.platform.utils.GeneralUtils
import com.pushwoosh.internal.specific.DeviceSpecificProvider
import com.pushwoosh.internal.utils.PWLog
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * The last document height (dp) each HTML banner version reported, so a view drawn again for the same
 * version (reattached, redrawn for a new scheme) keeps its place instead of collapsing until its page
 * reports. In memory, a few recent banners. Main thread only.
 */
internal object InlineHtmlHeights {
    private const val MAX_ENTRIES = 32

    private val heights = object : LinkedHashMap<String, Float>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Float>?): Boolean = size > MAX_ENTRIES
    }

    fun key(banner: InlineBanner): String = "${banner.bannerId}|${banner.version}|${banner.isTest}"

    fun get(key: String): Float? = heights[key]

    fun put(key: String, heightDp: Float) {
        heights[key] = heightDp
    }

    @VisibleForTesting
    fun clear() = heights.clear()
}

/** Draws HTML banners in a [WebView] made by [webViewFactory]; a WebView that cannot be made draws nothing. */
internal class InlineHtmlFactory(private val webViewFactory: (Context) -> WebView = ::WebView) : InlineContentFactory {

    override fun create(context: Context, banner: InlineBanner, dark: Boolean, host: InlineContentHost): InlineContent? {
        val html = banner.html ?: return null
        val webView = try {
            webViewFactory(ContextThemeWrapper(context, if (dark) R.style.PwInlineWeb_Dark else R.style.PwInlineWeb_Light))
        } catch (e: RuntimeException) {
            PWLog.warn(TAG, "No WebView for inline banner '${banner.bannerId}': ${e.message}")
            return null
        }
        val content = InlineHtmlBannerView(context, webView, html, banner, host)
        val close = banner.close?.takeIf { it.enabled } ?: return content
        return object : InlineContent by content {
            override val view: View = BannerFrame(context, content.view, close) { host.onClose() }
        }
    }

    private companion object {
        const val TAG = "InlineHtmlFactory"
    }
}

/**
 * An HTML banner. The page is laid out in a fixed viewport `max(screen width, height)` high, so its
 * `%` and `vh` sizes do not depend on the banner; the banner shows the document height the page
 * reports, at most the viewport. Until the first report the banner is a 1 dp placeholder and not
 * ready. The page is loaded once the banner has a width.
 */
@SuppressLint("ViewConstructor")
internal class InlineHtmlBannerView(
    context: Context,
    private val webView: WebView,
    private val html: String,
    private val banner: InlineBanner,
    private val host: InlineContentHost,
) : FrameLayout(context), InlineContent {

    private val density = resources.displayMetrics.density
    private val viewportPx = max(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val mainHandler = Handler(Looper.getMainLooper())
    private val taps = InlineHtmlTapCoordinator(
        MainInlineScheduler,
        onClick = { host.onClick(it, null) },
        onDismiss = host::onClose,
        onBannerTap = banner.action?.let { action -> { host.onClick(null, action) } },
    )
    private val heightKey = InlineHtmlHeights.key(banner)

    /** The height an earlier view of the same banner version reported; the placeholder until this page reports. */
    private val rememberedHeightPx = InlineHtmlHeights.get(heightKey)?.let { min(ceil(it * density).toInt(), viewportPx) }
    private val runOnMain: InlineMainRunner = { task -> mainHandler.post { if (!disposed) task() } }

    private var loaded = false
    private var disposed = false

    /** The page in the WebView is the banner's own document, not one a form or a reload navigated to. */
    private var ownDocument = false
    private var lastTapAt: Long? = null
    private var heightPx = 0
    private var tapX = 0f
    private var tapY = 0f
    private var tapTracking = false

    override val view: View get() = this

    override var isReady = false
        private set

    init {
        configure()
        addView(webView, LayoutParams(LayoutParams.MATCH_PARENT, viewportPx))
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun configure() {
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.overScrollMode = View.OVER_SCROLL_NEVER
        webView.isLongClickable = false
        webView.setOnLongClickListener { true }
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) disableForceDark(settings)
        val gate = InlineBridgeGate(::isLive) { InlineModule.managerOrNull()?.canPostEvent() == true }
        webView.addJavascriptInterface(InlineHeightJs(runOnMain, ::documentHeightChanged), InlineScripts.HEIGHT_INTERFACE)
        webView.addJavascriptInterface(InlinePushwooshJs(webView, runOnMain, taps, gate), PUSHWOOSH_INTERFACE)
        webView.addJavascriptInterface(InlinePushManagerJs(webView, runOnMain, taps, gate), PUSH_MANAGER_INTERFACE)
        webView.webViewClient = Client()
    }

    /** The slot still holds this banner; an expired one collapses its slot here. */
    private fun isLive(): Boolean = InlineModule.managerOrNull()?.isBannerLive(banner.slotId, banner.bannerId) == true

    @Suppress("DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun disableForceDark(settings: WebSettings) {
        settings.forceDark = WebSettings.FORCE_DARK_OFF
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = getDefaultSize(0, widthMeasureSpec)
        webView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(viewportPx, MeasureSpec.EXACTLY))
        setMeasuredDimension(width, if (isReady) heightPx else rememberedHeightPx ?: ceil(density).toInt())
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (loaded || disposed || width <= 0) return
        loaded = true
        loadBanner()
    }

    private fun loadBanner() {
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        trackTap(ev)
        val handled = super.dispatchTouchEvent(ev)
        // The rest of a gesture reaches the banner only when it takes the down.
        return handled || ev.actionMasked == MotionEvent.ACTION_DOWN
    }

    override fun onDetachedFromWindow() {
        taps.flush()
        super.onDetachedFromWindow()
    }

    override fun retire() {
        taps.flush()
    }

    override fun dispose() {
        if (disposed) return
        taps.flush()
        disposed = true
        webView.removeJavascriptInterface(InlineScripts.HEIGHT_INTERFACE)
        webView.removeJavascriptInterface(PUSHWOOSH_INTERFACE)
        webView.removeJavascriptInterface(PUSH_MANAGER_INTERFACE)
        webView.destroy()
    }

    /** The document's own height in CSS px (= dp), measured in the fixed viewport. */
    private fun documentHeightChanged(documentHeight: Double) {
        if (!documentHeight.isFinite() || documentHeight <= 0) return
        InlineHtmlHeights.put(heightKey, ceil(documentHeight).toFloat())
        val visible = min(ceil(ceil(documentHeight) * density).toInt(), viewportPx)
        if (isReady && visible == heightPx) return
        val becameReady = !isReady
        isReady = true
        heightPx = visible
        requestLayout()
        host.onContentSizeChanged()
        if (becameReady) host.onContentReady()
    }

    /** A tap = up within the touch slop of its down, one pointer. */
    private fun trackTap(ev: MotionEvent) {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                tapX = ev.x
                tapY = ev.y
                tapTracking = true
            }
            MotionEvent.ACTION_MOVE -> if (tapTracking && !withinSlop(ev)) tapTracking = false
            MotionEvent.ACTION_UP -> if (tapTracking && withinSlop(ev)) {
                tapTracking = false
                lastTapAt = SystemClock.uptimeMillis()
                tapped(ev.x - webView.left, ev.y - webView.top)
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> tapTracking = false
        }
    }

    private fun withinSlop(ev: MotionEvent): Boolean = abs(ev.x - tapX) <= touchSlop && abs(ev.y - tapY) <= touchSlop

    private fun tapped(x: Float, y: Float) {
        if (disposed) return
        taps.onTap()
        webView.evaluateJavascript(InlineScripts.hitTest((x / density).toDouble(), (y / density).toDouble())) { result ->
            if (disposed) return@evaluateJavascript
            val hit = InlineScripts.parseHit(result)
            taps.onTapResult(hit?.kind, hit?.id)
        }
    }

    private fun injectPushwooshApi() {
        val identity = CoreInlineIdentity
        val deviceType = DeviceSpecificProvider.getInstance()?.deviceType()?.toString().orEmpty()
        val script = InlineScripts.pushwooshApi(
            hwid = identity.hwid(),
            version = GeneralUtils.SDK_VERSION,
            appCode = identity.appCode().orEmpty(),
            userId = identity.userId().orEmpty(),
            deviceType = deviceType,
        )
        webView.evaluateJavascript(script, null)
    }

    private inner class Client : WebViewClient() {
        private var heightInjected = false

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            if (disposed) return
            ownDocument = isOwnDocument(url.orEmpty())
            if (ownDocument) {
                injectPushwooshApi()
                return
            }
            // A form post or history navigation skips shouldOverrideUrlLoading; the page leaves the banner then.
            webView.stopLoading()
            handle(url.orEmpty(), userActivated = lastTapAt?.let { SystemClock.uptimeMillis() - it <= USER_ACTIVATION_MS } == true)
            if (disposed) return
            heightInjected = false
            loadBanner()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            if (disposed || !ownDocument) return
            injectPushwooshApi()
            if (heightInjected) return
            heightInjected = true
            webView.evaluateJavascript(InlineScripts.NO_SELECTION, null)
            webView.evaluateJavascript(InlineScripts.HEIGHT, null)
        }

        /** The banner is loaded without a base URL, so its document is `about:blank` (or the `data:` URL on some WebViews). */
        private fun isOwnDocument(url: String): Boolean = url == BLANK_URL || url.startsWith(DATA_URL_PREFIX)

        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = handle(url.orEmpty(), userActivated = true)

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            request ?: return true
            if (!request.isForMainFrame && !request.hasGesture()) return false
            return handle(request.url.toString(), request.hasGesture())
        }

        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
            PWLog.warn(TAG, "Inline banner page renderer is gone, the banner collapses")
            if (!disposed) {
                removeView(webView)
                dispose()
                heightPx = 0
                requestLayout()
                host.onContentSizeChanged()
            }
            return true
        }

        private fun handle(url: String, userActivated: Boolean): Boolean {
            if (disposed) return true
            when (InlineLinkPolicy.decide(url)) {
                InlineLinkPolicy.Decision.OPEN_EXTERNAL -> when {
                    !isLive() -> Unit
                    userActivated -> taps.onExternalNavigation { InlineLinkOpener.open(webView.context, url) }
                    else -> PWLog.warn(TAG, "Inline banner page navigation to '$url' without a tap is ignored")
                }
                InlineLinkPolicy.Decision.CLOSE -> taps.onCloseInApp()
                InlineLinkPolicy.Decision.CONSUME -> Unit
            }
            return true
        }
    }

    private companion object {
        const val TAG = "InlineHtmlBannerView"
        const val PUSHWOOSH_INTERFACE = "pushwooshImpl"
        const val PUSH_MANAGER_INTERFACE = "pushManager"
        const val BLANK_URL = "about:blank"
        const val DATA_URL_PREFIX = "data:"

        /** How long after a tap a navigation counts as the user's, as a browser's transient activation. */
        const val USER_ACTIVATION_MS = 5_000L
    }
}
