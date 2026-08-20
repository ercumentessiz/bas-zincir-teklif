package com.baszincir.teklif.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.baszincir.teklif.data.Offer
import com.baszincir.teklif.data.Repository
import com.baszincir.teklif.databinding.ActivityOldOffersBinding
import com.baszincir.teklif.util.EditingOfferHolder
import com.baszincir.teklif.util.trLower

class OldOffersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOldOffersBinding
    private var tumTeklifler: List<Offer> = emptyList()
    private lateinit var adapter: OldOfferAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOldOffersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGeri.setOnClickListener { finish() }
        binding.rvTeklifler.layoutManager = LinearLayoutManager(this)

        adapter = OldOfferAdapter(
            items = emptyList(),
            onClick = { offer ->
                EditingOfferHolder.offer = offer
                startActivity(Intent(this, OfferActivity::class.java))
            },
            onDelete = { offer -> silOnayDialogu(offer) }
        )
        binding.rvTeklifler.adapter = adapter

        binding.etArama.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = trLower(s.toString().trim())
                val filtered = if (q.isEmpty()) tumTeklifler else tumTeklifler.filter { trLower(it.musteriAdi).contains(q) }
                adapter.updateList(filtered)
                binding.tvBos.visibility = if (filtered.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        })

        yukle()
    }

    override fun onResume() {
        super.onResume()
        yukle()
    }

    private fun yukle() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.tvBos.visibility = android.view.View.GONE
        Repository.getOffers(
            onResult = { list ->
                runOnUiThread {
                    binding.progressBar.visibility = android.view.View.GONE
                    tumTeklifler = list
                    val q = trLower(binding.etArama.text.toString().trim())
                    val filtered = if (q.isEmpty()) list else list.filter { trLower(it.musteriAdi).contains(q) }
                    binding.tvBos.visibility = if (filtered.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                    adapter.updateList(filtered)
                }
            },
            onError = { e ->
                runOnUiThread {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Teklifler yüklenemedi: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun silOnayDialogu(offer: Offer) {
        AlertDialog.Builder(this)
            .setTitle("Teklifi Sil")
            .setMessage("\"${offer.musteriAdi}\" adına hazırlanan bu teklif kalıcı olarak silinsin mi?")
            .setPositiveButton("Sil") { _, _ ->
                Repository.deleteOffer(
                    firestoreId = offer.firestoreId,
                    onResult = {
                        runOnUiThread {
                            Toast.makeText(this, "Teklif silindi", Toast.LENGTH_SHORT).show()
                            yukle()
                        }
                    },
                    onError = { e ->
                        runOnUiThread { Toast.makeText(this, "Silinemedi: ${e.message}", Toast.LENGTH_LONG).show() }
                    }
                )
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }
}
