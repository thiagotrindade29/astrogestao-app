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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.components.LowStockBadge
import com.example.ui.components.MovementTypeBadge
import com.example.ui.components.SolidKpiCard
import com.example.ui.components.daysUntil
import com.example.ui.components.formatBrl
import com.example.ui.components.formatDateTimeBr
import com.example.ui.components.formatQty
import com.example.ui.components.formatShortDateBr
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
                                    .height(46.dp)
                                    .testTag("dashboard_view_catalog_button")
                            ) {
                                Text(stringResource(R.string.action_view_catalog))
                            }
                            Button(
                                onClick = onNewProduct,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
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
                                .height(46.dp)
                                .testTag("dashboard_import_nfce_button")
                        ) {
                            Icon(Icons.Rounded.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.action_import_nfce))
                        }
                    }
                }
            }
        }

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
                }
            }
        }

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
                    }
                }
            }
        }
    }
}

@Composable
private fun RestockListCard(
    uiState: AstroUiState,
    onSeeAll: () -> Unit,
    onQuickRestock: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = AstroTheme.extended
    val lowStock = uiState.lowStockProducts

    Card(
        modifier = modifier.testTag("restock_panel_card"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extended.borderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.section_restock_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.section_restock_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = onSeeAll,
                    modifier = Modifier.testTag("restock_see_all_button")
                ) {
                    Text(stringResource(R.string.action_see_all))
                }
            }

            if (lowStock.isEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, extended.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.restock_empty_msg),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            } else {
                lowStock.take(6).forEach { product ->
                    val catName = product.categoryId?.let { uiState.categoryMap[it]?.name } ?: "Sem categoria"
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, extended.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restock_item_${product.id}")
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
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    LowStockBadge()
                                }
                                Text(
                                    text = "$catName • Atual: ${formatQty(product.quantity)} ${product.unit} (mín: ${formatQty(product.minStock)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = { onQuickRestock(product) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, extended.positive.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .testTag("quick_restock_${product.id}")
                            ) {
                                Text(
                                    text = stringResource(R.string.action_restock_short),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = extended.positive
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpiringSoonCard(
    uiState: AstroUiState,
    modifier: Modifier = Modifier
) {
    val extended = AstroTheme.extended
    val expiring = uiState.expiringIn30Days

    Card(
        modifier = modifier.testTag("expiring_panel_card"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extended.borderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.section_expiring_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.section_expiring_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expiring.isEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, extended.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.expiring_empty_msg),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            } else {
                expiring.forEach { product ->
                    val expiryMillis = product.expiryDateMillis ?: return@forEach
                    val days = daysUntil(expiryMillis)
                    val isExpired = days < 0
                    val daysLabel = when {
                        isExpired -> "Vencido há ${-days}d"
                        days == 0 -> "Vence hoje"
                        else -> "Vence em $days d"
                    }
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (isExpired || days <= 7) extended.negativeContainer.copy(alpha = 0.45f)
                        else extended.warningContainer.copy(alpha = 0.45f),
                        border = BorderStroke(
                            1.dp,
                            if (isExpired || days <= 7) extended.negative.copy(alpha = 0.3f)
                            else extended.warning.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expiring_item_${product.id}")
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WarningAmber,
                                    contentDescription = daysLabel,
                                    tint = if (isExpired || days <= 7) extended.negative else extended.warning
                                )
                                Column {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Validade: ${formatShortDateBr(expiryMillis)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = daysLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isExpired || days <= 7) extended.negative else extended.warning,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
