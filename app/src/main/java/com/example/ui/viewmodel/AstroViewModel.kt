package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.CategoryEntity
import com.example.data.local.HouseholdUserEntity
import com.example.data.local.MarketCheckoutEntity
import com.example.data.local.MovementType
import com.example.data.local.ProductAliasEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StockMovementEntity
import com.example.data.nfce.NfceParser
import com.example.data.nfce.ReceiptImageProcessor
import com.example.data.nfce.RegionalPortal
import com.example.data.nfce.SefazPortals
import com.example.data.remote.FirestoreAstroRepository
import com.example.data.remote.HouseholdDoc
import com.example.data.remote.unwrapFirestoreException
import com.example.data.repository.PosCheckoutEntry
import com.example.data.repository.ProductMatcher
import com.example.data.repository.ThemePreferencesRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.max

private data class FilterState(
    val productSearchQuery: String = "",
    val selectedCategoryId: Long? = null,
    val onlyLowStock: Boolean = false,
    val movementFilterType: MovementType? = null
)

private sealed interface CloudDataState {
    data object Loading : CloudDataState
    data class Ready(
        val household: HouseholdDoc,
        val members: List<HouseholdUserEntity>,
        val categories: List<CategoryEntity>,
        val products: List<ProductEntity>,
        val aliases: List<ProductAliasEntity>,
        val movements: List<StockMovementEntity>,
        val checkouts: List<MarketCheckoutEntity>
    ) : CloudDataState

    data class Error(val message: String) : CloudDataState
}

@OptIn(ExperimentalCoroutinesApi::class)
class AstroViewModel(
    private val repository: FirestoreAstroRepository,
    private val prefs: ThemePreferencesRepository,
    private val localDb: AppDatabase,
    private val currentUserId: String,
    private val userEmail: String,
    private val userName: String
) : ViewModel() {

    private companion object {
        const val TAG = "AstroVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }

    private val filterState = MutableStateFlow(FilterState())
    private val hasLocalMigration = MutableStateFlow(false)
    private val isSyncingWorkspace = MutableStateFlow(false)
    private val bootstrapError = MutableStateFlow<String?>(null)

    private val _nfceState = MutableStateFlow(NfceImportState())
    val nfceState: StateFlow<NfceImportState> = _nfceState.asStateFlow()

    private val _posState = MutableStateFlow(PosMarketState())
    val posState: StateFlow<PosMarketState> = _posState.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private var lastSyncedSignature: String? = null

    private fun <T> Flow<T>.unwrapOrFallback(fallback: T, label: String): Flow<T> = catch { error ->
        val firestoreError = error.unwrapFirestoreException()
        if (firestoreError !is FirebaseFirestoreException && error is CancellationException) {
            throw error
        }
        Log.w(TAG, "Erro ao observar $label: ${firestoreError.message}", firestoreError)
        emit(fallback)
    }

    private val cloudDataState: StateFlow<CloudDataState> = repository.observeUserProfile(currentUserId)
        .map { it?.activeHouseholdId.orEmpty() }
        .distinctUntilChanged()
        .flatMapLatest { householdId ->
            if (householdId.isBlank()) {
                flowOf<CloudDataState>(CloudDataState.Loading)
            } else {
                combine(
                    repository.observeHousehold(householdId, currentUserId),
                    repository.observeMembers(householdId, currentUserId).unwrapOrFallback(emptyList(), "members"),
                    repository.observeCategories(householdId, currentUserId).unwrapOrFallback(emptyList(), "categories"),
                    repository.observeProducts(householdId, currentUserId).unwrapOrFallback(emptyList(), "products"),
                    repository.observeAliases(householdId, currentUserId).unwrapOrFallback(emptyList(), "aliases"),
                    repository.observeMovements(householdId, currentUserId).unwrapOrFallback(emptyList(), "movements"),
                    repository.observeCheckouts(householdId, currentUserId).unwrapOrFallback(emptyList(), "checkouts")
                ) { values ->
                    val household = values[0] as HouseholdDoc?
                    @Suppress("UNCHECKED_CAST")
                    if (household == null) {
                        CloudDataState.Loading
                    } else {
                        CloudDataState.Ready(
                            household = household,
                            members = values[1] as List<HouseholdUserEntity>,
                            categories = values[2] as List<CategoryEntity>,
                            products = values[3] as List<ProductEntity>,
                            aliases = values[4] as List<ProductAliasEntity>,
                            movements = values[5] as List<StockMovementEntity>,
                            checkouts = values[6] as List<MarketCheckoutEntity>
                        )
                    }
                }
                .catch { error ->
                    val firestoreError = error.unwrapFirestoreException()
                    if (firestoreError !is FirebaseFirestoreException && error is CancellationException) {
                        throw error
                    }
                    Log.w(TAG, "Erro ao observar casa $householdId", firestoreError)
                    emit(CloudDataState.Error(firestoreError.localizedMessage ?: "Falha ao sincronizar dados com a nuvem."))
                }
            }
        }
        .catch { error ->
            val firestoreError = error.unwrapFirestoreException()
            if (firestoreError !is FirebaseFirestoreException && error is CancellationException) {
                throw error
            }
            Log.w(TAG, "Erro no fluxo Firestore", firestoreError)
            emit(CloudDataState.Error(firestoreError.localizedMessage ?: "Falha ao sincronizar dados com a nuvem."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = CloudDataState.Loading
        )

    val uiState: StateFlow<AstroUiState> = combine(
        cloudDataState,
        prefs.isDarkMode,
        filterState,
        hasLocalMigration,
        isSyncingWorkspace,
        bootstrapError
    ) { values ->
        val cloud = values[0] as CloudDataState
        val isDark = values[1] as Boolean
        val filters = values[2] as FilterState
        val localMig = values[3] as Boolean
        val syncing = values[4] as Boolean
        val bootErr = values[5] as String?

        when (cloud) {
            CloudDataState.Loading -> AstroUiState(
                isLoading = bootErr == null,
                isLoggedIn = true,
                currentUserId = currentUserId,
                userEmail = userEmail,
                userName = userName,
                cloudErrorMessage = bootErr,
                isSyncingWorkspace = syncing,
                isDarkMode = isDark,
                productSearchQuery = filters.productSearchQuery,
                selectedCategoryId = filters.selectedCategoryId,
                onlyLowStock = filters.onlyLowStock,
                movementFilterType = filters.movementFilterType
            )

            is CloudDataState.Error -> AstroUiState(
                isLoading = false,
                isLoggedIn = true,
                currentUserId = currentUserId,
                userEmail = userEmail,
                userName = userName,
                cloudErrorMessage = cloud.message,
                isSyncingWorkspace = syncing,
                isDarkMode = isDark,
                productSearchQuery = filters.productSearchQuery,
                selectedCategoryId = filters.selectedCategoryId,
                onlyLowStock = filters.onlyLowStock,
                movementFilterType = filters.movementFilterType
            )

            is CloudDataState.Ready -> AstroUiState(
                isLoading = false,
                isLoggedIn = true,
                currentUserId = currentUserId,
                userEmail = userEmail,
                userName = userName,
                householdId = cloud.household.docId,
                householdName = cloud.household.name,
                householdInviteCode = cloud.household.inviteCode,
                householdOwnerUid = cloud.household.ownerUid,
                memberCount = cloud.household.memberUids.size,
                hasLocalMigrationAvailable = localMig,
                cloudErrorMessage = bootErr,
                isSyncingWorkspace = syncing,
                isDarkMode = isDark,
                categories = cloud.categories,
                products = cloud.products,
                aliases = cloud.aliases,
                movements = cloud.movements,
                checkouts = cloud.checkouts,
                users = cloud.members,
                productSearchQuery = filters.productSearchQuery,
                selectedCategoryId = filters.selectedCategoryId,
                onlyLowStock = filters.onlyLowStock,
                movementFilterType = filters.movementFilterType
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = AstroUiState(
            isLoading = true,
            isLoggedIn = true,
            currentUserId = currentUserId,
            userEmail = userEmail,
            userName = userName
        )
    )

    init {
        initializeCloudWorkspace()
        observeHouseholdMembershipSync()
    }

    fun initializeCloudWorkspace() {
        viewModelScope.launch {
            bootstrapError.value = null
            val wsResult = repository.ensureUserWorkspace(displayName = userName, email = userEmail)
            wsResult.onFailure { err ->
                bootstrapError.value = err.localizedMessage ?: "Não foi possível preparar seu espaço na nuvem."
            }
            val hasLocal = withContext(Dispatchers.IO) {
                repository.hasLocalRoomProducts(localDb)
            }
            hasLocalMigration.value = hasLocal
        }
    }

    private fun observeHouseholdMembershipSync() {
        viewModelScope.launch {
            cloudDataState.collect { state ->
                if (state is CloudDataState.Ready) {
                    val sig = "${state.household.docId}:${state.household.memberUids.sorted().joinToString(",")}"
                    if (sig != lastSyncedSignature && state.household.memberUids.size > 1) {
                        lastSyncedSignature = sig
                        repository.syncMemberUidsAcrossHousehold(state.household)
                    }
                }
            }
        }
    }

    private fun activeReadyState(): CloudDataState.Ready? = cloudDataState.value as? CloudDataState.Ready

    private fun emitResultError(err: Throwable, fallback: String) {
        _snackbarMessages.tryEmit(err.localizedMessage ?: fallback)
    }

    // ---- FAMÍLIA, CASA & PREFERÊNCIAS ----

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { prefs.setDarkMode(enabled) }
    }

    fun renameHousehold(newName: String) {
        val ready = activeReadyState() ?: return
        viewModelScope.launch {
            isSyncingWorkspace.value = true
            repository.renameHousehold(ready.household, newName).fold(
                onSuccess = { _snackbarMessages.tryEmit("Nome do espaço atualizado!") },
                onFailure = { emitResultError(it, "Falha ao renomear espaço.") }
            )
            isSyncingWorkspace.value = false
        }
    }

    fun regenerateInviteCode() {
        val ready = activeReadyState() ?: return
        viewModelScope.launch {
            isSyncingWorkspace.value = true
            repository.regenerateInviteCode(ready.household).fold(
                onSuccess = { code -> _snackbarMessages.tryEmit("Novo código da família gerado: $code") },
                onFailure = { emitResultError(it, "Falha ao gerar novo código.") }
            )
            isSyncingWorkspace.value = false
        }
    }

    fun joinHouseholdByCode(inviteCode: String) {
        viewModelScope.launch {
            isSyncingWorkspace.value = true
            repository.joinHouseholdByInviteCode(inviteCode, userName, userEmail).fold(
                onSuccess = { houseName ->
                    _snackbarMessages.tryEmit("Você entrou em \"$houseName\"! Dados compartilhados sincronizados.")
                },
                onFailure = { emitResultError(it, "Código inválido ou não encontrado.") }
            )
            isSyncingWorkspace.value = false
        }
    }

    fun createNewPersonalHousehold(name: String) {
        viewModelScope.launch {
            isSyncingWorkspace.value = true
            repository.createPersonalHousehold(userName, userEmail, name).fold(
                onSuccess = {
                    _snackbarMessages.tryEmit("Novo espaço individual criado na nuvem!")
                },
                onFailure = { emitResultError(it, "Falha ao criar novo espaço.") }
            )
            isSyncingWorkspace.value = false
        }
    }

    fun migrateLocalDataToCloud() {
        val ready = activeReadyState() ?: return
        viewModelScope.launch {
            isSyncingWorkspace.value = true
            val result = withContext(Dispatchers.IO) {
                repository.migrateLocalRoomToCloud(ready.household, localDb)
            }
            result.fold(
                onSuccess = { summary ->
                    hasLocalMigration.value = false
                    _snackbarMessages.tryEmit(
                        "Migração concluída: ${summary.productsRestored} produto(s) enviados para a nuvem!"
                    )
                },
                onFailure = { emitResultError(it, "Falha ao migrar dados locais para a nuvem.") }
            )
            isSyncingWorkspace.value = false
        }
    }

    // ---- FILTROS E CATÁLOGO ----

    fun updateProductSearch(query: String) {
        filterState.value = filterState.value.copy(productSearchQuery = query)
    }

    fun selectCategoryFilter(categoryId: Long?) {
        filterState.value = filterState.value.copy(selectedCategoryId = categoryId)
    }

    fun toggleLowStockOnly(enabled: Boolean) {
        filterState.value = filterState.value.copy(onlyLowStock = enabled)
    }

    fun selectMovementFilter(type: MovementType?) {
        filterState.value = filterState.value.copy(movementFilterType = type)
    }

    fun saveProduct(product: ProductEntity) {
        val ready = activeReadyState() ?: return
        viewModelScope.launch {
            repository.saveProduct(
                household = ready.household,
                product = product,
                existingProducts = ready.products,
                existingAliases = ready.aliases,
                authorName = userName
            ).fold(
                onSuccess = {
                    _snackbarMessages.tryEmit(
                        if (product.id == 0L) "Produto cadastrado na nuvem" else "Produto atualizado"
                    )
                },
                onFailure = { emitResultError(it, "Falha ao salvar produto.") }
            )
        }
    }

    fun deleteProduct(product: ProductEntity) {
        val ready = activeReadyState() ?: return
        viewModelScope.launch {
            repository.deleteProduct(
                household = ready.household,
                productId = product.id,
                existingAliases = ready.aliases,
                existingMovements = ready.movements
            ).fold(
                onSuccess = { _snackbarMessages.tryEmit("Produto ${product.name} excluído") },
                onFailure = { emitResultError(it, "Falha ao excluir produto.") }
            )
        }
    }

    fun registerMovement(
        productId: Long,
        type: MovementType,
        amountOrBalance: Double,
        unitPriceOverride: Double?,
        reason: String
    ) {
        val ready = activeReadyState() ?: return
        val product = ready.products.firstOrNull { it.id == productId } ?: return
        viewModelScope.launch {
            repository.registerMovement(
                household = ready.household,
                product = product,
                type = type,
                amountOrTargetBalance = amountOrBalance,
                unitPriceOverride = unitPriceOverride,
                reason = reason,
                authorName = userName
            ).fold(
                onSuccess = { _snackbarMessages.tryEmit("${type.label} registrada com auditoria") },
                onFailure = { emitResultError(it, "Falha ao registrar movimentação.") }
            )
        }
    }

    // ---- MODO MERCADO (FRENTE DE CAIXA / POS) ----

    fun updatePosStoreName(name: String) {
        _posState.value = _posState.value.copy(storeName = name)
    }

    fun updatePosNotes(notes: String) {
        _posState.value = _posState.value.copy(notes = notes)
    }

    fun updatePosSearchQuery(query: String) {
        _posState.value = _posState.value.copy(searchQuery = query)
    }

    fun togglePosSaveToStock(enabled: Boolean) {
        _posState.value = _posState.value.copy(saveToStock = enabled)
    }

    fun dismissUnmatchedCode() {
        _posState.value = _posState.value.copy(unmatchedScannedCode = null)
    }

    fun onBarcodeScannedInMarket(rawCode: String) {
        val cleanCode = rawCode.trim()
        if (cleanCode.isBlank()) return

        val state = uiState.value
        val matchedProduct = ProductMatcher.findByCode(state.products, state.aliases, cleanCode)
        if (matchedProduct != null) {
            addProductToPosCart(product = matchedProduct, scannedCodeUsed = cleanCode)
            _snackbarMessages.tryEmit("${matchedProduct.name} (${matchedProduct.uniqueCode}) bipado!")
        } else {
            _posState.value = _posState.value.copy(unmatchedScannedCode = cleanCode)
        }
    }

    fun addProductToPosCart(product: ProductEntity, scannedCodeUsed: String? = null) {
        upsertProductIntoCart(
            product = product,
            scannedCodeUsed = scannedCodeUsed,
            quantityToAdd = 1.0,
            unitPriceOverride = null
        )
    }

    fun linkScannedCodeToExistingProduct(product: ProductEntity, scannedCode: String) {
        val ready = activeReadyState() ?: return
        val marketName = _posState.value.storeName
        viewModelScope.launch {
            repository.linkAliasToProduct(
                household = ready.household,
                product = product,
                externalCode = scannedCode,
                marketName = marketName,
                existingAliases = ready.aliases
            ).fold(
                onSuccess = {
                    addProductToPosCart(product, scannedCodeUsed = scannedCode)
                    _snackbarMessages.tryEmit("Código $scannedCode vinculado a ${product.name} (${product.uniqueCode})!")
                },
                onFailure = { emitResultError(it, "Falha ao vincular código.") }
            )
        }
    }

    fun resolveOrAddNewItemToPosCart(
        scannedCode: String,
        name: String,
        unit: String,
        unitPrice: Double,
        quantity: Double,
        categoryId: Long?
    ) {
        val ready = activeReadyState() ?: return
        val marketName = _posState.value.storeName
        val saveToStock = _posState.value.saveToStock
        viewModelScope.launch {
            if (saveToStock) {
                repository.resolveOrCreateCanonicalProduct(
                    household = ready.household,
                    existingProducts = ready.products,
                    existingAliases = ready.aliases,
                    scannedCode = scannedCode,
                    name = name,
                    unit = unit,
                    unitPrice = unitPrice,
                    marketName = marketName,
                    categoryId = categoryId
                ).fold(
                    onSuccess = { resolution ->
                        val prod = resolution.product
                        upsertProductIntoCart(
                            product = prod,
                            scannedCodeUsed = scannedCode.ifBlank { prod.code },
                            quantityToAdd = quantity,
                            unitPriceOverride = unitPrice.takeIf { it > 0.0 }
                        )
                        val msg = when (resolution.matchedBy) {
                            "name" -> "Nome já existente! Vinculado ao código único ${prod.uniqueCode} sem duplicar."
                            "code" -> "Item localizado (${prod.uniqueCode}) e adicionado ao carrinho."
                            else -> "Novo produto criado com código único ${prod.uniqueCode}."
                        }
                        _snackbarMessages.tryEmit(msg)
                    },
                    onFailure = { emitResultError(it, "Falha ao salvar novo item.") }
                )
            } else {
                val existingByName = ProductMatcher.findByNormalizedName(uiState.value.products, name)
                if (existingByName != null) {
                    upsertProductIntoCart(
                        product = existingByName,
                        scannedCodeUsed = scannedCode.ifBlank { existingByName.code },
                        quantityToAdd = quantity,
                        unitPriceOverride = unitPrice.takeIf { it > 0.0 }
                    )
                } else {
                    val current = _posState.value
                    val updatedList = current.cartItems.toMutableList()
                    updatedList.add(
                        0,
                        PosCartItem(
                            key = "draft_${System.currentTimeMillis()}",
                            canonicalProductId = null,
                            uniqueCode = "AVULSO",
                            scannedCode = scannedCode,
                            name = name.trim(),
                            unit = unit.ifBlank { "un" },
                            quantity = quantity,
                            unitPrice = unitPrice,
                            previousPrice = null,
                            categoryId = categoryId
                        )
                    )
                    _posState.value = current.copy(
                        cartItems = updatedList,
                        searchQuery = "",
                        unmatchedScannedCode = null
                    )
                }
                _snackbarMessages.tryEmit("Item adicionado ao carrinho (modo somente gasto).")
            }
        }
    }

    private fun upsertProductIntoCart(
        product: ProductEntity,
        scannedCodeUsed: String?,
        quantityToAdd: Double,
        unitPriceOverride: Double?
    ) {
        val current = _posState.value
        val updated = current.cartItems.toMutableList()
        val existingIndex = updated.indexOfFirst { it.canonicalProductId == product.id }
        if (existingIndex >= 0) {
            val prev = updated[existingIndex]
            updated[existingIndex] = prev.copy(
                quantity = prev.quantity + quantityToAdd,
                unitPrice = unitPriceOverride ?: prev.unitPrice
            )
        } else {
            updated.add(
                0,
                PosCartItem(
                    key = "prod_${product.id}",
                    canonicalProductId = product.id,
                    uniqueCode = product.uniqueCode,
                    scannedCode = scannedCodeUsed ?: product.code,
                    name = product.name,
                    unit = product.unit,
                    quantity = quantityToAdd,
                    unitPrice = unitPriceOverride ?: product.unitPrice,
                    previousPrice = product.lastPaidPrice ?: product.unitPrice.takeIf { it > 0.0 },
                    categoryId = product.categoryId
                )
            )
        }
        _posState.value = current.copy(
            cartItems = updated,
            searchQuery = "",
            unmatchedScannedCode = null
        )
    }

    fun loadLowStockIntoPosCart() {
        val lowStock = uiState.value.lowStockProducts
        if (lowStock.isEmpty()) {
            _snackbarMessages.tryEmit("Nenhum item com estoque baixo no momento.")
            return
        }
        val current = _posState.value
        val updated = current.cartItems.toMutableList()
        var added = 0
        lowStock.forEach { prod ->
            if (updated.none { it.canonicalProductId == prod.id }) {
                val suggestedQty = max(1.0, ceil((prod.minStock - prod.quantity).coerceAtLeast(1.0)))
                updated.add(
                    PosCartItem(
                        key = "prod_${prod.id}",
                        canonicalProductId = prod.id,
                        uniqueCode = prod.uniqueCode,
                        scannedCode = prod.code,
                        name = prod.name,
                        unit = prod.unit,
                        quantity = suggestedQty,
                        unitPrice = prod.unitPrice,
                        previousPrice = prod.lastPaidPrice ?: prod.unitPrice.takeIf { it > 0.0 },
                        categoryId = prod.categoryId
                    )
                )
                added++
            }
        }
        _posState.value = current.copy(cartItems = updated)
        if (added > 0) {
            _snackbarMessages.tryEmit("$added item(ns) com estoque baixo adicionados ao carrinho.")
        } else {
            _snackbarMessages.tryEmit("Os itens com estoque baixo já estão no carrinho.")
        }
    }

    fun updatePosItemQuantity(key: String, newQty: Double) {
        val current = _posState.value
        _posState.value = current.copy(
            cartItems = current.cartItems.map {
                if (it.key == key) it.copy(quantity = newQty.coerceAtLeast(0.1)) else it
            }
        )
    }

    fun updatePosItemPrice(key: String, newPrice: Double) {
        val current = _posState.value
        _posState.value = current.copy(
            cartItems = current.cartItems.map {
                if (it.key == key) it.copy(unitPrice = newPrice.coerceAtLeast(0.0)) else it
            }
        )
    }

    fun removePosItem(key: String) {
        val current = _posState.value
        _posState.value = current.copy(cartItems = current.cartItems.filterNot { it.key == key })
    }

    fun finalizePosCheckout() {
        val ready = activeReadyState() ?: return
        val current = _posState.value
        if (current.cartItems.isEmpty()) return
        viewModelScope.launch {
            repository.finalizeMarketPosCheckout(
                household = ready.household,
                existingProducts = ready.products,
                existingAliases = ready.aliases,
                storeName = current.storeName,
                notes = current.notes,
                entries = current.cartItems.map { item ->
                    PosCheckoutEntry(
                        canonicalProductId = item.canonicalProductId,
                        scannedCode = item.scannedCode,
                        name = item.name,
                        unit = item.unit,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice,
                        categoryId = item.categoryId
                    )
                },
                saveToStock = current.saveToStock,
                authorName = userName
            ).fold(
                onSuccess = {
                    _posState.value = PosMarketState(saveToStock = current.saveToStock)
                    _snackbarMessages.tryEmit(
                        if (current.saveToStock) {
                            "Compra finalizada! Itens salvos na nuvem e histórico financeiro atualizado."
                        } else {
                            "Compra registrada nas finanças sem alterar o saldo do estoque."
                        }
                    )
                },
                onFailure = { emitResultError(it, "Falha ao finalizar compra.") }
            )
        }
    }

    // ---- CATEGORIAS E MORADORES ----

    fun saveCategory(category: CategoryEntity) {
        val ready = activeReadyState() ?: return
        viewModelScope.launch {
            repository.saveCategory(ready.household, category).fold(
                onSuccess = {
                    _snackbarMessages.tryEmit(if (category.id == 0L) "Categoria criada" else "Categoria atualizada")
                },
                onFailure = { emitResultError(it, "Falha ao salvar categoria.") }
            )
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        val ready = activeReadyState() ?: return
        viewModelScope.launch {
            repository.deleteCategory(ready.household, category.id).fold(
                onSuccess = { _snackbarMessages.tryEmit("Categoria ${category.name} removida") },
                onFailure = { emitResultError(it, "Falha ao remover categoria.") }
            )
        }
    }

    fun addUser(name: String, email: String, role: String, @Suppress("UNUSED_PARAMETER") password: String = "") {
        val ready = activeReadyState() ?: return
        viewModelScope.launch {
            repository.addOperatorMember(
                household = ready.household,
                name = name,
                email = email,
                role = role
            ).fold(
                onSuccess = { _snackbarMessages.tryEmit("Operador ${name.trim()} adicionado") },
                onFailure = { emitResultError(it, "Falha ao adicionar operador.") }
            )
        }
    }

    fun deleteUser(user: HouseholdUserEntity) {
        val ready = activeReadyState() ?: return
        viewModelScope.launch {
            repository.deleteMember(ready.household, user.id).fold(
                onSuccess = { _snackbarMessages.tryEmit("Operador removido") },
                onFailure = { emitResultError(it, "Falha ao remover operador.") }
            )
        }
    }

    fun loadSampleCatalog() {
        val ready = activeReadyState() ?: return
        viewModelScope.launch {
            repository.loadSampleCatalog(ready.household, userName).fold(
                onSuccess = {
                    _snackbarMessages.tryEmit("Catálogo de exemplo carregado na nuvem com códigos AST")
                },
                onFailure = { emitResultError(it, "Falha ao carregar catálogo de exemplo.") }
            )
        }
    }

    fun exportBackupToUri(context: Context, uri: Uri) {
        val state = uiState.value
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val json = repository.exportBackupJson(
                        categories = state.categories,
                        products = state.products,
                        aliases = state.aliases,
                        movements = state.movements,
                        checkouts = state.checkouts
                    )
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Não foi possível abrir o destino selecionado.")
                }
            }
            result.fold(
                onSuccess = { _snackbarMessages.tryEmit("Backup exportado com sucesso!") },
                onFailure = { err ->
                    _snackbarMessages.tryEmit("Erro ao exportar backup: ${err.localizedMessage ?: "falha"}")
                }
            )
        }
    }

    fun importBackupFromUri(context: Context, uri: Uri) {
        val ready = activeReadyState() ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val raw = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        ?: error("Não foi possível ler o arquivo selecionado.")
                    repository.importBackupJson(ready.household, raw).getOrThrow()
                }
            }
            result.fold(
                onSuccess = { summary ->
                    _snackbarMessages.tryEmit(
                        "Backup restaurado na nuvem: ${summary.productsRestored} produto(s), ${summary.categoriesRestored} categoria(s)."
                    )
                },
                onFailure = { err ->
                    _snackbarMessages.tryEmit("Falha ao restaurar backup: ${err.localizedMessage ?: "arquivo inválido"}")
                }
            )
        }
    }

    fun buildDiagnosticReport(): String {
        val state = uiState.value
        return buildString {
            appendLine("AstroGestão — Relatório de Diagnóstico / Feedback")
            appendLine("Versão: ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")
            appendLine("Dispositivo: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("Espaço na nuvem: ${state.householdName} (${state.memberCount} membro(s))")
            appendLine("Registros sincronizados: ${state.products.size} produtos, ${state.categories.size} categorias, ${state.movements.size} movimentações, ${state.checkouts.size} compras")
            appendLine()
            appendLine("Descreva aqui o que aconteceu ou sua sugestão:")
        }
    }

    // ---- IMPORTADOR DE NFC-E / CUPONS FISCAIS ----

    fun selectNfceTab(tabIndex: Int) {
        _nfceState.value = _nfceState.value.copy(activeTab = tabIndex)
    }

    fun selectRegionalPortal(portal: RegionalPortal) {
        _nfceState.value = _nfceState.value.copy(
            selectedPortal = portal,
            activePortalUrl = portal.url,
            showWebPortal = true,
            statusMessage = "Portal ${portal.label} carregado abaixo."
        )
    }

    fun updateNfceInput(input: String) {
        val key = SefazPortals.extractAccessKey(input)
        val detectedPortal = if (input.isNotBlank()) SefazPortals.detectForInput(input) else _nfceState.value.selectedPortal
        _nfceState.value = _nfceState.value.copy(
            inputQuery = input,
            detectedKey = key,
            selectedPortal = detectedPortal
        )
    }

    fun updateRawReceiptTextInput(text: String) {
        _nfceState.value = _nfceState.value.copy(rawReceiptTextInput = text)
    }

    fun parsePastedReceiptText() {
        val raw = _nfceState.value.rawReceiptTextInput.trim()
        if (raw.isBlank()) return
        val result = NfceParser.parseRawReceiptText(raw)
        val defaultCat = uiState.value.categories.firstOrNull()?.id
        _nfceState.value = _nfceState.value.copy(
            storeName = result.storeName ?: _nfceState.value.storeName,
            detectedKey = result.accessKey ?: _nfceState.value.detectedKey,
            parsedItems = result.items.map { it.copy(categoryId = defaultCat) },
            statusMessage = result.message ?: "Não foi possível identificar itens neste texto. Tente separar nome, quantidade e preço (ex: Arroz 5kg  1 un  24,90)."
        )
    }

    fun processReceiptImage(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            _nfceState.value = _nfceState.value.copy(
                isFetching = true,
                statusMessage = "Analisando foto: procurando QR Code da SEFAZ e fazendo leitura ótica (OCR)..."
            )

            val scanResult = runCatching {
                withContext(Dispatchers.IO) { ReceiptImageProcessor.scanReceiptImage(context, imageUri) }
            }.getOrNull()

            if (scanResult == null) {
                _nfceState.value = _nfceState.value.copy(
                    isFetching = false,
                    statusMessage = "Não foi possível abrir a imagem selecionada. Tente outra foto mais nítida."
                )
                return@launch
            }

            val defaultCat = uiState.value.categories.firstOrNull()?.id
            val ocrResult = if (scanResult.ocrText.isNotBlank()) {
                NfceParser.parseOcrReceiptText(scanResult.ocrText)
            } else null

            val qrRaw = scanResult.qrOrBarcodeRaw
            if (!qrRaw.isNullOrBlank()) {
                val portal = SefazPortals.detectForInput(qrRaw)
                val key = SefazPortals.extractAccessKey(qrRaw) ?: ocrResult?.accessKey

                val safeQrUrl = SefazPortals.sanitizeSefazUrl(qrRaw)
                if (safeQrUrl != null) {
                    val sefazResult = withContext(Dispatchers.IO) { NfceParser.fetchAndParseUrl(safeQrUrl) }
                    when {
                        sefazResult.items.isNotEmpty() -> {
                            _nfceState.value = _nfceState.value.copy(
                                isFetching = false,
                                inputQuery = safeQrUrl,
                                detectedKey = sefazResult.accessKey ?: key,
                                selectedPortal = portal,
                                storeName = sefazResult.storeName ?: ocrResult?.storeName,
                                parsedItems = sefazResult.items.map { it.copy(categoryId = defaultCat) },
                                statusMessage = "QR Code da SEFAZ (${portal.ufSigla}) lido com sucesso! ${sefazResult.items.size} item(ns) extraído(s)."
                            )
                        }
                        ocrResult != null && ocrResult.items.isNotEmpty() -> {
                            _nfceState.value = _nfceState.value.copy(
                                isFetching = false,
                                inputQuery = safeQrUrl,
                                detectedKey = key,
                                selectedPortal = portal,
                                activePortalUrl = safeQrUrl,
                                storeName = ocrResult.storeName,
                                parsedItems = ocrResult.items.map { it.copy(categoryId = defaultCat) },
                                statusMessage = "QR Code (${portal.ufSigla}) detectado e ${ocrResult.items.size} item(ns) extraído(s) via OCR! Confira abaixo ou abra o link oficial da SEFAZ se faltar algum item."
                            )
                        }
                        else -> {
                            _nfceState.value = _nfceState.value.copy(
                                isFetching = false,
                                activeTab = 1,
                                inputQuery = safeQrUrl,
                                detectedKey = key,
                                selectedPortal = portal,
                                activePortalUrl = safeQrUrl,
                                showWebPortal = true,
                                statusMessage = "QR Code (${portal.ufSigla}) detectado! Abrindo a nota no portal SEFAZ abaixo."
                            )
                        }
                    }
                    return@launch
                } else if (qrRaw.startsWith("http://", ignoreCase = true) || qrRaw.startsWith("https://", ignoreCase = true)) {
                    _nfceState.value = _nfceState.value.copy(
                        isFetching = false,
                        statusMessage = "QR Code ignorado por segurança: apenas links oficiais da SEFAZ (.gov.br) são permitidos."
                    )
                    return@launch
                } else if (key != null) {
                    if (ocrResult != null && ocrResult.items.isNotEmpty()) {
                        _nfceState.value = _nfceState.value.copy(
                            isFetching = false,
                            inputQuery = key,
                            detectedKey = key,
                            selectedPortal = portal,
                            activePortalUrl = portal.url,
                            storeName = ocrResult.storeName,
                            parsedItems = ocrResult.items.map { it.copy(categoryId = defaultCat) },
                            statusMessage = "Chave (${portal.ufSigla}) e ${ocrResult.items.size} item(ns) extraídos via OCR! Confira abaixo."
                        )
                    } else {
                        _nfceState.value = _nfceState.value.copy(
                            isFetching = false,
                            activeTab = 1,
                            inputQuery = key,
                            detectedKey = key,
                            selectedPortal = portal,
                            activePortalUrl = portal.url,
                            showWebPortal = true,
                            statusMessage = "Chave (${portal.ufSigla}) detectada! Abrindo o portal da SEFAZ correspondente."
                        )
                    }
                    return@launch
                }
            }

            if (ocrResult != null && ocrResult.items.isNotEmpty()) {
                _nfceState.value = _nfceState.value.copy(
                    isFetching = false,
                    storeName = ocrResult.storeName,
                    detectedKey = ocrResult.accessKey ?: _nfceState.value.detectedKey,
                    selectedPortal = ocrResult.accessKey?.let { SefazPortals.detectForInput(it) } ?: _nfceState.value.selectedPortal,
                    parsedItems = ocrResult.items.map { it.copy(categoryId = defaultCat) },
                    statusMessage = "Leitura ótica (OCR) concluída! ${ocrResult.items.size} item(ns) identificado(s) no cupom."
                )
            } else {
                _nfceState.value = _nfceState.value.copy(
                    isFetching = false,
                    rawReceiptTextInput = scanResult.ocrText,
                    statusMessage = if (scanResult.ocrText.isNotBlank()) {
                        "Lemos o texto da foto, mas não identificamos linhas de itens com preço. Tente aproximar mais a câmera ou edite na aba \"Colar Texto\"."
                    } else {
                        "Não foi possível detectar QR Code nem texto legível na foto. Tente outra foto com boa iluminação."
                    }
                )
            }
        }
    }

    fun openRegionalPortal() {
        val current = _nfceState.value
        val trimmed = current.inputQuery.trim()
        val targetUrl = SefazPortals.sanitizeSefazUrl(trimmed) ?: current.selectedPortal.url
        _nfceState.value = current.copy(
            activePortalUrl = targetUrl,
            showWebPortal = true,
            statusMessage = if (current.detectedKey != null) {
                "Portal ${current.selectedPortal.ufSigla} aberto. Chave detectada (${current.detectedKey.take(8)}…): toque em \"Preencher Chave\", resolva o CAPTCHA e os itens serão capturados."
            } else {
                "Consulte a nota no portal ${current.selectedPortal.ufSigla} abaixo e toque em \"Extrair Itens\"."
            }
        )
    }

    fun dismissWebPortal() {
        _nfceState.value = _nfceState.value.copy(showWebPortal = false)
    }

    fun consultNfceInput() {
        val raw = _nfceState.value.inputQuery.trim()
        if (raw.isBlank()) {
            openRegionalPortal()
            return
        }

        if (!raw.startsWith("http", ignoreCase = true) && SefazPortals.extractAccessKey(raw) == null) {
            val textResult = NfceParser.parseRawReceiptText(raw)
            if (textResult.items.isNotEmpty()) {
                val defaultCat = uiState.value.categories.firstOrNull()?.id
                _nfceState.value = _nfceState.value.copy(
                    parsedItems = textResult.items.map { it.copy(categoryId = defaultCat) },
                    statusMessage = textResult.message
                )
                return
            }
        }

        if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
            val safeUrl = SefazPortals.sanitizeSefazUrl(raw)
            if (safeUrl == null) {
                _nfceState.value = _nfceState.value.copy(
                    isFetching = false,
                    showWebPortal = false,
                    statusMessage = "URL bloqueada por segurança: informe um link oficial da SEFAZ (.gov.br) com HTTPS."
                )
                return
            }
            viewModelScope.launch {
                val portal = SefazPortals.detectForInput(safeUrl)
                _nfceState.value = _nfceState.value.copy(
                    isFetching = true,
                    selectedPortal = portal,
                    statusMessage = "Consultando SEFAZ (${portal.ufSigla})..."
                )
                val result = withContext(Dispatchers.IO) { NfceParser.fetchAndParseUrl(safeUrl) }
                val defaultCat = uiState.value.categories.firstOrNull()?.id
                _nfceState.value = _nfceState.value.copy(
                    isFetching = false,
                    storeName = result.storeName,
                    detectedKey = result.accessKey ?: _nfceState.value.detectedKey,
                    parsedItems = result.items.map { it.copy(categoryId = defaultCat) },
                    showWebPortal = result.requiresWebPortal || result.items.isEmpty(),
                    activePortalUrl = result.portalUrl?.let { SefazPortals.sanitizeSefazUrl(it) } ?: portal.url,
                    statusMessage = result.message
                )
            }
        } else {
            val portal = SefazPortals.detectForInput(raw)
            _nfceState.value = _nfceState.value.copy(
                selectedPortal = portal,
                activePortalUrl = portal.url,
                showWebPortal = true,
                statusMessage = "Estado detectado automaticamente: ${portal.label}. Toque em \"Preencher Chave\", resolva o CAPTCHA e clique em \"Extrair Itens da Página\"."
            )
        }
    }

    fun parseNfceXmlContent(xmlContent: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) { NfceParser.parseNfceXml(xmlContent) }
            val defaultCat = uiState.value.categories.firstOrNull()?.id
            _nfceState.value = _nfceState.value.copy(
                storeName = result.storeName,
                detectedKey = result.accessKey ?: _nfceState.value.detectedKey,
                parsedItems = result.items.map { it.copy(categoryId = defaultCat) },
                statusMessage = result.message
            )
        }
    }

    fun applyWebViewExtractedJson(rawJson: String, isAutoScan: Boolean = false) {
        val result = NfceParser.parseWebViewJson(rawJson)
        val defaultCat = uiState.value.categories.firstOrNull()?.id
        if (result.items.isNotEmpty()) {
            _nfceState.value = _nfceState.value.copy(
                storeName = result.storeName ?: _nfceState.value.storeName,
                parsedItems = result.items.map { it.copy(categoryId = defaultCat) },
                showWebPortal = false,
                statusMessage = result.message
            )
        } else if (!isAutoScan) {
            _nfceState.value = _nfceState.value.copy(statusMessage = result.message)
        }
    }

    fun toggleParsedItem(index: Int) {
        val current = _nfceState.value.parsedItems.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(selected = !current[index].selected)
            _nfceState.value = _nfceState.value.copy(parsedItems = current)
        }
    }

    fun updateParsedItemCategory(index: Int, categoryId: Long) {
        val current = _nfceState.value.parsedItems.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(categoryId = categoryId)
            _nfceState.value = _nfceState.value.copy(parsedItems = current)
        }
    }

    fun confirmNfceImport(onSuccess: () -> Unit) {
        val ready = activeReadyState() ?: return
        val state = _nfceState.value
        val defaultCat = uiState.value.categories.firstOrNull()?.id
        viewModelScope.launch {
            repository.importNfceItems(
                household = ready.household,
                existingProducts = ready.products,
                existingAliases = ready.aliases,
                items = state.parsedItems,
                storeName = state.storeName,
                defaultCategoryId = defaultCat,
                authorName = userName
            ).fold(
                onSuccess = { count ->
                    if (count > 0) {
                        _snackbarMessages.tryEmit("$count item(ns) importado(s) da NFC-e na nuvem!")
                        _nfceState.value = NfceImportState()
                        onSuccess()
                    } else {
                        _snackbarMessages.tryEmit("Selecione ao menos um item para importar.")
                    }
                },
                onFailure = { emitResultError(it, "Falha ao importar itens do cupom.") }
            )
        }
    }

    class Factory(
        private val repository: FirestoreAstroRepository,
        private val prefs: ThemePreferencesRepository,
        private val localDb: AppDatabase,
        private val currentUserId: String,
        private val userEmail: String,
        private val userName: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AstroViewModel(
                repository = repository,
                prefs = prefs,
                localDb = localDb,
                currentUserId = currentUserId,
                userEmail = userEmail,
                userName = userName
            ) as T
        }
    }
}
