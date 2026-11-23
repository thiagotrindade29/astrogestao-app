package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CategoryEntity
import com.example.data.local.HouseholdUserEntity
import com.example.data.local.MarketCheckoutEntity
import com.example.data.local.MovementType
import com.example.data.local.ProductAliasEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StockMovementEntity
import com.example.data.nfce.NfceParser
import com.example.data.nfce.ParsedNfceItem
import com.example.data.nfce.ReceiptImageProcessor
import com.example.data.nfce.RegionalPortal
import com.example.data.repository.AstroRepository
import com.example.data.repository.AuthOutcome
import com.example.data.repository.PosCheckoutEntry
import com.example.data.repository.ThemePreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

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
    val selectedPortal: RegionalPortal = NfceParser.regionalPortals.first(),
    val inputQuery: String = "",
    val rawReceiptTextInput: String = "",
    val detectedKey: String? = null,
    val activePortalUrl: String = NfceParser.regionalPortals.first().url,
    val showWebPortal: Boolean = false,
    val isFetching: Boolean = false,
    val storeName: String? = null,
    val parsedItems: List<ParsedNfceItem> = emptyList(),
    val statusMessage: String? = null
)

data class AstroUiState(
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean = false,
    val userEmail: String = "thiagotrindade628@gmail.com",
    val userName: String = "Thiago Trindade",
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
            val now = System.currentTimeMillis()
            val limit = now + 30L * 86_400_000L
            return products
                .filter { it.expiryDateMillis != null && it.expiryDateMillis <= limit }
                .sortedBy { it.expiryDateMillis }
        }

    val filteredProducts: List<ProductEntity>
        get() = products.filter { product ->
            val q = productSearchQuery.trim().lowercase()
            val productAliases = aliasesByProductId[product.id].orEmpty()
            val matchesQuery = q.isEmpty() ||
                product.name.lowercase().contains(q) ||
                product.code.lowercase().contains(q) ||
                product.uniqueCode.lowercase().contains(q) ||
                productAliases.any { it.externalCode.lowercase().contains(q) }
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

private data class FilterState(
    val productSearchQuery: String = "",
    val selectedCategoryId: Long? = null,
    val onlyLowStock: Boolean = false,
    val movementFilterType: MovementType? = null
)

class AstroViewModel(
    private val repository: AstroRepository,
    private val prefs: ThemePreferencesRepository
) : ViewModel() {

    private val filterState = MutableStateFlow(FilterState())

    private val _nfceState = MutableStateFlow(NfceImportState())
    val nfceState: StateFlow<NfceImportState> = _nfceState.asStateFlow()

    private val _posState = MutableStateFlow(PosMarketState())
    val posState: StateFlow<PosMarketState> = _posState.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 6)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultsSeeded()
        }
    }

    val uiState: StateFlow<AstroUiState> = combine(
        prefs.isLoggedIn,
        prefs.userEmail,
        prefs.userName,
        prefs.isDarkMode,
        repository.categories,
        repository.products,
        repository.aliases,
        repository.movements,
        repository.checkouts,
        repository.users,
        filterState
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val filters = values[10] as FilterState
        @Suppress("UNCHECKED_CAST")
        AstroUiState(
            isLoading = false,
            isLoggedIn = values[0] as Boolean,
            userEmail = values[1] as String,
            userName = values[2] as String,
            isDarkMode = values[3] as Boolean,
            categories = values[4] as List<CategoryEntity>,
            products = values[5] as List<ProductEntity>,
            aliases = values[6] as List<ProductAliasEntity>,
            movements = values[7] as List<StockMovementEntity>,
            checkouts = values[8] as List<MarketCheckoutEntity>,
            users = values[9] as List<HouseholdUserEntity>,
            productSearchQuery = filters.productSearchQuery,
            selectedCategoryId = filters.selectedCategoryId,
            onlyLowStock = filters.onlyLowStock,
            movementFilterType = filters.movementFilterType
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AstroUiState(isLoading = true)
    )

    fun signInWithPassword(
        email: String,
        password: String,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            when (val outcome = repository.authenticateUser(email, password)) {
                is AuthOutcome.Success -> {
                    prefs.signIn(outcome.user.email, outcome.user.name)
                }
                AuthOutcome.UserNotFound -> {
                    onError("E-mail não cadastrado. Toque em Cadastre-se para criar sua conta.")
                }
                AuthOutcome.WrongPassword -> {
                    onError("Senha incorreta. Tente novamente.")
                }
            }
        }
    }

    fun signUp(
        name: String,
        email: String,
        password: String,
        role: String,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.registerUser(name = name, email = email, password = password, role = role)
            result.fold(
                onSuccess = { user ->
                    prefs.signIn(user.email, user.name)
                    _snackbarMessages.tryEmit("Conta criada! Bem-vindo(a), ${user.name}.")
                },
                onFailure = {
                    onError("Já existe um usuário cadastrado com este e-mail.")
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            prefs.signOut()
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { prefs.setDarkMode(enabled) }
    }

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
        viewModelScope.launch {
            repository.saveProduct(product, uiState.value.userName)
            _snackbarMessages.tryEmit(
                if (product.id == 0L) "Produto cadastrado no estoque" else "Produto atualizado"
            )
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product.id)
            _snackbarMessages.tryEmit("Produto ${product.name} excluído")
        }
    }

    fun registerMovement(
        productId: Long,
        type: MovementType,
        amountOrBalance: Double,
        unitPriceOverride: Double?,
        reason: String
    ) {
        viewModelScope.launch {
            repository.registerMovement(
                productId = productId,
                type = type,
                amountOrTargetBalance = amountOrBalance,
                unitPriceOverride = unitPriceOverride,
                reason = reason,
                authorName = uiState.value.userName
            )
            _snackbarMessages.tryEmit("${type.label} registrada com auditoria")
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
        val matchedProduct = state.products.firstOrNull {
            it.uniqueCode.equals(cleanCode, ignoreCase = true) ||
                it.code.equals(cleanCode, ignoreCase = true)
        } ?: state.aliases.firstOrNull {
            it.externalCode.equals(cleanCode, ignoreCase = true)
        }?.let { alias ->
            state.products.firstOrNull { it.id == alias.productId }
        }

        if (matchedProduct != null) {
            addProductToPosCart(product = matchedProduct, scannedCodeUsed = cleanCode)
            _snackbarMessages.tryEmit("${matchedProduct.name} (${matchedProduct.uniqueCode}) bipado!")
        } else {
            _posState.value = _posState.value.copy(unmatchedScannedCode = cleanCode)
        }
    }

    fun addProductToPosCart(product: ProductEntity, scannedCodeUsed: String? = null) {
        val current = _posState.value
        val existingIndex = current.cartItems.indexOfFirst { it.canonicalProductId == product.id }
        val updatedItems = current.cartItems.toMutableList()
        if (existingIndex >= 0) {
            val item = updatedItems[existingIndex]
            updatedItems[existingIndex] = item.copy(quantity = item.quantity + 1.0)
        } else {
            val refPrice = product.lastPaidPrice ?: product.unitPrice.takeIf { it > 0.0 }
            updatedItems.add(
                0,
                PosCartItem(
                    key = "prod_${product.id}",
                    canonicalProductId = product.id,
                    uniqueCode = product.uniqueCode,
                    scannedCode = scannedCodeUsed ?: product.code,
                    name = product.name,
                    unit = product.unit,
                    quantity = 1.0,
                    unitPrice = product.unitPrice,
                    previousPrice = refPrice,
                    categoryId = product.categoryId
                )
            )
        }
        _posState.value = current.copy(
            cartItems = updatedItems,
            searchQuery = "",
            unmatchedScannedCode = null
        )
    }

    fun linkScannedCodeToExistingProduct(product: ProductEntity, scannedCode: String) {
        val marketName = _posState.value.storeName
        viewModelScope.launch {
            repository.linkAliasToProduct(product.id, scannedCode, marketName)
            addProductToPosCart(product, scannedCodeUsed = scannedCode)
            _snackbarMessages.tryEmit("Código $scannedCode vinculado a ${product.name} (${product.uniqueCode})!")
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
        val marketName = _posState.value.storeName
        val saveToStock = _posState.value.saveToStock
        viewModelScope.launch {
            if (saveToStock) {
                val resolution = repository.resolveOrCreateCanonicalProduct(
                    scannedCode = scannedCode,
                    name = name,
                    unit = unit,
                    unitPrice = unitPrice,
                    marketName = marketName,
                    categoryId = categoryId
                )
                val prod = resolution.product
                val current = _posState.value
                val existingIndex = current.cartItems.indexOfFirst { it.canonicalProductId == prod.id }
                val updatedList = current.cartItems.toMutableList()
                if (existingIndex >= 0) {
                    val prevItem = updatedList[existingIndex]
                    updatedList[existingIndex] = prevItem.copy(
                        quantity = prevItem.quantity + quantity,
                        unitPrice = if (unitPrice > 0.0) unitPrice else prevItem.unitPrice
                    )
                } else {
                    updatedList.add(
                        0,
                        PosCartItem(
                            key = "prod_${prod.id}",
                            canonicalProductId = prod.id,
                            uniqueCode = prod.uniqueCode,
                            scannedCode = scannedCode.ifBlank { prod.code },
                            name = prod.name,
                            unit = prod.unit,
                            quantity = quantity,
                            unitPrice = unitPrice,
                            previousPrice = prod.lastPaidPrice ?: prod.unitPrice.takeIf { it > 0.0 },
                            categoryId = prod.categoryId
                        )
                    )
                }
                _posState.value = current.copy(
                    cartItems = updatedList,
                    searchQuery = "",
                    unmatchedScannedCode = null
                )
                val msg = when (resolution.matchedBy) {
                    "name" -> "Nome já existente! Vinculado ao código único ${prod.uniqueCode} sem duplicar."
                    "code" -> "Item localizado (${prod.uniqueCode}) e adicionado ao carrinho."
                    else -> "Novo produto criado com código único ${prod.uniqueCode}."
                }
                _snackbarMessages.tryEmit(msg)
            } else {
                // Even when not saving to stock, check if normalized name already exists in catalog
                val normalized = AstroRepository.normalizeProductName(name)
                val existingByName = uiState.value.products.firstOrNull {
                    AstroRepository.normalizeProductName(it.name) == normalized
                }
                val current = _posState.value
                val updatedList = current.cartItems.toMutableList()
                if (existingByName != null) {
                    val idx = updatedList.indexOfFirst { it.canonicalProductId == existingByName.id }
                    if (idx >= 0) {
                        val prevItem = updatedList[idx]
                        updatedList[idx] = prevItem.copy(quantity = prevItem.quantity + quantity)
                    } else {
                        updatedList.add(
                            0,
                            PosCartItem(
                                key = "prod_${existingByName.id}",
                                canonicalProductId = existingByName.id,
                                uniqueCode = existingByName.uniqueCode,
                                scannedCode = scannedCode.ifBlank { existingByName.code },
                                name = existingByName.name,
                                unit = existingByName.unit,
                                quantity = quantity,
                                unitPrice = if (unitPrice > 0.0) unitPrice else existingByName.unitPrice,
                                previousPrice = existingByName.lastPaidPrice ?: existingByName.unitPrice.takeIf { it > 0.0 },
                                categoryId = existingByName.categoryId
                            )
                        )
                    }
                } else {
                    val tempKey = "draft_${System.currentTimeMillis()}"
                    updatedList.add(
                        0,
                        PosCartItem(
                            key = tempKey,
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
                }
                _posState.value = current.copy(
                    cartItems = updatedList,
                    searchQuery = "",
                    unmatchedScannedCode = null
                )
                _snackbarMessages.tryEmit("Item adicionado ao carrinho (modo somente gasto).")
            }
        }
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
        val updated = current.cartItems.map {
            if (it.key == key) it.copy(quantity = newQty.coerceAtLeast(0.1)) else it
        }
        _posState.value = current.copy(cartItems = updated)
    }

    fun updatePosItemPrice(key: String, newPrice: Double) {
        val current = _posState.value
        val updated = current.cartItems.map {
            if (it.key == key) it.copy(unitPrice = newPrice.coerceAtLeast(0.0)) else it
        }
        _posState.value = current.copy(cartItems = updated)
    }

    fun removePosItem(key: String) {
        val current = _posState.value
        _posState.value = current.copy(cartItems = current.cartItems.filterNot { it.key == key })
    }

    fun finalizePosCheckout() {
        val current = _posState.value
        if (current.cartItems.isEmpty()) return
        viewModelScope.launch {
            repository.finalizeMarketPosCheckout(
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
                authorName = uiState.value.userName
            )
            _posState.value = PosMarketState(saveToStock = current.saveToStock)
            _snackbarMessages.tryEmit(
                if (current.saveToStock) {
                    "Compra finalizada! Itens salvos no estoque e histórico financeiro atualizado."
                } else {
                    "Compra registrada nas finanças sem alterar o saldo do estoque."
                }
            )
        }
    }

    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.saveCategory(category)
            _snackbarMessages.tryEmit(
                if (category.id == 0L) "Categoria criada" else "Categoria atualizada"
            )
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category.id)
            _snackbarMessages.tryEmit("Categoria ${category.name} removida")
        }
    }

    fun addUser(name: String, email: String, role: String) {
        viewModelScope.launch {
            repository.saveUser(HouseholdUserEntity(name = name, email = email, role = role))
            _snackbarMessages.tryEmit("Usuário $name adicionado")
        }
    }

    fun deleteUser(user: HouseholdUserEntity) {
        viewModelScope.launch {
            repository.deleteUser(user.id)
            _snackbarMessages.tryEmit("Usuário removido")
        }
    }

    fun loadSampleCatalog() {
        viewModelScope.launch {
            repository.loadSampleCatalog(uiState.value.userName)
            _snackbarMessages.tryEmit("Catálogo de exemplo carregado com códigos únicos AST")
        }
    }

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
        val key = NfceParser.extractAccessKey(input)
        val detectedPortal = if (input.isNotBlank()) NfceParser.detectPortalForInput(input) else _nfceState.value.selectedPortal
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
                withContext(Dispatchers.IO) {
                    ReceiptImageProcessor.scanReceiptImage(context, imageUri)
                }
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
            } else {
                null
            }

            val qrRaw = scanResult.qrOrBarcodeRaw
            if (!qrRaw.isNullOrBlank()) {
                val portal = NfceParser.detectPortalForInput(qrRaw)
                val key = NfceParser.extractAccessKey(qrRaw) ?: ocrResult?.accessKey

                if (qrRaw.startsWith("http://", ignoreCase = true) || qrRaw.startsWith("https://", ignoreCase = true)) {
                    val sefazResult = withContext(Dispatchers.IO) {
                        NfceParser.fetchAndParseUrl(qrRaw)
                    }
                    if (sefazResult.items.isNotEmpty()) {
                        _nfceState.value = _nfceState.value.copy(
                            isFetching = false,
                            inputQuery = qrRaw,
                            detectedKey = sefazResult.accessKey ?: key,
                            selectedPortal = portal,
                            storeName = sefazResult.storeName ?: ocrResult?.storeName,
                            parsedItems = sefazResult.items.map { it.copy(categoryId = defaultCat) },
                            statusMessage = "QR Code da SEFAZ (${portal.ufSigla}) lido com sucesso! ${sefazResult.items.size} item(ns) extraído(s)."
                        )
                        return@launch
                    } else if (ocrResult != null && ocrResult.items.isNotEmpty()) {
                        _nfceState.value = _nfceState.value.copy(
                            isFetching = false,
                            inputQuery = qrRaw,
                            detectedKey = key,
                            selectedPortal = portal,
                            activePortalUrl = qrRaw,
                            storeName = ocrResult.storeName,
                            parsedItems = ocrResult.items.map { it.copy(categoryId = defaultCat) },
                            statusMessage = "QR Code (${portal.ufSigla}) detectado e ${ocrResult.items.size} item(ns) extraído(s) via leitura ótica (OCR)! Confira abaixo."
                        )
                        return@launch
                    } else {
                        _nfceState.value = _nfceState.value.copy(
                            isFetching = false,
                            activeTab = 1,
                            inputQuery = qrRaw,
                            detectedKey = key,
                            selectedPortal = portal,
                            activePortalUrl = qrRaw,
                            showWebPortal = true,
                            statusMessage = "QR Code (${portal.ufSigla}) detectado! Abrindo a nota no portal SEFAZ abaixo."
                        )
                        return@launch
                    }
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
                        return@launch
                    }
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
                    return@launch
                }
            }

            // No QR code or direct URL — use OCR parsed items
            if (ocrResult != null && ocrResult.items.isNotEmpty()) {
                _nfceState.value = _nfceState.value.copy(
                    isFetching = false,
                    storeName = ocrResult.storeName,
                    detectedKey = ocrResult.accessKey ?: _nfceState.value.detectedKey,
                    selectedPortal = ocrResult.accessKey?.let { NfceParser.detectPortalForInput(it) } ?: _nfceState.value.selectedPortal,
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
        val targetUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            current.selectedPortal.url
        }
        _nfceState.value = current.copy(
            activePortalUrl = targetUrl,
            showWebPortal = true,
            statusMessage = if (current.detectedKey != null) {
                "Portal ${current.selectedPortal.ufSigla} aberto. Chave detectada (${current.detectedKey.take(8)}…): toque em \"Preencher Chave\", resolva o CAPTCHA e os itens serão capturados."
            } else {
                "Consulte a nota no portal ${current.selectedPortal.ufSigla} abaixo e toque em \"Extrair Itens da Página\"."
            }
        )
    }

    fun consultNfceInput() {
        val raw = _nfceState.value.inputQuery.trim()
        if (raw.isBlank()) {
            openRegionalPortal()
            return
        }

        if (!raw.startsWith("http", ignoreCase = true) && NfceParser.extractAccessKey(raw) == null) {
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
            viewModelScope.launch {
                val portal = NfceParser.detectPortalForInput(raw)
                _nfceState.value = _nfceState.value.copy(
                    isFetching = true,
                    selectedPortal = portal,
                    statusMessage = "Consultando SEFAZ (${portal.ufSigla})..."
                )
                val result = withContext(Dispatchers.IO) {
                    NfceParser.fetchAndParseUrl(raw)
                }
                val defaultCat = uiState.value.categories.firstOrNull()?.id
                _nfceState.value = _nfceState.value.copy(
                    isFetching = false,
                    storeName = result.storeName,
                    detectedKey = result.accessKey ?: _nfceState.value.detectedKey,
                    parsedItems = result.items.map { it.copy(categoryId = defaultCat) },
                    showWebPortal = result.requiresWebPortal || result.items.isEmpty(),
                    activePortalUrl = result.portalUrl ?: raw,
                    statusMessage = result.message
                )
            }
        } else {
            val portal = NfceParser.detectPortalForInput(raw)
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
            val result = withContext(Dispatchers.Default) {
                NfceParser.parseNfceXml(xmlContent)
            }
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
                statusMessage = result.message
            )
        } else if (!isAutoScan) {
            _nfceState.value = _nfceState.value.copy(
                statusMessage = result.message
            )
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
        val state = _nfceState.value
        val defaultCat = uiState.value.categories.firstOrNull()?.id
        viewModelScope.launch {
            val count = repository.importNfceItems(
                items = state.parsedItems,
                storeName = state.storeName,
                defaultCategoryId = defaultCat,
                authorName = uiState.value.userName
            )
            if (count > 0) {
                _snackbarMessages.tryEmit("$count item(ns) importado(s) da NFC-e com auditoria!")
                _nfceState.value = NfceImportState()
                onSuccess()
            } else {
                _snackbarMessages.tryEmit("Selecione ao menos um item para importar.")
            }
        }
    }

    fun resetNfceImport() {
        _nfceState.value = NfceImportState()
    }

    class Factory(
        private val repository: AstroRepository,
        private val prefs: ThemePreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AstroViewModel(repository, prefs) as T
        }
    }
}
