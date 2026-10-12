package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class JobEntry(
    val title: String = "",
    val employer: String = "",
    val dates: String = "",
    val bullets: String = "",
    val previousBulletsBeforeAi: String? = null
)

@Entity(tableName = "resume_drafts")
data class ResumeDraft(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val draftTitle: String = "My Resume",
    val fullName: String = "",
    val targetTitle: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val linkedin: String = "",
    val summary: String = "",
    val jobsJson: String = "[]",
    val education: String = "",
    val skills: String = "",
    val certificates: String = "",
    val lastAtsScore: Int? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

data class ResumeFormState(
    val id: Long = 0L,
    val fullName: String = "",
    val targetTitle: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val linkedin: String = "",
    val summary: String = "",
    val jobs: List<JobEntry> = listOf(JobEntry()),
    val education: String = "",
    val skills: String = "",
    val certificates: String = ""
) {
    fun contactParts(): List<String> =
        listOf(email.trim(), phone.trim(), location.trim(), linkedin.trim()).filter { it.isNotEmpty() }

    fun usedJobs(): List<JobEntry> =
        jobs.filter { it.title.isNotBlank() || it.employer.isNotBlank() || it.bullets.isNotBlank() }

    fun toPlainText(): String {
        val out = mutableListOf<String>()
        out.add(fullName.trim())
        out.add(targetTitle.trim())
        out.add(contactParts().joinToString(" | "))
        out.add("")

        if (summary.trim().isNotEmpty()) {
            out.add("Summary")
            out.add(summary.trim())
            out.add("")
        }

        val activeJobs = usedJobs()
        if (activeJobs.isNotEmpty()) {
            out.add("Work Experience")
            activeJobs.forEach { job ->
                out.add(job.title.trim())
                val empDate = listOf(job.employer.trim(), job.dates.trim())
                    .filter { it.isNotEmpty() }
                    .joinToString(" | ")
                out.add(empDate)
                splitLines(job.bullets).forEach { line ->
                    out.add("- ${stripBulletPrefix(line)}")
                }
                out.add("")
            }
        }

        val eduLines = splitLines(education)
        if (eduLines.isNotEmpty()) {
            out.add("Education")
            out.addAll(eduLines)
            out.add("")
        }

        if (skills.trim().isNotEmpty()) {
            out.add("Skills")
            out.add(skills.trim())
            out.add("")
        }

        val certLines = splitLines(certificates)
        if (certLines.isNotEmpty()) {
            out.add("Certifications")
            certLines.forEach { line ->
                out.add("- ${stripBulletPrefix(line)}")
            }
        }

        return out.joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    companion object {
        private val moshi: Moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        private val jobListType = Types.newParameterizedType(List::class.java, JobEntry::class.java)
        private val jobListAdapter = moshi.adapter<List<JobEntry>>(jobListType)

        fun splitLines(text: String?): List<String> =
            (text ?: "").split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        fun stripBulletPrefix(line: String): String =
            line.replace(Regex("^[-•*]\\s*"), "").trim()

        fun jobsToJson(jobs: List<JobEntry>): String =
            runCatching { jobListAdapter.toJson(jobs) }.getOrDefault("[]")

        fun jobsFromJson(json: String): List<JobEntry> =
            runCatching { jobListAdapter.fromJson(json) }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?: listOf(JobEntry())

        fun fromDraft(draft: ResumeDraft): ResumeFormState = ResumeFormState(
            id = draft.id,
            fullName = draft.fullName,
            targetTitle = draft.targetTitle,
            email = draft.email,
            phone = draft.phone,
            location = draft.location,
            linkedin = draft.linkedin,
            summary = draft.summary,
            jobs = jobsFromJson(draft.jobsJson),
            education = draft.education,
            skills = draft.skills,
            certificates = draft.certificates
        )

        fun toDraft(state: ResumeFormState, lastScore: Int? = null): ResumeDraft {
            val label = when {
                state.fullName.isNotBlank() && state.targetTitle.isNotBlank() ->
                    "${state.fullName.trim()} — ${state.targetTitle.trim()}"
                state.targetTitle.isNotBlank() -> state.targetTitle.trim()
                state.fullName.isNotBlank() -> state.fullName.trim()
                else -> "Untitled Resume"
            }
            return ResumeDraft(
                id = state.id,
                draftTitle = label,
                fullName = state.fullName,
                targetTitle = state.targetTitle,
                email = state.email,
                phone = state.phone,
                location = state.location,
                linkedin = state.linkedin,
                summary = state.summary,
                jobsJson = jobsToJson(state.jobs),
                education = state.education,
                skills = state.skills,
                certificates = state.certificates,
                lastAtsScore = lastScore,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}

data class IndustryExample(
    val id: String,
    val label: String,
    val badge: String,
    val formState: ResumeFormState,
    val sampleJobDescription: String
)

object CanadianResumeExamples {
    val examples: List<IndustryExample> = listOf(
        IndustryExample(
            id = "farm",
            label = "Farm Worker",
            badge = "NOC 85100 • Agriculture",
            formState = ResumeFormState(
                fullName = "Harpreet Singh Gill",
                targetTitle = "General Farm Worker (NOC 85100)",
                email = "harpreet.gill.work@email.ca",
                phone = "+1 (604) 555-0194",
                location = "Abbotsford, BC, Canada",
                linkedin = "linkedin.com/in/harpreetgill-ag",
                summary = "Reliable General Farm Worker with 5+ years of hands-on agricultural experience planting, cultivating, irrigating, and harvesting field and greenhouse crops including berries and vegetables across Abbotsford, BC. Skilled in operating and maintaining farm machinery, tractors, forklifts, and irrigation equipment safely while following food safety, hygiene, and WHMIS workplace safety guidelines in all weather conditions.",
                jobs = listOf(
                    JobEntry(
                        title = "General Farm Worker",
                        employer = "Fraser Valley Berry & Greenhouse Farms, Abbotsford, BC",
                        dates = "Mar 2022 - Present",
                        bullets = """
                            Plant, cultivate, irrigate, and harvest field and greenhouse crops including blueberries, strawberries, tomatoes, and greenhouse vegetables across 45 acres, packing 450+ flats daily with spoilage under 2%.
                            Operate and maintain farm machinery, Kubota utility tractors, forklifts, sprayers, and drip irrigation equipment safely with zero workplace accidents over 3 years.
                            Examine produce for quality, grade, sort, and pack fruits and vegetables for cold storage shipping while lifting 50 lbs repeatedly in a fast-paced outdoor team environment.
                            Train 12 seasonal farm workers on crop harvesting, pruning, grading, greenhouse operations, and WHMIS workplace safety guidelines, boosting crew output by 18%.
                        """.trimIndent()
                    ),
                    JobEntry(
                        title = "Agricultural Labourer & Packhouse Assistant",
                        employer = "Okanagan Orchard Co-op, Kelowna, BC",
                        dates = "Apr 2019 - Nov 2021",
                        bullets = """
                            Harvested, graded, sorted, and packed 1,200+ lbs of orchard fruits and field vegetables per shift for cold storage shipping according to Canada No. 1 quality standards.
                            Inspected sorting conveyors, sanitized harvesting bins, maintained irrigation lines, and prepared 35+ pallets daily for refrigerated transport.
                            Assisted farm mechanics with routine maintenance on farm machinery, tractors, mowers, and greenhouse ventilation systems in all weather conditions.
                        """.trimIndent()
                    )
                ),
                education = "High School Diploma, Rick Hansen Secondary School, Abbotsford, BC, 2018\nCertificate in Sustainable Agriculture & Greenhouse Operations, UFV Continuing Studies, 2020",
                skills = "Crop Harvesting, Greenhouse Operations, Plant & Cultivate Field Crops, Tractor & Forklift Operation, Farm Machinery Maintenance, Irrigation Equipment, Examine Produce Quality, Grade Sort & Pack, Pruning, Cold Storage Shipping, Food Safety & Hygiene, WHMIS Workplace Safety, Lift 50 lbs",
                certificates = "WHMIS 2015 Workplace Safety Certified\nBC Forklift Operator Certificate\nFood Safety & Agricultural Hygiene Training\nStandard First Aid & CPR-C"
            ),
            sampleJobDescription = """
                Job Title: General Farm Worker (NOC 85100)
                Location: Abbotsford, BC
                Responsibilities:
                - Plant, cultivate, irrigate, and harvest field and greenhouse crops including berries and vegetables.
                - Operate and maintain farm machinery, tractors, forklifts, and irrigation equipment safely.
                - Examine produce for quality, grade, sort, and pack fruits and vegetables for cold storage shipping.
                - Follow food safety, hygiene, and WHMIS workplace safety guidelines in all weather conditions.
                Requirements:
                - Previous farm experience in crop harvesting, pruning, grading, and greenhouse operations preferred.
                - Ability to lift 50 lbs repeatedly and work outdoors in a fast-paced team environment.
            """.trimIndent()
        ),
        IndustryExample(
            id = "construction",
            label = "Construction Worker",
            badge = "NOC 75110 • Trades & Site Labour",
            formState = ResumeFormState(
                fullName = "Marcus Desjardins",
                targetTitle = "Construction Labourer / Craft Worker (NOC 75110)",
                email = "marcus.desjardins@email.ca",
                phone = "+1 (416) 555-0148",
                location = "Mississauga, Greater Toronto Area, ON",
                linkedin = "linkedin.com/in/marcusdesjardins-build",
                summary = "Safety-focused Construction Labourer and Craft Worker with 6+ years of commercial and residential job site experience in the Greater Toronto Area. Proven expertise loading, unloading, and staging construction materials, assisting carpenters and concrete finishers with formwork, concrete pouring, framing support, and scaffolding setup while maintaining 100% OHSA safety compliance.",
                jobs = listOf(
                    JobEntry(
                        title = "Construction Labourer & Craft Worker",
                        employer = "MapleCrest Builders Ltd., Greater Toronto Area, ON",
                        dates = "May 2021 - Present",
                        bullets = """
                            Load, unload, and stage construction materials, lumber, rebar, and tools across 14 commercial and residential job sites valued at over $8M, reducing daily crew setup time by 20%.
                            Assist carpenters and concrete finishers with formwork, concrete pouring, framing support, blueprint reading, and scaffolding setup for 120+ cubic yards of foundation slabs.
                            Operate power tools, pneumatic hammers, compactors, rotary lasers, and skid-steer equipment safely during site preparation, demolition, trench cleanup, and traffic control duties.
                            Enforce Working at Heights certification, WHMIS, and OHSA safety standards during daily toolbox talks for a 25-person physically demanding team.
                        """.trimIndent()
                    ),
                    JobEntry(
                        title = "General Site Helper & Demolition Assistant",
                        employer = "NorthShore Renovations, Brampton, ON",
                        dates = "Jun 2018 - Apr 2021",
                        bullets = """
                            Performed site preparation, selective demolition, trench cleanup, debris removal, and scaffolding setup across 60+ residential and commercial job sites on schedule.
                            Measured, cut, and staged lumber and drywall materials while reading blueprints and operating power tools and pneumatic hammers safely.
                            Maintained 99% on-time attendance and earned 2 quarterly OHSA Safety Recognition awards for zero lost-time incidents.
                        """.trimIndent()
                    )
                ),
                education = "Ontario Secondary School Diploma (OSSD), Cawthra Park Secondary, Mississauga, ON, 2018\nConstruction Craft Worker Pre-Apprenticeship Program, George Brown College, Toronto, 2019",
                skills = "Load Unload & Stage Construction Materials, Formwork & Concrete Pouring, Framing Support, Scaffolding Setup, Assist Carpenters & Concrete Finishers, Power Tools & Pneumatic Hammers, Compactors & Skid-Steer Equipment, Site Preparation & Demolition, Trench Cleanup & Traffic Control, Reading Blueprints, OHSA Safety Standards, Working at Heights, WHMIS",
                certificates = "Working at Heights Certification (Ministry of Labour Approved)\nWHMIS 2015 & OHSA 4-Step Worker Safety Standards\nPropane in Construction & Traffic Control Certificate\nStandard First Aid & CPR Level C"
            ),
            sampleJobDescription = """
                Position: Construction Labourer / Craft Worker (NOC 75110)
                Location: Greater Toronto Area, ON
                Job Duties:
                - Load, unload, and stage construction materials, lumber, and tools at commercial and residential job sites.
                - Assist carpenters and concrete finishers with formwork, concrete pouring, framing support, and scaffolding setup.
                - Operate power tools, pneumatic hammers, compactors, and skid-steer equipment safely.
                - Perform site preparation, demolition, trench cleanup, and traffic control duties.
                Qualifications:
                - Working at Heights certification, WHMIS, and knowledge of OHSA safety standards required.
                - Experience reading blueprints, handling concrete, and working in a physically demanding team role.
            """.trimIndent()
        ),
        IndustryExample(
            id = "retail",
            label = "Retail Supervisor",
            badge = "NOC 62010 • Retail & Customer Service",
            formState = ResumeFormState(
                fullName = "Priya Nair",
                targetTitle = "Retail Store Supervisor (NOC 62010)",
                email = "priya.nair.retail@email.ca",
                phone = "+1 (403) 555-0182",
                location = "Calgary, AB, Canada",
                linkedin = "linkedin.com/in/priyanair-retail",
                summary = "Results-driven Retail Store Supervisor with 5+ years of retail sales and keyholder experience in Calgary, AB. Skilled in supervising, scheduling, and training retail sales associates and cashiers across daily shifts, monitoring sales KPIs, reconciling POS cash drawers, and overseeing inventory management, stock receiving, cycle counts, and loss prevention procedures.",
                jobs = listOf(
                    JobEntry(
                        title = "Retail Store Supervisor",
                        employer = "Prairie Market & Goods, Calgary, AB",
                        dates = "Jan 2022 - Present",
                        bullets = """
                            Supervise, schedule, and train 18 retail sales associates and cashiers across daily shifts in a high-volume Calgary store generating $45,000+ in daily revenue.
                            Assign duties, monitor sales KPIs, and maintain high standards of customer service and visual merchandising, increasing monthly store conversion rates by 14%.
                            Reconcile 8 POS cash drawers daily with 99.8% accuracy, prepare daily bank deposits, and resolve 30+ customer complaints and escalations weekly.
                            Oversee inventory management, stock receiving, cycle counts, and loss prevention procedures across 4,000+ SKUs, cutting inventory shrinkage by 22%.
                        """.trimIndent()
                    ),
                    JobEntry(
                        title = "Senior Retail Sales Associate & Keyholder",
                        employer = "Northern Trail Outfitters, Calgary, AB",
                        dates = "Aug 2019 - Dec 2021",
                        bullets = """
                            Exceeded monthly sales KPIs by 16% through customer service excellence, visual merchandising resets, and onboarding 400+ loyalty members.
                            Supported team supervision, staff scheduling, POS systems troubleshooting, daily bank deposits, and quarterly inventory control cycle counts.
                            Trained 12 new cashiers and sales associates on POS cash drawers, stock receiving, workplace safety, and loss prevention procedures.
                        """.trimIndent()
                    )
                ),
                education = "Diploma in Business Administration (Retail Management), SAIT Polytechnic, Calgary, AB, 2019",
                skills = "Team Supervision, Supervise Schedule & Train Associates, Staff Scheduling, POS Systems & Cash Drawers Reconciliation, Daily Bank Deposits, Sales KPIs Monitoring, Customer Service & Visual Merchandising, Resolve Customer Complaints & Escalations, Inventory Management & Inventory Control, Stock Receiving & Cycle Counts, Loss Prevention Procedures, Keyholder Experience, Workplace Safety",
                certificates = "Retail Store Supervisor Certificate — Retail Council of Canada\nWHMIS 2015 & Workplace Safety Certification\nFood Safe Level 1 Certificate\nEmergency First Aid & AED"
            ),
            sampleJobDescription = """
                Job Title: Retail Store Supervisor (NOC 62010)
                Location: Calgary, AB
                Key Responsibilities:
                - Supervise, schedule, and train retail sales associates and cashiers across daily shifts.
                - Assign duties, monitor sales KPIs, and ensure high standards of customer service and visual merchandising.
                - Reconcile POS cash drawers, prepare daily bank deposits, and resolve customer complaints and escalations.
                - Oversee inventory management, stock receiving, cycle counts, and loss prevention procedures.
                Requirements:
                - 2+ years of retail sales or keyholder experience with proven team supervision skills.
                - Strong knowledge of POS systems, inventory control, staff scheduling, and workplace safety.
            """.trimIndent()
        )
    )
}
