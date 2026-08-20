package com.baszincir.teklif.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.baszincir.teklif.data.Repository
import com.baszincir.teklif.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Oturum yoksa (örn. şifre sıfırlandıysa) giriş ekranına geri dön
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnYeniTeklif.setOnClickListener {
            startActivity(Intent(this, OfferActivity::class.java))
        }
        binding.btnEskiTeklifler.setOnClickListener {
            startActivity(Intent(this, OldOffersActivity::class.java))
        }
        binding.btnYonetim.setOnClickListener {
            startActivity(Intent(this, ManagementActivity::class.java))
        }
        binding.btnCikisYap.setOnClickListener { cikisOnayDialogu() }

        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.tvDurum.text = "Veriler hazırlanıyor..."
        binding.btnYeniTeklif.isEnabled = false

        Repository.ensureInitialSync(
            context = applicationContext,
            onDone = {
                runOnUiThread {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.tvDurum.text = ""
                    binding.btnYeniTeklif.isEnabled = true
                }
            },
            onError = { e ->
                runOnUiThread {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.tvDurum.text = "Bağlantı hatası: ${e.message}"
                    binding.btnYeniTeklif.isEnabled = true
                    Toast.makeText(
                        this,
                        "İnternet bağlantınızı kontrol edin. Firebase'e ilk senkronizasyon yapılamadı.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    private fun cikisOnayDialogu() {
        AlertDialog.Builder(this)
            .setTitle("Çıkış Yap")
            .setMessage("Oturumu kapatmak istediğinize emin misiniz?")
            .setPositiveButton("Çıkış Yap") { _, _ ->
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }
}
