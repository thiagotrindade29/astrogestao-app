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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "astro_erp_prefs")

class ThemePreferencesRepository(private val context: Context) {
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_NAME = stringPreferencesKey("user_name")
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DARK_MODE] ?: false
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IS_LOGGED_IN] ?: false
    }

    val userEmail: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.USER_EMAIL] ?: ""
    }

    val userName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.USER_NAME] ?: ""
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = enabled
        }
    }

    suspend fun signIn(email: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = true
            prefs[Keys.USER_EMAIL] = email.trim()
            prefs[Keys.USER_NAME] = name.trim().ifEmpty {
                email.substringBefore("@").replaceFirstChar { it.uppercase() }
            }
        }
    }

    suspend fun signOut() {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = false
        }
    }
}
