package com.pushwoosh.pushmart.ui.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.catalog.Catalog
import com.pushwoosh.pushmart.catalog.Product
import com.pushwoosh.pushmart.databinding.SheetProductDetailBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.Chips
import com.pushwoosh.pushmart.ui.common.Hearts
import com.pushwoosh.pushmart.ui.common.MiniProductAdapter
import com.pushwoosh.pushmart.ui.common.ProductImages
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ProductDetailSheet : BottomSheetDialogFragment() {
    private var option: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.sheet_product_detail, container, false)

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
        val binding = SheetProductDetailBinding.bind(view)
        val product = Catalog.byId(requireArguments().getString(ARG_ID).orEmpty())
        if (product == null) {
            dismiss()
            return
        }
        if (savedInstanceState == null) services.shopEvents.productOpened(product)
        option = savedInstanceState?.getString(STATE_OPTION) ?: product.optionValues.first()

        ProductImages.bind(binding.image, product)
        binding.badge.text = product.badge?.uppercase()
        binding.badge.visibility = if (product.badge == null) View.GONE else View.VISIBLE
        binding.brand.text = product.brand.uppercase()
        binding.brand.setTextColor(product.tintArgb)
        binding.name.text = product.name
        bindStars(binding.stars, product.rating)
        binding.rating.text = String.format(Locale.US, "%.1f", product.rating)
        binding.reviews.text = "· " + product.reviewsText
        binding.price.text = product.priceText

        binding.optionTitle.text = product.optionTitle
        for (value in product.optionValues) {
            val chip = Chips.make(binding.options, value, value == option)
            chip.setOnClickListener {
                option = value
                Chips.selectOnly(binding.options, chip)
            }
            binding.options.addView(chip)
        }

        binding.deliveryDay.text = "Free delivery by " + deliveryDay()
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

        binding.addToBag.text = "Add to bag · " + product.priceText
        binding.addToBag.setOnClickListener {
            services.cart.add(product.id)
            services.shopEvents.addedToCart(product, option)
            binding.addToBag.text = "Added to bag"
            binding.addToBag.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_check, 0, 0, 0)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_OPTION, option)
    }

    private fun bindStars(container: LinearLayout, rating: Double) {
        val size = (14 * resources.displayMetrics.density).toInt()
        for (i in 0 until 5) {
            val star = ImageView(requireContext())
            star.layoutParams = LinearLayout.LayoutParams(size, size)
            star.setImageResource(
                if (i + 0.5 < rating) R.drawable.ic_star else R.drawable.ic_star_outline)
            star.imageTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.pm_tangerine)
            container.addView(star)
        }
    }

    private fun deliveryDay(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 2)
        return SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
    }

    companion object {
        const val ARG_ID = "productId"
        private const val STATE_OPTION = "option"

        fun show(fm: FragmentManager, product: Product) {
            val sheet = ProductDetailSheet()
            sheet.arguments = bundleOf(ARG_ID to product.id)
            sheet.show(fm, "product-" + product.id)
        }
    }
}
