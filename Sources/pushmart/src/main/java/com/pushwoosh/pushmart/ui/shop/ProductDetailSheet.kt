package com.pushwoosh.pushmart.ui.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.catalog.Catalog
import com.pushwoosh.pushmart.catalog.Product
import com.pushwoosh.pushmart.databinding.SheetProductDetailBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.Chips
import com.pushwoosh.pushmart.ui.common.Hearts
import com.pushwoosh.pushmart.ui.common.MiniProductAdapter
import com.pushwoosh.pushmart.ui.common.PageSheet
import com.pushwoosh.pushmart.ui.common.ProductImages

class ProductDetailSheet : BottomSheetDialogFragment() {
    private var option: String = ""
    private var added = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.sheet_product_detail, container, false)

    override fun getTheme(): Int = R.style.ThemeOverlay_PushMart_BottomSheet_Page

    override fun onStart() {
        super.onStart()
        PageSheet.expand(dialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = SheetProductDetailBinding.bind(view)
        val product = Catalog.byId(requireArguments().getString(ARG_ID).orEmpty())
        if (product == null) {
            dismiss()
            return
        }
        services.recentlyViewed.record(product.id)
        option = savedInstanceState?.getString(STATE_OPTION) ?: product.sizes.first()
        added = savedInstanceState?.getBoolean(STATE_ADDED) ?: false

        ProductImages.bind(binding.image, product)
        binding.name.text = product.name
        binding.price.text = product.priceText

        for (value in product.sizes) {
            val chip = Chips.make(binding.options, value, value == option)
            chip.setOnClickListener {
                option = value
                Chips.selectOnly(binding.options, chip)
            }
            binding.options.addView(chip)
        }

        binding.blurb.text = product.blurb

        binding.priceAlert.isChecked = services.priceAlerts.isOn(product.id)
        binding.priceAlert.setOnCheckedChangeListener { _, checked ->
            services.priceAlerts.set(checked, product)
        }

        binding.related.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val related = MiniProductAdapter { show(parentFragmentManager, it) }
        related.submit(Catalog.related(product))
        binding.related.adapter = related

        Hearts.bind(binding.heart, services.wishlist.contains(product.id))
        binding.heart.setOnClickListener {
            services.wishlist.toggle(product)
            Hearts.bind(binding.heart, services.wishlist.contains(product.id))
        }
        binding.close.setOnClickListener { dismiss() }

        if (added) showAdded(binding)
        binding.addToBag.setOnClickListener {
            services.cart.add(product.id)
            added = true
            showAdded(binding)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_OPTION, option)
        outState.putBoolean(STATE_ADDED, added)
    }

    private fun showAdded(binding: SheetProductDetailBinding) {
        binding.addToBag.setText(R.string.pdp_added_to_bag)
        binding.addToBag.setIconResource(R.drawable.ic_check)
    }

    companion object {
        const val ARG_ID = "productId"
        private const val STATE_OPTION = "option"
        private const val STATE_ADDED = "added"

        fun show(fm: FragmentManager, product: Product): Boolean {
            val sheet = ProductDetailSheet()
            sheet.arguments = bundleOf(ARG_ID to product.id)
            return PageSheet.showOnce(fm, sheet, "product-" + product.id)
        }
    }
}
