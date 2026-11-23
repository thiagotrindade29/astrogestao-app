package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ProjectEntity
import com.example.data.local.TaskEntity
import com.example.ui.components.CashFlowCanvasChart
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatShortDate
import com.example.ui.theme.AstroTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.AstroUiState

@Composable
fun DashboardScreen(
    uiState: AstroUiState,
    onOpenSettings: () -> Unit,
    onNewProject: () -> Unit,
    onNewTransaction: () -> Unit,
    onLoadSampleData: () -> Unit,
    onOpenProject: (Long) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = AstroTheme.extended

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Executive Hero Card with bundled img_astro_hero banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_hero_card"),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, extended.borderSubtle),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(206.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_astro_hero),
                        contentDescription = stringResource(R.string.dashboard_hero_banner_cd),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xEE090D1A),
                                        Color(0xBF0F1629),
                                        Color(0x550F1629)
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.dashboard_eyebrow),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.businessName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color(0xFFF8FAFC),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.dashboard_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFCBD5E1),
                                    maxLines = 2
                                )
                            }
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = stringResource(R.string.action_settings),
                                    tint = Color(0xFFF8FAFC)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onNewProject,
                                modifier = Modifier.testTag("hero_new_project_button"),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = stringResource(R.string.action_new_project))
                            }
                            OutlinedButton(
                                onClick = onNewTransaction,
                                modifier = Modifier.testTag("hero_new_transaction_button"),
                                border = BorderStroke(1.dp, Color(0x66F8FAFC)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFF8FAFC)
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddTask,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = stringResource(R.string.action_new_transaction))
                            }
                        }
                    }
                }
            }
        }

        // First-launch Empty Workspace Prompt
        if (uiState.isWorkspaceEmpty && !uiState.isLoading) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.AutoAwesome,
                    title = stringResource(R.string.empty_workspace_title),
                    body = stringResource(R.string.empty_workspace_body),
                    primaryActionLabel = stringResource(R.string.action_load_sample_data),
                    onPrimaryAction = onLoadSampleData,
                    secondaryActionLabel = stringResource(R.string.action_new_project),
                    onSecondaryAction = onNewProject,
                    testTag = "dashboard_empty_state"
                )
            }
        }

        // Responsive KPI Grid (2 columns on phones, 4 columns on wider tablets)
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 680.dp
                val balanceColor = if (uiState.netBalance >= 0) extended.positive else extended.negative
                val balanceContainer = if (uiState.netBalance >= 0) extended.positiveContainer else extended.negativeContainer

                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiMetricCard(
                            title = stringResource(R.string.kpi_net_balance),
                            value = formatCurrency(uiState.netBalance),
                            subtitle = "${uiState.profitMarginPercent}% margin",
                            icon = Icons.Rounded.AccountBalanceWallet,
                            accentColor = balanceColor,
                            accentContainer = balanceContainer,
                            modifier = Modifier.weight(1f),
                            testTag = "kpi_net_balance"
                        )
                        KpiMetricCard(
                            title = stringResource(R.string.kpi_total_revenue),
                            value = formatCurrency(uiState.totalIncome),
                            subtitle = "${uiState.transactions.size} entries",
                            icon = Icons.Rounded.TrendingUp,
                            accentColor = MaterialTheme.colorScheme.primary,
                            accentContainer = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.weight(1f),
                            testTag = "kpi_total_revenue"
                        )
                        KpiMetricCard(
                            title = stringResource(R.string.kpi_active_projects),
                            value = uiState.activeProjectCount.toString(),
                            subtitle = "${uiState.projects.size} total orders",
                            icon = Icons.Rounded.WorkOutline,
                            accentColor = extended.info,
                            accentContainer = extended.infoContainer,
                            modifier = Modifier.weight(1f),
                            testTag = "kpi_active_projects"
                        )
                        KpiMetricCard(
                            title = stringResource(R.string.kpi_open_tasks),
                            value = uiState.openTaskCount.toString(),
                            subtitle = "${uiState.clients.size} clients",
                            icon = Icons.Rounded.AssignmentTurnedIn,
                            accentColor = extended.review,
                            accentContainer = extended.reviewContainer,
                            modifier = Modifier.weight(1f),
                            testTag = "kpi_open_tasks"
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KpiMetricCard(
                                title = stringResource(R.string.kpi_net_balance),
                                value = formatCurrency(uiState.netBalance),
                                subtitle = "${uiState.profitMarginPercent}% margin",
                                icon = Icons.Rounded.AccountBalanceWallet,
                                accentColor = balanceColor,
                                accentContainer = balanceContainer,
                                modifier = Modifier.weight(1f),
                                testTag = "kpi_net_balance"
                            )
                            KpiMetricCard(
                                title = stringResource(R.string.kpi_total_revenue),
                                value = formatCurrency(uiState.totalIncome),
                                subtitle = "${uiState.transactions.size} entries",
                                icon = Icons.Rounded.TrendingUp,
                                accentColor = MaterialTheme.colorScheme.primary,
                                accentContainer = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.weight(1f),
                                testTag = "kpi_total_revenue"
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KpiMetricCard(
                                title = stringResource(R.string.kpi_active_projects),
                                value = uiState.activeProjectCount.toString(),
                                subtitle = "${uiState.projects.size} total orders",
                                icon = Icons.Rounded.WorkOutline,
                                accentColor = extended.info,
                                accentContainer = extended.infoContainer,
                                modifier = Modifier.weight(1f),
                                testTag = "kpi_active_projects"
                            )
                            KpiMetricCard(
                                title = stringResource(R.string.kpi_open_tasks),
                                value = uiState.openTaskCount.toString(),
                                subtitle = "${uiState.clients.size} clients",
                                icon = Icons.Rounded.AssignmentTurnedIn,
                                accentColor = extended.review,
                                accentContainer = extended.reviewContainer,
                                modifier = Modifier.weight(1f),
                                testTag = "kpi_open_tasks"
                            )
                        }
                    }
                }
            }
        }

        // Cash Flow Pulse Canvas Chart
        if (uiState.transactions.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cash_flow_chart_card"),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, extended.borderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.section_cash_flow_pulse),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.section_cash_flow_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        CashFlowCanvasChart(points = uiState.monthlyCashFlow)
                    }
                }
            }
        }

        // Active Deadlines Section
        if (uiState.upcomingProjects.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.section_upcoming_projects),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(uiState.upcomingProjects, key = { "dash_proj_${it.id}" }) { project ->
                DashboardProjectRow(
                    project = project,
                    clientName = project.clientId?.let { uiState.clientMap[it]?.company ?: uiState.clientMap[it]?.name },
                    onClick = { onOpenProject(project.id) }
                )
            }
        }

        // Priority Deliverables Section
        if (uiState.priorityOpenTasks.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.section_priority_tasks),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(uiState.priorityOpenTasks, key = { "dash_task_${it.id}" }) { task ->
                val projectTitle = uiState.projectMap[task.projectId]?.code ?: ""
                DashboardTaskRow(
                    task = task,
                    projectCode = projectTitle,
                    onToggle = { onToggleTask(task) },
                    onOpenProject = { onOpenProject(task.projectId) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun DashboardProjectRow(
    project: ProjectEntity,
    clientName: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("dashboard_project_${project.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, AstroTheme.extended.borderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    Text(
                        text = project.code,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(status = project.status)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = clientName ?: stringResource(R.string.project_client_unassigned),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${stringResource(R.string.project_due_label)} ${formatShortDate(project.dueDateMillis)} • ${project.progressPercent}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { (project.progressPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun DashboardTaskRow(
    task: TaskEntity,
    projectCode: String,
    onToggle: () -> Unit,
    onOpenProject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onOpenProject)
            .testTag("dashboard_task_${task.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, AstroTheme.extended.borderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("toggle_task_${task.id}")
            ) {
                Icon(
                    imageVector = if (task.isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = if (task.isDone) "Mark incomplete" else "Mark completed",
                    tint = if (task.isDone) AstroTheme.extended.positive else MaterialTheme.colorScheme.primary
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (projectCode.isNotBlank()) {
                        Text(
                            text = projectCode,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    PriorityBadge(priority = task.priority)
                    Text(
                        text = formatShortDate(task.dueDateMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
