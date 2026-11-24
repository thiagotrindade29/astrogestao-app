package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MovementType(val label: String) {
    ENTRADA("Entrada (Compra)"),
    SAIDA("Saída (Consumo)"),
    AJUSTE("Ajuste de Saldo")
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val colorHex: Long,
    val sectorNote: String = ""
)

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("uniqueCode")]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val uniqueCode: String = "",
    val code: String = "",
    val name: String,
    val categoryId: Long? = null,
    val unit: String = "un",
    val quantity: Double = 0.0,
    val minStock: Double = 1.0,
    val unitPrice: Double = 0.0,
    val lastPaidPrice: Double? = null,
    val expiryDateMillis: Long? = null,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = quantity <= minStock

    val estimatedTotalValue: Double
        get() = quantity * unitPrice
}

@Entity(
    tableName = "product_aliases",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productId"), Index("externalCode")]
)
data class ProductAliasEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val productId: Long,
    val externalCode: String,
    val marketName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productId")]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val productId: Long,
    val productNameSnapshot: String,
    val productUnitSnapshot: String,
    val type: MovementType,
    val quantityDelta: Double,
    val previousBalance: Double,
    val newBalance: Double,
    val unitPrice: Double,
    val totalValue: Double,
    val reason: String = "",
    val authorName: String = "Morador",
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "market_checkouts")
data class MarketCheckoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val storeName: String,
    val notes: String = "",
    val totalAmount: Double,
    val itemCount: Int,
    val savedToStock: Boolean,
    val authorName: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "household_users")
data class HouseholdUserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val email: String,
    val passwordHash: String = "",
    val role: String = "Administrador",
    val createdAt: Long = System.currentTimeMillis()
)
