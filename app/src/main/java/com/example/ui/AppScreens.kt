package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.AppDao
import com.example.data.AppliedJobsEntity
import com.example.data.SharedPrefsHelper
import com.example.data.UserProfileEntity
import com.example.data.VaultEntity
import com.example.domain.GeminiRewriteService
import com.example.domain.Job
import com.example.domain.ResumeTailorEngine
import com.example.domain.TailorResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val BgColor = Color(0xFF080812)
val PanelColor = Color(0xFF151526)
val PurpleAccent = Color(0xFF9B5CFF)
val CyanAccent = Color(0xFF54D8FF)
val GreenSuccess = Color(0xFF00C853)
val RedError = Color(0xFFD50000)

@Composable
fun SettingsScreen(sharedPrefsHelper: SharedPrefsHelper) {
    var geminiKey by remember { mutableStateOf(sharedPrefsHelper.getGeminiKey()) }
    var adzunaAppId by remember { mutableStateOf(sharedPrefsHelper.getAdzunaAppId()) }
    var adzunaKey by remember { mutableStateOf(sharedPrefsHelper.getAdzunaKey()) }
    var gmailToken by remember { mutableStateOf(sharedPrefsHelper.getGmailToken()) }
    var saveBanner by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        Text(
            text = "Settings & API Keys",
            color = CyanAccent,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Keys are encrypted on-device using EncryptedSharedPreferences (AES-256 GCM).",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = geminiKey,
            onValueChange = {
                geminiKey = it
                sharedPrefsHelper.saveGeminiKey(it)
                saveBanner = "Saved Gemini API Key"
            },
            label = { Text("Gemini API Key", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = PurpleAccent) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().testTag("setting_gemini_key")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = adzunaAppId,
            onValueChange = {
                adzunaAppId = it
                sharedPrefsHelper.saveAdzunaAppId(it)
                saveBanner = "Saved Adzuna App ID"
            },
            label = { Text("Adzuna App ID (Optional)", color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().testTag("setting_adzuna_id")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = adzunaKey,
            onValueChange = {
                adzunaKey = it
                sharedPrefsHelper.saveAdzunaKey(it)
                saveBanner = "Saved Adzuna API Key"
            },
            label = { Text("Adzuna API Key (Optional)", color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().testTag("setting_adzuna_key")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = gmailToken,
            onValueChange = {
                gmailToken = it
                sharedPrefsHelper.saveGmailToken(it)
                saveBanner = "Saved Gmail OAuth Token"
            },
            label = { Text("Gmail OAuth Token / App Password", color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().testTag("setting_gmail_token")
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (saveBanner != null) {
            Text(saveBanner!!, color = GreenSuccess, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Designed & Developed by Dayaram Chouhan ❤️",
            color = Color(0xFFFF2092),
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = "ATS Optimized for Overseas Jobs (Canada, Australia, NZ)",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ResumeScreen(
    baseResumeText: String,
    sharedPrefsHelper: SharedPrefsHelper
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val tailorEngine = remember { ResumeTailorEngine(context, sharedPrefsHelper) }

    var jobDesc by remember { mutableStateOf("") }
    var baseResumeInput by remember { mutableStateOf(baseResumeText) }
    var tailorResult by remember { mutableStateOf<TailorResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var pdfPath by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(16.dp)
            .testTag("resume_screen")
    ) {
        Text("ATS Resume Builder & Tailor", color = CyanAccent, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = baseResumeInput,
            onValueChange = { baseResumeInput = it },
            label = { Text("Candidate Base Resume Text", color = Color.Gray) },
            minLines = 4,
            maxLines = 6,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().testTag("input_base_resume")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = jobDesc,
            onValueChange = { jobDesc = it },
            label = { Text("Paste Job Description", color = Color.Gray) },
            minLines = 4,
            maxLines = 6,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().testTag("input_tailor_jd")
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                if (baseResumeInput.isBlank()) return@Button
                isLoading = true
                coroutineScope.launch {
                    val result = tailorEngine.generateTailoredResume(baseResumeInput, jobDesc)
                    tailorResult = result
                    val path = tailorEngine.generatePdf(result.tailoredResumeText, "Tailored_ATS_Resume")
                    pdfPath = path
                    isLoading = false
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
            modifier = Modifier.fillMaxWidth().testTag("tailor_resume_button")
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tailoring Resume with SANAM...")
            } else {
                Icon(Icons.Default.Description, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tailor Resume & Generate PDF")
            }
        }

        if (tailorResult != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PanelColor,
                border = BorderStroke(1.dp, GreenSuccess),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gap Analysis (Roman Urdu)", color = CyanAccent, style = MaterialTheme.typography.titleSmall)
                        Text("Match: ${tailorResult!!.estimatedMatchAfter}", color = GreenSuccess, style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(tailorResult!!.gapAnalysisUrdu, color = Color.White, style = MaterialTheme.typography.bodyMedium)

                    if (pdfPath != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = GreenSuccess)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF Generated: ${File(pdfPath!!).name}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackerScreen(dao: AppDao) {
    val appliedJobs by dao.getAllAppliedJobs().collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val now = System.currentTimeMillis()
    val twentyEightDaysMs = 28L * 24 * 60 * 60 * 1000

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(16.dp)
            .testTag("tracker_screen")
    ) {
        Text("Applied Jobs Memory", color = CyanAccent, style = MaterialTheme.typography.headlineMedium)
        Text("Track application history and follow-up status", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(14.dp))

        if (appliedJobs.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PanelColor,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Work, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No applied jobs tracked yet", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text("Apply to jobs from Home or Speech commands to log history.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(appliedJobs) { job ->
                    val isOld = (now - job.dateApplied) >= twentyEightDaysMs && job.status == "Applied"
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = PanelColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(job.role, color = CyanAccent, style = MaterialTheme.typography.titleMedium)
                                    Text(job.company, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                }
                                AssistChip(
                                    onClick = {},
                                    label = { Text(job.status, color = GreenSuccess) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = BgColor)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Applied: ${dateFormat.format(Date(job.dateApplied))}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

                            if (isOld) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { /* Trigger Follow Up Email */ },
                                    border = BorderStroke(1.dp, RedError),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError)
                                ) {
                                    Text("28+ days old • Follow-up bheju?")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VisualLensScreen(onSearchDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var recognizedText by remember { mutableStateOf("Point camera at job poster / ad") }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> }

    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(16.dp)
            .testTag("visual_lens_screen")
    ) {
        Text("SANAM Vision • Visual Lens", color = CyanAccent, style = MaterialTheme.typography.headlineMedium)
        Text("Scan printed job posters or ad screens with camera OCR", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(PanelColor)
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val capture = ImageCapture.Builder().build()
                        imageCapture = capture
                        val selector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                        } catch (e: Exception) { }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = PanelColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = recognizedText,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                val capture = imageCapture ?: return@Button
                val file = File(context.cacheDir, "lens_ocr_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                capture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
                            val inputImage = InputImage.fromBitmap(bitmap, 0)
                            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                            recognizer.process(inputImage)
                                .addOnSuccessListener { visionText ->
                                    val text = visionText.text
                                    recognizedText = text.ifBlank { "No text detected." }
                                    if (text.isNotBlank()) {
                                        val firstLine = text.lines().firstOrNull { it.isNotBlank() } ?: text
                                        onSearchDetected(firstLine)
                                    }
                                }
                        }
                        override fun onError(exception: ImageCaptureException) {
                            recognizedText = "Camera capture error."
                        }
                    }
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().testTag("scan_poster_button")
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan Job Poster with Camera")
        }
    }
}

@Composable
fun InterviewPrepScreen(
    sharedPrefsHelper: SharedPrefsHelper,
    tts: TextToSpeech?,
    onStartVoiceListen: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var answerInput by remember { mutableStateOf("") }
    var feedbackText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    val questions = remember {
        listOf(
            "Tell me about yourself and your experience in Farm Work or Trades.",
            "How do you handle outdoor physical work in cold weather conditions?",
            "What do you know about WHMIS and safety guidelines on Canadian job sites?",
            "Why do you want to work in Canada or abroad?"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(16.dp)
            .testTag("interview_prep_screen")
    ) {
        Text("SANAM Interview Coach", color = CyanAccent, style = MaterialTheme.typography.headlineMedium)
        Text("Mock Interview with Roman Urdu voice feedback", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PanelColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Question ${currentQuestionIndex + 1} of ${questions.size}", color = PurpleAccent, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(questions[currentQuestionIndex], color = Color.White, style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            tts?.speak(questions[currentQuestionIndex], TextToSpeech.QUEUE_FLUSH, null, null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                    ) {
                        Text("🔊 Read Question")
                    }
                    OutlinedButton(onClick = onStartVoiceListen) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = CyanAccent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Speak Answer", color = CyanAccent)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = answerInput,
            onValueChange = { answerInput = it },
            label = { Text("Your Answer (Roman Urdu or English)", color = Color.Gray) },
            minLines = 4,
            maxLines = 6,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().testTag("input_interview_answer")
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val apiKey = sharedPrefsHelper.getGeminiKey()
                if (answerInput.isBlank()) return@Button
                isThinking = true
                coroutineScope.launch {
                    val prompt = "Question: ${questions[currentQuestionIndex]}\nCandidate Answer: $answerInput\nGive 2 short lines of Roman Urdu feedback and a score out of 10."
                    val result = GeminiRewriteService.rewriteExperienceToEnglishBullets(apiKey, "Mock Interview", prompt)
                    val feedback = result.getOrDefault("Aap ka jawab achha tha. Detail thodi aur add karein. Score: 8/10")
                    feedbackText = feedback
                    tts?.speak(feedback.replace(Regex("[^a-zA-Z0-9 ]"), ""), TextToSpeech.QUEUE_FLUSH, null, null)
                    isThinking = false
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
            modifier = Modifier.fillMaxWidth().testTag("get_feedback_button")
        ) {
            if (isThinking) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
            } else {
                Icon(Icons.Default.Psychology, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Get SANAM Feedback")
            }
        }

        if (feedbackText.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = PanelColor,
                border = BorderStroke(1.dp, CyanAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(feedbackText, color = Color.White, modifier = Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
fun VaultScreen(dao: AppDao) {
    val vaultDocs by dao.getVaultDocs().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val name = uri.lastPathSegment?.substringAfterLast("/") ?: "Document"
                val entity = VaultEntity(
                    documentName = name,
                    filePath = uri.toString(),
                    extractedText = "Document vaulted securely on device.",
                    uploadDate = System.currentTimeMillis()
                )
                dao.insertVaultDoc(entity)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(16.dp)
            .testTag("vault_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Document Vault", color = CyanAccent, style = MaterialTheme.typography.headlineMedium)
                Text("Store certificates, passport copies & licenses safely", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
            ) {
                Text("+ Upload")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (vaultDocs.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PanelColor,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Vault is empty", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text("Upload your documents to store them on-device.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(vaultDocs) { doc ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = PanelColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = CyanAccent)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doc.documentName, color = Color.White, style = MaterialTheme.typography.titleSmall)
                                Text("Uploaded: ${dateFormat.format(Date(doc.uploadDate))}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
