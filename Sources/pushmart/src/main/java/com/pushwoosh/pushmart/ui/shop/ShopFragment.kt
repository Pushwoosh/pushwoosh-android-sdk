package com.pushwoosh.pushmart.ui.shop

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.catalog.Catalog
import com.pushwoosh.pushmart.databinding.FragmentShopBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.Chips
import com.pushwoosh.pushmart.ui.common.ProductCardAdapter

class ShopFragment : Fragment(R.layout.fragment_shop) {
    private var binding: FragmentShopBinding? = null
    private var adapter: ProductCardAdapter? = null
    private var category = "All"
    private val cartListener: () -> Unit = { renderCartBadge() }
    private val wishlistListener: () -> Unit = { adapter?.notifyDataSetChanged() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentShopBinding.bind(view)
        this.binding = binding
        category = savedInstanceState?.getString(STATE_CATEGORY) ?: "All"

        val adapter =
            ProductCardAdapter(services.wishlist) {
                ProductDetailSheet.show(parentFragmentManager, it)
            }
        this.adapter = adapter
        binding.grid.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.grid.adapter = adapter

        for (name in Catalog.categories) {
            val chip = Chips.make(binding.categories, name, name == category)
            chip.setOnClickListener {
                category = name
                Chips.selectOnly(binding.categories, chip)
                refresh()
            }
            binding.categories.addView(chip)
        }
        binding.search.doAfterTextChanged { refresh() }
        binding.cartButton.setOnClickListener { CartSheet().show(parentFragmentManager, "cart") }
        services.cart.addListener(cartListener)
        services.wishlist.addListener(wishlistListener)
        refresh()
    }

    override fun onResume() {
        super.onResume()
        adapter?.notifyDataSetChanged()
        renderCartBadge()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_CATEGORY, category)
    }

    override fun onDestroyView() {
        services.cart.removeListener(cartListener)
        services.wishlist.removeListener(wishlistListener)
        binding = null
        adapter = null
        super.onDestroyView()
    }

    private fun refresh() {
        val binding = binding ?: return
        val query = binding.search.text.toString()
        val results = Catalog.search(category, query)
        adapter?.submit(results)
        binding.empty.text = "No products match “" + query.trim() + "”."
        binding.empty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
        binding.grid.visibility = if (results.isEmpty()) View.GONE else View.VISIBLE
        renderCartBadge()
    }

    private fun renderCartBadge() {
        val binding = binding ?: return
        val count = services.cart.count
        binding.cartBadge.text = if (count > 9) "9+" else count.toString()
        binding.cartBadge.visibility = if (count == 0) View.GONE else View.VISIBLE
    }

    companion object {
        private const val STATE_CATEGORY = "category"
    }
}
