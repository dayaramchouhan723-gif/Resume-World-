package com.example

import com.example.data.CanadianResumeExamples
import com.example.domain.AtsAnalyzer
import com.example.domain.PhonePcController
import com.example.domain.SanamMultiAgentEngine
import com.example.domain.ScamLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SanamLogicUnitTest {

    @Test
    fun multiAgent_scamDetectorIdentifiesScamKeywords() {
        val scamJob = SanamMultiAgentEngine.runScamDetectorAgent(
            jobTitle = "Farm Worker",
            companyName = "Fake Recruiting Co",
            description = "Send payment required $200 for visa processing fee to WhatsApp",
            link = "http://fakejob.com"
        )
        assertEquals(ScamLevel.SCAM, scamJob.level)
        assertTrue(scamJob.title.contains("Paise maang raha hai"))
    }

    @Test
    fun multiAgent_countryFormatsMatchRules() {
        val canadaCfg = SanamMultiAgentEngine.getCountryFormatConfig("Canada")
        assertEquals("Canada 🇨🇦", canadaCfg.countryName)
        assertEquals(false, canadaCfg.photoRecommended)

        val ausCfg = SanamMultiAgentEngine.getCountryFormatConfig("Australia")
        assertEquals("Australia 🇦🇺", ausCfg.countryName)
        assertEquals(true, ausCfg.photoRecommended)
    }

    @Test
    fun atsAnalyzer_verifiesSampleData() {
        for (example in CanadianResumeExamples.examples) {
            val plain = example.formState.toPlainText()
            val result = AtsAnalyzer.analyze(plain, example.sampleJobDescription)
            assertTrue("Expected score >= 75 for ${example.label}", result.totalScore >= 75)
            assertNotNull(result.keywordMatch)
        }
    }

    @Test
    fun atsScorerAgent_verifiesEmptyVsFilledProfileScore() {
        val dummyReqs = com.example.domain.agents.ExtractedJobRequirements(
            title = "Canada Farm Worker",
            company = "Agri Farms Inc",
            requiredSkills = listOf("Equipment operation", "Tractor driving", "WHMIS", "Crop harvesting"),
            minExperienceYears = 3,
            requiredEducation = "Trade Certificate",
            contactEmail = "hr@agrifarms.com",
            topKeywords = listOf("Equipment operation", "Tractor driving", "WHMIS")
        )

        // 1. Empty Profile -> Score 45 & Requires Profile
        val emptyProfileResult = com.example.domain.agents.ATSScorerAgent.calculateAtsMatch(
            userProfile = null,
            jobRequirements = dummyReqs
        )
        assertEquals(45, emptyProfileResult.currentScore)
        assertTrue(emptyProfileResult.requiresProfile)

        // 2. Filled Profile -> Score 78-85 & Match Calculated
        val filledProfile = com.example.data.UserProfile(
            name = "Dayaram Chouhan",
            experience = "3 years Farm Worker in Punjab",
            skills = "Equipment operation, Tractor driving, WHMIS"
        )
        val filledResult = com.example.domain.agents.ATSScorerAgent.calculateAtsMatch(
            userProfile = filledProfile,
            jobRequirements = dummyReqs
        )
        assertEquals(false, filledResult.requiresProfile)
        assertTrue("Score should increase to 78+", filledResult.currentScore >= 78)
        assertEquals(94, filledResult.tailoredScore)
    }
}
