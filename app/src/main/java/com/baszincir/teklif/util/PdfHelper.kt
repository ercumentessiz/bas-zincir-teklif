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

    fun createAndSaveOfferPdf(context: Context, offer: Offer): Uri? {
        val document = PdfDocument()

        val titlePaint = Paint().apply { color = Color.BLACK; textSize = 20f }
        val subPaint = Paint().apply { color = Color.DKGRAY; textSize = 11f }
        val headerPaint = Paint().apply { color = Color.WHITE; textSize = 10f }
        val cellPaint = Paint().apply { color = Color.BLACK; textSize = 9.5f }
        val cellPaintBold = Paint().apply { color = Color.BLACK; textSize = 10.5f }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        val headerBgPaint = Paint().apply { color = Color.parseColor("#2A2E7F") }
        val totalsLabelPaint = Paint().apply { color = Color.BLACK; textSize = 11f }
        val totalsValuePaint = Paint().apply { color = Color.BLACK; textSize = 11f }
        val footerPaint = Paint().apply { color = Color.GRAY; textSize = 9f }

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

        fun drawBold(text: String, x: Float, y: Float, paint: Paint) {
            canvas.drawText(text, x, y, paint)
            canvas.drawText(text, x + 0.35f, y, paint)
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
            } catch (_: Exception) { }

            val tarihStr = "Tarih: ${DateHelper.formatTarih(offer.tarihMillis)}"
            drawBold("TEKLİF", MARGIN, yy + 14f, titlePaint)
            val tarihValWidth = subPaint.measureText(tarihStr)
            canvas.drawText(tarihStr, PAGE_WIDTH - MARGIN - tarihValWidth, yy + 14f, subPaint)

            yy += 22f
            canvas.drawLine(MARGIN, yy, PAGE_WIDTH - MARGIN, yy, linePaint)
            yy += 16f

            drawBold("Müşteri: ${offer.musteriAdi}", MARGIN, yy, cellPaintBold)
            yy += 14f
            if (offer.musteriIl.isNotBlank()) {
                canvas.drawText("İl: ${offer.musteriIl}", MARGIN, yy, subPaint)
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
            drawBold("No", x, ty, headerPaint); x += colSira
            drawBold("Ürün", x, ty, headerPaint); x += colUrun
            drawBold("Miktar", x, ty, headerPaint); x += colAdet
            drawBold("Birim", x, ty, headerPaint); x += colBirim
            drawBold("B.Fiyat", x, ty, headerPaint); x += colFiyat
            drawBold("Tutar", x, ty, headerPaint)
            return yy + 20f
        }

        y = drawHeader()
        y = drawTableHeader(y)

        val rowHeight = 18f
        val bottomLimit = PAGE_HEIGHT - MARGIN - 90f

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
            canvas.drawText(line.siraNo.toString(), x, ty, cellPaint); x += colSira
            val urunAdiKirp = if (line.urunAdi.length > 46) line.urunAdi.substring(0, 44) + "…" else line.urunAdi
            canvas.drawText(urunAdiKirp, x, ty, cellPaint); x += colUrun
            canvas.drawText(miktarFormat.format(line.adet), x, ty, cellPaint); x += colAdet
            canvas.drawText(birimKisaltma(line.birim), x, ty, cellPaint); x += colBirim
            canvas.drawText(tlFormat.format(line.birimFiyat), x, ty, cellPaint); x += colFiyat
            canvas.drawText(tlFormat.format(line.satirToplam), x, ty, cellPaint)

            y += rowHeight
            canvas.drawLine(tableLeft, y, tableLeft + tableWidth, y, linePaint)
        }

        val totalsHeight = 130f
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
            totalsLabelPaint.measureText(araToplamLabel),
            totalsLabelPaint.measureText(iskontoLabel),
            totalsLabelPaint.measureText(iskontoSonrasiLabel),
            totalsLabelPaint.measureText(kdvLabel),
            totalsValuePaint.measureText(genelToplamLabel)
        )
        val colonX = labelX + (etiketGenislikleri.maxOrNull() ?: 0f)

        fun totalRow(label: String, value: String, bold: Boolean = false) {
            val lp = if (bold) totalsValuePaint else totalsLabelPaint
            val labelWidth = lp.measureText(label)
            if (bold) drawBold(label, colonX - labelWidth, y, lp) else canvas.drawText(label, colonX - labelWidth, y, lp)
            val vw = lp.measureText(value)
            if (bold) drawBold(value, valueRight - vw, y, lp) else canvas.drawText(value, valueRight - vw, y, lp)
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
        drawBold(
            "Teklif Geçerlilik Tarihi: ${DateHelper.formatTarih(offer.gecerlilikMillis)} Saat: ${DateHelper.formatSaat(offer.gecerlilikMillis)}",
            MARGIN, y, cellPaintBold
        )

        y += 18f
        val teslimMetni = offer.teslimSuresi.ifBlank { "-" }
        drawBold("Teslim Süresi: $teslimMetni", MARGIN, y, cellPaintBold)

        y += 20f
        val kdvNotu = if (offer.kdvOrani == 0.0)
            "* Bu teklif KDV'siz hazırlanmıştır."
        else
            "* Belirtilen ürün fiyatlarına KDV dahil değildir, KDV ayrıca eklenmiştir."
        canvas.drawText("$kdvNotu Fiyat değiştirme hakkımız mahfuzdur.", MARGIN, y, footerPaint)

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
                androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        } catch (e: Exception) {
            null
        }
    }
}
