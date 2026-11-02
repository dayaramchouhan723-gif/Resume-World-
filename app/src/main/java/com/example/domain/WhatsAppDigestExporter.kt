package com.example.domain

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WhatsAppDigestExporter {

    fun generateDailyDigestPdf(context: Context, jobs: List<Job>): File? {
        if (jobs.isEmpty()) return null
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.parseColor("#080812")
            textSize = 18f
            isFakeBoldText = true
        }
        val subPaint = Paint().apply {
            color = Color.parseColor("#FF2092")
            textSize = 12f
        }
        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
        }

        var y = 40f
        canvas.drawText("SANAM Daily Job Digest • Top 5 Verified Overseas Jobs", 40f, y, titlePaint)
        y += 20f
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        canvas.drawText("Generated on ${dateFormat.format(Date())} • Boli se Resume, Voice se Naukri", 40f, y, subPaint)
        y += 30f

        jobs.take(5).forEachIndexed { idx, job ->
            canvas.drawText("${idx + 1}. ${job.title} — ${job.company}", 40f, y, Paint().apply { textSize = 12f; isFakeBoldText = true; color = Color.BLACK })
            y += 15f
            canvas.drawText("   Location: ${job.location} | Source: ${job.source}", 40f, y, bodyPaint)
            y += 14f
            canvas.drawText("   Status: ${job.sponsorshipStatus} | Link: ${job.applyLink}", 40f, y, bodyPaint)
            y += 24f
        }

        canvas.drawText("Design by Dayaram Chouhan • SANAM AI Resume Girl", 40f, 800f, subPaint)
        doc.finishPage(page)

        val file = File(context.cacheDir, "SANAM_Daily_Digest_${System.currentTimeMillis()}.pdf")
        return try {
            FileOutputStream(file).use { out -> doc.writeTo(out) }
            doc.close()
            file
        } catch (e: Exception) {
            doc.close()
            null
        }
    }

    fun shareDigestToWhatsApp(context: Context, pdfFile: File?) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            if (pdfFile != null) {
                val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            putExtra(Intent.EXTRA_TEXT, "Hi Boss! Here is today's SANAM Top 5 Overseas Job Digest (LMIA & Verified Jobs) 📄✨\nDownload app: SANAM - My AI Resume Girl")
            setPackage("com.whatsapp")
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback chooser if WhatsApp not installed
            val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "SANAM Daily Jobs Digest: Check top LMIA jobs for Canada & Australia!")
            }, "Share SANAM Jobs Digest")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooser)
        }
    }
}
