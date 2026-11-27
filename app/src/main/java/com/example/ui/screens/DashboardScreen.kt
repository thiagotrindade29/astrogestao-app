package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
<<<<<<< HEAD
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
=======
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
>>>>>>> origin/main
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
<<<<<<< HEAD
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.ui.AppTab
import com.example.ui.DashboardComputed
import com.example.ui.components.AstroCard
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.KpiCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalAstroColors
import com.example.util.daysUntilExpiration
import com.example.util.formatCurrency
import com.example.util.formatIsoDate
import com.example.util.formatNumber
import com.example.util.formatTimestamp

@Composable
fun DashboardScreen(
    dashboard: DashboardComputed,
    onNavigateTab: (AppTab, Boolean) -> Unit,
    onOpenNfeImport: () -> Unit,
    onQuickRestock: (Product) -> Unit,
    onQuickConsume: (Product) -> Unit,
    onLoadLowStockToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val astroColors = LocalAstroColors.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = astroColors.spaceDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Vai ao mercado?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Some seus itens antes do caixa, leia o código de barras e compare se o preço subiu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD8CFF8)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigateTab(AppTab.MARKET, false) },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("dashboard_open_market_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = astroColors.primary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Modo Mercado", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = onOpenNfeImport,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("dashboard_open_nfe_button"),
                            border = BorderStroke(1.dp, Color(0xFF9F88E8)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cupom / NF-e", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 600.dp
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiCard(
                            title = "Produtos em Casa",
                            value = dashboard.totalProducts.toString(),
                            subtitle = "Itens cadastrados",
                            icon = Icons.Outlined.Inventory2,
                            accentColor = astroColors.primary,
                            accentBg = astroColors.primary.copy(alpha = 0.14f),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateTab(AppTab.PRODUCTS, false) }
                        )
                        KpiCard(
                            title = "Unidades Totais",
                            value = formatNumber(dashboard.totalUnits),
                            subtitle = "Na despensa e geladeira",
                            icon = Icons.Outlined.SwapVert,
                            accentColor = astroColors.info,
                            accentBg = astroColors.infoBg,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "Valor em Estoque",
                            value = formatCurrency(dashboard.totalValue),
                            subtitle = "Baseado no último preço",
                            icon = Icons.Outlined.AttachMoney,
                            accentColor = astroColors.success,
                            accentBg = astroColors.successBg,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateTab(AppTab.FINANCE, false) }
                        )
                        KpiCard(
                            title = "Faltando / Repor",
                            value = dashboard.lowStockProducts.size.toString(),
                            subtitle = "Abaixo do mínimo",
                            icon = Icons.Outlined.WarningAmber,
                            accentColor = astroColors.warning,
                            accentBg = astroColors.warningBg,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateTab(AppTab.PRODUCTS, true) }
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiCard(
                                title = "Produtos em Casa",
                                value = dashboard.totalProducts.toString(),
                                subtitle = "Cadastrados",
                                icon = Icons.Outlined.Inventory2,
                                accentColor = astroColors.primary,
                                accentBg = astroColors.primary.copy(alpha = 0.14f),
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateTab(AppTab.PRODUCTS, false) }
                            )
                            KpiCard(
                                title = "Unidades",
                                value = formatNumber(dashboard.totalUnits),
                                subtitle = "Em casa",
                                icon = Icons.Outlined.SwapVert,
                                accentColor = astroColors.info,
                                accentBg = astroColors.infoBg,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiCard(
                                title = "Valor em Estoque",
                                value = formatCurrency(dashboard.totalValue),
                                subtitle = "Estimado",
                                icon = Icons.Outlined.AttachMoney,
                                accentColor = astroColors.success,
                                accentBg = astroColors.successBg,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateTab(AppTab.FINANCE, false) }
                            )
                            KpiCard(
                                title = "Para Repor",
                                value = dashboard.lowStockProducts.size.toString(),
                                subtitle = "Abaixo do mínimo",
                                icon = Icons.Outlined.WarningAmber,
                                accentColor = astroColors.warning,
                                accentBg = astroColors.warningBg,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateTab(AppTab.PRODUCTS, true) }
                            )
=======
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ProductEntity
import com.example.ui.components.AlertRedKpiCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MovementTypeBadge
import com.example.ui.components.SolidKpiCard
import com.example.ui.components.formatBrl
import com.example.ui.components.formatDateTimeBr
import com.example.ui.components.formatQty
import com.example.ui.theme.AstroTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.AstroUiState

@Composable
fun DashboardScreen(
    uiState: AstroUiState,
    onOpenCatalog: () -> Unit,
    onOpenMarket: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenNfceImport: () -> Unit,
    onNewProduct: () -> Unit,
    onQuickRestock: (ProductEntity) -> Unit,
    onLoadSampleCatalog: () -> Unit,
    onOpenUpdate: (String) -> Unit = {},
    onDismissUpdate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val extended = AstroTheme.extended

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Ver Catálogo, Importar Cupom / NF-e, and + Novo Produto
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 640.dp
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.dashboard_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.dashboard_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onOpenCatalog,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.testTag("dashboard_view_catalog_button")
                            ) {
                                Text(stringResource(R.string.action_view_catalog))
                            }
                            OutlinedButton(
                                onClick = onOpenNfceImport,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                                modifier = Modifier.testTag("dashboard_import_nfce_button")
                            ) {
                                Icon(Icons.Rounded.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.action_import_nfce))
                            }
                            Button(
                                onClick = onNewProduct,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.testTag("dashboard_new_product_button")
                            ) {
                                Text(stringResource(R.string.action_new_product))
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.dashboard_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.dashboard_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenCatalog,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("dashboard_view_catalog_button")
                            ) {
                                Text(stringResource(R.string.action_view_catalog))
                            }
                            Button(
                                onClick = onNewProduct,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("dashboard_new_product_button")
                            ) {
                                Text(stringResource(R.string.action_new_product))
                            }
                        }
                        OutlinedButton(
                            onClick = onOpenNfceImport,
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("dashboard_import_nfce_button")
                        ) {
                            Icon(Icons.Rounded.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.action_import_nfce))
>>>>>>> origin/main
                        }
                    }
                }
            }
        }

<<<<<<< HEAD
        // Lista de Compras (Reposição)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lista de Compras (Reposição)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = astroColors.textPrimary
                )
                if (dashboard.lowStockProducts.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            onLoadLowStockToCart()
                            onNavigateTab(AppTab.MARKET, false)
                        },
                        modifier = Modifier.testTag("dashboard_send_low_stock_market")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Levar ao Mercado")
=======
        // Firestore live sync & active household status bar
        item {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, extended.borderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_cloud_status_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDone,
                            contentDescription = stringResource(R.string.dashboard_cloud_synced),
                            tint = extended.positive,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = buildString {
                                append(uiState.householdName.ifBlank { "Meu Espaço" })
                                append(" • ")
                                append(stringResource(R.string.dashboard_cloud_synced))
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = extended.positiveContainer
                    ) {
                        Text(
                            text = "${uiState.memberCount} membro(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = extended.positive,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
>>>>>>> origin/main
                    }
                }
            }
        }

<<<<<<< HEAD
        if (dashboard.lowStockProducts.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Outlined.CheckCircle,
                    title = "Tudo abastecido em casa!",
                    subtitle = "Nenhum item está abaixo da quantidade mínima."
                )
            }
        } else {
            items(dashboard.lowStockProducts.take(6), key = { "low_${it.id}" }) { product ->
                AstroCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = astroColors.textPrimary
                            )
                            Text(
                                text = "${product.location} • Atual: ${formatNumber(product.quantity)} ${product.unit} (mín. ${formatNumber(product.minQuantity)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = astroColors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = { onQuickRestock(product) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("quick_restock_${product.id}")
                        ) {
                            Text("+ Repor")
                        }
                    }
                }
            }
        }

        // Próximos do vencimento
        item {
            Text(
                text = "Próximos do Vencimento (30 dias)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = astroColors.textPrimary
            )
        }

        if (dashboard.expiringSoonProducts.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Outlined.EventBusy,
                    title = "Sem alertas de validade",
                    subtitle = "Nenhum item vence nos próximos 30 dias."
                )
            }
        } else {
            items(dashboard.expiringSoonProducts.take(5), key = { "exp_${it.id}" }) { product ->
                val days = daysUntilExpiration(product.expirationDate) ?: 0L
                val isExpired = days < 0
                AstroCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = astroColors.textPrimary
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusPill(
                                    text = if (isExpired) "Vencido" else "Vence em $days dias",
                                    color = if (isExpired) astroColors.danger else astroColors.warning,
                                    backgroundColor = if (isExpired) astroColors.dangerBg else astroColors.warningBg
                                )
                                Text(
                                    text = formatIsoDate(product.expirationDate),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = astroColors.textMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { onQuickConsume(product) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text("- Consumir")
=======
        // Firestore remote app version update notification banner
        val updateInfo = uiState.appUpdateInfo
        if (updateInfo != null && updateInfo.isUpdateAvailable && !updateInfo.isDismissed) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_update_banner"),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Atualização disponível • v${updateInfo.latestVersionName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = buildString {
                                        append("Versão instalada: v${updateInfo.currentVersionName}")
                                        if (updateInfo.releaseNotes.isNotBlank()) {
                                            append(" • ")
                                            append(updateInfo.releaseNotes)
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!updateInfo.isBelowMinimum) {
                                TextButton(
                                    onClick = onDismissUpdate,
                                    modifier = Modifier.testTag("dismiss_update_banner_button")
                                ) {
                                    Text("Depois")
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Button(
                                onClick = { onOpenUpdate(updateInfo.downloadUrl) },
                                modifier = Modifier.testTag("open_update_button")
                            ) {
                                Text(
                                    if (updateInfo.downloadUrl.startsWith("https://", ignoreCase = true)) {
                                        "Atualizar agora"
                                    } else {
                                        "Ver atualização"
                                    }
                                )
                            }
>>>>>>> origin/main
                        }
                    }
                }
            }
        }

<<<<<<< HEAD
        // Últimas movimentações
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Últimas Movimentações",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = astroColors.textPrimary
                )
                TextButton(onClick = { onNavigateTab(AppTab.MOVEMENTS, false) }) {
                    Text("Ver todas")
=======
        // Empty catalog onboarding option
        if (uiState.products.isEmpty() && !uiState.isLoading) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Inventory2,
                    title = stringResource(R.string.products_empty_title),
                    body = stringResource(R.string.products_empty_body),
                    primaryActionLabel = stringResource(R.string.action_load_sample_catalog),
                    onPrimaryAction = onLoadSampleCatalog,
                    secondaryActionLabel = stringResource(R.string.action_import_nfce),
                    onSecondaryAction = onOpenNfceImport,
                    testTag = "dashboard_empty_state"
                )
            }
        }

        // 4 KPI Cards: Total de Produtos, Valor Estimado Total, Itens em Reposição, Alertas de Vencimento Próximo
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isFourCol = maxWidth >= 760.dp
                val isTwoCol = maxWidth >= 420.dp

                val card1: @Composable (Modifier) -> Unit = { mod ->
                    SolidKpiCard(
                        label = stringResource(R.string.kpi_registered_products),
                        value = uiState.registeredProductCount.toString(),
                        subtitle = stringResource(R.string.kpi_registered_sub, formatQty(uiState.totalUnitsInStock)),
                        containerColor = extended.kpiNavy,
                        contentColor = extended.kpiNavyContent,
                        modifier = mod,
                        testTag = "kpi_products_count"
                    )
                }
                val card2: @Composable (Modifier) -> Unit = { mod ->
                    SolidKpiCard(
                        label = stringResource(R.string.kpi_estimated_value),
                        value = formatBrl(uiState.totalEstimatedValue),
                        subtitle = stringResource(R.string.kpi_estimated_sub),
                        containerColor = extended.kpiEmerald,
                        contentColor = extended.kpiEmeraldContent,
                        modifier = mod,
                        testTag = "kpi_estimated_value"
                    )
                }
                val card3: @Composable (Modifier) -> Unit = { mod ->
                    SolidKpiCard(
                        label = stringResource(R.string.kpi_restock_alerts),
                        value = uiState.restockAlertsCount.toString(),
                        subtitle = stringResource(R.string.kpi_restock_sub),
                        containerColor = extended.kpiBlue,
                        contentColor = extended.kpiBlueContent,
                        modifier = mod,
                        testTag = "kpi_restock_count"
                    )
                }
                val card4: @Composable (Modifier) -> Unit = { mod ->
                    AlertRedKpiCard(
                        label = stringResource(R.string.kpi_expiry_alerts),
                        value = uiState.expiringIn30Days.size.toString(),
                        subtitle = stringResource(R.string.kpi_expiring_sub),
                        modifier = mod,
                        testTag = "kpi_expiry_alerts"
                    )
                }

                when {
                    isFourCol -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            card1(Modifier.weight(1f))
                            card2(Modifier.weight(1f))
                            card3(Modifier.weight(1f))
                            card4(Modifier.weight(1f))
                        }
                    }
                    isTwoCol -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                card1(Modifier.weight(1f))
                                card2(Modifier.weight(1f))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                card3(Modifier.weight(1f))
                                card4(Modifier.weight(1f))
                            }
                        }
                    }
                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            card1(Modifier.fillMaxWidth())
                            card2(Modifier.fillMaxWidth())
                            card3(Modifier.fillMaxWidth())
                            card4(Modifier.fillMaxWidth())
                        }
                    }
>>>>>>> origin/main
                }
            }
        }

<<<<<<< HEAD
        if (dashboard.recentMovements.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Outlined.History,
                    title = "Nenhuma movimentação ainda",
                    subtitle = "Suas entradas e consumos aparecem aqui."
                )
            }
        } else {
            items(dashboard.recentMovements.take(5), key = { "dash_mov_${it.id}" }) { mov ->
                val isIn = mov.type == "IN"
                val isOut = mov.type == "OUT"
                AstroCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = mov.productName,
                                style = MaterialTheme.typography.titleSmall,
                                color = astroColors.textPrimary
                            )
                            Text(
                                text = "${mov.reason ?: "Movimentação"} • ${formatTimestamp(mov.createdAt?.toDate())}",
                                style = MaterialTheme.typography.bodySmall,
                                color = astroColors.textMuted
                            )
                        }
                        StatusPill(
                            text = when {
                                isIn -> "+${formatNumber(mov.quantity)} ${mov.productUnit}"
                                isOut -> "-${formatNumber(mov.quantity)} ${mov.productUnit}"
                                else -> "=${formatNumber(mov.newQuantity)} ${mov.productUnit}"
                            },
                            color = when {
                                isIn -> astroColors.success
                                isOut -> astroColors.danger
                                else -> astroColors.info
                            },
                            backgroundColor = when {
                                isIn -> astroColors.successBg
                                isOut -> astroColors.dangerBg
                                else -> astroColors.infoBg
                            }
                        )
=======
        // Restock & Expiry Panels (side-by-side on wide screens, stacked on phones)
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val sideBySide = maxWidth >= 700.dp
                if (sideBySide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        RestockListCard(
                            uiState = uiState,
                            onSeeAll = onOpenMarket,
                            onQuickRestock = onQuickRestock,
                            modifier = Modifier.weight(1f)
                        )
                        ExpiringSoonCard(
                            uiState = uiState,
                            onSeeCatalog = onOpenCatalog,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        RestockListCard(
                            uiState = uiState,
                            onSeeAll = onOpenMarket,
                            onQuickRestock = onQuickRestock,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ExpiringSoonCard(
                            uiState = uiState,
                            onSeeCatalog = onOpenCatalog,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Recent Audited Movements Section
        if (uiState.movements.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_recent_movements_card"),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, extended.borderSubtle)
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
                            Text(
                                text = stringResource(R.string.section_recent_movements),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onOpenHistory) {
                                Text(stringResource(R.string.action_see_history))
                            }
                        }

                        uiState.movements.take(4).forEach { mv ->
                            val sign = if (mv.quantityDelta >= 0) "+" else ""
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            MovementTypeBadge(type = mv.type)
                                            Text(
                                                text = mv.productNameSnapshot,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "${mv.reason} • ${formatDateTimeBr(mv.timestampMillis)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$sign${formatQty(mv.quantityDelta)} ${mv.productUnitSnapshot}",
                                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = formatBrl(mv.totalValue),
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
>>>>>>> origin/main
                    }
                }
            }
        }
    }
}
