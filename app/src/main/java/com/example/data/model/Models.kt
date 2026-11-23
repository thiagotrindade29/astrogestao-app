package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val seededDefaults: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Category(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String? = null,
    val color: String = "#3b82f6",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val name: String = "",
    val sku: String? = null,
    val barcode: String? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val categoryColor: String? = null,
    val location: String = "Despensa",
    val unit: String = "un",
    val quantity: Double = 0.0,
    val minQuantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val previousUnitPrice: Double = 0.0,
    val expirationDate: String? = null,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StockMovement(
    @DocumentId val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val productSku: String? = null,
    val productUnit: String = "un",
    val type: String = "IN",
    val quantity: Double = 0.0,
    val previousQuantity: Double = 0.0,
    val newQuantity: Double = 0.0,
    val reason: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Purchase(
    @DocumentId val id: String = "",
    val storeName: String = "Supermercado",
    val notes: String? = null,
    val totalAmount: Double = 0.0,
    val itemsCount: Long = 0L,
    val totalUnits: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PurchaseItem(
    @DocumentId val id: String = "",
    val purchaseId: String = "",
    val productId: String? = null,
    val productName: String = "",
    val barcode: String? = null,
    val categoryName: String? = null,
    val categoryColor: String? = null,
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val previousUnitPrice: Double = 0.0,
    val totalPrice: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class CartItem(
    val localId: String,
    val productId: String? = null,
    val name: String = "",
    val barcode: String = "",
    val categoryId: String? = null,
    val categoryName: String = "Geral",
    val categoryColor: String = "#3b82f6",
    val quantityInput: String = "1",
    val unitPriceInput: String = "",
    val previousUnitPrice: Double = 0.0,
    val unit: String = "un"
) {
    val parsedQuantity: Double
        get() = quantityInput.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    val parsedUnitPrice: Double
        get() = unitPriceInput.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    val subtotal: Double
        get() = parsedQuantity * parsedUnitPrice

    val diffPercent: Double
        get() = if (previousUnitPrice > 0.0 && parsedUnitPrice > 0.0) {
            ((parsedUnitPrice - previousUnitPrice) / previousUnitPrice) * 100.0
        } else {
            0.0
        }
}

data class CartTotals(
    val totalValue: Double = 0.0,
    val totalUnits: Double = 0.0,
    val previousTotalValue: Double = 0.0,
    val diff: Double = 0.0,
    val variationPct: Double = 0.0
)

data class FinishedPurchaseSummary(
    val storeName: String,
    val totalAmount: Double,
    val itemsCount: Int,
    val totalUnits: Double
)

data class ParsedReceiptItem(
    val localId: String,
    val productId: String? = null,
    val name: String,
    val barcode: String = "",
    val categoryId: String? = null,
    val categoryName: String = "Alimentos Secos",
    val categoryColor: String = "#3b82f6",
    val quantityInput: String = "1",
    val unit: String = "un",
    val unitPriceInput: String = "0.00",
    val previousUnitPrice: Double = 0.0,
    val existingStock: Double = 0.0,
    val alreadyInStock: Boolean = false
) {
    val parsedQuantity: Double
        get() = quantityInput.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    val parsedUnitPrice: Double
        get() = unitPriceInput.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    val totalPrice: Double
        get() = parsedQuantity * parsedUnitPrice
}

data class ParsedReceiptData(
    val storeName: String,
    val notes: String,
    val sourceLabel: String,
    val items: List<ParsedReceiptItem>
)

data class CategorySpending(
    val categoryName: String,
    val categoryColor: String,
    val totalSpent: Double,
    val sharePct: Double
)
