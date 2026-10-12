package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.ResumeFormState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

object ResumeFileHelper {

    /**
     * Saves a clean, single-column PDF directly to the device's Downloads folder
     * AND returns the user-visible file name.
     *
     * Because the PDF is rendered strictly top-to-bottom in a single column without tables
     * or sidebars, copy-pasting the PDF text into Notepad preserves the exact reading order.
     */
    fun downloadPdfToDownloads(context: Context, formState: ResumeFormState): Result<String> {
        return runCatching {
            val safeBase = (formState.fullName.ifBlank { "Canadian_Resume" })
                .trim()
                .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val fileName = "${safeBase}_Resume.pdf"
            val doc = buildPdfDocument(formState)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        ?: throw IllegalStateException("Could not create PDF in Downloads.")
                    resolver.openOutputStream(uri)?.use { out ->
                        doc.writeTo(out)
                    } ?: throw IllegalStateException("Could not write PDF stream.")
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                } else {
                    @Suppress("DEPRECATION")
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        ?: context.filesDir
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    val outFile = File(downloadsDir, fileName)
                    FileOutputStream(outFile).use { out ->
                        doc.writeTo(out)
                    }
                }
            } finally {
                doc.close()
            }
            fileName
        }
    }

    /**
     * Saves the plain-text resume (.txt) directly to the device's Downloads folder
     * so the user can open it in Notepad or upload it to any job portal.
     */
    fun downloadTxtToDownloads(context: Context, formState: ResumeFormState): Result<String> {
        return runCatching {
            val safeBase = (formState.fullName.ifBlank { "Canadian_Resume" })
                .trim()
                .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val fileName = "${safeBase}_Resume.txt"
            val plainText = formState.toPlainText()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IllegalStateException("Could not create TXT file in Downloads.")
                resolver.openOutputStream(uri)?.use { out ->
                    out.write(plainText.toByteArray(Charsets.UTF_8))
                } ?: throw IllegalStateException("Could not write TXT stream.")
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: context.filesDir
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val outFile = File(downloadsDir, fileName)
                outFile.writeText(plainText, Charsets.UTF_8)
            }
            fileName
        }
    }

    /**
     * Launches the Android system Print / Save-as-PDF dialog with a clean,
     * single-column Canadian resume layout matching the `.paper` preview.
     */
    fun printOrSavePdf(context: Context, formState: ResumeFormState) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = if (formState.fullName.isNotBlank()) {
            "${formState.fullName.trim().replace(Regex("\\s+"), "_")}_Resume"
        } else {
            "Canadian_ATS_Resume"
        }

        printManager.print(
            jobName,
            object : PrintDocumentAdapter() {
                private var pdfDocument: PdfDocument? = null

                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onLayoutCancelled()
                        return
                    }
                    pdfDocument = buildPdfDocument(formState)
                    val pageCount = pdfDocument?.pages?.size ?: 1
                    val info = PrintDocumentInfo.Builder("$jobName.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(pageCount)
                        .build()
                    callback.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback
                ) {
                    val doc = pdfDocument ?: buildPdfDocument(formState)
                    try {
                        FileOutputStream(destination.fileDescriptor).use { out ->
                            doc.writeTo(out)
                        }
                        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback.onWriteFailed(e.message)
                    } finally {
                        doc.close()
                        pdfDocument = null
                    }
                }
            },
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.NA_LETTER)
                .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                .build()
        )
    }

    fun exportPdfToFile(context: Context, formState: ResumeFormState): File {
        val doc = buildPdfDocument(formState)
        val safeName = (formState.fullName.ifBlank { "Resume" })
            .trim()
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(context.cacheDir, "${safeName}_ATS_Resume.pdf")
        FileOutputStream(file).use { out ->
            doc.writeTo(out)
        }
        doc.close()
        return file
    }

    /**
     * Renders a strict single-column PDF (no tables, no multi-column boxes)
     * in sequential top-to-bottom order so copy-pasting into Notepad keeps 100% exact line order.
     */
    fun buildPdfDocument(formState: ResumeFormState): PdfDocument {
        val doc = PdfDocument()
        val pageWidth = 612 // Letter width in points (8.5in * 72)
        val pageHeight = 792 // Letter height in points (11in * 72)
        val marginH = 44f
        val marginV = 44f
        val contentWidth = (pageWidth - marginH * 2).toInt()

        var pageNumber = 1
        var currentPage = doc.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        var canvas: Canvas = currentPage.canvas
        var y = marginV

        val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(17, 17, 17)
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(17, 17, 17)
            textSize = 12.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val contactPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 51, 51)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val headingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(17, 17, 17)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val jobTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(17, 17, 17)
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(17, 17, 17)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(136, 136, 136)
            strokeWidth = 0.8f
        }

        fun ensureSpace(requiredHeight: Float) {
            if (y + requiredHeight > pageHeight - marginV) {
                doc.finishPage(currentPage)
                pageNumber++
                currentPage = doc.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                )
                canvas = currentPage.canvas
                y = marginV
            }
        }

        fun drawParagraph(text: String, paint: TextPaint, indent: Float = 0f, spacingAfter: Float = 4f) {
            if (text.isBlank()) return
            val width = (contentWidth - indent).toInt().coerceAtLeast(100)
            val layout = StaticLayout.Builder
                .obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(1.5f, 1.05f)
                .setIncludePad(false)
                .build()
            ensureSpace(layout.height.toFloat() + spacingAfter)
            canvas.save()
            canvas.translate(marginH + indent, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + spacingAfter
        }

        fun drawSectionHeading(title: String) {
            y += 8f
            ensureSpace(24f)
            drawParagraph(title, headingPaint, spacingAfter = 2f)
            canvas.drawLine(marginH, y, pageWidth - marginH, y, linePaint)
            y += 6f
        }

        // 1. Header (Name, Target Title, Contact Info)
        drawParagraph(formState.fullName.ifBlank { "Your Name" }, namePaint, spacingAfter = 2f)
        if (formState.targetTitle.isNotBlank()) {
            drawParagraph(formState.targetTitle.trim(), titlePaint, spacingAfter = 3f)
        }
        val contactLine = formState.contactParts().joinToString(" | ")
        if (contactLine.isNotEmpty()) {
            drawParagraph(contactLine, contactPaint, spacingAfter = 6f)
        }

        // 2. Summary
        if (formState.summary.isNotBlank()) {
            drawSectionHeading("Summary")
            drawParagraph(formState.summary.trim(), bodyPaint, spacingAfter = 4f)
        }

        // 3. Work Experience
        val activeJobs = formState.usedJobs()
        if (activeJobs.isNotEmpty()) {
            drawSectionHeading("Work Experience")
            activeJobs.forEach { job ->
                y += 3f
                if (job.title.isNotBlank()) {
                    drawParagraph(job.title.trim(), jobTitlePaint, spacingAfter = 1.5f)
                }
                val meta = listOf(job.employer.trim(), job.dates.trim())
                    .filter { it.isNotEmpty() }
                    .joinToString(" | ")
                if (meta.isNotEmpty()) {
                    drawParagraph(meta, contactPaint, spacingAfter = 3f)
                }
                ResumeFormState.splitLines(job.bullets).forEach { rawBullet ->
                    val clean = ResumeFormState.stripBulletPrefix(rawBullet)
                    drawParagraph("- $clean", bodyPaint, indent = 8f, spacingAfter = 2.5f)
                }
            }
        }

        // 4. Education
        val eduLines = ResumeFormState.splitLines(formState.education)
        if (eduLines.isNotEmpty()) {
            drawSectionHeading("Education")
            eduLines.forEach { line ->
                drawParagraph(line, bodyPaint, spacingAfter = 3f)
            }
        }

        // 5. Skills
        if (formState.skills.isNotBlank()) {
            drawSectionHeading("Skills")
            drawParagraph(formState.skills.trim(), bodyPaint, spacingAfter = 4f)
        }

        // 6. Certifications
        val certLines = ResumeFormState.splitLines(formState.certificates)
        if (certLines.isNotEmpty()) {
            drawSectionHeading("Certifications")
            certLines.forEach { rawCert ->
                val clean = ResumeFormState.stripBulletPrefix(rawCert)
                drawParagraph("- $clean", bodyPaint, indent = 8f, spacingAfter = 2.5f)
            }
        }

        // 7. Subtle footer credit at bottom of PDF page
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(120, 120, 120)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val footerText = "Created with ResumeBuilder - Design by Dayaram Chouhan"
        val footerY = (pageHeight - 20).toFloat()
        canvas.drawText(footerText, marginH, footerY, footerPaint)

        doc.finishPage(currentPage)
        return doc
    }

    fun sharePlainText(context: Context, fullName: String, plainText: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_SUBJECT,
                if (fullName.isNotBlank()) "${fullName.trim()} - Resume" else "Resume"
            )
            putExtra(Intent.EXTRA_TEXT, plainText)
        }
        val chooser = Intent.createChooser(sendIntent, "Share Resume Text")
        context.startActivity(chooser)
    }

    suspend fun extractTextFromUri(context: Context, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri)?.lowercase() ?: ""
                val pathName = uri.lastPathSegment?.lowercase() ?: ""

                when {
                    mimeType.contains("wordprocessingml") || pathName.endsWith(".docx") -> {
                        extractDocxText(context, uri)
                    }
                    mimeType.contains("pdf") || pathName.endsWith(".pdf") -> {
                        extractPdfTextFallback(context, uri)
                    }
                    else -> {
                        contentResolver.openInputStream(uri)?.use { stream ->
                            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                        } ?: throw IllegalArgumentException("Could not open selected file.")
                    }
                }.trim().also { extracted ->
                    if (extracted.length < 20) {
                        throw IllegalArgumentException(
                            "Could not extract enough readable text from this file. Try a .docx or .txt file, or paste your resume text directly."
                        )
                    }
                }
            }
        }

    private fun extractDocxText(context: Context, uri: Uri): String {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Unable to read .docx file.")
        stream.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        val xml = zip.bufferedReader(Charsets.UTF_8).readText()
                        return xml
                            .replace(Regex("</w:p>"), "\n")
                            .replace(Regex("<w:tab[^>]*/?>"), " ")
                            .replace(Regex("<w:br[^>]*/?>"), "\n")
                            .replace(Regex("<[^>]+>"), "")
                            .replace("&amp;", "&")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&quot;", "\"")
                            .replace("&apos;", "'")
                            .lines()
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .joinToString("\n")
                    }
                    entry = zip.nextEntry
                }
            }
        }
        throw IllegalArgumentException("No word/document.xml found in .docx archive.")
    }

    private fun extractPdfTextFallback(context: Context, uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Unable to read PDF file.")
        val raw = String(bytes, Charsets.ISO_8859_1)
        val chunks = mutableListOf<String>()

        val btEtRegex = Regex("BT\\s*(.*?)\\s*ET", RegexOption.DOT_MATCHES_ALL)
        val parenRegex = Regex("\\(([^()\\\\]*(?:\\\\.[^()\\\\]*)*)\\)")

        for (block in btEtRegex.findAll(raw)) {
            val lineParts = mutableListOf<String>()
            for (match in parenRegex.findAll(block.groupValues[1])) {
                val cleaned = match.groupValues[1]
                    .replace("\\n", "\n")
                    .replace("\\r", " ")
                    .replace("\\t", " ")
                    .replace("\\(", "(")
                    .replace("\\)", ")")
                    .replace("\\\\", "\\")
                    .filter { it.code in 32..126 || it == '\n' }
                if (cleaned.isNotBlank()) {
                    lineParts.add(cleaned)
                }
            }
            if (lineParts.isNotEmpty()) {
                chunks.add(lineParts.joinToString(" "))
            }
        }

        val joined = chunks.joinToString("\n")
            .replace(Regex("[ \\t]{2,}"), " ")
            .trim()
        if (joined.length >= 40) return joined

        val asciiLines = Regex("[A-Za-z0-9@.,:/()'\"%+&#\\- ]{12,}")
            .findAll(raw)
            .map { it.value.trim() }
            .filter { line ->
                !line.startsWith("%PDF") &&
                    !line.contains("/Type") &&
                    !line.contains("/Font") &&
                    !line.contains("endobj") &&
                    !line.contains("stream") &&
                    line.count { it == ' ' } >= 2
            }
            .toList()

        return asciiLines.joinToString("\n")
    }
}
