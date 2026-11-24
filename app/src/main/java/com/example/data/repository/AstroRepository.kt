package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.CategoryEntity
import com.example.data.local.HouseholdUserEntity
import com.example.data.local.MarketCheckoutEntity
import com.example.data.local.MovementType
import com.example.data.local.ProductAliasEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StockMovementEntity
import com.example.data.nfce.ParsedNfceItem
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.math.abs

data class PosCheckoutEntry(
    val canonicalProductId: Long?,
    val scannedCode: String,
    val name: String,
    val unit: String,
    val quantity: Double,
    val unitPrice: Double,
    val categoryId: Long?
)

sealed interface AuthOutcome {
    data class Success(val user: HouseholdUserEntity) : AuthOutcome
    data object UserNotFound : AuthOutcome
    data object WrongPassword : AuthOutcome
    data object NoPasswordConfigured : AuthOutcome
}

data class CanonicalResolution(
    val product: ProductEntity,
    val matchedBy: String,
    val linkedNewAlias: Boolean
)

class AstroRepository(private val db: AppDatabase) {
    private val categoryDao = db.categoryDao()
    private val productDao = db.productDao()
    private val aliasDao = db.productAliasDao()
    private val movementDao = db.stockMovementDao()
    private val checkoutDao = db.marketCheckoutDao()
    private val userDao = db.householdUserDao()
    private val seeder = SampleDataSeeder(db)

    val categories: Flow<List<CategoryEntity>> = categoryDao.observeAll()
    val products: Flow<List<ProductEntity>> = productDao.observeAll()
    val aliases: Flow<List<ProductAliasEntity>> = aliasDao.observeAll()
    val movements: Flow<List<StockMovementEntity>> = movementDao.observeAll()
    val checkouts: Flow<List<MarketCheckoutEntity>> = checkoutDao.observeAll()
    val users: Flow<List<HouseholdUserEntity>> = userDao.observeAll()

    companion object {
        private const val PBKDF2_PREFIX = "pbkdf2$"
        private const val PBKDF2_ITERATIONS = 10_000
        private const val PBKDF2_KEY_BITS = 256
        private val secureRandom = SecureRandom()

        fun hashPassword(raw: String): String {
            val salt = ByteArray(16)
            secureRandom.nextBytes(salt)
            val hash = pbkdf2(raw, salt)
            return "$PBKDF2_PREFIX${salt.toHex()}$${hash.toHex()}"
        }

        fun verifyPassword(raw: String, storedHash: String): Boolean {
            if (storedHash.isBlank()) return false
            if (storedHash.startsWith(PBKDF2_PREFIX)) {
                val parts = storedHash.removePrefix(PBKDF2_PREFIX).split("$")
                if (parts.size != 2) return false
                val salt = parts[0].hexToBytesOrNull() ?: return false
                val expected = parts[1].hexToBytesOrNull() ?: return false
                val actual = pbkdf2(raw, salt)
                return MessageDigest.isEqual(actual, expected)
            }
            return MessageDigest.isEqual(
                sha256Hex(raw).toByteArray(Charsets.UTF_8),
                storedHash.toByteArray(Charsets.UTF_8)
            )
        }

        private fun pbkdf2(password: String, salt: ByteArray): ByteArray {
            val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
            return try {
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            } finally {
                spec.clearPassword()
            }
        }

        private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

        private fun String.hexToBytesOrNull(): ByteArray? {
            if (length % 2 != 0) return null
            return runCatching {
                ByteArray(length / 2) { idx ->
                    substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
                }
            }.getOrNull()
        }

        fun sha256Hex(raw: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    suspend fun ensureDefaultsSeeded() = seeder.ensureDefaultsSeeded()

    suspend fun loadSampleCatalog(authorName: String) = seeder.loadSampleCatalog(authorName)

    suspend fun exportBackupJson(): String = BackupManager.exportJson(db)

    suspend fun importBackupJson(rawJson: String): Result<BackupRestoreSummary> =
        BackupManager.importJson(db, rawJson)

    suspend fun registerUser(
        name: String,
        email: String,
        password: String,
        role: String
    ): Result<HouseholdUserEntity> {
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim()
        return db.withTransaction {
            val existing = userDao.getByEmail(cleanEmail)
            if (existing != null) {
                Result.failure(IllegalArgumentException("email_exists"))
            } else {
                val user = HouseholdUserEntity(
                    name = cleanName,
                    email = cleanEmail,
                    passwordHash = hashPassword(password),
                    role = role
                )
                val newId = userDao.insert(user)
                Result.success(user.copy(id = newId))
            }
        }
    }

    suspend fun authenticateUser(email: String, password: String): AuthOutcome {
        val cleanEmail = email.trim().lowercase()
        val user = userDao.getByEmail(cleanEmail) ?: return AuthOutcome.UserNotFound
        if (user.passwordHash.isBlank()) {
            return AuthOutcome.NoPasswordConfigured
        }
        return if (verifyPassword(password, user.passwordHash)) {
            AuthOutcome.Success(user)
        } else {
            AuthOutcome.WrongPassword
        }
    }

    suspend fun saveCategory(category: CategoryEntity): Long {
        return if (category.id == 0L) categoryDao.insert(category)
        else {
            categoryDao.update(category)
            category.id
        }
    }

    suspend fun deleteCategory(id: Long) = categoryDao.deleteById(id)

    suspend fun saveProduct(product: ProductEntity, authorName: String): Long {
        return db.withTransaction {
            val isNew = product.id == 0L
            val allProducts = productDao.getAllOnce()
            val previous = if (!isNew) productDao.getById(product.id) else null
            val assignedUniqueCode = product.uniqueCode.ifBlank {
                previous?.uniqueCode?.ifBlank { null }
                    ?: ProductMatcher.formatUniqueCode(ProductMatcher.nextUniqueSequence(allProducts))
            }
            val cleanProduct = product.copy(
                uniqueCode = assignedUniqueCode,
                code = product.code.trim(),
                name = product.name.trim(),
                updatedAt = System.currentTimeMillis()
            )
            val savedId = if (isNew) {
                productDao.insert(cleanProduct)
            } else {
                productDao.update(cleanProduct)
                cleanProduct.id
            }

            if (cleanProduct.code.isNotBlank() && aliasDao.findByCode(cleanProduct.code) == null) {
                aliasDao.insert(
                    ProductAliasEntity(
                        productId = savedId,
                        externalCode = cleanProduct.code,
                        marketName = "Catálogo"
                    )
                )
            }

            if (isNew && cleanProduct.quantity > 0.0) {
                movementDao.insert(
                    StockMovementEntity(
                        productId = savedId,
                        productNameSnapshot = cleanProduct.name,
                        productUnitSnapshot = cleanProduct.unit,
                        type = MovementType.ENTRADA,
                        quantityDelta = cleanProduct.quantity,
                        previousBalance = 0.0,
                        newBalance = cleanProduct.quantity,
                        unitPrice = cleanProduct.unitPrice,
                        totalValue = cleanProduct.quantity * cleanProduct.unitPrice,
                        reason = "Cadastro inicial de estoque ($assignedUniqueCode)",
                        authorName = authorName
                    )
                )
            } else if (previous != null && abs(previous.quantity - cleanProduct.quantity) > 0.0001) {
                val diff = cleanProduct.quantity - previous.quantity
                movementDao.insert(
                    StockMovementEntity(
                        productId = savedId,
                        productNameSnapshot = cleanProduct.name,
                        productUnitSnapshot = cleanProduct.unit,
                        type = MovementType.AJUSTE,
                        quantityDelta = diff,
                        previousBalance = previous.quantity,
                        newBalance = cleanProduct.quantity,
                        unitPrice = cleanProduct.unitPrice,
                        totalValue = abs(diff) * cleanProduct.unitPrice,
                        reason = "Edição manual no catálogo ($assignedUniqueCode)",
                        authorName = authorName
                    )
                )
            }
            savedId
        }
    }

    suspend fun deleteProduct(id: Long) = productDao.deleteById(id)

    suspend fun linkAliasToProduct(productId: Long, externalCode: String, marketName: String) {
        val cleanCode = externalCode.trim()
        if (cleanCode.isBlank()) return
        db.withTransaction {
            val prod = productDao.getById(productId) ?: return@withTransaction
            if (aliasDao.findByCode(cleanCode) == null) {
                aliasDao.insert(
                    ProductAliasEntity(
                        productId = productId,
                        externalCode = cleanCode,
                        marketName = marketName.ifBlank { "Mercado" }
                    )
                )
            }
            if (prod.code.isBlank()) {
                productDao.update(prod.copy(code = cleanCode, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    suspend fun resolveOrCreateCanonicalProduct(
        scannedCode: String,
        name: String,
        unit: String,
        unitPrice: Double,
        marketName: String,
        categoryId: Long?
    ): CanonicalResolution {
        return db.withTransaction {
            val allProducts = productDao.getAllOnce()
            val allAliases = aliasDao.getAllOnce()
            val cleanCode = scannedCode.trim()
            val cleanName = name.trim()

            ProductMatcher.findByCode(allProducts, allAliases, cleanCode)?.let { matched ->
                return@withTransaction CanonicalResolution(
                    product = matched,
                    matchedBy = "code",
                    linkedNewAlias = false
                )
            }

            ProductMatcher.findByNormalizedName(allProducts, cleanName)?.let { matched ->
                var linked = false
                if (cleanCode.isNotBlank() && aliasDao.findByCode(cleanCode) == null) {
                    aliasDao.insert(
                        ProductAliasEntity(
                            productId = matched.id,
                            externalCode = cleanCode,
                            marketName = marketName.ifBlank { "Mercado" }
                        )
                    )
                    linked = true
                }
                return@withTransaction CanonicalResolution(
                    product = matched,
                    matchedBy = "name",
                    linkedNewAlias = linked
                )
            }

            val newUniqueCode = ProductMatcher.formatUniqueCode(ProductMatcher.nextUniqueSequence(allProducts))
            val newProduct = ProductEntity(
                uniqueCode = newUniqueCode,
                code = cleanCode,
                name = cleanName,
                categoryId = categoryId,
                unit = unit.ifBlank { "un" },
                quantity = 0.0,
                minStock = 1.0,
                unitPrice = unitPrice,
                lastPaidPrice = null
            )
            val newId = productDao.insert(newProduct)
            if (cleanCode.isNotBlank()) {
                aliasDao.insert(
                    ProductAliasEntity(
                        productId = newId,
                        externalCode = cleanCode,
                        marketName = marketName.ifBlank { "Mercado" }
                    )
                )
            }
            CanonicalResolution(
                product = newProduct.copy(id = newId),
                matchedBy = "created",
                linkedNewAlias = cleanCode.isNotBlank()
            )
        }
    }

    suspend fun registerMovement(
        productId: Long,
        type: MovementType,
        amountOrTargetBalance: Double,
        unitPriceOverride: Double?,
        reason: String,
        authorName: String
    ) {
        db.withTransaction {
            val product = productDao.getById(productId) ?: return@withTransaction
            val previous = product.quantity
            val effectivePrice = unitPriceOverride?.takeIf { it >= 0.0 } ?: product.unitPrice

            val newBalance = when (type) {
                MovementType.ENTRADA -> (previous + amountOrTargetBalance).coerceAtLeast(0.0)
                MovementType.SAIDA -> (previous - amountOrTargetBalance).coerceAtLeast(0.0)
                MovementType.AJUSTE -> amountOrTargetBalance.coerceAtLeast(0.0)
            }

            val delta = when (type) {
                MovementType.ENTRADA -> newBalance - previous
                MovementType.SAIDA -> -(previous - newBalance)
                MovementType.AJUSTE -> newBalance - previous
            }

            val updatedLastPaid = if (type == MovementType.ENTRADA && product.unitPrice > 0.0 && effectivePrice != product.unitPrice) {
                product.unitPrice
            } else {
                product.lastPaidPrice
            }

            productDao.update(
                product.copy(
                    quantity = newBalance,
                    unitPrice = if (type == MovementType.ENTRADA && effectivePrice > 0.0) effectivePrice else product.unitPrice,
                    lastPaidPrice = updatedLastPaid,
                    updatedAt = System.currentTimeMillis()
                )
            )

            movementDao.insert(
                StockMovementEntity(
                    productId = product.id,
                    productNameSnapshot = product.name,
                    productUnitSnapshot = product.unit,
                    type = type,
                    quantityDelta = delta,
                    previousBalance = previous,
                    newBalance = newBalance,
                    unitPrice = effectivePrice,
                    totalValue = abs(delta) * effectivePrice,
                    reason = reason.ifBlank { type.label },
                    authorName = authorName,
                    timestampMillis = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun finalizeMarketPosCheckout(
        storeName: String,
        notes: String,
        entries: List<PosCheckoutEntry>,
        saveToStock: Boolean,
        authorName: String
    ) {
        val validEntries = entries.filter { it.quantity > 0.0 && it.name.isNotBlank() }
        if (validEntries.isEmpty()) return

        val cleanStore = storeName.trim().ifBlank { "Supermercado" }
        val totalAmount = validEntries.sumOf { it.quantity * it.unitPrice }

        db.withTransaction {
            val now = System.currentTimeMillis()
            checkoutDao.insert(
                MarketCheckoutEntity(
                    storeName = cleanStore,
                    notes = notes.trim(),
                    totalAmount = totalAmount,
                    itemCount = validEntries.size,
                    savedToStock = saveToStock,
                    authorName = authorName,
                    timestampMillis = now
                )
            )
            if (!saveToStock) return@withTransaction

            val existingProducts = productDao.getAllOnce().toMutableList()
            val existingAliases = aliasDao.getAllOnce().toMutableList()
            var nextSeq = ProductMatcher.nextUniqueSequence(existingProducts)

            validEntries.forEach { entry ->
                val cleanCode = entry.scannedCode.trim()
                val match = entry.canonicalProductId?.let { id ->
                    existingProducts.firstOrNull { it.id == id }
                } ?: ProductMatcher.findCanonicalMatch(existingProducts, existingAliases, cleanCode, entry.name)

                val (targetProduct, prevBalance, seqUsed) = upsertIncomingStockItem(
                    existingProducts = existingProducts,
                    existingAliases = existingAliases,
                    match = match,
                    nextSeq = nextSeq,
                    scannedCode = cleanCode,
                    name = entry.name,
                    unit = entry.unit,
                    quantity = entry.quantity,
                    unitPrice = entry.unitPrice,
                    categoryId = entry.categoryId,
                    marketName = cleanStore,
                    timestampMillis = now
                )
                nextSeq = seqUsed

                val effectivePrice = if (entry.unitPrice > 0.0) entry.unitPrice else targetProduct.unitPrice
                val reason = buildList {
                    add("Frente de Caixa — $cleanStore (${targetProduct.uniqueCode})")
                    if (notes.isNotBlank()) add(notes.trim())
                }.joinToString(" • ")

                movementDao.insert(
                    StockMovementEntity(
                        productId = targetProduct.id,
                        productNameSnapshot = targetProduct.name,
                        productUnitSnapshot = targetProduct.unit,
                        type = MovementType.ENTRADA,
                        quantityDelta = entry.quantity,
                        previousBalance = prevBalance,
                        newBalance = prevBalance + entry.quantity,
                        unitPrice = effectivePrice,
                        totalValue = entry.quantity * effectivePrice,
                        reason = reason,
                        authorName = authorName,
                        timestampMillis = now
                    )
                )
            }
        }
    }

    suspend fun importNfceItems(
        items: List<ParsedNfceItem>,
        storeName: String?,
        defaultCategoryId: Long?,
        authorName: String
    ): Int {
        val selected = items.filter { it.selected && it.name.isNotBlank() && it.quantity > 0.0 }
        if (selected.isEmpty()) return 0

        return db.withTransaction {
            val now = System.currentTimeMillis()
            val existing = productDao.getAllOnce().toMutableList()
            val existingAliases = aliasDao.getAllOnce().toMutableList()
            var nextSeq = ProductMatcher.nextUniqueSequence(existing)
            val cleanStore = storeName?.trim().orEmpty()
            val reasonLabel = if (cleanStore.isNotBlank()) "Importação NFC-e ($cleanStore)" else "Importação Cupom / NFC-e"
            var importedCount = 0

            selected.forEach { parsed ->
                val cleanCode = parsed.code.trim()
                val match = ProductMatcher.findCanonicalMatch(existing, existingAliases, cleanCode, parsed.name)

                val (targetProduct, prevBalance, seqUsed) = upsertIncomingStockItem(
                    existingProducts = existing,
                    existingAliases = existingAliases,
                    match = match,
                    nextSeq = nextSeq,
                    scannedCode = cleanCode,
                    name = parsed.name,
                    unit = parsed.unit,
                    quantity = parsed.quantity,
                    unitPrice = parsed.unitPrice,
                    categoryId = parsed.categoryId ?: defaultCategoryId,
                    marketName = cleanStore.ifBlank { "NFC-e" },
                    timestampMillis = now
                )
                nextSeq = seqUsed

                val effectivePrice = if (parsed.unitPrice > 0.0) parsed.unitPrice else targetProduct.unitPrice
                movementDao.insert(
                    StockMovementEntity(
                        productId = targetProduct.id,
                        productNameSnapshot = targetProduct.name,
                        productUnitSnapshot = targetProduct.unit,
                        type = MovementType.ENTRADA,
                        quantityDelta = parsed.quantity,
                        previousBalance = prevBalance,
                        newBalance = prevBalance + parsed.quantity,
                        unitPrice = effectivePrice,
                        totalValue = parsed.quantity * effectivePrice,
                        reason = "$reasonLabel (${targetProduct.uniqueCode})",
                        authorName = authorName,
                        timestampMillis = now
                    )
                )
                importedCount++
            }
            importedCount
        }
    }

    private suspend fun upsertIncomingStockItem(
        existingProducts: MutableList<ProductEntity>,
        existingAliases: MutableList<ProductAliasEntity>,
        match: ProductEntity?,
        nextSeq: Int,
        scannedCode: String,
        name: String,
        unit: String,
        quantity: Double,
        unitPrice: Double,
        categoryId: Long?,
        marketName: String,
        timestampMillis: Long
    ): Triple<ProductEntity, Double, Int> {
        var seq = nextSeq
        val targetProduct: ProductEntity
        val previousBalance: Double

        if (match != null) {
            previousBalance = match.quantity
            val prevPrice = if (match.unitPrice > 0.0) match.unitPrice else match.lastPaidPrice
            val newPrice = if (unitPrice > 0.0) unitPrice else match.unitPrice
            val updated = match.copy(
                quantity = previousBalance + quantity,
                unitPrice = newPrice,
                lastPaidPrice = if (prevPrice != null && prevPrice != newPrice) prevPrice else match.lastPaidPrice,
                updatedAt = timestampMillis
            )
            productDao.update(updated)
            existingProducts[existingProducts.indexOf(match)] = updated
            targetProduct = updated
        } else {
            val uniqueCode = ProductMatcher.formatUniqueCode(seq++)
            previousBalance = 0.0
            val created = ProductEntity(
                uniqueCode = uniqueCode,
                code = scannedCode,
                name = name.trim(),
                categoryId = categoryId,
                unit = unit.ifBlank { "un" },
                quantity = quantity,
                minStock = 1.0,
                unitPrice = unitPrice,
                lastPaidPrice = null,
                updatedAt = timestampMillis
            )
            val newId = productDao.insert(created)
            val saved = created.copy(id = newId)
            existingProducts.add(saved)
            targetProduct = saved
        }

        if (scannedCode.isNotBlank() && existingAliases.none { it.externalCode.equals(scannedCode, ignoreCase = true) }) {
            val alias = ProductAliasEntity(
                productId = targetProduct.id,
                externalCode = scannedCode,
                marketName = marketName,
                createdAt = timestampMillis
            )
            val aliasId = aliasDao.insert(alias)
            existingAliases.add(alias.copy(id = aliasId))
        }

        return Triple(targetProduct, previousBalance, seq)
    }

    suspend fun saveUser(user: HouseholdUserEntity) {
        userDao.insert(user)
    }

    suspend fun deleteUser(id: Long) {
        userDao.deleteById(id)
    }
}
