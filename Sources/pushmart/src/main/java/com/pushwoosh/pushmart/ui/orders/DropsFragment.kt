package com.pushwoosh.pushmart.ui.orders

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.FragmentDropsBinding
import com.pushwoosh.pushmart.databinding.ItemDropCardBinding
import com.pushwoosh.pushmart.live.Drop
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.PageSheet
import com.pushwoosh.pushmart.ui.common.categoryTitle
import java.util.Date
import kotlin.math.roundToInt

/**
 * Orders > More for you > Upcoming drops, as iOS `LiveScoreView`: each drop leads with its photo,
 * then Remind me, or the time its reminder is set for with Cancel.
 */
class DropsFragment : Fragment(R.layout.fragment_drops) {
    private var binding: FragmentDropsBinding? = null
    private val cards = mutableListOf<Pair<Drop, ItemDropCardBinding>>()
    private val dropsListener: () -> Unit = { render() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentDropsBinding.bind(view)
        this.binding = binding
        binding.dropsClose.setOnClickListener { findNavController().popBackStack() }
        val context = requireContext()
        val gap = (CARD_GAP_DP * resources.displayMetrics.density).roundToInt()
        cards.clear()
        services.drops.drops.forEachIndexed { index, drop ->
            val card = ItemDropCardBinding.inflate(layoutInflater, binding.dropsList, true)
            if (index > 0) {
                card.root.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = gap }
            }
            bindPhoto(card.dropPhoto, drop)
            card.dropCategory.text = context.categoryTitle(drop.category)
            card.dropName.text = drop.name
            card.dropTagline.setText(drop.tagline)
            card.dropRemind.contentDescription = getString(R.string.drop_remind_cd, drop.name)
            card.dropRemind.setOnClickListener {
                PageSheet.showOnce(
                    childFragmentManager, DropTimeSheet.newInstance(drop.id), DropTimeSheet.TAG)
            }
            card.dropCancel.contentDescription = getString(R.string.drop_cancel_cd, drop.name)
            card.dropCancel.setOnClickListener { services.drops.cancel(drop) }
            cards.add(drop to card)
        }
        services.drops.addListener(dropsListener)
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onDestroyView() {
        services.drops.removeListener(dropsListener)
        cards.clear()
        binding = null
        super.onDestroyView()
    }

    private fun render() {
        val binding = binding ?: return
        val drops = services.drops
        binding.dropsSubtitle.setText(
            if (drops.countdownAvailable) R.string.drops_subtitle_countdown
            else R.string.drops_subtitle_reminder)
        val timeFormat = DateFormat.getTimeFormat(requireContext())
        for ((drop, card) in cards) {
            val start = drops.scheduledAt(drop)
            card.dropRemind.isVisible = start == null
            card.dropScheduled.isVisible = start != null
            if (start != null) {
                card.dropScheduledText.text =
                    getString(R.string.drop_reminder_set, timeFormat.format(Date(start)))
            }
        }
    }

    companion object {
        private const val CARD_GAP_DP = 32

        /** Shows the photo of [drop] in [view]; a missing photo leaves it empty. */
        @SuppressLint("DiscouragedApi")
        fun bindPhoto(view: ImageView, drop: Drop) {
            val context = view.context
            val id = context.resources.getIdentifier(drop.photo, "drawable", context.packageName)
            if (id != 0) view.setImageResource(id)
        }
    }
}
