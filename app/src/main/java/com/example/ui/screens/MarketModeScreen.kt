package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CartItem
import com.example.data.model.CartTotals
import com.example.data.model.Category
import com.example.data.model.FinishedPurchaseSummary
import com.example.data.model.Product
import com.example.ui.ScanFeedback
import com.example.ui.components.AstroCard
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalAstroColors
import com.example.util.formatCurrency
import com.example.util.formatNumber
import kotlin.math.abs

@Composable
fun MarketModeScreen(
    cartItems: List<CartItem>,
    cartTotals: CartTotals,
    storeName: String,
    purchaseNotes: String,
    isSubmitting: Boolean,
    scanFeedback: ScanFeedback?,
    finishedSummary: FinishedPurchaseSummary?,
    products: List<Product>,
    categories: List<Category>,
    onStoreNameChange: (String) -> Unit,
    onPurchaseNotesChange: (String) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenNfeImport: () -> Unit,
    onLoadLowStock: () -> Unit,
    onAddProductToCart: (Product) -> Unit,
    onAddCustomItem: (String) -> Unit,
    onUpdateCartItem: (localId: String, name: String?, qty: String?, price: String?) -> Unit,
    onRemoveCartItem: (localId: String) -> Unit,
    onSubmitPurchase: () -> Unit,
    onDismissScanFeedback: () -> Unit,
    onDismissFinishedSummary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val astroColors = LocalAstroColors.current
    var searchQuery by remember { mutableStateOf("") }

    val quickMatches = remember(searchQuery, products) {
        val term = searchQuery.trim()
        if (term.isEmpty()) emptyList()
        else products.filter {
            it.name.contains(term, ignoreCase = true) ||
                it.barcode.orEmpty().contains(term, ignoreCase = true) ||
                it.sku.orEmpty().contains(term, ignoreCase = true)
        }.take(5)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Live Cart Total Header ("Antes do caixa")
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = astroColors.spaceDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total estimado no carrinho",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFD8CFF8)
                            )
                            Text(
                                text = formatCurrency(cartTotals.totalValue),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.testTag("market_cart_total_text")
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${cartItems.size} itens • ${formatNumber(cartTotals.totalUnits)} un.",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (cartItems.isNotEmpty() && cartTotals.previousTotalValue > 0.0) {
                        val isMoreExpensive = cartTotals.diff > 0.01
                        val isCheaper = cartTotals.diff < -0.01
                        val statusText = when {
                            isMoreExpensive -> "+${formatCurrency(cartTotals.diff)} (+${String.format("%.1f", cartTotals.variationPct)}%) vs última compra"
                            isCheaper -> "-${formatCurrency(abs(cartTotals.diff))} (${String.format("%.1f", cartTotals.variationPct)}%) mais barato"
                            else -> "Mesmo valor da última compra"
                        }
                        StatusPill(
                            text = statusText,
                            color = when {
                                isMoreExpensive -> Color(0xFFFCA5A5)
                                isCheaper -> Color(0xFF6EE7B7)
                                else -> Color(0xFFE2E8F0)
                            },
                            backgroundColor = Color.White.copy(alpha = 0.12f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenScanner,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("market_scan_barcode_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = astroColors.primary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ler Código", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = onOpenNfeImport,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("market_open_nfe_button"),
                            border = BorderStroke(1.dp, Color(0xFF9F88E8)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cupom / NF-e", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        if (finishedSummary != null) {
            item {
                AstroCard(
                    containerColor = astroColors.successBg,
                    borderColor = astroColors.success.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = astroColors.success
                            )
                            Column {
                                Text(
                                    text = "Compra salva! (${finishedSummary.storeName})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = astroColors.textPrimary
                                )
                                Text(
                                    text = "${finishedSummary.itemsCount} itens • ${formatCurrency(finishedSummary.totalAmount)} adicionados ao estoque.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = astroColors.textSecondary
                                )
                            }
                        }
                        TextButton(onClick = onDismissFinishedSummary) {
                            Text("OK")
                        }
                    }
                }
            }
        }

        if (scanFeedback != null) {
            item {
                AstroCard(
                    containerColor = if (scanFeedback.isExisting) astroColors.infoBg else astroColors.warningBg,
                    borderColor = if (scanFeedback.isExisting) astroColors.info.copy(alpha = 0.4f) else astroColors.warning.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = scanFeedback.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = astroColors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismissScanFeedback) {
                            Text("Fechar")
                        }
                    }
                }
            }
        }

        // Quick Add / Search & Load Low Stock
        item {
            AstroCard {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Buscar produto ou digitar item avulso") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (searchQuery.isNotBlank()) {
                                    if (quickMatches.isNotEmpty()) {
                                        onAddProductToCart(quickMatches.first())
                                    } else {
                                        onAddCustomItem(searchQuery)
                                    }
                                    searchQuery = ""
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("market_search_add_input")
                    )

                    if (searchQuery.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            quickMatches.forEach { prod ->
                                FilledTonalButton(
                                    onClick = {
                                        onAddProductToCart(prod)
                                        searchQuery = ""
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "+ ${prod.name} (${formatCurrency(prod.unitPrice)})",
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    onAddCustomItem(searchQuery)
                                    searchQuery = ""
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("market_add_custom_item_button")
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Adicionar \"${searchQuery.trim()}\" como item novo")
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onLoadLowStock,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("market_load_low_stock_button")
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Carregar itens em falta da casa")
                    }
                }
            }
        }

        if (cartItems.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Outlined.ShoppingCart,
                    title = "Carrinho vazio",
                    subtitle = "Escaneie o código de barras na prateleira ou busque um produto acima para ir somando antes de passar no caixa."
                )
            }
        } else {
            items(cartItems, key = { it.localId }) { item ->
                AstroCard(modifier = Modifier.testTag("cart_item_${item.localId}")) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.productId != null) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = astroColors.textPrimary
                                    )
                                    if (item.previousUnitPrice > 0.0) {
                                        Text(
                                            text = "Último preço: ${formatCurrency(item.previousUnitPrice)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = astroColors.textMuted
                                        )
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = item.name,
                                    onValueChange = { onUpdateCartItem(item.localId, it, null, null) },
                                    label = { Text("Nome do produto") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            IconButton(
                                onClick = { onRemoveCartItem(item.localId) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Remover ${item.name} do carrinho",
                                    tint = astroColors.danger
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = item.quantityInput,
                                onValueChange = { onUpdateCartItem(item.localId, null, it, null) },
                                label = { Text("Qtd (${item.unit})") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = item.unitPriceInput,
                                onValueChange = { onUpdateCartItem(item.localId, null, null, it) },
                                label = { Text("Preço un. (R$)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.previousUnitPrice > 0.0 && item.parsedUnitPrice > 0.0) {
                                val pct = item.diffPercent
                                when {
                                    pct > 0.5 -> StatusPill(
                                        text = "Subiu ${String.format("%.1f", pct)}%",
                                        color = astroColors.danger,
                                        backgroundColor = astroColors.dangerBg
                                    )
                                    pct < -0.5 -> StatusPill(
                                        text = "Caiu ${String.format("%.1f", abs(pct))}%",
                                        color = astroColors.success,
                                        backgroundColor = astroColors.successBg
                                    )
                                    else -> StatusPill(
                                        text = "Mesmo preço",
                                        color = astroColors.textSecondary,
                                        backgroundColor = astroColors.pageBackground
                                    )
                                }
                            } else {
                                StatusPill(
                                    text = if (item.barcode.isNotBlank()) "EAN ${item.barcode}" else item.categoryName,
                                    color = astroColors.info,
                                    backgroundColor = astroColors.infoBg
                                )
                            }
                            Text(
                                text = "Subtotal: ${formatCurrency(item.subtotal)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = astroColors.primary
                            )
                        }
                    }
                }
            }

            item {
                AstroCard {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Fechar Compra",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = astroColors.textPrimary
                        )
                        OutlinedTextField(
                            value = storeName,
                            onValueChange = onStoreNameChange,
                            label = { Text("Mercado / Loja") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("market_store_name_input")
                        )
                        OutlinedTextField(
                            value = purchaseNotes,
                            onValueChange = onPurchaseNotesChange,
                            label = { Text("Observação (opcional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        HorizontalDivider(color = astroColors.border)
                        Button(
                            onClick = onSubmitPurchase,
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("market_finish_purchase_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = astroColors.success)
                        ) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSubmitting) {
                                    "Salvando compra..."
                                } else {
                                    "Finalizar Compra (${formatCurrency(cartTotals.totalValue)})"
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
