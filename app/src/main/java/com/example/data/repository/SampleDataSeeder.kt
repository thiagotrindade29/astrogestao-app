package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.CategoryEntity
import com.example.data.local.HouseholdUserEntity
import com.example.data.local.MovementType
import com.example.data.local.ProductAliasEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StockMovementEntity
import java.util.Calendar

internal class SampleDataSeeder(private val db: AppDatabase) {
    private val categoryDao = db.categoryDao()
    private val productDao = db.productDao()
    private val aliasDao = db.productAliasDao()
    private val movementDao = db.stockMovementDao()
    private val checkoutDao = db.marketCheckoutDao()
    private val userDao = db.householdUserDao()

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
            val currentProducts = productDao.getAllOnce()
            var nextSeq = ProductMatcher.nextUniqueSequence(currentProducts)
            currentProducts.filter { it.uniqueCode.isBlank() }.forEach { prod ->
                productDao.update(prod.copy(uniqueCode = ProductMatcher.formatUniqueCode(nextSeq++)))
            }
        }
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

            listOf(
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
            ).forEach { movementDao.insert(it) }
        }
    }
}
