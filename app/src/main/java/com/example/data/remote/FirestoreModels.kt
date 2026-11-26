package com.example.data.remote

import androidx.annotation.Keep
import com.example.data.local.CategoryEntity
import com.example.data.local.HouseholdUserEntity
import com.example.data.local.MarketCheckoutEntity
import com.example.data.local.MovementType
import com.example.data.local.ProductAliasEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StockMovementEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfileDoc(
    @DocumentId val docId: String = "",
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val activeHouseholdId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HouseholdDoc(
    @DocumentId val docId: String = "",
    val ownerUid: String = "",
    val name: String = "",
    val inviteCode: String = "",
    val memberUids: List<String> = emptyList(),
    val nextProductSeq: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HouseholdMemberDoc(
    @DocumentId val docId: String = "",
    val householdId: String = "",
    val memberUids: List<String> = emptyList(),
    val uid: String = "",
    val localId: Long = 0L,
    val name: String = "",
    val email: String = "",
    val role: String = "Morador",
    val createdAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): HouseholdUserEntity = HouseholdUserEntity(
        id = if (localId > 0L) localId else docId.toLongOrNull() ?: (uid.hashCode().toLong() and 0x7FFFFFFFL).coerceAtLeast(1L),
        name = name,
        email = email,
        passwordHash = "",
        role = role,
        createdAt = createdAtMillis
    )
}

@Keep
@IgnoreExtraProperties
data class CategoryDoc(
    @DocumentId val docId: String = "",
    val householdId: String = "",
    val memberUids: List<String> = emptyList(),
    val localId: Long = 0L,
    val name: String = "",
    val colorHex: Long = 0xFF5B3DF5L,
    val sectorNote: String = "",
    val singleConsumption: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): CategoryEntity = CategoryEntity(
        id = if (localId > 0L) localId else docId.toLongOrNull() ?: 1L,
        name = name,
        colorHex = colorHex,
        sectorNote = sectorNote,
        singleConsumption = singleConsumption
    )
}

@Keep
@IgnoreExtraProperties
data class ProductDoc(
    @DocumentId val docId: String = "",
    val householdId: String = "",
    val memberUids: List<String> = emptyList(),
    val localId: Long = 0L,
    val uniqueCode: String = "",
    val code: String = "",
    val name: String = "",
    val categoryId: Long? = null,
    val unit: String = "un",
    val quantity: Double = 0.0,
    val minStock: Double = 1.0,
    val unitPrice: Double = 0.0,
    val lastPaidPrice: Double? = null,
    val expiryDateMillis: Long? = null,
    val notes: String = "",
    val updatedAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): ProductEntity = ProductEntity(
        id = if (localId > 0L) localId else docId.toLongOrNull() ?: 1L,
        uniqueCode = uniqueCode,
        code = code,
        name = name,
        categoryId = categoryId,
        unit = unit.ifBlank { "un" },
        quantity = quantity,
        minStock = minStock,
        unitPrice = unitPrice,
        lastPaidPrice = lastPaidPrice,
        expiryDateMillis = expiryDateMillis,
        notes = notes,
        updatedAt = updatedAtMillis
    )
}

@Keep
@IgnoreExtraProperties
data class ProductAliasDoc(
    @DocumentId val docId: String = "",
    val householdId: String = "",
    val memberUids: List<String> = emptyList(),
    val localId: Long = 0L,
    val productId: Long = 0L,
    val externalCode: String = "",
    val marketName: String = "",
    val createdAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): ProductAliasEntity = ProductAliasEntity(
        id = if (localId > 0L) localId else docId.toLongOrNull() ?: 1L,
        productId = productId,
        externalCode = externalCode,
        marketName = marketName,
        createdAt = createdAtMillis
    )
}

@Keep
@IgnoreExtraProperties
data class StockMovementDoc(
    @DocumentId val docId: String = "",
    val householdId: String = "",
    val memberUids: List<String> = emptyList(),
    val localId: Long = 0L,
    val productId: Long = 0L,
    val productNameSnapshot: String = "",
    val productUnitSnapshot: String = "un",
    val type: String = MovementType.ENTRADA.name,
    val quantityDelta: Double = 0.0,
    val previousBalance: Double = 0.0,
    val newBalance: Double = 0.0,
    val unitPrice: Double = 0.0,
    val totalValue: Double = 0.0,
    val reason: String = "",
    val authorName: String = "Morador",
    val timestampMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): StockMovementEntity {
        val safeType = MovementType.entries.firstOrNull { it.name == type } ?: MovementType.ENTRADA
        return StockMovementEntity(
            id = if (localId > 0L) localId else docId.toLongOrNull() ?: 1L,
            productId = productId,
            productNameSnapshot = productNameSnapshot,
            productUnitSnapshot = productUnitSnapshot,
            type = safeType,
            quantityDelta = quantityDelta,
            previousBalance = previousBalance,
            newBalance = newBalance,
            unitPrice = unitPrice,
            totalValue = totalValue,
            reason = reason,
            authorName = authorName,
            timestampMillis = timestampMillis
        )
    }
}

@Keep
@IgnoreExtraProperties
data class MarketCheckoutDoc(
    @DocumentId val docId: String = "",
    val householdId: String = "",
    val memberUids: List<String> = emptyList(),
    val localId: Long = 0L,
    val storeName: String = "Supermercado",
    val notes: String = "",
    val totalAmount: Double = 0.0,
    val itemCount: Int = 0,
    val savedToStock: Boolean = true,
    val authorName: String = "Morador",
    val timestampMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): MarketCheckoutEntity = MarketCheckoutEntity(
        id = if (localId > 0L) localId else docId.toLongOrNull() ?: 1L,
        storeName = storeName,
        notes = notes,
        totalAmount = totalAmount,
        itemCount = itemCount,
        savedToStock = savedToStock,
        authorName = authorName,
        timestampMillis = timestampMillis
    )
}

@Keep
@IgnoreExtraProperties
data class AppVersionConfigDoc(
    @DocumentId val docId: String = "",
    val latestVersionName: String = "",
    val minSupportedVersionName: String = "",
    val releaseNotes: String = "",
    val downloadUrl: String = "",
    val updatedAt: Timestamp? = null
)

