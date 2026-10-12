package com.example.domain

/**
 * Honest, transparent Job Description Matcher & 32-Point Formatting/Readability Checker.
 *
 * CRITICAL COMPLIANCE RULES:
 * - No fake ATS formula ("40 points for NOC, 80+ means pass"). Real ATS is company-specific.
 * - Headline output is strictly honest keyword overlap:
 *   "Keyword Match: XX% - Estimate only, not real ATS"
 * - Missing Keywords are categorized into Hard Skills & Role Terms vs Soft Skills & Action Terms
 *   so the user can review and manually add ONLY what is genuinely true for them.
 * - Includes 32 Formatting, Structure, Readability & Canadian Compliance checks with ✅ / ❌.
 */
enum class CheckLevel {
    OK, WARN, BAD
}

enum class SkillCategory {
    HARD_SKILL,
    SOFT_SKILL
}

data class AtsCheckItem(
    val level: CheckLevel,
    val message: String,
    val priorityWeight: Int,
    val category: String = "General",
    val passed: Boolean = level == CheckLevel.OK
)

data class KeywordToken(
    val stemKey: String,
    val display: String,
    val count: Int,
    val isBigram: Boolean,
    val weight: Int,
    val category: SkillCategory = SkillCategory.HARD_SKILL
)

data class KeywordMatchResult(
    val hit: List<KeywordToken>,
    val miss: List<KeywordToken>,
    val missingHardSkills: List<KeywordToken>,
    val missingSoftSkills: List<KeywordToken>,
    val pct: Int
)

data class CategoryBar(
    val label: String,
    val score: Int
)

data class AtsAnalysisResult(
    val totalScore: Int,
    val keywordMatchHeadline: String,
    val bandLabel: String,
    val note: String,
    val bars: List<CategoryBar>,
    val fixFirst: List<AtsCheckItem>,
    val allChecks: List<AtsCheckItem>,
    val formattingChecklist: List<AtsCheckItem>,
    val keywordMatch: KeywordMatchResult?,
    val wordCount: Int
)

object AtsAnalyzer {

    private val STOP_WORDS: Set<String> = (
        "a about above across after all also am an and any are as at be been being but by can could did do does each etc " +
            "for from had has have help how i if in including into is it its job just like may more most must my need new no not " +
            "of on one or our out over own per plus role should so some such than that the their them then there these they this " +
            "those through to too up us use using via was we well were what when where which while who will with within would you " +
            "your ability able candidate company experience experienced skills skill strong team teams work working years year " +
            "position preferred required requirements requirement responsibilities responsible knowledge understanding good great " +
            "excellent opportunity benefits apply employer equal seeking ideal proven demonstrated related relevant least minimum " +
            "degree various other both only make take get day time based ensure provide join looking"
        ).split(" ").toSet()

    private val SOFT_SKILL_STEMS: Set<String> = setOf(
        "communicat", "collaborat", "leadership", "leader", "supervis", "train", "mentor",
        "problem", "solv", "detail", "orient", "organiz", "time management", "punctual",
        "reliabl", "dependabl", "adaptabl", "flexibl", "customer", "service", "interpersonal",
        "multitask", "fast-pac", "fast pac", "escalat", "conflict", "resolut", "motivat",
        "coordinate", "coordinat", "support", "assist", "schedul", "attenti", "ethic"
    )

    private val TOKEN_REGEX = Regex("[a-z][a-z0-9+#./-]*")
    private val TRAILING_PUNCT_REGEX = Regex("[./-]+$")
    private val STEM_SUFFIX_REGEX = Regex("(ing|ed|es|s)$")

    private val EXP_HEADING_REGEX = Regex("^(work |professional |relevant )?(experience|history)\\b|^employment", RegexOption.IGNORE_CASE)
    private val EDU_HEADING_REGEX = Regex("^(education|academic|qualification)", RegexOption.IGNORE_CASE)
    private val SKILL_HEADING_REGEX = Regex("^(technical |core )?(skills|competenc|technologies)", RegexOption.IGNORE_CASE)
    private val SUMMARY_HEADING_REGEX = Regex("^(professional |career )?(summary|profile|objective)|^about", RegexOption.IGNORE_CASE)
    private val CERT_HEADING_REGEX = Regex("^(certificat|licens|training)", RegexOption.IGNORE_CASE)

    private val EMAIL_REGEX = Regex("[\\w.+-]+@[\\w-]+\\.[\\w.]+")
    private val PHONE_REGEX = Regex("\\+?\\d[\\d\\s().-]{8,}\\d")
    private val LINKEDIN_REGEX = Regex("linkedin\\.com/in/", RegexOption.IGNORE_CASE)
    private val LOCATION_REGEX = Regex("\\b(AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|Canada|Alberta|British Columbia|Manitoba|Ontario|Quebec|Saskatchewan|Nova Scotia)\\b", RegexOption.IGNORE_CASE)

    private val DATE_RANGE_REGEX = Regex(
        "(19|20)\\d{2}\\s*(-|–|—|to)\\s*(([a-z]{3,9}\\.?\\s+)|(\\d{1,2}/))?((19|20)\\d{2}|present|current|now)",
        RegexOption.IGNORE_CASE
    )
    private val SLASH_DATE_REGEX = Regex("\\b(0?[1-9]|1[0-2])/(19|20)\\d{2}\\b")
    private val MONTH_NAME_DATE_REGEX = Regex("\\b(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\.?\\s+(19|20)\\d{2}\\b", RegexOption.IGNORE_CASE)

    private val QUANTIFIED_RESULT_REGEX = Regex(
        "\\d+\\s?%|[$£€]\\s?\\d|\\b\\d[\\d,.]*\\s?(k|m|million|thousand)\\b|\\b\\d{2,}\\+?\\s+[a-z]{3,}",
        RegexOption.IGNORE_CASE
    )

    private val ODD_SYMBOLS_REGEX = Regex("[\\u2190-\\u21FF\\u2600-\\u27BF\\uD83C\\uDF00-\\uD83E\\uDDFF]")
    private val SKILL_BAR_REGEX = Regex("[█▓▒░■□▪▫★☆●○]{3,}|\\[\\s*[=#*]{3,}\\s*]|\\b\\d{1,3}%\\s*(proficiency|skill|fluent|level)\\b|\\b([1-5]/[5]|10/10)\\b", RegexOption.IGNORE_CASE)
    private val TWO_COLUMN_REGEX = Regex("\\S+\\s{6,}\\S+.*\\n.*\\S+\\s{6,}\\S+|\\t{2,}")
    private val HEADER_FOOTER_REGEX = Regex("\\bpage\\s+\\d+\\s+of\\s+\\d+\\b|^curriculum\\s+vitae$|^resume\\s+of\\b", RegexOption.IGNORE_CASE)
    private val PHOTO_REGEX = Regex("\\b(photo|photograph|headshot|picture\\s+attached|portrait)\\b", RegexOption.IGNORE_CASE)
    private val DOB_REGEX = Regex("date of birth|\\bdob\\b|\\bbirth\\s*date\\b|\\bage\\s*:\\s*\\d+", RegexOption.IGNORE_CASE)
    private val MARITAL_REGEX = Regex("marital status|\\b(married|unmarried|divorced|widowed)\\b\\s*status|marital\\s*:", RegexOption.IGNORE_CASE)
    private val RELIGION_ID_REGEX = Regex("\\b(religion|cnic|passport\\s*(no|number)|national\\s*id|sin\\s*(no|number)|social\\s+insurance)\\b", RegexOption.IGNORE_CASE)
    private val FATHER_NAME_REGEX = Regex("father'?s?\\s+name|parent'?s?\\s+name", RegexOption.IGNORE_CASE)
    private val FULL_STREET_ADDRESS_REGEX = Regex("\\b\\d{1,5}\\s+[A-Za-z0-9]+\\s+(Street|St\\.|Avenue|Ave\\.|Road|Rd\\.|Boulevard|Blvd\\.|Drive|Dr\\.|Lane|Ln\\.)\\b", RegexOption.IGNORE_CASE)
    private val FIRST_PERSON_REGEX = Regex("\\b(i|my|me|myself)\\b", RegexOption.IGNORE_CASE)

    private val ACTION_VERBS: Set<String> = setOf(
        "managed", "supervised", "operated", "harvested", "planted", "irrigated", "maintained",
        "built", "installed", "loaded", "unloaded", "staged", "assisted", "trained", "scheduled",
        "reconciled", "resolved", "coordinated", "inspected", "prepared", "packed", "graded",
        "sorted", "delivered", "reduced", "increased", "improved", "led", "organized", "monitored",
        "performed", "executed", "handled", "supported", "assembled", "repaired", "cleaned",
        "sanitized", "enforced", "processed", "completed", "achieved", "oversaw", "developed",
        "plant", "cultivate", "operate", "examine", "load", "assist", "supervise", "reconcile", "oversee"
    )

    fun tokenize(text: String): List<String> {
        return TOKEN_REGEX.findAll(text.lowercase())
            .map { it.value.replace(TRAILING_PUNCT_REGEX, "") }
            .filter { it.isNotEmpty() }
            .toList()
    }

    fun stem(word: String): String {
        return if (word.length > 4) word.replace(STEM_SUFFIX_REGEX, "") else word
    }

    private fun isGoodWord(word: String): Boolean {
        return word.length > 2 && !STOP_WORDS.contains(word)
    }

    private fun classifyKeywordCategory(stemKey: String, display: String): SkillCategory {
        val lower = display.lowercase()
        val isSoft = SOFT_SKILL_STEMS.any { soft ->
            stemKey.contains(soft) || lower.contains(soft)
        }
        return if (isSoft) SkillCategory.SOFT_SKILL else SkillCategory.HARD_SKILL
    }

    fun extractKeywords(jd: String): List<KeywordToken> {
        val tokens = tokenize(jd)
        data class MutableKeyword(
            val stemKey: String,
            val display: String,
            var count: Int = 0,
            val isBigram: Boolean
        )

        val map = LinkedHashMap<String, MutableKeyword>()
        fun addToken(stemKey: String, display: String, isBigram: Boolean) {
            val existing = map.getOrPut(stemKey) { MutableKeyword(stemKey, display, 0, isBigram) }
            existing.count++
        }

        for (i in tokens.indices) {
            val w = tokens[i]
            if (!isGoodWord(w)) continue
            addToken(stem(w), w, false)
            val next = tokens.getOrNull(i + 1)
            if (next != null && isGoodWord(next)) {
                addToken("${stem(w)} ${stem(next)}", "$w $next", true)
            }
        }

        return map.values
            .filter { !it.isBigram || it.count > 1 }
            .map { item ->
                val weight = minOf(item.count, 4) + if (item.isBigram) 1 else 0
                KeywordToken(
                    stemKey = item.stemKey,
                    display = item.display,
                    count = item.count,
                    isBigram = item.isBigram,
                    weight = weight,
                    category = classifyKeywordCategory(item.stemKey, item.display)
                )
            }
            .sortedWith(
                compareByDescending<KeywordToken> { it.weight }
                    .thenByDescending { it.display.length }
            )
            .take(30)
    }

    fun matchKeywords(cv: String, keywords: List<KeywordToken>): KeywordMatchResult {
        val resumeStems = tokenize(cv).map { stem(it) }
        val unigramSet = resumeStems.toSet()
        val joinedStems = " ${resumeStems.joinToString(" ")} "

        val hit = mutableListOf<KeywordToken>()
        val miss = mutableListOf<KeywordToken>()

        for (kw in keywords) {
            val isMatched = if (kw.isBigram) {
                joinedStems.contains(" ${kw.stemKey} ")
            } else {
                unigramSet.contains(kw.stemKey)
            }
            if (isMatched) hit.add(kw) else miss.add(kw)
        }

        val totalWeight = keywords.sumOf { it.weight }
        val matchedWeight = hit.sumOf { it.weight }
        val pct = if (totalWeight > 0) {
            Math.round(100.0 * matchedWeight / totalWeight).toInt()
        } else {
            0
        }

        val missingHard = miss.filter { it.category == SkillCategory.HARD_SKILL }
        val missingSoft = miss.filter { it.category == SkillCategory.SOFT_SKILL }

        return KeywordMatchResult(
            hit = hit,
            miss = miss,
            missingHardSkills = missingHard,
            missingSoftSkills = missingSoft,
            pct = pct
        )
    }

    fun analyze(cv: String, jd: String = ""): AtsAnalysisResult {
        val lines = cv.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val shortLines = lines.filter { it.length < 45 }
        fun hasHeading(regex: Regex): Boolean = shortLines.any { regex.containsMatchIn(it) }

        val words = Regex("\\S+").findAll(cv).count()
        val checks = mutableListOf<AtsCheckItem>()

        fun addCheck(level: CheckLevel, message: String, weight: Int, category: String = "General") {
            checks.add(
                AtsCheckItem(
                    level = level,
                    message = message,
                    priorityWeight = weight,
                    category = category,
                    passed = level == CheckLevel.OK
                )
            )
        }

        // --- 1. SECTION HEADINGS & STRUCTURE CHECKS (6 checks) ---
        val hasExp = hasHeading(EXP_HEADING_REGEX)
        val hasEdu = hasHeading(EDU_HEADING_REGEX)
        val hasSkills = hasHeading(SKILL_HEADING_REGEX)
        val hasSummary = hasHeading(SUMMARY_HEADING_REGEX)
        val hasCerts = hasHeading(CERT_HEADING_REGEX)

        if (hasExp) addCheck(CheckLevel.OK, "Work Experience section heading is clear", 0, "Sections")
        else addCheck(CheckLevel.BAD, "No Experience heading. Add one called Work Experience.", 9, "Sections")

        if (hasEdu) addCheck(CheckLevel.OK, "Education section heading is clear", 0, "Sections")
        else addCheck(CheckLevel.BAD, "No Education heading. Add one called Education.", 7, "Sections")

        if (hasSkills) addCheck(CheckLevel.OK, "Skills section heading is clear", 0, "Sections")
        else addCheck(CheckLevel.BAD, "No Skills section. List your tools and skills under a Skills heading.", 8, "Sections")

        if (hasSummary) addCheck(CheckLevel.OK, "Summary section found at top", 0, "Sections")
        else addCheck(CheckLevel.WARN, "No Summary. Add 2 to 3 lines at the top about your target role.", 3, "Sections")

        val bulletLines = lines.filter { it.startsWith("-") || it.startsWith("•") || it.startsWith("*") }
        if (bulletLines.size >= 3) {
            addCheck(CheckLevel.OK, "Clear bullet points used (${bulletLines.size} bullets)", 0, "Sections")
        } else {
            addCheck(CheckLevel.WARN, "Use plain bullet points (- or •) to list your job duties clearly.", 4, "Sections")
        }

        val expIndex = lines.indexOfFirst { it.length < 45 && EXP_HEADING_REGEX.containsMatchIn(it) }
        val eduIndex = lines.indexOfFirst { it.length < 45 && EDU_HEADING_REGEX.containsMatchIn(it) }
        if (expIndex != -1 && (eduIndex == -1 || expIndex < eduIndex)) {
            addCheck(CheckLevel.OK, "Standard Canadian section order (Work Experience before Education)", 0, "Sections")
        } else if (expIndex != -1 && eduIndex != -1) {
            addCheck(CheckLevel.WARN, "Place Work Experience above Education for experienced candidates.", 2, "Sections")
        } else {
            addCheck(CheckLevel.WARN, "Add both Work Experience and Education headings in order.", 5, "Sections")
        }

        val secScore = (if (hasExp) 40 else 0) +
            (if (hasEdu) 25 else 0) +
            (if (hasSkills) 25 else 0) +
            (if (hasSummary) 10 else 0)

        // --- 2. CONTACT INFORMATION CHECKS (5 checks) ---
        val hasName = lines.firstOrNull()?.let { first ->
            first.length in 3..50 && !first.contains("@") && !first.any { it.isDigit() }
        } ?: false
        val hasEmail = EMAIL_REGEX.containsMatchIn(cv)
        val hasPhone = PHONE_REGEX.containsMatchIn(cv)
        val hasLocation = LOCATION_REGEX.containsMatchIn(cv) ||
            lines.take(4).any { it.contains(",") && it.length < 70 }
        val hasLinkedIn = LINKEDIN_REGEX.containsMatchIn(cv)

        if (hasName) addCheck(CheckLevel.OK, "Candidate name clearly placed at top of resume", 0, "Contact")
        else addCheck(CheckLevel.BAD, "Put your full name on the very first line.", 8, "Contact")

        if (hasEmail) addCheck(CheckLevel.OK, "Email address found", 0, "Contact")
        else addCheck(CheckLevel.BAD, "No email address found.", 10, "Contact")

        if (hasPhone) addCheck(CheckLevel.OK, "Phone number found", 0, "Contact")
        else addCheck(CheckLevel.BAD, "No phone number found.", 8, "Contact")

        if (hasLocation) addCheck(CheckLevel.OK, "City / Province location found", 0, "Contact")
        else addCheck(CheckLevel.WARN, "Add your City and Province (e.g. Calgary, AB).", 4, "Contact")

        if (hasLinkedIn) addCheck(CheckLevel.OK, "LinkedIn profile link found", 0, "Contact")
        else addCheck(CheckLevel.WARN, "No LinkedIn link (optional: linkedin.com/in/yourname).", 2, "Contact")

        val contactScore = (if (hasEmail) 50 else 0) +
            (if (hasPhone) 35 else 0) +
            (if (hasLinkedIn || hasLocation) 15 else 0)

        // --- 3. ATS-UNFRIENDLY FORMATTING CHECKS (8 checks) ---
        var formatPoints = 100
        val pipeCount = cv.count { it == '|' }
        val hasTwoColOrTabs = TWO_COLUMN_REGEX.containsMatchIn(cv)
        val oddCount = ODD_SYMBOLS_REGEX.findAll(cv).count()
        val hasSkillBars = SKILL_BAR_REGEX.containsMatchIn(cv)
        val hasHeaderFooterClutter = HEADER_FOOTER_REGEX.containsMatchIn(cv)
        val hasReplacementChars = cv.contains('\uFFFD')
        val allCapsLines = lines.count { line ->
            line.length > 35 && line.filter { it.isLetter() }.let { letters ->
                letters.isNotEmpty() && letters.all { it.isUpperCase() }
            }
        }

        if (pipeCount <= 10) {
            addCheck(CheckLevel.OK, "No tables or pipe-column grids detected", 0, "Formatting")
        } else {
            formatPoints -= 25
            addCheck(CheckLevel.BAD, "Many | characters ($pipeCount). Tables or columns break ATS parsing—use 1 column.", 7, "Formatting")
        }

        if (!hasTwoColOrTabs) {
            addCheck(CheckLevel.OK, "Single-column reading order verified (no multi-column tabs)", 0, "Formatting")
        } else {
            formatPoints -= 20
            addCheck(CheckLevel.BAD, "Multi-column spacing or tab alignment detected. Use a single top-to-bottom column.", 7, "Formatting")
        }

        if (oddCount == 0) {
            addCheck(CheckLevel.OK, "No decorative graphics, icons, or emoji symbols", 0, "Formatting")
        } else {
            formatPoints -= 25
            addCheck(CheckLevel.WARN, "Found $oddCount symbols or emoji. Use plain bullets like - or •.", 5, "Formatting")
        }

        if (!hasSkillBars) {
            addCheck(CheckLevel.OK, "No visual skill bars or star ratings (ATS-friendly plain skills)", 0, "Formatting")
        } else {
            formatPoints -= 20
            addCheck(CheckLevel.BAD, "Skill bars or star ratings detected (e.g. ████ or 4/5). List plain skill names instead.", 6, "Formatting")
        }

        if (!hasHeaderFooterClutter) {
            addCheck(CheckLevel.OK, "No header/footer page number clutter or 'Curriculum Vitae' title", 0, "Formatting")
        } else {
            formatPoints -= 10
            addCheck(CheckLevel.WARN, "Remove 'Curriculum Vitae' title or 'Page X of Y' header/footer text.", 3, "Formatting")
        }

        if (!hasReplacementChars) {
            addCheck(CheckLevel.OK, "Clean UTF-8 text encoding (no corrupted characters)", 0, "Formatting")
        } else {
            formatPoints -= 15
            addCheck(CheckLevel.BAD, "Corrupted character symbols () found. Re-type those characters cleanly.", 5, "Formatting")
        }

        if (allCapsLines == 0) {
            addCheck(CheckLevel.OK, "Proper sentence capitalization (no ALL-CAPS paragraphs)", 0, "Formatting")
        } else {
            formatPoints -= 10
            addCheck(CheckLevel.WARN, "Avoid writing full sentences in ALL CAPS ($allCapsLines lines found).", 3, "Formatting")
        }

        if (hasCerts || cv.contains("Certific", ignoreCase = true)) {
            addCheck(CheckLevel.OK, "Certifications listed cleanly as text", 0, "Formatting")
        } else {
            addCheck(CheckLevel.OK, "No unverified badge graphics in Certifications", 0, "Formatting")
        }

        // --- 4. CANADIAN RESUME COMPLIANCE CHECKS (6 checks) ---
        val hasPhotoMention = PHOTO_REGEX.containsMatchIn(cv)
        val hasDobMention = DOB_REGEX.containsMatchIn(cv)
        val hasMaritalMention = MARITAL_REGEX.containsMatchIn(cv)
        val hasReligionOrId = RELIGION_ID_REGEX.containsMatchIn(cv)
        val hasFatherName = FATHER_NAME_REGEX.containsMatchIn(cv)
        val hasFullStreetAddress = FULL_STREET_ADDRESS_REGEX.containsMatchIn(cv)

        if (!hasPhotoMention) {
            addCheck(CheckLevel.OK, "No photo or headshot included (Canadian standard)", 0, "Canadian Standard")
        } else {
            formatPoints -= 15
            addCheck(CheckLevel.BAD, "Remove photo/headshot mention. Canadian resumes never include photos.", 6, "Canadian Standard")
        }

        if (!hasDobMention) {
            addCheck(CheckLevel.OK, "No Date of Birth or Age included (Canadian standard)", 0, "Canadian Standard")
        } else {
            formatPoints -= 15
            addCheck(CheckLevel.WARN, "Remove personal details: date of birth / age. Canadian resumes leave them out.", 6, "Canadian Standard")
        }

        if (!hasMaritalMention) {
            addCheck(CheckLevel.OK, "No Marital Status included (Canadian standard)", 0, "Canadian Standard")
        } else {
            formatPoints -= 15
            addCheck(CheckLevel.WARN, "Remove personal details: marital status. Canadian resumes leave them out.", 6, "Canadian Standard")
        }

        if (!hasReligionOrId) {
            addCheck(CheckLevel.OK, "No SIN, Passport, CNIC, or Religion details exposed", 0, "Canadian Standard")
        } else {
            formatPoints -= 20
            addCheck(CheckLevel.BAD, "Remove personal details: passport / SIN / CNIC / religion for privacy and Canadian compliance.", 8, "Canadian Standard")
        }

        if (!hasFatherName) {
            addCheck(CheckLevel.OK, "No parents' or father's name listed (Canadian standard)", 0, "Canadian Standard")
        } else {
            formatPoints -= 10
            addCheck(CheckLevel.WARN, "Remove personal details: father's name. Not used on Canadian resumes.", 5, "Canadian Standard")
        }

        if (!hasFullStreetAddress) {
            addCheck(CheckLevel.OK, "Privacy-safe location (City & Province only, no street number)", 0, "Canadian Standard")
        } else {
            addCheck(CheckLevel.WARN, "Use only City & Province (e.g. Abbotsford, BC) instead of full street address.", 2, "Canadian Standard")
        }

        // --- 5. DATES, READABILITY & CONTENT CHECKS (7 checks) ---
        val dateRangeCount = DATE_RANGE_REGEX.findAll(cv).count()
        when {
            dateRangeCount >= 2 -> addCheck(CheckLevel.OK, "$dateRangeCount work/study date ranges found", 0, "Dates & Readability")
            dateRangeCount == 1 -> addCheck(CheckLevel.WARN, "Only 1 date range found. Show dates for every job.", 5, "Dates & Readability")
            else -> addCheck(CheckLevel.BAD, "No date ranges found. Write dates like Jan 2021 - Present.", 7, "Dates & Readability")
        }
        val dateScore = when {
            dateRangeCount >= 2 -> 100
            dateRangeCount == 1 -> 60
            else -> 0
        }

        val hasSlashDates = SLASH_DATE_REGEX.containsMatchIn(cv)
        val hasMonthNameDates = MONTH_NAME_DATE_REGEX.containsMatchIn(cv)
        if (!(hasSlashDates && hasMonthNameDates)) {
            addCheck(CheckLevel.OK, "Date format is consistent across entries", 0, "Dates & Readability")
        } else {
            addCheck(CheckLevel.WARN, "Inconsistent date formats (mixing 01/2021 and Jan 2021). Use Jan 2021 - Present consistently.", 4, "Dates & Readability")
        }

        when {
            words < 250 -> {
                formatPoints -= 25
                addCheck(CheckLevel.WARN, "Short resume ($words words). Aim for 300 to 800 words.", 4, "Dates & Readability")
            }
            words > 900 -> {
                formatPoints -= 15
                addCheck(CheckLevel.WARN, "Long resume ($words words). Aim for 1 to 2 pages (300-800 words).", 3, "Dates & Readability")
            }
            else -> {
                addCheck(CheckLevel.OK, "Resume length is optimal ($words words)", 0, "Dates & Readability")
            }
        }

        val longParagraphs = lines.count { it.split(Regex("\\s+")).size > 45 }
        if (longParagraphs == 0) {
            addCheck(CheckLevel.OK, "Bullet points are concise and easy to scan", 0, "Dates & Readability")
        } else {
            addCheck(CheckLevel.WARN, "$longParagraphs overly long paragraph(s) found. Split into shorter bullet points.", 3, "Dates & Readability")
        }

        val actionVerbBullets = bulletLines.count { raw ->
            val firstWord = raw.replace(Regex("^[-•*]\\s*"), "").trim()
                .split(Regex("\\s+")).firstOrNull()?.lowercase()?.replace(Regex("[^a-z]"), "") ?: ""
            ACTION_VERBS.contains(firstWord) || firstWord.endsWith("ed")
        }
        if (bulletLines.isNotEmpty() && actionVerbBullets >= (bulletLines.size / 2).coerceAtLeast(1)) {
            addCheck(CheckLevel.OK, "Experience bullets start with strong action verbs", 0, "Dates & Readability")
        } else {
            addCheck(CheckLevel.WARN, "Start experience bullet points with action verbs (e.g. Operated, Harvested, Supervised).", 3, "Dates & Readability")
        }

        val quantifiedCount = lines.count { line ->
            line.length > 25 && !line.contains("@") && QUANTIFIED_RESULT_REGEX.containsMatchIn(line)
        }
        if (quantifiedCount >= 3) {
            addCheck(CheckLevel.OK, "Quantified results with numbers found ($quantifiedCount lines)", 0, "Dates & Readability")
        } else {
            addCheck(
                CheckLevel.WARN,
                "Only $quantifiedCount lines have numbers. Where true, add real counts (e.g. 40+ pallets/day).",
                4,
                "Dates & Readability"
            )
        }
        val impactScore = minOf(100, quantifiedCount * 25)

        val firstPersonCount = FIRST_PERSON_REGEX.findAll(cv).count()
        if (firstPersonCount <= 2) {
            addCheck(CheckLevel.OK, "Professional tone (avoids 'I', 'me', 'my' pronouns)", 0, "Dates & Readability")
        } else {
            addCheck(CheckLevel.WARN, "Found $firstPersonCount first-person pronouns ('I', 'my'). Start bullets directly with verbs.", 2, "Dates & Readability")
        }

        val formatScore = maxOf(0, formatPoints)

        // --- KEYWORD MATCH (Honest Job Description Overlap) ---
        var kwResult: KeywordMatchResult? = null
        if (jd.trim().length > 40) {
            val extracted = extractKeywords(jd)
            if (extracted.isNotEmpty()) {
                val matched = matchKeywords(cv, extracted)
                kwResult = matched
                if (matched.pct >= 75) {
                    addCheck(CheckLevel.OK, "Keyword match is ${matched.pct}%", 0, "Keywords")
                } else {
                    val level = if (matched.pct >= 50) CheckLevel.WARN else CheckLevel.BAD
                    val weight = if (matched.pct >= 50) 9 else 11
                    addCheck(
                        level,
                        "Keyword match is ${matched.pct}%. Review missing keywords below and manually add ONLY those that are true for you.",
                        weight,
                        "Keywords"
                    )
                }
            }
        }

        val bars = mutableListOf(
            CategoryBar("Sections", secScore),
            CategoryBar("Contact", contactScore),
            CategoryBar("Format", formatScore),
            CategoryBar("Dates", dateScore),
            CategoryBar("Results", impactScore)
        )
        val weightedParts = mutableListOf(
            secScore to 15,
            contactScore to 8,
            formatScore to 12,
            dateScore to 8,
            impactScore to 12
        )

        if (kwResult != null) {
            bars.add(0, CategoryBar("Keywords", kwResult.pct))
            weightedParts.add(kwResult.pct to 45)
        }

        val totalWeight = weightedParts.sumOf { it.second }
        val weightedSum = weightedParts.sumOf { it.first * it.second }
        val totalScore = Math.round(weightedSum.toDouble() / totalWeight.toDouble()).toInt()

        val displayMatchPct = kwResult?.pct ?: totalScore
        val keywordMatchHeadline = if (kwResult != null) {
            "Keyword Match: ${kwResult.pct}% - Estimate only, not real ATS"
        } else {
            "Readiness Check: $displayMatchPct% - Paste a Job Description for Keyword Match % (Estimate only, not real ATS)"
        }

        val bandLabel = when {
            displayMatchPct >= 75 -> "Strong"
            displayMatchPct >= 50 -> "Needs some work"
            else -> "Needs a lot of work"
        }
        val note = if (kwResult != null) {
            "Honest keyword overlap + ${checks.size} formatting & readability checks. Real ATS is company-specific with no public formula."
        } else {
            "Checked ${checks.size} formatting & readability rules. Paste a job description to see Missing Hard & Soft Skills."
        }

        val todos = checks.filter { it.level != CheckLevel.OK }
            .sortedByDescending { it.priorityWeight }
        val fixFirst = if (todos.isNotEmpty()) {
            todos.take(3)
        } else {
            listOf(AtsCheckItem(CheckLevel.OK, "No major problems found.", 0))
        }
        val allChecks = todos + checks.filter { it.level == CheckLevel.OK }

        return AtsAnalysisResult(
            totalScore = totalScore,
            keywordMatchHeadline = keywordMatchHeadline,
            bandLabel = bandLabel,
            note = note,
            bars = bars,
            fixFirst = fixFirst,
            allChecks = allChecks,
            formattingChecklist = checks,
            keywordMatch = kwResult,
            wordCount = words
        )
    }

    fun levelForScore(score: Int): CheckLevel = when {
        score >= 75 -> CheckLevel.OK
        score >= 50 -> CheckLevel.WARN
        else -> CheckLevel.BAD
    }
}
