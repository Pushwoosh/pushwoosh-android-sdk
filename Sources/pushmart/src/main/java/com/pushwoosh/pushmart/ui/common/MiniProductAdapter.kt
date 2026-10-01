package com.pushwoosh.pushmart.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pushwoosh.pushmart.catalog.Product
import com.pushwoosh.pushmart.databinding.ItemMiniProductBinding

class MiniProductAdapter(private val onClick: (Product) -> Unit) :
    RecyclerView.Adapter<MiniProductAdapter.Holder>() {
    private var products: List<Product> = emptyList()

    class Holder(val binding: ItemMiniProductBinding) : RecyclerView.ViewHolder(binding.root)

    fun submit(list: List<Product>) {
        products = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemMiniProductBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = products.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val product = products[position]
        ProductImages.bind(holder.binding.image, product)
        holder.binding.name.text = product.name
        holder.binding.price.text = product.priceText
        holder.binding.root.setOnClickListener { onClick(product) }
    }
}
