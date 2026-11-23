package com.pushwoosh.pushmart.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.ItemOrderLineBinding
import com.pushwoosh.pushmart.databinding.SheetOrderDetailBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.shop.Order
import com.pushwoosh.pushmart.ui.common.PageSheet
import com.pushwoosh.pushmart.ui.common.ProductImages
import com.pushwoosh.pushmart.ui.common.categoryTitle
import kotlin.math.roundToInt

/** One order end to end, as iOS `OrderDetailView`: status, items, summary and Buy again. */
class OrderDetailSheet : BottomSheetDialogFragment() {
    private var binding: SheetOrderDetailBinding? = null
    private var orderId: String? = null
    private val liveListener: () -> Unit = { bindTracking() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.sheet_order_detail, container, false)

    override fun getTheme(): Int = R.style.ThemeOverlay_PushMart_BottomSheet_Page

    override fun onStart() {
        super.onStart()
        PageSheet.expand(dialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = SheetOrderDetailBinding.bind(view)
        val order = services.orders.byId(requireArguments().getString(ARG_ID).orEmpty())
        if (order == null) {
            dismiss()
            return
        }
        val context = requireContext()
        binding.title.text = getString(R.string.order_number, order.id)
        binding.placed.text =
            getString(R.string.order_placed_on, OrderText.longDateTime(order.placedAtMillis))
        binding.confirmedAt.text = OrderText.mediumDateTime(order.placedAtMillis)
        this.binding = binding
        orderId = order.id
        bindTracking()
        services.liveOrders.addListener(liveListener)

        val products = order.products
        binding.itemsTitle.text = getString(R.string.order_section_items, products.size)
        val gap = (LINE_GAP_DP * resources.displayMetrics.density).roundToInt()
        products.forEachIndexed { index, product ->
            val line = ItemOrderLineBinding.inflate(layoutInflater, binding.items, true)
            if (index > 0) {
                line.root.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = gap }
            }
            ProductImages.bind(line.image, product)
            line.name.text = product.name
            line.category.text = context.categoryTitle(product.category)
            line.price.text = product.priceText
        }
        binding.itemsCount.text = products.size.toString()
        binding.total.text = order.totalText

        binding.buyAgain.setOnClickListener {
            OrderText.buyAgain(requireContext(), order, services.cart)
        }
        binding.close.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        services.liveOrders.removeListener(liveListener)
        binding = null
        super.onDestroyView()
    }

    private fun bindTracking() {
        val binding = binding ?: return
        val id = orderId ?: return
        val live = services.liveOrders
        val available = live.isAvailable
        binding.trackDivider.isVisible = available
        binding.trackRow.isVisible = available
        if (!available) return
        binding.trackSwitch.setOnCheckedChangeListener(null)
        binding.trackSwitch.isChecked = live.trackedOrderId == id
        binding.trackSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked) live.start(id) else live.stop()
        }
    }

    companion object {
        const val ARG_ID = "orderId"
        private const val LINE_GAP_DP = 12

        fun show(fm: FragmentManager, order: Order): Boolean {
            val sheet = OrderDetailSheet()
            sheet.arguments = bundleOf(ARG_ID to order.id)
            return PageSheet.showOnce(fm, sheet, "order-" + order.id)
        }
    }
}
