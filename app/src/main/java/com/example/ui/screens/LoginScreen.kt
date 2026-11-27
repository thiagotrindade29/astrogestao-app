package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.remote.onEmailSignInClicked
import com.example.data.remote.onEmailSignUpClicked
import com.example.data.remote.onGoogleSignInClicked
import com.example.data.remote.onSendPasswordResetClicked
import com.example.ui.components.AstroBrandLogo
import com.example.ui.theme.AstroTheme
import com.example.ui.theme.AzulSideralBg
import com.example.ui.theme.AzulSideralBorder
import com.example.ui.theme.AzulSideralElevated
import com.example.ui.theme.AzulSideralSurface
import com.example.ui.theme.CianoEletrico
import com.example.ui.theme.IndigoNoturno
import com.example.ui.theme.IndigoNoturnoDeep
import com.example.ui.theme.IndigoNoturnoElevated
import com.example.ui.theme.LilasOrion
import com.example.ui.theme.LilasOrionContainer
import com.example.ui.theme.LilasOrionSoft
import com.example.ui.theme.PessegoClaro
import com.example.ui.theme.PessegoClaroSoft
import com.example.ui.theme.PessegoClaroWorkspace
import com.example.ui.theme.PrataNiquel
import com.example.ui.theme.PrataNiquelLight
import com.example.ui.theme.PrataNiquelMuted

private data class LoginPalette(
    val bgOuter: Color,
    val bgMid: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val wellBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val accentDisabledBg: Color,
    val secondaryCard: Color,
    val onSecondaryCard: Color
)

@Composable
private fun rememberLoginPalette(): LoginPalette {
    val isDark = AstroTheme.extended.isDark
    return if (isDark) {
        LoginPalette(
            bgOuter = AzulSideralBg,
            bgMid = AzulSideralSurface,
            cardBg = AzulSideralSurface,
            cardBorder = AzulSideralBorder,
            wellBg = AzulSideralBg,
            textPrimary = PrataNiquelLight,
            textSecondary = PrataNiquel,
            textMuted = PrataNiquelMuted,
            accent = LilasOrion,
            accentSoft = LilasOrionSoft,
            onAccent = AzulSideralBg,
            accentDisabledBg = LilasOrionContainer,
            secondaryCard = PrataNiquel,
            onSecondaryCard = AzulSideralBg
        )
    } else {
        LoginPalette(
            bgOuter = IndigoNoturnoDeep,
            bgMid = IndigoNoturno,
            cardBg = IndigoNoturno,
            cardBorder = PessegoClaro.copy(alpha = 0.25f),
            wellBg = IndigoNoturnoDeep,
            textPrimary = PessegoClaroWorkspace,
            textSecondary = PessegoClaro,
            textMuted = PessegoClaro.copy(alpha = 0.72f),
            accent = CianoEletrico,
            accentSoft = CianoEletrico,
            onAccent = IndigoNoturno,
            accentDisabledBg = IndigoNoturnoElevated,
            secondaryCard = PessegoClaro,
            onSecondaryCard = IndigoNoturno
        )
    }
}

private enum class EmailAuthMode {
    SIGN_IN,
    SIGN_UP
}

@Composable
fun LoginScreen(
    onAuthSuccess: () -> Unit,
    sessionNotice: String? = null,
    onDismissNotice: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var authMode by rememberSaveable { mutableStateOf(EmailAuthMode.SIGN_IN) }
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var isEmailBusy by remember { mutableStateOf(false) }
    var isGoogleBusy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    val isBusy = isEmailBusy || isGoogleBusy

    fun submitEmailAuth() {
        errorMessage = null
        infoMessage = null
        onDismissNotice()
        isEmailBusy = true
        if (authMode == EmailAuthMode.SIGN_IN) {
            onEmailSignInClicked(
                email = email,
                password = password,
                onAuthSuccess = {
                    isEmailBusy = false
                    password = ""
                    onAuthSuccess()
                },
                onAuthError = { msg ->
                    isEmailBusy = false
                    errorMessage = msg
                },
                scope = scope
            )
        } else {
            onEmailSignUpClicked(
                name = fullName,
                email = email,
                password = password,
                confirmPassword = confirmPassword,
                onAuthSuccess = {
                    isEmailBusy = false
                    password = ""
                    confirmPassword = ""
                    onAuthSuccess()
                },
                onAuthError = { msg ->
                    isEmailBusy = false
                    errorMessage = msg
                },
                scope = scope
            )
        }
    }

    val palette = rememberLoginPalette()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(palette.bgOuter, palette.bgMid, palette.bgOuter)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val isWide = maxWidth >= 740.dp
            Card(
                modifier = Modifier
                    .widthIn(max = 940.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = palette.cardBg),
                border = BorderStroke(1.dp, palette.cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                val formPanel: @Composable (Modifier) -> Unit = { formMod ->
                    DualAuthPanel(
                        authMode = authMode,
                        onSelectMode = { mode ->
                            authMode = mode
                            errorMessage = null
                            infoMessage = null
                        },
                        fullName = fullName,
                        onFullNameChange = { fullName = it },
                        email = email,
                        onEmailChange = { email = it },
                        password = password,
                        onPasswordChange = { password = it },
                        confirmPassword = confirmPassword,
                        onConfirmPasswordChange = { confirmPassword = it },
                        showPassword = showPassword,
                        onToggleShowPassword = { showPassword = !showPassword },
                        isEmailBusy = isEmailBusy,
                        isGoogleBusy = isGoogleBusy,
                        isBusy = isBusy,
                        sessionNotice = sessionNotice,
                        onDismissNotice = onDismissNotice,
                        errorMessage = errorMessage,
                        infoMessage = infoMessage,
                        onSubmitEmail = ::submitEmailAuth,
                        onForgotPassword = {
                            errorMessage = null
                            infoMessage = null
                            isEmailBusy = true
                            onSendPasswordResetClicked(
                                email = email,
                                onResetSent = { msg ->
                                    isEmailBusy = false
                                    infoMessage = msg
                                },
                                onAuthError = { msg ->
                                    isEmailBusy = false
                                    errorMessage = msg
                                },
                                scope = scope
                            )
                        },
                        onGoogleClick = {
                            errorMessage = null
                            infoMessage = null
                            onDismissNotice()
                            isGoogleBusy = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isGoogleBusy = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isGoogleBusy = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isGoogleBusy = false
                                }
                            )
                        },
                        modifier = formMod
                    )
                }

                if (isWide) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        CosmicPromoPanel(
                            modifier = Modifier
                                .weight(0.95f)
                                .height(610.dp)
                        )
                        formPanel(Modifier.weight(1.15f))
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CosmicPromoPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(176.dp)
                        )
                        formPanel(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun CosmicPromoPanel(modifier: Modifier = Modifier) {
    val palette = rememberLoginPalette()
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
                            palette.bgOuter.copy(alpha = 0.88f),
                            palette.bgMid.copy(alpha = 0.78f),
                            palette.bgOuter.copy(alpha = 0.85f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
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
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.login_eyebrow),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accentSoft
                )
                Text(
                    text = stringResource(R.string.login_headline),
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.login_subheadline),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }
    }
}

@Composable
private fun DualAuthPanel(
    authMode: EmailAuthMode,
    onSelectMode: (EmailAuthMode) -> Unit,
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onToggleShowPassword: () -> Unit,
    isEmailBusy: Boolean,
    isGoogleBusy: Boolean,
    isBusy: Boolean,
    sessionNotice: String?,
    onDismissNotice: () -> Unit,
    errorMessage: String?,
    infoMessage: String?,
    onSubmitEmail: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoogleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberLoginPalette()
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = palette.textPrimary,
        unfocusedTextColor = palette.textPrimary,
        focusedBorderColor = palette.accent,
        unfocusedBorderColor = palette.cardBorder,
        focusedLabelColor = palette.accentSoft,
        unfocusedLabelColor = palette.textMuted,
        focusedLeadingIconColor = palette.accentSoft,
        unfocusedLeadingIconColor = palette.textMuted,
        focusedTrailingIconColor = palette.accentSoft,
        unfocusedTrailingIconColor = palette.textMuted,
        cursorColor = palette.accent
    )

    Column(
        modifier = modifier.padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.login_welcome_tag),
            style = MaterialTheme.typography.labelSmall,
            color = palette.accentSoft
        )
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineSmall,
            color = palette.textPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted
        )

        if (!sessionNotice.isNullOrBlank()) {
            Surface(
                color = palette.secondaryCard.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, palette.secondaryCard.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = palette.secondaryCard,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = sessionNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismissNotice,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.action_cancel),
                            tint = palette.secondaryCard,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Mode switcher: Entrar vs Criar Conta
        Surface(
            color = palette.wellBg,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, palette.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AuthModeTabButton(
                    label = stringResource(R.string.login_tab_sign_in),
                    selected = authMode == EmailAuthMode.SIGN_IN,
                    enabled = !isBusy,
                    onClick = { onSelectMode(EmailAuthMode.SIGN_IN) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("login_tab_signin")
                )
                AuthModeTabButton(
                    label = stringResource(R.string.login_tab_sign_up),
                    selected = authMode == EmailAuthMode.SIGN_UP,
                    enabled = !isBusy,
                    onClick = { onSelectMode(EmailAuthMode.SIGN_UP) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("login_tab_signup")
                )
            }
        }

        AnimatedVisibility(visible = authMode == EmailAuthMode.SIGN_UP) {
            OutlinedTextField(
                value = fullName,
                onValueChange = onFullNameChange,
                enabled = !isBusy,
                singleLine = true,
                label = { Text(stringResource(R.string.label_name)) },
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("name_input")
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            enabled = !isBusy,
            singleLine = true,
            label = { Text(stringResource(R.string.label_email)) },
            leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            colors = fieldColors,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("email_input")
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            enabled = !isBusy,
            singleLine = true,
            label = { Text(stringResource(R.string.label_password)) },
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(
                    onClick = onToggleShowPassword,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("toggle_password_visibility")
                ) {
                    Icon(
                        imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (showPassword) "Ocultar senha" else "Mostrar senha"
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = if (authMode == EmailAuthMode.SIGN_IN) ImeAction.Done else ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (authMode == EmailAuthMode.SIGN_IN && !isBusy) {
                        onSubmitEmail()
                    }
                }
            ),
            colors = fieldColors,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("password_input")
        )

        AnimatedVisibility(visible = authMode == EmailAuthMode.SIGN_UP) {
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                enabled = !isBusy,
                singleLine = true,
                label = { Text(stringResource(R.string.label_confirm_password)) },
                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (!isBusy) onSubmitEmail()
                    }
                ),
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_password_input")
            )
        }

        if (authMode == EmailAuthMode.SIGN_IN) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onForgotPassword,
                    enabled = !isBusy,
                    modifier = Modifier.testTag("forgot_password_button")
                ) {
                    Text(
                        text = stringResource(R.string.action_forgot_password),
                        color = palette.accentSoft,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        if (errorMessage != null) {
            Surface(
                color = palette.secondaryCard.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, palette.secondaryCard.copy(alpha = 0.5f))
            ) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
            }
        }

        if (infoMessage != null) {
            Surface(
                color = palette.accent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDone,
                        contentDescription = null,
                        tint = palette.accentSoft,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = infoMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textPrimary
                    )
                }
            }
        }

        Button(
            onClick = onSubmitEmail,
            enabled = !isBusy,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.accent,
                contentColor = palette.onAccent,
                disabledContainerColor = palette.accentDisabledBg,
                disabledContentColor = palette.textMuted
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("email_auth_submit_button")
        ) {
            if (isEmailBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = palette.onAccent,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (authMode == EmailAuthMode.SIGN_IN) "Entrando..." else "Criando conta...",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = stringResource(
                        if (authMode == EmailAuthMode.SIGN_IN) R.string.action_sign_in_email
                        else R.string.action_create_account
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = palette.cardBorder)
            Text(
                text = stringResource(R.string.login_divider_or),
                style = MaterialTheme.typography.labelMedium,
                color = palette.textMuted
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = palette.cardBorder)
        }

        Button(
            onClick = onGoogleClick,
            enabled = !isBusy,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.secondaryCard,
                contentColor = palette.onSecondaryCard,
                disabledContainerColor = palette.secondaryCard.copy(alpha = 0.5f),
                disabledContentColor = palette.onSecondaryCard.copy(alpha = 0.6f)
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("google_signin_button")
        ) {
            if (isGoogleBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = palette.onSecondaryCard,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Conectando com Google...",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(palette.onSecondaryCard.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "G",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.onSecondaryCard
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.action_sign_in_google),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = stringResource(R.string.login_footer_note),
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted
        )
    }
}

@Composable
private fun AuthModeTabButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberLoginPalette()
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(9.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) palette.accent else Color.Transparent,
            contentColor = if (selected) palette.onAccent else palette.textMuted,
            disabledContainerColor = if (selected) palette.accentDisabledBg else Color.Transparent,
            disabledContentColor = palette.textMuted
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        contentPadding = PaddingValues(vertical = 10.dp),
        modifier = modifier.height(48.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
