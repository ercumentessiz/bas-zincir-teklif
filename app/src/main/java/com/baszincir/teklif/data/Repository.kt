package com.baszincir.teklif.data

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import org.json.JSONArray
import org.json.JSONObject

/**
 * Tüm Firebase Firestore işlemlerinin tek merkezi.
 * - Müşteri listesi ilk açılışta assets içindeki JSON dosyasından
 *   Firestore'a otomatik olarak yüklenir (koleksiyon boşsa).
 * - Ürün fiyat listesi, uygulama güncellendiğinde (CURRENT_PRODUCTS_DATA_VERSION
 *   artırıldığında) eski katalog silinip assets'teki güncel veriyle yeniden
 *   yüklenir; elle eklenen özel ürünler bundan etkilenmez.
 * - Sonrasında fiyat/müşteri güncellemeleri Firebase Console'dan veya
 *   uygulama içindeki Yönetim ekranından yapılabilir.
 */
object Repository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    const val PRODUCTS_COLLECTION = "urunler"
    const val CUSTOMERS_COLLECTION = "musteriler"
    const val OFFERS_COLLECTION = "teklifler"

    private const val PREFS = "bas_zincir_prefs"
    private const val KEY_CUSTOMERS_SYNCED = "customers_synced_v1"
    private const val KEY_PRODUCTS_DATA_VERSION = "products_data_version"

    // Ürün fiyat listesi (assets/products.json) her değiştiğinde bu sayı
    // artırılmalı. Uygulama, cihazda kayıtlı sürüm bu sayıdan küçükse
    // "urunler" koleksiyonundaki fiyatları assets'teki güncel verilerle
    // otomatik olarak üzerine yazar (elle eklenen özel ürünlere dokunmaz,
    // çünkü onların id'si assets'tekilerle çakışmaz).
    private const val CURRENT_PRODUCTS_DATA_VERSION = 4

    // ------------------------------------------------------------------
    // İLK KURULUM / GÜNCELLEME:
    // - Müşteri listesi: koleksiyon boşsa bir kereliğine assets'ten yüklenir.
    // - Ürün listesi: cihazdaki sürüm numarası güncel değilse, assets'teki
    //   fiyat listesi Firestore'daki ürünlerin üzerine yazılır (id eşleşmesiyle).
    // ------------------------------------------------------------------
    fun ensureInitialSync(context: Context, onDone: () -> Unit, onError: (Exception) -> Unit) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        fun musterileriSenkronEt(devam: () -> Unit) {
            if (prefs.getBoolean(KEY_CUSTOMERS_SYNCED, false)) {
                devam()
                return
            }
            db.collection(CUSTOMERS_COLLECTION).limit(1).get()
                .addOnSuccessListener { snap ->
                    if (!snap.isEmpty) {
                        prefs.edit().putBoolean(KEY_CUSTOMERS_SYNCED, true).apply()
                        devam()
                        return@addOnSuccessListener
                    }
                    try {
                        val customersJson = context.assets.open("customers.json")
                            .bufferedReader(Charsets.UTF_8).use { it.readText() }
                        syncCustomers(JSONArray(customersJson)) {
                            prefs.edit().putBoolean(KEY_CUSTOMERS_SYNCED, true).apply()
                            devam()
                        }
                    } catch (e: Exception) {
                        onError(e)
                    }
                }
                .addOnFailureListener { onError(it) }
        }

        fun urunleriSenkronEt(devam: () -> Unit) {
            val kayitliSurum = prefs.getInt(KEY_PRODUCTS_DATA_VERSION, 0)
            if (kayitliSurum >= CURRENT_PRODUCTS_DATA_VERSION) {
                devam()
                return
            }
            try {
                val productsJson = context.assets.open("products.json")
                    .bufferedReader(Charsets.UTF_8).use { it.readText() }
                // Önce eski katalog ürünlerini (id deseni "pNNNN" olanları) temizle,
                // ardından güncel fiyat listesini yükle. Elle eklenen özel ürünler
                // (rastgele Firestore id'li) bu temizlikten etkilenmez.
                temizleEskiKatalog {
                    syncProducts(JSONArray(productsJson)) {
                        prefs.edit().putInt(KEY_PRODUCTS_DATA_VERSION, CURRENT_PRODUCTS_DATA_VERSION).apply()
                        devam()
                    }
                }
            } catch (e: Exception) {
                onError(e)
            }
        }

        urunleriSenkronEt {
            musterileriSenkronEt {
                onDone()
            }
        }
    }

    private fun temizleEskiKatalog(onDone: () -> Unit) {
        val katalogIdDeseni = Regex("^p\\d+$")
        db.collection(PRODUCTS_COLLECTION).get()
            .addOnSuccessListener { snap ->
                val silinecekIdler = snap.documents
                    .map { it.id }
                    .filter { katalogIdDeseni.matches(it) }
                if (silinecekIdler.isEmpty()) {
                    onDone()
                    return@addOnSuccessListener
                }
                var index = 0
                val chunkSize = 400
                fun deleteNextChunk() {
                    if (index >= silinecekIdler.size) { onDone(); return }
                    val batch = db.batch()
                    val end = minOf(index + chunkSize, silinecekIdler.size)
                    for (i in index until end) {
                        batch.delete(db.collection(PRODUCTS_COLLECTION).document(silinecekIdler[i]))
                    }
                    batch.commit().addOnSuccessListener {
                        index = end
                        deleteNextChunk()
                    }.addOnFailureListener { onDone() }
                }
                deleteNextChunk()
            }
            .addOnFailureListener { onDone() }
    }

    private fun syncProducts(arr: JSONArray, onDone: () -> Unit) {
        val chunkSize = 400
        val total = arr.length()
        if (total == 0) { onDone(); return }
        var index = 0

        fun writeNextChunk() {
            if (index >= total) { onDone(); return }
            val batch = db.batch()
            val end = minOf(index + chunkSize, total)
            for (i in index until end) {
                val o = arr.getJSONObject(i)
                val docRef = db.collection(PRODUCTS_COLLECTION).document(o.optString("id"))
                val data = mapOf(
                    "id" to o.optString("id"),
                    "ad" to o.optString("ad"),
                    "kategori" to o.optString("kategori"),
                    "birim" to o.optString("birim"),
                    "fiyat" to o.optDouble("fiyat"),
                    "ekBilgi" to o.optString("ekBilgi")
                )
                batch.set(docRef, data)
            }
            batch.commit().addOnSuccessListener {
                index = end
                writeNextChunk()
            }.addOnFailureListener { onDone() }
        }
        writeNextChunk()
    }

    private fun syncCustomers(arr: JSONArray, onDone: () -> Unit) {
        val chunkSize = 400
        val total = arr.length()
        if (total == 0) { onDone(); return }
        var index = 0

        fun writeNextChunk() {
            if (index >= total) { onDone(); return }
            val batch = db.batch()
            val end = minOf(index + chunkSize, total)
            for (i in index until end) {
                val o = arr.getJSONObject(i)
                val docRef = db.collection(CUSTOMERS_COLLECTION).document(o.optString("id"))
                val data = mapOf(
                    "id" to o.optString("id"),
                    "ad" to o.optString("ad"),
                    "il" to o.optString("il")
                )
                batch.set(docRef, data)
            }
            batch.commit().addOnSuccessListener {
                index = end
                writeNextChunk()
            }.addOnFailureListener { onDone() }
        }
        writeNextChunk()
    }

    // ------------------------------------------------------------------
    // OKUMA
    // ------------------------------------------------------------------
    fun getProducts(onResult: (List<Product>) -> Unit, onError: (Exception) -> Unit) {
        db.collection(PRODUCTS_COLLECTION).get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { it.toObject(Product::class.java) }
                    .sortedBy { it.ad }
                onResult(list)
            }
            .addOnFailureListener { onError(it) }
    }

    fun getCustomers(onResult: (List<Customer>) -> Unit, onError: (Exception) -> Unit) {
        db.collection(CUSTOMERS_COLLECTION).get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { it.toObject(Customer::class.java) }
                    .sortedBy { it.ad }
                onResult(list)
            }
            .addOnFailureListener { onError(it) }
    }

    fun getOffers(onResult: (List<Offer>) -> Unit, onError: (Exception) -> Unit) {
        db.collection(OFFERS_COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { doc ->
                    doc.toObject(Offer::class.java)?.also { it.firestoreId = doc.id }
                }
                onResult(list)
            }
            .addOnFailureListener { onError(it) }
    }

    // ------------------------------------------------------------------
    // ÜRÜN / MÜŞTERİ YÖNETİMİ (uygulama içinden ekle / düzenle / sil)
    // ------------------------------------------------------------------
    fun saveProduct(product: Product, onResult: () -> Unit, onError: (Exception) -> Unit) {
        val id = product.id.ifBlank { db.collection(PRODUCTS_COLLECTION).document().id }
        val data = mapOf(
            "id" to id,
            "ad" to product.ad,
            "kategori" to product.kategori,
            "birim" to product.birim,
            "fiyat" to product.fiyat,
            "ekBilgi" to product.ekBilgi
        )
        db.collection(PRODUCTS_COLLECTION).document(id).set(data)
            .addOnSuccessListener { onResult() }
            .addOnFailureListener { onError(it) }
    }

    fun deleteProduct(id: String, onResult: () -> Unit, onError: (Exception) -> Unit) {
        db.collection(PRODUCTS_COLLECTION).document(id).delete()
            .addOnSuccessListener { onResult() }
            .addOnFailureListener { onError(it) }
    }

    fun saveCustomer(customer: Customer, onResult: () -> Unit, onError: (Exception) -> Unit) {
        val id = customer.id.ifBlank { db.collection(CUSTOMERS_COLLECTION).document().id }
        val data = mapOf(
            "id" to id,
            "ad" to customer.ad,
            "il" to customer.il
        )
        db.collection(CUSTOMERS_COLLECTION).document(id).set(data)
            .addOnSuccessListener { onResult() }
            .addOnFailureListener { onError(it) }
    }

    fun deleteCustomer(id: String, onResult: () -> Unit, onError: (Exception) -> Unit) {
        db.collection(CUSTOMERS_COLLECTION).document(id).delete()
            .addOnSuccessListener { onResult() }
            .addOnFailureListener { onError(it) }
    }

    // ------------------------------------------------------------------
    // YAZMA (Teklifler)
    // ------------------------------------------------------------------
    fun saveNewOffer(offer: Offer, onResult: (String) -> Unit, onError: (Exception) -> Unit) {
        val data = offerToMap(offer)
        db.collection(OFFERS_COLLECTION).add(data)
            .addOnSuccessListener { ref -> onResult(ref.id) }
            .addOnFailureListener { onError(it) }
    }

    fun updateOffer(offer: Offer, onResult: () -> Unit, onError: (Exception) -> Unit) {
        if (offer.firestoreId.isBlank()) {
            onError(IllegalStateException("Teklifin Firestore kimliği yok"))
            return
        }
        val data = offerToMap(offer)
        db.collection(OFFERS_COLLECTION).document(offer.firestoreId).set(data)
            .addOnSuccessListener { onResult() }
            .addOnFailureListener { onError(it) }
    }

    fun deleteOffer(firestoreId: String, onResult: () -> Unit, onError: (Exception) -> Unit) {
        db.collection(OFFERS_COLLECTION).document(firestoreId).delete()
            .addOnSuccessListener { onResult() }
            .addOnFailureListener { onError(it) }
    }

    private fun offerToMap(offer: Offer): Map<String, Any?> {
        val linesList = offer.lines.map { line ->
            mapOf(
                "siraNo" to line.siraNo,
                "urunAdi" to line.urunAdi,
                "birim" to line.birim,
                "adet" to line.adet,
                "birimFiyat" to line.birimFiyat,
                "ekBilgi" to line.ekBilgi,
                "iskontoUygulanir" to line.iskontoUygulanir
            )
        }
        return mapOf(
            "musteriAdi" to offer.musteriAdi,
            "musteriIl" to offer.musteriIl,
            "tarihMillis" to offer.tarihMillis,
            "gecerlilikMillis" to offer.gecerlilikMillis,
            "iskontoYuzde" to offer.iskontoYuzde,
            "lines" to linesList,
            "araToplam" to offer.araToplam,
            "iskontoTutari" to offer.iskontoTutari,
            "kdvOrani" to offer.kdvOrani,
            "kdvTutari" to offer.kdvTutari,
            "genelToplam" to offer.genelToplam,
            "olusturanEmail" to offer.olusturanEmail,
            "teslimSuresi" to offer.teslimSuresi,
            "createdAt" to if (offer.createdAt != 0L) offer.createdAt else System.currentTimeMillis()
        )
    }
}
