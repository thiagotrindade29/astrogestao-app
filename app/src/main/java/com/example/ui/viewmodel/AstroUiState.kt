package com.example.ui.viewmodel

import com.example.data.local.CategoryEntity
import com.example.data.local.HouseholdUserEntity
import com.example.data.local.MarketCheckoutEntity
import com.example.data.local.MovementType
import com.example.data.local.ProductAliasEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StockMovementEntity
import com.example.data.nfce.ParsedNfceItem
import com.example.data.nfce.RegionalPortal
import com.example.data.nfce.SefazPortals
import com.example.data.repository.ProductMatcher
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MonthlySpendingPoint(
    val monthLabel: String,
    val purchases: Double,
    val consumption: Double
)

data class CategoryStockValue(
    val categoryName: String,
    val colorHex: Long,
    val value: Double,
    val itemCount: Int,
    val fraction: Float
)

data class PosCartItem(
    val key: String,
    val canonicalProductId: Long?,
    val uniqueCode: String,
    val scannedCode: String,
    val name: String,
    val unit: String,
    val quantity: Double,
    val unitPrice: Double,
    val previousPrice: Double?,
    val categoryId: Long?
) {
    val subtotal: Double
        get() = quantity * unitPrice
}

data class PosMarketState(
    val storeName: String = "Supermercado",
    val notes: String = "",
    val searchQuery: String = "",
    val saveToStock: Boolean = true,
    val cartItems: List<PosCartItem> = emptyList(),
    val unmatchedScannedCode: String? = null
) {
    val totalAmount: Double
        get() = cartItems.sumOf { it.subtotal }
}

data class NfceImportState(
    val activeTab: Int = 0,
    val selectedPortal: RegionalPortal = SefazPortals.allPortals.first(),
    val inputQuery: String = "",
    val rawReceiptTextInput: String = "",
    val detectedKey: String? = null,
    val activePortalUrl: String = SefazPortals.allPortals.first().url,
    val showWebPortal: Boolean = false,
    val isFetching: Boolean = false,
    val storeName: String? = null,
    val parsedItems: List<ParsedNfceItem> = emptyList(),
    val statusMessage: String? = null
)

data class AstroUiState(
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean = false,
    val currentUserId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val householdId: String = "",
    val householdName: String = "",
    val householdInviteCode: String = "",
    val householdOwnerUid: String = "",
    val memberCount: Int = 1,
    val hasLocalMigrationAvailable: Boolean = false,
    val cloudErrorMessage: String? = null,
    val isSyncingWorkspace: Boolean = false,
    val isDarkMode: Boolean = false,
    val categories: List<CategoryEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val aliases: List<ProductAliasEntity> = emptyList(),
    val movements: List<StockMovementEntity> = emptyList(),
    val checkouts: List<MarketCheckoutEntity> = emptyList(),
    val users: List<HouseholdUserEntity> = emptyList(),
    val productSearchQuery: String = "",
    val selectedCategoryId: Long? = null,
    val onlyLowStock: Boolean = false,
    val movementFilterType: MovementType? = null
) {
    val isHouseholdOwner: Boolean
        get() = currentUserId.isNotBlank() && currentUserId == householdOwnerUid
    val categoryMap: Map<Long, CategoryEntity>
        get() = categories.associateBy { it.id }

    val aliasesByProductId: Map<Long, List<ProductAliasEntity>>
        get() = aliases.groupBy { it.productId }

    val registeredProductCount: Int
        get() = products.size

    val totalUnitsInStock: Double
        get() = products.sumOf { it.quantity }

    val totalEstimatedValue: Double
        get() = products.sumOf { it.estimatedTotalValue }

    val lowStockProducts: List<ProductEntity>
        get() = products.filter { it.isLowStock }

    val restockAlertsCount: Int
        get() = lowStockProducts.size

    val expiringIn30Days: List<ProductEntity>
        get() {
            val limit = System.currentTimeMillis() + 30L * 86_400_000L
            return products
                .filter { it.expiryDateMillis != null && it.expiryDateMillis <= limit }
                .sortedBy { it.expiryDateMillis }
        }

    val filteredProducts: List<ProductEntity>
        get() = products.filter { product ->
            val productAliases = aliasesByProductId[product.id].orEmpty()
            val matchesQuery = ProductMatcher.matchesSearch(product, productAliases, productSearchQuery)
            val matchesCategory = selectedCategoryId == null || product.categoryId == selectedCategoryId
            val matchesLowStock = !onlyLowStock || product.isLowStock
            matchesQuery && matchesCategory && matchesLowStock
        }

    val filteredMovements: List<StockMovementEntity>
        get() = movements.filter { movement ->
            movementFilterType == null || movement.type == movementFilterType
        }

    val totalPurchasesSpent: Double
        get() = movements.filter { it.type == MovementType.ENTRADA }.sumOf { it.totalValue } +
            checkouts.filter { !it.savedToStock }.sumOf { it.totalAmount }

    val totalConsumedValue: Double
        get() = movements.filter { it.type == MovementType.SAIDA }.sumOf { it.totalValue }

    val monthlySpending: List<MonthlySpendingPoint>
        get() {
            val ptBr = Locale("pt", "BR")
            val monthFmt = SimpleDateFormat("MMM", ptBr)
            val keyFmt = SimpleDateFormat("yyyy-MM", Locale.US)
            val buckets = linkedMapOf<String, Triple<String, MutableList<StockMovementEntity>, MutableList<MarketCheckoutEntity>>>()
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -4)
            repeat(5) {
                val key = keyFmt.format(cal.time)
                val label = monthFmt.format(cal.time)
                buckets[key] = Triple(label, mutableListOf(), mutableListOf())
                cal.add(Calendar.MONTH, 1)
            }
            movements.forEach { mv ->
                val key = keyFmt.format(mv.timestampMillis)
                buckets[key]?.second?.add(mv)
            }
            checkouts.filter { !it.savedToStock }.forEach { ck ->
                val key = keyFmt.format(ck.timestampMillis)
                buckets[key]?.third?.add(ck)
            }
            return buckets.values.map { (label, mvList, ckList) ->
                MonthlySpendingPoint(
                    monthLabel = label,
                    purchases = mvList.filter { it.type == MovementType.ENTRADA }.sumOf { it.totalValue } +
                        ckList.sumOf { it.totalAmount },
                    consumption = mvList.filter { it.type == MovementType.SAIDA }.sumOf { it.totalValue }
                )
            }
        }

    val categoryStockValues: List<CategoryStockValue>
        get() {
            val grouped = products.groupBy { it.categoryId }
            val maxVal = grouped.values
                .maxOfOrNull { list -> list.sumOf { it.estimatedTotalValue } }
                ?.coerceAtLeast(1.0) ?: 1.0
            return grouped.map { (catId, list) ->
                val cat = catId?.let { categoryMap[it] }
                val sum = list.sumOf { it.estimatedTotalValue }
                CategoryStockValue(
                    categoryName = cat?.name ?: "Sem categoria",
                    colorHex = cat?.colorHex ?: 0xFF64748B,
                    value = sum,
                    itemCount = list.size,
                    fraction = (sum / maxVal).toFloat().coerceIn(0.06f, 1f)
                )
            }.sortedByDescending { it.value }
        }
}
