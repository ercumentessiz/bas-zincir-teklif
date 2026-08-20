package com.baszincir.teklif.util

import java.util.Locale

private val TR_LOCALE = Locale("tr", "TR")

/**
 * Türkçe karakterlere göre doğru küçük harfe çevirme (İ -> i, I -> ı).
 * Java'nın varsayılan (İngilizce) locale'i ile küçültme yapıldığında
 * "İSTANBUL".lowercase() -> "i̇stanbul" olur ve düz "i" ile eşleşmez.
 * Arama kutularında büyük/küçük harf duyarsız eşleşme için bunun yerine
 * bu fonksiyon kullanılmalı.
 */
fun trLower(text: String): String = text.lowercase(TR_LOCALE)

/**
 * Veritabanında "KG" / "METRE" / "ADET" olarak saklanan birim değerini,
 * ekranda ve PDF'te gösterilecek kısaltmaya çevirir. Saklanan değer
 * (arama, karşılaştırma vb. için) hiç değişmez, sadece görünüm kısaltılır.
 */
fun birimKisaltma(birim: String): String = when (birim) {
    "KG" -> "KG."
    "METRE" -> "MT."
    "ADET" -> "AD."
    else -> birim
}
