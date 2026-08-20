package com.baszincir.teklif.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.baszincir.teklif.databinding.ActivityManagementBinding

class ManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManagementBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGeri.setOnClickListener { finish() }
        binding.btnUrunler.setOnClickListener {
            startActivity(Intent(this, ProductManageActivity::class.java))
        }
        binding.btnMusteriler.setOnClickListener {
            startActivity(Intent(this, CustomerManageActivity::class.java))
        }
    }
}
