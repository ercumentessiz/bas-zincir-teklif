package com.baszincir.teklif.data

import com.baszincir.teklif.util.trLower
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

/**
 * Firestore "urunler" koleksiyonundaki bir ürünü temsil eder.
 */
data class Product(
    var id: String = "",
    var ad: String = "",
    var kategori: String = "",
    var birim: String = "",
    var fiyat: Double = 0.0,
    var ekBilgi: String = ""
) {
    @get:Exclude
    val aramaMetni: String
        get() = trLower(ad)
}

/**
 * Firestore "musteriler" koleksiyonundaki bir müşteriyi temsil eder.
 */
data class Customer(
    var id: String = "",
    var ad: String = "",
    var il: String = ""
) {
    @get:Exclude
    val aramaMetni: String
        get() = trLower("$ad $il")
}

/**
 * Bir tekliftteki tek bir ürün satırı.
 */
data class OfferLine(
    var siraNo: Int = 0,
    var urunAdi: String = "",
    var birim: String = "",
    var adet: Double = 0.0,
    var birimFiyat: Double = 0.0,
    var ekBilgi: String = "",
    var iskontoUygulanir: Boolean = true
) {
    @get:Exclude
    val satirToplam: Double
        get() = adet * birimFiyat
}

/**
 * Firestore "teklifler" koleksiyonundaki bir teklifi temsil eder.
 */
data class Offer(
    @get:Exclude @set:Exclude
    var firestoreId: String = "",
    var musteriAdi: String = "",
    var musteriIl: String = "",
    var tarihMillis: Long = 0L,
    var gecerlilikMillis: Long = 0L,
    var iskontoYuzde: Double = 0.0,
    var lines: List<OfferLine> = emptyList(),
    var araToplam: Double = 0.0,
    var iskontoTutari: Double = 0.0,
    var kdvOrani: Double = 20.0,
    var kdvTutari: Double = 0.0,
    var genelToplam: Double = 0.0,
    var olusturanEmail: String = "",
    var teslimSuresi: String = "",
    @PropertyName("createdAt")
    var createdAt: Long = 0L
)
