package com.pushwoosh.demoapp.ui.inline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.pushwoosh.inline.PushwooshInlineView
import com.pushwoosh.sampleapp.databinding.FragmentInlineBinding

/**
 * Test screen for inline slots: three XML-declared slots separated by fillers, plus a fixed control
 * bar for manual requests. Heights and marker positions are logged under [InlineQa.TAG].
 */
class InlineFragment : Fragment() {

    private var binding: FragmentInlineBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInlineBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding!!

        val screenHeight = resources.displayMetrics.heightPixels
        b.fillerAfterHome.layoutParams.height = (screenHeight * 1.5f).toInt()
        b.fillerAfterShop.layoutParams.height = screenHeight

        trackSlot(b.slotHomeDeals, b.markerHomeDeals, "home_deals")
        trackSlot(b.slotShopGrid, b.markerShopGrid, "shop_grid")
        trackSlot(b.slotCartCheckout, b.markerCartCheckout, "cart_checkout")

        b.requestHomeDealsButton.setOnClickListener { InlineQa.request(listOf("home_deals")) }
        b.request11SlotsButton.setOnClickListener { InlineQa.request((0..10).map { "slot_$it" }) }
        b.setUserIdButton.setOnClickListener { InlineQa.setUserId(b.userIdInput.text.toString()) }
        b.postEventButton.setOnClickListener { InlineQa.postEvent(b.eventInput.text.toString()) }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun trackSlot(slot: PushwooshInlineView, marker: View, slotId: String) {
        slot.setOnHeightChangeListener { heightDp ->
            InlineQa.log("height slot=$slotId dp=$heightDp")
        }
        marker.addOnLayoutChangeListener { _, _, top, _, _, _, _, _, _ ->
            InlineQa.log("marker $slotId top=$top")
        }
    }
}
