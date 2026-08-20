package com.baszincir.teklif.ui

import com.baszincir.teklif.util.trLower
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.baszincir.teklif.data.Customer
import com.baszincir.teklif.data.Repository
import com.baszincir.teklif.databinding.ActivityCustomerManageBinding
import com.baszincir.teklif.databinding.DialogMusteriDuzenleBinding

class CustomerManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerManageBinding
    private var tumMusteriler: List<Customer> = emptyList()
    private lateinit var adapter: CustomerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnKapat.setOnClickListener { finish() }
        binding.fabEkle.setOnClickListener { musteriDuzenleDialogGoster(null) }

        adapter = CustomerAdapter(emptyList()) { customer -> musteriDuzenleDialogGoster(customer) }
        binding.rvListe.layoutManager = LinearLayoutManager(this)
        binding.rvListe.adapter = adapter

        binding.etArama.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = trLower(s.toString().trim())
                val filtered = if (q.isEmpty()) tumMusteriler else tumMusteriler.filter { it.aramaMetni.contains(q) }
                adapter.updateList(filtered)
            }
        })

        yukle()
    }

    private fun yukle() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        Repository.getCustomers(
            onResult = { list ->
                runOnUiThread {
                    tumMusteriler = list
                    adapter.updateList(list)
                    binding.progressBar.visibility = android.view.View.GONE
                }
            },
            onError = { e ->
                runOnUiThread {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Müşteriler yüklenemedi: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun musteriDuzenleDialogGoster(customer: Customer?) {
        val dialogBinding = DialogMusteriDuzenleBinding.inflate(layoutInflater)
        val duzenlemeModu = customer != null

        if (customer != null) {
            dialogBinding.etAd.setText(customer.ad)
            dialogBinding.etIl.setText(customer.il)
        }

        val builder = AlertDialog.Builder(this)
            .setTitle(if (duzenlemeModu) "Müşteriyi Düzenle" else "Yeni Müşteri Ekle")
            .setView(dialogBinding.root)
            .setPositiveButton("Kaydet") { _, _ ->
                val ad = dialogBinding.etAd.text.toString().trim()
                val il = dialogBinding.etIl.text.toString().trim()

                if (ad.isBlank()) {
                    Toast.makeText(this, "Müşteri adı boş olamaz", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val kaydedilecek = Customer(
                    id = customer?.id ?: "",
                    ad = ad,
                    il = il
                )
                binding.progressBar.visibility = android.view.View.VISIBLE
                Repository.saveCustomer(
                    customer = kaydedilecek,
                    onResult = {
                        runOnUiThread {
                            Toast.makeText(this, "Müşteri kaydedildi", Toast.LENGTH_SHORT).show()
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
            builder.setNeutralButton("Sil") { _, _ -> silOnayDialogu(customer!!) }
        }

        builder.show()
    }

    private fun silOnayDialogu(customer: Customer) {
        AlertDialog.Builder(this)
            .setTitle("Müşteriyi Sil")
            .setMessage("\"${customer.ad}\" kalıcı listeden silinsin mi?")
            .setPositiveButton("Sil") { _, _ ->
                binding.progressBar.visibility = android.view.View.VISIBLE
                Repository.deleteCustomer(
                    id = customer.id,
                    onResult = {
                        runOnUiThread {
                            Toast.makeText(this, "Müşteri silindi", Toast.LENGTH_SHORT).show()
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
