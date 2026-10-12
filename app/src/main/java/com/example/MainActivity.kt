package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ResumeDatabase
import com.example.data.ResumeRepository
import com.example.ui.ResumeAppScreen
import com.example.ui.ResumeViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = ResumeDatabase.getInstance(applicationContext)
        val repository = ResumeRepository(database.resumeDao())

        setContent {
            val resumeViewModel: ResumeViewModel = viewModel(
                factory = ResumeViewModel.provideFactory(repository)
            )
            val uiState by resumeViewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = uiState.isDarkThemeOverride ?: systemDark

            MyApplicationTheme(darkTheme = useDarkTheme) {
                ResumeAppScreen(
                    viewModel = resumeViewModel,
                    isDarkTheme = useDarkTheme
                )
            }
        }
    }
}
