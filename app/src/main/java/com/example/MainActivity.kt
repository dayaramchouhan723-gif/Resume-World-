package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.ResumeRepository
import com.example.data.SharedPrefsHelper
import com.example.domain.JobSearchService
import com.example.ui.MainViewModel
import com.example.ui.SanamBg
import com.example.ui.SanamCommunityScreen
import com.example.ui.SanamCyan
import com.example.ui.SanamHomeScreen
import com.example.ui.SanamJobsScreen
import com.example.ui.SanamNeuralLensScreen
import com.example.ui.SanamPanel
import com.example.ui.SanamPink
import com.example.ui.SanamResumeScreen
import com.example.ui.SanamSettingsScreen
import com.example.ui.SanamTab
import com.example.ui.SanamTrackerScreen
import com.example.ui.components.SanamOrbMode
import com.example.ui.theme.MyApplicationTheme
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        tts = TextToSpeech(this, this)

        com.example.domain.TriggerManager.scheduleDailyFollowUpCheck(applicationContext)

        val database = AppDatabase.getInstance(applicationContext)
        val dao = database.appDao()
        val repository = ResumeRepository(database.resumeDao())
        val sharedPrefsHelper = SharedPrefsHelper(applicationContext)
        val jobSearchService = JobSearchService(sharedPrefsHelper)

        setContent {
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModel.provideFactory(dao, sharedPrefsHelper, jobSearchService)
            )
            val resumeViewModel: com.example.ui.ResumeViewModel = viewModel(
                factory = com.example.ui.ResumeViewModel.provideFactory(repository)
            )

            val mainState by mainViewModel.uiState.collectAsStateWithLifecycle()
            val resumeState by resumeViewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(mainState.statusBanner) {
                mainState.statusBanner?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                }
            }

            // Speech Recognizer Activity Launcher
            val speechLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                    val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    if (!matches.isNullOrEmpty()) {
                        val spoken = matches[0]
                        mainViewModel.processVoiceCommand(this@MainActivity, spoken, tts)
                    } else {
                        mainViewModel.setOrbMode(SanamOrbMode.IDLE, "Boss, samajh nahi aaya! Phir se bolein.")
                    }
                } else {
                    mainViewModel.setOrbMode(SanamOrbMode.IDLE, "Hi Boss! Mai SANAM! Aapki Job Apply Assistant! Bolo kaunsi job apply karwani hai!")
                }
            }

            val startVoiceRecognition = {
                if (!isNetworkAvailable(this@MainActivity)) {
                    val offlineMsg = "Boss net ON karo, tabhi mai Canada jobs dhoondungi ❤️"
                    mainViewModel.setOrbMode(SanamOrbMode.MUTED, offlineMsg)
                    mainViewModel.speakText(tts, offlineMsg)
                } else {
                    mainViewModel.setOrbMode(SanamOrbMode.LISTENING, "Listening Boss... Bolo!")
                    try {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "SANAM Job Assistant - Speak command for Boss")
                        }
                        speechLauncher.launch(intent)
                    } catch (e: Exception) {
                        mainViewModel.setOrbMode(SanamOrbMode.IDLE, "Voice recognition unavailable on device")
                    }
                }
            }

            // AUTO WAKE UP + FIRST GREETING ON OPEN
            LaunchedEffect(isTtsInitialized) {
                if (isTtsInitialized) {
                    if (!isNetworkAvailable(this@MainActivity)) {
                        val offlineMsg = "Boss net ON karo, tabhi mai Canada jobs dhoondungi ❤️"
                        mainViewModel.setOrbMode(SanamOrbMode.MUTED, offlineMsg)
                        mainViewModel.speakText(tts, offlineMsg)
                    } else {
                        val welcomeMsg = "Hi Boss! Mai SANAM! Aapki Job Apply Assistant! Bolo kaunsi job apply karwani hai!"
                        mainViewModel.setOrbMode(SanamOrbMode.IDLE, welcomeMsg)
                        mainViewModel.speakText(tts, welcomeMsg)
                    }
                }
            }

            MyApplicationTheme(darkTheme = true) {
                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        SanamBottomNavigationDock(
                            selectedTab = mainState.selectedTab,
                            orbMode = mainState.orbMode,
                            onSelectTab = { mainViewModel.selectTab(it) },
                            onCenterOrbClick = startVoiceRecognition
                        )
                    },
                    containerColor = SanamBg
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(SanamBg)
                    ) {
                        when (mainState.selectedTab) {
                            SanamTab.HOME -> SanamHomeScreen(
                                mainUiState = mainState,
                                onOrbSingleTap = startVoiceRecognition,
                                onOrbLongPress = startVoiceRecognition,
                                onOrbDoubleTap = {
                                    tts?.stop()
                                    mainViewModel.setOrbMode(SanamOrbMode.MUTED, "Boss, Mute kar diya!")
                                },
                                onQuickVoiceAction = { action ->
                                    if (!isNetworkAvailable(this@MainActivity)) {
                                        val offlineMsg = "Boss net ON karo, tabhi mai Canada jobs dhoondungi ❤️"
                                        mainViewModel.setOrbMode(SanamOrbMode.MUTED, offlineMsg)
                                        mainViewModel.speakText(tts, offlineMsg)
                                    } else {
                                        mainViewModel.processVoiceCommand(this@MainActivity, action, tts)
                                    }
                                },
                                onGenerateResumeCard = {
                                    mainViewModel.showResumeOverlay(true)
                                },
                                onCloseOverlayResume = {
                                    mainViewModel.showResumeOverlay(false)
                                },
                                onNavigateToProfile = {
                                    mainViewModel.selectTab(SanamTab.PROFILE)
                                }
                            )

                            SanamTab.JOBS -> SanamJobsScreen(
                                mainUiState = mainState,
                                onSearch = { q, c -> mainViewModel.performMultiAgentJobSearch(q, c, tts) },
                                onApplyJob = { job -> mainViewModel.applyToJob(this@MainActivity, job) },
                                onShareDigest = { mainViewModel.shareWhatsAppDigest(this@MainActivity) }
                            )

                            SanamTab.RESUME -> SanamResumeScreen(
                                baseResumeText = resumeState.formState.toPlainText(),
                                sharedPrefsHelper = sharedPrefsHelper,
                                dao = dao
                            )

                            SanamTab.PROFILE -> com.example.ui.SanamProfileScreen(
                                sharedPrefsHelper = sharedPrefsHelper,
                                onProfileSaved = { mainViewModel.onProfileUpdated() }
                            )

                            SanamTab.TRACKER -> SanamTrackerScreen(dao = dao)

                            SanamTab.LENS -> SanamNeuralLensScreen(dao = dao)

                            SanamTab.COMMUNITY -> SanamCommunityScreen(dao = dao)

                            SanamTab.SETTINGS -> SanamSettingsScreen(
                                sharedPrefsHelper = sharedPrefsHelper,
                                onToggleFreeMode = { mainViewModel.toggleFreeMode(it) },
                                onToggleAutoTrigger = { mainViewModel.toggleAutoTrigger(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.ENGLISH
            tts?.setPitch(1.15f)
            tts?.setSpeechRate(0.95f)
            isTtsInitialized = true
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun SanamBottomNavigationDock(
    selectedTab: SanamTab,
    orbMode: SanamOrbMode,
    onSelectTab: (SanamTab) -> Unit,
    onCenterOrbClick: () -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        contentColor = Color.Black,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sanam_bottom_dock")
    ) {
        // 1. Home
        NavigationBarItem(
            selected = selectedTab == SanamTab.HOME,
            onClick = { onSelectTab(SanamTab.HOME) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 11.sp, fontWeight = if (selectedTab == SanamTab.HOME) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF7C3AED),
                selectedTextColor = Color(0xFF7C3AED),
                unselectedIconColor = Color(0xFF888888),
                unselectedTextColor = Color(0xFF888888),
                indicatorColor = Color(0xFFF2F0FF)
            )
        )

        // 2. Chat
        NavigationBarItem(
            selected = selectedTab == SanamTab.JOBS || selectedTab == SanamTab.RESUME,
            onClick = { onSelectTab(SanamTab.JOBS) },
            icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Chat") },
            label = { Text("Chat", fontSize = 11.sp, fontWeight = if (selectedTab == SanamTab.JOBS || selectedTab == SanamTab.RESUME) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF7C3AED),
                selectedTextColor = Color(0xFF7C3AED),
                unselectedIconColor = Color(0xFF888888),
                unselectedTextColor = Color(0xFF888888),
                indicatorColor = Color(0xFFF2F0FF)
            )
        )

        // 3. CENTER MINI ORB (68dp)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .clickable { onCenterOrbClick() }
        ) {
            com.example.ui.components.SanamHolographicOrb(
                mode = orbMode,
                statusMessage = "",
                onSingleTap = onCenterOrbClick,
                onLongPress = onCenterOrbClick,
                onDoubleTap = {},
                isFullScreen = false,
                orbDiameterDp = 68.dp
            )
        }

        // 4. History / Tracker
        NavigationBarItem(
            selected = selectedTab == SanamTab.TRACKER,
            onClick = { onSelectTab(SanamTab.TRACKER) },
            icon = { Icon(Icons.Default.History, contentDescription = "History") },
            label = { Text("History", fontSize = 11.sp, fontWeight = if (selectedTab == SanamTab.TRACKER) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF7C3AED),
                selectedTextColor = Color(0xFF7C3AED),
                unselectedIconColor = Color(0xFF888888),
                unselectedTextColor = Color(0xFF888888),
                indicatorColor = Color(0xFFF2F0FF)
            )
        )

        // 5. Settings
        NavigationBarItem(
            selected = selectedTab == SanamTab.SETTINGS,
            onClick = { onSelectTab(SanamTab.SETTINGS) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 11.sp, fontWeight = if (selectedTab == SanamTab.SETTINGS) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF7C3AED),
                selectedTextColor = Color(0xFF7C3AED),
                unselectedIconColor = Color(0xFF888888),
                unselectedTextColor = Color(0xFF888888),
                indicatorColor = Color(0xFFF2F0FF)
            )
        )
    }
}
