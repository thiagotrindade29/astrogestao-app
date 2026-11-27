package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
<<<<<<< HEAD
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.AstroRepository
import com.example.ui.AstroViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.screens.MainShellScreen
import com.example.ui.theme.AstroGestaoTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val dbId = getString(R.string.firestore_database_id)
        setContent {
            AstroGestaoTheme {
                val auth = remember { Firebase.auth }
                val currentUser by auth.authStateFlow()
                    .collectAsStateWithLifecycle(initialValue = auth.currentUser)

                val user = currentUser
                if (user == null) {
                    AuthScreen(onAuthSuccess = {})
                } else {
                    key(user.uid) {
                        val db = remember(dbId) { FirebaseFirestore.getInstance(dbId) }
                        val repository = remember(db) { AstroRepository(db = db, auth = auth) }
                        val viewModel: AstroViewModel = viewModel(
                            key = user.uid,
                            factory = viewModelFactory {
                                initializer {
                                    AstroViewModel(
                                        repository = repository,
                                        currentUser = user
                                    )
                                }
                            }
                        )
                        MainShellScreen(user = user, viewModel = viewModel)
                    }
                }
            }
=======
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
>>>>>>> origin/main
        }
    }
}
