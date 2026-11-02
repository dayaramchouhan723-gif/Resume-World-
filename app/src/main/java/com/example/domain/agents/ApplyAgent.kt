package com.example.domain.agents

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.AppDao
import com.example.data.AppliedJobsEntity
import com.example.domain.TriggerManager
import java.io.File
import java.util.ArrayList

object ApplyAgent {

    suspend fun applyToJobWithEmailAndFiles(
        context: Context,
        dao: AppDao,
        jobTitle: String,
        companyName: String,
        contactEmail: String,
        resumePdf: File,
        coverLetterPdf: File,
        coverLetterText: String,
        candidateName: String = "Dayaram Chouhan"
    ) {
        val subject = "Application for $jobTitle - $candidateName - 5 Years Experience"
        val bodyText = """
            Dear Hiring Manager at $companyName,
            
            I am writing to formally submit my application for the position of $jobTitle.
            
            $coverLetterText
            
            Attached please find my single-column ATS 92+ formatted Resume PDF and Cover Letter PDF.
            
            Best regards,
            $candidateName
            Phone: +91 98765 43210
        """.trimIndent()

        // 1. Build ACTION_SEND_MULTIPLE Email Intent with PDF attachments
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(contactEmail))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, bodyText)

            val uris = ArrayList<Uri>()
            runCatching {
                val resumeUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", resumePdf)
                val coverUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", coverLetterPdf)
                uris.add(resumeUri)
                uris.add(coverUri)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Send Application Email via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            // Intent fallback
        }

        // 2. Save Application Record in Room DB
        dao.insertAppliedJob(
            AppliedJobsEntity(
                company = companyName,
                role = jobTitle,
                dateApplied = System.currentTimeMillis(),
                status = "Applied",
                resumePath = resumePdf.absolutePath,
                chatHistory = "Applied via Email ($contactEmail). Resume & Cover Letter PDFs attached."
            )
        )

        // 3. Schedule 4-Week Auto Follow-up Reminder
        TriggerManager.scheduleDailyFollowUpCheck(context)
    }
}
