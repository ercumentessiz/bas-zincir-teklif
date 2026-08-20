package com.baszincir.teklif.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.baszincir.teklif.data.Product
import com.baszincir.teklif.databinding.ItemProductBinding
import com.baszincir.teklif.databinding.ItemProductHeaderBinding
import com.baszincir.teklif.util.ProductSortUtil
import com.baszincir.teklif.util.birimKisaltma
import java.text.NumberFormat
import java.util.Locale

sealed class ProductListItem {
    data class Header(val kategori: String) : ProductListItem()
    data class Row(val product: Product) : ProductListItem()
}

class GroupedProductAdapter(
    private val onClick: (Product) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<ProductListItem> = emptyList()

    private val tlFormat = NumberFormat.getNumberInstance(Locale("tr", "TR")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ROW = 1
    }

    inner class HeaderVH(val binding: ItemProductHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    inner class RowVH(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is ProductListItem.Header -> TYPE_HEADER
            is ProductListItem.Row -> TYPE_ROW
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            HeaderVH(ItemProductHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            RowVH(ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ProductListItem.Header -> {
                (holder as HeaderVH).binding.root.text = item.kategori
            }
            is ProductListItem.Row -> {
                val p = item.product
                val h = holder as RowVH
                h.binding.tvAd.text = p.ad
                val ekBilgiStr = if (p.ekBilgi.isNotBlank()) " • ${p.ekBilgi}" else ""
                h.binding.tvDetay.text = "${tlFormat.format(p.fiyat)} TL / ${birimKisaltma(p.birim)}$ekBilgiStr"
                h.binding.root.setOnClickListener { onClick(p) }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    /** Kategorilere ayrılmış, başlıklı tam liste (arama kutusu boşken). */
    fun tumListeyiGoster(urunler: List<Product>) {
        val sirali = ProductSortUtil.sirala(urunler)
        val yeniListe = mutableListOf<ProductListItem>()
        var sonKategori: String? = null
        for (p in sirali) {
            if (p.kategori != sonKategori) {
                yeniListe.add(ProductListItem.Header(p.kategori))
                sonKategori = p.kategori
            }
            yeniListe.add(ProductListItem.Row(p))
        }
        items = yeniListe
        notifyDataSetChanged()
    }

    /** Arama sonucu: başlıksız, düz filtrelenmiş liste. */
    fun aramaSonucuGoster(urunler: List<Product>) {
        items = ProductSortUtil.sirala(urunler).map { ProductListItem.Row(it) }
        notifyDataSetChanged()
    }
}
