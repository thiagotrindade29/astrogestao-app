package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ProjectStatus(val label: String) {
    PLANNED("Planned"),
    IN_PROGRESS("In Progress"),
    REVIEW("Review"),
    COMPLETED("Completed")
}

enum class TaskPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High")
}

enum class TransactionType(val label: String) {
    INCOME("Income"),
    EXPENSE("Expense")
}

enum class TransactionCategory(val label: String, val isIncomeDefault: Boolean) {
    PROJECT_FEE("Project Fee", true),
    RETAINER("Retainer", true),
    CONSULTING("Consulting", true),
    SOFTWARE("Software & Cloud", false),
    OPERATIONS("Operations", false),
    PAYROLL("Team & Contractors", false),
    MARKETING("Marketing", false),
    OTHER("Other", false)
}

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val company: String,
    val segment: String,
    val email: String,
    val phone: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "projects",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("clientId")]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val code: String,
    val title: String,
    val clientId: Long? = null,
    val description: String = "",
    val status: ProjectStatus = ProjectStatus.PLANNED,
    val budget: Double = 0.0,
    val progressPercent: Int = 0,
    val dueDateMillis: Long = System.currentTimeMillis() + 14L * 86_400_000L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val projectId: Long,
    val title: String,
    val isDone: Boolean = false,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDateMillis: Long = System.currentTimeMillis() + 7L * 86_400_000L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("projectId"), Index("clientId")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val type: TransactionType,
    val category: TransactionCategory,
    val amount: Double,
    val projectId: Long? = null,
    val clientId: Long? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
