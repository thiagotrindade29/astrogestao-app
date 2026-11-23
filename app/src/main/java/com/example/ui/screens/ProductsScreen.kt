package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                    )
                }
            }
        }

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
                }
            }
        }
    }

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
}
