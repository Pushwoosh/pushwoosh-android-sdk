package com.pushwoosh.pushmart.ui.promo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pushwoosh.pushmart.catalog.Product
import com.pushwoosh.pushmart.databinding.ItemSalePickBinding
import com.pushwoosh.pushmart.ui.common.ProductImages

/** The season's picks on Sale; any card leads to the shop, as on iOS. */
class SalePickAdapter(private val products: List<Product>, private val onClick: () -> Unit) :
    RecyclerView.Adapter<SalePickAdapter.Holder>() {

    class Holder(val binding: ItemSalePickBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemSalePickBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = products.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val product = products[position]
        ProductImages.bind(holder.binding.image, product)
        holder.binding.name.text = product.name
        holder.binding.price.text = product.priceText
        holder.binding.root.setOnClickListener { onClick() }
    }
}
