package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.AstroGestaoApplication
import com.example.R
import com.example.data.local.CategoryEntity
import com.example.data.local.MovementType
import com.example.data.local.ProductEntity
import com.example.data.remote.authStateFlow
import com.example.data.remote.performGoogleSignOut
import com.example.ui.components.AstroBrandLogo
import com.example.ui.screens.CategoryEditorDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FinanceScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MarketModeScreen
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.MovementEditorDialog
import com.example.ui.screens.NfceImportScreen
import com.example.ui.screens.ProductEditorDialog
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.UserEditorDialog
import com.example.ui.theme.AstroGestaoTheme
import com.example.ui.theme.AstroTheme
import com.example.ui.viewmodel.AstroViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object AstroRoutes {
    const val ROUTE_HOME = "home"
    const val ROUTE_PRODUCTS = "products"
    const val ROUTE_MARKET = "market"
    const val ROUTE_FINANCE = "finance"
    const val ROUTE_MORE = "more"
    const val ROUTE_NFCE_IMPORT = "nfce_import"
}

private enum class TopTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME(
        route = AstroRoutes.ROUTE_HOME,
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Rounded.GridView,
        unselectedIcon = Icons.Outlined.GridView,
        testTag = "nav_home"
    ),
    PRODUCTS(
        route = AstroRoutes.ROUTE_PRODUCTS,
        labelRes = R.string.nav_products,
        selectedIcon = Icons.Rounded.Inventory2,
        unselectedIcon = Icons.Outlined.Inventory2,
        testTag = "nav_products"
    ),
    MARKET(
        route = AstroRoutes.ROUTE_MARKET,
        labelRes = R.string.nav_market,
        selectedIcon = Icons.Rounded.ShoppingCart,
        unselectedIcon = Icons.Outlined.ShoppingCart,
        testTag = "nav_market"
    ),
    FINANCE(
        route = AstroRoutes.ROUTE_FINANCE,
        labelRes = R.string.nav_finance,
        selectedIcon = Icons.Rounded.AttachMoney,
        unselectedIcon = Icons.Outlined.AttachMoney,
        testTag = "nav_finance"
    ),
    MORE(
        route = AstroRoutes.ROUTE_MORE,
        labelRes = R.string.nav_more,
        selectedIcon = Icons.Rounded.Menu,
        unselectedIcon = Icons.Outlined.Menu,
        testTag = "nav_more"
    )
}

private sealed interface ActiveDialog {
    data class ProductForm(val product: ProductEntity? = null) : ActiveDialog
    data class MovementForm(
        val product: ProductEntity? = null,
        val type: MovementType = MovementType.ENTRADA
    ) : ActiveDialog

    data class CategoryForm(val category: CategoryEntity? = null) : ActiveDialog
    data object UserForm : ActiveDialog
}

@Composable
fun AstroGestaoApp(app: AstroGestaoApplication) {
    val auth = Firebase.auth
    val firebaseUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val isSessionUnlocked by app.isSessionUnlocked.collectAsStateWithLifecycle()
    val sessionNotice by app.sessionNotice.collectAsStateWithLifecycle()
    val isDarkMode by app.preferencesRepository.isDarkMode.collectAsStateWithLifecycle(initialValue = false)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    // Sempre iniciar na tela de login ao abrir o aplicativo:
    // limpa qualquer sessão persistida antes que listeners do Firestore sejam ativados.
    LaunchedEffect(isSessionUnlocked) {
        if (!isSessionUnlocked && auth.currentUser != null) {
            withContext(NonCancellable) {
                runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
                auth.signOut()
            }
        }
    }

    AstroGestaoTheme(darkTheme = isDarkMode) {
        val currentUser = firebaseUser
        if (currentUser == null || !isSessionUnlocked) {
            LoginScreen(
                onAuthSuccess = {
                    app.unlockSession()
                },
                sessionNotice = sessionNotice,
                onDismissNotice = app::clearSessionNotice
            )
        } else {
            val uid = currentUser.uid
            val email = currentUser.email.orEmpty()
            val displayName = currentUser.displayName
                ?.takeIf { it.isNotBlank() }
                ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }.ifBlank { "Morador" }

            key(uid) {
                val viewModel: AstroViewModel = viewModel(
                    key = uid,
                    factory = AstroViewModel.Factory(
                        repository = app.cloudRepository,
                        prefs = app.preferencesRepository,
                        localDb = app.database,
                        currentUserId = uid,
                        userEmail = email,
                        userName = displayName
                    )
                )
                AuthenticatedAstroContent(
                    app = app,
                    viewModel = viewModel,
                    onSignOut = { notice ->
                        app.lockSession(notice = notice)
                        performGoogleSignOut(
                            credentialManager = credentialManager,
                            scope = scope
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedAstroContent(
    app: AstroGestaoApplication,
    viewModel: AstroViewModel,
    onSignOut: (notice: String?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nfceState by viewModel.nfceState.collectAsStateWithLifecycle()
    val posState by viewModel.posState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    val timeoutNotice = stringResource(R.string.session_expired_inactivity)

    LaunchedEffect(viewModel) {
        while (true) {
            delay(15_000L)
            if (app.isSessionTimedOut()) {
                onSignOut(timeoutNotice)
                break
            }
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (app.isSessionTimedOut()) {
                    onSignOut(timeoutNotice)
                } else {
                    app.recordUserActivity()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val onUserSignOut: () -> Unit = { onSignOut(null) }

    if (uiState.cloudErrorMessage != null && uiState.householdId.isBlank()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.widthIn(max = 420.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = "Não foi possível carregar seu espaço na nuvem",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = uiState.cloudErrorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = viewModel::initializeCloudWorkspace) {
                        Text("Tentar Novamente")
                    }
                    OutlinedButton(onClick = onUserSignOut) {
                        Text(stringResource(R.string.action_logout))
                    }
                }
            }
        }
        return
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Sincronizando seu espaço na nuvem...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    var activeDialog by remember { mutableStateOf<ActiveDialog?>(null) }
    var moreInitialTab by remember { mutableIntStateOf(0) }

    val isTopLevel = currentRoute in TopTab.entries.map { it.route }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    when (val dialog = activeDialog) {
        is ActiveDialog.ProductForm -> {
            ProductEditorDialog(
                initialProduct = dialog.product,
                categories = uiState.categories,
                onSave = {
                    viewModel.saveProduct(it)
                    activeDialog = null
                },
                onDismiss = { activeDialog = null }
            )
        }

        is ActiveDialog.MovementForm -> {
            MovementEditorDialog(
                products = uiState.products,
                preselectedProduct = dialog.product,
                preselectedType = dialog.type,
                onConfirm = { productId, type, amountOrBalance, unitPrice, reason ->
                    viewModel.registerMovement(productId, type, amountOrBalance, unitPrice, reason)
                    activeDialog = null
                },
                onDismiss = { activeDialog = null }
            )
        }

        is ActiveDialog.CategoryForm -> {
            CategoryEditorDialog(
                initialCategory = dialog.category,
                onSave = {
                    viewModel.saveCategory(it)
                    activeDialog = null
                },
                onDismiss = { activeDialog = null }
            )
        }

        ActiveDialog.UserForm -> {
            UserEditorDialog(
                onSave = { name, email, role, password ->
                    viewModel.addUser(name, email, role, password)
                    activeDialog = null
                },
                onDismiss = { activeDialog = null }
            )
        }

        null -> Unit
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 640.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (!useRail && isTopLevel) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(
                                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                                    )
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AstroBrandLogo(sizeDp = 36)
                                    Column {
                                        Text(
                                            text = stringResource(R.string.app_name),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = uiState.householdName.ifBlank { stringResource(R.string.brand_subtitle) },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { navController.navigate(AstroRoutes.ROUTE_NFCE_IMPORT) },
                                    modifier = Modifier.testTag("topbar_nfce_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ReceiptLong,
                                        contentDescription = stringResource(R.string.action_import_nfce),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            HorizontalDivider(color = AstroTheme.extended.borderSubtle)
                        }
                    }
                }
            },
            bottomBar = {
                if (!useRail && isTopLevel) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        TopTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTab(tab.route) },
                                icon = {
                                    val iconContent: @Composable () -> Unit = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    }
                                    if (tab == TopTab.PRODUCTS && uiState.registeredProductCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge {
                                                    Text(uiState.registeredProductCount.toString())
                                                }
                                            }
                                        ) {
                                            iconContent()
                                        }
                                    } else {
                                        iconContent()
                                    }
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentRoute == AstroRoutes.ROUTE_PRODUCTS) {
                    FloatingActionButton(
                        onClick = { activeDialog = ActiveDialog.ProductForm() },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("products_fab")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_new_product))
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && isTopLevel) {
                    NavigationRail(
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        containerColor = MaterialTheme.colorScheme.surface,
                        header = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                AstroBrandLogo(sizeDp = 38)
                                Spacer(modifier = Modifier.height(8.dp))
                                IconButton(
                                    onClick = { navController.navigate(AstroRoutes.ROUTE_NFCE_IMPORT) },
                                    modifier = Modifier.testTag("rail_nfce_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ReceiptLong,
                                        contentDescription = stringResource(R.string.action_import_nfce),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    ) {
                        TopTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTab(tab.route) },
                                icon = {
                                    if (tab == TopTab.PRODUCTS && uiState.registeredProductCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge {
                                                    Text(uiState.registeredProductCount.toString())
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = stringResource(tab.labelRes)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    }
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = AstroRoutes.ROUTE_HOME,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1040.dp)
                    ) {
                        composable(AstroRoutes.ROUTE_HOME) {
                            DashboardScreen(
                                uiState = uiState,
                                onOpenCatalog = { navigateTab(AstroRoutes.ROUTE_PRODUCTS) },
                                onOpenMarket = { navigateTab(AstroRoutes.ROUTE_MARKET) },
                                onOpenHistory = {
                                    moreInitialTab = 0
                                    navigateTab(AstroRoutes.ROUTE_MORE)
                                },
                                onOpenNfceImport = {
                                    navController.navigate(AstroRoutes.ROUTE_NFCE_IMPORT)
                                },
                                onNewProduct = { activeDialog = ActiveDialog.ProductForm() },
                                onQuickRestock = { product ->
                                    activeDialog = ActiveDialog.MovementForm(
                                        product = product,
                                        type = MovementType.ENTRADA
                                    )
                                },
                                onLoadSampleCatalog = viewModel::loadSampleCatalog
                            )
                        }

                        composable(AstroRoutes.ROUTE_PRODUCTS) {
                            ProductsScreen(
                                uiState = uiState,
                                onSearchChange = viewModel::updateProductSearch,
                                onSelectCategory = viewModel::selectCategoryFilter,
                                onToggleLowStock = viewModel::toggleLowStockOnly,
                                onCreateProduct = { activeDialog = ActiveDialog.ProductForm() },
                                onEditProduct = { product ->
                                    activeDialog = ActiveDialog.ProductForm(product = product)
                                },
                                onDeleteProduct = viewModel::deleteProduct,
                                onQuickMovement = { product, type ->
                                    activeDialog = ActiveDialog.MovementForm(product = product, type = type)
                                }
                            )
                        }

                        composable(AstroRoutes.ROUTE_MARKET) {
                            MarketModeScreen(
                                uiState = uiState,
                                posState = posState,
                                onStoreNameChange = viewModel::updatePosStoreName,
                                onNotesChange = viewModel::updatePosNotes,
                                onSearchQueryChange = viewModel::updatePosSearchQuery,
                                onToggleSaveToStock = viewModel::togglePosSaveToStock,
                                onBarcodeScanned = viewModel::onBarcodeScannedInMarket,
                                onAddExistingProduct = { prod ->
                                    viewModel.addProductToPosCart(prod)
                                },
                                onLinkCodeToExisting = viewModel::linkScannedCodeToExistingProduct,
                                onCreateOrResolveNewItem = viewModel::resolveOrAddNewItemToPosCart,
                                onDismissUnmatchedCode = viewModel::dismissUnmatchedCode,
                                onLoadLowStock = viewModel::loadLowStockIntoPosCart,
                                onUpdateItemQty = viewModel::updatePosItemQuantity,
                                onUpdateItemPrice = viewModel::updatePosItemPrice,
                                onRemoveItem = viewModel::removePosItem,
                                onFinalizeCheckout = viewModel::finalizePosCheckout,
                                onOpenNfceImport = {
                                    navController.navigate(AstroRoutes.ROUTE_NFCE_IMPORT)
                                }
                            )
                        }

                        composable(AstroRoutes.ROUTE_FINANCE) {
                            FinanceScreen(
                                uiState = uiState,
                                onOpenCatalog = { navigateTab(AstroRoutes.ROUTE_PRODUCTS) }
                            )
                        }

                        composable(AstroRoutes.ROUTE_MORE) {
                            val appContext = LocalContext.current
                            MoreScreen(
                                uiState = uiState,
                                initialTab = moreInitialTab,
                                onSelectMovementFilter = viewModel::selectMovementFilter,
                                onNewCategory = { activeDialog = ActiveDialog.CategoryForm() },
                                onEditCategory = { cat ->
                                    activeDialog = ActiveDialog.CategoryForm(category = cat)
                                },
                                onDeleteCategory = viewModel::deleteCategory,
                                onNewUser = { activeDialog = ActiveDialog.UserForm },
                                onDeleteUser = viewModel::deleteUser,
                                onRenameHousehold = viewModel::renameHousehold,
                                onRegenerateInviteCode = viewModel::regenerateInviteCode,
                                onJoinHousehold = viewModel::joinHouseholdByCode,
                                onCreatePersonalHousehold = viewModel::createNewPersonalHousehold,
                                onMigrateLocalToCloud = viewModel::migrateLocalDataToCloud,
                                onToggleDarkMode = viewModel::setDarkMode,
                                onLoadSampleCatalog = viewModel::loadSampleCatalog,
                                onExportBackup = { uri -> viewModel.exportBackupToUri(appContext, uri) },
                                onRestoreBackup = { uri -> viewModel.importBackupFromUri(appContext, uri) },
                                onBuildDiagnosticReport = viewModel::buildDiagnosticReport,
                                onSignOut = onUserSignOut
                            )
                        }

                        composable(AstroRoutes.ROUTE_NFCE_IMPORT) {
                            val appContext = LocalContext.current
                            NfceImportScreen(
                                uiState = uiState,
                                nfceState = nfceState,
                                onBack = { navController.popBackStack() },
                                onSelectTab = viewModel::selectNfceTab,
                                onSelectPortal = viewModel::selectRegionalPortal,
                                onInputChange = viewModel::updateNfceInput,
                                onRawTextChange = viewModel::updateRawReceiptTextInput,
                                onParseRawText = viewModel::parsePastedReceiptText,
                                onProcessReceiptImage = { uri ->
                                    viewModel.processReceiptImage(appContext, uri)
                                },
                                onConsultInput = viewModel::consultNfceInput,
                                onOpenPortal = viewModel::openRegionalPortal,
                                onDismissWebPortal = viewModel::dismissWebPortal,
                                onXmlLoaded = viewModel::parseNfceXmlContent,
                                onWebViewExtracted = { raw, isAuto ->
                                    viewModel.applyWebViewExtractedJson(raw, isAuto)
                                },
                                onToggleItem = viewModel::toggleParsedItem,
                                onSelectItemCategory = viewModel::updateParsedItemCategory,
                                onConfirmImport = {
                                    viewModel.confirmNfceImport {
                                        navController.popBackStack()
                                    }
                                },
                                onDiscardImport = viewModel::clearNfceImport
                            )
                        }
                    }
                }
            }
        }
    }
}
