package com.baszincir.teklif.util

import com.baszincir.teklif.data.Product

object ProductSortUtil {

    // Fiyat listesindeki mantıksal kategori sırası. Listede olmayan
    // (örn. "Manuel Eklenen Ürün") kategoriler en sona düşer.
    private val KATEGORI_SIRA = listOf(
        "Makaralı Zincir",
        "DIN 766 Kalibre Zincir",
        "DIN 5685 Torba Zincir",
        "DIN 763 Torba Zincir",
        "G-80 Zincir",
        "Bükülü Zincir",
        "Halka",
        "Toka",
        "Dizgin Toka",
        "Düğümlü Zincir",
        "Florasan Askı Zinciri",
        "Besi Zinciri",
        "Köpek Zinciri (Çılbır)",
        "D Halka",
        "Köprü",
        "Çift Kafa Döner",
        "Saraç Döneri",
        "Dönerli Maşa",
        "S Kanca"
    )

    fun kategoriIndex(kategori: String): Int {
        val idx = KATEGORI_SIRA.indexOf(kategori)
        return if (idx == -1) KATEGORI_SIRA.size else idx
    }

    private val sayiRegex = Regex("\\d+(?:[.,]\\d+)?")

    /**
     * Ürün adındaki sayıları (mm, No, ölçü vb.) bularak küçükten büyüğe
     * doğal bir sıralama anahtarı üretir. "Beyaz" varyantı "Normal"/"Parlak"
     * varyantından sonra gelsin diye ayrı bir rütbe eklenir.
     */
    private fun dogalSiralamaAnahtari(ad: String): List<Double> {
        val sayilar = sayiRegex.findAll(ad)
            .map { it.value.replace(',', '.').toDoubleOrNull() ?: 0.0 }
            .toList()
        val birinci = sayilar.getOrElse(0) { 0.0 }
        val ikinci = sayilar.getOrElse(1) { 0.0 }
        val varyantRutbe = if (ad.contains("Beyaz")) 1.0 else 0.0
        return listOf(birinci, ikinci, varyantRutbe)
    }

    /**
     * Ürünleri önce kategori sırasına, sonra kategori içinde küçükten
     * büyüğe (sayısal) sıraya göre sıralar.
     */
    fun sirala(urunler: List<Product>): List<Product> {
        return urunler.sortedWith(
            compareBy<Product> { kategoriIndex(it.kategori) }
                .thenBy { it.kategori }
                .thenComparator { a, b ->
                    val ka = dogalSiralamaAnahtari(a.ad)
                    val kb = dogalSiralamaAnahtari(b.ad)
                    for (i in ka.indices) {
                        val cmp = ka[i].compareTo(kb[i])
                        if (cmp != 0) return@thenComparator cmp
                    }
                    a.ad.compareTo(b.ad)
                }
        )
    }
}
