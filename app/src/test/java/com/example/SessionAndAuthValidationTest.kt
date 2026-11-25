package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.isValidEmailAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SessionAndAuthValidationTest {

    @Test
    fun `email validation accepts valid emails and rejects invalid formats`() {
        assertTrue(isValidEmailAddress("familia.silva@gmail.com"))
        assertTrue(isValidEmailAddress("  usuario@empresa.com.br "))
        assertFalse(isValidEmailAddress(""))
        assertFalse(isValidEmailAddress("sem-arroba.com"))
        assertFalse(isValidEmailAddress("usuario@"))
    }

    @Test
    fun `session starts locked on cold start and enforces 1 hour inactivity timeout`() {
        val app = ApplicationProvider.getApplicationContext<AstroGestaoApplication>()
        app.resetSessionForFreshLaunch()

        assertFalse(app.isSessionUnlocked.value)
        assertFalse(app.isSessionTimedOut(app.lastActivityMillis.value + AstroGestaoApplication.INACTIVITY_TIMEOUT_MS + 1000L))

        app.unlockSession()
        assertTrue(app.isSessionUnlocked.value)
        assertNull(app.sessionNotice.value)

        val baseTime = app.lastActivityMillis.value
        assertFalse(app.isSessionTimedOut(baseTime + AstroGestaoApplication.INACTIVITY_TIMEOUT_MS - 1000L))
        assertTrue(app.isSessionTimedOut(baseTime + AstroGestaoApplication.INACTIVITY_TIMEOUT_MS))
        assertTrue(app.isSessionTimedOut(baseTime + AstroGestaoApplication.INACTIVITY_TIMEOUT_MS + 60_000L))

        app.lockSession("Sessão encerrada após 1 hora de inatividade.")
        assertFalse(app.isSessionUnlocked.value)
        assertEquals("Sessão encerrada após 1 hora de inatividade.", app.sessionNotice.value)
    }
}
