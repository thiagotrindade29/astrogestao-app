package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.AstroGestaoApp
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
            AstroGestaoApp(viewModel = viewModel)
        }
    }
}
