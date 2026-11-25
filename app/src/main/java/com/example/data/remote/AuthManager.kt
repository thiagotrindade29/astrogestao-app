package com.example.data.remote

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.util.Patterns
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val AUTH_TAG = "Auth"

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun isValidEmailAddress(email: String): Boolean {
    val trimmed = email.trim()
    return trimmed.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
}

private fun mapFirebaseAuthError(error: Exception): String {
    if (error is FirebaseNetworkException) {
        return "Sem conexão com a internet. Verifique sua rede e tente novamente."
    }
    if (error is FirebaseAuthException) {
        return when (error.errorCode) {
            "ERROR_INVALID_EMAIL" -> "Informe um endereço de e-mail válido."
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" ->
                "E-mail ou senha incorretos. Se ainda não cadastrou uma senha, toque em \"Criar Conta\" ou use \"Entrar com Google\"."
            "ERROR_USER_NOT_FOUND" ->
                "Nenhuma conta encontrada com este e-mail. Toque em \"Criar Conta\" para se cadastrar."
            "ERROR_EMAIL_ALREADY_IN_USE" ->
                "Este e-mail já está cadastrado. Use \"Entrar\", \"Entrar com Google\" ou \"Esqueci minha senha\"."
            "ERROR_WEAK_PASSWORD" ->
                "A senha precisa ter pelo menos 6 caracteres."
            "ERROR_USER_DISABLED" ->
                "Esta conta foi desativada no Firebase."
            "ERROR_TOO_MANY_REQUESTS" ->
                "Muitas tentativas seguidas. Aguarde alguns instantes e tente novamente."
            "ERROR_OPERATION_NOT_ALLOWED" ->
                "O login por e-mail e senha não está ativo no Firebase Console (Authentication > Sign-in method)."
            "ERROR_NETWORK_REQUEST_FAILED" ->
                "Falha de rede ao autenticar. Verifique sua conexão."
            else -> error.localizedMessage ?: "Falha ao autenticar (${error.errorCode})."
        }
    }
    return error.localizedMessage ?: "Não foi possível autenticar. Tente novamente."
}

fun onEmailSignInClicked(
    email: String,
    password: String,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope
) {
    val trimmedEmail = email.trim()
    if (!isValidEmailAddress(trimmedEmail)) {
        onAuthError("Informe um endereço de e-mail válido.")
        return
    }
    if (password.isBlank()) {
        onAuthError("Digite sua senha para entrar.")
        return
    }
    scope.launch {
        try {
            Firebase.auth.signInWithEmailAndPassword(trimmedEmail, password).await()
            onAuthSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(AUTH_TAG, "Email sign-in failed: ${(e as? FirebaseAuthException)?.errorCode ?: e.javaClass.simpleName}")
            onAuthError(mapFirebaseAuthError(e))
        }
    }
}

fun onEmailSignUpClicked(
    name: String,
    email: String,
    password: String,
    confirmPassword: String,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope
) {
    val trimmedName = name.trim()
    val trimmedEmail = email.trim()
    if (trimmedName.length < 2) {
        onAuthError("Informe seu nome (mínimo 2 caracteres).")
        return
    }
    if (!isValidEmailAddress(trimmedEmail)) {
        onAuthError("Informe um endereço de e-mail válido.")
        return
    }
    if (password.length < 6) {
        onAuthError("A senha deve ter pelo menos 6 caracteres.")
        return
    }
    if (password != confirmPassword) {
        onAuthError("As senhas digitadas não coincidem.")
        return
    }
    scope.launch {
        try {
            val result = Firebase.auth.createUserWithEmailAndPassword(trimmedEmail, password).await()
            val user = result.user
            if (user != null) {
                runCatching {
                    user.updateProfile(userProfileChangeRequest { displayName = trimmedName }).await()
                }
            }
            onAuthSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(AUTH_TAG, "Email sign-up failed: ${(e as? FirebaseAuthException)?.errorCode ?: e.javaClass.simpleName}")
            onAuthError(mapFirebaseAuthError(e))
        }
    }
}

fun onSendPasswordResetClicked(
    email: String,
    onResetSent: (String) -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope
) {
    val trimmedEmail = email.trim()
    if (!isValidEmailAddress(trimmedEmail)) {
        onAuthError("Digite um e-mail válido no campo acima para receber o link de recuperação.")
        return
    }
    scope.launch {
        try {
            Firebase.auth.sendPasswordResetEmail(trimmedEmail).await()
            onResetSent("Enviamos as instruções de redefinição de senha para $trimmedEmail.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(AUTH_TAG, "Password reset failed: ${(e as? FirebaseAuthException)?.errorCode ?: e.javaClass.simpleName}")
            onAuthError(mapFirebaseAuthError(e))
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Configuração do Google Sign-In ausente (default_web_client_id).")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activityContext = context.findActivity() ?: context
            val cred = credentialManager.getCredential(activityContext, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("Tipo de credencial inesperado ao entrar com Google.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Falha ao entrar com Google. Verifique sua conexão.")
        }
    }
}

fun performGoogleSignOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onBeforeSignOut: suspend () -> Unit = {},
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                onBeforeSignOut()
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}
