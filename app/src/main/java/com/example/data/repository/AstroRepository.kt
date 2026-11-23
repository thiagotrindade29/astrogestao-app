package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.ClientEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectStatus
import com.example.data.local.TaskEntity
import com.example.data.local.TaskPriority
import com.example.data.local.TransactionCategory
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class AstroRepository(private val db: AppDatabase) {
    private val clientDao = db.clientDao()
    private val projectDao = db.projectDao()
    private val taskDao = db.taskDao()
    private val transactionDao = db.transactionDao()

    val clients: Flow<List<ClientEntity>> = clientDao.observeAll()
    val projects: Flow<List<ProjectEntity>> = projectDao.observeAll()
    val tasks: Flow<List<TaskEntity>> = taskDao.observeAll()
    val transactions: Flow<List<TransactionEntity>> = transactionDao.observeAll()

    suspend fun saveClient(client: ClientEntity): Long {
        return if (client.id == 0L) clientDao.insert(client)
        else {
            clientDao.update(client)
            client.id
        }
    }

    suspend fun deleteClient(id: Long) = clientDao.deleteById(id)

    suspend fun saveProject(project: ProjectEntity): Long {
        return if (project.id == 0L) projectDao.insert(project)
        else {
            projectDao.update(project)
            project.id
        }
    }

    suspend fun deleteProject(id: Long) = projectDao.deleteById(id)

    suspend fun saveTask(task: TaskEntity): Long {
        return if (task.id == 0L) taskDao.insert(task)
        else {
            taskDao.update(task)
            task.id
        }
    }

    suspend fun deleteTask(id: Long) = taskDao.deleteById(id)

    suspend fun saveTransaction(transaction: TransactionEntity): Long {
        return if (transaction.id == 0L) transactionDao.insert(transaction)
        else {
            transactionDao.update(transaction)
            transaction.id
        }
    }

    suspend fun deleteTransaction(id: Long) = transactionDao.deleteById(id)

    suspend fun clearAllData() {
        db.withTransaction {
            transactionDao.deleteAll()
            taskDao.deleteAll()
            projectDao.deleteAll()
            clientDao.deleteAll()
        }
    }

    suspend fun loadDemoWorkspace() {
        db.withTransaction {
            transactionDao.deleteAll()
            taskDao.deleteAll()
            projectDao.deleteAll()
            clientDao.deleteAll()

            val now = System.currentTimeMillis()
            val day = 86_400_000L

            val c1 = clientDao.insert(
                ClientEntity(
                    name = "Mariana Costa",
                    company = "Vela Logistics",
                    segment = "Supply Chain",
                    email = "mariana@velalogistics.io",
                    phone = "+55 11 98410-2240",
                    notes = "Quarterly operations retainer and warehouse telemetry rollout."
                )
            )
            val c2 = clientDao.insert(
                ClientEntity(
                    name = "Lucas Ferreira",
                    company = "Helios Energy",
                    segment = "CleanTech",
                    email = "l.ferreira@heliosgrid.com",
                    phone = "+55 21 99201-7712",
                    notes = "Solar micro-grid analytics portal and billing automation."
                )
            )
            val c3 = clientDao.insert(
                ClientEntity(
                    name = "Clara Mendes",
                    company = "Nordic Atelier",
                    segment = "Retail & Design",
                    email = "clara@nordicatelier.co",
                    phone = "+55 31 99740-1180",
                    notes = "Omnichannel inventory audit and seasonal launch."
                )
            )

            val p1 = projectDao.insert(
                ProjectEntity(
                    code = "AG-101",
                    title = "Fleet Telemetry Dashboard",
                    clientId = c1,
                    description = "Real-time dispatch KPI board and route cost optimization suite.",
                    status = ProjectStatus.IN_PROGRESS,
                    budget = 14500.0,
                    progressPercent = 68,
                    dueDateMillis = now + 5 * day
                )
            )
            val p2 = projectDao.insert(
                ProjectEntity(
                    code = "AG-102",
                    title = "Solar Grid Billing Portal",
                    clientId = c2,
                    description = "Automated monthly invoicing and consumption forecasting.",
                    status = ProjectStatus.REVIEW,
                    budget = 19800.0,
                    progressPercent = 90,
                    dueDateMillis = now + 2 * day
                )
            )
            val p3 = projectDao.insert(
                ProjectEntity(
                    code = "AG-103",
                    title = "Q4 Inventory Restructuring",
                    clientId = c3,
                    description = "SKU taxonomy cleanup and supplier lead-time SLA rules.",
                    status = ProjectStatus.PLANNED,
                    budget = 7200.0,
                    progressPercent = 20,
                    dueDateMillis = now + 18 * day
                )
            )
            val p4 = projectDao.insert(
                ProjectEntity(
                    code = "AG-098",
                    title = "Executive Finance Audit",
                    clientId = c1,
                    description = "Completed margin audit and automated tax reconciliation.",
                    status = ProjectStatus.COMPLETED,
                    budget = 8400.0,
                    progressPercent = 100,
                    dueDateMillis = now - 6 * day
                )
            )

            taskDao.insert(
                TaskEntity(
                    projectId = p1,
                    title = "Finalize GPS latency threshold alerts",
                    isDone = false,
                    priority = TaskPriority.HIGH,
                    dueDateMillis = now + 2 * day
                )
            )
            taskDao.insert(
                TaskEntity(
                    projectId = p1,
                    title = "Connect regional depot fuel ledger",
                    isDone = true,
                    priority = TaskPriority.MEDIUM,
                    dueDateMillis = now - 1 * day
                )
            )
            taskDao.insert(
                TaskEntity(
                    projectId = p2,
                    title = "Sign off UAT invoice PDF exports",
                    isDone = false,
                    priority = TaskPriority.HIGH,
                    dueDateMillis = now + 1 * day
                )
            )
            taskDao.insert(
                TaskEntity(
                    projectId = p3,
                    title = "Import 420 catalog SKUs from ERP",
                    isDone = false,
                    priority = TaskPriority.MEDIUM,
                    dueDateMillis = now + 9 * day
                )
            )

            fun monthsAgo(months: Int, dayOfMonth: Int = 12): Long {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -months)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
                return cal.timeInMillis
            }

            val sampleTx = listOf(
                TransactionEntity(
                    title = "Solar Portal Final Milestone",
                    type = TransactionType.INCOME,
                    category = TransactionCategory.PROJECT_FEE,
                    amount = 11400.0,
                    projectId = p2,
                    clientId = c2,
                    dateMillis = monthsAgo(0, 18),
                    notes = "80% release milestone"
                ),
                TransactionEntity(
                    title = "Vela Monthly Retainer",
                    type = TransactionType.INCOME,
                    category = TransactionCategory.RETAINER,
                    amount = 4800.0,
                    projectId = p1,
                    clientId = c1,
                    dateMillis = monthsAgo(0, 10)
                ),
                TransactionEntity(
                    title = "Cloud Infrastructure & DB",
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.SOFTWARE,
                    amount = 1420.0,
                    dateMillis = monthsAgo(0, 8)
                ),
                TransactionEntity(
                    title = "Specialist UX Contractor",
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.PAYROLL,
                    amount = 3200.0,
                    projectId = p1,
                    dateMillis = monthsAgo(0, 5)
                ),
                TransactionEntity(
                    title = "Executive Audit Settlement",
                    type = TransactionType.INCOME,
                    category = TransactionCategory.PROJECT_FEE,
                    amount = 8400.0,
                    projectId = p4,
                    clientId = c1,
                    dateMillis = monthsAgo(1, 22)
                ),
                TransactionEntity(
                    title = "Nordic Discovery Workshop",
                    type = TransactionType.INCOME,
                    category = TransactionCategory.CONSULTING,
                    amount = 3600.0,
                    projectId = p3,
                    clientId = c3,
                    dateMillis = monthsAgo(1, 9)
                ),
                TransactionEntity(
                    title = "Operations & Legal Overhead",
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.OPERATIONS,
                    amount = 2900.0,
                    dateMillis = monthsAgo(1, 14)
                ),
                TransactionEntity(
                    title = "Helios Phase 1 Kickoff",
                    type = TransactionType.INCOME,
                    category = TransactionCategory.PROJECT_FEE,
                    amount = 8400.0,
                    projectId = p2,
                    clientId = c2,
                    dateMillis = monthsAgo(2, 16)
                ),
                TransactionEntity(
                    title = "Brand & Outreach Campaign",
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.MARKETING,
                    amount = 1850.0,
                    dateMillis = monthsAgo(2, 11)
                ),
                TransactionEntity(
                    title = "Vela Q3 Retainer",
                    type = TransactionType.INCOME,
                    category = TransactionCategory.RETAINER,
                    amount = 7900.0,
                    clientId = c1,
                    dateMillis = monthsAgo(3, 15)
                ),
                TransactionEntity(
                    title = "Team & Tools Allocation",
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.PAYROLL,
                    amount = 4100.0,
                    dateMillis = monthsAgo(3, 19)
                )
            )
            sampleTx.forEach { transactionDao.insert(it) }
        }
    }
}
