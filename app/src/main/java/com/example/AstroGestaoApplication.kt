package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.remote.FirestoreAstroRepository
import com.example.data.repository.ThemePreferencesRepository

class AstroGestaoApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val cloudRepository: FirestoreAstroRepository by lazy { FirestoreAstroRepository(this) }
    val preferencesRepository: ThemePreferencesRepository by lazy { ThemePreferencesRepository(this) }
}
