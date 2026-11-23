package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.local.ClientEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.TransactionEntity
import com.example.ui.screens.ClientDetailScreen
import com.example.ui.screens.ClientEditorDialog
import com.example.ui.screens.ClientsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FinanceScreen
import com.example.ui.screens.ProjectDetailScreen
import com.example.ui.screens.ProjectEditorDialog
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.TaskCreatorDialog
import com.example.ui.screens.TransactionEditorDialog
import com.example.ui.screens.WorkspaceSettingsDialog
import com.example.ui.viewmodel.AstroViewModel

object AstroRoutes {
    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_PROJECTS = "projects"
    const val ROUTE_CLIENTS = "clients"
    const val ROUTE_FINANCE = "finance"
    const val ROUTE_PROJECT_DETAIL = "project_detail/{projectId}"
    const val ROUTE_CLIENT_DETAIL = "client_detail/{clientId}"

    fun projectDetail(projectId: Long) = "project_detail/$projectId"
    fun clientDetail(clientId: Long) = "client_detail/$clientId"
}

private enum class TopDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD(
        route = AstroRoutes.ROUTE_DASHBOARD,
        labelRes = R.string.nav_dashboard,
        selectedIcon = Icons.Rounded.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_dashboard"
    ),
    PROJECTS(
        route = AstroRoutes.ROUTE_PROJECTS,
        labelRes = R.string.nav_projects,
        selectedIcon = Icons.Rounded.Work,
        unselectedIcon = Icons.Outlined.WorkOutline,
        testTag = "nav_projects"
    ),
    CLIENTS(
        route = AstroRoutes.ROUTE_CLIENTS,
        labelRes = R.string.nav_clients,
        selectedIcon = Icons.Rounded.Groups,
        unselectedIcon = Icons.Outlined.Groups,
        testTag = "nav_clients"
    ),
    FINANCE(
        route = AstroRoutes.ROUTE_FINANCE,
        labelRes = R.string.nav_finance,
        selectedIcon = Icons.Rounded.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet,
        testTag = "nav_finance"
    )
}

private sealed interface ActiveDialog {
    data class ProjectForm(
        val project: ProjectEntity? = null,
        val preselectedClientId: Long? = null
    ) : ActiveDialog

    data class ClientForm(val client: ClientEntity? = null) : ActiveDialog
    data class TaskForm(val projectId: Long) : ActiveDialog
    data class TransactionForm(
        val transaction: TransactionEntity? = null,
        val prelinkedProject: ProjectEntity? = null
    ) : ActiveDialog

    data object Settings : ActiveDialog
}

@Composable
fun AstroGestaoApp(viewModel: AstroViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    var activeDialog by remember { mutableStateOf<ActiveDialog?>(null) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val isTopLevel = currentRoute in TopDestination.entries.map { it.route }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Active modals & dialogs
    when (val dialog = activeDialog) {
        is ActiveDialog.ProjectForm -> {
            val nextCode = "AG-${101 + uiState.projects.size}"
            ProjectEditorDialog(
                initialProject = dialog.project,
                preselectedClientId = dialog.preselectedClientId,
                clients = uiState.clients,
                nextDefaultCode = nextCode,
                onSave = {
                    viewModel.saveProject(it)
                    activeDialog = null
                },
                onDismiss = { activeDialog = null }
            )
        }
        is ActiveDialog.ClientForm -> {
            ClientEditorDialog(
                initialClient = dialog.client,
                onSave = {
                    viewModel.saveClient(it)
                    activeDialog = null
                },
                onDismiss = { activeDialog = null }
            )
        }
        is ActiveDialog.TaskForm -> {
            TaskCreatorDialog(
                onSave = { title, priority, dueDays ->
                    viewModel.saveTask(dialog.projectId, title, priority, dueDays)
                    activeDialog = null
                },
                onDismiss = { activeDialog = null }
            )
        }
        is ActiveDialog.TransactionForm -> {
            TransactionEditorDialog(
                initialTransaction = dialog.transaction,
                prelinkedProject = dialog.prelinkedProject,
                projects = uiState.projects,
                onSave = {
                    viewModel.saveTransaction(it)
                    activeDialog = null
                },
                onDismiss = { activeDialog = null }
            )
        }
        ActiveDialog.Settings -> {
            WorkspaceSettingsDialog(
                businessName = uiState.businessName,
                isDarkMode = uiState.isDarkMode,
                onUpdateBusinessName = viewModel::updateBusinessName,
                onToggleDarkMode = viewModel::setDarkMode,
                onLoadSampleData = viewModel::loadDemoWorkspace,
                onClearWorkspace = viewModel::clearWorkspace,
                onDismiss = { activeDialog = null }
            )
        }
        null -> Unit
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!useRail && isTopLevel) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        TopDestination.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (isTopLevel) {
                    val fabAction: () -> Unit = when (currentRoute) {
                        AstroRoutes.ROUTE_CLIENTS -> { -> activeDialog = ActiveDialog.ClientForm() }
                        AstroRoutes.ROUTE_FINANCE -> { -> activeDialog = ActiveDialog.TransactionForm() }
                        else -> { -> activeDialog = ActiveDialog.ProjectForm() }
                    }
                    val fabDescription = when (currentRoute) {
                        AstroRoutes.ROUTE_CLIENTS -> stringResource(R.string.action_new_client)
                        AstroRoutes.ROUTE_FINANCE -> stringResource(R.string.action_new_transaction)
                        else -> stringResource(R.string.action_new_project)
                    }
                    FloatingActionButton(
                        onClick = fabAction,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("primary_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = fabDescription
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && isTopLevel) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        TopDestination.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = AstroRoutes.ROUTE_DASHBOARD,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 920.dp)
                    ) {
                        composable(AstroRoutes.ROUTE_DASHBOARD) {
                            DashboardScreen(
                                uiState = uiState,
                                onOpenSettings = { activeDialog = ActiveDialog.Settings },
                                onNewProject = { activeDialog = ActiveDialog.ProjectForm() },
                                onNewTransaction = { activeDialog = ActiveDialog.TransactionForm() },
                                onLoadSampleData = viewModel::loadDemoWorkspace,
                                onOpenProject = { id ->
                                    navController.navigate(AstroRoutes.projectDetail(id))
                                },
                                onToggleTask = viewModel::toggleTaskDone
                            )
                        }

                        composable(AstroRoutes.ROUTE_PROJECTS) {
                            ProjectsScreen(
                                uiState = uiState,
                                onSearchChange = viewModel::updateProjectSearch,
                                onSelectStatus = viewModel::selectProjectStatus,
                                onOpenProject = { id ->
                                    navController.navigate(AstroRoutes.projectDetail(id))
                                },
                                onCreateProject = { activeDialog = ActiveDialog.ProjectForm() }
                            )
                        }

                        composable(AstroRoutes.ROUTE_CLIENTS) {
                            ClientsScreen(
                                uiState = uiState,
                                onSearchChange = viewModel::updateClientSearch,
                                onOpenClient = { id ->
                                    navController.navigate(AstroRoutes.clientDetail(id))
                                },
                                onCreateClient = { activeDialog = ActiveDialog.ClientForm() }
                            )
                        }

                        composable(AstroRoutes.ROUTE_FINANCE) {
                            FinanceScreen(
                                uiState = uiState,
                                onSelectFilter = viewModel::selectTransactionFilter,
                                onEditTransaction = { tx ->
                                    activeDialog = ActiveDialog.TransactionForm(transaction = tx)
                                },
                                onDeleteTransaction = viewModel::deleteTransaction,
                                onCreateTransaction = { activeDialog = ActiveDialog.TransactionForm() }
                            )
                        }

                        composable(
                            route = AstroRoutes.ROUTE_PROJECT_DETAIL,
                            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
                        ) { entry ->
                            val projectId = entry.arguments?.getLong("projectId") ?: 0L
                            ProjectDetailScreen(
                                projectId = projectId,
                                uiState = uiState,
                                onBack = { navController.popBackStack() },
                                onEditProject = { project ->
                                    activeDialog = ActiveDialog.ProjectForm(project = project)
                                },
                                onDeleteProject = viewModel::deleteProject,
                                onUpdateProgress = viewModel::updateProjectProgress,
                                onUpdateStatus = viewModel::updateProjectStatus,
                                onAddTask = { pid ->
                                    activeDialog = ActiveDialog.TaskForm(projectId = pid)
                                },
                                onToggleTask = viewModel::toggleTaskDone,
                                onDeleteTask = viewModel::deleteTask,
                                onLogRevenue = { project ->
                                    activeDialog = ActiveDialog.TransactionForm(prelinkedProject = project)
                                }
                            )
                        }

                        composable(
                            route = AstroRoutes.ROUTE_CLIENT_DETAIL,
                            arguments = listOf(navArgument("clientId") { type = NavType.LongType })
                        ) { entry ->
                            val clientId = entry.arguments?.getLong("clientId") ?: 0L
                            ClientDetailScreen(
                                clientId = clientId,
                                uiState = uiState,
                                onBack = { navController.popBackStack() },
                                onEditClient = { client ->
                                    activeDialog = ActiveDialog.ClientForm(client = client)
                                },
                                onDeleteClient = viewModel::deleteClient,
                                onNewProjectForClient = { cid ->
                                    activeDialog = ActiveDialog.ProjectForm(preselectedClientId = cid)
                                },
                                onOpenProject = { pid ->
                                    navController.navigate(AstroRoutes.projectDetail(pid))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
