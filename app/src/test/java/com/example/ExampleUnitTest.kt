package com.example

import com.example.data.CanadianResumeExamples
import com.example.domain.AtsAnalyzer
import com.example.domain.CheckLevel
import com.example.domain.GeminiRewriteService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun atsAnalyzer_scoresStrongCanadianExamplesAndFormatsHonestHeadline() {
        for (example in CanadianResumeExamples.examples) {
            val plain = example.formState.toPlainText()
            val result = AtsAnalyzer.analyze(plain, example.sampleJobDescription)
            assertTrue("Expected strong score for ${example.label}, got ${result.totalScore}", result.totalScore >= 75)
            assertEquals("Strong", result.bandLabel)
            assertNotNull(result.keywordMatch)
            assertTrue(
                "Headline must show Estimate only, not real ATS",
                result.keywordMatchHeadline.contains("Estimate only, not real ATS")
            )
            assertTrue("Expected 30+ formatting & readability checks", result.formattingChecklist.size >= 30)
        }
    }

    @Test
    fun atsAnalyzer_flagsTablesSkillBarsDobMaritalAndPhoto() {
        val flawedResume = """
            John Doe
            Photo attached | Date of Birth: 1995-04-12 | Marital Status: Single | Religion: None
            Skill1 | Skill2 | Skill3 | Skill4 | Skill5 | Skill6 | Skill7 | Skill8 | Skill9 | Skill10 | Skill11 | Skill12
            Forklift ████████ 80% proficiency (4/5)
            Page 1 of 2
        """.trimIndent()
        val result = AtsAnalyzer.analyze(flawedResume, "")
        assertTrue(result.totalScore < 50)
        assertTrue(result.allChecks.any { it.level != CheckLevel.OK && it.message.contains("Tables or columns") })
        assertTrue(result.allChecks.any { it.level != CheckLevel.OK && it.message.contains("Skill bars") })
        assertTrue(result.allChecks.any { it.level != CheckLevel.OK && it.message.contains("date of birth") })
        assertTrue(result.allChecks.any { it.level != CheckLevel.OK && it.message.contains("marital status") })
        assertTrue(result.allChecks.any { it.level != CheckLevel.OK && it.message.contains("photo") })
    }

    @Test
    fun geminiSanitizer_stripsInventedCertificatesNotInUserInput() {
        val userRomanUrdu = "Main ne warehouse mein 2 saal forklift chalaya aur rozana 50 pallets load kiye"
        val hallucinatedAiOutput = """
            - Operated a forklift in a warehouse for 2 years, loading 50 pallets daily.
            - Worked safely with WHMIS and Red Seal certification and willing to relocate.
        """.trimIndent()

        val sanitized = GeminiRewriteService.sanitizeAndEnforceNoInventedClaims(
            userOriginal = userRomanUrdu,
            aiOutput = hallucinatedAiOutput
        )
        assertFalse("Must strip WHMIS when user did not type it", sanitized.lowercase().contains("whmis"))
        assertFalse("Must strip Red Seal when user did not type it", sanitized.lowercase().contains("red seal"))
        assertFalse("Must strip willing to relocate when user did not type it", sanitized.lowercase().contains("willing to relocate"))
        assertTrue("Must keep user's real 50 pallets fact", sanitized.contains("50 pallets"))
    }
}
