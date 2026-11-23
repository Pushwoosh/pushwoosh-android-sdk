package com.pushwoosh.pushmart.ui.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.SheetCartBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.PageSheet
import com.pushwoosh.pushmart.ui.profile.PersonalCodeSheet

class CartSheet : BottomSheetDialogFragment() {
    private var binding: SheetCartBinding? = null
    private var adapter: CartRowAdapter? = null
    private var placed = false
    private var placedTracked = false
    private val cartListener: () -> Unit = { render() }
    private val switchListener: () -> Unit = { render() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.sheet_cart, container, false)

    override fun getTheme(): Int = R.style.ThemeOverlay_PushMart_BottomSheet_Page

    override fun onStart() {
        super.onStart()
        PageSheet.expand(dialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = SheetCartBinding.bind(view)
        this.binding = binding
        placed = savedInstanceState?.getBoolean(STATE_PLACED) ?: false
        placedTracked = savedInstanceState?.getBoolean(STATE_PLACED_TRACKED) ?: false
        val adapter = CartRowAdapter(services.cart)
        this.adapter = adapter
        binding.rows.layoutManager = LinearLayoutManager(requireContext())
        binding.rows.adapter = adapter
        binding.close.setOnClickListener { dismiss() }
        binding.keepShopping.setOnClickListener { dismiss() }
        binding.clear.setOnClickListener { services.shopEvents.clearBag(services.cart) }
        binding.codeNeeded.setOnClickListener {
            PageSheet.showOnce(parentFragmentManager, PersonalCodeSheet(), PersonalCodeSheet.TAG)
        }
        binding.checkout.setOnClickListener {
            if (placed || services.account.isGuest || services.cart.count == 0)
                return@setOnClickListener
            placed = true
            val order = services.purchaseReporter.checkout(services.cart)
            placedTracked = order != null && services.liveOrders.startIfAllowed(order.id)
            render()
        }
        services.cart.addListener(cartListener)
        services.accountManager.addSwitchListener(switchListener)
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_PLACED, placed)
        outState.putBoolean(STATE_PLACED_TRACKED, placedTracked)
    }

    override fun onDestroyView() {
        services.cart.removeListener(cartListener)
        services.accountManager.removeSwitchListener(switchListener)
        binding = null
        adapter = null
        super.onDestroyView()
    }

    private fun render() {
        val binding = binding ?: return
        val cart = services.cart
        binding.statePlaced.visibility = if (placed) View.VISIBLE else View.GONE
        binding.placedBody.setText(
            if (placedTracked) R.string.cart_placed_body_live else R.string.cart_placed_body)
        binding.stateEmpty.visibility = if (!placed && cart.count == 0) View.VISIBLE else View.GONE
        binding.stateFilled.visibility = if (!placed && cart.count > 0) View.VISIBLE else View.GONE
        if (!placed && cart.count > 0) {
            adapter?.submit(cart.lines)
            binding.total.text = cart.totalText
            val guest = services.account.isGuest
            binding.codeNeeded.isVisible = guest
            binding.checkout.isEnabled = !guest
            binding.checkout.alpha = if (guest) 0.5f else 1f
        }
    }

    companion object {
        const val TAG = "cart"
        private const val STATE_PLACED = "placed"
        private const val STATE_PLACED_TRACKED = "placedTracked"

        fun show(fm: FragmentManager): Boolean = PageSheet.showOnce(fm, CartSheet(), TAG)
    }
}
