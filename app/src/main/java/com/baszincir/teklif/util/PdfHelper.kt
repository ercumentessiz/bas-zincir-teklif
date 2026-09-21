package com.baszincir.teklif.util

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.baszincir.teklif.data.Offer
import java.text.NumberFormat
import java.util.Locale

object PdfHelper {

    private const val PAGE_WIDTH = 595   // A4 - 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f

    private val tlFormat: NumberFormat = NumberFormat.getNumberInstance(Locale("tr", "TR")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    private val miktarFormat: NumberFormat = NumberFormat.getNumberInstance(Locale("tr", "TR")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }

    private fun money(v: Double): String = "${tlFormat.format(v)} TL"

    fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), " ").trim()
    }

    private fun pdfPaint(): Paint = Paint().apply {
        isAntiAlias = true
        isLinearText = true
        isSubpixelText = true
    }

    fun createAndSaveOfferPdf(context: Context, offer: Offer): Uri? {
        val document = PdfDocument()

        val titlePaint = pdfPaint().apply { color = Color.BLACK; textSize = 20f }
        val subPaint = pdfPaint().apply { color = Color.DKGRAY; textSize = 11f }
        val headerPaint = pdfPaint().apply { color = Color.WHITE; textSize = 10f }
        val cellPaint = pdfPaint().apply { color = Color.BLACK; textSize = 9.5f }
        val cellPaintBold = pdfPaint().apply { color = Color.BLACK; textSize = 10.5f }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        val headerBgPaint = Paint().apply { color = Color.parseColor("#2A2E7F") }
        val totalsLabelPaint = pdfPaint().apply { color = Color.BLACK; textSize = 11f }
        val totalsValuePaint = pdfPaint().apply { color = Color.BLACK; textSize = 11f }
        val footerPaint = pdfPaint().apply { color = Color.GRAY; textSize = 9f }

        val colSira = 28f
        val colUrun = 235f
        val colBirim = 38f
        val colAdet = 48f
        val colFiyat = 82f
        val colTutar = 92f
        val tableLeft = MARGIN
        val tableWidth = colSira + colUrun + colBirim + colAdet + colFiyat + colTutar

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y: Float

        fun harfGenisligi(text: String, paint: Paint): Float {
            var w = 0f
            for (ch in text) w += paint.measureText(ch.toString())
            return w
        }

        fun ciz(text: String, x: Float, y: Float, paint: Paint, kalin: Boolean = false) {
            var cx = x
            for (ch in text) {
                val s = ch.toString()
                canvas.drawText(s, cx, y, paint)
                if (kalin) canvas.drawText(s, cx + 0.3f, y, paint)
                cx += paint.measureText(s)
            }
        }

        fun drawHeader(): Float {
            var yy = MARGIN
            val contentWidth = PAGE_WIDTH - 2 * MARGIN

            try {
                context.assets.open("letterhead.png").use { input ->
                    val bmp = BitmapFactory.decodeStream(input)
                    val letterheadH = contentWidth * bmp.height / bmp.width
                    val dst = RectF(MARGIN, yy, MARGIN + contentWidth, yy + letterheadH)
                    canvas.drawBitmap(bmp, null, dst, null)
                    yy += letterheadH + 12f
                }
            } catch (_: Exception) {
                // Antet bulunamazsa sessizce geç
            }

            val tarihStr = "Tarih: ${DateHelper.formatTarih(offer.tarihMillis)}"
            ciz("TEKLİF", MARGIN, yy + 14f, titlePaint, kalin = true)
            val tarihValWidth = harfGenisligi(tarihStr, subPaint)
            ciz(tarihStr, PAGE_WIDTH - MARGIN - tarihValWidth, yy + 14f, subPaint)

            yy += 22f
            canvas.drawLine(MARGIN, yy, PAGE_WIDTH - MARGIN, yy, linePaint)
            yy += 16f

            ciz("Müşteri: ${offer.musteriAdi}", MARGIN, yy, cellPaintBold, kalin = true)
            yy += 14f
            if (offer.musteriIl.isNotBlank()) {
                ciz("İl: ${offer.musteriIl}", MARGIN, yy, subPaint)
                yy += 14f
            }
            yy += 8f
            return yy
        }

        fun drawTableHeader(yStart: Float): Float {
            var yy = yStart
            canvas.drawRect(tableLeft, yy, tableLeft + tableWidth, yy + 20f, headerBgPaint)
            var x = tableLeft + 4f
            val ty = yy + 14f
            ciz("No", x, ty, headerPaint, kalin = true); x += colSira
            ciz("Ürün", x, ty, headerPaint, kalin = true); x += colUrun
            ciz("Miktar", x, ty, headerPaint, kalin = true); x += colAdet
            ciz("Birim", x, ty, headerPaint, kalin = true); x += colBirim
            ciz("B.Fiyat", x, ty, headerPaint, kalin = true); x += colFiyat
            ciz("Tutar", x, ty, headerPaint, kalin = true)
            return yy + 20f
        }

        y = drawHeader()
        y = drawTableHeader(y)

        val rowHeight = 18f
        val bottomLimit = PAGE_HEIGHT - MARGIN - 110f // alt bilgiler için pay (Teslim Süresi + Ödeme dahil)

        for (line in offer.lines) {
            if (y + rowHeight > bottomLimit) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                y = MARGIN
                y = drawTableHeader(y)
            }
            var x = tableLeft + 4f
            val ty = y + 13f
            ciz(line.siraNo.toString(), x, ty, cellPaint); x += colSira
            val urunAdiKirp = if (line.urunAdi.length > 46) line.urunAdi.substring(0, 44) + "…" else line.urunAdi
            ciz(urunAdiKirp, x, ty, cellPaint); x += colUrun
            ciz(miktarFormat.format(line.adet), x, ty, cellPaint); x += colAdet
            ciz(birimKisaltma(line.birim), x, ty, cellPaint); x += colBirim
            ciz(tlFormat.format(line.birimFiyat), x, ty, cellPaint); x += colFiyat
            ciz(tlFormat.format(line.satirToplam), x, ty, cellPaint)

            y += rowHeight
            canvas.drawLine(tableLeft, y, tableLeft + tableWidth, y, linePaint)
        }

        // Toplamlar için yer kontrolü
        val totalsHeight = 150f
        if (y + totalsHeight > PAGE_HEIGHT - MARGIN) {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            y = MARGIN
        }

        y += 16f
        val labelX = tableLeft + colSira + colUrun + colBirim
        val valueRight = tableLeft + tableWidth

        val araToplamLabel = "Ara Toplam:"
        val iskontoLabel = "İskonto (%${tlFormat.format(offer.iskontoYuzde)}):"
        val iskontoSonrasi = offer.araToplam - offer.iskontoTutari
        val iskontoSonrasiLabel = "İskonto Sonrası Toplam:"
        val kdvLabel = "KDV (%${tlFormat.format(offer.kdvOrani)}):"
        val genelToplamLabel = "GENEL TOPLAM:"

        val etiketGenislikleri = listOf(
            harfGenisligi(araToplamLabel, totalsLabelPaint),
            harfGenisligi(iskontoLabel, totalsLabelPaint),
            harfGenisligi(iskontoSonrasiLabel, totalsLabelPaint),
            harfGenisligi(kdvLabel, totalsLabelPaint),
            harfGenisligi(genelToplamLabel, totalsValuePaint)
        )
        val colonX = labelX + (etiketGenislikleri.maxOrNull() ?: 0f)

        fun totalRow(label: String, value: String, bold: Boolean = false) {
            val lp = if (bold) totalsValuePaint else totalsLabelPaint
            val labelWidth = harfGenisligi(label, lp)
            ciz(label, colonX - labelWidth, y, lp, kalin = bold)
            val vw = harfGenisligi(value, lp)
            ciz(value, valueRight - vw, y, lp, kalin = bold)
            y += 16f
        }

        totalRow(araToplamLabel, money(offer.araToplam))
        totalRow(iskontoLabel, "- ${money(offer.iskontoTutari)}")
        totalRow(iskontoSonrasiLabel, money(iskontoSonrasi))
        totalRow(kdvLabel, money(offer.kdvTutari))
        canvas.drawLine(labelX, y, valueRight, y, linePaint)
        y += 14f
        totalRow(genelToplamLabel, money(offer.genelToplam), bold = true)

        y += 18f
        ciz(
            "Teklif Geçerlilik Tarihi: ${DateHelper.formatTarih(offer.gecerlilikMillis)} Saat: ${DateHelper.formatSaat(offer.gecerlilikMillis)}",
            MARGIN, y, cellPaintBold, kalin = true
        )

        // Teslim Süresi: kullanıcının formda girdiği metin (örn. "3 İş Günü")
        y += 18f
        val teslimMetni = offer.teslimSuresi.ifBlank { "-" }
        ciz("Teslim Süresi: $teslimMetni", MARGIN, y, cellPaintBold, kalin = true)

        // Ödeme: Peşin/Vadeli seçimi + varsa opsiyonel detay metni.
        // Detay girilmişse detay gösterilir, girilmemişse sadece seçilen tip.
        // Hiçbir seçim yapılmamışsa "-" gösterilir.
        y += 18f
        val odemeMetni = when {
            offer.odemeTipi.isBlank() -> "-"
            offer.odemeDetay.isNotBlank() -> offer.odemeDetay
            else -> offer.odemeTipi
        }
        ciz("Ödeme: $odemeMetni", MARGIN, y, cellPaintBold, kalin = true)

        y += 20f
        val kdvNotu = if (offer.kdvOrani == 0.0)
            "* Bu teklif KDV'siz hazırlanmıştır."
        else
            "* Belirtilen ürün fiyatlarına KDV dahil değildir, KDV ayrıca eklenmiştir."
        ciz("$kdvNotu Fiyat değiştirme hakkımız mahfuzdur.", MARGIN, y, footerPaint)

        document.finishPage(page)

        val fileName = "${sanitizeFileName(offer.musteriAdi)} – Teklif.pdf"
        val uri = savePdfToDownloads(context, document, fileName)
        document.close()
        return uri
    }

    private fun savePdfToDownloads(context: Context, document: PdfDocument, fileName: String): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
                resolver.openOutputStream(uri)?.use { out -> document.writeTo(out) }
                uri
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = java.io.File(downloadsDir, fileName)
                java.io.FileOutputStream(file).use { out -> document.writeTo(out) }
                androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}
