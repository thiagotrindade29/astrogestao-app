package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.AstroRepository
import com.example.data.repository.ThemePreferencesRepository

class AstroGestaoApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: AstroRepository by lazy { AstroRepository(database) }
    val preferencesRepository: ThemePreferencesRepository by lazy { ThemePreferencesRepository(this) }
}
