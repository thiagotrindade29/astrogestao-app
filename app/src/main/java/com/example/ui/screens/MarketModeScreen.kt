package com.example.ui.screens

<<<<<<< HEAD
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
=======
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
>>>>>>> origin/main
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
<<<<<<< HEAD
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
=======
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
>>>>>>> origin/main
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
<<<<<<< HEAD
import androidx.compose.material3.TextButton
=======
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
=======
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
>>>>>>> origin/main
                        }
                    }
                }
            }

            item {
<<<<<<< HEAD
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
=======
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
>>>>>>> origin/main
                        }
                    }
                }
            }
<<<<<<< HEAD
=======

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
>>>>>>> origin/main
        }
    }
}
