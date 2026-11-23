package com.example.ui.screens

import android.util.Patterns
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ClientEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectStatus
import com.example.data.local.TaskPriority
import com.example.data.local.TransactionCategory
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.ui.components.ConfirmationDialog

@Composable
fun ProjectEditorDialog(
    initialProject: ProjectEntity?,
    preselectedClientId: Long?,
    clients: List<ClientEntity>,
    nextDefaultCode: String,
    onSave: (ProjectEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf(initialProject?.code ?: nextDefaultCode) }
    var title by remember { mutableStateOf(initialProject?.title ?: "") }
    var description by remember { mutableStateOf(initialProject?.description ?: "") }
    var budgetText by remember {
        mutableStateOf(
            if (initialProject != null && initialProject.budget > 0) initialProject.budget.toString() else ""
        )
    }
    var dueDaysText by remember { mutableStateOf("14") }
    var selectedClientId by remember { mutableStateOf(initialProject?.clientId ?: preselectedClientId) }
    var selectedStatus by remember { mutableStateOf(initialProject?.status ?: ProjectStatus.PLANNED) }
    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialProject == null) stringResource(R.string.action_new_project) else "Edit Project",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(R.string.label_code)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_code_input")
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = false
                    },
                    label = { Text(stringResource(R.string.label_title)) },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text(stringResource(R.string.error_required_field)) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_title_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.label_description)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = { budgetText = it },
                        label = { Text(stringResource(R.string.label_budget)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("project_budget_input")
                    )
                    if (initialProject == null) {
                        OutlinedTextField(
                            value = dueDaysText,
                            onValueChange = { dueDaysText = it },
                            label = { Text(stringResource(R.string.label_due_in_days)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (clients.isNotEmpty()) {
                    Text("Client", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedClientId == null,
                            onClick = { selectedClientId = null },
                            label = { Text("None") }
                        )
                        clients.forEach { client ->
                            FilterChip(
                                selected = selectedClientId == client.id,
                                onClick = { selectedClientId = client.id },
                                label = { Text(client.company.ifBlank { client.name }) }
                            )
                        }
                    }
                }

                Text("Status", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ProjectStatus.entries.forEach { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = st },
                            label = { Text(st.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        return@Button
                    }
                    val budget = budgetText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    val days = dueDaysText.toIntOrNull()?.coerceAtLeast(1) ?: 14
                    val dueMillis = initialProject?.dueDateMillis
                        ?: (System.currentTimeMillis() + days * 86_400_000L)
                    onSave(
                        ProjectEntity(
                            id = initialProject?.id ?: 0L,
                            code = code.trim().ifBlank { nextDefaultCode },
                            title = title.trim(),
                            clientId = selectedClientId,
                            description = description.trim(),
                            status = selectedStatus,
                            budget = budget,
                            progressPercent = initialProject?.progressPercent ?: 0,
                            dueDateMillis = dueMillis,
                            createdAt = initialProject?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                },
                modifier = Modifier.testTag("save_project_button")
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
fun ClientEditorDialog(
    initialClient: ClientEntity?,
    onSave: (ClientEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialClient?.name ?: "") }
    var company by remember { mutableStateOf(initialClient?.company ?: "") }
    var segment by remember { mutableStateOf(initialClient?.segment ?: "") }
    var email by remember { mutableStateOf(initialClient?.email ?: "") }
    var phone by remember { mutableStateOf(initialClient?.phone ?: "") }
    var notes by remember { mutableStateOf(initialClient?.notes ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialClient == null) stringResource(R.string.action_new_client) else "Edit Client",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text(stringResource(R.string.label_client_name)) },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text(stringResource(R.string.error_required_field)) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_name_input")
                )
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text(stringResource(R.string.label_company)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_company_input")
                )
                OutlinedTextField(
                    value = segment,
                    onValueChange = { segment = it },
                    label = { Text(stringResource(R.string.label_segment)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = false
                    },
                    label = { Text(stringResource(R.string.label_email)) },
                    isError = emailError,
                    supportingText = if (emailError) {
                        { Text(stringResource(R.string.error_invalid_email)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_email_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.label_phone)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.label_notes)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val trimmedEmail = email.trim()
                    if (trimmedEmail.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                        emailError = true
                        return@Button
                    }
                    onSave(
                        ClientEntity(
                            id = initialClient?.id ?: 0L,
                            name = name.trim(),
                            company = company.trim(),
                            segment = segment.trim(),
                            email = trimmedEmail,
                            phone = phone.trim(),
                            notes = notes.trim(),
                            createdAt = initialClient?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                },
                modifier = Modifier.testTag("save_client_button")
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
fun TaskCreatorDialog(
    onSave: (title: String, priority: TaskPriority, dueInDays: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var dueDays by remember { mutableStateOf("5") }
    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.project_add_task), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = false
                    },
                    label = { Text(stringResource(R.string.label_title)) },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text(stringResource(R.string.error_required_field)) }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )
                OutlinedTextField(
                    value = dueDays,
                    onValueChange = { dueDays = it },
                    label = { Text(stringResource(R.string.label_due_in_days)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.label_priority), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        return@Button
                    }
                    onSave(title.trim(), priority, dueDays.toIntOrNull()?.coerceAtLeast(0) ?: 5)
                },
                modifier = Modifier.testTag("save_task_button")
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
fun TransactionEditorDialog(
    initialTransaction: TransactionEntity?,
    prelinkedProject: ProjectEntity?,
    projects: List<ProjectEntity>,
    onSave: (TransactionEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember {
        mutableStateOf(
            initialTransaction?.title
                ?: prelinkedProject?.let { "${it.code} Payment" }
                ?: ""
        )
    }
    var type by remember {
        mutableStateOf(initialTransaction?.type ?: TransactionType.INCOME)
    }
    var category by remember {
        mutableStateOf(initialTransaction?.category ?: TransactionCategory.PROJECT_FEE)
    }
    var amountText by remember {
        mutableStateOf(
            initialTransaction?.amount?.toString()
                ?: prelinkedProject?.budget?.takeIf { it > 0.0 }?.toString()
                ?: ""
        )
    }
    var selectedProjectId by remember {
        mutableStateOf(initialTransaction?.projectId ?: prelinkedProject?.id)
    }
    var notes by remember { mutableStateOf(initialTransaction?.notes ?: "") }
    var titleError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialTransaction == null) stringResource(R.string.action_new_transaction) else "Edit Entry",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = {
                                type = t
                                category = if (t == TransactionType.INCOME) {
                                    TransactionCategory.PROJECT_FEE
                                } else {
                                    TransactionCategory.OPERATIONS
                                }
                            },
                            label = { Text(t.label) },
                            modifier = Modifier.testTag("tx_type_${t.name.lowercase()}")
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = false
                    },
                    label = { Text(stringResource(R.string.label_title)) },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text(stringResource(R.string.error_required_field)) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_title_input")
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = false
                    },
                    label = { Text(stringResource(R.string.label_amount)) },
                    isError = amountError,
                    supportingText = if (amountError) {
                        { Text(stringResource(R.string.error_invalid_amount)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_amount_input")
                )

                Text(stringResource(R.string.label_category), style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TransactionCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }

                if (projects.isNotEmpty()) {
                    Text("Link Project", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedProjectId == null,
                            onClick = { selectedProjectId = null },
                            label = { Text("None") }
                        )
                        projects.forEach { p ->
                            FilterChip(
                                selected = selectedProjectId == p.id,
                                onClick = { selectedProjectId = p.id },
                                label = { Text(p.code) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.label_notes)) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        return@Button
                    }
                    val amount = amountText.replace(",", ".").toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        amountError = true
                        return@Button
                    }
                    val linkedClient = selectedProjectId?.let { pid ->
                        projects.find { it.id == pid }?.clientId
                    } ?: initialTransaction?.clientId
                    onSave(
                        TransactionEntity(
                            id = initialTransaction?.id ?: 0L,
                            title = title.trim(),
                            type = type,
                            category = category,
                            amount = amount,
                            projectId = selectedProjectId,
                            clientId = linkedClient,
                            dateMillis = initialTransaction?.dateMillis ?: System.currentTimeMillis(),
                            notes = notes.trim(),
                            createdAt = initialTransaction?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                },
                modifier = Modifier.testTag("save_tx_button")
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
fun WorkspaceSettingsDialog(
    businessName: String,
    isDarkMode: Boolean,
    onUpdateBusinessName: (String) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onLoadSampleData: () -> Unit,
    onClearWorkspace: () -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf(businessName) }
    var confirmReset by remember { mutableStateOf(false) }

    if (confirmReset) {
        ConfirmationDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_reset_workspace),
            onConfirm = {
                confirmReset = false
                onClearWorkspace()
                onDismiss()
            },
            onDismiss = { confirmReset = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_settings), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(stringResource(R.string.label_business_name)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_business_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.label_dark_theme),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onToggleDarkMode,
                        modifier = Modifier.testTag("settings_dark_theme_switch")
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                OutlinedButton(
                    onClick = {
                        onLoadSampleData()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_load_sample_data))
                }

                OutlinedButton(
                    onClick = { confirmReset = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_clear_workspace))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isNotBlank() && nameInput.trim() != businessName) {
                        onUpdateBusinessName(nameInput.trim())
                    }
                    onDismiss()
                },
                modifier = Modifier.testTag("save_settings_button")
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
