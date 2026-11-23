package com.pushwoosh.pushmart.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.SheetDealPicksBinding
import com.pushwoosh.pushmart.databinding.ViewDealPicksHeaderBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.PageSheet
import com.pushwoosh.pushmart.ui.common.ProductCardAdapter
import com.pushwoosh.pushmart.ui.common.ProductGrid
import com.pushwoosh.pushmart.ui.shop.ProductDetailSheet

/**
 * A guest's collection, as iOS `DealPicksSheet`: the deal's catalog picks, each opening its
 * product.
 */
class DealPicksSheet : BottomSheetDialogFragment() {
    private var adapter: ProductCardAdapter? = null
    private val wishlistListener: () -> Unit = { adapter?.notifyDataSetChanged() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.sheet_deal_picks, container, false)

    override fun getTheme(): Int = R.style.ThemeOverlay_PushMart_BottomSheet_Page

    override fun onStart() {
        super.onStart()
        PageSheet.expand(dialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = SheetDealPicksBinding.bind(view)
        val name = requireArguments().getString(ARG_DEAL)
        val deal = Deal.values().firstOrNull { it.name == name }
        if (deal == null) {
            dismiss()
            return
        }
        val header = ViewDealPicksHeaderBinding.inflate(layoutInflater)
        header.root.setText(deal.title)
        val adapter =
            ProductCardAdapter(services.wishlist) {
                ProductDetailSheet.show(parentFragmentManager, it)
            }
        this.adapter = adapter
        ProductGrid.attach(binding.grid, header.root, adapter)
        adapter.submit(deal.products)
        services.wishlist.addListener(wishlistListener)
    }

    override fun onDestroyView() {
        services.wishlist.removeListener(wishlistListener)
        adapter = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_DEAL = "deal"

        fun show(fm: FragmentManager, deal: Deal): Boolean {
            val sheet = DealPicksSheet()
            sheet.arguments = bundleOf(ARG_DEAL to deal.name)
            return PageSheet.showOnce(fm, sheet, "deal-" + deal.name)
        }
    }
}
