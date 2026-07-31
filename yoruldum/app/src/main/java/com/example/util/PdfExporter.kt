package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.JournalEntryEntity
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    fun generateAndShareJournalPdf(
        context: Context,
        userName: String,
        userCode: String,
        entries: List<JournalEntryEntity>
    ): File? {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 width in points
            val pageHeight = 842 // A4 height in points
            val leftMargin = 40f
            val rightMargin = 555f
            val maxTextWidth = rightMargin - leftMargin // 515 points

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.rgb(15, 23, 42) // Slate Dark
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val subtitlePaint = Paint().apply {
                color = Color.rgb(180, 140, 20) // Gold accent
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            }

            val bodyTitlePaint = Paint().apply {
                color = Color.rgb(30, 41, 59)
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val bodyContentPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 11f
                typeface = Typeface.DEFAULT
            }

            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1.5f
            }

            fun drawHeader(c: Canvas, yStart: Float): Float {
                var currentY = yStart
                c.drawText("SoyAğacı Miras Günlüğü", leftMargin, currentY, titlePaint)
                currentY += 22f
                c.drawText("Kullanıcı: $userName (ID: $userCode) • Toplam Anı: ${entries.size}", leftMargin, currentY, subtitlePaint)
                currentY += 18f
                c.drawLine(leftMargin, currentY, rightMargin, currentY, linePaint)
                currentY += 25f
                return currentY
            }

            fun drawFooter(c: Canvas) {
                c.drawText("SoyAğacı Miras & Vasiyet Sistemi • Sayfa $pageNumber", leftMargin, 810f, subtitlePaint)
            }

            var y = drawHeader(canvas, 50f)

            fun newPage(): Canvas {
                drawFooter(canvas)
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                val newCanvas = page.canvas
                y = drawHeader(newCanvas, 50f)
                return newCanvas
            }

            fun drawWrappedParagraph(
                text: String,
                paint: Paint,
                lineSpacing: Float
            ) {
                val paragraphs = text.split("\n")
                for (paragraph in paragraphs) {
                    val words = paragraph.split(Regex("\\s+"))
                    var currentLine = ""
                    for (word in words) {
                        if (word.isEmpty()) continue
                        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                        if (paint.measureText(testLine) > maxTextWidth && currentLine.isNotEmpty()) {
                            if (y > 770f) canvas = newPage()
                            canvas.drawText(currentLine, leftMargin, y, paint)
                            y += lineSpacing
                            currentLine = word
                        } else {
                            currentLine = testLine
                        }
                    }
                    if (currentLine.isNotEmpty()) {
                        if (y > 770f) canvas = newPage()
                        canvas.drawText(currentLine, leftMargin, y, paint)
                        y += lineSpacing
                    }
                }
            }

            if (entries.isEmpty()) {
                canvas.drawText("Henüz kaydedilmiş bir günlük anısı bulunmuyor.", leftMargin, y, bodyContentPaint)
            } else {
                for ((index, entry) in entries.withIndex()) {
                    if (y > 730f) canvas = newPage()

                    // Title
                    drawWrappedParagraph("${index + 1}. ${entry.title}", bodyTitlePaint, 18f)
                    
                    // Date & Location
                    drawWrappedParagraph("Tarih: ${entry.dateString} ${entry.timeString} | Konum: ${entry.deviceLocationInfo}", subtitlePaint, 15f)
                    y += 4f

                    // Full Content formatted & wrapped
                    drawWrappedParagraph(entry.content, bodyContentPaint, 15f)
                    y += 20f

                    if (index < entries.size - 1) {
                        if (y > 770f) canvas = newPage()
                        canvas.drawLine(leftMargin, y - 10f, rightMargin, y - 10f, linePaint)
                        y += 10f
                    }
                }
            }

            drawFooter(canvas)
            pdfDocument.finishPage(page)

            val outputDir = File(context.cacheDir, "pdf")
            if (!outputDir.exists()) outputDir.mkdirs()
            val pdfFile = File(outputDir, "Miras_Gunlugu_${System.currentTimeMillis()}.pdf")

            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun sharePdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Günlük PDF Dosyasını Paylaş"))
    }
}

