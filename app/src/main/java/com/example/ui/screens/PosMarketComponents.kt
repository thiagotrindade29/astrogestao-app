package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CategoryEntity
import com.example.data.local.ProductEntity
import com.example.data.repository.ProductMatcher
import com.example.ui.components.CategoryPill
import com.example.ui.components.formatBrl
import com.example.ui.components.formatQty
import com.example.ui.theme.AstroTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.PosCartItem
import com.example.ui.viewmodel.PosMarketState
import java.util.Locale
import kotlin.math.abs

@Composable
internal fun PosSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmitCodeOrSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.market_search_hint)) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Clear, contentDescription = "Limpar busca")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmitCodeOrSearch() }),
        modifier = modifier.testTag("market_search_input")
    )
}

@Composable
internal fun PosCartItemCard(
    item: PosCartItem,
    category: CategoryEntity?,
    onQtyChange: (Double) -> Unit,
    onPriceChange: (Double) -> Unit,
    onRemove: () -> Unit
) {
    val extended = AstroTheme.extended
    var priceText by remember(item.key, item.unitPrice) {
        mutableStateOf(
            if (item.unitPrice > 0.0) String.format(Locale.US, "%.2f", item.unitPrice).replace(".", ",")
            else ""
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pos_cart_item_${item.key}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extended.borderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = item.uniqueCode,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (item.scannedCode.isNotBlank() && item.scannedCode != item.uniqueCode) {
                            Text(
                                text = "EAN: ${item.scannedCode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (category != null) {
                            CategoryPill(name = category.name, colorHex = category.colorHex)
                        }
                    }
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = extended.negative
                    )
                }
            }

            if (item.previousPrice != null && item.previousPrice > 0.0 && item.unitPrice > 0.0) {
                val diff = item.unitPrice - item.previousPrice
                if (abs(diff) >= 0.01) {
                    val isCheaper = diff < 0
                    val badgeColor = if (isCheaper) extended.positive else extended.negative
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isCheaper) Icons.Rounded.TrendingDown else Icons.Rounded.TrendingUp,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isCheaper) {
                                    "Mais barato ${formatBrl(abs(diff))} vs. compra anterior (${formatBrl(item.previousPrice)})"
                                } else {
                                    "Subiu +${formatBrl(diff)} vs. compra anterior (${formatBrl(item.previousPrice)})"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { raw ->
                        priceText = raw
                        val parsed = raw.replace(",", ".").toDoubleOrNull()
                        if (parsed != null && parsed >= 0.0) {
                            onPriceChange(parsed)
                        }
                    },
                    label = { Text("Preço unit. (R$)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onQtyChange((item.quantity - 1.0).coerceAtLeast(1.0)) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Diminuir quantidade", modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "${formatQty(item.quantity)} ${item.unit}",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily)
                        )
                        IconButton(
                            onClick = { onQtyChange(item.quantity + 1.0) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Aumentar quantidade", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Subtotal: ${formatBrl(item.subtotal)}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = extended.positive
                )
            }
        }
    }
}

@Composable
internal fun PosCheckoutBar(
    posState: PosMarketState,
    onToggleSaveToStock: (Boolean) -> Unit,
    onFinalizeCheckout: () -> Unit
) {
    val extended = AstroTheme.extended
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.market_save_to_stock_toggle),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (posState.saveToStock) {
                            "Soma quantidades no estoque e grava auditoria"
                        } else {
                            "Registra somente o gasto financeiro, sem alterar saldo"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = posState.saveToStock,
                    onCheckedChange = onToggleSaveToStock,
                    modifier = Modifier.testTag("market_save_to_stock_switch")
                )
            }

            HorizontalDivider(color = extended.borderSubtle)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total (${posState.cartItems.size} ${if (posState.cartItems.size == 1) "item" else "itens"})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatBrl(posState.totalAmount),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = extended.positive
                    )
                }
                Button(
                    onClick = onFinalizeCheckout,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("market_checkout_button")
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.market_checkout_button))
                }
            }
        }
    }
}

@Composable
internal fun UnmatchedBarcodeDialog(
    scannedCode: String,
    products: List<ProductEntity>,
    categories: List<CategoryEntity>,
    onLinkExisting: (ProductEntity) -> Unit,
    onCreateNew: (name: String, unit: String, unitPrice: Double, qty: Double, categoryId: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchByName by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newUnit by remember { mutableStateOf("un") }
    var newPriceText by remember { mutableStateOf("") }
    var newQtyText by remember { mutableStateOf("1") }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id) }
    var nameError by remember { mutableStateOf(false) }

    val matchingProducts = remember(searchByName, products) {
        val norm = ProductMatcher.normalizeProductName(searchByName)
        if (norm.isBlank()) products.take(8)
        else products.filter {
            ProductMatcher.normalizeProductName(it.name).contains(norm) ||
                it.uniqueCode.contains(searchByName, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Código não encontrado", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = "Código bipado: $scannedCode",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Mercados diferentes usam códigos distintos. Vincule a um produto existente pelo nome (mesmo código AST único) ou crie um novo produto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Vincular Existente", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Novo Produto", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }

                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = searchByName,
                        onValueChange = { searchByName = it },
                        label = { Text("Buscar por nome do produto") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (matchingProducts.isEmpty()) {
                        Text(
                            text = "Nenhum produto encontrado. Use a aba \"Novo Produto\" para gerar um código único AST.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        matchingProducts.forEach { prod ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLinkExisting(prod) },
                                shape = MaterialTheme.shapes.small,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prod.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = "${prod.uniqueCode} • Atual: ${formatQty(prod.quantity)} ${prod.unit}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.Link,
                                        contentDescription = "Vincular",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            newName = it
                            nameError = false
                        },
                        label = { Text(stringResource(R.string.label_name)) },
                        isError = nameError,
                        supportingText = if (nameError) {
                            { Text(stringResource(R.string.error_required)) }
                        } else null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pos_new_product_name_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newPriceText,
                            onValueChange = { newPriceText = it },
                            label = { Text("Preço (R$)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newQtyText,
                            onValueChange = { newQtyText = it },
                            label = { Text("Qtd") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(0.7f)
                        )
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("un", "kg", "g", "L", "ml", "pct").forEach { u ->
                            FilterChip(
                                selected = newUnit == u,
                                onClick = { newUnit = u },
                                label = { Text(u) }
                            )
                        }
                    }

                    if (categories.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategoryId == cat.id,
                                    onClick = { selectedCategoryId = cat.id },
                                    label = { Text(cat.name) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == 1) {
                Button(
                    onClick = {
                        if (newName.isBlank()) {
                            nameError = true
                            return@Button
                        }
                        val price = newPriceText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                        val qty = newQtyText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.1) ?: 1.0
                        onCreateNew(newName.trim(), newUnit, price, qty, selectedCategoryId)
                    },
                    modifier = Modifier.testTag("pos_create_new_product_button")
                ) {
                    Text("Adicionar ao Carrinho")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
