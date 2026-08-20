package com.baszincir.teklif.ui

import com.baszincir.teklif.util.trLower
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.baszincir.teklif.data.Product
import com.baszincir.teklif.data.Repository
import com.baszincir.teklif.databinding.ActivityProductManageBinding
import com.baszincir.teklif.databinding.DialogUrunDuzenleBinding

class ProductManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductManageBinding
    private var tumUrunler: List<Product> = emptyList()
    private lateinit var adapter: GroupedProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnKapat.setOnClickListener { finish() }
        binding.fabEkle.setOnClickListener { urunDuzenleDialogGoster(null) }

        adapter = GroupedProductAdapter { product -> urunDuzenleDialogGoster(product) }
        binding.rvListe.layoutManager = LinearLayoutManager(this)
        binding.rvListe.adapter = adapter

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

        yukle()
    }

    private fun yukle() {
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
    }

    private fun urunDuzenleDialogGoster(product: Product?) {
        val dialogBinding = DialogUrunDuzenleBinding.inflate(layoutInflater)
        val duzenlemeModu = product != null

        if (product != null) {
            dialogBinding.etAd.setText(product.ad)
            dialogBinding.etKategori.setText(product.kategori)
            dialogBinding.etBirim.setText(product.birim)
            dialogBinding.etFiyat.setText(
                if (product.fiyat == product.fiyat.toLong().toDouble()) product.fiyat.toLong().toString() else product.fiyat.toString()
            )
            dialogBinding.etEkBilgi.setText(product.ekBilgi)
        } else {
            dialogBinding.etBirim.setText("ADET")
        }

        val builder = AlertDialog.Builder(this)
            .setTitle(if (duzenlemeModu) "Ürünü Düzenle" else "Yeni Ürün Ekle")
            .setView(dialogBinding.root)
            .setPositiveButton("Kaydet") { _, _ ->
                val ad = dialogBinding.etAd.text.toString().trim()
                val kategori = dialogBinding.etKategori.text.toString().trim().ifBlank { "Diğer" }
                val birim = dialogBinding.etBirim.text.toString().trim().ifBlank { "ADET" }
                val fiyat = dialogBinding.etFiyat.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
                val ekBilgi = dialogBinding.etEkBilgi.text.toString().trim()

                if (ad.isBlank()) {
                    Toast.makeText(this, "Ürün adı boş olamaz", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val kaydedilecek = Product(
                    id = product?.id ?: "",
                    ad = ad,
                    kategori = kategori,
                    birim = birim,
                    fiyat = fiyat,
                    ekBilgi = ekBilgi
                )
                binding.progressBar.visibility = android.view.View.VISIBLE
                Repository.saveProduct(
                    product = kaydedilecek,
                    onResult = {
                        runOnUiThread {
                            Toast.makeText(this, "Ürün kaydedildi", Toast.LENGTH_SHORT).show()
                            yukle()
                        }
                    },
                    onError = { e ->
                        runOnUiThread {
                            binding.progressBar.visibility = android.view.View.GONE
                            Toast.makeText(this, "Kaydedilemedi: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
            .setNegativeButton("İptal", null)

        if (duzenlemeModu) {
            builder.setNeutralButton("Sil") { _, _ -> silOnayDialogu(product!!) }
        }

        builder.show()
    }

    private fun silOnayDialogu(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("Ürünü Sil")
            .setMessage("\"${product.ad}\" kalıcı listeden silinsin mi?")
            .setPositiveButton("Sil") { _, _ ->
                binding.progressBar.visibility = android.view.View.VISIBLE
                Repository.deleteProduct(
                    id = product.id,
                    onResult = {
                        runOnUiThread {
                            Toast.makeText(this, "Ürün silindi", Toast.LENGTH_SHORT).show()
                            yukle()
                        }
                    },
                    onError = { e ->
                        runOnUiThread {
                            binding.progressBar.visibility = android.view.View.GONE
                            Toast.makeText(this, "Silinemedi: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }
}
