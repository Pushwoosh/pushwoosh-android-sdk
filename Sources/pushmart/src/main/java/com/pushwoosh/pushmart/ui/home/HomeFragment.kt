package com.pushwoosh.pushmart.ui.home

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.catalog.Catalog
import com.pushwoosh.pushmart.databinding.FragmentHomeBinding
import com.pushwoosh.pushmart.databinding.ViewCollectionChipBinding
import com.pushwoosh.pushmart.databinding.ViewHomeHeaderBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.MiniProductAdapter
import com.pushwoosh.pushmart.ui.common.ProductCardAdapter
import com.pushwoosh.pushmart.ui.common.ProductGrid
import com.pushwoosh.pushmart.ui.common.ProductImages
import com.pushwoosh.pushmart.ui.common.Wordmark
import com.pushwoosh.pushmart.ui.shop.ProductDetailSheet

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var header: ViewHomeHeaderBinding? = null
    private var adapter: ProductCardAdapter? = null
    private var recent: MiniProductAdapter? = null
    private val wishlistListener: () -> Unit = { adapter?.notifyDataSetChanged() }
    private val recentListener: () -> Unit = { renderRecent() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentHomeBinding.bind(view)
        Wordmark.size(binding.wordmark, 22f)
        binding.bell.setOnClickListener { selectTab(R.id.offersFragment) }

        val header = ViewHomeHeaderBinding.inflate(layoutInflater)
        this.header = header
        bindSeason(header, Season.current)
        for (deal in Deal.values()) addChip(header, deal)
        val recent = MiniProductAdapter { ProductDetailSheet.show(parentFragmentManager, it) }
        this.recent = recent
        header.recent.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        header.recent.adapter = recent
        header.seeAll.setOnClickListener { selectTab(R.id.shopFragment) }

        val adapter =
            ProductCardAdapter(services.wishlist) {
                ProductDetailSheet.show(parentFragmentManager, it)
            }
        this.adapter = adapter
        ProductGrid.attach(binding.feed, header.root, adapter)
        adapter.submit(Catalog.featured)

        services.wishlist.addListener(wishlistListener)
        services.recentlyViewed.addListener(recentListener)
        renderRecent()
    }

    override fun onDestroyView() {
        services.wishlist.removeListener(wishlistListener)
        services.recentlyViewed.removeListener(recentListener)
        header = null
        adapter = null
        recent = null
        super.onDestroyView()
    }

    private fun bindSeason(header: ViewHomeHeaderBinding, season: Season) {
        header.hero.backgroundTintList =
            ColorStateList.valueOf(requireContext().getColor(season.card))
        header.seasonEdit.setText(season.edit)
        header.seasonSubtitle.setText(season.heroTitle)
        Catalog.byId(season.heroProductId)?.let { ProductImages.bind(header.seasonImage, it) }
        val openSale =
            View.OnClickListener {
                findNavController().navigate(R.id.saleFragment, null, SINGLE_TOP)
            }
        header.hero.setOnClickListener(openSale)
        header.openEdit.setOnClickListener(openSale)
    }

    private fun addChip(header: ViewHomeHeaderBinding, deal: Deal) {
        val chip = ViewCollectionChipBinding.inflate(layoutInflater, header.collections, true)
        chip.icon.setImageResource(deal.icon)
        chip.icon.imageTintList = ColorStateList.valueOf(requireContext().getColor(deal.tint))
        chip.title.setText(deal.title)
        chip.root.setOnClickListener {
            if (services.account.isGuest) DealPicksSheet.show(parentFragmentManager, deal)
            else services.gateway.postEvent(deal.event, emptyMap())
        }
    }

    private fun renderRecent() {
        val header = header ?: return
        val products = services.recentlyViewed.products
        recent?.submit(products)
        header.recentSection.isVisible = products.isNotEmpty()
    }

    private fun selectTab(@IdRes id: Int) {
        requireActivity().findViewById<BottomNavigationView>(R.id.nav_view).selectedItemId = id
    }

    companion object {
        private val SINGLE_TOP = NavOptions.Builder().setLaunchSingleTop(true).build()
    }
}
