package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.CartItem
import com.example.data.model.Category
import com.example.data.model.FinishedPurchaseSummary
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.PurchaseItem
import com.example.data.model.StockMovement
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

class AstroRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Você precisa entrar com sua conta Google antes de salvar.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun observeCategories(userId: String): Flow<List<Category>> =
        observeUserCollection(
            userId = userId,
            collectionName = "categories",
            queryBuilder = { ref -> ref.orderBy("name", Query.Direction.ASCENDING) },
            mapper = { snap ->
                snap.toObjects(Category::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        )

    fun observeProducts(userId: String): Flow<List<Product>> =
        observeUserCollection(
            userId = userId,
            collectionName = "products",
            queryBuilder = { ref -> ref.orderBy("name", Query.Direction.ASCENDING) },
            mapper = { snap ->
                snap.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        )

    fun observeMovements(userId: String): Flow<List<StockMovement>> =
        observeUserCollection(
            userId = userId,
            collectionName = "movements",
            queryBuilder = { ref ->
                ref.orderBy("createdAt", Query.Direction.DESCENDING).limit(120)
            },
            mapper = { snap ->
                snap.toObjects(StockMovement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        )

    fun observePurchases(userId: String): Flow<List<Purchase>> =
        observeUserCollection(
            userId = userId,
            collectionName = "purchases",
            queryBuilder = { ref ->
                ref.orderBy("createdAt", Query.Direction.DESCENDING).limit(60)
            },
            mapper = { snap ->
                snap.toObjects(Purchase::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        )

    fun observePurchaseItems(userId: String): Flow<List<PurchaseItem>> =
        observeUserCollection(
            userId = userId,
            collectionName = "purchaseItems",
            queryBuilder = { ref ->
                ref.orderBy("createdAt", Query.Direction.DESCENDING).limit(300)
            },
            mapper = { snap ->
                snap.toObjects(PurchaseItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        )

    private fun <T> observeUserCollection(
        userId: String,
        collectionName: String,
        queryBuilder: (com.google.firebase.firestore.CollectionReference) -> Query,
        mapper: (com.google.firebase.firestore.QuerySnapshot) -> List<T>
    ): Flow<List<T>> = flow {
        val uid = userId.takeIf { it.isNotBlank() } ?: requireUserId()
        val path = "users/$uid/$collectionName"
        val query = queryBuilder(userDoc(uid).collection(collectionName))
        emitAll(
            query.snapshots()
                .map { snapshot -> mapper(snapshot) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfileAndSeed(user: FirebaseUser): Result<Unit> {
        val uid = user.uid
        val path = "users/$uid"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Sessão alterada durante inicialização." }

            val profileSnap = userDoc(uid).get().await()
            val alreadySeeded = profileSnap.exists() && (profileSnap.getBoolean("seededDefaults") == true)
            if (!profileSnap.exists()) {
                val profilePayload = mapOf(
                    "userId" to uid,
                    "displayName" to (user.displayName?.take(120)?.ifBlank { null } ?: "Usuário"),
                    "email" to user.email?.take(200),
                    "seededDefaults" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                userDoc(uid).set(profilePayload).await()
            } else if (!alreadySeeded) {
                userDoc(uid).update(
                    mapOf(
                        "seededDefaults" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            if (!alreadySeeded) {
                val existingCats = userDoc(uid).collection("categories").limit(1).get().await()
                if (existingCats.isEmpty) {
                    seedStarterHouseholdInventory(uid)
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun seedStarterHouseholdInventory(uid: String) {
        val batch = db.batch()
        val categoriesCol = userDoc(uid).collection("categories")
        val productsCol = userDoc(uid).collection("products")
        val movementsCol = userDoc(uid).collection("movements")

        data class SeedCat(val name: String, val desc: String, val color: String)
        val seedCats = listOf(
            SeedCat("Alimentos Secos", "Grãos, massas, farinhas e enlatados", "#f59e0b"),
            SeedCat("Geladeira & Laticínios", "Leite, queijos, ovos e perecíveis", "#0ea5e9"),
            SeedCat("Limpeza", "Detergentes, desinfetantes e esponjas", "#10b981"),
            SeedCat("Higiene Pessoal", "Sabonetes, pastas de dente e papel higiênico", "#8b5cf6"),
            SeedCat("Bebidas", "Café, sucos, água e chás", "#ec4899")
        )

        val catRefs = seedCats.associate { cat ->
            val ref = categoriesCol.document()
            batch.set(
                ref,
                mapOf(
                    "name" to cat.name,
                    "description" to cat.desc,
                    "color" to cat.color,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            cat.name to (ref.id to cat.color)
        }

        data class SeedProd(
            val sku: String,
            val name: String,
            val catName: String,
            val location: String,
            val unit: String,
            val qty: Double,
            val minQty: Double,
            val price: Double,
            val daysToExpire: Long?,
            val notes: String?
        )

        val seedProds = listOf(
            SeedProd("ALI-001", "Arroz Tipo 1 (5kg)", "Alimentos Secos", "Despensa", "pct", 2.0, 1.0, 29.90, 180L, "Marca preferida da casa"),
            SeedProd("ALI-002", "Feijão Carioca (1kg)", "Alimentos Secos", "Despensa", "pct", 1.0, 2.0, 8.50, 120L, "Abaixo do mínimo para lembrar de repor"),
            SeedProd("GEL-001", "Leite Integral (1L)", "Geladeira & Laticínios", "Geladeira", "L", 4.0, 3.0, 5.49, 20L, null),
            SeedProd("LIM-001", "Detergente Líquido (500ml)", "Limpeza", "Lavanderia", "un", 1.0, 2.0, 2.99, null, "Comprar no próximo mercado"),
            SeedProd("BEB-001", "Café Torrado e Moído (500g)", "Bebidas", "Armário Cozinha", "pct", 2.0, 1.0, 19.90, 90L, null)
        )

        val today = LocalDate.now()
        for (p in seedProds) {
            val (catId, catColor) = catRefs[p.catName] ?: ("" to "#3b82f6")
            val prodRef = productsCol.document()
            val exp = p.daysToExpire?.let { today.plusDays(it).toString() }
            val prodMap = mapOf(
                "name" to p.name,
                "sku" to p.sku,
                "categoryId" to catId.ifBlank { null },
                "categoryName" to p.catName,
                "categoryColor" to catColor,
                "location" to p.location,
                "unit" to p.unit,
                "quantity" to p.qty,
                "minQuantity" to p.minQty,
                "unitPrice" to p.price,
                "previousUnitPrice" to p.price,
                "expirationDate" to exp,
                "notes" to p.notes,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            batch.set(prodRef, prodMap)

            val movRef = movementsCol.document()
            batch.set(
                movRef,
                mapOf(
                    "productId" to prodRef.id,
                    "productName" to p.name,
                    "productSku" to p.sku,
                    "productUnit" to p.unit,
                    "type" to "IN",
                    "quantity" to p.qty,
                    "previousQuantity" to 0.0,
                    "newQuantity" to p.qty,
                    "reason" to "Carga inicial do estoque da casa",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }

        batch.commit().await()
    }

    suspend fun saveCategory(
        categoryId: String?,
        name: String,
        description: String?,
        color: String
    ): Result<Unit> {
        val cleanName = name.trim().take(80)
        val cleanDesc = description?.trim()?.take(250)?.ifBlank { null }
        val cleanColor = color.trim().ifBlank { "#3b82f6" }.take(16)
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/categories"
        return try {
            require(cleanName.isNotEmpty()) { "Informe o nome da categoria." }
            val uid = requireUserId()
            val col = userDoc(uid).collection("categories")
            if (categoryId.isNullOrBlank()) {
                val ref = col.document()
                docPath = ref.path
                val payload = mapOf(
                    "name" to cleanName,
                    "description" to cleanDesc,
                    "color" to cleanColor,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                ref.set(payload).await()
            } else {
                val ref = col.document(categoryId)
                docPath = ref.path
                val updates = mutableMapOf<String, Any>(
                    "name" to cleanName,
                    "color" to cleanColor,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanDesc != null) updates["description"] = cleanDesc
                ref.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/categories/$categoryId"
        return try {
            require(categoryId.isNotBlank()) { "Categoria inválida." }
            val uid = requireUserId()
            val ref = userDoc(uid).collection("categories").document(categoryId)
            docPath = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveProduct(
        existingProduct: Product?,
        name: String,
        sku: String?,
        barcode: String?,
        category: Category?,
        location: String,
        unit: String,
        quantity: Double,
        minQuantity: Double,
        unitPrice: Double,
        previousUnitPrice: Double,
        expirationDate: String?,
        notes: String?
    ): Result<Unit> {
        val cleanName = name.trim().take(150)
        val cleanLocation = location.trim().ifBlank { "Despensa" }.take(80)
        val cleanUnit = unit.trim().ifBlank { "un" }.take(16)
        val safeQty = quantity.coerceIn(0.0, 1_000_000.0)
        val safeMinQty = minQuantity.coerceIn(0.0, 1_000_000.0)
        val safePrice = unitPrice.coerceIn(0.0, 10_000_000.0)
        val safePrevPrice = previousUnitPrice.coerceIn(0.0, 10_000_000.0)
        val cleanSku = sku?.trim()?.take(64)?.ifBlank { null }
        val cleanBarcode = barcode?.trim()?.take(64)?.ifBlank { null } ?: cleanSku
        val cleanExp = expirationDate?.trim()?.take(32)?.ifBlank { null }
        val cleanNotes = notes?.trim()?.take(500)?.ifBlank { null }

        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/products"
        return try {
            require(cleanName.isNotEmpty()) { "Informe o nome do produto." }
            val uid = requireUserId()
            val productsCol = userDoc(uid).collection("products")
            val movementsCol = userDoc(uid).collection("movements")
            val batch = db.batch()

            if (existingProduct == null) {
                val ref = productsCol.document()
                docPath = ref.path
                val payload = mapOf(
                    "name" to cleanName,
                    "sku" to cleanSku,
                    "barcode" to cleanBarcode,
                    "categoryId" to category?.id?.ifBlank { null },
                    "categoryName" to category?.name?.take(80),
                    "categoryColor" to category?.color?.take(16),
                    "location" to cleanLocation,
                    "unit" to cleanUnit,
                    "quantity" to safeQty,
                    "minQuantity" to safeMinQty,
                    "unitPrice" to safePrice,
                    "previousUnitPrice" to safePrevPrice,
                    "expirationDate" to cleanExp,
                    "notes" to cleanNotes,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                batch.set(ref, payload)

                if (safeQty > 0.0) {
                    val movRef = movementsCol.document()
                    batch.set(
                        movRef,
                        mapOf(
                            "productId" to ref.id,
                            "productName" to cleanName,
                            "productSku" to cleanSku,
                            "productUnit" to cleanUnit,
                            "type" to "IN",
                            "quantity" to safeQty,
                            "previousQuantity" to 0.0,
                            "newQuantity" to safeQty,
                            "reason" to "Cadastro inicial",
                            "createdAt" to FieldValue.serverTimestamp()
                        ).filterValues { it != null }
                    )
                }
            } else {
                val ref = productsCol.document(existingProduct.id)
                docPath = ref.path
                val updates = mutableMapOf<String, Any>(
                    "name" to cleanName,
                    "location" to cleanLocation,
                    "unit" to cleanUnit,
                    "quantity" to safeQty,
                    "minQuantity" to safeMinQty,
                    "unitPrice" to safePrice,
                    "previousUnitPrice" to safePrevPrice,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanSku != null) updates["sku"] = cleanSku
                if (cleanBarcode != null) updates["barcode"] = cleanBarcode
                if (category != null) {
                    updates["categoryId"] = category.id
                    updates["categoryName"] = category.name.take(80)
                    updates["categoryColor"] = category.color.take(16)
                }
                if (cleanExp != null) updates["expirationDate"] = cleanExp
                if (cleanNotes != null) updates["notes"] = cleanNotes

                batch.update(ref, updates)

                val diff = safeQty - existingProduct.quantity
                if (kotlin.math.abs(diff) > 0.0001) {
                    val movRef = movementsCol.document()
                    batch.set(
                        movRef,
                        mapOf(
                            "productId" to existingProduct.id,
                            "productName" to cleanName,
                            "productSku" to cleanSku,
                            "productUnit" to cleanUnit,
                            "type" to "ADJUST",
                            "quantity" to kotlin.math.abs(diff),
                            "previousQuantity" to existingProduct.quantity,
                            "newQuantity" to safeQty,
                            "reason" to "Ajuste manual no cadastro",
                            "createdAt" to FieldValue.serverTimestamp()
                        ).filterValues { it != null }
                    )
                }
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/products/$productId"
        return try {
            require(productId.isNotBlank()) { "Produto inválido." }
            val uid = requireUserId()
            val ref = userDoc(uid).collection("products").document(productId)
            docPath = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordMovement(
        product: Product,
        type: String,
        quantity: Double,
        reason: String?
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/products/${product.id}"
        return try {
            require(type == "IN" || type == "OUT") { "Tipo de movimentação inválido." }
            require(quantity > 0.0 && quantity <= 1_000_000.0) { "A quantidade deve ser maior que zero." }
            val uid = requireUserId()
            val newQty = if (type == "IN") product.quantity + quantity else product.quantity - quantity
            require(newQty >= 0.0) { "Saldo insuficiente no estoque." }

            val prodRef = userDoc(uid).collection("products").document(product.id)
            docPath = prodRef.path
            val movRef = userDoc(uid).collection("movements").document()

            val batch = db.batch()
            batch.update(
                prodRef,
                mapOf(
                    "quantity" to newQty,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                movRef,
                mapOf(
                    "productId" to product.id,
                    "productName" to product.name.take(150),
                    "productSku" to product.sku?.take(64),
                    "productUnit" to product.unit.take(16),
                    "type" to type,
                    "quantity" to quantity,
                    "previousQuantity" to product.quantity,
                    "newQuantity" to newQty,
                    "reason" to reason?.trim()?.take(250)?.ifBlank { null },
                    "createdAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun finishPurchase(
        storeName: String,
        notes: String?,
        cartItems: List<CartItem>,
        existingProducts: List<Product>,
        categories: List<Category>
    ): Result<FinishedPurchaseSummary> {
        val cleanStore = storeName.trim().ifBlank { "Supermercado" }.take(120)
        val cleanNotes = notes?.trim()?.take(300)?.ifBlank { null }
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/purchases"
        return try {
            require(cartItems.isNotEmpty()) { "Adicione pelo menos um item ao carrinho." }
            for (item in cartItems) {
                require(item.name.trim().isNotEmpty()) {
                    "Informe o nome de todos os itens antes de finalizar."
                }
                require(item.parsedQuantity > 0.0) {
                    "A quantidade de \"${item.name}\" deve ser maior que zero."
                }
            }

            val uid = requireUserId()
            val purchasesCol = userDoc(uid).collection("purchases")
            val purchaseItemsCol = userDoc(uid).collection("purchaseItems")
            val productsCol = userDoc(uid).collection("products")
            val movementsCol = userDoc(uid).collection("movements")

            val purchaseRef = purchasesCol.document()
            docPath = purchaseRef.path

            val totalAmount = cartItems.sumOf { it.subtotal }
            val totalUnits = cartItems.sumOf { it.parsedQuantity }
            val batch = db.batch()

            batch.set(
                purchaseRef,
                mapOf(
                    "storeName" to cleanStore,
                    "notes" to cleanNotes,
                    "totalAmount" to totalAmount,
                    "itemsCount" to cartItems.size.toLong(),
                    "totalUnits" to totalUnits,
                    "createdAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
            )

            val productById = existingProducts.associateBy { it.id }
            for (item in cartItems) {
                val qty = item.parsedQuantity
                val price = item.parsedUnitPrice
                val cleanName = item.name.trim().take(150)
                val cleanBarcode = item.barcode.trim().take(64).ifBlank { null }
                val existing = item.productId?.let { productById[it] }

                val resolvedProductId: String
                val prevQty: Double
                val prevPrice: Double
                val catName: String
                val catColor: String

                if (existing != null) {
                    resolvedProductId = existing.id
                    prevQty = existing.quantity
                    prevPrice = if (existing.unitPrice > 0.0 && existing.unitPrice != price) {
                        existing.unitPrice
                    } else {
                        existing.previousUnitPrice
                    }
                    catName = existing.categoryName ?: item.categoryName
                    catColor = existing.categoryColor ?: item.categoryColor

                    val prodRef = productsCol.document(existing.id)
                    val updates = mutableMapOf<String, Any>(
                        "quantity" to (existing.quantity + qty),
                        "unitPrice" to price,
                        "previousUnitPrice" to prevPrice,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (cleanBarcode != null && existing.barcode.isNullOrBlank()) {
                        updates["barcode"] = cleanBarcode
                    }
                    batch.update(prodRef, updates)
                } else {
                    val newProdRef = productsCol.document()
                    resolvedProductId = newProdRef.id
                    prevQty = 0.0
                    prevPrice = price
                    val matchedCat = categories.firstOrNull { it.id == item.categoryId }
                        ?: categories.firstOrNull()
                    catName = matchedCat?.name ?: item.categoryName
                    catColor = matchedCat?.color ?: item.categoryColor

                    val newProdMap = mapOf(
                        "name" to cleanName,
                        "barcode" to cleanBarcode,
                        "categoryId" to matchedCat?.id?.ifBlank { null },
                        "categoryName" to catName.take(80),
                        "categoryColor" to catColor.take(16),
                        "location" to "Despensa",
                        "unit" to item.unit.take(16).ifBlank { "un" },
                        "quantity" to qty,
                        "minQuantity" to 1.0,
                        "unitPrice" to price,
                        "previousUnitPrice" to prevPrice,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                    batch.set(newProdRef, newProdMap)
                }

                val movRef = movementsCol.document()
                batch.set(
                    movRef,
                    mapOf(
                        "productId" to resolvedProductId,
                        "productName" to cleanName,
                        "productSku" to cleanBarcode,
                        "productUnit" to item.unit.take(16).ifBlank { "un" },
                        "type" to "IN",
                        "quantity" to qty,
                        "previousQuantity" to prevQty,
                        "newQuantity" to (prevQty + qty),
                        "reason" to "Compra em $cleanStore".take(250),
                        "createdAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                )

                val piRef = purchaseItemsCol.document()
                batch.set(
                    piRef,
                    mapOf(
                        "purchaseId" to purchaseRef.id,
                        "productId" to resolvedProductId,
                        "productName" to cleanName,
                        "barcode" to cleanBarcode,
                        "categoryName" to catName.take(80),
                        "categoryColor" to catColor.take(16),
                        "quantity" to qty,
                        "unitPrice" to price,
                        "previousUnitPrice" to prevPrice,
                        "totalPrice" to (qty * price),
                        "createdAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                )
            }

            batch.commit().await()
            Result.success(
                FinishedPurchaseSummary(
                    storeName = cleanStore,
                    totalAmount = totalAmount,
                    itemsCount = cartItems.size,
                    totalUnits = totalUnits
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }
}
