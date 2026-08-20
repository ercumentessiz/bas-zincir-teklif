package com.baszincir.teklif.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.baszincir.teklif.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Zaten giriş yapılmışsa doğrudan ana ekrana geç
        if (auth.currentUser != null) {
            anaEkraneGit()
            return
        }

        binding.btnGiris.setOnClickListener { girisYap() }
    }

    private fun girisYap() {
        val email = binding.etEmail.text.toString().trim()
        val sifre = binding.etSifre.text.toString()

        if (email.isBlank() || sifre.isBlank()) {
            binding.tvHata.text = "E-posta ve şifre boş olamaz"
            return
        }

        binding.tvHata.text = ""
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnGiris.isEnabled = false

        auth.signInWithEmailAndPassword(email, sifre)
            .addOnSuccessListener {
                anaEkraneGit()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnGiris.isEnabled = true
                binding.tvHata.text = "Giriş başarısız: e-posta veya şifre hatalı"
            }
    }

    private fun anaEkraneGit() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
