package com.pushwoosh.pushmart.ui.orders

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.FragmentOrdersBinding
import com.pushwoosh.pushmart.databinding.ItemOrderCardBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.ResultBus
import kotlin.math.roundToInt

class OrdersFragment : Fragment(R.layout.fragment_orders) {
    private var binding: FragmentOrdersBinding? = null
    private val ordersListener: () -> Unit = { render() }
    private val liveListener: () -> Unit = { render() }
    private val wishlistListener: () -> Unit = { render() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentOrdersBinding.bind(view)
        this.binding = binding
        binding.ordersStartShopping.setOnClickListener {
            requireActivity().findViewById<BottomNavigationView>(R.id.nav_view).selectedItemId =
                R.id.shopFragment
        }
        binding.moreDrops.root.setOnClickListener {
            val nav = findNavController()
            if (nav.currentDestination?.id == R.id.ordersFragment) nav.navigate(R.id.dropsFragment)
        }
        services.orders.addListener(ordersListener)
        services.liveOrders.addListener(liveListener)
        services.wishlist.addListener(wishlistListener)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onDestroyView() {
        services.orders.removeListener(ordersListener)
        services.liveOrders.removeListener(liveListener)
        services.wishlist.removeListener(wishlistListener)
        binding = null
        super.onDestroyView()
    }

    private fun render() {
        val binding = binding ?: return
        val context = requireContext()
        val orders = services.orders.orders
        binding.ordersSubtitle.text =
            if (orders.isEmpty()) getString(R.string.orders_subtitle_empty)
            else OrderText.orders(context, orders.size)
        binding.ordersEmpty.isVisible = orders.isEmpty()
        binding.ordersList.isVisible = orders.isNotEmpty()
        binding.ordersList.removeAllViews()
        val live = services.liveOrders
        val trackable = live.isAvailable
        val trackedId = if (trackable) live.trackedOrderId else null
        orders.forEachIndexed { index, order ->
            val card = ItemOrderCardBinding.inflate(layoutInflater, binding.ordersList, true)
            card.date.text = OrderText.shortDate(order.placedAtMillis)
            OrderThumbnails.bind(card.thumbnails, order.products, if (index == 0) 72 else 54)
            card.itemsSummary.text = OrderText.summary(context, order)
            card.itemsTotal.text =
                getString(
                    R.string.order_items_total,
                    OrderText.items(context, order.productIds.size),
                    order.totalText,
                )
            card.number.text = getString(R.string.order_number, order.id)
            val openDetails =
                View.OnClickListener { OrderDetailSheet.show(parentFragmentManager, order) }
            card.orderSummary.setOnClickListener(openDetails)
            card.viewDetails.setOnClickListener(openDetails)
            card.buyAgain.setOnClickListener {
                OrderText.buyAgain(requireContext(), order, services.cart)
            }
            card.trackRow.isVisible = index == 0 && trackable
            if (card.trackRow.isVisible) {
                card.trackSwitch.isChecked = trackedId == order.id
                card.trackSwitch.setOnCheckedChangeListener { _, checked ->
                    if (checked) live.start(order.id) else live.stop()
                }
            }
        }
        val gap = if (orders.isEmpty()) MORE_GAP_EMPTY_DP else MORE_GAP_LIST_DP
        binding.moreForYou.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = (gap * resources.displayMetrics.density).roundToInt()
        }
        renderMore(binding)
    }

    /** More for you, as iOS `moreForYou`: Upcoming drops, then Keep your picks close. */
    private fun renderMore(binding: FragmentOrdersBinding) {
        val drops = binding.moreDrops
        drops.moreIcon.setImageResource(R.drawable.ic_flame)
        drops.moreTitle.setText(R.string.drops_title)
        drops.moreSubtitle.setText(
            if (services.drops.countdownAvailable) R.string.more_drops_subtitle_countdown
            else R.string.more_drops_subtitle_reminder)

        val pins = services.wishlistPins
        binding.moreDivider.isVisible = pins.isAvailable
        binding.morePicks.root.isVisible = pins.isAvailable
        if (!pins.isAvailable) return
        val picks = binding.morePicks
        if (pins.isRunning) {
            picks.moreIcon.setImageResource(R.drawable.ic_push_pin)
            picks.moreTitle.setText(R.string.more_picks_running_title)
            picks.moreSubtitle.setText(R.string.more_picks_running_subtitle)
            picks.moreTrailing.isVisible = false
            picks.root.setOnClickListener {
                pins.unpinAll()
                render()
            }
            return
        }
        val count = services.wishlist.products.size
        picks.moreIcon.setImageResource(R.drawable.ic_heart)
        picks.moreTitle.setText(R.string.more_picks_title)
        picks.moreSubtitle.setText(R.string.more_picks_subtitle)
        picks.moreTrailing.isVisible = count > 0
        picks.moreTrailing.text = count.toString()
        picks.root.setOnClickListener {
            val products = services.wishlist.products
            if (products.isEmpty()) {
                ResultBus.fail(
                    getString(R.string.picks_empty_title), getString(R.string.picks_empty_body))
            } else {
                pins.pin(products) { render() }
            }
        }
    }

    companion object {
        private const val MORE_GAP_EMPTY_DP = 40
        // Each order card keeps a 16dp bottom margin: 24 + 16 = iOS 40.
        private const val MORE_GAP_LIST_DP = 24
    }
}
