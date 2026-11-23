package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.AstroRepository
import com.example.data.model.CartItem
import com.example.data.model.CartTotals
import com.example.data.model.Category
import com.example.data.model.CategorySpending
import com.example.data.model.FinishedPurchaseSummary
import com.example.data.model.ParsedReceiptItem
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.PurchaseItem
import com.example.data.model.StockMovement
import com.example.util.daysUntilExpiration
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

enum class AppTab {
    DASHBOARD,
    PRODUCTS,
    MARKET,
    FINANCE,
    MOVEMENTS,
    CATEGORIES
}

enum class PriceVariationFilter {
    ALL,
    UP,
    EQUAL,
    DOWN
}

sealed interface DataUiState {
    data object Loading : DataUiState
    data class Success(
        val categories: List<Category>,
        val products: List<Product>,
        val movements: List<StockMovement>,
        val purchases: List<Purchase>,
        val purchaseItems: List<PurchaseItem>
    ) : DataUiState
    data class Error(val message: String) : DataUiState
}

data class ProductFilters(
    val search: String = "",
    val categoryId: String = "",
    val lowStockOnly: Boolean = false
)

data class FinancialFilters(
    val searchTerm: String = "",
    val categoryName: String = "",
    val variation: PriceVariationFilter = PriceVariationFilter.ALL,
    val selectedPurchaseId: String? = null
)

data class ScanFeedback(
    val isExisting: Boolean,
    val message: String
)

data class DashboardComputed(
    val totalProducts: Int,
    val totalUnits: Double,
    val totalValue: Double,
    val lowStockProducts: List<Product>,
    val expiringSoonProducts: List<Product>,
    val recentMovements: List<StockMovement>
)

data class FinancialComputed(
    val monthTotalSpent: Double,
    val monthPurchaseCount: Int,
    val avgVariationPct: Double,
    val itemsUp: Int,
    val itemsSame: Int,
    val itemsDown: Int,
    val spendingByCategory: List<CategorySpending>,
    val filteredItems: List<PurchaseItem>
)

class AstroViewModel(
    private val repository: AstroRepository,
    private val currentUser: FirebaseUser
) : ViewModel() {

    private val uid = currentUser.uid

    private val _activeTab = MutableStateFlow(AppTab.DASHBOARD)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    private val _productFilters = MutableStateFlow(ProductFilters())
    val productFilters: StateFlow<ProductFilters> = _productFilters.asStateFlow()

    private val _financialFilters = MutableStateFlow(FinancialFilters())
    val financialFilters: StateFlow<FinancialFilters> = _financialFilters.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _storeName = MutableStateFlow("Supermercado")
    val storeName: StateFlow<String> = _storeName.asStateFlow()

    private val _purchaseNotes = MutableStateFlow("")
    val purchaseNotes: StateFlow<String> = _purchaseNotes.asStateFlow()

    private val _isSubmittingPurchase = MutableStateFlow(false)
    val isSubmittingPurchase: StateFlow<Boolean> = _isSubmittingPurchase.asStateFlow()

    private val _scanFeedback = MutableStateFlow<ScanFeedback?>(null)
    val scanFeedback: StateFlow<ScanFeedback?> = _scanFeedback.asStateFlow()

    private val _finishedSummary = MutableStateFlow<FinishedPurchaseSummary?>(null)
    val finishedSummary: StateFlow<FinishedPurchaseSummary?> = _finishedSummary.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val dataState: StateFlow<DataUiState> = combine(
        repository.observeCategories(uid),
        repository.observeProducts(uid),
        repository.observeMovements(uid),
        repository.observePurchases(uid),
        repository.observePurchaseItems(uid)
    ) { categories, products, movements, purchases, purchaseItems ->
        DataUiState.Success(
            categories = categories,
            products = products,
            movements = movements,
            purchases = purchases,
            purchaseItems = purchaseItems
        ) as DataUiState
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w("AstroViewModel", "Error observing Firestore data", rootError)
        emit(DataUiState.Error(rootError.message ?: "Erro ao carregar seus dados da nuvem."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DataUiState.Loading
    )

    init {
        viewModelScope.launch {
            val res = repository.ensureUserProfileAndSeed(currentUser)
            res.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Erro ao preparar conta inicial."
            }
        }
    }

    fun selectTab(tab: AppTab, onlyLowStock: Boolean = false) {
        _activeTab.value = tab
        if (tab == AppTab.PRODUCTS && onlyLowStock) {
            _productFilters.update { it.copy(lowStockOnly = true) }
        }
    }

    fun updateProductFilters(filters: ProductFilters) {
        _productFilters.value = filters
    }

    fun updateFinancialFilters(filters: FinancialFilters) {
        _financialFilters.value = filters
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun updateStoreName(value: String) {
        _storeName.value = value
    }

    fun updatePurchaseNotes(value: String) {
        _purchaseNotes.value = value
    }

    fun dismissScanFeedback() {
        _scanFeedback.value = null
    }

    fun dismissFinishedSummary() {
        _finishedSummary.value = null
    }

    fun computeCartTotals(items: List<CartItem>): CartTotals {
        var totalValue = 0.0
        var totalUnits = 0.0
        var prevTotal = 0.0
        for (item in items) {
            val q = item.parsedQuantity
            val p = item.parsedUnitPrice
            val prevP = if (item.previousUnitPrice > 0.0) item.previousUnitPrice else p
            totalValue += q * p
            totalUnits += q
            prevTotal += q * prevP
        }
        val diff = totalValue - prevTotal
        val pct = if (prevTotal > 0.0) (diff / prevTotal) * 100.0 else 0.0
        return CartTotals(
            totalValue = totalValue,
            totalUnits = totalUnits,
            previousTotalValue = prevTotal,
            diff = diff,
            variationPct = pct
        )
    }

    fun addProductToCart(product: Product, customBarcode: String? = null) {
        _cartItems.update { current ->
            val idx = current.indexOfFirst { it.productId == product.id }
            if (idx >= 0) {
                current.mapIndexed { i, item ->
                    if (i == idx) {
                        val nextQty = item.parsedQuantity + 1.0
                        item.copy(quantityInput = formatQty(nextQty))
                    } else {
                        item
                    }
                }
            } else {
                val unitPrice = product.unitPrice
                val prevPrice = if (product.previousUnitPrice > 0.0) product.previousUnitPrice else unitPrice
                current + CartItem(
                    localId = UUID.randomUUID().toString(),
                    productId = product.id,
                    name = product.name,
                    barcode = customBarcode ?: product.barcode ?: product.sku.orEmpty(),
                    categoryId = product.categoryId,
                    categoryName = product.categoryName ?: "Geral",
                    categoryColor = product.categoryColor ?: "#3b82f6",
                    quantityInput = "1",
                    unitPriceInput = if (unitPrice > 0.0) formatPrice(unitPrice) else "",
                    previousUnitPrice = prevPrice,
                    unit = product.unit
                )
            }
        }
    }

    fun addCartItemByBarcode(
        rawBarcode: String,
        products: List<Product>,
        categories: List<Category>
    ) {
        val clean = rawBarcode.trim()
        if (clean.isEmpty()) return

        val matched = products.firstOrNull { p ->
            p.barcode?.trim() == clean || p.sku?.trim() == clean
        }
        if (matched != null) {
            addProductToCart(matched, clean)
            _scanFeedback.value = ScanFeedback(
                isExisting = true,
                message = "\"${matched.name}\" adicionado ao carrinho! Preço anterior: R$ ${formatPrice(matched.unitPrice)}"
            )
        } else {
            val defaultCat = categories.firstOrNull()
            _cartItems.update { current ->
                current + CartItem(
                    localId = UUID.randomUUID().toString(),
                    productId = null,
                    name = "",
                    barcode = clean,
                    categoryId = defaultCat?.id,
                    categoryName = defaultCat?.name ?: "Geral",
                    categoryColor = defaultCat?.color ?: "#3b82f6",
                    quantityInput = "1",
                    unitPriceInput = "",
                    previousUnitPrice = 0.0,
                    unit = "un"
                )
            }
            _scanFeedback.value = ScanFeedback(
                isExisting = false,
                message = "Novo código lido ($clean)! Preencha o nome e o preço da prateleira."
            )
        }
    }

    fun addCustomCartItem(nameOrBarcode: String, categories: List<Category>) {
        val clean = nameOrBarcode.trim()
        if (clean.isEmpty()) return
        val isBarcode = Regex("^\\d{8,14}$").matches(clean)
        val defaultCat = categories.firstOrNull()
        _cartItems.update { current ->
            current + CartItem(
                localId = UUID.randomUUID().toString(),
                productId = null,
                name = if (isBarcode) "" else clean,
                barcode = if (isBarcode) clean else "",
                categoryId = defaultCat?.id,
                categoryName = defaultCat?.name ?: "Geral",
                categoryColor = defaultCat?.color ?: "#3b82f6",
                quantityInput = "1",
                unitPriceInput = "",
                previousUnitPrice = 0.0,
                unit = "un"
            )
        }
    }

    fun loadLowStockIntoCart(products: List<Product>) {
        val lowStock = products.filter { it.quantity <= it.minQuantity }
        _cartItems.update { current ->
            val existingIds = current.mapNotNull { it.productId }.toSet()
            val toAdd = lowStock.filter { it.id !in existingIds }.map { prod ->
                val needed = (prod.minQuantity - prod.quantity).coerceAtLeast(1.0)
                val price = prod.unitPrice
                val prev = if (prod.previousUnitPrice > 0.0) prod.previousUnitPrice else price
                CartItem(
                    localId = UUID.randomUUID().toString(),
                    productId = prod.id,
                    name = prod.name,
                    barcode = prod.barcode ?: prod.sku.orEmpty(),
                    categoryId = prod.categoryId,
                    categoryName = prod.categoryName ?: "Geral",
                    categoryColor = prod.categoryColor ?: "#3b82f6",
                    quantityInput = formatQty(needed),
                    unitPriceInput = if (price > 0.0) formatPrice(price) else "",
                    previousUnitPrice = prev,
                    unit = prod.unit
                )
            }
            current + toAdd
        }
    }

    fun updateCartItem(
        localId: String,
        name: String? = null,
        quantityInput: String? = null,
        unitPriceInput: String? = null
    ) {
        _cartItems.update { current ->
            current.map { item ->
                if (item.localId != localId) item
                else item.copy(
                    name = name ?: item.name,
                    quantityInput = quantityInput ?: item.quantityInput,
                    unitPriceInput = unitPriceInput ?: item.unitPriceInput
                )
            }
        }
    }

    fun removeCartItem(localId: String) {
        _cartItems.update { current -> current.filterNot { it.localId == localId } }
    }

    fun submitCartPurchase(products: List<Product>, categories: List<Category>) {
        val items = _cartItems.value
        if (items.isEmpty()) return
        if (items.any { it.name.trim().isEmpty() }) {
            _snackbarMessage.value = "Informe o nome de todos os itens do carrinho antes de finalizar."
            return
        }
        if (items.any { it.parsedQuantity <= 0.0 || it.unitPriceInput.trim().isEmpty() }) {
            _snackbarMessage.value = "Confira as quantidades e os preços antes de finalizar a compra."
            return
        }

        viewModelScope.launch {
            _isSubmittingPurchase.value = true
            val result = repository.finishPurchase(
                storeName = _storeName.value,
                notes = _purchaseNotes.value,
                cartItems = items,
                existingProducts = products,
                categories = categories
            )
            _isSubmittingPurchase.value = false
            result.onSuccess { summary ->
                _finishedSummary.value = summary
                _cartItems.value = emptyList()
                _scanFeedback.value = null
                _snackbarMessage.value = "Compra finalizada e estoque atualizado!"
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Erro ao salvar compra."
            }
        }
    }

    fun importParsedReceipt(
        storeName: String,
        notes: String,
        parsedItems: List<ParsedReceiptItem>,
        products: List<Product>,
        categories: List<Category>,
        onDone: () -> Unit
    ) {
        val asCart = parsedItems.map { item ->
            CartItem(
                localId = item.localId,
                productId = item.productId,
                name = item.name,
                barcode = item.barcode,
                categoryId = item.categoryId,
                categoryName = item.categoryName,
                categoryColor = item.categoryColor,
                quantityInput = item.quantityInput,
                unitPriceInput = item.unitPriceInput,
                previousUnitPrice = item.previousUnitPrice,
                unit = item.unit
            )
        }
        viewModelScope.launch {
            _isSubmittingPurchase.value = true
            val result = repository.finishPurchase(
                storeName = storeName,
                notes = notes,
                cartItems = asCart,
                existingProducts = products,
                categories = categories
            )
            _isSubmittingPurchase.value = false
            result.onSuccess { summary ->
                _finishedSummary.value = summary
                _snackbarMessage.value = "Cupom importado! ${summary.itemsCount} itens enviados ao estoque."
                onDone()
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Erro ao importar cupom."
            }
        }
    }

    fun saveProduct(
        existing: Product?,
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
        notes: String?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.saveProduct(
                existingProduct = existing,
                name = name,
                sku = sku,
                barcode = barcode,
                category = category,
                location = location,
                unit = unit,
                quantity = quantity,
                minQuantity = minQuantity,
                unitPrice = unitPrice,
                previousUnitPrice = previousUnitPrice,
                expirationDate = expirationDate,
                notes = notes
            )
            result.onSuccess {
                _snackbarMessage.value = if (existing == null) {
                    "Produto \"$name\" cadastrado!"
                } else {
                    "Produto \"$name\" atualizado!"
                }
                onDone()
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Erro ao salvar produto."
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product.id)
                .onSuccess { _snackbarMessage.value = "Produto \"${product.name}\" removido." }
                .onFailure { err -> _snackbarMessage.value = err.message ?: "Erro ao excluir produto." }
        }
    }

    fun recordMovement(
        product: Product,
        type: String,
        quantity: Double,
        reason: String?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            repository.recordMovement(product, type, quantity, reason)
                .onSuccess {
                    _snackbarMessage.value = if (type == "IN") {
                        "Entrada registrada no estoque!"
                    } else {
                        "Saída registrada no estoque!"
                    }
                    onDone()
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Erro ao registrar movimentação."
                }
        }
    }

    fun saveCategory(
        categoryId: String?,
        name: String,
        description: String?,
        color: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            repository.saveCategory(categoryId, name, description, color)
                .onSuccess {
                    _snackbarMessage.value = if (categoryId == null) {
                        "Categoria \"$name\" criada!"
                    } else {
                        "Categoria \"$name\" atualizada!"
                    }
                    onDone()
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Erro ao salvar categoria."
                }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category.id)
                .onSuccess { _snackbarMessage.value = "Categoria \"${category.name}\" removida." }
                .onFailure { err -> _snackbarMessage.value = err.message ?: "Erro ao excluir categoria." }
        }
    }

    fun computeDashboard(state: DataUiState.Success): DashboardComputed {
        val products = state.products
        val lowStock = products.filter { it.quantity <= it.minQuantity }
        val expiring = products.filter { p ->
            val days = daysUntilExpiration(p.expirationDate)
            days != null && days <= 30L && p.quantity > 0.0
        }.sortedBy { daysUntilExpiration(it.expirationDate) ?: Long.MAX_VALUE }

        return DashboardComputed(
            totalProducts = products.size,
            totalUnits = products.sumOf { it.quantity },
            totalValue = products.sumOf { it.quantity * it.unitPrice },
            lowStockProducts = lowStock,
            expiringSoonProducts = expiring,
            recentMovements = state.movements.take(10)
        )
    }

    fun computeFinancial(
        state: DataUiState.Success,
        filters: FinancialFilters
    ): FinancialComputed {
        val nowCal = Calendar.getInstance()
        val currentMonth = nowCal.get(Calendar.MONTH)
        val currentYear = nowCal.get(Calendar.YEAR)

        val monthPurchases = state.purchases.filter { pur ->
            val d = pur.createdAt?.toDate() ?: return@filter true
            val c = Calendar.getInstance().apply { time = d }
            c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
        }
        val monthTotal = monthPurchases.sumOf { it.totalAmount }

        val items = state.purchaseItems
        var itemsUp = 0
        var itemsSame = 0
        var itemsDown = 0
        var totalPctSum = 0.0
        var comparedCount = 0

        for (it in items) {
            if (it.previousUnitPrice > 0.0 && it.unitPrice > 0.0) {
                val diff = it.unitPrice - it.previousUnitPrice
                val pct = (diff / it.previousUnitPrice) * 100.0
                totalPctSum += pct
                comparedCount++
                when {
                    diff > 0.005 -> itemsUp++
                    diff < -0.005 -> itemsDown++
                    else -> itemsSame++
                }
            }
        }
        val avgVariation = if (comparedCount > 0) totalPctSum / comparedCount else 0.0

        val catGrouped = items.groupBy { it.categoryName ?: "Sem categoria" }
        val totalAll = items.sumOf { it.totalPrice }
        val spending = catGrouped.map { (catName, list) ->
            val sum = list.sumOf { it.totalPrice }
            CategorySpending(
                categoryName = catName,
                categoryColor = list.firstOrNull()?.categoryColor ?: "#3b82f6",
                totalSpent = sum,
                sharePct = if (totalAll > 0.0) (sum / totalAll) * 100.0 else 0.0
            )
        }.sortedByDescending { it.totalSpent }

        val filtered = items.filter { item ->
            val matchesPurchase = filters.selectedPurchaseId == null ||
                item.purchaseId == filters.selectedPurchaseId
            val matchesCat = filters.categoryName.isBlank() ||
                item.categoryName == filters.categoryName
            val term = filters.searchTerm.trim()
            val matchesSearch = term.isEmpty() ||
                item.productName.contains(term, ignoreCase = true) ||
                item.barcode.orEmpty().contains(term, ignoreCase = true)

            val diff = item.unitPrice - item.previousUnitPrice
            val matchesVar = when (filters.variation) {
                PriceVariationFilter.ALL -> true
                PriceVariationFilter.UP -> item.previousUnitPrice > 0.0 && diff > 0.005
                PriceVariationFilter.DOWN -> item.previousUnitPrice > 0.0 && diff < -0.005
                PriceVariationFilter.EQUAL -> item.previousUnitPrice <= 0.0 || kotlin.math.abs(diff) <= 0.005
            }
            matchesPurchase && matchesCat && matchesSearch && matchesVar
        }

        return FinancialComputed(
            monthTotalSpent = monthTotal,
            monthPurchaseCount = monthPurchases.size,
            avgVariationPct = avgVariation,
            itemsUp = itemsUp,
            itemsSame = itemsSame,
            itemsDown = itemsDown,
            spendingByCategory = spending,
            filteredItems = filtered
        )
    }

    private fun formatQty(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else String.format(java.util.Locale.US, "%.2f", value)
    }

    private fun formatPrice(value: Double): String {
        return String.format(java.util.Locale.US, "%.2f", value)
    }
}
