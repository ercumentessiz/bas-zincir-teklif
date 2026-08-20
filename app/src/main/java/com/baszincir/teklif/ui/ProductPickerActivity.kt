package com.baszincir.teklif.ui

import com.baszincir.teklif.util.trLower
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.baszincir.teklif.data.Product
import com.baszincir.teklif.data.Repository
import com.baszincir.teklif.databinding.ActivityPickerBinding
import com.baszincir.teklif.databinding.DialogManuelUrunBinding

class ProductPickerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_AD = "extra_ad"
        const val EXTRA_BIRIM = "extra_birim"
        const val EXTRA_FIYAT = "extra_fiyat"
        const val EXTRA_EK_BILGI = "extra_ek_bilgi"
        const val EXTRA_ADET = "extra_adet"
    }

    private lateinit var binding: ActivityPickerBinding
    private var tumUrunler: List<Product> = emptyList()
    private lateinit var adapter: GroupedProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvBaslik.text = "Ürün Seç"
        binding.btnManuel.visibility = android.view.View.VISIBLE
        binding.btnKapat.setOnClickListener { finish() }
        binding.btnManuel.setOnClickListener { manuelUrunDialogGoster() }

        adapter = GroupedProductAdapter { product -> urunSecildi(product) }
        binding.rvListe.layoutManager = LinearLayoutManager(this)
        binding.rvListe.adapter = adapter

        binding.progressBar.visibility = android.view.View.VISIBLE
        Repository.getProducts(
            onResult = { list ->
                runOnUiThread {
                    tumUrunler = list
                    adapter.tumListeyiGoster(list)
                    binding.progressBar.visibility = android.view.View.GONE
                }
            },
            onError = { e ->
                runOnUiThread {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Ürünler yüklenemedi: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        )

        binding.etArama.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = trLower(s.toString().trim())
                if (q.isEmpty()) {
                    adapter.tumListeyiGoster(tumUrunler)
                } else {
                    val filtered = tumUrunler.filter { it.aramaMetni.contains(q) }
                    adapter.aramaSonucuGoster(filtered)
                }
            }
        })
    }

    private fun urunSecildi(product: Product) {
        val data = Intent().apply {
            putExtra(EXTRA_AD, product.ad)
            putExtra(EXTRA_BIRIM, product.birim)
            putExtra(EXTRA_FIYAT, product.fiyat)
            putExtra(EXTRA_EK_BILGI, product.ekBilgi)
        }
        setResult(RESULT_OK, data)
        finish()
    }

    private fun manuelUrunDialogGoster() {
        val dialogBinding = DialogManuelUrunBinding.inflate(layoutInflater)
        AlertDialog.Builder(this)
            .setTitle("Manuel Ürün Ekle")
            .setView(dialogBinding.root)
            .setPositiveButton("Ekle") { _, _ ->
                val ad = dialogBinding.etAd.text.toString().trim()
                val birim = dialogBinding.etBirim.text.toString().trim().ifBlank { "ADET" }
                val fiyat = dialogBinding.etFiyat.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
                val adet = dialogBinding.etAdet.text.toString().replace(",", ".").toDoubleOrNull() ?: 1.0
                if (ad.isBlank()) {
                    Toast.makeText(this, "Ürün adı boş olamaz", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Kalıcı ürün listesine de kaydet (Firestore "urunler" koleksiyonu)
                val kalici = Product(ad = ad, kategori = "Manuel Eklenen Ürün", birim = birim, fiyat = fiyat, ekBilgi = "")
                Repository.saveProduct(
                    product = kalici,
                    onResult = { },
                    onError = { }
                )

                val data = Intent().apply {
                    putExtra(EXTRA_AD, ad)
                    putExtra(EXTRA_BIRIM, birim)
                    putExtra(EXTRA_FIYAT, fiyat)
                    putExtra(EXTRA_EK_BILGI, "")
                    putExtra(EXTRA_ADET, adet)
                }
                setResult(RESULT_OK, data)
                finish()
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}
