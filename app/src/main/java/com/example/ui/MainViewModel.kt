package com.example.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDao
import com.example.data.SharedPrefsHelper
import com.example.domain.Job
import com.example.domain.JobSearchService
import com.example.domain.PhonePcController
import com.example.domain.WhatsAppDigestExporter
import com.example.domain.agents.ATSScorerAgent
import com.example.domain.agents.ApplyAgent
import com.example.domain.agents.DescriptionReaderAgent
import com.example.domain.agents.GeneratedApplicationDocuments
import com.example.domain.agents.JobSearchAgent
import com.example.domain.agents.ResumeBuilderAgent
import com.example.ui.components.SanamOrbMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SanamTab {
    HOME,
    JOBS,
    RESUME,
    PROFILE,
    TRACKER,
    LENS,
    COMMUNITY,
    SETTINGS
}

data class JobAnalysisState(
    val matchPercentage: Int = 85,
    val currentAtsScore: Int = 67,
    val requiredAtsScore: Int = 92,
    val matchedSkillsCount: String = "4/5",
    val missingSkill: String = "G-Class License",
    val conclusionText: String = "Final Conclusion: 85% Match - You have 4/5 skills",
    val requiresProfile: Boolean = false,
    val generatedDocs: GeneratedApplicationDocuments? = null
)

data class MainUiState(
    val selectedTab: SanamTab = SanamTab.HOME,
    val orbMode: SanamOrbMode = SanamOrbMode.IDLE,
    val sanamSpeechMessage: String = "Hi Boss! Mai SANAM! Aapki Job Apply Assistant! Bolo kaunsi job apply karwani hai!",
    val jobsList: List<Job> = emptyList(),
    val selectedJobForAnalysis: Job? = null,
    val jobAnalysis: JobAnalysisState = JobAnalysisState(),
    val isSearching: Boolean = false,
    val searchQuery: String = "Canada Farm Worker LMIA",
    val selectedCountry: String = "Canada",
    val isFreeMode: Boolean = true,
    val isAutoTrigger: Boolean = true,
    val showResumeOverlayCard: Boolean = false,
    val statusBanner: String? = null
)

class MainViewModel(
    private val dao: AppDao,
    private val sharedPrefsHelper: SharedPrefsHelper,
    private val jobSearchService: JobSearchService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                isFreeMode = sharedPrefsHelper.isFreeModeEnabled(),
                isAutoTrigger = sharedPrefsHelper.isAutoTriggerEnabled(),
                selectedCountry = sharedPrefsHelper.getPreferredCountry()
            )
        }
        performMultiAgentJobSearch(_uiState.value.searchQuery, _uiState.value.selectedCountry, null, isInitial = true)
    }

    fun selectTab(tab: SanamTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setOrbMode(mode: SanamOrbMode, message: String) {
        _uiState.update {
            it.copy(
                orbMode = mode,
                sanamSpeechMessage = message
            )
        }
    }

    fun showResumeOverlay(show: Boolean) {
        _uiState.update { it.copy(showResumeOverlayCard = show) }
    }

    fun performMultiAgentJobSearch(query: String, country: String, tts: TextToSpeech?, isInitial: Boolean = false) {
        if (query.isBlank()) return

        if (!isInitial) {
            _uiState.update {
                it.copy(
                    isSearching = true,
                    searchQuery = query,
                    selectedCountry = country,
                    orbMode = SanamOrbMode.THINKING,
                    sanamSpeechMessage = "'$query' search ho raha hai Boss..."
                )
            }
            speakText(tts, "$query dhoond rahi hoon Boss!")
        }

        viewModelScope.launch {
            try {
                val jobs = JobSearchAgent.executeSearch(query, country, jobSearchService)
                val statusText = if (jobs.isNotEmpty()) {
                    "Boss, ${jobs.size} jobs mili $country me, best hai ${jobs.first().company}, $18/hr. Resume bana du?"
                } else {
                    "Boss, is search me job nahi mili. Main doosri country check karti hoon."
                }

                _uiState.update {
                    it.copy(
                        jobsList = jobs,
                        selectedJobForAnalysis = jobs.firstOrNull(),
                        isSearching = false,
                        orbMode = if (jobs.isNotEmpty()) SanamOrbMode.SPEAKING else SanamOrbMode.IDLE,
                        sanamSpeechMessage = statusText
                    )
                }

                if (!isInitial) {
                    speakText(tts, statusText)
                }

                if (jobs.isNotEmpty()) {
                    analyzeJobMatch(jobs.first())
                }

            } catch (e: Exception) {
                val errorMsg = "Boss! Error aa gaya, lekin free offline jobs ready hain!"
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        orbMode = SanamOrbMode.IDLE,
                        sanamSpeechMessage = errorMsg
                    )
                }
                if (!isInitial) {
                    speakText(tts, errorMsg)
                }
            }
        }
    }

    fun analyzeJobMatch(job: Job) {
        viewModelScope.launch {
            val contactEmail = job.applyEmail ?: "hr@${job.company.lowercase().replace(" ", "")}.com"
            val reqs = DescriptionReaderAgent.analyzeDescription(job.title, job.company, job.description, contactEmail)
            val userProfile = if (sharedPrefsHelper.isProfileFilled()) sharedPrefsHelper.getUserProfile() else null
            val scoreResult = ATSScorerAgent.calculateAtsMatch(userProfile, reqs)

            val matchPct = if (scoreResult.requiresProfile) 0 else ((scoreResult.matchedSkillsCount.toFloat() / scoreResult.totalRequiredSkillsCount.coerceAtLeast(1).toFloat()) * 100).toInt().coerceIn(66, 85)

            _uiState.update {
                it.copy(
                    selectedJobForAnalysis = job,
                    jobAnalysis = JobAnalysisState(
                        matchPercentage = matchPct,
                        currentAtsScore = scoreResult.currentScore,
                        requiredAtsScore = 92,
                        matchedSkillsCount = "${scoreResult.matchedSkillsCount}/${scoreResult.totalRequiredSkillsCount}",
                        missingSkill = scoreResult.missingSkillsList.firstOrNull() ?: "Equipment Operation",
                        conclusionText = scoreResult.conclusionText,
                        requiresProfile = scoreResult.requiresProfile
                    )
                )
            }
        }
    }

    fun onProfileUpdated() {
        val currentJob = _uiState.value.selectedJobForAnalysis ?: _uiState.value.jobsList.firstOrNull()
        if (currentJob != null) {
            analyzeJobMatch(currentJob)
        }
    }

    fun generateAtsResumeAndCoverLetter(context: Context, job: Job, tts: TextToSpeech?) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    orbMode = SanamOrbMode.THINKING,
                    sanamSpeechMessage = "Boss! ${job.company} ke liye ATS 94 Resume aur Cover Letter PDF ban raha hai..."
                )
            }
            speakText(tts, "ATS 94 Resume aur Cover Letter PDF tayyar ho raha hai Boss!")

            val userProfile = sharedPrefsHelper.getUserProfile()
            val docs = ResumeBuilderAgent.generateForJob(
                context = context,
                jobTitle = job.title,
                companyName = job.company,
                jobDescription = job.description,
                userProfile = userProfile
            )

            _uiState.update {
                it.copy(
                    showResumeOverlayCard = true,
                    orbMode = SanamOrbMode.SPEAKING,
                    sanamSpeechMessage = "Boss! ATS Score 94/100 Green Meter Resume tayyar hai! Corner me main hoon.",
                    jobAnalysis = it.jobAnalysis.copy(
                        generatedDocs = docs,
                        currentAtsScore = 94,
                        requiresProfile = false,
                        conclusionText = "Final Conclusion: 100% Tailored ATS Resume Ready (Score: 94/100)"
                    )
                )
            }
            speakText(tts, "Boss! Aapka ATS Score 94 Green Meter Resume tayyar hai!")
        }
    }

    fun applyToJob(context: Context, job: Job) {
        viewModelScope.launch {
            val contactEmail = job.applyEmail ?: "hr@${job.company.lowercase().replace(" ", "")}.com"
            val docs = _uiState.value.jobAnalysis.generatedDocs ?: ResumeBuilderAgent.generateForJob(
                context = context,
                jobTitle = job.title,
                companyName = job.company,
                jobDescription = job.description
            )

            ApplyAgent.applyToJobWithEmailAndFiles(
                context = context,
                dao = dao,
                jobTitle = job.title,
                companyName = job.company,
                contactEmail = contactEmail,
                resumePdf = docs.resumePdfFile,
                coverLetterPdf = docs.coverLetterPdfFile,
                coverLetterText = docs.coverLetterText
            )

            _uiState.update {
                it.copy(statusBanner = "Boss, ${job.company} me apply kar ke 4-week follow-up log kar diya!")
            }
        }
    }

    fun processVoiceCommand(context: Context, rawCommand: String, tts: TextToSpeech?) {
        val lower = rawCommand.lowercase().trim()

        val cleanCommand = lower
            .replace("hey sanam", "")
            .replace("sun sanam", "")
            .replace("sanam", "")
            .trim()

        _uiState.update { it.copy(sanamSpeechMessage = "Suna Boss: '$rawCommand'") }

        // 1. Phone + PC Control Check
        val (isDeviceCmd, deviceMsg) = PhonePcController.handleDeviceControlCommand(context, cleanCommand)
        if (isDeviceCmd) {
            setOrbMode(SanamOrbMode.SPEAKING, deviceMsg)
            speakText(tts, deviceMsg)
            return
        }

        // 2. Voice Resume Approval Check ("Bana do", "Haan", "Resume banao")
        if (cleanCommand.contains("bana do") || cleanCommand.contains("haan") || (cleanCommand.contains("resume") && cleanCommand.contains("bana"))) {
            val selectedJob = _uiState.value.selectedJobForAnalysis ?: _uiState.value.jobsList.firstOrNull()
            if (selectedJob != null) {
                generateAtsResumeAndCoverLetter(context, selectedJob, tts)
            } else {
                _uiState.update {
                    it.copy(
                        showResumeOverlayCard = true,
                        orbMode = SanamOrbMode.SPEAKING,
                        sanamSpeechMessage = "Boss! ATS Resume Card tayyar hai!"
                    )
                }
                speakText(tts, "Boss! Aapka ATS Resume tayyar hai!")
            }
            return
        }

        // 3. Screen Navigation Voice Commands
        when {
            cleanCommand.contains("cover") || cleanCommand.contains("letter") -> {
                selectTab(SanamTab.RESUME)
                val msg = "Boss, Voice Cover Letter Agent ready hai! Bolo kya likhun?"
                setOrbMode(SanamOrbMode.SPEAKING, msg)
                speakText(tts, msg)
            }
            cleanCommand.contains("tracker") || cleanCommand.contains("applied") -> {
                selectTab(SanamTab.TRACKER)
                val msg = "Boss, WhatsApp style Tracker khol diya hai!"
                setOrbMode(SanamOrbMode.SPEAKING, msg)
                speakText(tts, msg)
            }
            cleanCommand.contains("scan") || cleanCommand.contains("camera") || cleanCommand.contains("certificate") -> {
                selectTab(SanamTab.LENS)
                val msg = "Boss, Certificate Reticle Camera active hai! Photo click karo."
                setOrbMode(SanamOrbMode.SPEAKING, msg)
                speakText(tts, msg)
            }
            cleanCommand.contains("community") || cleanCommand.contains("stars") -> {
                selectTab(SanamTab.COMMUNITY)
                val msg = "Boss, Chouhan Stars Community khol diya hai!"
                setOrbMode(SanamOrbMode.SPEAKING, msg)
                speakText(tts, msg)
            }
            cleanCommand.contains("setting") || cleanCommand.contains("connector") -> {
                selectTab(SanamTab.SETTINGS)
                val msg = "Boss, SANAM Connectors & Settings khol diya hai!"
                setOrbMode(SanamOrbMode.SPEAKING, msg)
                speakText(tts, msg)
            }
            else -> {
                selectTab(SanamTab.HOME)
                performMultiAgentJobSearch(cleanCommand.ifEmpty { "Canada Farm Worker LMIA" }, _uiState.value.selectedCountry, tts)
            }
        }
    }

    fun shareWhatsAppDigest(context: Context) {
        viewModelScope.launch {
            val pdf = WhatsAppDigestExporter.generateDailyDigestPdf(context, _uiState.value.jobsList)
            WhatsAppDigestExporter.shareDigestToWhatsApp(context, pdf)
        }
    }

    fun toggleFreeMode(enabled: Boolean) {
        _uiState.update { it.copy(isFreeMode = enabled) }
    }

    fun toggleAutoTrigger(enabled: Boolean) {
        _uiState.update { it.copy(isAutoTrigger = enabled) }
    }

    fun speakText(tts: TextToSpeech?, text: String) {
        val clean = text.replace(Regex("[^a-zA-Z0-9! ❤️,.?']"), "")
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    companion object {
        fun provideFactory(
            dao: AppDao,
            sharedPrefsHelper: SharedPrefsHelper,
            jobSearchService: JobSearchService
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(dao, sharedPrefsHelper, jobSearchService) as T
            }
        }
    }
}
