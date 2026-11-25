package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.AstroGestaoApp

class MainActivity : ComponentActivity() {
    private lateinit var astroApp: AstroGestaoApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        astroApp = application as AstroGestaoApplication
        if (savedInstanceState == null) {
            astroApp.resetSessionForFreshLaunch()
        }
        setContent {
            AstroGestaoApp(app = astroApp)
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (::astroApp.isInitialized) {
            astroApp.recordUserActivity()
        }
    }
}
