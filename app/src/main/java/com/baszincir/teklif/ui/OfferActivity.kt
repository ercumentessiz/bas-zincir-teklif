package com.baszincir.teklif.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.baszincir.teklif.data.Offer
import com.baszincir.teklif.data.OfferLine
import com.baszincir.teklif.data.Repository
import com.baszincir.teklif.databinding.ActivityOfferBinding
import com.baszincir.teklif.util.DateHelper
import com.baszincir.teklif.util.EditingOfferHolder
import com.baszincir.teklif.util.PdfHelper
import java.text.NumberFormat
import java.util.Locale

class OfferActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOfferBinding
    private val lines = mutableListOf<OfferLine>()
    private lateinit var adapter: OfferLineAdapter

    private var musteriAdi: String = ""
    private var musteriIl: String = ""
    private var teklifTarihiMillis: Long = 0L
    private var firestoreId: String = ""
    // Bir eski teklif düzenlemek için açıldığında, o teklifin YÜKLENDİĞİ
    // andaki müşteri adı burada tutulur. Kaydederken güncel müşteri adı
    // bundan farklıysa, teklif GÜNCELLENMEZ; yeni bir teklif olarak
    // kaydedilir ki eski müşterinin teklifi de Eski Teklifler'de kalsın.
    private var yuklenenTeklifMusteriAdi: String? = null
    // Mevcut oturumdaki kullanıcının e-postası; teklif ilk kaydedilirken
    // "oluşturan" olarak damgalanır. Sonraki düzenlemelerde (müşteri
    // değişmediği sürece) bu değer korunur.
    private var mevcutOlusturanEmail: String = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""

    private val tlFormat = NumberFormat.getNumberInstance(Locale("tr", "TR")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    private val musteriPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            musteriAdi = data.getStringExtra(CustomerPickerActivity.EXTRA_AD) ?: ""
            musteriIl = data.getStringExtra(CustomerPickerActivity.EXTRA_IL) ?: ""
            binding.tvMusteri.text = if (musteriIl.isNotBlank()) "$musteriAdi ($musteriIl)" else musteriAdi
        }
    }

    private val urunPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val ad = data.getStringExtra(ProductPickerActivity.EXTRA_AD) ?: return@registerForActivityResult
            val birim = data.getStringExtra(ProductPickerActivity.EXTRA_BIRIM) ?: "ADET"
            val fiyat = data.getDoubleExtra(ProductPickerActivity.EXTRA_FIYAT, 0.0)
            val ekBilgi = data.getStringExtra(ProductPickerActivity.EXTRA_EK_BILGI) ?: ""
            // Katalogdan seçilen ürün için miktar alanı, birim ne olursa
            // olsun boş başlasın; kullanıcı her zaman elle girsin.
            val adet = data.getDoubleExtra(ProductPickerActivity.EXTRA_ADET, 0.0)
            lines.add(
                OfferLine(
                    siraNo = lines.size + 1,
                    urunAdi = ad,
                    birim = birim,
                    adet = adet,
                    birimFiyat = fiyat,
                    ekBilgi = ekBilgi
                )
            )
            refreshList()
        }
    }

    private val storagePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            olusturVeKaydetPdf()
        } else {
            Toast.makeText(this, "PDF kaydetmek için depolama izni gerekli", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOfferBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = OfferLineAdapter(
            lines = lines,
            onChanged = { hesaplaVeGoster() },
            onRemove = { pos ->
                lines.removeAt(pos)
                for (i in lines.indices) lines[i].siraNo = i + 1
                refreshList()
            }
        )
        binding.rvSatirlar.layoutManager = LinearLayoutManager(this)
        binding.rvSatirlar.adapter = adapter

        val duzenlenecekTeklif = EditingOfferHolder.offer
        EditingOfferHolder.offer = null

        if (duzenlenecekTeklif != null) {
            firestoreId = duzenlenecekTeklif.firestoreId
            musteriAdi = duzenlenecekTeklif.musteriAdi
            musteriIl = duzenlenecekTeklif.musteriIl
            yuklenenTeklifMusteriAdi = duzenlenecekTeklif.musteriAdi
            mevcutOlusturanEmail = duzenlenecekTeklif.olusturanEmail.ifBlank { mevcutOlusturanEmail }
            teklifTarihiMillis = duzenlenecekTeklif.tarihMillis
            lines.clear()
            lines.addAll(duzenlenecekTeklif.lines.map { it.copy() })
            binding.etIskonto.setText(
                if (duzenlenecekTeklif.iskontoYuzde == duzenlenecekTeklif.iskontoYuzde.toLong().toDouble())
                    duzenlenecekTeklif.iskontoYuzde.toLong().toString()
                else duzenlenecekTeklif.iskontoYuzde.toString()
            )
            if (duzenlenecekTeklif.kdvOrani == 0.0) {
                binding.rbKdv0.isChecked = true
            } else {
                binding.rbKdv20.isChecked = true
            }
            binding.etTeslimSuresi.setText(duzenlenecekTeklif.teslimSuresi)
            when (duzenlenecekTeklif.odemeTipi) {
                "Peşin" -> binding.rbPesin.isChecked = true
                "Vadeli" -> binding.rbVadeli.isChecked = true
            }
            binding.etOdemeDetay.setText(duzenlenecekTeklif.odemeDetay)
            binding.tvMusteri.text = if (musteriIl.isNotBlank()) "$musteriAdi ($musteriIl)" else musteriAdi
        } else {
            teklifTarihiMillis = DateHelper.bugunMillis()
        }

        binding.tvTarih.text = "Tarih: ${DateHelper.formatTarih(teklifTarihiMillis)}"

        binding.btnGeri.setOnClickListener { finish() }

        binding.btnMusteriSec.setOnClickListener {
            musteriPickerLauncher.launch(android.content.Intent(this, CustomerPickerActivity::class.java))
        }

        binding.btnUrunEkle.setOnClickListener {
            urunPickerLauncher.launch(android.content.Intent(this, ProductPickerActivity::class.java))
        }

        binding.etIskonto.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { hesaplaVeGoster() }
        })

        binding.rgKdv.setOnCheckedChangeListener { _, _ -> hesaplaVeGoster() }

        binding.btnKaydet.setOnClickListener { teklifiKaydet(pdfOlustur = false) }
        binding.btnPdf.setOnClickListener { pdfIcinIzinKontrolEt() }

        refreshList()
    }

    private fun refreshList() {
        adapter.notifyDataSetChanged()
        binding.tvBosSatir.visibility = if (lines.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        binding.rvSatirlar.visibility = if (lines.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        // RecyclerView'ın kaydırmalı üst kapsayıcı (NestedScrollView) içinde
        // yeni eklenen satırlar dahil tam yüksekliğini doğru hesaplaması için
        // düzeni bir sonraki çizim döngüsünde zorla yeniden ölçtür.
        binding.rvSatirlar.post {
            binding.rvSatirlar.requestLayout()
        }
        hesaplaVeGoster()
    }

    private fun iskontoYuzdeAl(): Double =
        binding.etIskonto.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0

    private fun kdvOraniAl(): Double =
        if (binding.rbKdv0.isChecked) 0.0 else 20.0

    private fun odemeTipiAl(): String = when {
        binding.rbPesin.isChecked -> "Peşin"
        binding.rbVadeli.isChecked -> "Vadeli"
        else -> ""
    }

    private fun hesaplaVeGoster() {
        val araToplam = lines.sumOf { it.satirToplam }
        val iskontoyaTabiToplam = lines.filter { it.iskontoUygulanir }.sumOf { it.satirToplam }
        val iskontoYuzde = iskontoYuzdeAl()
        val iskontoTutari = iskontoyaTabiToplam * (iskontoYuzde / 100.0)
        val sonrasiToplam = araToplam - iskontoTutari
        val kdvOrani = kdvOraniAl()
        val kdvTutari = sonrasiToplam * (kdvOrani / 100.0)
        val genelToplam = sonrasiToplam + kdvTutari

        binding.tvAraToplam.text = "Ara Toplam: ${tlFormat.format(araToplam)} TL"
        binding.tvIskontoTutari.text = "İskonto (%${tlFormat.format(iskontoYuzde)}): - ${tlFormat.format(iskontoTutari)} TL"
        binding.tvKdv.text = "KDV (%${tlFormat.format(kdvOrani)}): ${tlFormat.format(kdvTutari)} TL"
        binding.tvGenelToplam.text = "GENEL TOPLAM: ${tlFormat.format(genelToplam)} TL"

        val gecerlilik = DateHelper.gecerlilikTarihiHesapla(teklifTarihiMillis)
        binding.tvGecerlilik.text = "Teklif Geçerlilik Tarihi: ${DateHelper.formatTarihSaat(gecerlilik)}"
    }

    private fun teklifOlustur(): Offer? {
        if (musteriAdi.isBlank()) {
            Toast.makeText(this, "Lütfen bir müşteri seçin", Toast.LENGTH_SHORT).show()
            return null
        }
        if (lines.isEmpty()) {
            Toast.makeText(this, "Lütfen en az bir ürün ekleyin", Toast.LENGTH_SHORT).show()
            return null
        }
        val araToplam = lines.sumOf { it.satirToplam }
        val iskontoyaTabiToplam = lines.filter { it.iskontoUygulanir }.sumOf { it.satirToplam }
        val iskontoYuzde = iskontoYuzdeAl()
        val iskontoTutari = iskontoyaTabiToplam * (iskontoYuzde / 100.0)
        val sonrasiToplam = araToplam - iskontoTutari
        val kdvOrani = kdvOraniAl()
        val kdvTutari = sonrasiToplam * (kdvOrani / 100.0)
        val genelToplam = sonrasiToplam + kdvTutari
        val gecerlilik = DateHelper.gecerlilikTarihiHesapla(teklifTarihiMillis)

        return Offer(
            firestoreId = firestoreId,
            musteriAdi = musteriAdi,
            musteriIl = musteriIl,
            tarihMillis = teklifTarihiMillis,
            gecerlilikMillis = gecerlilik,
            iskontoYuzde = iskontoYuzde,
            lines = lines.map { it.copy() },
            araToplam = araToplam,
            iskontoTutari = iskontoTutari,
            kdvOrani = kdvOrani,
            kdvTutari = kdvTutari,
            genelToplam = genelToplam,
            olusturanEmail = mevcutOlusturanEmail,
            teslimSuresi = binding.etTeslimSuresi.text.toString().trim(),
            odemeTipi = odemeTipiAl(),
            odemeDetay = binding.etOdemeDetay.text.toString().trim()
        )
    }

    private fun teklifiKaydet(pdfOlustur: Boolean) {
        val offer = teklifOlustur() ?: return

        // Mevcut bir teklif düzenleniyor ama müşteri adı değiştirilmişse,
        // üzerine yazmak yerine yeni bir teklif olarak kaydet; eski
        // müşterinin teklifi Eski Teklifler'de olduğu gibi kalsın.
        val musteriDegisti = yuklenenTeklifMusteriAdi != null && yuklenenTeklifMusteriAdi != musteriAdi
        val yeniKayitOlarakKaydet = firestoreId.isBlank() || musteriDegisti

        if (yeniKayitOlarakKaydet) {
            val guncelKullaniciEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""
            val kaydedilecekOffer = if (musteriDegisti)
                offer.copy(firestoreId = "", olusturanEmail = guncelKullaniciEmail)
            else
                offer
            Repository.saveNewOffer(
                offer = kaydedilecekOffer,
                onResult = { yeniId ->
                    firestoreId = yeniId
                    yuklenenTeklifMusteriAdi = musteriAdi
                    runOnUiThread {
                        if (!pdfOlustur) {
                            Toast.makeText(this, "Teklif kaydedildi", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            pdfUret(kaydedilecekOffer.copy(firestoreId = yeniId))
                        }
                    }
                },
                onError = { e ->
                    runOnUiThread { Toast.makeText(this, "Kaydedilemedi: ${e.message}", Toast.LENGTH_LONG).show() }
                }
            )
        } else {
            Repository.updateOffer(
                offer = offer,
                onResult = {
                    runOnUiThread {
                        if (!pdfOlustur) {
                            Toast.makeText(this, "Teklif güncellendi", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            pdfUret(offer)
                        }
                    }
                },
                onError = { e ->
                    runOnUiThread { Toast.makeText(this, "Güncellenemedi: ${e.message}", Toast.LENGTH_LONG).show() }
                }
            )
        }
    }

    private fun pdfIcinIzinKontrolEt() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val izin = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (izin != PackageManager.PERMISSION_GRANTED) {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                return
            }
        }
        olusturVeKaydetPdf()
    }

    private fun olusturVeKaydetPdf() {
        teklifiKaydet(pdfOlustur = true)
    }

    private fun pdfUret(offer: Offer) {
        val uri = PdfHelper.createAndSaveOfferPdf(this, offer)
        if (uri != null) {
            val dosyaAdi = "${PdfHelper.sanitizeFileName(offer.musteriAdi)} – Teklif.pdf"
            val dialog = AlertDialog.Builder(this)
                .setTitle("PDF Oluşturuldu")
                .setMessage("\"$dosyaAdi\" dosyası İndirilenler klasörüne kaydedildi.")
                .setPositiveButton("Paylaş") { _, _ -> pdfPaylas(uri) }
                .setNegativeButton("Tamam", null)
                .create()
            // Kullanıcı "Paylaş" veya "Tamam" ile pencereyi kapattığında
            // (ya da dışarı dokunarak kapatsa bile) otomatik olarak
            // bir önceki ekrana (ana sayfa / eski teklifler) dön.
            dialog.setOnDismissListener { finish() }
            dialog.show()
        } else {
            Toast.makeText(this, "PDF oluşturulamadı", Toast.LENGTH_LONG).show()
        }
    }

    private fun pdfPaylas(uri: android.net.Uri) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(android.content.Intent.createChooser(intent, "Teklifi Paylaş"))
    }
}
