package com.example.domain.agents

data class ExtractedJobRequirements(
    val title: String,
    val company: String,
    val requiredSkills: List<String>,
    val minExperienceYears: Int,
    val requiredEducation: String,
    val contactEmail: String,
    val topKeywords: List<String>
)

object DescriptionReaderAgent {

    fun analyzeDescription(
        title: String,
        company: String,
        description: String,
        contactEmail: String = ""
    ): ExtractedJobRequirements {
        val lower = description.lowercase()

        val keywords = mutableListOf<String>()
        val possibleKeywords = listOf(
            "whmis", "tractor", "irrigation", "greenhouse", "harvesting",
            "first aid", "forklift", "csts", "scaffolding", "rebar",
            "concrete pouring", "pos system", "inventory", "customer service",
            "g-class license", "red seal", "ohsa", "heavy machinery"
        )

        for (kw in possibleKeywords) {
            if (lower.contains(kw)) {
                keywords.add(kw.replaceFirstChar { it.uppercase() })
            }
        }

        if (keywords.isEmpty()) {
            keywords.addAll(listOf("Equipment Operation", "Safety Compliance", "Team Coordination"))
        }

        val years = when {
            lower.contains("5 years") || lower.contains("5+ years") -> 5
            lower.contains("3 years") || lower.contains("3+ years") -> 3
            lower.contains("2 years") || lower.contains("2+ years") -> 2
            else -> 1
        }

        val emailMatch = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}").find(description)?.value
            ?: contactEmail.ifBlank { "hr@${company.lowercase().replace(" ", "")}.com" }

        return ExtractedJobRequirements(
            title = title,
            company = company,
            requiredSkills = keywords,
            minExperienceYears = years,
            requiredEducation = if (lower.contains("degree") || lower.contains("bachelor")) "Bachelor's Degree / Diploma" else "High School / Trade Certificate",
            contactEmail = emailMatch,
            topKeywords = keywords
        )
    }
}
