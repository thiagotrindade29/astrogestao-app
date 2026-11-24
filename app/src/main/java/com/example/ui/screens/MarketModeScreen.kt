package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ProductEntity
import com.example.data.repository.ProductMatcher
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.CategoryPill
import com.example.ui.components.formatBrl
import com.example.ui.components.formatQty
import com.example.ui.theme.AstroTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.AstroUiState
import com.example.ui.viewmodel.PosMarketState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarketModeScreen(
    uiState: AstroUiState,
    posState: PosMarketState,
    onStoreNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSaveToStock: (Boolean) -> Unit,
    onBarcodeScanned: (String) -> Unit,
    onAddExistingProduct: (ProductEntity) -> Unit,
    onLinkCodeToExisting: (ProductEntity, String) -> Unit,
    onCreateOrResolveNewItem: (scannedCode: String, name: String, unit: String, unitPrice: Double, qty: Double, categoryId: Long?) -> Unit,
    onDismissUnmatchedCode: () -> Unit,
    onLoadLowStock: () -> Unit,
    onUpdateItemQty: (key: String, qty: Double) -> Unit,
    onUpdateItemPrice: (key: String, price: Double) -> Unit,
    onRemoveItem: (key: String) -> Unit,
    onFinalizeCheckout: () -> Unit,
    onOpenNfceImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val extended = AstroTheme.extended
    val lowStock = uiState.lowStockProducts
    var showScanner by remember { mutableStateOf(false) }

    if (showScanner) {
        BarcodeScannerDialog(
            onCodeDetected = { code ->
                showScanner = false
                onBarcodeScanned(code)
            },
            onDismiss = { showScanner = false }
        )
    }

    posState.unmatchedScannedCode?.let { scannedCode ->
        UnmatchedBarcodeDialog(
            scannedCode = scannedCode,
            products = uiState.products,
            categories = uiState.categories,
            onLinkExisting = { existingProd -> onLinkCodeToExisting(existingProd, scannedCode) },
            onCreateNew = { name, unit, price, qty, catId ->
                onCreateOrResolveNewItem(scannedCode, name, unit, price, qty, catId)
            },
            onDismiss = onDismissUnmatchedCode
        )
    }

    val searchQ = posState.searchQuery.trim()
    val matchingSuggestions = remember(searchQ, uiState.products, uiState.aliases) {
        if (searchQ.isBlank()) {
            emptyList()
        } else {
            uiState.products.filter { prod ->
                ProductMatcher.matchesSearch(
                    product = prod,
                    aliasesForProduct = uiState.aliasesByProductId[prod.id].orEmpty(),
                    rawQuery = searchQ
                )
            }.take(6)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("market_pos_list"),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ShoppingCart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    text = stringResource(R.string.market_title),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.market_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (lowStock.isNotEmpty() || posState.cartItems.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    val shareLines = if (posState.cartItems.isNotEmpty()) {
                                        posState.cartItems.joinToString("\n") {
                                            "• ${it.name} (${it.uniqueCode}): ${formatQty(it.quantity)} ${it.unit} — ${formatBrl(it.subtotal)}"
                                        }
                                    } else {
                                        lowStock.joinToString("\n") {
                                            "• ${it.name} (${it.uniqueCode}): atual ${formatQty(it.quantity)} ${it.unit}"
                                        }
                                    }
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Compras — ${posState.storeName}")
                                        putExtra(Intent.EXTRA_TEXT, "AstroGestão (${posState.storeName}):\n\n$shareLines")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Compartilhar Lista"))
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("market_share_button")
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = "Compartilhar lista")
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenNfceImport,
                            modifier = Modifier.testTag("market_import_nfce_button")
                        ) {
                            Icon(Icons.Rounded.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_import_nfce))
                        }

                        // Shown in header only when cart is non-empty so empty-cart card stays the sole quick-restock CTA
                        if (posState.cartItems.isNotEmpty() && lowStock.isNotEmpty()) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = extended.warningContainer,
                                border = BorderStroke(1.dp, extended.warning.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable(onClick = onLoadLowStock)
                                    .testTag("market_restock_badge_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bolt,
                                        contentDescription = null,
                                        tint = extended.warning,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.market_restock_badge, lowStock.size),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = extended.warning,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, extended.borderSubtle)
                ) {
                    BoxWithConstraints(modifier = Modifier.padding(14.dp)) {
                        val isWide = maxWidth >= 540.dp
                        if (isWide) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = posState.storeName,
                                    onValueChange = onStoreNameChange,
                                    label = { Text(stringResource(R.string.market_store_label)) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("market_store_input")
                                )
                                OutlinedTextField(
                                    value = posState.notes,
                                    onValueChange = onNotesChange,
                                    label = { Text(stringResource(R.string.market_notes_label)) },
                                    placeholder = { Text(stringResource(R.string.market_notes_hint)) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1.4f)
                                        .testTag("market_notes_input")
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = posState.storeName,
                                    onValueChange = onStoreNameChange,
                                    label = { Text(stringResource(R.string.market_store_label)) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("market_store_input")
                                )
                                OutlinedTextField(
                                    value = posState.notes,
                                    onValueChange = onNotesChange,
                                    label = { Text(stringResource(R.string.market_notes_label)) },
                                    placeholder = { Text(stringResource(R.string.market_notes_hint)) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("market_notes_input")
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val onSearchSubmit: () -> Unit = {
                        if (matchingSuggestions.size == 1) {
                            onAddExistingProduct(matchingSuggestions.first())
                        } else if (searchQ.isNotBlank()) {
                            onBarcodeScanned(searchQ)
                        }
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isWide = maxWidth >= 520.dp
                        if (isWide) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PosSearchField(
                                    query = posState.searchQuery,
                                    onQueryChange = onSearchQueryChange,
                                    onSubmitCodeOrSearch = onSearchSubmit,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { showScanner = true },
                                    modifier = Modifier
                                        .height(54.dp)
                                        .testTag("market_scan_camera_button")
                                ) {
                                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.market_scan_camera))
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                PosSearchField(
                                    query = posState.searchQuery,
                                    onQueryChange = onSearchQueryChange,
                                    onSubmitCodeOrSearch = onSearchSubmit,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = { showScanner = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("market_scan_camera_button")
                                ) {
                                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.market_scan_camera))
                                }
                            }
                        }
                    }

                    if (searchQ.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                matchingSuggestions.forEach { prod ->
                                    val cat = prod.categoryId?.let { uiState.categoryMap[it] }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onAddExistingProduct(prod) }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = prod.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = prod.uniqueCode.ifBlank { "CAT" },
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                if (prod.code.isNotBlank()) {
                                                    Text(
                                                        text = "• ${prod.code}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (cat != null) {
                                                    CategoryPill(name = cat.name, colorHex = cat.colorHex)
                                                }
                                            }
                                        }
                                        Text(
                                            text = "+ ${formatBrl(prod.unitPrice)}",
                                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                                            color = extended.positive
                                        )
                                    }
                                    HorizontalDivider(color = extended.borderSubtle)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onBarcodeScanned(searchQ) }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.QrCodeScanner,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Usar \"$searchQ\" como novo código / produto",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (posState.cartItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("market_empty_cart_card"),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, extended.borderSubtle)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(52.dp)
                            )
                            Text(
                                text = stringResource(R.string.market_empty_cart_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.market_empty_cart_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (lowStock.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                val quickLoadLabel = if (lowStock.size == 1) {
                                    stringResource(R.string.market_load_low_stock_single)
                                } else {
                                    stringResource(R.string.market_load_low_stock, lowStock.size)
                                }
                                OutlinedButton(
                                    onClick = onLoadLowStock,
                                    modifier = Modifier.testTag("market_load_low_stock_button")
                                ) {
                                    Icon(
                                        Icons.Rounded.Bolt,
                                        contentDescription = null,
                                        tint = extended.warning,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(quickLoadLabel)
                                }
                            }
                        }
                    }
                }
            } else {
                items(posState.cartItems, key = { it.key }) { item ->
                    PosCartItemCard(
                        item = item,
                        category = item.categoryId?.let { uiState.categoryMap[it] },
                        onQtyChange = { onUpdateItemQty(item.key, it) },
                        onPriceChange = { onUpdateItemPrice(item.key, it) },
                        onRemove = { onRemoveItem(item.key) }
                    )
                }
            }
        }

        if (posState.cartItems.isNotEmpty()) {
            PosCheckoutBar(
                posState = posState,
                onToggleSaveToStock = onToggleSaveToStock,
                onFinalizeCheckout = onFinalizeCheckout
            )
        }
    }
}
