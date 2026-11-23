package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Product
import com.example.ui.AppTab
import com.example.ui.AstroViewModel
import com.example.ui.DataUiState
import com.example.ui.auth.signOut
import com.example.ui.components.EmptyStatePanel
import com.example.ui.modals.BarcodeScannerDialog
import com.example.ui.modals.MovementDialog
import com.example.ui.modals.NfeImportDialog
import com.example.ui.modals.ProductDialog
import com.example.ui.theme.LocalAstroColors
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShellScreen(
    user: FirebaseUser,
    viewModel: AstroViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val astroColors = LocalAstroColors.current
    val snackbarHostState = remember { SnackbarHostState() }

    val dataState by viewModel.dataState.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val productFilters by viewModel.productFilters.collectAsStateWithLifecycle()
    val financialFilters by viewModel.financialFilters.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val storeName by viewModel.storeName.collectAsStateWithLifecycle()
    val purchaseNotes by viewModel.purchaseNotes.collectAsStateWithLifecycle()
    val isSubmittingPurchase by viewModel.isSubmittingPurchase.collectAsStateWithLifecycle()
    val scanFeedback by viewModel.scanFeedback.collectAsStateWithLifecycle()
    val finishedSummary by viewModel.finishedSummary.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var showMoreMenu by remember { mutableStateOf(false) }
    var showProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var movementTarget by remember { mutableStateOf<Pair<Product, String>?>(null) }
    var showScannerForMarket by remember { mutableStateOf(false) }
    var showNfeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    if (activeTab != AppTab.DASHBOARD) {
        BackHandler {
            viewModel.selectTab(AppTab.DASHBOARD)
        }
    }

    val topTitle = when (activeTab) {
        AppTab.DASHBOARD -> stringResource(R.string.app_name)
        AppTab.PRODUCTS -> stringResource(R.string.nav_products)
        AppTab.MARKET -> stringResource(R.string.nav_market)
        AppTab.FINANCE -> stringResource(R.string.nav_finance)
        AppTab.MOVEMENTS -> stringResource(R.string.nav_movements)
        AppTab.CATEGORIES -> stringResource(R.string.nav_categories)
    }

    val primaryNavItems = listOf(
        Triple(AppTab.DASHBOARD, stringResource(R.string.nav_dashboard), Icons.Outlined.Dashboard),
        Triple(AppTab.PRODUCTS, stringResource(R.string.nav_products), Icons.Outlined.Inventory2),
        Triple(AppTab.MARKET, stringResource(R.string.nav_market), Icons.Outlined.ShoppingCart),
        Triple(AppTab.FINANCE, stringResource(R.string.nav_finance), Icons.Outlined.Analytics)
    )

    val cartBadgeCount = cartItems.size

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = astroColors.pageBackground,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = astroColors.spaceDark,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    title = {
                        Column {
                            Text(
                                text = topTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = user.displayName?.takeIf { it.isNotBlank() } ?: user.email ?: "Conta pessoal",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD8CFF8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showNfeDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("topbar_nfe_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                contentDescription = stringResource(R.string.cd_import_nfe)
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("topbar_more_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = stringResource(R.string.nav_more)
                                )
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.nav_movements)) },
                                    leadingIcon = { Icon(Icons.Outlined.History, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.selectTab(AppTab.MOVEMENTS)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.nav_categories)) },
                                    leadingIcon = { Icon(Icons.Outlined.Category, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.selectTab(AppTab.CATEGORIES)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.nav_import_nfe)) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        showNfeDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.nav_sign_out)) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        signOut(credentialManager, scope)
                                    }
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        modifier = Modifier.navigationBarsPadding(),
                        containerColor = astroColors.cardSurface
                    ) {
                        primaryNavItems.forEach { (tab, label, icon) ->
                            NavigationBarItem(
                                selected = activeTab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    if (tab == AppTab.MARKET && cartBadgeCount > 0) {
                                        BadgedBox(badge = { Badge { Text(cartBadgeCount.toString()) } }) {
                                            Icon(icon, contentDescription = label)
                                        }
                                    } else {
                                        Icon(icon, contentDescription = label)
                                    }
                                },
                                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag("bottom_nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (activeTab == AppTab.PRODUCTS) {
                    FloatingActionButton(
                        onClick = {
                            editingProduct = null
                            showProductDialog = true
                        },
                        containerColor = astroColors.primary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_new_product")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.cd_add_product)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(containerColor = astroColors.cardSurface) {
                        primaryNavItems.forEach { (tab, label, icon) ->
                            NavigationRailItem(
                                selected = activeTab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    if (tab == AppTab.MARKET && cartBadgeCount > 0) {
                                        BadgedBox(badge = { Badge { Text(cartBadgeCount.toString()) } }) {
                                            Icon(icon, contentDescription = label)
                                        }
                                    } else {
                                        Icon(icon, contentDescription = label)
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                        NavigationRailItem(
                            selected = activeTab == AppTab.MOVEMENTS,
                            onClick = { viewModel.selectTab(AppTab.MOVEMENTS) },
                            icon = { Icon(Icons.Outlined.History, contentDescription = stringResource(R.string.nav_movements)) },
                            label = { Text("Histórico") }
                        )
                        NavigationRailItem(
                            selected = activeTab == AppTab.CATEGORIES,
                            onClick = { viewModel.selectTab(AppTab.CATEGORIES) },
                            icon = { Icon(Icons.Outlined.Category, contentDescription = stringResource(R.string.nav_categories)) },
                            label = { Text("Categorias") }
                        )
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.widthIn(max = 900.dp).fillMaxSize()) {
                        when (val current = dataState) {
                            DataUiState.Loading -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = astroColors.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Sincronizando seu estoque...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = astroColors.textSecondary,
                                        modifier = Modifier.padding(top = 12.dp)
                                    )
                                }
                            }

                            is DataUiState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EmptyStatePanel(
                                        icon = Icons.Outlined.WarningAmber,
                                        title = "Não foi possível carregar os dados",
                                        subtitle = current.message
                                    )
                                }
                            }

                            is DataUiState.Success -> {
                                val dashboard = remember(current) { viewModel.computeDashboard(current) }
                                val cartTotals = remember(cartItems) { viewModel.computeCartTotals(cartItems) }
                                val financial = remember(current, financialFilters) {
                                    viewModel.computeFinancial(current, financialFilters)
                                }

                                when (activeTab) {
                                    AppTab.DASHBOARD -> DashboardScreen(
                                        dashboard = dashboard,
                                        onNavigateTab = viewModel::selectTab,
                                        onOpenNfeImport = { showNfeDialog = true },
                                        onQuickRestock = { prod -> movementTarget = prod to "IN" },
                                        onQuickConsume = { prod -> movementTarget = prod to "OUT" },
                                        onLoadLowStockToCart = { viewModel.loadLowStockIntoCart(current.products) }
                                    )

                                    AppTab.PRODUCTS -> ProductsScreen(
                                        products = current.products,
                                        categories = current.categories,
                                        filters = productFilters,
                                        onFiltersChange = viewModel::updateProductFilters,
                                        onNewProduct = {
                                            editingProduct = null
                                            showProductDialog = true
                                        },
                                        onEditProduct = { prod ->
                                            editingProduct = prod
                                            showProductDialog = true
                                        },
                                        onDeleteProduct = viewModel::deleteProduct,
                                        onOpenMovement = { prod, type -> movementTarget = prod to type }
                                    )

                                    AppTab.MARKET -> MarketModeScreen(
                                        cartItems = cartItems,
                                        cartTotals = cartTotals,
                                        storeName = storeName,
                                        purchaseNotes = purchaseNotes,
                                        isSubmitting = isSubmittingPurchase,
                                        scanFeedback = scanFeedback,
                                        finishedSummary = finishedSummary,
                                        products = current.products,
                                        categories = current.categories,
                                        onStoreNameChange = viewModel::updateStoreName,
                                        onPurchaseNotesChange = viewModel::updatePurchaseNotes,
                                        onOpenScanner = { showScannerForMarket = true },
                                        onOpenNfeImport = { showNfeDialog = true },
                                        onLoadLowStock = { viewModel.loadLowStockIntoCart(current.products) },
                                        onAddProductToCart = { prod -> viewModel.addProductToCart(prod) },
                                        onAddCustomItem = { text -> viewModel.addCustomCartItem(text, current.categories) },
                                        onUpdateCartItem = viewModel::updateCartItem,
                                        onRemoveCartItem = viewModel::removeCartItem,
                                        onSubmitPurchase = { viewModel.submitCartPurchase(current.products, current.categories) },
                                        onDismissScanFeedback = viewModel::dismissScanFeedback,
                                        onDismissFinishedSummary = viewModel::dismissFinishedSummary
                                    )

                                    AppTab.FINANCE -> FinancialScreen(
                                        financial = financial,
                                        purchases = current.purchases,
                                        filters = financialFilters,
                                        onFiltersChange = viewModel::updateFinancialFilters
                                    )

                                    AppTab.MOVEMENTS -> MovementsScreen(
                                        movements = current.movements
                                    )

                                    AppTab.CATEGORIES -> CategoriesScreen(
                                        categories = current.categories,
                                        products = current.products,
                                        onSaveCategory = viewModel::saveCategory,
                                        onDeleteCategory = viewModel::deleteCategory
                                    )
                                }

                                if (showProductDialog) {
                                    ProductDialog(
                                        existingProduct = editingProduct,
                                        categories = current.categories,
                                        onSave = viewModel::saveProduct,
                                        onDismiss = {
                                            showProductDialog = false
                                            editingProduct = null
                                        }
                                    )
                                }

                                movementTarget?.let { (targetProduct, type) ->
                                    MovementDialog(
                                        product = targetProduct,
                                        initialType = type,
                                        onConfirm = { mType, qty, reason, onDone ->
                                            viewModel.recordMovement(targetProduct, mType, qty, reason, onDone)
                                        },
                                        onDismiss = { movementTarget = null }
                                    )
                                }

                                if (showScannerForMarket) {
                                    BarcodeScannerDialog(
                                        onBarcodeDetected = { code ->
                                            showScannerForMarket = false
                                            viewModel.addCartItemByBarcode(code, current.products, current.categories)
                                        },
                                        onDismiss = { showScannerForMarket = false }
                                    )
                                }

                                if (showNfeDialog) {
                                    NfeImportDialog(
                                        existingProducts = current.products,
                                        categories = current.categories,
                                        isSubmitting = isSubmittingPurchase,
                                        onConfirmImport = { sName, notes, items, onDone ->
                                            viewModel.importParsedReceipt(
                                                storeName = sName,
                                                notes = notes,
                                                parsedItems = items,
                                                products = current.products,
                                                categories = current.categories,
                                                onDone = onDone
                                            )
                                        },
                                        onDismiss = { showNfeDialog = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
