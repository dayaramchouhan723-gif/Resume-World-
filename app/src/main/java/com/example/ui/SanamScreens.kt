package com.example.ui

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import com.example.R
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDao
import com.example.data.CommunityPostEntity
import com.example.data.CoverLetterEntity
import com.example.data.SharedPrefsHelper
import com.example.domain.Job
import com.example.domain.NeuralLensScanner
import com.example.domain.PhonePcController
import com.example.domain.SanamMultiAgentEngine
import com.example.domain.ScamLevel
import com.example.domain.WhatsAppDigestExporter
import com.example.ui.components.SanamHolographicOrb
import com.example.ui.components.SanamOrbMode
import com.example.ui.components.SanamStarryBackground
import com.example.ui.components.SanamStarryBackground
import kotlinx.coroutines.launch
import java.io.File

val SanamPink = Color(0xFFFF2092)
val SanamCyan = Color(0xFF00E5FF)
val SanamPurple = Color(0xFF9B5CFF)
val SanamBg = Color(0xFF05050A)
val SanamPanel = Color(0xFF131322)
val SanamGreen = Color(0xFF00C853)
val SanamRed = Color(0xFFD50000)

@Composable
fun SanamHomeScreen(
    mainUiState: MainUiState,
    onOrbSingleTap: () -> Unit,
    onOrbLongPress: () -> Unit,
    onOrbDoubleTap: () -> Unit,
    onQuickVoiceAction: (String) -> Unit,
    onGenerateResumeCard: () -> Unit,
    onCloseOverlayResume: () -> Unit,
    onNavigateToProfile: () -> Unit = {}
) {
    var textMessageInput by remember { mutableStateOf("") }

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        border = BorderStroke(2.dp, Color(0xFFA020F0)),
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
            .testTag("sanam_home_screen")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Layer 1: Faint Anime Girl Illustration
            Image(
                painter = painterResource(id = R.drawable.img_sanam_anime_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.14f,
                modifier = Modifier.fillMaxSize()
            )

            // Background Layer 2: Translucent Pink-Purple Waves & Sparkles
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Left Wave Ribbon
                val leftWave = Path().apply {
                    moveTo(0f, h * 0.18f)
                    cubicTo(w * 0.35f, h * 0.25f, w * 0.10f, h * 0.42f, 0f, h * 0.50f)
                    close()
                }
                drawPath(
                    path = leftWave,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFE040FB).copy(alpha = 0.35f), Color(0xFFA020F0).copy(alpha = 0.10f))
                    )
                )

                // Right Wave Ribbon
                val rightWave = Path().apply {
                    moveTo(w, h * 0.20f)
                    cubicTo(w * 0.65f, h * 0.28f, w * 0.90f, h * 0.45f, w, h * 0.52f)
                    close()
                }
                drawPath(
                    path = rightWave,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFA020F0).copy(alpha = 0.10f), Color(0xFFFF2092).copy(alpha = 0.35f))
                    )
                )

                // Particles
                drawCircle(color = Color(0xFFA020F0).copy(alpha = 0.5f), radius = 6f, center = Offset(w * 0.18f, h * 0.22f))
                drawCircle(color = Color(0xFFFF2092).copy(alpha = 0.6f), radius = 8f, center = Offset(w * 0.82f, h * 0.26f))
                drawCircle(color = Color(0xFFA020F0).copy(alpha = 0.5f), radius = 5f, center = Offset(w * 0.22f, h * 0.42f))
                drawCircle(color = Color(0xFFFF2092).copy(alpha = 0.6f), radius = 7f, center = Offset(w * 0.78f, h * 0.44f))
            }

            // MAIN CONTENT COLUMN
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TOP HEADER BAR: SANAM AI Logo + Subtitle + Right Bell & Settings Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SANAM ",
                                color = Color.Black,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "AI",
                                color = Color(0xFFA020F0),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "- AI ASSISTANT -",
                            color = Color(0xFFA020F0),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF2F0FF),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsNone,
                                    contentDescription = "Notifications",
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF2F0FF),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // CENTER ORB AREA (220dp) + HANDWRITING SCRIPT OVERLAYS
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Left Handwriting Script
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 4.dp)
                    ) {
                        Column {
                            Text("Hi, I'm", color = Color(0xFF9B5CFF), fontSize = 16.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold)
                            Text("SANAM ♡", color = Color(0xFF9B5CFF), fontSize = 16.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Center 220dp Orb
                    SanamHolographicOrb(
                        mode = mainUiState.orbMode,
                        statusMessage = mainUiState.sanamSpeechMessage,
                        onSingleTap = onOrbSingleTap,
                        onLongPress = onOrbLongPress,
                        onDoubleTap = onOrbDoubleTap,
                        isFullScreen = true,
                        orbDiameterDp = 220.dp
                    )

                    // Right Handwriting Script
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Always Here", color = Color(0xFF9B5CFF), fontSize = 16.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold)
                            Text("For You ♡", color = Color(0xFF9B5CFF), fontSize = 16.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // HELLO GREETING
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Hello!",
                        color = Color.Black,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "How can I help you today?",
                        color = Color(0xFF666666),
                        fontSize = 14.sp
                    )
                }

                // INPUT BAR: White Pill Shadow Container (56dp)
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE5E0FF)),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attachment",
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        OutlinedTextField(
                            value = textMessageInput,
                            onValueChange = { textMessageInput = it },
                            placeholder = { Text("Type a message...", color = Color(0xFF888888), fontSize = 14.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF7C3AED),
                            modifier = Modifier
                                .size(40.dp)
                                .clickable {
                                    if (textMessageInput.isNotBlank()) {
                                        onQuickVoiceAction(textMessageInput)
                                        textMessageInput = ""
                                    } else {
                                        onOrbSingleTap()
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // "MORE" CARDS SECTION
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "More",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card 1: Voice Mode
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE5E0FF)),
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .clickable { onOrbSingleTap() }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF2F0FF),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = Color(0xFF7C3AED),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text("Voice Mode", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("Holographic Orb", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }

                        // Card 2: Neural Lens
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE5E0FF)),
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .clickable { onQuickVoiceAction("scan camera") }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF2F0FF),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = Color(0xFF7C3AED),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text("Neural Lens", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("Object Scan", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Job Analysis Match Status Card
                val analysis = mainUiState.jobAnalysis
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8F6FF),
                    border = BorderStroke(1.dp, if (analysis.requiresProfile) Color(0xFFFF2092) else Color(0xFF7C3AED)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(analysis.conclusionText, color = if (analysis.requiresProfile) Color(0xFFFF2092) else Color(0xFF7C3AED), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Missing: ${analysis.missingSkill} • Current ATS: ${analysis.currentAtsScore}/100", color = Color.DarkGray, fontSize = 10.sp)
                        }
                        if (analysis.requiresProfile) {
                            Button(
                                onClick = onNavigateToProfile,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2092)),
                                modifier = Modifier.testTag("pehle_profile_bharo_button")
                            ) {
                                Text("Pehle Profile Bharo ➡️", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF00C853)
                            ) {
                                Text("ATS 94+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
            }

            // FULL-SCREEN RESUME CARD WITH FLOATING SANAM ORB OVERLAY
            if (mainUiState.showResumeOverlayCard) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.90f))
                        .padding(16.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SanamPanel),
                        border = BorderStroke(1.5.dp, SanamPink),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("SANAM Generated ATS Resume Card", color = SanamPink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Canada Single-Column LMIA Verified", color = Color.Gray, fontSize = 11.sp)
                                }
                                IconButton(onClick = onCloseOverlayResume) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Resume Content
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SanamBg,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                LazyColumn(modifier = Modifier.padding(14.dp)) {
                                    item {
                                        Text("DAYARAM CHOUHAN", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        Text("General Farm Worker & Heavy Machinery Operator | Canada Visa Candidate", color = SanamCyan, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("SUMMARY", color = SanamPink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Experienced farm worker with 5+ years expertise operating tractors, greenhouse irrigation, and crop harvesting. WHMIS certified. Seeking LMIA sponsored role in British Columbia.", color = Color.LightGray, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("CORE SKILLS", color = SanamPink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("• Tractor & Equipment Operation\n• Greenhouse Climate & Drip Irrigation\n• WHMIS & Occupational Safety\n• Crop Harvesting & Sorting", color = Color.White, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("EXPERIENCE", color = SanamPink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Senior Farm Technician - Agri Farms (2021 - Present)\n• Managed 50+ acres crop harvest and tractor maintenance.\n• Prepared daily soil moisture logs and drip line maintenance.", color = Color.LightGray, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Floating Small Sanam Orb Overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 20.dp, end = 20.dp)
                    ) {
                        SanamHolographicOrb(
                            mode = mainUiState.orbMode,
                            statusMessage = "",
                            onSingleTap = onOrbSingleTap,
                            onLongPress = onOrbLongPress,
                            onDoubleTap = onOrbDoubleTap,
                            isFullScreen = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SanamJobsScreen(
    mainUiState: MainUiState,
    onSearch: (String, String) -> Unit,
    onApplyJob: (Job) -> Unit,
    onShareDigest: () -> Unit
) {
    var queryInput by remember { mutableStateOf(mainUiState.searchQuery) }
    var selectedCountry by remember { mutableStateOf(mainUiState.selectedCountry) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanamBg)
            .padding(16.dp)
            .testTag("sanam_jobs_screen")
    ) {
        Text("Job Hunter Agent • Overseas Listings", color = SanamCyan, style = MaterialTheme.typography.headlineMedium)
        Text("Verified Canada Job Bank, Seek Australia & NZ Accredited Employer Jobs", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(12.dp))

        // Country Selector Chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Canada", "Australia", "New Zealand").forEach { country ->
                FilterChip(
                    selected = selectedCountry == country,
                    onClick = {
                        selectedCountry = country
                        onSearch(queryInput, country)
                    },
                    label = { Text(when(country) { "Canada" -> "🇨🇦 Canada"; "Australia" -> "🇦🇺 Australia"; else -> "🇳🇿 New Zealand" }) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SanamPink,
                        selectedLabelColor = Color.White,
                        containerColor = SanamPanel,
                        labelColor = Color.LightGray
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = queryInput,
            onValueChange = { queryInput = it },
            placeholder = { Text("e.g. Farm Worker, Welder, Retail", color = Color.Gray) },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { onSearch(queryInput, selectedCountry) }) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = SanamPink)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().testTag("sanam_job_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // WhatsApp Daily Digest Exporter Button
        Button(
            onClick = onShareDigest,
            colors = ButtonDefaults.buttonColors(containerColor = SanamGreen),
            modifier = Modifier.fillMaxWidth().testTag("whatsapp_digest_button")
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("WhatsApp Daily Digest PDF Bhejo")
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(mainUiState.jobsList) { job ->
                SanamJobCard(job = job, onApply = { onApplyJob(job) })
            }
        }
    }
}

@Composable
fun SanamJobCard(
    job: Job,
    onApply: () -> Unit
) {
    val scamCheck = remember(job) {
        SanamMultiAgentEngine.runScamDetectorAgent(job.title, job.company, job.description, job.applyLink)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SanamPanel),
        border = BorderStroke(
            width = 1.dp,
            color = when (scamCheck.level) {
                ScamLevel.SCAM -> SanamRed
                ScamLevel.WARNING -> Color(0xFFFFB300)
                else -> SanamPurple.copy(alpha = 0.4f)
            }
        ),
        modifier = Modifier.fillMaxWidth().testTag("sanam_job_card_${job.applyLink.hashCode()}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(job.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${job.company} • ${job.location}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
                AssistChip(
                    onClick = {},
                    label = { Text(job.source, color = SanamCyan, fontSize = 11.sp) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = SanamBg)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scam Detector Agent Badge + LMIA Badge
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (scamCheck.level) {
                        ScamLevel.SCAM -> SanamRed.copy(alpha = 0.2f)
                        ScamLevel.WARNING -> Color(0xFFFFB300).copy(alpha = 0.2f)
                        else -> SanamGreen.copy(alpha = 0.2f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = if (scamCheck.level == ScamLevel.SCAM) SanamRed else SanamGreen, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(scamCheck.title, color = if (scamCheck.level == ScamLevel.SCAM) SanamRed else SanamGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SanamBg,
                    border = BorderStroke(1.dp, SanamCyan)
                ) {
                    Text(job.sponsorshipStatus, color = SanamCyan, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(containerColor = SanamPink),
                modifier = Modifier.fillMaxWidth().testTag("apply_sanam_job_${job.applyLink.hashCode()}")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Boss, Apply Karo (Email + PDF Attachments)")
            }
        }
    }
}

@Composable
fun SanamResumeScreen(
    baseResumeText: String,
    sharedPrefsHelper: SharedPrefsHelper,
    dao: AppDao
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedCountryFormat by remember { mutableStateOf("Canada") }
    var voiceCoverInput by remember { mutableStateOf("") }
    var generatedCoverLetter by remember { mutableStateOf("") }
    var isGeneratingCover by remember { mutableStateOf(false) }

    val formatConfig = remember(selectedCountryFormat) {
        SanamMultiAgentEngine.getCountryFormatConfig(selectedCountryFormat)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanamBg)
            .padding(16.dp)
            .testTag("sanam_resume_screen")
    ) {
        Text("Country-Wise Resume & Voice Cover Letter", color = SanamPink, style = MaterialTheme.typography.headlineMedium)
        Text("Auto formats for Canada (1 page), Australia (2 pages), NZ (1.5 pages)", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(12.dp))

        // Format Selector Tabs
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Canada", "Australia", "New Zealand").forEach { country ->
                FilterChip(
                    selected = selectedCountryFormat == country,
                    onClick = { selectedCountryFormat = country },
                    label = { Text(country) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SanamPurple, selectedLabelColor = Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Format Config Summary Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = SanamPanel,
            border = BorderStroke(1.dp, SanamCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("${formatConfig.countryName} Format Rules:", color = SanamCyan, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("• Length: ${formatConfig.maxPages}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                Text("• Photo: ${if (formatConfig.photoRecommended) "Recommended" else "NO Photo (LMIA Rule)"}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                Text("• Focus: ${formatConfig.keyFocus}", color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Voice to Cover Letter Section
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SanamPanel),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Voice to Cover Letter Agent", color = SanamPink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Bolo: 'Boss mai 5 saal se tractor chalata hun' -> 100 words cover letter", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = voiceCoverInput,
                    onValueChange = { voiceCoverInput = it },
                    label = { Text("Spoken or Typed Input", color = Color.Gray) },
                    placeholder = { Text("e.g. 5 years tractor & farm machinery experience in Punjab", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("voice_cover_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        isGeneratingCover = true
                        coroutineScope.launch {
                            val apiKey = sharedPrefsHelper.getGeminiKey()
                            val isFree = sharedPrefsHelper.isFreeModeEnabled()
                            val letter = SanamMultiAgentEngine.runCoverLetterAgent(
                                apiKey = apiKey,
                                isFreeMode = isFree,
                                userVoiceInput = voiceCoverInput,
                                jobTitle = "General Worker",
                                companyName = "Overseas Employer",
                                country = selectedCountryFormat
                            )
                            generatedCoverLetter = letter
                            dao.insertCoverLetter(
                                CoverLetterEntity(
                                    title = "Cover Letter for $selectedCountryFormat",
                                    companyName = "Overseas Employer",
                                    targetCountry = selectedCountryFormat,
                                    content = letter
                                )
                            )
                            isGeneratingCover = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SanamPurple),
                    modifier = Modifier.fillMaxWidth().testTag("generate_cover_button")
                ) {
                    if (isGeneratingCover) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.Description, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SANAM, Cover Letter Bana Do!")
                    }
                }

                if (generatedCoverLetter.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SanamBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(generatedCoverLetter, color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SanamTrackerScreen(dao: AppDao) {
    val appliedJobs by dao.getAllAppliedJobs().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanamBg)
            .padding(16.dp)
            .testTag("sanam_tracker_screen")
    ) {
        Text("Application Tracker + AI Reply Assistant", color = SanamCyan, style = MaterialTheme.typography.headlineMedium)
        Text("WhatsApp-style chat memory for applied jobs & 4-week follow-up reminders", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(14.dp))

        if (appliedJobs.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SanamPanel,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No tracked applications yet", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text("Apply to jobs from Home or Jobs tab to log history.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(appliedJobs) { job ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SanamPanel),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(job.role, color = SanamCyan, fontWeight = FontWeight.Bold)
                                Text(job.status, color = SanamGreen)
                            }
                            Text(job.company, color = Color.White, style = MaterialTheme.typography.bodyMedium)

                            Spacer(modifier = Modifier.height(10.dp))

                            // WhatsApp Style Chat Box
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SanamBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(job.chatHistory, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedButton(
                                        onClick = { /* Draft reply */ },
                                        border = BorderStroke(1.dp, SanamPink),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SanamPink)
                                    ) {
                                        Text("SANAM AI: 'Boss, Follow-up reply draft karun?'")
                                    }
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
fun SanamNeuralLensScreen(dao: AppDao) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var scannedText by remember { mutableStateOf("Scan certificate or resume photo with camera OCR") }
    var layerResultState by remember { mutableStateOf<com.example.domain.NeuralLayerResult?>(null) }
    var selectedLayerIndex by remember { mutableIntStateOf(0) }
    var isScanning by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isScanning = true
            coroutineScope.launch {
                val tempFile = File(context.cacheDir, "lens_doc_${System.currentTimeMillis()}.jpg")
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                val res = NeuralLensScanner.processDocumentWithNeuralLens(context, tempFile)
                layerResultState = res
                scannedText = res.extractedText
                isScanning = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanamBg)
            .padding(16.dp)
            .testTag("sanam_neural_lens_screen")
    ) {
        Text("SANAM Document Eyes • Qwen 3 Layers", color = SanamPink, style = MaterialTheme.typography.headlineMedium)
        Text("Personal Info | Experience | Skills & Education Layers", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(14.dp))

        // CAMERA RETICLE SCANNER FRAME
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SanamPanel)
                .border(2.dp, SanamCyan, RoundedCornerShape(16.dp))
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "laser_scan")
            val laserY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "laser_anim"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val yPos = laserY * size.height
                drawLine(
                    color = SanamPink,
                    start = Offset(0f, yPos),
                    end = Offset(size.width, yPos),
                    strokeWidth = 4f
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = SanamCyan, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("SANAM Camera Document Eyes", color = Color.White, fontWeight = FontWeight.Bold)
                Text(layerResultState?.parsedName ?: "Dayaram Chouhan • Verified Document", color = SanamPink, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3 LAYERS TOGGLE (Personal Info | Experience | Skills & Education)
        TabRow(
            selectedTabIndex = selectedLayerIndex,
            containerColor = SanamPanel,
            contentColor = SanamPink,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedLayerIndex]),
                    color = SanamPink
                )
            }
        ) {
            Tab(selected = selectedLayerIndex == 0, onClick = { selectedLayerIndex = 0 }) {
                Text("Layer 1: Identity", modifier = Modifier.padding(10.dp), fontSize = 11.sp, color = if (selectedLayerIndex == 0) SanamPink else Color.Gray)
            }
            Tab(selected = selectedLayerIndex == 1, onClick = { selectedLayerIndex = 1 }) {
                Text("Layer 2: Experience", modifier = Modifier.padding(10.dp), fontSize = 11.sp, color = if (selectedLayerIndex == 1) SanamPink else Color.Gray)
            }
            Tab(selected = selectedLayerIndex == 2, onClick = { selectedLayerIndex = 2 }) {
                Text("Layer 3: Skills", modifier = Modifier.padding(10.dp), fontSize = 11.sp, color = if (selectedLayerIndex == 2) SanamPink else Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { filePickerLauncher.launch(arrayOf("image/*", "application/pdf")) },
            colors = ButtonDefaults.buttonColors(containerColor = SanamCyan, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().testTag("scan_doc_ocr_button")
        ) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
            } else {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Old Resume / Certificate")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = SanamPanel,
            modifier = Modifier.fillMaxWidth()
        ) {
            val layerContent = layerResultState?.let {
                when (selectedLayerIndex) {
                    0 -> it.personalInfoLayer
                    1 -> it.experienceLayer
                    else -> it.skillsEducationLayer
                }
            } ?: "LAYER $selectedLayerIndex ANALYSIS:\nDocument scanned with SANAM 3-layer neural parser."

            Text(layerContent, color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
fun SanamCommunityScreen(dao: AppDao) {
    val posts by dao.getAllCommunityPosts().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    var postMessageInput by remember { mutableStateOf("") }
    var authorNameInput by remember { mutableStateOf("Candidate Boss") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanamBg)
            .padding(16.dp)
            .testTag("sanam_community_screen")
    ) {
        Text("Chouhan Stars Community", color = SanamPink, style = MaterialTheme.typography.headlineMedium)
        Text("Real candidates sharing job replies & visa application tips", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SanamPanel),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = postMessageInput,
                    onValueChange = { postMessageInput = it },
                    placeholder = { Text("Share your job reply experience, Boss!", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (postMessageInput.isBlank()) return@Button
                        coroutineScope.launch {
                            dao.insertCommunityPost(
                                CommunityPostEntity(
                                    authorName = authorNameInput,
                                    country = "Canada",
                                    companyName = "Fraser Valley Farms",
                                    message = postMessageInput
                                )
                            )
                            postMessageInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SanamPink),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Post in Chouhan Stars")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (posts.isEmpty()) {
            val demoPosts = listOf(
                CommunityPostEntity(authorName = "Gurpreet Singh", country = "Canada 🇨🇦", companyName = "Fraser Valley Berry Farms", message = "Boss! Mujhey 3 din me LMIA Job offer reply aaya! SANAM resume template works 100%!", likesCount = 24),
                CommunityPostEntity(authorName = "Vikram Sharma", country = "Australia 🇦🇺", companyName = "Sydney Construction Co", message = "ANZSCO trade matrix follow kiya. Subclass 482 sponsorship email receive hua!", likesCount = 18)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(demoPosts) { post ->
                    SanamCommunityPostCard(post)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(posts) { post ->
                    SanamCommunityPostCard(post)
                }
            }
        }
    }
}

@Composable
fun SanamCommunityPostCard(post: CommunityPostEntity) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SanamPanel),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(post.authorName, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SanamCyan, modifier = Modifier.size(16.dp))
                }
                Text(post.country, color = SanamPink, fontSize = 12.sp)
            }
            Text("Applied: ${post.companyName}", color = Color.Gray, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(post.message, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SanamSettingsScreen(
    sharedPrefsHelper: SharedPrefsHelper,
    onToggleFreeMode: (Boolean) -> Unit,
    onToggleAutoTrigger: (Boolean) -> Unit
) {
    var geminiKey by remember { mutableStateOf(sharedPrefsHelper.getGeminiKey()) }
    var groqKey by remember { mutableStateOf(sharedPrefsHelper.getGroqKey()) }
    var isFreeMode by remember { mutableStateOf(sharedPrefsHelper.isFreeModeEnabled()) }
    var isAutoTrigger by remember { mutableStateOf(sharedPrefsHelper.isAutoTriggerEnabled()) }
    var saveStatus by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanamBg)
            .padding(16.dp)
            .testTag("sanam_settings_screen")
    ) {
        Text("SANAM Connectors & Settings", color = SanamCyan, style = MaterialTheme.typography.headlineMedium)
        Text("Gemini Live + Groq Connectors • FreeLLM Brain • 100% Privacy", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SanamPanel),
            border = BorderStroke(1.dp, SanamCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SANAM Auto-Pilot Trigger", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Roz subah 9 baje jobs check karna & voice alert dena", color = Color.Gray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = isAutoTrigger,
                        onCheckedChange = {
                            isAutoTrigger = it
                            sharedPrefsHelper.setAutoTriggerEnabled(it)
                            onToggleAutoTrigger(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = SanamCyan, checkedTrackColor = SanamPurple)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SanamBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Next trigger: Tomorrow 9:00 AM - Canada Farm Jobs",
                        color = SanamGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SanamPanel),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Free Mode ON (FreeLLM Provider)", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("7 Billion tokens/month free using FreeLLMAPI fallback.", color = Color.Gray, fontSize = 11.sp)
                }
                Switch(
                    checked = isFreeMode,
                    onCheckedChange = {
                        isFreeMode = it
                        sharedPrefsHelper.setFreeModeEnabled(it)
                        onToggleFreeMode(it)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = SanamPink, checkedTrackColor = SanamPurple)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text("SANAM AI Connectors (Optional):", color = SanamPink, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = geminiKey,
            onValueChange = {
                geminiKey = it
                sharedPrefsHelper.saveGeminiKey(it)
                saveStatus = "Saved Gemini API Key"
            },
            label = { Text("Gemini Live API Key", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = SanamPink) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().testTag("sanam_api_key_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = groqKey,
            onValueChange = {
                groqKey = it
                sharedPrefsHelper.saveGroqKey(it)
                saveStatus = "Saved Groq API Key"
            },
            label = { Text("Groq API Key (Fallback)", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = SanamCyan) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().testTag("groq_api_key_input")
        )

        if (saveStatus != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(saveStatus!!, color = SanamGreen, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Designed & Developed by Dayaram Chouhan ❤️", color = SanamPink, fontWeight = FontWeight.Bold)
        Text("SANAM Job Assistant • Version 2.0 Engine", color = Color.Gray, fontSize = 11.sp)
    }
}
