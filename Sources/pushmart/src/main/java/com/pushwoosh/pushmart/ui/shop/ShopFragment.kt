package com.pushwoosh.pushmart.ui.shop

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.catalog.Catalog
import com.pushwoosh.pushmart.databinding.FragmentShopBinding
import com.pushwoosh.pushmart.databinding.ViewShopHeaderBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.Chips
import com.pushwoosh.pushmart.ui.common.ProductCardAdapter
import com.pushwoosh.pushmart.ui.common.ProductGrid
import com.pushwoosh.pushmart.ui.common.categoryTitle

class ShopFragment : Fragment(R.layout.fragment_shop) {
    private var binding: FragmentShopBinding? = null
    private var header: ViewShopHeaderBinding? = null
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
        val header = ViewShopHeaderBinding.inflate(layoutInflater)
        this.header = header

        for (name in Catalog.categories) {
            val chip =
                Chips.make(
                    header.categories, requireContext().categoryTitle(name), name == category)
            chip.setOnClickListener {
                category = name
                Chips.selectOnly(header.categories, chip)
                refresh()
            }
            header.categories.addView(chip)
        }
        ProductGrid.attach(binding.grid, header.root, adapter)
        binding.search.doAfterTextChanged { refresh() }
        binding.search.setOnEditorActionListener { field, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return@setOnEditorActionListener false
            WindowCompat.getInsetsController(requireActivity().window, field)
                .hide(WindowInsetsCompat.Type.ime())
            field.clearFocus()
            true
        }
        binding.clearSearch.setOnClickListener { binding.search.text.clear() }
        binding.cartButton.setOnClickListener {
            if (CartSheet.show(parentFragmentManager))
                services.shopEvents.basketOpened(services.cart)
        }
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
        header = null
        adapter = null
        super.onDestroyView()
    }

    private fun refresh() {
        val binding = binding ?: return
        val header = header ?: return
        val query = binding.search.text.toString()
        val results = Catalog.search(category, query)
        adapter?.submit(results)
        header.empty.text = getString(R.string.shop_no_results, query)
        header.empty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
        binding.clearSearch.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
        renderCartBadge()
    }

    private fun renderCartBadge() {
        val binding = binding ?: return
        val count = services.cart.count
        binding.cartBadge.text = count.toString()
        binding.cartBadge.visibility = if (count == 0) View.GONE else View.VISIBLE
        binding.cartButton.contentDescription =
            if (count == 0) getString(R.string.shop_bag)
            else getString(R.string.shop_bag_items, count)
    }

    companion object {
        private const val STATE_CATEGORY = "category"
    }
}
