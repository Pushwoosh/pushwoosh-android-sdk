package com.pushwoosh.pushmart.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.catalog.Catalog
import com.pushwoosh.pushmart.databinding.FragmentHomeBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.ProductCardAdapter
import com.pushwoosh.pushmart.ui.shop.ProductDetailSheet

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var binding: FragmentHomeBinding? = null
    private var adapter: ProductCardAdapter? = null
    private val wishlistListener: () -> Unit = { adapter?.notifyDataSetChanged() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentHomeBinding.bind(view)
        this.binding = binding
        binding.hero.setOnClickListener { findNavController().navigate(R.id.saleFragment) }
        binding.seeAll.setOnClickListener {
            requireActivity().findViewById<BottomNavigationView>(R.id.nav_view).selectedItemId =
                R.id.shopFragment
        }
        val adapter =
            ProductCardAdapter(services.wishlist) {
                ProductDetailSheet.show(parentFragmentManager, it)
            }
        this.adapter = adapter
        binding.featured.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.featured.adapter = adapter
        adapter.submit(Catalog.featured)
        services.wishlist.addListener(wishlistListener)
    }

    override fun onResume() {
        super.onResume()
        val first = services.account.firstName
        binding?.greeting?.text = if (first.isEmpty()) "Welcome back" else "Hi, $first"
        adapter?.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        services.wishlist.removeListener(wishlistListener)
        binding = null
        adapter = null
        super.onDestroyView()
    }
}
