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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
                        }
                    }
                }
            }
        }

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
                    }
                }
            }
        }

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
                        }
                    }
                }
            }
        }

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
                }
            }
        }

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
                    }
                }
            }
        }
    }
}
