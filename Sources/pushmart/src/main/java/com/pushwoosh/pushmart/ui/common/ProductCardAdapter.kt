package com.pushwoosh.pushmart.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pushwoosh.pushmart.catalog.Product
import com.pushwoosh.pushmart.databinding.ItemProductCardBinding
import com.pushwoosh.pushmart.shop.WishlistStore

class ProductCardAdapter(
    private val wishlist: WishlistStore,
    private val onClick: (Product) -> Unit,
) : RecyclerView.Adapter<ProductCardAdapter.Holder>() {
    private var products: List<Product> = emptyList()

    class Holder(val binding: ItemProductCardBinding) : RecyclerView.ViewHolder(binding.root)

    fun submit(list: List<Product>) {
        products = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemProductCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = products.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val product = products[position]
        val binding = holder.binding
        ProductImages.bind(binding.image, product)
        binding.badge.text = product.badge?.uppercase()
        binding.badge.visibility = if (product.badge == null) View.GONE else View.VISIBLE
        binding.brand.text = product.brand
        binding.name.text = product.name
        binding.price.text = product.priceText
        Hearts.bind(binding.heart, wishlist.contains(product.id))
        binding.root.setOnClickListener { onClick(product) }
        binding.heart.setOnClickListener {
            wishlist.toggle(product)
            Hearts.bind(binding.heart, wishlist.contains(product.id))
        }
    }
}
