package com.baszincir.teklif.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateHelper {

    private val displayFormat = SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR"))
    private val displayFormatWithTime = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))
    private val timeOnlyFormat = SimpleDateFormat("HH:mm", Locale("tr", "TR"))

    fun bugunMillis(): Long = System.currentTimeMillis()

    fun formatTarih(millis: Long): String = displayFormat.format(millis)

    fun formatTarihSaat(millis: Long): String = displayFormatWithTime.format(millis)

    fun formatSaat(millis: Long): String = timeOnlyFormat.format(millis)

    /**
     * Teklif geçerlilik tarihi:
     * - Normalde bir sonraki iş günü saat 17:00
     * - Teklif Cuma günü hazırlanmışsa: Pazartesi saat 17:00
     */
    fun gecerlilikTarihiHesapla(teklifTarihiMillis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = teklifTarihiMillis

        val gun = cal.get(Calendar.DAY_OF_WEEK) // Calendar.SUNDAY=1 ... Calendar.SATURDAY=7

        val eklenecekGun = when (gun) {
            Calendar.FRIDAY -> 3    // Cuma -> Pazartesi
            Calendar.SATURDAY -> 2  // Cumartesi -> Pazartesi
            else -> 1               // diğer günler -> bir sonraki gün
        }

        cal.add(Calendar.DAY_OF_YEAR, eklenecekGun)
        cal.set(Calendar.HOUR_OF_DAY, 17)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
