package com.example.ui

import androidx.compose.runtime.Composable
import com.example.domain.Job

@Composable
fun HomeScreen(
    mainUiState: MainUiState,
    onOrbClick: () -> Unit,
    onSearchQuery: (String) -> Unit,
    onApplyJob: (Job) -> Unit
) {
    SanamHomeScreen(
        mainUiState = mainUiState,
        onOrbSingleTap = onOrbClick,
        onOrbLongPress = onOrbClick,
        onOrbDoubleTap = {},
        onQuickVoiceAction = { action -> onSearchQuery(action) },
        onGenerateResumeCard = {},
        onCloseOverlayResume = {}
    )
}
