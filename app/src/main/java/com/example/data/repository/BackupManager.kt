package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.CategoryEntity
import com.example.data.local.MarketCheckoutEntity
import com.example.data.local.MovementType
import com.example.data.local.ProductAliasEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StockMovementEntity
import org.json.JSONArray
import org.json.JSONObject

data class BackupRestoreSummary(
    val categoriesRestored: Int,
    val productsRestored: Int,
    val aliasesRestored: Int,
    val movementsRestored: Int,
    val checkoutsRestored: Int
)

internal object BackupManager {
    private const val BACKUP_FORMAT_VERSION = 1

    suspend fun exportJson(db: AppDatabase): String {
        val categories = db.categoryDao().getAllOnce()
        val products = db.productDao().getAllOnce()
        val aliases = db.productAliasDao().getAllOnce()
        val movements = db.stockMovementDao().getAllOnce()
        val checkouts = db.marketCheckoutDao().getAllOnce()

        val root = JSONObject().apply {
            put("app", "AstroGestao")
            put("backupVersion", BACKUP_FORMAT_VERSION)
            put("exportedAtMillis", System.currentTimeMillis())

            put("categories", JSONArray().apply {
                categories.forEach { cat ->
                    put(
                        JSONObject().apply {
                            put("id", cat.id)
                            put("name", cat.name)
                            put("colorHex", cat.colorHex)
                            put("sectorNote", cat.sectorNote)
                        }
                    )
                }
            })

            put("products", JSONArray().apply {
                products.forEach { prod ->
                    put(
                        JSONObject().apply {
                            put("id", prod.id)
                            put("uniqueCode", prod.uniqueCode)
                            put("code", prod.code)
                            put("name", prod.name)
                            if (prod.categoryId != null) put("categoryId", prod.categoryId) else put("categoryId", JSONObject.NULL)
                            put("unit", prod.unit)
                            put("quantity", prod.quantity)
                            put("minStock", prod.minStock)
                            put("unitPrice", prod.unitPrice)
                            if (prod.lastPaidPrice != null) put("lastPaidPrice", prod.lastPaidPrice) else put("lastPaidPrice", JSONObject.NULL)
                            if (prod.expiryDateMillis != null) put("expiryDateMillis", prod.expiryDateMillis) else put("expiryDateMillis", JSONObject.NULL)
                            put("notes", prod.notes)
                            put("updatedAt", prod.updatedAt)
                        }
                    )
                }
            })

            put("aliases", JSONArray().apply {
                aliases.forEach { alias ->
                    put(
                        JSONObject().apply {
                            put("productId", alias.productId)
                            put("externalCode", alias.externalCode)
                            put("marketName", alias.marketName)
                            put("createdAt", alias.createdAt)
                        }
                    )
                }
            })

            put("movements", JSONArray().apply {
                movements.forEach { mv ->
                    put(
                        JSONObject().apply {
                            put("productId", mv.productId)
                            put("productNameSnapshot", mv.productNameSnapshot)
                            put("productUnitSnapshot", mv.productUnitSnapshot)
                            put("type", mv.type.name)
                            put("quantityDelta", mv.quantityDelta)
                            put("previousBalance", mv.previousBalance)
                            put("newBalance", mv.newBalance)
                            put("unitPrice", mv.unitPrice)
                            put("totalValue", mv.totalValue)
                            put("reason", mv.reason)
                            put("authorName", mv.authorName)
                            put("timestampMillis", mv.timestampMillis)
                        }
                    )
                }
            })

            put("checkouts", JSONArray().apply {
                checkouts.forEach { ck ->
                    put(
                        JSONObject().apply {
                            put("storeName", ck.storeName)
                            put("notes", ck.notes)
                            put("totalAmount", ck.totalAmount)
                            put("itemCount", ck.itemCount)
                            put("savedToStock", ck.savedToStock)
                            put("authorName", ck.authorName)
                            put("timestampMillis", ck.timestampMillis)
                        }
                    )
                }
            })
        }

        return root.toString(2)
    }

    suspend fun importJson(db: AppDatabase, rawJson: String): Result<BackupRestoreSummary> {
        return runCatching {
            val root = JSONObject(rawJson)
            require(root.optString("app") == "AstroGestao") {
                "Arquivo inválido: este JSON não é um backup do AstroGestão."
            }
            val version = root.optInt("backupVersion", 0)
            require(version in 1..BACKUP_FORMAT_VERSION) {
                "Versão de backup não suportada ($version)."
            }

            val categoriesJson = root.optJSONArray("categories") ?: JSONArray()
            val productsJson = root.optJSONArray("products") ?: JSONArray()
            val aliasesJson = root.optJSONArray("aliases") ?: JSONArray()
            val movementsJson = root.optJSONArray("movements") ?: JSONArray()
            val checkoutsJson = root.optJSONArray("checkouts") ?: JSONArray()

            db.withTransaction {
                val categoryDao = db.categoryDao()
                val productDao = db.productDao()
                val aliasDao = db.productAliasDao()
                val movementDao = db.stockMovementDao()
                val checkoutDao = db.marketCheckoutDao()

                checkoutDao.deleteAll()
                aliasDao.deleteAll()
                movementDao.deleteAll()
                productDao.deleteAll()
                categoryDao.deleteAll()

                val categoryIdMap = mutableMapOf<Long, Long>()
                var catCount = 0
                for (i in 0 until categoriesJson.length()) {
                    val obj = categoriesJson.getJSONObject(i)
                    val oldId = obj.optLong("id", 0L)
                    val name = obj.optString("name").trim()
                    if (name.isBlank()) continue
                    val newId = categoryDao.insert(
                        CategoryEntity(
                            name = name,
                            colorHex = obj.optLong("colorHex", 0xFF5B3DF5),
                            sectorNote = obj.optString("sectorNote", "")
                        )
                    )
                    if (oldId > 0L) categoryIdMap[oldId] = newId
                    catCount++
                }

                val productIdMap = mutableMapOf<Long, Long>()
                val existingForSeq = mutableListOf<ProductEntity>()
                var nextSeq = 1
                var prodCount = 0

                for (i in 0 until productsJson.length()) {
                    val obj = productsJson.getJSONObject(i)
                    val oldId = obj.optLong("id", 0L)
                    val name = obj.optString("name").trim()
                    if (name.isBlank()) continue
                    val oldCatId = if (obj.isNull("categoryId")) null else obj.optLong("categoryId")
                    val mappedCatId = oldCatId?.let { categoryIdMap[it] }
                    val rawUnique = obj.optString("uniqueCode").trim()
                    val assignedUnique = rawUnique.ifBlank {
                        ProductMatcher.formatUniqueCode(nextSeq++)
                    }
                    val lastPaid = if (obj.isNull("lastPaidPrice")) null else obj.optDouble("lastPaidPrice")
                    val expiry = if (obj.isNull("expiryDateMillis")) null else obj.optLong("expiryDateMillis")

                    val entity = ProductEntity(
                        uniqueCode = assignedUnique,
                        code = obj.optString("code", ""),
                        name = name,
                        categoryId = mappedCatId,
                        unit = obj.optString("unit", "un").ifBlank { "un" },
                        quantity = obj.optDouble("quantity", 0.0).coerceAtLeast(0.0),
                        minStock = obj.optDouble("minStock", 1.0).coerceAtLeast(0.0),
                        unitPrice = obj.optDouble("unitPrice", 0.0).coerceAtLeast(0.0),
                        lastPaidPrice = lastPaid,
                        expiryDateMillis = expiry,
                        notes = obj.optString("notes", ""),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                    val newId = productDao.insert(entity)
                    existingForSeq.add(entity.copy(id = newId))
                    nextSeq = maxOf(nextSeq, ProductMatcher.nextUniqueSequence(existingForSeq))
                    if (oldId > 0L) productIdMap[oldId] = newId
                    prodCount++
                }

                var aliasCount = 0
                for (i in 0 until aliasesJson.length()) {
                    val obj = aliasesJson.getJSONObject(i)
                    val mappedProdId = productIdMap[obj.optLong("productId", 0L)] ?: continue
                    val extCode = obj.optString("externalCode").trim()
                    if (extCode.isBlank()) continue
                    aliasDao.insert(
                        ProductAliasEntity(
                            productId = mappedProdId,
                            externalCode = extCode,
                            marketName = obj.optString("marketName", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                    aliasCount++
                }

                var moveCount = 0
                for (i in 0 until movementsJson.length()) {
                    val obj = movementsJson.getJSONObject(i)
                    val mappedProdId = productIdMap[obj.optLong("productId", 0L)] ?: continue
                    val typeName = obj.optString("type", MovementType.ENTRADA.name)
                    val type = runCatching { MovementType.valueOf(typeName) }.getOrDefault(MovementType.ENTRADA)
                    movementDao.insert(
                        StockMovementEntity(
                            productId = mappedProdId,
                            productNameSnapshot = obj.optString("productNameSnapshot", ""),
                            productUnitSnapshot = obj.optString("productUnitSnapshot", "un"),
                            type = type,
                            quantityDelta = obj.optDouble("quantityDelta", 0.0),
                            previousBalance = obj.optDouble("previousBalance", 0.0),
                            newBalance = obj.optDouble("newBalance", 0.0),
                            unitPrice = obj.optDouble("unitPrice", 0.0),
                            totalValue = obj.optDouble("totalValue", 0.0),
                            reason = obj.optString("reason", ""),
                            authorName = obj.optString("authorName", "Morador"),
                            timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis())
                        )
                    )
                    moveCount++
                }

                var checkoutCount = 0
                for (i in 0 until checkoutsJson.length()) {
                    val obj = checkoutsJson.getJSONObject(i)
                    checkoutDao.insert(
                        MarketCheckoutEntity(
                            storeName = obj.optString("storeName", "Supermercado"),
                            notes = obj.optString("notes", ""),
                            totalAmount = obj.optDouble("totalAmount", 0.0),
                            itemCount = obj.optInt("itemCount", 0),
                            savedToStock = obj.optBoolean("savedToStock", true),
                            authorName = obj.optString("authorName", "Morador"),
                            timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis())
                        )
                    )
                    checkoutCount++
                }

                BackupRestoreSummary(
                    categoriesRestored = catCount,
                    productsRestored = prodCount,
                    aliasesRestored = aliasCount,
                    movementsRestored = moveCount,
                    checkoutsRestored = checkoutCount
                )
            }
        }
    }
}
