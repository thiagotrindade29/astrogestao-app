package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "astro_prefs")

class ThemePreferencesRepository(private val context: Context) {
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val BUSINESS_NAME = stringPreferencesKey("business_name")
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DARK_MODE] ?: true
    }

    val businessName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.BUSINESS_NAME] ?: "AstroGestão"
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = enabled
        }
    }

    suspend fun setBusinessName(name: String) {
        context.dataStore.edit { prefs ->
            val trimmed = name.trim()
            if (trimmed.isNotEmpty()) {
                prefs[Keys.BUSINESS_NAME] = trimmed
            }
        }
    }
}
