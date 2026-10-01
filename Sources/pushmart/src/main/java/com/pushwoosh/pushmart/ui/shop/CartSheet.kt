package com.pushwoosh.pushmart.ui.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.SheetCartBinding
import com.pushwoosh.pushmart.services

class CartSheet : BottomSheetDialogFragment() {
    private var binding: SheetCartBinding? = null
    private var adapter: CartRowAdapter? = null
    private var placed = false
    private val cartListener: () -> Unit = { render() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.sheet_cart, container, false)

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog ?: return
        dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let {
            it.layoutParams = it.layoutParams.apply { height = ViewGroup.LayoutParams.MATCH_PARENT }
        }
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = SheetCartBinding.bind(view)
        this.binding = binding
        placed = savedInstanceState?.getBoolean(STATE_PLACED) ?: false
        val adapter = CartRowAdapter(services.cart)
        this.adapter = adapter
        binding.rows.layoutManager = LinearLayoutManager(requireContext())
        binding.rows.adapter = adapter
        binding.close.setOnClickListener { dismiss() }
        binding.keepShopping.setOnClickListener { dismiss() }
        binding.checkout.setOnClickListener {
            placed = true
            services.purchaseReporter.report(services.cart)
            services.cart.clear()
            render()
        }
        services.cart.addListener(cartListener)
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_PLACED, placed)
    }

    override fun onDestroyView() {
        services.cart.removeListener(cartListener)
        binding = null
        adapter = null
        super.onDestroyView()
    }

    private fun render() {
        val binding = binding ?: return
        val cart = services.cart
        binding.statePlaced.visibility = if (placed) View.VISIBLE else View.GONE
        binding.stateEmpty.visibility = if (!placed && cart.count == 0) View.VISIBLE else View.GONE
        binding.stateFilled.visibility = if (!placed && cart.count > 0) View.VISIBLE else View.GONE
        if (!placed && cart.count > 0) {
            adapter?.submit(cart.lines)
            binding.total.text = cart.totalText
        }
    }

    companion object {
        private const val STATE_PLACED = "placed"
    }
}
