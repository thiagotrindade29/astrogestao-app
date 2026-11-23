package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ClientEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectStatus
import com.example.data.local.TaskEntity
import com.example.data.local.TaskPriority
import com.example.data.local.TransactionCategory
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.repository.AstroRepository
import com.example.data.repository.ThemePreferencesRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MonthlyCashFlowPoint(
    val monthLabel: String,
    val income: Double,
    val expense: Double
)

data class CategoryBreakdown(
    val category: TransactionCategory,
    val total: Double,
    val fraction: Float,
    val isIncome: Boolean
)

data class AstroUiState(
    val isLoading: Boolean = true,
    val businessName: String = "AstroGestão",
    val isDarkMode: Boolean = true,
    val clients: List<ClientEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val projectSearchQuery: String = "",
    val selectedProjectStatus: ProjectStatus? = null,
    val clientSearchQuery: String = "",
    val selectedTransactionFilter: TransactionType? = null
) {
    val isWorkspaceEmpty: Boolean
        get() = clients.isEmpty() && projects.isEmpty() && tasks.isEmpty() && transactions.isEmpty()

    val totalIncome: Double
        get() = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

    val totalExpense: Double
        get() = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    val netBalance: Double
        get() = totalIncome - totalExpense

    val profitMarginPercent: Int
        get() = if (totalIncome > 0.0) ((netBalance / totalIncome) * 100.0).toInt().coerceIn(-999, 100) else 0

    val activeProjectCount: Int
        get() = projects.count { it.status != ProjectStatus.COMPLETED }

    val openTaskCount: Int
        get() = tasks.count { !it.isDone }

    val clientMap: Map<Long, ClientEntity>
        get() = clients.associateBy { it.id }

    val projectMap: Map<Long, ProjectEntity>
        get() = projects.associateBy { it.id }

    val filteredProjects: List<ProjectEntity>
        get() = projects.filter { project ->
            val matchesStatus = selectedProjectStatus == null || project.status == selectedProjectStatus
            val query = projectSearchQuery.trim().lowercase()
            val clientName = project.clientId?.let { clientMap[it]?.company ?: clientMap[it]?.name }.orEmpty()
            val matchesQuery = query.isEmpty() ||
                project.title.lowercase().contains(query) ||
                project.code.lowercase().contains(query) ||
                clientName.lowercase().contains(query)
            matchesStatus && matchesQuery
        }

    val filteredClients: List<ClientEntity>
        get() = clients.filter { client ->
            val query = clientSearchQuery.trim().lowercase()
            query.isEmpty() ||
                client.name.lowercase().contains(query) ||
                client.company.lowercase().contains(query) ||
                client.segment.lowercase().contains(query)
        }

    val filteredTransactions: List<TransactionEntity>
        get() = transactions.filter { tx ->
            selectedTransactionFilter == null || tx.type == selectedTransactionFilter
        }

    val priorityOpenTasks: List<TaskEntity>
        get() = tasks.filter { !it.isDone }.take(5)

    val upcomingProjects: List<ProjectEntity>
        get() = projects.filter { it.status != ProjectStatus.COMPLETED }.take(4)

    val monthlyCashFlow: List<MonthlyCashFlowPoint>
        get() {
            val monthFmt = SimpleDateFormat("MMM", Locale.getDefault())
            val keyFmt = SimpleDateFormat("yyyy-MM", Locale.US)
            val buckets = linkedMapOf<String, Pair<String, MutableList<TransactionEntity>>>()
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -4)
            repeat(5) {
                val key = keyFmt.format(cal.time)
                val label = monthFmt.format(cal.time)
                buckets[key] = label to mutableListOf()
                cal.add(Calendar.MONTH, 1)
            }
            transactions.forEach { tx ->
                val key = keyFmt.format(tx.dateMillis)
                buckets[key]?.second?.add(tx)
            }
            return buckets.values.map { (label, list) ->
                MonthlyCashFlowPoint(
                    monthLabel = label,
                    income = list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                    expense = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                )
            }
        }

    val categoryBreakdowns: List<CategoryBreakdown>
        get() {
            val scopeList = filteredTransactions
            val maxSum = scopeList.groupBy { it.category }
                .values
                .maxOfOrNull { group -> group.sumOf { it.amount } }
                ?.coerceAtLeast(1.0) ?: 1.0
            return scopeList.groupBy { it.category to (it.type == TransactionType.INCOME) }
                .map { (pair, group) ->
                    val sum = group.sumOf { it.amount }
                    CategoryBreakdown(
                        category = pair.first,
                        total = sum,
                        fraction = (sum / maxSum).toFloat().coerceIn(0.05f, 1f),
                        isIncome = pair.second
                    )
                }
                .sortedByDescending { it.total }
                .take(5)
        }
}

private data class FilterState(
    val projectSearchQuery: String = "",
    val selectedProjectStatus: ProjectStatus? = null,
    val clientSearchQuery: String = "",
    val selectedTransactionFilter: TransactionType? = null
)

class AstroViewModel(
    private val repository: AstroRepository,
    private val prefs: ThemePreferencesRepository
) : ViewModel() {

    private val filterState = MutableStateFlow(FilterState())

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    val uiState: StateFlow<AstroUiState> = combine(
        repository.clients,
        repository.projects,
        repository.tasks,
        repository.transactions,
        prefs.isDarkMode,
        prefs.businessName,
        filterState
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val clients = values[0] as List<ClientEntity>
        @Suppress("UNCHECKED_CAST")
        val projects = values[1] as List<ProjectEntity>
        @Suppress("UNCHECKED_CAST")
        val tasks = values[2] as List<TaskEntity>
        @Suppress("UNCHECKED_CAST")
        val transactions = values[3] as List<TransactionEntity>
        val isDark = values[4] as Boolean
        val businessName = values[5] as String
        val filters = values[6] as FilterState

        AstroUiState(
            isLoading = false,
            businessName = businessName,
            isDarkMode = isDark,
            clients = clients,
            projects = projects,
            tasks = tasks,
            transactions = transactions,
            projectSearchQuery = filters.projectSearchQuery,
            selectedProjectStatus = filters.selectedProjectStatus,
            clientSearchQuery = filters.clientSearchQuery,
            selectedTransactionFilter = filters.selectedTransactionFilter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AstroUiState(isLoading = true)
    )

    fun updateProjectSearch(query: String) {
        filterState.value = filterState.value.copy(projectSearchQuery = query)
    }

    fun selectProjectStatus(status: ProjectStatus?) {
        filterState.value = filterState.value.copy(selectedProjectStatus = status)
    }

    fun updateClientSearch(query: String) {
        filterState.value = filterState.value.copy(clientSearchQuery = query)
    }

    fun selectTransactionFilter(type: TransactionType?) {
        filterState.value = filterState.value.copy(selectedTransactionFilter = type)
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { prefs.setDarkMode(enabled) }
    }

    fun updateBusinessName(name: String) {
        viewModelScope.launch {
            prefs.setBusinessName(name)
            _snackbarMessages.tryEmit("Workspace settings updated")
        }
    }

    fun saveClient(client: ClientEntity) {
        viewModelScope.launch {
            repository.saveClient(client)
            _snackbarMessages.tryEmit(if (client.id == 0L) "Client added" else "Client updated")
        }
    }

    fun deleteClient(client: ClientEntity) {
        viewModelScope.launch {
            repository.deleteClient(client.id)
            _snackbarMessages.tryEmit("Removed ${client.company.ifBlank { client.name }}")
        }
    }

    fun saveProject(project: ProjectEntity) {
        viewModelScope.launch {
            val normalized = if (project.status == ProjectStatus.COMPLETED) {
                project.copy(progressPercent = 100)
            } else {
                project
            }
            repository.saveProject(normalized)
            _snackbarMessages.tryEmit(if (project.id == 0L) "Project created" else "Project updated")
        }
    }

    fun updateProjectProgress(project: ProjectEntity, progress: Int) {
        viewModelScope.launch {
            val clamped = progress.coerceIn(0, 100)
            val newStatus = when {
                clamped == 100 -> ProjectStatus.COMPLETED
                clamped > 0 && project.status == ProjectStatus.PLANNED -> ProjectStatus.IN_PROGRESS
                clamped < 100 && project.status == ProjectStatus.COMPLETED -> ProjectStatus.REVIEW
                else -> project.status
            }
            repository.saveProject(project.copy(progressPercent = clamped, status = newStatus))
        }
    }

    fun updateProjectStatus(project: ProjectEntity, status: ProjectStatus) {
        viewModelScope.launch {
            val progress = if (status == ProjectStatus.COMPLETED) 100 else project.progressPercent
            repository.saveProject(project.copy(status = status, progressPercent = progress))
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(project.id)
            _snackbarMessages.tryEmit("Deleted project ${project.code}")
        }
    }

    fun saveTask(projectId: Long, title: String, priority: TaskPriority, dueInDays: Int) {
        viewModelScope.launch {
            val due = System.currentTimeMillis() + dueInDays.coerceAtLeast(0) * 86_400_000L
            repository.saveTask(
                TaskEntity(
                    projectId = projectId,
                    title = title.trim(),
                    priority = priority,
                    dueDateMillis = due
                )
            )
            _snackbarMessages.tryEmit("Task added")
        }
    }

    fun toggleTaskDone(task: TaskEntity) {
        viewModelScope.launch {
            repository.saveTask(task.copy(isDone = !task.isDone))
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task.id)
        }
    }

    fun saveTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.saveTransaction(transaction)
            _snackbarMessages.tryEmit(
                if (transaction.id == 0L) "Financial entry recorded" else "Entry updated"
            )
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction.id)
            _snackbarMessages.tryEmit("Removed ${transaction.title}")
        }
    }

    fun loadDemoWorkspace() {
        viewModelScope.launch {
            repository.loadDemoWorkspace()
            _snackbarMessages.tryEmit("Sample workspace loaded")
        }
    }

    fun clearWorkspace() {
        viewModelScope.launch {
            repository.clearAllData()
            _snackbarMessages.tryEmit("All workspace records cleared")
        }
    }

    class Factory(
        private val repository: AstroRepository,
        private val prefs: ThemePreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AstroViewModel(repository, prefs) as T
        }
    }
}
