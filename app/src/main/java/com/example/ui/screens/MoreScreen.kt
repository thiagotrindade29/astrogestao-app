package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.local.CategoryEntity
import com.example.data.local.HouseholdUserEntity
import com.example.data.local.MovementType
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MovementTypeBadge
import com.example.ui.components.formatBrl
import com.example.ui.components.formatDateTimeBr
import com.example.ui.components.formatQty
import com.example.ui.theme.AstroTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.AstroUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MoreScreen(
    uiState: AstroUiState,
    initialTab: Int = 0,
    onSelectMovementFilter: (MovementType?) -> Unit,
    onNewCategory: () -> Unit,
    onEditCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onNewUser: () -> Unit,
    onDeleteUser: (HouseholdUserEntity) -> Unit,
    onRenameHousehold: (String) -> Unit,
    onRegenerateInviteCode: () -> Unit,
    onJoinHousehold: (String) -> Unit,
    onCreatePersonalHousehold: (String) -> Unit,
    onMigrateLocalToCloud: () -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onLoadSampleCatalog: () -> Unit,
    onExportBackup: (Uri) -> Unit,
    onRestoreBackup: (Uri) -> Unit,
    onBuildDiagnosticReport: () -> String,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var pendingDeleteCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showSampleOverwriteConfirm by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showJoinHouseholdDialog by remember { mutableStateOf(false) }
    var showRenameHouseholdDialog by remember { mutableStateOf(false) }
    var showCreateHouseholdDialog by remember { mutableStateOf(false) }

    pendingDeleteCategory?.let { cat ->
        ConfirmationDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_category, cat.name),
            onConfirm = {
                onDeleteCategory(cat)
                pendingDeleteCategory = null
            },
            onDismiss = { pendingDeleteCategory = null }
        )
    }

    if (showSampleOverwriteConfirm) {
        ConfirmationDialog(
            title = stringResource(R.string.confirm_sample_title),
            message = stringResource(R.string.confirm_sample_body),
            confirmLabel = stringResource(R.string.action_replace),
            onConfirm = {
                showSampleOverwriteConfirm = false
                onLoadSampleCatalog()
            },
            onDismiss = { showSampleOverwriteConfirm = false }
        )
    }

    pendingRestoreUri?.let { uri ->
        ConfirmationDialog(
            title = stringResource(R.string.confirm_restore_title),
            message = stringResource(R.string.confirm_restore_body),
            confirmLabel = stringResource(R.string.action_replace),
            onConfirm = {
                pendingRestoreUri = null
                onRestoreBackup(uri)
            },
            onDismiss = { pendingRestoreUri = null }
        )
    }

    if (showJoinHouseholdDialog) {
        JoinHouseholdDialog(
            onJoin = { code ->
                showJoinHouseholdDialog = false
                onJoinHousehold(code)
            },
            onDismiss = { showJoinHouseholdDialog = false }
        )
    }

    if (showRenameHouseholdDialog) {
        HouseholdNameDialog(
            title = "Renomear Espaço",
            initialName = uiState.householdName,
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = { newName ->
                showRenameHouseholdDialog = false
                onRenameHousehold(newName)
            },
            onDismiss = { showRenameHouseholdDialog = false }
        )
    }

    if (showCreateHouseholdDialog) {
        HouseholdNameDialog(
            title = "Criar Nova Casa Individual",
            initialName = "Casa de ${uiState.userName.substringBefore(" ").ifBlank { "Mim" }}",
            confirmLabel = "Criar Espaço",
            onConfirm = { newName ->
                showCreateHouseholdDialog = false
                onCreatePersonalHousehold(newName)
            },
            onDismiss = { showCreateHouseholdDialog = false }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.more_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        val tabs = listOf(
            Triple(0, R.string.tab_movements, "more_tab_movements"),
            Triple(1, R.string.tab_categories, "more_tab_categories"),
            Triple(2, R.string.tab_users, "more_tab_users")
        )
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEach { (index, labelRes, tag) ->
                val isSelected = selectedTab == index
                Tab(
                    selected = isSelected,
                    onClick = { selectedTab = index },
                    modifier = Modifier.testTag(tag)
                ) {
                    Text(
                        text = stringResource(labelRes),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 14.dp)
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> MovementsAuditTab(
                uiState = uiState,
                onSelectFilter = onSelectMovementFilter
            )
            1 -> CategoriesTab(
                uiState = uiState,
                onNewCategory = onNewCategory,
                onEditCategory = onEditCategory,
                onDeleteCategory = { pendingDeleteCategory = it }
            )
            else -> UsersAndSettingsTab(
                uiState = uiState,
                onNewUser = onNewUser,
                onDeleteUser = onDeleteUser,
                onOpenRenameHousehold = { showRenameHouseholdDialog = true },
                onRegenerateInviteCode = onRegenerateInviteCode,
                onOpenJoinHousehold = { showJoinHouseholdDialog = true },
                onOpenCreatePersonalHousehold = { showCreateHouseholdDialog = true },
                onMigrateLocalToCloud = onMigrateLocalToCloud,
                onToggleDarkMode = onToggleDarkMode,
                onRequestSampleCatalog = {
                    if (uiState.products.isNotEmpty() || uiState.movements.isNotEmpty()) {
                        showSampleOverwriteConfirm = true
                    } else {
                        onLoadSampleCatalog()
                    }
                },
                onExportBackup = onExportBackup,
                onRequestRestoreBackup = { uri -> pendingRestoreUri = uri },
                onBuildDiagnosticReport = onBuildDiagnosticReport,
                onSignOut = onSignOut
            )
        }
    }
}

@Composable
private fun MovementsAuditTab(
    uiState: AstroUiState,
    onSelectFilter: (MovementType?) -> Unit
) {
    val extended = AstroTheme.extended
    val movements = uiState.filteredMovements

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("movements_audit_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Histórico com Auditoria",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Registro automático de compras, saídas no catálogo e ajustes de estoque.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.movementFilterType == null,
                    onClick = { onSelectFilter(null) },
                    label = { Text(stringResource(R.string.filter_all)) }
                )
                MovementType.entries.forEach { t ->
                    FilterChip(
                        selected = uiState.movementFilterType == t,
                        onClick = {
                            onSelectFilter(if (uiState.movementFilterType == t) null else t)
                        },
                        label = { Text(t.label) }
                    )
                }
            }
        }

        if (movements.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.History,
                    title = "Sem movimentações registradas",
                    body = stringResource(R.string.movements_empty)
                )
            }
        } else {
            items(movements, key = { it.id }) { mv ->
                val deltaSign = if (mv.quantityDelta >= 0) "+" else ""
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("movement_audit_card_${mv.id}"),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, extended.borderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MovementTypeBadge(type = mv.type)
                            Text(
                                text = formatDateTimeBr(mv.timestampMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = mv.productNameSnapshot,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$deltaSign${formatQty(mv.quantityDelta)} ${mv.productUnitSnapshot}  •  Saldo: ${formatQty(mv.previousBalance)} → ${formatQty(mv.newBalance)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = formatBrl(mv.totalValue),
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "${mv.reason} • Auditoria: ${mv.authorName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoriesTab(
    uiState: AstroUiState,
    onNewCategory: () -> Unit,
    onEditCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit
) {
    val extended = AstroTheme.extended
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("categories_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Setores da Casa",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onNewCategory,
                    modifier = Modifier.testTag("add_category_button")
                ) {
                    Text(stringResource(R.string.action_new_category))
                }
            }
        }
        items(uiState.categories, key = { it.id }) { cat ->
            val count = uiState.products.count { it.categoryId == cat.id }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditCategory(cat) }
                    .testTag("category_card_${cat.id}"),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, extended.borderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(cat.colorHex))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = listOfNotNull(
                                cat.sectorNote.takeIf { it.isNotBlank() },
                                "$count produto(s)"
                            ).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onEditCategory(cat) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.action_edit))
                    }
                    IconButton(
                        onClick = { onDeleteCategory(cat) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = extended.negative
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UsersAndSettingsTab(
    uiState: AstroUiState,
    onNewUser: () -> Unit,
    onDeleteUser: (HouseholdUserEntity) -> Unit,
    onOpenRenameHousehold: () -> Unit,
    onRegenerateInviteCode: () -> Unit,
    onOpenJoinHousehold: () -> Unit,
    onOpenCreatePersonalHousehold: () -> Unit,
    onMigrateLocalToCloud: () -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onRequestSampleCatalog: () -> Unit,
    onExportBackup: (Uri) -> Unit,
    onRequestRestoreBackup: (Uri) -> Unit,
    onBuildDiagnosticReport: () -> String,
    onSignOut: () -> Unit
) {
    val extended = AstroTheme.extended
    val context = LocalContext.current

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportBackup(uri)
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onRequestRestoreBackup(uri)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.hasLocalMigrationAvailable) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Dados locais encontrados neste aparelho",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Encontramos produtos cadastrados antes da atualização na nuvem. Deseja enviá-los para este espaço sincronizado?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onMigrateLocalToCloud,
                            enabled = !uiState.isSyncingWorkspace,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("migrate_local_cloud_button")
                        ) {
                            Text("Enviar Dados Locais para a Nuvem")
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, extended.borderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.userName.ifBlank { "Conta Google" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.userEmail.isNotBlank()) {
                                Text(
                                    text = uiState.userEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = extended.negative),
                            modifier = Modifier.testTag("logout_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_logout))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.label_dark_mode), style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = uiState.isDarkMode,
                            onCheckedChange = onToggleDarkMode,
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }

                    OutlinedButton(
                        onClick = onRequestSampleCatalog,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("load_sample_catalog_button")
                    ) {
                        Text(stringResource(R.string.action_load_sample_catalog))
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, extended.borderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Rounded.FamilyRestroom,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = uiState.householdName.ifBlank { "Meu Espaço na Nuvem" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${uiState.memberCount} conta(s) conectada(s) neste banco",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = onOpenRenameHousehold,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Renomear casa")
                        }
                    }

                    if (uiState.householdInviteCode.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, extended.borderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Código de Convite da Família",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = uiState.householdInviteCode,
                                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMonoFamily),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row {
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(
                                                    ClipData.newPlainText("Código AstroGestão", uiState.householdInviteCode)
                                                )
                                                Toast.makeText(context, "Código copiado!", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copiar código")
                                        }
                                        IconButton(
                                            onClick = {
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(
                                                        Intent.EXTRA_TEXT,
                                                        "Entre na nossa casa \"${uiState.householdName}\" no AstroGestão usando o código: ${uiState.householdInviteCode}"
                                                    )
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Convidar família"))
                                            }
                                        ) {
                                            Icon(Icons.Rounded.Share, contentDescription = "Compartilhar código")
                                        }
                                        if (uiState.isHouseholdOwner) {
                                            IconButton(
                                                onClick = onRegenerateInviteCode,
                                                enabled = !uiState.isSyncingWorkspace
                                            ) {
                                                Icon(Icons.Rounded.Refresh, contentDescription = "Gerar novo código")
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = "Envie este código para um familiar entrar com a conta Google dele e gerenciar o mesmo estoque e finanças.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenJoinHousehold,
                            enabled = !uiState.isSyncingWorkspace,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("join_household_button")
                        ) {
                            Icon(Icons.Rounded.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Entrar por Código", maxLines = 1, softWrap = false)
                        }
                        OutlinedButton(
                            onClick = onOpenCreatePersonalHousehold,
                            enabled = !uiState.isSyncingWorkspace,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_household_button")
                        ) {
                            Icon(Icons.Rounded.HomeWork, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Nova Casa", maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, extended.borderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.section_backup_beta),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                exportBackupLauncher.launch("astrogestao_backup_$stamp.json")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_backup_button")
                        ) {
                            Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exportar", maxLines = 1, softWrap = false)
                        }
                        OutlinedButton(
                            onClick = {
                                restoreBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("restore_backup_button")
                        ) {
                            Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restaurar", maxLines = 1, softWrap = false)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val report = onBuildDiagnosticReport()
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Feedback AstroGestão v${BuildConfig.VERSION_NAME}")
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Enviar feedback"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_diagnostics_button")
                    ) {
                        Icon(Icons.Rounded.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_share_diagnostics))
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Moradores & Operadores",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = onNewUser) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adicionar")
                }
            }
        }

        items(uiState.users, key = { it.id }) { user ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, extended.borderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.PersonOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = listOfNotNull(
                                user.email.takeIf { it.isNotBlank() },
                                user.role
                            ).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (uiState.users.size > 1 && user.email != uiState.userEmail) {
                        IconButton(onClick = { onDeleteUser(user) }) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = extended.negative
                            )
                        }
                    }
                }
            }
        }
    }
}
