package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "astro_erp_prefs")

class ThemePreferencesRepository(private val context: Context) {
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_NAME = stringPreferencesKey("user_name")
        val LAST_ACTIVITY_MILLIS = longPreferencesKey("last_activity_millis")
        val LOW_STOCK_NOTIFICATIONS_ENABLED = booleanPreferencesKey("low_stock_notifications_enabled")
        val LAST_LOW_STOCK_SIGNATURE = stringPreferencesKey("last_low_stock_signature")
        val LAST_LOW_STOCK_NOTIFIED_MILLIS = longPreferencesKey("last_low_stock_notified_millis")
        val DISMISSED_UPDATE_VERSION = stringPreferencesKey("dismissed_update_version")
        val NOTIFICATION_PERMISSION_PROMPTED = booleanPreferencesKey("notification_permission_prompted")
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

    val lowStockNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.LOW_STOCK_NOTIFICATIONS_ENABLED] ?: true
    }

    val dismissedUpdateVersion: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DISMISSED_UPDATE_VERSION] ?: ""
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = enabled
        }
    }

    suspend fun setLowStockNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LOW_STOCK_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setDismissedUpdateVersion(versionName: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DISMISSED_UPDATE_VERSION] = versionName.trim()
        }
    }

    suspend fun getLowStockNotificationRecord(): Pair<String, Long> {
        return context.dataStore.data.map { prefs ->
            val sig = prefs[Keys.LAST_LOW_STOCK_SIGNATURE] ?: ""
            val ts = prefs[Keys.LAST_LOW_STOCK_NOTIFIED_MILLIS] ?: 0L
            sig to ts
        }.first()
    }

    suspend fun saveLowStockNotificationRecord(signature: String, timestampMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_LOW_STOCK_SIGNATURE] = signature
            prefs[Keys.LAST_LOW_STOCK_NOTIFIED_MILLIS] = timestampMillis
        }
    }

    suspend fun clearLowStockNotificationRecord() {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_LOW_STOCK_SIGNATURE] = ""
            prefs[Keys.LAST_LOW_STOCK_NOTIFIED_MILLIS] = 0L
        }
    }

    suspend fun wasNotificationPermissionPrompted(): Boolean {
        return context.dataStore.data.map { prefs ->
            prefs[Keys.NOTIFICATION_PERMISSION_PROMPTED] ?: false
        }.first()
    }

    suspend fun markNotificationPermissionPrompted() {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATION_PERMISSION_PROMPTED] = true
        }
    }

    suspend fun saveLastActivityMillis(timestampMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_ACTIVITY_MILLIS] = timestampMillis
        }
    }

    suspend fun getLastActivityMillis(): Long {
        return context.dataStore.data.map { prefs ->
            prefs[Keys.LAST_ACTIVITY_MILLIS] ?: 0L
        }.first()
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
