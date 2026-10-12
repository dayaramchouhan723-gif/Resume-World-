package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.IndustryExample
import com.example.data.JobEntry
import com.example.data.ResumeDraft
import com.example.data.ResumeFormState
import com.example.data.ResumeRepository
import com.example.data.SecureApiKeyManager
import com.example.domain.AtsAnalysisResult
import com.example.domain.AtsAnalyzer
import com.example.domain.GeminiRewriteService
import com.example.util.ResumeFileHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab {
    BUILD_RESUME,
    MATCH_WITH_JOB,
    SAVED_DRAFTS
}

data class ResumeUiState(
    val activeTab: AppTab = AppTab.BUILD_RESUME,
    val formState: ResumeFormState = ResumeFormState(),
    val cvText: String = "",
    val jdText: String = "",
    val checkerErrorMessage: String = "",
    val analysisResult: AtsAnalysisResult? = null,
    val selectedExampleId: String? = null,
    val rewritingJobIndex: Int? = null,
    val showApiKeyDialog: Boolean = false,
    val hasCustomApiKey: Boolean = false,
    val statusBanner: String? = null,
    val isDarkThemeOverride: Boolean? = true // Default to dark theme as requested ("Keep existing dark theme")
)

class ResumeViewModel(
    private val repository: ResumeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResumeUiState())
    val uiState: StateFlow<ResumeUiState> = _uiState.asStateFlow()

    val savedDrafts: StateFlow<List<ResumeDraft>> = repository.allDrafts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var autoSaveJob: Job? = null

    init {
        viewModelScope.launch {
            val latest = repository.getLatestDraft()
            if (latest != null) {
                val restored = ResumeFormState.fromDraft(latest)
                _uiState.update {
                    it.copy(
                        formState = restored,
                        cvText = restored.toPlainText()
                    )
                }
            }
        }
    }

    fun refreshApiKeyStatus(context: Context) {
        val hasUserKey = SecureApiKeyManager.getUserStoredApiKey(context).isNotEmpty()
        _uiState.update { it.copy(hasCustomApiKey = hasUserKey) }
    }

    fun setShowApiKeyDialog(show: Boolean) {
        _uiState.update { it.copy(showApiKeyDialog = show) }
    }

    fun saveUserGeminiKey(context: Context, key: String) {
        SecureApiKeyManager.saveUserApiKey(context, key)
        _uiState.update {
            it.copy(
                hasCustomApiKey = key.trim().isNotEmpty(),
                showApiKeyDialog = false,
                statusBanner = if (key.trim().isNotEmpty()) {
                    "Gemini API key encrypted & saved locally on device."
                } else {
                    "Cleared custom Gemini API key."
                }
            )
        }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { current ->
            // Auto-fill Current Resume Text from Build tab when entering Match with Job
            val builderPlain = current.formState.toPlainText()
            val updatedCv = if (tab == AppTab.MATCH_WITH_JOB && (current.cvText.isBlank() || builderPlain.length > 30)) {
                builderPlain
            } else {
                current.cvText
            }
            current.copy(
                activeTab = tab,
                cvText = updatedCv,
                checkerErrorMessage = ""
            )
        }
    }

    fun toggleDarkTheme(currentDark: Boolean) {
        _uiState.update { it.copy(isDarkThemeOverride = !currentDark) }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }

    fun showBanner(message: String) {
        _uiState.update { it.copy(statusBanner = message) }
    }

    fun updateForm(transform: (ResumeFormState) -> ResumeFormState) {
        _uiState.update { current ->
            val updated = transform(current.formState)
            current.copy(formState = updated)
        }
        scheduleAutoSave()
    }

    fun addJob() {
        updateForm { state ->
            state.copy(jobs = state.jobs + JobEntry())
        }
    }

    fun updateJob(index: Int, transform: (JobEntry) -> JobEntry) {
        updateForm { state ->
            if (index !in state.jobs.indices) return@updateForm state
            val updatedJobs = state.jobs.toMutableList()
            updatedJobs[index] = transform(updatedJobs[index])
            state.copy(jobs = updatedJobs)
        }
    }

    fun removeJob(index: Int) {
        updateForm { state ->
            if (state.jobs.size <= 1 || index !in state.jobs.indices) return@updateForm state
            val updatedJobs = state.jobs.toMutableList().apply { removeAt(index) }
            state.copy(jobs = updatedJobs)
        }
    }

    /**
     * Rewrites "What you did" (Urdu, Hindi, Roman Urdu, or rough English) into 3-5 English bullet points
     * using ONLY what the user wrote. Sends ONLY jobTitle + job.bullets to Gemini API.
     */
    fun rewriteJobWithAi(context: Context, index: Int) {
        val currentJobs = _uiState.value.formState.jobs
        if (index !in currentJobs.indices) return
        val job = currentJobs[index]
        if (job.bullets.trim().length < 5) {
            _uiState.update {
                it.copy(statusBanner = "Write what you did in Job ${index + 1} first (Roman Urdu, Urdu, Hindi, or English).")
            }
            return
        }

        val apiKey = SecureApiKeyManager.getEffectiveApiKey(context)
        if (apiKey.isBlank()) {
            _uiState.update {
                it.copy(
                    showApiKeyDialog = true,
                    statusBanner = "Enter your free Gemini API key (stored in EncryptedSharedPreferences) to use AI Rewrite."
                )
            }
            return
        }

        _uiState.update { it.copy(rewritingJobIndex = index) }

        viewModelScope.launch {
            val result = GeminiRewriteService.rewriteExperienceToEnglishBullets(
                apiKey = apiKey,
                jobTitle = job.title,
                whatYouDidInput = job.bullets
            )
            result.onSuccess { rewrittenBullets ->
                updateJob(index) { existing ->
                    existing.copy(
                        previousBulletsBeforeAi = existing.previousBulletsBeforeAi ?: existing.bullets,
                        bullets = rewrittenBullets
                    )
                }
                _uiState.update {
                    it.copy(
                        rewritingJobIndex = null,
                        statusBanner = "Rewrote Job ${index + 1} in English using only your input. Tap 'Undo AI' anytime."
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        rewritingJobIndex = null,
                        statusBanner = error.message ?: "AI Rewrite failed. Check your API key or connection."
                    )
                }
            }
        }
    }

    /**
     * Restores the user's original Roman Urdu / Urdu / Hindi / English text before AI rewrite.
     */
    fun undoJobAiRewrite(index: Int) {
        val currentJobs = _uiState.value.formState.jobs
        if (index !in currentJobs.indices) return
        val prev = currentJobs[index].previousBulletsBeforeAi ?: return
        updateJob(index) { existing ->
            existing.copy(
                bullets = prev,
                previousBulletsBeforeAi = null
            )
        }
        _uiState.update {
            it.copy(statusBanner = "Restored original text for Job ${index + 1}.")
        }
    }

    fun startOver() {
        val blank = ResumeFormState(id = 0L)
        _uiState.update {
            it.copy(
                formState = blank,
                selectedExampleId = null,
                statusBanner = "Started a fresh blank resume."
            )
        }
    }

    fun loadExample(example: IndustryExample) {
        val populated = example.formState.copy(id = _uiState.value.formState.id)
        val plain = populated.toPlainText()
        val analysis = AtsAnalyzer.analyze(plain, example.sampleJobDescription)
        _uiState.update {
            it.copy(
                formState = populated,
                cvText = plain,
                jdText = example.sampleJobDescription,
                analysisResult = analysis,
                checkerErrorMessage = "",
                selectedExampleId = example.id,
                statusBanner = "Loaded ${example.label} sample. Edit any skills/certificates that don't apply to you!"
            )
        }
        scheduleAutoSave(score = analysis.keywordMatch?.pct ?: analysis.totalScore)
    }

    fun checkCurrentBuilderResumeScore() {
        val plain = _uiState.value.formState.toPlainText()
        _uiState.update {
            it.copy(
                cvText = plain,
                activeTab = AppTab.MATCH_WITH_JOB,
                checkerErrorMessage = ""
            )
        }
        runScoreCheck()
    }

    fun updateCvText(text: String) {
        _uiState.update { it.copy(cvText = text, checkerErrorMessage = "") }
    }

    fun updateJdText(text: String) {
        _uiState.update { it.copy(jdText = text) }
    }

    fun runScoreCheck() {
        val current = _uiState.value
        val cv = current.cvText.ifBlank { current.formState.toPlainText() }
        val jd = current.jdText
        if (cv.trim().length < 80) {
            _uiState.update {
                it.copy(
                    cvText = cv,
                    checkerErrorMessage = "Paste or build your full resume text first (at least 80 characters).",
                    analysisResult = null
                )
            }
            return
        }
        val result = AtsAnalyzer.analyze(cv, jd)
        _uiState.update {
            it.copy(
                cvText = cv,
                checkerErrorMessage = "",
                analysisResult = result
            )
        }
        scheduleAutoSave(score = result.keywordMatch?.pct ?: result.totalScore)
    }

    fun importResumeFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = ResumeFileHelper.extractTextFromUri(context, uri)
            result.onSuccess { extractedText ->
                val analysis = if (extractedText.trim().length >= 80) {
                    AtsAnalyzer.analyze(extractedText, _uiState.value.jdText)
                } else null
                _uiState.update {
                    it.copy(
                        cvText = extractedText,
                        activeTab = AppTab.MATCH_WITH_JOB,
                        checkerErrorMessage = if (analysis == null) "Paste your full resume text first." else "",
                        analysisResult = analysis,
                        statusBanner = "Imported resume text (${extractedText.split(Regex("\\s+")).size} words)."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        statusBanner = err.message ?: "Could not read selected file."
                    )
                }
            }
        }
    }

    fun saveDraftNow() {
        viewModelScope.launch {
            val state = _uiState.value.formState
            val plain = state.toPlainText()
            val score = if (plain.length >= 80) {
                val res = AtsAnalyzer.analyze(plain, _uiState.value.jdText)
                res.keywordMatch?.pct ?: res.totalScore
            } else null
            val id = repository.saveDraft(ResumeFormState.toDraft(state, score))
            _uiState.update {
                it.copy(
                    formState = state.copy(id = id),
                    statusBanner = "Saved resume draft locally on device."
                )
            }
        }
    }

    fun loadSavedDraft(draft: ResumeDraft) {
        val restored = ResumeFormState.fromDraft(draft)
        val plain = restored.toPlainText()
        _uiState.update {
            it.copy(
                formState = restored,
                cvText = plain,
                activeTab = AppTab.BUILD_RESUME,
                statusBanner = "Loaded draft: ${draft.draftTitle}"
            )
        }
    }

    fun deleteSavedDraft(id: Long) {
        viewModelScope.launch {
            repository.deleteDraft(id)
            if (_uiState.value.formState.id == id) {
                _uiState.update { it.copy(formState = it.formState.copy(id = 0L)) }
            }
            _uiState.update { it.copy(statusBanner = "Draft deleted.") }
        }
    }

    private fun scheduleAutoSave(score: Int? = null) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500)
            val state = _uiState.value.formState
            val hasContent = state.fullName.isNotBlank() ||
                state.targetTitle.isNotBlank() ||
                state.email.isNotBlank() ||
                state.summary.isNotBlank() ||
                state.usedJobs().isNotEmpty()
            if (!hasContent) return@launch

            val computedScore = score ?: _uiState.value.analysisResult?.let {
                it.keywordMatch?.pct ?: it.totalScore
            }
            val savedId = repository.saveDraft(ResumeFormState.toDraft(state, computedScore))
            if (state.id == 0L && savedId > 0L) {
                _uiState.update { current ->
                    current.copy(formState = current.formState.copy(id = savedId))
                }
            }
        }
    }

    companion object {
        fun provideFactory(repository: ResumeRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ResumeViewModel(repository) as T
                }
            }
    }
}
