package com.example.ui.screens

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CategoryEntity

private val categoryColorPalette = listOf(
    0xFF5B3DF5L,
    0xFF0284C7L,
    0xFF059669L,
    0xFFD97706L,
    0xFFDC2626L,
    0xFF7C3AEDL,
    0xFFDB2777L,
    0xFF0F172AL
)

@Composable
fun CategoryEditorDialog(
    initialCategory: CategoryEntity?,
    onSave: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var sectorNote by remember { mutableStateOf(initialCategory?.sectorNote ?: "") }
    var colorHex by remember {
        mutableLongStateOf(initialCategory?.colorHex ?: categoryColorPalette.first())
    }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCategory == null) "Nova Categoria" else "Editar Categoria",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text(stringResource(R.string.label_name)) },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text(stringResource(R.string.error_required)) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_name_input")
                )

                OutlinedTextField(
                    value = sectorNote,
                    onValueChange = { sectorNote = it },
                    label = { Text(stringResource(R.string.label_sector_note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(stringResource(R.string.label_color), style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    categoryColorPalette.forEach { hex ->
                        val isSelected = colorHex == hex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .then(
                                    if (isSelected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    onSave(
                        CategoryEntity(
                            id = initialCategory?.id ?: 0L,
                            name = name.trim(),
                            colorHex = colorHex,
                            sectorNote = sectorNote.trim()
                        )
                    )
                },
                modifier = Modifier.testTag("save_category_button")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun UserEditorDialog(
    onSave: (name: String, email: String, role: String, password: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Morador") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Morador / Usuário", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorText = null
                    },
                    label = { Text(stringResource(R.string.label_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorText = null
                    },
                    label = { Text(stringResource(R.string.label_email)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorText = null
                    },
                    label = { Text("Senha de acesso (opcional)") },
                    supportingText = {
                        Text("Opcional (mín. 6 caracteres) se este usuário for entrar no app.")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Administrador", "Morador").forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r) }
                        )
                    }
                }
                errorText?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedPassword = password.trim()
                    when {
                        name.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> {
                            errorText = "Informe nome e e-mail válidos."
                        }
                        trimmedPassword.isNotEmpty() && trimmedPassword.length < 6 -> {
                            errorText = "A senha deve ter ao menos 6 caracteres (ou deixe em branco)."
                        }
                        else -> onSave(name.trim(), email.trim(), role, trimmedPassword)
                    }
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
