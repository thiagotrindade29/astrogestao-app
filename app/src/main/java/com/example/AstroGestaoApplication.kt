package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.remote.FirestoreAstroRepository
import com.example.data.repository.ThemePreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AstroGestaoApplication : Application() {
    companion object {
        const val INACTIVITY_TIMEOUT_MS: Long = 60L * 60L * 1000L // 1 hora
    }

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val cloudRepository: FirestoreAstroRepository by lazy { FirestoreAstroRepository(this) }
    val preferencesRepository: ThemePreferencesRepository by lazy { ThemePreferencesRepository(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastPersistedActivityMillis: Long = 0L

    private val _isSessionUnlocked = MutableStateFlow(false)
    val isSessionUnlocked: StateFlow<Boolean> = _isSessionUnlocked.asStateFlow()

    private val _sessionNotice = MutableStateFlow<String?>(null)
    val sessionNotice: StateFlow<String?> = _sessionNotice.asStateFlow()

    private val _lastActivityMillis = MutableStateFlow(System.currentTimeMillis())
    val lastActivityMillis: StateFlow<Long> = _lastActivityMillis.asStateFlow()

    fun resetSessionForFreshLaunch() {
        _isSessionUnlocked.value = false
    }

    fun unlockSession() {
        _sessionNotice.value = null
        recordUserActivity(forcePersist = true)
        _isSessionUnlocked.value = true
    }

    fun lockSession(notice: String? = null) {
        _sessionNotice.value = notice
        _isSessionUnlocked.value = false
    }

    fun clearSessionNotice() {
        _sessionNotice.value = null
    }

    fun recordUserActivity(forcePersist: Boolean = false) {
        val now = System.currentTimeMillis()
        _lastActivityMillis.value = now
        if (forcePersist || now - lastPersistedActivityMillis >= 15_000L) {
            lastPersistedActivityMillis = now
            appScope.launch {
                runCatching { preferencesRepository.saveLastActivityMillis(now) }
            }
        }
    }

    fun isSessionTimedOut(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return _isSessionUnlocked.value && (nowMillis - _lastActivityMillis.value >= INACTIVITY_TIMEOUT_MS)
    }
}
