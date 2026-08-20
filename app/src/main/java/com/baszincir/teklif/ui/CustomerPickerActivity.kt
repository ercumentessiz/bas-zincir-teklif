package com.baszincir.teklif.ui

import com.baszincir.teklif.util.trLower
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.baszincir.teklif.data.Customer
import com.baszincir.teklif.data.Repository
import com.baszincir.teklif.databinding.ActivityPickerBinding

class CustomerPickerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_AD = "extra_ad"
        const val EXTRA_IL = "extra_il"
    }

    private lateinit var binding: ActivityPickerBinding
    private var tumMusteriler: List<Customer> = emptyList()
    private lateinit var adapter: CustomerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvBaslik.text = "Müşteri Seç"
        binding.btnKapat.setOnClickListener { finish() }

        adapter = CustomerAdapter(emptyList()) { customer -> musteriSecildi(customer) }
        binding.rvListe.layoutManager = LinearLayoutManager(this)
        binding.rvListe.adapter = adapter

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

        binding.etArama.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = trLower(s.toString().trim())
                val filtered = if (q.isEmpty()) tumMusteriler else tumMusteriler.filter { it.aramaMetni.contains(q) }
                adapter.updateList(filtered)
            }
        })
    }

    private fun musteriSecildi(customer: Customer) {
        val data = Intent().apply {
            putExtra(EXTRA_AD, customer.ad)
            putExtra(EXTRA_IL, customer.il)
        }
        setResult(RESULT_OK, data)
        finish()
    }
}
