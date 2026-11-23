package com.pushwoosh.pushmart.ui.promo

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextPaint
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.view.doOnLayout
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.catalog.Catalog
import com.pushwoosh.pushmart.databinding.FragmentSaleBinding
import com.pushwoosh.pushmart.ui.home.Season

/** The season's edit (`pushwoosh://sale`, the Home season card), as iOS `SaleView`. */
class SaleFragment : FullBleedFragment(R.layout.fragment_sale) {
    private val animators = mutableListOf<Animator>()

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentSaleBinding.bind(view)
        val context = requireContext()
        val season = Season.current
        val gradient = season.gradient.map { context.getColor(it) }.toIntArray()
        view.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, gradient)
        glow(binding.glowA, context.getColor(season.glowA))
        glow(binding.glowB, context.getColor(season.glowB))

        val ticker = getString(season.ticker)
        binding.ticker.text = ticker.repeat(TICKER_COPIES)
        binding.ticker.setTextColor(gradient[1])
        binding.tickerScroll.setOnTouchListener { _, _ -> true }
        val (top, bottom) = getString(season.title).split("\n")
        binding.titleTop.text = top
        binding.titleBottom.text = bottom
        binding.symbol.setImageResource(season.symbol)
        binding.symbol.imageTintList = ColorStateList.valueOf(context.getColor(season.symbolColor))
        binding.blurb.setText(season.blurb)
        binding.picks.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.picks.adapter = SalePickAdapter(Catalog.featured) { close(toShop = true) }
        binding.shopButton.setTextColor(gradient[0])
        binding.shopButton.iconTint = ColorStateList.valueOf(gradient[0])
        binding.shopButton.setOnClickListener { close(toShop = true) }
        binding.close.setOnClickListener { close(toShop = false) }

        binding.titleColumn.doOnLayout {
            fit(binding.titleTop)
            fit(binding.titleBottom)
        }
        binding.ticker.doOnLayout { scroll(binding.ticker, ticker) }
        pulse(binding)
        reveal(
            listOf(
                binding.tickerBand,
                binding.newSeason,
                binding.titleRow,
                binding.blurb,
                binding.picks,
                binding.shopButton))
    }

    override fun onDestroyView() {
        animators.forEach { it.cancel() }
        animators.clear()
        super.onDestroyView()
    }

    private fun close(toShop: Boolean) {
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.saleFragment) return
        val tabs = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        navController.navigateUp()
        if (toShop) tabs.selectedItemId = R.id.shopFragment
    }

    /** iOS `minimumScaleFactor(0.5)`, one line at a time. */
    private fun fit(line: TextView) {
        val paint = TextPaint(line.paint).apply { textSize = sp(TITLE_SP) }
        val width = (line.width - line.paddingStart - line.paddingEnd).toFloat()
        val size =
            (TITLE_SP * width / paint.measureText(line.text.toString())).coerceIn(
                TITLE_SP * MIN_TITLE_SCALE, TITLE_SP)
        line.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    }

    private fun scroll(text: TextView, copy: String) {
        val width = text.paint.measureText(copy)
        val speed = TICKER_DP_PER_SECOND * resources.displayMetrics.density
        start(
            ObjectAnimator.ofFloat(text, View.TRANSLATION_X, 0f, -width).apply {
                duration = (width / speed * 1000).toLong()
                interpolator = LinearInterpolator()
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
            })
    }

    private fun pulse(binding: FragmentSaleBinding) {
        val dp = resources.displayMetrics.density
        start(
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = GLOW_MS
                interpolator = AccelerateDecelerateInterpolator()
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                addUpdateListener {
                    val f = it.animatedValue as Float
                    binding.glowA.translationY = lerp(-250f, -300f, f) * dp
                    binding.glowB.translationY = lerp(360f, 320f, f) * dp
                    binding.symbol.rotation = lerp(-10f, 30f, f)
                }
            })
    }

    private fun reveal(views: List<View>) {
        val rise = REVEAL_RISE_DP * resources.displayMetrics.density
        views.zip(REVEAL_DELAYS_MS) { view, delay ->
            view.alpha = 0f
            view.translationY = rise
            start(
                ObjectAnimator.ofPropertyValuesHolder(
                        view,
                        PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
                        PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, rise, 0f))
                    .apply {
                        duration = REVEAL_MS
                        startDelay = delay
                        interpolator = DecelerateInterpolator()
                    })
        }
    }

    private fun start(animator: Animator) {
        animators += animator
        animator.start()
    }

    /** A soft disc: the color fades to its own zero-alpha self, so the edge has no dark fringe. */
    private fun glow(view: View, @ColorInt color: Int) {
        view.background =
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = view.layoutParams.width / 2f
                colors = intArrayOf(color, ColorUtils.setAlphaComponent(color, 0))
            }
    }

    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

    private fun lerp(from: Float, to: Float, fraction: Float): Float = from + (to - from) * fraction

    private companion object {
        const val TITLE_SP = 66f
        const val MIN_TITLE_SCALE = 0.5f
        const val TICKER_COPIES = 3
        const val TICKER_DP_PER_SECOND = 40f
        const val GLOW_MS = 3500L
        const val REVEAL_MS = 600L
        const val REVEAL_RISE_DP = 26f
        val REVEAL_DELAYS_MS = listOf(0L, 50L, 120L, 180L, 240L, 300L)
    }
}
