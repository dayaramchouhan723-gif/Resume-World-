package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.CanadianResumeExamples
import com.example.data.IndustryExample
import com.example.data.JobEntry
import com.example.data.ResumeDraft
import com.example.data.ResumeFormState
import com.example.data.SecureApiKeyManager
import com.example.domain.AtsAnalyzer
import com.example.ui.components.AtsCategoryBarsSection
import com.example.ui.components.AtsCheckListSection
import com.example.ui.components.AtsScoreHeader
import com.example.ui.components.ExportTutorialPrefs
import com.example.ui.components.ExportTutorialSheet
import com.example.ui.components.MissingKeywordsSection
import com.example.ui.components.PaperPreviewCard
import com.example.ui.components.colorForLevel
import com.example.ui.theme.LocalAtsColors
import com.example.util.ResumeFileHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ResumeAppScreen(
    viewModel: ResumeViewModel,
    isDarkTheme: Boolean
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedDrafts by viewModel.savedDrafts.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refreshApiKeyStatus(context)
    }

    // Back navigation handler for secondary tabs
    BackHandler(enabled = uiState.activeTab != AppTab.BUILD_RESUME) {
        viewModel.selectTab(AppTab.BUILD_RESUME)
    }

    // File picker launcher for importing .txt, .docx, .pdf resume files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importResumeFromUri(context, uri)
        }
    }

    LaunchedEffect(uiState.statusBanner) {
        val message = uiState.statusBanner
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.dismissBanner()
        }
    }

    var showExportTutorial by remember { mutableStateOf(false) }
    var pendingExportAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var hasShownTutorialThisSession by remember { mutableStateOf(false) }

    fun runWithExportTutorialCheck(action: () -> Unit) {
        if (!hasShownTutorialThisSession && ExportTutorialPrefs.shouldShowOnFirstExport(context)) {
            hasShownTutorialThisSession = true
            pendingExportAction = action
            showExportTutorial = true
        } else {
            action()
        }
    }

    if (showExportTutorial) {
        ExportTutorialSheet(
            initialDontShowAgain = !ExportTutorialPrefs.shouldShowOnFirstExport(context),
            onDismiss = { dontShow ->
                ExportTutorialPrefs.setDontShowAgain(context, dontShow)
                showExportTutorial = false
                pendingExportAction?.invoke()
                pendingExportAction = null
            },
            onProceedAction = if (pendingExportAction != null) { dontShow ->
                ExportTutorialPrefs.setDontShowAgain(context, dontShow)
                showExportTutorial = false
                pendingExportAction?.invoke()
                pendingExportAction = null
            } else null
        )
    }

    if (uiState.showApiKeyDialog) {
        GeminiApiKeyDialog(
            initialKey = SecureApiKeyManager.getUserStoredApiKey(context),
            onSaveKey = { key -> viewModel.saveUserGeminiKey(context, key) },
            onClearKey = { viewModel.saveUserGeminiKey(context, "") },
            onDismiss = { viewModel.setShowApiKeyDialog(false) }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isExpandedScreen = maxWidth >= 840.dp
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val maxContentWidth = if (isExpandedScreen) 1080.dp else 640.dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = maxContentWidth)
                ) {
                    TopHeaderSection(
                        isDarkTheme = isDarkTheme,
                        draftCount = savedDrafts.size,
                        hasCustomApiKey = uiState.hasCustomApiKey,
                        onOpenApiKeyDialog = { viewModel.setShowApiKeyDialog(true) },
                        onToggleDarkTheme = { viewModel.toggleDarkTheme(isDarkTheme) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ExampleTemplatesRow(
                        examples = CanadianResumeExamples.examples,
                        selectedId = uiState.selectedExampleId,
                        onSelectExample = { viewModel.loadExample(it) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    NavigationTabsRow(
                        activeTab = uiState.activeTab,
                        draftCount = savedDrafts.size,
                        onSelectTab = { viewModel.selectTab(it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    when (uiState.activeTab) {
                        AppTab.BUILD_RESUME -> {
                            val handleDownloadPdf = {
                                runWithExportTutorialCheck {
                                    val res = ResumeFileHelper.downloadPdfToDownloads(context, uiState.formState)
                                    res.onSuccess { fileName ->
                                        viewModel.showBanner("Saved single-column PDF to Downloads/$fileName")
                                    }.onFailure {
                                        ResumeFileHelper.printOrSavePdf(context, uiState.formState)
                                    }
                                }
                            }
                            val handleDownloadTxt = {
                                runWithExportTutorialCheck {
                                    val res = ResumeFileHelper.downloadTxtToDownloads(context, uiState.formState)
                                    res.onSuccess { fileName ->
                                        viewModel.showBanner("Saved plain-text resume to Downloads/$fileName")
                                    }.onFailure { err ->
                                        viewModel.showBanner(err.message ?: "Could not save TXT file.")
                                    }
                                }
                            }
                            val handleCopyPlainText = {
                                runWithExportTutorialCheck {
                                    copyToClipboard(context, "Resume Text", uiState.formState.toPlainText())
                                    viewModel.showBanner("Copied plain text to clipboard (ATS ready).")
                                }
                            }

                            if (isExpandedScreen) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1.1f)) {
                                        ResumeBuilderFormSection(
                                            formState = uiState.formState,
                                            rewritingJobIndex = uiState.rewritingJobIndex,
                                            onUpdateForm = viewModel::updateForm,
                                            onAddJob = viewModel::addJob,
                                            onUpdateJob = viewModel::updateJob,
                                            onRemoveJob = viewModel::removeJob,
                                            onRewriteJobWithAi = { idx -> viewModel.rewriteJobWithAi(context, idx) },
                                            onUndoJobAi = viewModel::undoJobAiRewrite,
                                            onOpenExportTutorial = {
                                                pendingExportAction = null
                                                showExportTutorial = true
                                            },
                                            onDownloadPdf = handleDownloadPdf,
                                            onPrintPdf = {
                                                ResumeFileHelper.printOrSavePdf(context, uiState.formState)
                                            },
                                            onDownloadTxt = handleDownloadTxt,
                                            onCopyPlainText = handleCopyPlainText,
                                            onSharePlainText = {
                                                ResumeFileHelper.sharePlainText(
                                                    context,
                                                    uiState.formState.fullName,
                                                    uiState.formState.toPlainText()
                                                )
                                            },
                                            onCheckScore = viewModel::checkCurrentBuilderResumeScore,
                                            onSaveDraft = viewModel::saveDraftNow,
                                            onStartOver = viewModel::startOver,
                                            showInlinePaperPreview = false
                                        )
                                    }
                                    Column(modifier = Modifier.weight(0.9f)) {
                                        Text(
                                            text = stringResource(R.string.section_preview),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        PaperPreviewCard(formState = uiState.formState)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.canadian_resume_note),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LocalAtsColors.current.muted
                                        )
                                    }
                                }
                            } else {
                                ResumeBuilderFormSection(
                                    formState = uiState.formState,
                                    rewritingJobIndex = uiState.rewritingJobIndex,
                                    onUpdateForm = viewModel::updateForm,
                                    onAddJob = viewModel::addJob,
                                    onUpdateJob = viewModel::updateJob,
                                    onRemoveJob = viewModel::removeJob,
                                    onRewriteJobWithAi = { idx -> viewModel.rewriteJobWithAi(context, idx) },
                                    onUndoJobAi = viewModel::undoJobAiRewrite,
                                    onOpenExportTutorial = {
                                        pendingExportAction = null
                                        showExportTutorial = true
                                    },
                                    onDownloadPdf = handleDownloadPdf,
                                    onPrintPdf = {
                                        ResumeFileHelper.printOrSavePdf(context, uiState.formState)
                                    },
                                    onDownloadTxt = handleDownloadTxt,
                                    onCopyPlainText = handleCopyPlainText,
                                    onSharePlainText = {
                                        ResumeFileHelper.sharePlainText(
                                            context,
                                            uiState.formState.fullName,
                                            uiState.formState.toPlainText()
                                        )
                                    },
                                    onCheckScore = viewModel::checkCurrentBuilderResumeScore,
                                    onSaveDraft = viewModel::saveDraftNow,
                                    onStartOver = viewModel::startOver,
                                    showInlinePaperPreview = true
                                )
                            }
                        }

                        AppTab.MATCH_WITH_JOB -> {
                            JobMatcherSection(
                                cvText = uiState.cvText,
                                jdText = uiState.jdText,
                                errorMessage = uiState.checkerErrorMessage,
                                analysisResult = uiState.analysisResult,
                                onCvTextChange = viewModel::updateCvText,
                                onJdTextChange = viewModel::updateJdText,
                                onRunCheck = viewModel::runScoreCheck,
                                onUploadFileClick = {
                                    filePickerLauncher.launch(
                                        arrayOf(
                                            "text/plain",
                                            "application/pdf",
                                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                        )
                                    )
                                },
                                onUseBuilderResume = {
                                    val builderText = uiState.formState.toPlainText()
                                    viewModel.updateCvText(builderText)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Synced Current Resume Text from Build tab.")
                                    }
                                },
                                onCopyMissingKeywords = { missingStr ->
                                    copyToClipboard(context, "Missing Keywords", missingStr)
                                }
                            )
                        }

                        AppTab.SAVED_DRAFTS -> {
                            SavedDraftsSection(
                                drafts = savedDrafts,
                                activeDraftId = uiState.formState.id,
                                onLoadDraft = viewModel::loadSavedDraft,
                                onDeleteDraft = viewModel::deleteSavedDraft,
                                onStartNewDraft = {
                                    viewModel.startOver()
                                    viewModel.selectTab(AppTab.BUILD_RESUME)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Centered Dayaram Chouhan Footer
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("dayaram_footer_section"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.about_designed_by),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFFF2092),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.about_ats_optimized),
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAtsColors.current.muted,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TopHeaderSection(
    isDarkTheme: Boolean,
    draftCount: Int,
    hasCustomApiKey: Boolean,
    onOpenApiKeyDialog: () -> Unit,
    onToggleDarkTheme: () -> Unit
) {
    val atsColors = LocalAtsColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.header_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.testTag("app_header_title")
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.developer_name),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFFF2092),
                    modifier = Modifier.testTag("header_developer_name")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.header_developer_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = atsColors.muted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.header_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = atsColors.muted
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onOpenApiKeyDialog,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(atsColors.card)
                        .testTag("api_key_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Gemini API Key (Encrypted on-device)",
                        tint = if (hasCustomApiKey) atsColors.ok else MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onToggleDarkTheme,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(atsColors.card)
                        .testTag("theme_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDarkTheme) "Switch to light mode" else "Switch to dark mode",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Canadian Standard 100% On-Device tag & banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = atsColors.card,
            border = BorderStroke(1.dp, atsColors.line),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("canadian_standard_on_device_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_resume_banner_1791701272974),
                    contentDescription = "Canadian Resume Builder and ATS Checker illustration",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 88.dp, height = 56.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Canadian Standard • 100% On-Device",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (draftCount > 0) {
                            "$draftCount local draft(s) saved on device. Never auto-adds skills or certificates you didn't write."
                        } else {
                            "Single-column format. Leaves out photo, DOB & marital status. Never auto-adds unverified claims."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = atsColors.muted
                    )
                }
            }
        }
    }
}

@Composable
private fun GeminiApiKeyDialog(
    initialKey: String,
    onSaveKey: (String) -> Unit,
    onClearKey: () -> Unit,
    onDismiss: () -> Unit
) {
    var keyInput by remember { mutableStateOf(initialKey) }
    val atsColors = LocalAtsColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Local Gemini API Key",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Your Gemini API key is encrypted and stored locally on your device using Android EncryptedSharedPreferences (AES-256 GCM). It is never uploaded to any third-party server.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Privacy Rule: When you tap 'Rewrite in English with AI', ONLY the Job Title and 'What you did' text are sent to Gemini (free Flash Lite / Flash model). Your name, phone, and email are NEVER sent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = atsColors.muted
                )
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("Paste Gemini API Key (AIza...)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_gemini_api_key")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.about_designed_by),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFF2092),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.about_ats_optimized),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = atsColors.muted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveKey(keyInput) },
                modifier = Modifier.testTag("save_api_key_button")
            ) {
                Text("Save Encrypted")
            }
        },
        dismissButton = {
            Row {
                if (initialKey.isNotEmpty()) {
                    TextButton(onClick = onClearKey) {
                        Text("Clear", color = atsColors.bad)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun ExampleTemplatesRow(
    examples: List<IndustryExample>,
    selectedId: String?,
    onSelectExample: (IndustryExample) -> Unit
) {
    val atsColors = LocalAtsColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WorkOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Load sample resume & job ad (edit to match your real history):",
                style = MaterialTheme.typography.labelMedium,
                color = atsColors.muted
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            examples.forEach { example ->
                val isSelected = selectedId == example.id
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    } else {
                        atsColors.card
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else atsColors.line
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectExample(example) }
                        .testTag("load_example_${example.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = example.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                text = example.badge,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = atsColors.muted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationTabsRow(
    activeTab: AppTab,
    draftCount: Int,
    onSelectTab: (AppTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TabSwitchButton(
            text = stringResource(R.string.tab_build),
            isSelected = activeTab == AppTab.BUILD_RESUME,
            onClick = { onSelectTab(AppTab.BUILD_RESUME) },
            modifier = Modifier
                .weight(1f)
                .testTag("tab_build_resume")
        )
        TabSwitchButton(
            text = stringResource(R.string.tab_match),
            isSelected = activeTab == AppTab.MATCH_WITH_JOB,
            onClick = { onSelectTab(AppTab.MATCH_WITH_JOB) },
            modifier = Modifier
                .weight(1.1f)
                .testTag("tab_check_score")
        )
        TabSwitchButton(
            text = if (draftCount > 0) "Drafts ($draftCount)" else stringResource(R.string.tab_saved),
            isSelected = activeTab == AppTab.SAVED_DRAFTS,
            onClick = { onSelectTab(AppTab.SAVED_DRAFTS) },
            modifier = Modifier
                .weight(0.85f)
                .testTag("tab_saved_drafts")
        )
    }
}

@Composable
private fun TabSwitchButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val atsColors = LocalAtsColors.current
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else atsColors.card
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else atsColors.line

    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ResumeBuilderFormSection(
    formState: ResumeFormState,
    rewritingJobIndex: Int?,
    onUpdateForm: ((ResumeFormState) -> ResumeFormState) -> Unit,
    onAddJob: () -> Unit,
    onUpdateJob: (Int, (JobEntry) -> JobEntry) -> Unit,
    onRemoveJob: (Int) -> Unit,
    onRewriteJobWithAi: (Int) -> Unit,
    onUndoJobAi: (Int) -> Unit,
    onOpenExportTutorial: () -> Unit,
    onDownloadPdf: () -> Unit,
    onPrintPdf: () -> Unit,
    onDownloadTxt: () -> Unit,
    onCopyPlainText: () -> Unit,
    onSharePlainText: () -> Unit,
    onCheckScore: () -> Unit,
    onSaveDraft: () -> Unit,
    onStartOver: () -> Unit,
    showInlinePaperPreview: Boolean
) {
    val atsColors = LocalAtsColors.current
    var copyFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(copyFeedback) {
        if (copyFeedback) {
            delay(1500)
            copyFeedback = false
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Full Name
        FormFieldLabel(boldText = stringResource(R.string.label_full_name))
        ResumeTextField(
            value = formState.fullName,
            onValueChange = { value -> onUpdateForm { it.copy(fullName = value) } },
            placeholder = "e.g. Alex Morgan",
            singleLine = true,
            testTag = "input_full_name"
        )

        // Target Job Title
        FormFieldLabel(boldText = stringResource(R.string.label_target_title))
        ResumeTextField(
            value = formState.targetTitle,
            onValueChange = { value -> onUpdateForm { it.copy(targetTitle = value) } },
            placeholder = "e.g. Farm Worker, Retail Supervisor",
            singleLine = true,
            testTag = "input_target_title"
        )

        // Email & Phone
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isTwoColumn = maxWidth >= 420.dp
            if (isTwoColumn) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        FormFieldLabel(boldText = stringResource(R.string.label_email))
                        ResumeTextField(
                            value = formState.email,
                            onValueChange = { value -> onUpdateForm { it.copy(email = value) } },
                            placeholder = "name@email.com",
                            keyboardType = KeyboardType.Email,
                            singleLine = true,
                            testTag = "input_email"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        FormFieldLabel(boldText = stringResource(R.string.label_phone))
                        ResumeTextField(
                            value = formState.phone,
                            onValueChange = { value -> onUpdateForm { it.copy(phone = value) } },
                            placeholder = "+1 (604) 555-0199",
                            keyboardType = KeyboardType.Phone,
                            singleLine = true,
                            testTag = "input_phone"
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    FormFieldLabel(boldText = stringResource(R.string.label_email))
                    ResumeTextField(
                        value = formState.email,
                        onValueChange = { value -> onUpdateForm { it.copy(email = value) } },
                        placeholder = "name@email.com",
                        keyboardType = KeyboardType.Email,
                        singleLine = true,
                        testTag = "input_email"
                    )
                    FormFieldLabel(boldText = stringResource(R.string.label_phone))
                    ResumeTextField(
                        value = formState.phone,
                        onValueChange = { value -> onUpdateForm { it.copy(phone = value) } },
                        placeholder = "+1 (604) 555-0199",
                        keyboardType = KeyboardType.Phone,
                        singleLine = true,
                        testTag = "input_phone"
                    )
                }
            }
        }

        // Location & LinkedIn
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isTwoColumn = maxWidth >= 420.dp
            if (isTwoColumn) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        FormFieldLabel(boldText = stringResource(R.string.label_location))
                        ResumeTextField(
                            value = formState.location,
                            onValueChange = { value -> onUpdateForm { it.copy(location = value) } },
                            placeholder = "e.g. Calgary, AB",
                            singleLine = true,
                            testTag = "input_location"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        FormFieldLabel(boldText = "LinkedIn", hintText = "(optional)")
                        ResumeTextField(
                            value = formState.linkedin,
                            onValueChange = { value -> onUpdateForm { it.copy(linkedin = value) } },
                            placeholder = "linkedin.com/in/yourname",
                            singleLine = true,
                            testTag = "input_linkedin"
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    FormFieldLabel(boldText = stringResource(R.string.label_location))
                    ResumeTextField(
                        value = formState.location,
                        onValueChange = { value -> onUpdateForm { it.copy(location = value) } },
                        placeholder = "e.g. Calgary, AB",
                        singleLine = true,
                        testTag = "input_location"
                    )
                    FormFieldLabel(boldText = "LinkedIn", hintText = "(optional)")
                    ResumeTextField(
                        value = formState.linkedin,
                        onValueChange = { value -> onUpdateForm { it.copy(linkedin = value) } },
                        placeholder = "linkedin.com/in/yourname",
                        singleLine = true,
                        testTag = "input_linkedin"
                    )
                }
            }
        }

        // Summary
        FormFieldLabel(boldText = "Summary", hintText = "(2 to 3 lines)")
        ResumeTextField(
            value = formState.summary,
            onValueChange = { value -> onUpdateForm { it.copy(summary = value) } },
            placeholder = "Briefly describe your years of experience, core strengths, and target role.",
            minLines = 3,
            maxLines = 6,
            testTag = "input_summary"
        )

        // Jobs Fieldsets with Roman Urdu / Urdu / Hindi / English -> English AI Rewrite & Undo AI
        formState.jobs.forEachIndexed { index, job ->
            Spacer(modifier = Modifier.height(14.dp))
            JobFieldsetCard(
                index = index,
                job = job,
                isRewriting = rewritingJobIndex == index,
                canRemove = formState.jobs.size > 1,
                onUpdateJob = { transform -> onUpdateJob(index, transform) },
                onRewriteWithAi = { onRewriteJobWithAi(index) },
                onUndoAi = { onUndoJobAi(index) },
                onRemoveJob = { onRemoveJob(index) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SecondaryActionButton(
            text = stringResource(R.string.btn_add_job),
            icon = Icons.Default.Add,
            onClick = onAddJob,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_job_button")
        )

        // Education
        FormFieldLabel(boldText = "Education", hintText = "(one per line: diploma, school, year)")
        ResumeTextField(
            value = formState.education,
            onValueChange = { value -> onUpdateForm { it.copy(education = value) } },
            placeholder = "High School Diploma, Central Secondary School, 2020",
            minLines = 3,
            maxLines = 6,
            testTag = "input_education"
        )

        // Skills (Never auto-added)
        FormFieldLabel(boldText = "Skills", hintText = "(separate with commas — only list skills you truly have)")
        ResumeTextField(
            value = formState.skills,
            onValueChange = { value -> onUpdateForm { it.copy(skills = value) } },
            placeholder = "Type only skills you genuinely possess (e.g. Inventory Control, Customer Service)",
            minLines = 3,
            maxLines = 5,
            testTag = "input_skills"
        )

        // Certificates (Strict Canadian Anti-Misrepresentation Rule)
        FormFieldLabel(
            boldText = "Certificates",
            hintText = "(one per line — NEVER list certificates you do not hold)"
        )
        ResumeTextField(
            value = formState.certificates,
            onValueChange = { value -> onUpdateForm { it.copy(certificates = value) } },
            placeholder = "Leave blank if none, or type only certificates you actually hold",
            minLines = 3,
            maxLines = 6,
            testTag = "input_certificates"
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Export Options & Actions Section (Requirement C)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Export Options (100% ATS Ready)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(
                    onClick = onOpenExportTutorial,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("open_export_tutorial_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "How to Export for 100% ATS?",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            PrimaryGoButton(
                text = stringResource(R.string.btn_download_pdf),
                icon = Icons.Default.PictureAsPdf,
                onClick = onDownloadPdf,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("download_pdf_button")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryActionButton(
                    text = if (copyFeedback) stringResource(R.string.btn_copied) else stringResource(R.string.btn_copy_plain_text),
                    icon = Icons.Default.ContentCopy,
                    onClick = {
                        onCopyPlainText()
                        copyFeedback = true
                    },
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("copy_plain_text_button")
                )
                SecondaryActionButton(
                    text = stringResource(R.string.btn_download_txt),
                    icon = Icons.Default.Download,
                    onClick = onDownloadTxt,
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("download_txt_button")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryActionButton(
                    text = "Print / Save as PDF",
                    icon = Icons.Default.Print,
                    onClick = onPrintPdf,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("print_pdf_button")
                )
                SecondaryActionButton(
                    text = "Share Text",
                    icon = Icons.Default.Share,
                    onClick = onSharePlainText,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_plain_text_button")
                )
            }

            SecondaryActionButton(
                text = stringResource(R.string.btn_check_this_score),
                icon = Icons.Default.Analytics,
                onClick = onCheckScore,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("check_builder_score_button")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryActionButton(
                    text = stringResource(R.string.btn_save_draft),
                    icon = Icons.Default.Save,
                    onClick = onSaveDraft,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_draft_button")
                )
                SecondaryActionButton(
                    text = stringResource(R.string.btn_start_over),
                    icon = Icons.Default.Refresh,
                    onClick = onStartOver,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("start_over_button")
                )
            }
        }

        if (showInlinePaperPreview) {
            Spacer(modifier = Modifier.height(26.dp))
            Text(
                text = stringResource(R.string.section_preview),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            PaperPreviewCard(formState = formState)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.canadian_resume_note),
                style = MaterialTheme.typography.bodySmall,
                color = atsColors.muted
            )
        }
    }
}

@Composable
private fun JobFieldsetCard(
    index: Int,
    job: JobEntry,
    isRewriting: Boolean,
    canRemove: Boolean,
    onUpdateJob: ((JobEntry) -> JobEntry) -> Unit,
    onRewriteWithAi: () -> Unit,
    onUndoAi: () -> Unit,
    onRemoveJob: () -> Unit
) {
    val atsColors = LocalAtsColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("job_fieldset_$index"),
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, atsColors.line)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "Job ${index + 1}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            FormFieldLabel(boldText = stringResource(R.string.label_job_title))
            ResumeTextField(
                value = job.title,
                onValueChange = { value -> onUpdateJob { it.copy(title = value) } },
                placeholder = "e.g. General Farm Worker",
                singleLine = true,
                testTag = "input_job_title_$index"
            )

            FormFieldLabel(boldText = stringResource(R.string.label_employer))
            ResumeTextField(
                value = job.employer,
                onValueChange = { value -> onUpdateJob { it.copy(employer = value) } },
                placeholder = "e.g. Green Valley Farms, Abbotsford, BC",
                singleLine = true,
                testTag = "input_job_employer_$index"
            )

            FormFieldLabel(boldText = "Dates", hintText = "(e.g. Jan 2021 - Present)")
            ResumeTextField(
                value = job.dates,
                onValueChange = { value -> onUpdateJob { it.copy(dates = value) } },
                placeholder = "Jan 2021 - Present",
                singleLine = true,
                testTag = "input_job_dates_$index"
            )

            FormFieldLabel(
                boldText = "What you did",
                hintText = "(Urdu, Hindi, Roman Urdu, or English — one point per line)"
            )
            ResumeTextField(
                value = job.bullets,
                onValueChange = { value -> onUpdateJob { it.copy(bullets = value) } },
                placeholder = "e.g. Main ne greenhouse mein rozana 400 boxes pack kiye, ya English mein likhein...",
                minLines = 4,
                maxLines = 8,
                testTag = "input_job_bullets_$index"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Requirement D: "Rewrite in English with AI" + "Undo AI" under each experience item
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onRewriteWithAi,
                    enabled = !isRewriting,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rewrite_ai_button_$index")
                ) {
                    if (isRewriting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Rewriting in English...",
                            style = MaterialTheme.typography.labelLarge
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.btn_rewrite_ai),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                if (job.previousBulletsBeforeAi != null) {
                    SecondaryActionButton(
                        text = stringResource(R.string.btn_undo_ai),
                        icon = Icons.AutoMirrored.Filled.Undo,
                        onClick = onUndoAi,
                        modifier = Modifier.testTag("undo_ai_button_$index")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.ai_privacy_note),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = atsColors.muted
            )

            if (canRemove) {
                Spacer(modifier = Modifier.height(10.dp))
                SecondaryActionButton(
                    text = stringResource(R.string.btn_remove_job),
                    icon = Icons.Default.DeleteOutline,
                    onClick = onRemoveJob,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("remove_job_button_$index")
                )
            }
        }
    }
}

/**
 * Requirement A & B: Job Description Matcher (like Jobscan & SkillSyncer)
 * + 32 Formatting & Readability Checks with ✅ / ❌.
 */
@Composable
private fun JobMatcherSection(
    cvText: String,
    jdText: String,
    errorMessage: String,
    analysisResult: com.example.domain.AtsAnalysisResult?,
    onCvTextChange: (String) -> Unit,
    onJdTextChange: (String) -> Unit,
    onRunCheck: () -> Unit,
    onUploadFileClick: () -> Unit,
    onUseBuilderResume: () -> Unit,
    onCopyMissingKeywords: (String) -> Unit
) {
    val atsColors = LocalAtsColors.current
    var missingKeywordsCopied by remember { mutableStateOf(false) }

    LaunchedEffect(missingKeywordsCopied) {
        if (missingKeywordsCopied) {
            delay(1500)
            missingKeywordsCopied = false
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Quick Import / Sync Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SecondaryActionButton(
                text = "Upload .txt / .docx / .pdf",
                icon = Icons.Default.UploadFile,
                onClick = onUploadFileClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("upload_resume_file_button")
            )
            SecondaryActionButton(
                text = "Sync from Build tab",
                icon = Icons.Default.Description,
                onClick = onUseBuilderResume,
                modifier = Modifier
                    .weight(0.95f)
                    .testTag("use_builder_resume_button")
            )
        }

        // Two Text Fields: Paste Job Description (left on wide screen / first) & Current Resume Text (right / second)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isSideBySide = maxWidth >= 680.dp
            if (isSideBySide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        FormFieldLabel(
                            boldText = stringResource(R.string.label_job_description),
                            hintText = "(left)"
                        )
                        ResumeTextField(
                            value = jdText,
                            onValueChange = onJdTextChange,
                            placeholder = stringResource(R.string.placeholder_jd),
                            minLines = 9,
                            maxLines = 14,
                            testTag = "input_checker_jd"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        FormFieldLabel(
                            boldText = "Current Resume Text",
                            hintText = "(auto-filled from Build tab)"
                        )
                        ResumeTextField(
                            value = cvText,
                            onValueChange = onCvTextChange,
                            placeholder = stringResource(R.string.placeholder_cv),
                            minLines = 9,
                            maxLines = 14,
                            testTag = "input_checker_cv"
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    FormFieldLabel(
                        boldText = stringResource(R.string.label_job_description),
                        hintText = "(adds honest keyword overlap)"
                    )
                    ResumeTextField(
                        value = jdText,
                        onValueChange = onJdTextChange,
                        placeholder = stringResource(R.string.placeholder_jd),
                        minLines = 6,
                        maxLines = 12,
                        testTag = "input_checker_jd"
                    )

                    FormFieldLabel(
                        boldText = "Current Resume Text",
                        hintText = "(auto-filled from Build tab)"
                    )
                    ResumeTextField(
                        value = cvText,
                        onValueChange = onCvTextChange,
                        placeholder = stringResource(R.string.placeholder_cv),
                        minLines = 7,
                        maxLines = 14,
                        testTag = "input_checker_cv"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        PrimaryGoButton(
            text = stringResource(R.string.btn_check_my_score),
            icon = Icons.Default.CheckCircle,
            onClick = onRunCheck,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("check_my_score_button")
        )

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = atsColors.bad,
                modifier = Modifier.testTag("checker_error_message")
            )
        }

        AnimatedVisibility(
            visible = analysisResult != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (analysisResult != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ats_results_section")
                ) {
                    AtsScoreHeader(result = analysisResult)

                    Spacer(modifier = Modifier.height(8.dp))

                    AtsCategoryBarsSection(bars = analysisResult.bars)

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.section_fix_first),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    AtsCheckListSection(items = analysisResult.fixFirst, useCheckmarkEmoji = false)

                    val kwMatch = analysisResult.keywordMatch
                    if (kwMatch != null && (kwMatch.miss.isNotEmpty() || kwMatch.hit.isNotEmpty())) {
                        MissingKeywordsSection(
                            keywordMatch = kwMatch,
                            isCopied = missingKeywordsCopied,
                            onCopyMissing = {
                                val textToCopy = kwMatch.miss.joinToString(", ") { it.display }
                                onCopyMissingKeywords(textToCopy)
                                missingKeywordsCopied = true
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(26.dp))
                    Text(
                        text = "Formatting & Readability Checklist (${analysisResult.formattingChecklist.size} Checks)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Checks for tables, 2 columns, graphics, headers/footers, photo, DOB, marital status, skill bars, contact info, clear section headings, and date format consistency.",
                        style = MaterialTheme.typography.bodySmall,
                        color = atsColors.muted,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    AtsCheckListSection(
                        items = analysisResult.allChecks,
                        useCheckmarkEmoji = true,
                        modifier = Modifier.testTag("formatting_readability_checklist")
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.ats_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = atsColors.muted
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedDraftsSection(
    drafts: List<ResumeDraft>,
    activeDraftId: Long,
    onLoadDraft: (ResumeDraft) -> Unit,
    onDeleteDraft: (Long) -> Unit,
    onStartNewDraft: () -> Unit
) {
    val atsColors = LocalAtsColors.current
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_drafts_section")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Saved Resume Drafts",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Stored 100% privately on your device using Room Database.",
                    style = MaterialTheme.typography.bodySmall,
                    color = atsColors.muted
                )
            }
            SecondaryActionButton(
                text = "+ New blank",
                icon = Icons.Default.Add,
                onClick = onStartNewDraft,
                modifier = Modifier.testTag("new_blank_draft_button")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (drafts.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = atsColors.card,
                border = BorderStroke(1.dp, atsColors.line)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdded,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No saved drafts yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Start typing in the Build resume tab—your progress saves automatically on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = atsColors.muted
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                drafts.forEach { draft ->
                    val isCurrent = draft.id == activeDraftId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("draft_card_${draft.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = atsColors.card),
                        border = BorderStroke(
                            width = if (isCurrent) 1.5.dp else 1.dp,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else atsColors.line
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = draft.draftTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Updated ${dateFormat.format(Date(draft.updatedAt))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = atsColors.muted
                                    )
                                }

                                if (draft.lastAtsScore != null) {
                                    val scoreColor = colorForLevel(AtsAnalyzer.levelForScore(draft.lastAtsScore))
                                    Surface(
                                        shape = CircleShape,
                                        color = scoreColor.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, scoreColor)
                                    ) {
                                        Text(
                                            text = "Match ${draft.lastAtsScore}%",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = scoreColor,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SecondaryActionButton(
                                    text = if (isCurrent) "Continue editing" else "Open in builder",
                                    icon = Icons.Default.FolderOpen,
                                    onClick = { onLoadDraft(draft) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_draft_${draft.id}")
                                )
                                SecondaryActionButton(
                                    text = "Delete",
                                    icon = Icons.Default.DeleteOutline,
                                    onClick = { onDeleteDraft(draft.id) },
                                    modifier = Modifier.testTag("delete_draft_${draft.id}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormFieldLabel(
    boldText: String,
    hintText: String? = null
) {
    val atsColors = LocalAtsColors.current
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)) {
                append(boldText)
            }
            if (!hintText.isNullOrBlank()) {
                append(" ")
                withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = atsColors.muted)) {
                    append(hintText)
                }
            }
        },
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
    )
}

@Composable
private fun ResumeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 10,
    keyboardType: KeyboardType = KeyboardType.Text,
    testTag: String = ""
) {
    val atsColors = LocalAtsColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = atsColors.muted.copy(alpha = 0.65f)
            )
        },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default
        ),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = atsColors.card,
            unfocusedContainerColor = atsColors.card,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = atsColors.line,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    )
}

@Composable
private fun PrimaryGoButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun SecondaryActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val atsColors = LocalAtsColors.current
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, atsColors.line),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = atsColors.card,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
