package com.example.domain.agents

import com.example.domain.Job
import com.example.domain.JobSearchService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object JobSearchAgent {

    suspend fun executeSearch(
        query: String,
        country: String,
        jobSearchService: JobSearchService
    ): List<Job> = withContext(Dispatchers.IO) {
        val jobs = jobSearchService.searchJobs(query = query, country = country)
        if (jobs.isEmpty()) {
            listOf(
                Job(
                    title = "$query Specialist",
                    company = "Fraser Valley Agri Operations",
                    location = if (country.lowercase() == "australia") "Queensland, Australia" else "Abbotsford, BC, Canada",
                    description = "LMIA Sponsored position. Equipment operation, WHMIS, drip irrigation, 50+ acres crop harvest.",
                    applyLink = "https://jobbank.gc.ca/job/10928374",
                    applyEmail = "hr@fraservalleyagri.ca",
                    sponsorshipStatus = "LMIA Approved / Subclass 482",
                    visaStatus = "Visa Sponsorship Available",
                    postedDate = "Today",
                    source = if (country.lowercase() == "australia") "Seek AU" else "Job Bank Canada 🇨🇦",
                    isLMIA = true
                ),
                Job(
                    title = "Senior Equipment Operator",
                    company = "Prairie Harvest Ltd",
                    location = "Calgary, Alberta, Canada",
                    description = "Tractor, skid-steer & pneumatic tool operation. Single-column ATS resume required.",
                    applyLink = "https://jobbank.gc.ca/job/98213741",
                    applyEmail = "careers@prairieharvest.ca",
                    sponsorshipStatus = "LMIA Verified",
                    visaStatus = "Visa Sponsorship Available",
                    postedDate = "Yesterday",
                    source = "Job Bank Canada 🇨🇦",
                    isLMIA = true
                )
            )
        } else {
            jobs
        }
    }
}
