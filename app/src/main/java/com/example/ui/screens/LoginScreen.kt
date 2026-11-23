package com.example.ui.screens

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.AstroBrandLogo

@Composable
fun LoginScreen(
    defaultEmail: String,
    onSignIn: (email: String, password: String, onError: (String) -> Unit) -> Unit,
    onSignUp: (name: String, email: String, password: String, role: String, onError: (String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSignUpMode by remember { mutableStateOf(false) }

    // Shared / Login state
    var email by remember { mutableStateOf(defaultEmail) }
    var password by remember { mutableStateOf("••••••••") }
    var showPassword by remember { mutableStateOf(false) }

    // Sign-Up state
    var fullName by remember { mutableStateOf("") }
    var signUpEmail by remember { mutableStateOf("") }
    var signUpPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Morador") }
    var showSignUpPassword by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val invalidCredentialsMsg = stringResource(R.string.error_invalid_credentials)
    val requiredMsg = stringResource(R.string.error_required)
    val passwordShortMsg = stringResource(R.string.error_password_short)
    val passwordMismatchMsg = stringResource(R.string.error_password_mismatch)

    if (isSignUpMode) {
        BackHandler {
            isSignUpMode = false
            errorMessage = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0E24), Color(0xFF131936), Color(0xFF090D1D))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            val isWide = maxWidth >= 740.dp
            Card(
                modifier = Modifier
                    .widthIn(max = 900.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1326)),
                border = BorderStroke(1.dp, Color(0xFF283058)),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                val formContent: @Composable (Modifier) -> Unit = { formMod ->
                    if (!isSignUpMode) {
                        LoginFormPanel(
                            email = email,
                            onEmailChange = {
                                email = it
                                errorMessage = null
                            },
                            password = password,
                            onPasswordChange = {
                                password = it
                                errorMessage = null
                            },
                            showPassword = showPassword,
                            onTogglePassword = { showPassword = !showPassword },
                            errorMessage = errorMessage,
                            onSubmit = {
                                val trimmedEmail = email.trim()
                                if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() || password.length < 4) {
                                    errorMessage = invalidCredentialsMsg
                                } else {
                                    onSignIn(trimmedEmail, password) { err -> errorMessage = err }
                                }
                            },
                            onSwitchToSignUp = {
                                errorMessage = null
                                isSignUpMode = true
                            },
                            modifier = formMod
                        )
                    } else {
                        SignUpFormPanel(
                            fullName = fullName,
                            onFullNameChange = {
                                fullName = it
                                errorMessage = null
                            },
                            email = signUpEmail,
                            onEmailChange = {
                                signUpEmail = it
                                errorMessage = null
                            },
                            password = signUpPassword,
                            onPasswordChange = {
                                signUpPassword = it
                                errorMessage = null
                            },
                            confirmPassword = confirmPassword,
                            onConfirmPasswordChange = {
                                confirmPassword = it
                                errorMessage = null
                            },
                            selectedRole = selectedRole,
                            onSelectRole = { selectedRole = it },
                            showPassword = showSignUpPassword,
                            onTogglePassword = { showSignUpPassword = !showSignUpPassword },
                            errorMessage = errorMessage,
                            onSubmit = {
                                val trimmedName = fullName.trim()
                                val trimmedEmail = signUpEmail.trim()
                                when {
                                    trimmedName.length < 2 -> errorMessage = requiredMsg
                                    !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() -> errorMessage = invalidCredentialsMsg
                                    signUpPassword.length < 6 -> errorMessage = passwordShortMsg
                                    signUpPassword != confirmPassword -> errorMessage = passwordMismatchMsg
                                    else -> onSignUp(trimmedName, trimmedEmail, signUpPassword, selectedRole) { err ->
                                        errorMessage = err
                                    }
                                }
                            },
                            onSwitchToLogin = {
                                errorMessage = null
                                isSignUpMode = false
                            },
                            modifier = formMod
                        )
                    }
                }

                if (isWide) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        CosmicPromoPanel(
                            modifier = Modifier
                                .weight(1f)
                                .height(if (isSignUpMode) 600.dp else 490.dp)
                        )
                        formContent(Modifier.weight(1f))
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CosmicPromoPanel(modifier = Modifier.fillMaxWidth().height(215.dp))
                        formContent(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun CosmicPromoPanel(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = R.drawable.img_astro_hero),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xD92E1065),
                            Color(0xB3312E81),
                            Color(0xAA0F172A)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AstroBrandLogo(sizeDp = 34)
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.login_eyebrow),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA5B4FC)
                )
                Text(
                    text = stringResource(R.string.login_headline),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.login_subheadline),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )
            }
        }
    }
}

@Composable
private fun authInputColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color(0xFFF1F5F9),
    unfocusedContainerColor = Color(0xFFF1F5F9),
    focusedTextColor = Color(0xFF0F172A),
    unfocusedTextColor = Color(0xFF0F172A),
    focusedLabelColor = Color(0xFFA5B4FC),
    unfocusedLabelColor = Color(0xFF94A3B8)
)

@Composable
private fun LoginFormPanel(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onTogglePassword: () -> Unit,
    errorMessage: String?,
    onSubmit: () -> Unit,
    onSwitchToSignUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = stringResource(R.string.login_welcome_tag),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF818CF8)
        )
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF94A3B8)
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(R.string.label_email)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = authInputColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_email_input")
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.label_password)) },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (showPassword) "Ocultar senha" else "Mostrar senha",
                        tint = Color(0xFF475569)
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = authInputColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_password_input")
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFB7185)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF6366F1), Color(0xFF3B82F6))
                    )
                )
                .testTag("login_submit_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = stringResource(R.string.action_sign_in),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        TextButton(
            onClick = onSwitchToSignUp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .testTag("go_to_signup_button")
        ) {
            Text(
                text = stringResource(R.string.action_go_to_signup),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFA5B4FC),
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = stringResource(R.string.login_footer_note),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B)
        )
    }
}

@Composable
private fun SignUpFormPanel(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    selectedRole: String,
    onSelectRole: (String) -> Unit,
    showPassword: Boolean,
    onTogglePassword: () -> Unit,
    errorMessage: String?,
    onSubmit: () -> Unit,
    onSwitchToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.signup_tag),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF818CF8)
        )
        Text(
            text = stringResource(R.string.signup_title),
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.signup_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
        )

        OutlinedTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            label = { Text(stringResource(R.string.label_full_name)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = authInputColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_name_input")
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(R.string.label_email)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = authInputColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_email_input")
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Perfil:",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFCBD5E1)
            )
            listOf("Morador", "Administrador").forEach { role ->
                val isSelected = selectedRole == role
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectRole(role) },
                    label = { Text(role) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color(0xFFCBD5E1),
                        selectedContainerColor = Color(0xFF6366F1),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("signup_role_${role.lowercase()}")
                )
            }
        }

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.label_password)) },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (showPassword) "Ocultar senha" else "Mostrar senha",
                        tint = Color(0xFF475569)
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = authInputColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_password_input")
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = { Text(stringResource(R.string.label_confirm_password)) },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = authInputColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_confirm_password_input")
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFB7185)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF6366F1), Color(0xFF3B82F6))
                    )
                )
                .testTag("signup_submit_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = stringResource(R.string.action_create_account),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        TextButton(
            onClick = onSwitchToLogin,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .testTag("back_to_login_button")
        ) {
            Text(
                text = stringResource(R.string.action_back_to_login),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFA5B4FC),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
