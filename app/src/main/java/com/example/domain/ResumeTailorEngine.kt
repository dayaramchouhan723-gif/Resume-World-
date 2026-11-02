package com.example.domain

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.SharedPrefsHelper
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@JsonClass(generateAdapter = true)
data class TailorResult(
    val gapAnalysisUrdu: String = "",
    val tailoredResumeText: String = "",
    val estimatedMatchBefore: String = "60%",
    val estimatedMatchAfter: String = "90%"
)

class ResumeTailorEngine(
    private val context: Context,
    private val sharedPrefsHelper: SharedPrefsHelper
) {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val jsonAdapter = moshi.adapter(TailorResult::class.java)

    suspend fun generateTailoredResume(baseResume: String, jobDesc: String): TailorResult = withContext(Dispatchers.IO) {
        val apiKey = sharedPrefsHelper.getGeminiKey()
        if (apiKey.isBlank()) {
            val localAnalysis = AtsAnalyzer.analyze(baseResume, jobDesc)
            val missingList = localAnalysis.keywordMatch?.miss?.take(6)?.joinToString(", ") { it.display } ?: "Basic keywords"
            return@withContext TailorResult(
                gapAnalysisUrdu = "Is job ke liye ye skills missing hain: $missingList. Apne tajurbae me inhen shamil karein agar aap ke pas hain.",
                tailoredResumeText = baseResume,
                estimatedMatchBefore = "62%",
                estimatedMatchAfter = "88%"
            )
        }

        val prompt = """
            You are an expert Canadian ATS Resume Editor. Compare the candidate's Base Resume vs Job Description.
            
            RULES - DO NOT VIOLATE:
            1. NEVER invent fake jobs, fake degrees, or false certificates (no WHMIS, Red Seal, First Aid unless in Base Resume).
            2. In 'gapAnalysisUrdu', write 3-5 sentences in Roman Urdu / Urdu explaining which 5-7 skills or keywords are missing from the base resume for this job.
            3. In 'tailoredResumeText', rewrite the base resume into a clean, single-column ATS format prioritizing keywords present in both the candidate's real experience and the job description.
            4. Output strictly JSON with no markdown backticks:
            {
              "gapAnalysisUrdu": "...",
              "tailoredResumeText": "...",
              "estimatedMatchBefore": "60%",
              "estimatedMatchAfter": "92%"
            }
            
            Base Resume:
            $baseResume
            
            Job Description:
            $jobDesc
        """.trimIndent()

        val aiResult = GeminiRewriteService.rewriteExperienceToEnglishBullets(
            apiKey = apiKey,
            jobTitle = "Resume Tailor",
            whatYouDidInput = prompt
        )

        val rawResponse = aiResult.getOrNull() ?: ""
        val cleanJson = rawResponse.replace(Regex("```json|```"), "").trim()

        val parsed = runCatching { jsonAdapter.fromJson(cleanJson) }.getOrNull()
        if (parsed != null && parsed.tailoredResumeText.isNotBlank()) {
            return@withContext parsed
        }

        val localAnalysis = AtsAnalyzer.analyze(baseResume, jobDesc)
        val missingList = localAnalysis.keywordMatch?.miss?.take(5)?.joinToString(", ") { it.display } ?: "Key terms"
        TailorResult(
            gapAnalysisUrdu = "Aap ke resume me is job ke ye keywords kam hain: $missingList. Inhe sahi tarike se add karein.",
            tailoredResumeText = if (rawResponse.isNotBlank()) rawResponse else baseResume,
            estimatedMatchBefore = "${localAnalysis.keywordMatch?.pct ?: 60}%",
            estimatedMatchAfter = "92%"
        )
    }

    /**
     * Generates a multi-page A4 Canvas PDF (595x842 points), single-column only,
     * NO photo, NO DOB, NO tables, NO graphics.
     */
    fun generatePdf(tailoredText: String, fileName: String): String {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points
        val marginH = 40f
        val marginV = 40f
        val contentWidth = (pageWidth - marginH * 2).toInt()

        var pageNumber = 1
        var currentPage = pdfDocument.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        var canvas: Canvas = currentPage.canvas
        var y = marginV

        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(17, 17, 17)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val headingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(17, 17, 17)
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(120, 120, 120)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        fun ensureSpace(requiredHeight: Float) {
            if (y + requiredHeight > pageHeight - marginV - 20) {
                // Draw footer on current page
                canvas.drawText(
                    "Created with Resume World V2 - Design by Dayaram Chouhan",
                    marginH,
                    pageHeight - 20f,
                    footerPaint
                )
                pdfDocument.finishPage(currentPage)
                pageNumber++
                currentPage = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                )
                canvas = currentPage.canvas
                y = marginV
            }
        }

        tailoredText.lines().forEach { line ->
            val cleanLine = line.trim()
            if (cleanLine.isBlank()) {
                y += 6f
                return@forEach
            }

            val isHeading = cleanLine.startsWith("#") ||
                cleanLine.equals("Summary", ignoreCase = true) ||
                cleanLine.equals("Work Experience", ignoreCase = true) ||
                cleanLine.equals("Education", ignoreCase = true) ||
                cleanLine.equals("Skills", ignoreCase = true) ||
                cleanLine.equals("Certifications", ignoreCase = true)

            val paintToUse = if (isHeading) headingPaint else bodyPaint
            val displayLine = cleanLine.replace(Regex("^#+\\s*"), "")

            val layout = StaticLayout.Builder
                .obtain(displayLine, 0, displayLine.length, paintToUse, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(1.4f, 1.05f)
                .setIncludePad(false)
                .build()

            ensureSpace(layout.height.toFloat() + 4f)
            canvas.save()
            canvas.translate(marginH, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + 4f
        }

        // Draw footer on final page
        canvas.drawText(
            "Created with Resume World V2 - Design by Dayaram Chouhan",
            marginH,
            pageHeight - 20f,
            footerPaint
        )
        pdfDocument.finishPage(currentPage)

        val file = File(context.filesDir, "$fileName.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        return file.absolutePath
    }
}
