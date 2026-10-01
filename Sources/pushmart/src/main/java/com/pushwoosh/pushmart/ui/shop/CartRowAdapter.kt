package com.pushwoosh.pushmart.ui.shop

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pushwoosh.pushmart.databinding.ItemCartRowBinding
import com.pushwoosh.pushmart.shop.CartLine
import com.pushwoosh.pushmart.shop.CartStore
import com.pushwoosh.pushmart.ui.common.ProductImages

class CartRowAdapter(private val cart: CartStore) : RecyclerView.Adapter<CartRowAdapter.Holder>() {
    private var lines: List<CartLine> = emptyList()

    class Holder(val binding: ItemCartRowBinding) : RecyclerView.ViewHolder(binding.root)

    fun submit(list: List<CartLine>) {
        lines = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemCartRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = lines.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val line = lines[position]
        val binding = holder.binding
        ProductImages.bind(binding.image, line.product)
        binding.name.text = line.product.name
        binding.price.text = line.product.priceText
        binding.quantity.text = line.quantity.toString()
        binding.minus.setOnClickListener { cart.setQuantity(line.product.id, line.quantity - 1) }
        binding.plus.setOnClickListener { cart.setQuantity(line.product.id, line.quantity + 1) }
    }
}
