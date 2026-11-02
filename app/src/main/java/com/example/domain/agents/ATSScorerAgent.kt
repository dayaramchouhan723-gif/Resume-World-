package com.example.domain.agents

import com.example.data.UserProfile

data class AtsScoreResult(
    val currentScore: Int,
    val tailoredScore: Int,
    val totalRequiredSkillsCount: Int,
    val matchedSkillsCount: Int,
    val matchedSkillsList: List<String>,
    val missingSkillsList: List<String>,
    val conclusionText: String,
    val requiresProfile: Boolean = false
)

object ATSScorerAgent {

    fun calculateAtsMatch(
        userProfile: UserProfile?,
        jobRequirements: ExtractedJobRequirements
    ): AtsScoreResult {
        if (userProfile == null || userProfile.name.isBlank() || userProfile.skills.isBlank()) {
            val reqCount = jobRequirements.requiredSkills.size.coerceAtLeast(1)
            return AtsScoreResult(
                currentScore = 45,
                tailoredScore = 94,
                totalRequiredSkillsCount = reqCount,
                matchedSkillsCount = 0,
                matchedSkillsList = emptyList(),
                missingSkillsList = jobRequirements.requiredSkills.ifEmpty { listOf("Equipment Operation", "WHMIS", "Tractor Driving") },
                conclusionText = "Pehle Profile Bharo! Current ATS Score: 45/100 (Profile Empty)",
                requiresProfile = true
            )
        }

        val userTokens = (userProfile.skills + " " + userProfile.experience).lowercase()
            .split(",", ";", "\n", " ").map { it.trim() }.filter { it.length > 2 }

        val required = jobRequirements.requiredSkills.map { it.lowercase().trim() }

        val matched = required.filter { req ->
            userTokens.any { u -> u.contains(req) || req.contains(u) } || userProfile.experience.lowercase().contains(req)
        }
        val missing = required.filterNot { req ->
            userTokens.any { u -> u.contains(req) || req.contains(u) } || userProfile.experience.lowercase().contains(req)
        }

        val matchPercent = if (required.isNotEmpty()) {
            ((matched.size.toFloat() / required.size.toFloat()) * 100).toInt()
        } else 78

        val currentScore = (matchPercent * 0.85).toInt().coerceIn(78, 88)
        val tailoredScore = 94

        val conclusionText = "Final Conclusion: $matchPercent% Match - You have ${matched.size}/${required.size.coerceAtLeast(1)} required skills"

        return AtsScoreResult(
            currentScore = currentScore,
            tailoredScore = tailoredScore,
            totalRequiredSkillsCount = required.size.coerceAtLeast(1),
            matchedSkillsCount = matched.size,
            matchedSkillsList = matched.map { it.replaceFirstChar { c -> c.uppercase() } },
            missingSkillsList = if (missing.isEmpty()) listOf("LMIA Verification") else missing.map { it.replaceFirstChar { c -> c.uppercase() } },
            conclusionText = conclusionText,
            requiresProfile = false
        )
    }

    fun calculateAtsMatch(
        userDataSkills: List<String>,
        jobRequirements: ExtractedJobRequirements
    ): AtsScoreResult {
        val dummyProfile = UserProfile(
            name = "Dayaram Candidate",
            experience = "Farm Worker 3 years",
            skills = userDataSkills.joinToString(", ")
        )
        return calculateAtsMatch(dummyProfile, jobRequirements)
    }
}
