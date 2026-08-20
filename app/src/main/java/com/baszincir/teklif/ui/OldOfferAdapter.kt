package com.baszincir.teklif.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.baszincir.teklif.data.Offer
import com.baszincir.teklif.databinding.ItemOldOfferBinding
import com.baszincir.teklif.util.DateHelper
import java.text.NumberFormat
import java.util.Locale

class OldOfferAdapter(
    private var items: List<Offer>,
    private val onClick: (Offer) -> Unit,
    private val onDelete: (Offer) -> Unit
) : RecyclerView.Adapter<OldOfferAdapter.VH>() {

    private val tlFormat = NumberFormat.getNumberInstance(Locale("tr", "TR")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    inner class VH(val binding: ItemOldOfferBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemOldOfferBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val o = items[position]
        holder.binding.tvMusteri.text = o.musteriAdi
        holder.binding.tvTarih.text = "${DateHelper.formatTarih(o.tarihMillis)} • ${o.lines.size} kalem"
        holder.binding.tvToplam.text = "Genel Toplam: ${tlFormat.format(o.genelToplam)} TL"
        holder.binding.tvOlusturan.text = if (o.olusturanEmail.isNotBlank()) {
            "Hazırlayan: ${o.olusturanEmail}"
        } else {
            "Hazırlayan: -"
        }
        holder.itemView.setOnClickListener { onClick(o) }
        holder.binding.btnSil.setOnClickListener { onDelete(o) }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<Offer>) {
        items = newItems
        notifyDataSetChanged()
    }
}
