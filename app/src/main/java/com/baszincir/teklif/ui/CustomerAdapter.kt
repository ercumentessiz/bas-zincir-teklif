package com.baszincir.teklif.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.baszincir.teklif.data.Customer
import com.baszincir.teklif.databinding.ItemCustomerBinding

class CustomerAdapter(
    private var items: List<Customer>,
    private val onClick: (Customer) -> Unit
) : RecyclerView.Adapter<CustomerAdapter.VH>() {

    inner class VH(val binding: ItemCustomerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.binding.tvAd.text = c.ad
        holder.binding.tvIl.text = c.il
        holder.itemView.setOnClickListener { onClick(c) }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<Customer>) {
        items = newItems
        notifyDataSetChanged()
    }
}
