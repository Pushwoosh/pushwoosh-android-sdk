package com.pushwoosh.pushmart.ui.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.FragmentWishlistBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.ProductCardAdapter
import com.pushwoosh.pushmart.ui.shop.ProductDetailSheet

class WishlistFragment : Fragment(R.layout.fragment_wishlist) {
    private var binding: FragmentWishlistBinding? = null
    private var adapter: ProductCardAdapter? = null
    private val listener: () -> Unit = { render() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentWishlistBinding.bind(view)
        this.binding = binding
        binding.backButton.back.setOnClickListener { findNavController().navigateUp() }
        val adapter =
            ProductCardAdapter(services.wishlist) {
                ProductDetailSheet.show(parentFragmentManager, it)
            }
        this.adapter = adapter
        binding.grid.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.grid.adapter = adapter
        services.wishlist.addListener(listener)
        render()
    }

    override fun onDestroyView() {
        services.wishlist.removeListener(listener)
        binding = null
        adapter = null
        super.onDestroyView()
    }

    private fun render() {
        val binding = binding ?: return
        val products = services.wishlist.products
        adapter?.submit(products)
        binding.empty.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
        binding.grid.visibility = if (products.isEmpty()) View.GONE else View.VISIBLE
    }
}
