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
import java.text.Normalizer
import java.util.Calendar
import kotlin.math.abs

data class MarketCartEntry(
    val product: ProductEntity,
    val quantityToBuy: Double,
    val unitPrice: Double
)

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
}

data class CanonicalResolution(
    val product: ProductEntity,
    val matchedBy: String, // "code", "name", or "created"
    val linkedNewAlias: Boolean
)

class AstroRepository(private val db: AppDatabase) {
    private val categoryDao = db.categoryDao()
    private val productDao = db.productDao()
    private val aliasDao = db.productAliasDao()
    private val movementDao = db.stockMovementDao()
    private val checkoutDao = db.marketCheckoutDao()
    private val userDao = db.householdUserDao()

    val categories: Flow<List<CategoryEntity>> = categoryDao.observeAll()
    val products: Flow<List<ProductEntity>> = productDao.observeAll()
    val aliases: Flow<List<ProductAliasEntity>> = aliasDao.observeAll()
    val movements: Flow<List<StockMovementEntity>> = movementDao.observeAll()
    val checkouts: Flow<List<MarketCheckoutEntity>> = checkoutDao.observeAll()
    val users: Flow<List<HouseholdUserEntity>> = userDao.observeAll()

    companion object {
        private val combiningMarksRegex = "\\p{InCombiningDiacriticalMarks}+".toRegex()
        private val multiSpaceRegex = "\\s+".toRegex()

        fun normalizeProductName(name: String): String {
            val decomposed = Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
            return combiningMarksRegex.replace(decomposed, "")
                .lowercase()
                .replace(multiSpaceRegex, " ")
                .trim()
        }

        fun formatUniqueCode(sequence: Int): String {
            return "AST-${sequence.toString().padStart(4, '0')}"
        }

        fun nextUniqueSequence(products: List<ProductEntity>): Int {
            val maxSeq = products.mapNotNull { prod ->
                prod.uniqueCode.removePrefix("AST-").toIntOrNull()
            }.maxOrNull() ?: 0
            return maxSeq + 1
        }

        fun sha256Hex(raw: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    suspend fun ensureDefaultsSeeded() {
        db.withTransaction {
            if (categoryDao.count() == 0) {
                val defaults = listOf(
                    CategoryEntity(name = "Despensa", colorHex = 0xFF5B3DF5, sectorNote = "Armários secos e grãos"),
                    CategoryEntity(name = "Geladeira", colorHex = 0xFF0284C7, sectorNote = "Frios, laticínios e bebidas"),
                    CategoryEntity(name = "Limpeza", colorHex = 0xFF059669, sectorNote = "Lavanderia e área de serviço"),
                    CategoryEntity(name = "Higiene", colorHex = 0xFFD97706, sectorNote = "Banheiros e cuidados pessoais"),
                    CategoryEntity(name = "Congelados", colorHex = 0xFF7C3AED, sectorNote = "Freezer e salgados"),
                    CategoryEntity(name = "Hortifruti", colorHex = 0xFF16A34A, sectorNote = "Frutas, verduras e legumes")
                )
                defaults.forEach { categoryDao.insert(it) }
            }
            if (userDao.count() == 0) {
                userDao.insert(
                    HouseholdUserEntity(
                        name = "Thiago Trindade",
                        email = "thiagotrindade628@gmail.com",
                        passwordHash = "",
                        role = "Administrador"
                    )
                )
            }
            // Backfill uniqueCode for any legacy rows missing AST-XXXX
            val currentProducts = productDao.getAllOnce()
            var nextSeq = nextUniqueSequence(currentProducts)
            currentProducts.filter { it.uniqueCode.isBlank() }.forEach { prod ->
                productDao.update(prod.copy(uniqueCode = formatUniqueCode(nextSeq++)))
            }
        }
    }

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
                    passwordHash = sha256Hex(password),
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
            // Default seeded owner profile supports quick demo sign-in or sets password on login
            return if (password.length >= 4) AuthOutcome.Success(user) else AuthOutcome.WrongPassword
        }
        return if (user.passwordHash == sha256Hex(password)) {
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
                previous?.uniqueCode?.ifBlank { null } ?: formatUniqueCode(nextUniqueSequence(allProducts))
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

            if (cleanProduct.code.isNotBlank()) {
                val existingAlias = aliasDao.findByCode(cleanProduct.code)
                if (existingAlias == null) {
                    aliasDao.insert(
                        ProductAliasEntity(
                            productId = savedId,
                            externalCode = cleanProduct.code,
                            marketName = "Catálogo"
                        )
                    )
                }
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
            val existing = aliasDao.findByCode(cleanCode)
            if (existing == null) {
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
            val normalizedTargetName = normalizeProductName(cleanName)

            // 1. Lookup by uniqueCode, primary code, or alias externalCode
            val codeMatchedProduct = if (cleanCode.isNotBlank()) {
                allProducts.firstOrNull {
                    it.uniqueCode.equals(cleanCode, ignoreCase = true) ||
                        it.code.equals(cleanCode, ignoreCase = true)
                } ?: allAliases.firstOrNull {
                    it.externalCode.equals(cleanCode, ignoreCase = true)
                }?.let { alias ->
                    allProducts.firstOrNull { it.id == alias.productId }
                }
            } else null

            if (codeMatchedProduct != null) {
                return@withTransaction CanonicalResolution(
                    product = codeMatchedProduct,
                    matchedBy = "code",
                    linkedNewAlias = false
                )
            }

            // 2. Lookup by normalized product name to avoid duplicates across markets
            val nameMatchedProduct = allProducts.firstOrNull {
                normalizeProductName(it.name) == normalizedTargetName
            }

            if (nameMatchedProduct != null) {
                var linked = false
                if (cleanCode.isNotBlank() && aliasDao.findByCode(cleanCode) == null) {
                    aliasDao.insert(
                        ProductAliasEntity(
                            productId = nameMatchedProduct.id,
                            externalCode = cleanCode,
                            marketName = marketName.ifBlank { "Mercado" }
                        )
                    )
                    linked = true
                }
                return@withTransaction CanonicalResolution(
                    product = nameMatchedProduct,
                    matchedBy = "name",
                    linkedNewAlias = linked
                )
            }

            // 3. Create new canonical product with AST-XXXX unique code
            val newUniqueCode = formatUniqueCode(nextUniqueSequence(allProducts))
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

            if (!saveToStock) {
                // Only saves financial checkout record; stock balances stay untouched.
                return@withTransaction
            }

            val existingProducts = productDao.getAllOnce().toMutableList()
            val existingAliases = aliasDao.getAllOnce().toMutableList()
            var nextSeq = nextUniqueSequence(existingProducts)

            validEntries.forEach { entry ->
                val cleanCode = entry.scannedCode.trim()
                val normalizedName = normalizeProductName(entry.name)

                var match = entry.canonicalProductId?.let { id ->
                    existingProducts.firstOrNull { it.id == id }
                }

                if (match == null && cleanCode.isNotBlank()) {
                    match = existingProducts.firstOrNull {
                        it.uniqueCode.equals(cleanCode, ignoreCase = true) ||
                            it.code.equals(cleanCode, ignoreCase = true)
                    } ?: existingAliases.firstOrNull {
                        it.externalCode.equals(cleanCode, ignoreCase = true)
                    }?.let { alias ->
                        existingProducts.firstOrNull { it.id == alias.productId }
                    }
                }

                if (match == null) {
                    match = existingProducts.firstOrNull {
                        normalizeProductName(it.name) == normalizedName
                    }
                }

                val targetProduct: ProductEntity
                val previousBalance: Double
                val previousPrice: Double?

                if (match != null) {
                    previousBalance = match.quantity
                    previousPrice = if (match.unitPrice > 0.0) match.unitPrice else match.lastPaidPrice
                    val newPrice = if (entry.unitPrice > 0.0) entry.unitPrice else match.unitPrice
                    val updated = match.copy(
                        quantity = previousBalance + entry.quantity,
                        unitPrice = newPrice,
                        lastPaidPrice = if (previousPrice != null && previousPrice != newPrice) previousPrice else match.lastPaidPrice,
                        updatedAt = now
                    )
                    productDao.update(updated)
                    existingProducts[existingProducts.indexOf(match)] = updated
                    targetProduct = updated
                } else {
                    val uniqueCode = formatUniqueCode(nextSeq++)
                    previousBalance = 0.0
                    val created = ProductEntity(
                        uniqueCode = uniqueCode,
                        code = cleanCode,
                        name = entry.name.trim(),
                        categoryId = entry.categoryId,
                        unit = entry.unit.ifBlank { "un" },
                        quantity = entry.quantity,
                        minStock = 1.0,
                        unitPrice = entry.unitPrice,
                        lastPaidPrice = null,
                        updatedAt = now
                    )
                    val newId = productDao.insert(created)
                    val saved = created.copy(id = newId)
                    existingProducts.add(saved)
                    targetProduct = saved
                }

                if (cleanCode.isNotBlank() && existingAliases.none { it.externalCode.equals(cleanCode, ignoreCase = true) }) {
                    val alias = ProductAliasEntity(
                        productId = targetProduct.id,
                        externalCode = cleanCode,
                        marketName = cleanStore,
                        createdAt = now
                    )
                    val aliasId = aliasDao.insert(alias)
                    existingAliases.add(alias.copy(id = aliasId))
                }

                val effectivePrice = if (entry.unitPrice > 0.0) entry.unitPrice else targetProduct.unitPrice
                val reasonParts = buildList {
                    add("Frente de Caixa — $cleanStore (${targetProduct.uniqueCode})")
                    if (notes.isNotBlank()) add(notes.trim())
                }

                movementDao.insert(
                    StockMovementEntity(
                        productId = targetProduct.id,
                        productNameSnapshot = targetProduct.name,
                        productUnitSnapshot = targetProduct.unit,
                        type = MovementType.ENTRADA,
                        quantityDelta = entry.quantity,
                        previousBalance = previousBalance,
                        newBalance = previousBalance + entry.quantity,
                        unitPrice = effectivePrice,
                        totalValue = entry.quantity * effectivePrice,
                        reason = reasonParts.joinToString(" • "),
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
            val existing = productDao.getAllOnce().toMutableList()
            val existingAliases = aliasDao.getAllOnce().toMutableList()
            var nextSeq = nextUniqueSequence(existing)
            val cleanStore = storeName?.trim().orEmpty()
            val reasonLabel = if (cleanStore.isNotBlank()) {
                "Importação NFC-e ($cleanStore)"
            } else {
                "Importação Cupom / NFC-e"
            }
            var importedCount = 0

            selected.forEach { parsed ->
                val cleanCode = parsed.code.trim()
                val normalizedName = normalizeProductName(parsed.name)

                val match = (if (cleanCode.isNotBlank()) {
                    existing.firstOrNull {
                        it.uniqueCode.equals(cleanCode, ignoreCase = true) ||
                            it.code.equals(cleanCode, ignoreCase = true)
                    } ?: existingAliases.firstOrNull {
                        it.externalCode.equals(cleanCode, ignoreCase = true)
                    }?.let { alias -> existing.firstOrNull { it.id == alias.productId } }
                } else null) ?: existing.firstOrNull {
                    normalizeProductName(it.name) == normalizedName
                }

                val targetCategory = parsed.categoryId ?: defaultCategoryId
                if (match != null) {
                    val prev = match.quantity
                    val next = prev + parsed.quantity
                    val price = if (parsed.unitPrice > 0.0) parsed.unitPrice else match.unitPrice
                    val updated = match.copy(
                        quantity = next,
                        unitPrice = price,
                        lastPaidPrice = if (match.unitPrice > 0.0 && match.unitPrice != price) match.unitPrice else match.lastPaidPrice,
                        updatedAt = System.currentTimeMillis()
                    )
                    productDao.update(updated)
                    existing[existing.indexOf(match)] = updated

                    if (cleanCode.isNotBlank() && existingAliases.none { it.externalCode.equals(cleanCode, ignoreCase = true) }) {
                        val alias = ProductAliasEntity(
                            productId = match.id,
                            externalCode = cleanCode,
                            marketName = cleanStore.ifBlank { "NFC-e" }
                        )
                        val aliasId = aliasDao.insert(alias)
                        existingAliases.add(alias.copy(id = aliasId))
                    }

                    movementDao.insert(
                        StockMovementEntity(
                            productId = match.id,
                            productNameSnapshot = match.name,
                            productUnitSnapshot = match.unit,
                            type = MovementType.ENTRADA,
                            quantityDelta = parsed.quantity,
                            previousBalance = prev,
                            newBalance = next,
                            unitPrice = price,
                            totalValue = parsed.quantity * price,
                            reason = "$reasonLabel (${match.uniqueCode})",
                            authorName = authorName
                        )
                    )
                } else {
                    val uniqueCode = formatUniqueCode(nextSeq++)
                    val newProd = ProductEntity(
                        uniqueCode = uniqueCode,
                        code = cleanCode,
                        name = parsed.name.trim(),
                        categoryId = targetCategory,
                        unit = parsed.unit,
                        quantity = parsed.quantity,
                        minStock = 1.0,
                        unitPrice = parsed.unitPrice
                    )
                    val newId = productDao.insert(newProd)
                    val savedProd = newProd.copy(id = newId)
                    existing.add(savedProd)

                    if (cleanCode.isNotBlank()) {
                        val alias = ProductAliasEntity(
                            productId = newId,
                            externalCode = cleanCode,
                            marketName = cleanStore.ifBlank { "NFC-e" }
                        )
                        val aliasId = aliasDao.insert(alias)
                        existingAliases.add(alias.copy(id = aliasId))
                    }

                    movementDao.insert(
                        StockMovementEntity(
                            productId = newId,
                            productNameSnapshot = newProd.name,
                            productUnitSnapshot = newProd.unit,
                            type = MovementType.ENTRADA,
                            quantityDelta = parsed.quantity,
                            previousBalance = 0.0,
                            newBalance = parsed.quantity,
                            unitPrice = parsed.unitPrice,
                            totalValue = parsed.quantity * parsed.unitPrice,
                            reason = "$reasonLabel ($uniqueCode)",
                            authorName = authorName
                        )
                    )
                }
                importedCount++
            }
            importedCount
        }
    }

    suspend fun saveUser(user: HouseholdUserEntity) {
        userDao.insert(user)
    }

    suspend fun deleteUser(id: Long) {
        userDao.deleteById(id)
    }

    suspend fun loadSampleCatalog(authorName: String) {
        ensureDefaultsSeeded()
        db.withTransaction {
            checkoutDao.deleteAll()
            aliasDao.deleteAll()
            movementDao.deleteAll()
            productDao.deleteAll()
            categoryDao.deleteAll()

            val despensaId = categoryDao.insert(
                CategoryEntity(name = "Despensa", colorHex = 0xFF5B3DF5, sectorNote = "Armários secos e bebidas")
            )
            val geladeiraId = categoryDao.insert(
                CategoryEntity(name = "Geladeira", colorHex = 0xFF0284C7, sectorNote = "Frios e perecíveis")
            )
            val limpezaId = categoryDao.insert(
                CategoryEntity(name = "Limpeza", colorHex = 0xFF059669, sectorNote = "Lavanderia e cozinha")
            )
            val higieneId = categoryDao.insert(
                CategoryEntity(name = "Higiene", colorHex = 0xFFD97706, sectorNote = "Banheiros")
            )

            val now = System.currentTimeMillis()
            val day = 86_400_000L

            val p1 = productDao.insert(
                ProductEntity(
                    uniqueCode = "AST-0001",
                    code = "7891000100103",
                    name = "COXINHA 60GR",
                    categoryId = despensaId,
                    unit = "kg",
                    quantity = 0.17,
                    minStock = 1.0,
                    unitPrice = 32.90,
                    lastPaidPrice = 30.50,
                    expiryDateMillis = now + 12 * day
                )
            )
            val p2 = productDao.insert(
                ProductEntity(
                    uniqueCode = "AST-0002",
                    code = "7891000200202",
                    name = "PAO DE QUEIJO",
                    categoryId = despensaId,
                    unit = "kg",
                    quantity = 0.22,
                    minStock = 1.0,
                    unitPrice = 24.50,
                    lastPaidPrice = 26.00,
                    expiryDateMillis = now + 21 * day
                )
            )
            val p3 = productDao.insert(
                ProductEntity(
                    uniqueCode = "AST-0003",
                    code = "7891149103508",
                    name = "CERVEJA SKOL 350ML",
                    categoryId = despensaId,
                    unit = "un",
                    quantity = 1.0,
                    minStock = 6.0,
                    unitPrice = 4.29,
                    lastPaidPrice = 3.99
                )
            )
            val p4 = productDao.insert(
                ProductEntity(
                    uniqueCode = "AST-0004",
                    code = "7891000400401",
                    name = "PASTEL FRITO DE CALABRESA C QUEIJO",
                    categoryId = geladeiraId,
                    unit = "un",
                    quantity = 1.0,
                    minStock = 4.0,
                    unitPrice = 7.50,
                    lastPaidPrice = 7.50,
                    expiryDateMillis = now + 5 * day
                )
            )
            val p5 = productDao.insert(
                ProductEntity(
                    uniqueCode = "AST-0005",
                    code = "7891024135006",
                    name = "DETERGENTE NEUTRO 500ML",
                    categoryId = limpezaId,
                    unit = "un",
                    quantity = 1.0,
                    minStock = 3.0,
                    unitPrice = 3.45,
                    lastPaidPrice = 3.89
                )
            )
            val p6 = productDao.insert(
                ProductEntity(
                    uniqueCode = "AST-0006",
                    code = "7891150024601",
                    name = "SABONETE LIQUIDO LAVANDA",
                    categoryId = higieneId,
                    unit = "un",
                    quantity = 1.0,
                    minStock = 2.0,
                    unitPrice = 14.50,
                    lastPaidPrice = 13.90
                )
            )

            listOf(
                ProductAliasEntity(productId = p1, externalCode = "7891000100103", marketName = "Angeloni"),
                ProductAliasEntity(productId = p2, externalCode = "7891000200202", marketName = "Angeloni"),
                ProductAliasEntity(productId = p3, externalCode = "7891149103508", marketName = "Bistek"),
                ProductAliasEntity(productId = p3, externalCode = "00035012", marketName = "Giassi"),
                ProductAliasEntity(productId = p4, externalCode = "7891000400401", marketName = "Padaria Central"),
                ProductAliasEntity(productId = p5, externalCode = "7891024135006", marketName = "Fort Atacadista"),
                ProductAliasEntity(productId = p6, externalCode = "7891150024601", marketName = "Farmácia")
            ).forEach { aliasDao.insert(it) }

            fun monthsAgo(months: Int): Long {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -months)
                return cal.timeInMillis
            }

            val seedMovements = listOf(
                StockMovementEntity(
                    productId = p1,
                    productNameSnapshot = "COXINHA 60GR",
                    productUnitSnapshot = "kg",
                    type = MovementType.ENTRADA,
                    quantityDelta = 1.0,
                    previousBalance = 0.0,
                    newBalance = 1.0,
                    unitPrice = 32.90,
                    totalValue = 32.90,
                    reason = "Compra semanal (AST-0001)",
                    authorName = authorName,
                    timestampMillis = monthsAgo(1)
                ),
                StockMovementEntity(
                    productId = p1,
                    productNameSnapshot = "COXINHA 60GR",
                    productUnitSnapshot = "kg",
                    type = MovementType.SAIDA,
                    quantityDelta = -0.83,
                    previousBalance = 1.0,
                    newBalance = 0.17,
                    unitPrice = 32.90,
                    totalValue = 27.31,
                    reason = "Consumo doméstico",
                    authorName = authorName,
                    timestampMillis = now - 2 * day
                ),
                StockMovementEntity(
                    productId = p2,
                    productNameSnapshot = "PAO DE QUEIJO",
                    productUnitSnapshot = "kg",
                    type = MovementType.ENTRADA,
                    quantityDelta = 1.0,
                    previousBalance = 0.0,
                    newBalance = 1.0,
                    unitPrice = 24.50,
                    totalValue = 24.50,
                    reason = "Reposição café da manhã (AST-0002)",
                    authorName = authorName,
                    timestampMillis = monthsAgo(2)
                ),
                StockMovementEntity(
                    productId = p3,
                    productNameSnapshot = "CERVEJA SKOL 350ML",
                    productUnitSnapshot = "un",
                    type = MovementType.ENTRADA,
                    quantityDelta = 6.0,
                    previousBalance = 0.0,
                    newBalance = 6.0,
                    unitPrice = 4.29,
                    totalValue = 25.74,
                    reason = "Compra mercado (AST-0003)",
                    authorName = authorName,
                    timestampMillis = monthsAgo(0) - 5 * day
                ),
                StockMovementEntity(
                    productId = p4,
                    productNameSnapshot = "PASTEL FRITO DE CALABRESA C QUEIJO",
                    productUnitSnapshot = "un",
                    type = MovementType.ENTRADA,
                    quantityDelta = 3.0,
                    previousBalance = 0.0,
                    newBalance = 3.0,
                    unitPrice = 7.50,
                    totalValue = 22.50,
                    reason = "Compra feira (AST-0004)",
                    authorName = authorName,
                    timestampMillis = monthsAgo(0) - 3 * day
                ),
                StockMovementEntity(
                    productId = p5,
                    productNameSnapshot = "DETERGENTE NEUTRO 500ML",
                    productUnitSnapshot = "un",
                    type = MovementType.SAIDA,
                    quantityDelta = -2.0,
                    previousBalance = 3.0,
                    newBalance = 1.0,
                    unitPrice = 3.45,
                    totalValue = 6.90,
                    reason = "Uso cozinha",
                    authorName = authorName,
                    timestampMillis = monthsAgo(0) - 1 * day
                ),
                StockMovementEntity(
                    productId = p6,
                    productNameSnapshot = "SABONETE LIQUIDO LAVANDA",
                    productUnitSnapshot = "un",
                    type = MovementType.ENTRADA,
                    quantityDelta = 2.0,
                    previousBalance = 0.0,
                    newBalance = 2.0,
                    unitPrice = 14.50,
                    totalValue = 29.00,
                    reason = "Compra farmácia (AST-0006)",
                    authorName = authorName,
                    timestampMillis = monthsAgo(3)
                )
            )
            seedMovements.forEach { movementDao.insert(it) }
        }
    }
}
