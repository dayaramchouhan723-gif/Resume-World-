package com.example.domain

import com.example.data.SharedPrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ScamLevel {
    SAFE,
    WARNING,
    SCAM
}

data class ScamCheckResult(
    val level: ScamLevel,
    val title: String,
    val explanation: String
)

data class CountryFormatConfig(
    val countryName: String,
    val maxPages: String,
    val photoRecommended: Boolean,
    val keyFocus: String,
    val sampleTip: String
)

object SanamMultiAgentEngine {

    // AGENT 1: Job Hunter Agent
    suspend fun runJobHunterAgent(
        query: String,
        country: String,
        jobSearchService: JobSearchService
    ): List<Job> = withContext(Dispatchers.IO) {
        val rawJobs = jobSearchService.searchJobs(query, country)
        rawJobs.map { job ->
            val scamResult = runScamDetectorAgent(job.title, job.company, job.description, job.applyLink)
            job.copy(
                sponsorshipStatus = if (scamResult.level == ScamLevel.SCAM) {
                    "⚠️ SCAM WARNING: Fee Requested"
                } else job.sponsorshipStatus
            )
        }
    }

    // AGENT 2: Resume Tailor Agent (Country-Wise Format)
    fun getCountryFormatConfig(country: String): CountryFormatConfig {
        val c = country.lowercase()
        return when {
            c.contains("canada") || c.contains("ca") -> CountryFormatConfig(
                countryName = "Canada 🇨🇦",
                maxPages = "1 Page Strictly",
                photoRecommended = false,
                keyFocus = "Single-column, LMIA alignment, NO photo/DOB/marital status",
                sampleTip = "Canadian employers filter by single-column text ATS order."
            )
            c.contains("australia") || c.contains("au") -> CountryFormatConfig(
                countryName = "Australia 🇦🇺",
                maxPages = "2 Pages Allowed",
                photoRecommended = true,
                keyFocus = "ANZSCO skills matrix, photo optional, key competencies list",
                sampleTip = "Australian resumes emphasize technical competencies and ANZSCO skill levels."
            )
            c.contains("zealand") || c.contains("nz") -> CountryFormatConfig(
                countryName = "New Zealand 🇳🇿",
                maxPages = "1.5 Pages",
                photoRecommended = false,
                keyFocus = "Accredited employer suitability, references section required",
                sampleTip = "New Zealand employers expect 2 verified referee contacts."
            )
            else -> CountryFormatConfig(
                countryName = "Global Standard",
                maxPages = "1 to 2 Pages",
                photoRecommended = false,
                keyFocus = "Clean single-column formatting",
                sampleTip = "Universal ATS-optimized layout."
            )
        }
    }

    // AGENT 3: Cover Letter Agent
    suspend fun runCoverLetterAgent(
        apiKey: String,
        isFreeMode: Boolean,
        userVoiceInput: String,
        jobTitle: String,
        companyName: String,
        country: String
    ): String = withContext(Dispatchers.Default) {
        if (!isFreeMode && apiKey.isNotBlank()) {
            val prompt = "Create a 100-word professional cover letter for $jobTitle at $companyName ($country). Candidate spoken input: '$userVoiceInput'."
            val result = GeminiRewriteService.rewriteExperienceToEnglishBullets(apiKey, "Cover Letter", prompt)
            result.getOrNull() ?: generateLocalCoverLetter(userVoiceInput, jobTitle, companyName, country)
        } else {
            generateLocalCoverLetter(userVoiceInput, jobTitle, companyName, country)
        }
    }

    private fun generateLocalCoverLetter(
        voiceInput: String,
        jobTitle: String,
        companyName: String,
        country: String
    ): String {
        val rawInput = voiceInput.ifBlank { "5 years of hands-on experience in farm work, machinery, and team operations" }
        return """
            Dear Hiring Manager,
            
            I am writing to express my enthusiastic interest in the $jobTitle position at $companyName in $country.
            
            My background includes: $rawInput. I take pride in workplace safety, reliability, and maintaining high productivity standards in fast-paced environments.
            
            I am eager to bring my work ethic and practical skills to $companyName. Thank you for considering my application.
            
            Sincerely,
            Candidate
        """.trimIndent()
    }

    // AGENT 4: Scam Detector Agent
    fun runScamDetectorAgent(
        jobTitle: String,
        companyName: String,
        description: String,
        link: String
    ): ScamCheckResult {
        val text = "$jobTitle $description $companyName $link".lowercase()

        return when {
            text.contains("send money") || text.contains("payment required") || text.contains("processing fee") || text.contains("pay $") -> ScamCheckResult(
                level = ScamLevel.SCAM,
                title = "Paise maang raha hai, Scam!",
                explanation = "Koi bhi asli employer visa ya interview ke paise nahi maangta. Beware!"
            )
            text.contains("whatsapp only") || text.contains("telegram") || text.contains("western union") -> ScamCheckResult(
                level = ScamLevel.WARNING,
                title = "Suspicious Contact Channel",
                explanation = "Direct WhatsApp/Telegram recruitment without official company website is risky."
            )
            text.contains("lmia") || text.contains("jobbank.gc.ca") || text.contains("adzuna") -> ScamCheckResult(
                level = ScamLevel.SAFE,
                title = "Safe hai • Verified Employer",
                explanation = "Official job source with standard recruitment procedures."
            )
            else -> ScamCheckResult(
                level = ScamLevel.SAFE,
                title = "Standard Job Listing",
                explanation = "Standard employer advertisement. Never pay any fee upfront."
            )
        }
    }

    // AGENT 5: Interview Practice Agent
    fun getInterviewQuestionsForCountry(country: String): List<String> = when {
        country.contains("Canada", ignoreCase = true) -> listOf(
            "Tell me about your experience working outdoors in Canadian winter conditions.",
            "How do you ensure WHMIS and workplace safety guidelines on farm/construction sites?",
            "Why do you want to move to Canada under LMIA/work permit?"
        )
        country.contains("Australia", ignoreCase = true) -> listOf(
            "What technical skills do you have according to ANZSCO trade standards?",
            "How do you handle heavy equipment operation in Australian summer heat?",
            "Do you hold any relevant tickets or white card certifications?"
        )
        else -> listOf(
            "Tell me about yourself and your practical working experience.",
            "Can you provide two professional references from your previous employers?",
            "How do you handle team tasks under high productivity targets?"
        )
    }
}
