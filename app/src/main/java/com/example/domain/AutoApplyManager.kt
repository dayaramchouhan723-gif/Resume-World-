package com.example.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.example.data.AppDao
import com.example.data.AppliedJobsEntity

class AutoApplyManager(
    private val context: Context,
    private val dao: AppDao
) {

    fun prepareApplication(job: Job): Intent {
        if (!job.applyEmail.isNullOrBlank()) {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${job.applyEmail}")
                putExtra(Intent.EXTRA_SUBJECT, "Application for ${job.title} - ${job.company}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Dear Hiring Team at ${job.company},\n\nI am writing to express my strong interest in the ${job.title} position.\n\nPlease find my resume attached.\n\nBest regards,\nCandidate"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return emailIntent
        }

        val builder = CustomTabsIntent.Builder()
        builder.setToolbarColor(android.graphics.Color.parseColor("#151526"))
        val customTabsIntent = builder.build()
        customTabsIntent.intent.data = Uri.parse(job.applyLink)
        customTabsIntent.intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        return customTabsIntent.intent
    }

    suspend fun logApplication(job: Job, resumePath: String = "") {
        val entity = AppliedJobsEntity(
            company = job.company,
            role = job.title,
            dateApplied = System.currentTimeMillis(),
            status = "Applied",
            resumePath = resumePath,
            jobLink = job.applyLink
        )
        dao.insertAppliedJob(entity)
    }

    companion object {
        suspend fun applyToJob(
            context: Context,
            job: Job,
            dao: AppDao,
            candidateName: String = "Candidate",
            candidateResumeText: String = ""
        ) {
            val manager = AutoApplyManager(context, dao)
            val intent = manager.prepareApplication(job)
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(job.applyLink)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
            }
            manager.logApplication(job)
        }
    }
}
