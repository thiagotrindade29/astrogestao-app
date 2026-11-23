package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AstroGestaoApp
import com.example.ui.theme.AstroGestaoTheme
import com.example.ui.viewmodel.AstroViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AstroViewModel by viewModels {
        val app = application as AstroGestaoApplication
        AstroViewModel.Factory(app.repository, app.preferencesRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            AstroGestaoTheme(darkTheme = uiState.isDarkMode) {
                AstroGestaoApp(viewModel = viewModel)
            }
        }
    }
}
