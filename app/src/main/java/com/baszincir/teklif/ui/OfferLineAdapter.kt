package com.baszincir.teklif.ui

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.baszincir.teklif.data.OfferLine
import com.baszincir.teklif.databinding.ItemOfferLineBinding
import com.baszincir.teklif.util.birimKisaltma
import java.text.NumberFormat
import java.util.Locale

class OfferLineAdapter(
    private val lines: MutableList<OfferLine>,
    private val onChanged: () -> Unit,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<OfferLineAdapter.LineViewHolder>() {

    private val tlFormat = NumberFormat.getNumberInstance(Locale("tr", "TR")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    inner class LineViewHolder(val binding: ItemOfferLineBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineViewHolder {
        val binding = ItemOfferLineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LineViewHolder, position: Int) {
        val line = lines[position]
        val b = holder.binding

        b.tvSiraNo.text = (position + 1).toString()
        b.tvUrunAdi.text = line.urunAdi
        b.tvEkBilgi.text = line.ekBilgi
        b.tvEkBilgi.visibility = if (line.ekBilgi.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
        b.tvBirim.text = birimKisaltma(line.birim)
        b.tilAdet.hint = when (line.birim) {
            "KG" -> "Kilogram"
            "METRE" -> "Metre"
            else -> "Adet"
        }

        // Watcher'ları geçici olarak kaldırıp tekrar eklemek için tag kullan
        b.etAdet.removeTextChangedListener(b.etAdet.tag as? TextWatcher)
        b.etFiyat.removeTextChangedListener(b.etFiyat.tag as? TextWatcher)

        val adetStr = if (line.adet == 0.0) "" else if (line.adet == line.adet.toLong().toDouble()) line.adet.toLong().toString() else line.adet.toString()
        val fiyatStr = if (line.birimFiyat == line.birimFiyat.toLong().toDouble()) line.birimFiyat.toLong().toString() else line.birimFiyat.toString()

        if (b.etAdet.text.toString() != adetStr) b.etAdet.setText(adetStr)
        if (b.etFiyat.text.toString() != fiyatStr) b.etFiyat.setText(fiyatStr)

        updateSatirToplam(b, line)

        val adetWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val v = s.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
                line.adet = v
                updateSatirToplam(b, line)
                onChanged()
            }
        }
        b.etAdet.addTextChangedListener(adetWatcher)
        b.etAdet.tag = adetWatcher

        val fiyatWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val v = s.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
                line.birimFiyat = v
                updateSatirToplam(b, line)
                onChanged()
            }
        }
        b.etFiyat.addTextChangedListener(fiyatWatcher)
        b.etFiyat.tag = fiyatWatcher

        b.btnSil.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onRemove(pos)
        }

        b.cbIskonto.setOnCheckedChangeListener(null)
        b.cbIskonto.isChecked = line.iskontoUygulanir
        b.cbIskonto.setOnCheckedChangeListener { _, isChecked ->
            line.iskontoUygulanir = isChecked
            onChanged()
        }
    }

    private fun updateSatirToplam(b: ItemOfferLineBinding, line: OfferLine) {
        b.tvSatirToplam.text = "Tutar: ${tlFormat.format(line.satirToplam)} TL"
    }

    override fun getItemCount(): Int = lines.size
}
