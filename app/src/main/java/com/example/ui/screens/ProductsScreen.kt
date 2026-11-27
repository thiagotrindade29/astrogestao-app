package com.example.ui.screens

<<<<<<< HEAD
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
=======
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
>>>>>>> origin/main
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
<<<<<<< HEAD
=======
import androidx.compose.foundation.layout.height
>>>>>>> origin/main
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
<<<<<<< HEAD
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
=======
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
>>>>>>> origin/main
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
<<<<<<< HEAD
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
=======
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
>>>>>>> origin/main
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
<<<<<<< HEAD
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.ui.ProductFilters
import com.example.ui.components.AstroCard
import com.example.ui.components.CategoryBadge
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.StockStatusPill
import com.example.ui.theme.LocalAstroColors
import com.example.util.formatCurrency
import com.example.util.formatIsoDate
import com.example.util.formatNumber
import com.example.util.parseHexColor

@Composable
fun ProductsScreen(
    products: List<Product>,
    categories: List<Category>,
    filters: ProductFilters,
    onFiltersChange: (ProductFilters) -> Unit,
    onNewProduct: () -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onOpenMovement: (Product, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val astroColors = LocalAstroColors.current
    var pendingDelete by remember { mutableStateOf<Product?>(null) }

    val filteredProducts = remember(products, filters) {
        val term = filters.search.trim()
        products.filter { p ->
            val matchesSearch = term.isEmpty() ||
                p.name.contains(term, ignoreCase = true) ||
                p.location.contains(term, ignoreCase = true) ||
                p.barcode.orEmpty().contains(term, ignoreCase = true) ||
                p.sku.orEmpty().contains(term, ignoreCase = true)
            val matchesCat = filters.categoryId.isEmpty() || p.categoryId == filters.categoryId
            val matchesLow = !filters.lowStockOnly || p.quantity <= p.minQuantity
            matchesSearch && matchesCat && matchesLow
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = filters.search,
                onValueChange = { onFiltersChange(filters.copy(search = it)) },
                label = { Text("Buscar na despensa ou código de barras...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("products_search_input")
            )
        }

        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filters.lowStockOnly,
                    onClick = { onFiltersChange(filters.copy(lowStockOnly = !filters.lowStockOnly)) },
                    label = { Text("Somente em falta / baixo") },
                    modifier = Modifier.testTag("filter_low_stock_chip")
                )
                FilterChip(
                    selected = filters.categoryId.isEmpty(),
                    onClick = { onFiltersChange(filters.copy(categoryId = "")) },
                    label = { Text("Todas categorias") }
                )
                categories.forEach { cat ->
                    FilterChip(
                        selected = filters.categoryId == cat.id,
                        onClick = {
                            onFiltersChange(
                                filters.copy(
                                    categoryId = if (filters.categoryId == cat.id) "" else cat.id
                                )
                            )
                        },
                        label = { Text(cat.name) }
=======
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CategoryEntity
import com.example.data.local.MovementType
import com.example.data.local.ProductEntity
import com.example.ui.components.CategoryPill
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LowStockBadge
import com.example.ui.components.daysUntil
import com.example.ui.components.formatBrl
import com.example.ui.components.formatQty
import com.example.ui.components.formatShortDateBr
import com.example.ui.theme.AstroTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.AstroUiState

@Composable
fun ProductsScreen(
    uiState: AstroUiState,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (Long?) -> Unit,
    onToggleLowStock: (Boolean) -> Unit,
    onCreateProduct: () -> Unit,
    onEditProduct: (ProductEntity) -> Unit,
    onDeleteProduct: (ProductEntity) -> Unit,
    onQuickMovement: (ProductEntity, MovementType) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<ProductEntity?>(null) }

    pendingDelete?.let { product ->
        ConfirmationDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_product, product.name),
            onConfirm = {
                onDeleteProduct(product)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.products_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = uiState.productSearchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text(stringResource(R.string.products_search_hint)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.productSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Limpar busca")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("products_search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Category Filter Chips with Colored Identifiers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.selectedCategoryId == null,
                onClick = { onSelectCategory(null) },
                label = { Text(stringResource(R.string.filter_all)) },
                modifier = Modifier.testTag("category_filter_all")
            )
            uiState.categories.forEach { cat ->
                FilterChip(
                    selected = uiState.selectedCategoryId == cat.id,
                    onClick = {
                        onSelectCategory(if (uiState.selectedCategoryId == cat.id) null else cat.id)
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(cat.colorHex))
                        )
                    },
                    label = { Text(cat.name) },
                    modifier = Modifier.testTag("category_filter_${cat.id}")
                )
            }
        }

        // Low-Stock Switch Filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.filter_low_stock_only),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Switch(
                checked = uiState.onlyLowStock,
                onCheckedChange = onToggleLowStock,
                modifier = Modifier.testTag("low_stock_filter_switch")
            )
        }

        val products = uiState.filteredProducts
        if (products.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Rounded.Inventory2,
                title = stringResource(R.string.products_empty_title),
                body = stringResource(R.string.products_empty_body),
                primaryActionLabel = stringResource(R.string.action_new_product),
                onPrimaryAction = onCreateProduct,
                modifier = Modifier.padding(top = 12.dp),
                testTag = "products_empty_state"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("products_list"),
                contentPadding = PaddingValues(top = 4.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    val category = product.categoryId?.let { uiState.categoryMap[it] }
                    val aliasCount = uiState.aliasesByProductId[product.id]?.size ?: 0
                    ProductCatalogCard(
                        product = product,
                        category = category,
                        aliasCount = aliasCount,
                        isLowStock = uiState.isProductLowStock(product),
                        isSingleConsumption = uiState.isSingleConsumptionCategory(product.categoryId),
                        onEdit = { onEditProduct(product) },
                        onDelete = { pendingDelete = product },
                        onEntry = { onQuickMovement(product, MovementType.ENTRADA) },
                        onExit = { onQuickMovement(product, MovementType.SAIDA) }
>>>>>>> origin/main
                    )
                }
            }
        }
<<<<<<< HEAD

        if (filteredProducts.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Outlined.Inventory2,
                    title = "Nenhum produto encontrado",
                    subtitle = "Ajuste os filtros ou cadastre um novo item da sua casa.",
                    action = {
                        Button(
                            onClick = onNewProduct,
                            colors = ButtonDefaults.buttonColors(containerColor = astroColors.primary)
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Novo Produto")
                        }
                    }
                )
            }
        } else {
            items(filteredProducts, key = { it.id }) { product ->
                AstroCard(modifier = Modifier.testTag("product_card_${product.id}")) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = astroColors.textPrimary
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!product.categoryName.isNullOrBlank()) {
                                        CategoryBadge(
                                            name = product.categoryName,
                                            color = parseHexColor(product.categoryColor)
                                        )
                                    }
                                    Text(
                                        text = product.location,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = astroColors.textSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            StockStatusPill(
                                quantity = product.quantity,
                                minQuantity = product.minQuantity
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${formatNumber(product.quantity)} ${product.unit}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = astroColors.primary
                                )
                                Text(
                                    text = "Mínimo: ${formatNumber(product.minQuantity)} ${product.unit}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = astroColors.textMuted
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (product.unitPrice > 0.0) {
                                        "${formatCurrency(product.unitPrice)} / ${product.unit}"
                                    } else "Sem preço",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = astroColors.textPrimary
                                )
                                if (!product.expirationDate.isNullOrBlank()) {
                                    Text(
                                        text = "Validade: ${formatIsoDate(product.expirationDate)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = astroColors.textMuted
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { onOpenMovement(product, "IN") },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("product_in_${product.id}")
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Entrada")
                            }
                            OutlinedButton(
                                onClick = { onOpenMovement(product, "OUT") },
                                enabled = product.quantity > 0.0,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("product_out_${product.id}")
                            ) {
                                Icon(Icons.Outlined.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Saída")
                            }
                            IconButton(
                                onClick = { onEditProduct(product) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Editar ${product.name}",
                                    tint = astroColors.textSecondary
                                )
                            }
                            IconButton(
                                onClick = { pendingDelete = product },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Excluir ${product.name}",
                                    tint = astroColors.danger
                                )
                            }
                        }
                    }
=======
    }
}

@Composable
private fun ProductCatalogCard(
    product: ProductEntity,
    category: CategoryEntity?,
    aliasCount: Int,
    isLowStock: Boolean,
    isSingleConsumption: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEntry: () -> Unit,
    onExit: () -> Unit
) {
    val extended = AstroTheme.extended
    val expiryDays = product.expiryDateMillis?.let { daysUntil(it) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onEdit)
            .testTag("product_card_${product.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isLowStock) extended.warning.copy(alpha = 0.5f) else extended.borderSubtle
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (category != null) {
                        CategoryPill(name = category.name, colorHex = category.colorHex)
                    }
                    if (isLowStock) {
                        LowStockBadge()
                    }
                }
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.action_edit),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = extended.negative,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val codeMeta = buildList {
                    if (product.uniqueCode.isNotBlank()) add("ID: ${product.uniqueCode}")
                    if (product.code.isNotBlank()) add("EAN: ${product.code}")
                    if (aliasCount > 1) add("$aliasCount códigos vinculados")
                }.joinToString(" • ")
                if (codeMeta.isNotBlank()) {
                    Text(
                        text = codeMeta,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isSingleConsumption) {
                            "Saldo: ${formatQty(product.quantity)} ${product.unit} • Consumo único"
                        } else {
                            "Saldo: ${formatQty(product.quantity)} ${product.unit} (mín: ${formatQty(product.minStock)})"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isLowStock) extended.warning else MaterialTheme.colorScheme.onSurface
                    )
                    if (product.expiryDateMillis != null && expiryDays != null) {
                        Text(
                            text = "Validade: ${formatShortDateBr(product.expiryDateMillis)}" +
                                if (expiryDays <= 30) " (${if (expiryDays < 0) "Vencido" else "$expiryDays dias"})" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (expiryDays <= 30) extended.negative else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatBrl(product.estimatedTotalValue),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = extended.positive
                    )
                    Text(
                        text = "${formatBrl(product.unitPrice)} / ${product.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("product_exit_${product.id}"),
                    border = BorderStroke(1.dp, extended.negative.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = null,
                        tint = extended.negative,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Saída", color = extended.negative)
                }
                OutlinedButton(
                    onClick = onEntry,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("product_entry_${product.id}"),
                    border = BorderStroke(1.dp, extended.positive.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = extended.positive,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Entrada", color = extended.positive)
>>>>>>> origin/main
                }
            }
        }
    }
<<<<<<< HEAD

    pendingDelete?.let { prod ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remover produto") },
            text = { Text("Deseja excluir \"${prod.name}\" do seu estoque?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteProduct(prod)
                        pendingDelete = null
                    }
                ) {
                    Text("Excluir", color = astroColors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
=======
>>>>>>> origin/main
}
