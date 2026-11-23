package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Purchase
import com.example.ui.FinancialComputed
import com.example.ui.FinancialFilters
import com.example.ui.PriceVariationFilter
import com.example.ui.components.AstroCard
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.KpiCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalAstroColors
import com.example.util.formatCurrency
import com.example.util.formatNumber
import com.example.util.formatTimestamp
import com.example.util.parseHexColor
import kotlin.math.abs

@Composable
fun FinancialScreen(
    financial: FinancialComputed,
    purchases: List<Purchase>,
    filters: FinancialFilters,
    onFiltersChange: (FinancialFilters) -> Unit,
    modifier: Modifier = Modifier
) {
    val astroColors = LocalAstroColors.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "Gasto no mês",
                    value = formatCurrency(financial.monthTotalSpent),
                    subtitle = "${financial.monthPurchaseCount} compras registradas",
                    icon = Icons.Outlined.AttachMoney,
                    accentColor = astroColors.primary,
                    accentBg = astroColors.primary.copy(alpha = 0.14f),
                    modifier = Modifier.weight(1f)
                )
                val isUp = financial.avgVariationPct > 0.2
                val isDown = financial.avgVariationPct < -0.2
                KpiCard(
                    title = "Variação média",
                    value = "${if (financial.avgVariationPct >= 0) "+" else ""}${String.format("%.1f", financial.avgVariationPct)}%",
                    subtitle = "↑${financial.itemsUp}  =${financial.itemsSame}  ↓${financial.itemsDown}",
                    icon = if (isDown) Icons.AutoMirrored.Outlined.TrendingDown else Icons.AutoMirrored.Outlined.TrendingUp,
                    accentColor = when {
                        isUp -> astroColors.danger
                        isDown -> astroColors.success
                        else -> astroColors.info
                    },
                    accentBg = when {
                        isUp -> astroColors.dangerBg
                        isDown -> astroColors.successBg
                        else -> astroColors.infoBg
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Gastos por Categoria
        item {
            Text(
                text = "Gastos por Categoria",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = astroColors.textPrimary
            )
        }

        if (financial.spendingByCategory.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Outlined.Analytics,
                    title = "Sem compras registradas ainda",
                    subtitle = "Finalize uma compra no Modo Mercado ou importe um cupom fiscal para ver sua distribuição de gastos."
                )
            }
        } else {
            item {
                AstroCard {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        financial.spendingByCategory.forEach { cat ->
                            val color = parseHexColor(cat.categoryColor)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = cat.categoryName,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = astroColors.textPrimary
                                    )
                                    Text(
                                        text = "${formatCurrency(cat.totalSpent)} (${String.format("%.0f", cat.sharePct)}%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = astroColors.textSecondary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (cat.sharePct / 100.0).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = color,
                                    trackColor = astroColors.pageBackground
                                )
                            }
                        }
                    }
                }
            }
        }

        // Compras Realizadas (Histórico)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Histórico de Compras",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = astroColors.textPrimary
                )
                if (filters.selectedPurchaseId != null) {
                    TextButton(
                        onClick = { onFiltersChange(filters.copy(selectedPurchaseId = null)) }
                    ) {
                        Text("Mostrar todas")
                    }
                }
            }
        }

        if (purchases.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    purchases.take(12).forEach { pur ->
                        val selected = filters.selectedPurchaseId == pur.id
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onFiltersChange(
                                    filters.copy(
                                        selectedPurchaseId = if (selected) null else pur.id
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null)
                            },
                            label = {
                                Text("${pur.storeName} • ${formatCurrency(pur.totalAmount)} (${formatTimestamp(pur.createdAt?.toDate())})")
                            }
                        )
                    }
                }
            }
        }

        // Comparativo Item a Item
        item {
            Text(
                text = "Comparativo de Preços (Item a Item)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = astroColors.textPrimary
            )
        }

        item {
            OutlinedTextField(
                value = filters.searchTerm,
                onValueChange = { onFiltersChange(filters.copy(searchTerm = it)) },
                label = { Text("Filtrar por produto ou código...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("finance_search_input")
            )
        }

        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val options = listOf(
                    PriceVariationFilter.ALL to "Todos",
                    PriceVariationFilter.UP to "Subiram",
                    PriceVariationFilter.EQUAL to "Iguais",
                    PriceVariationFilter.DOWN to "Desceram"
                )
                options.forEach { (kind, label) ->
                    FilterChip(
                        selected = filters.variation == kind,
                        onClick = { onFiltersChange(filters.copy(variation = kind)) },
                        label = { Text(label) }
                    )
                }
            }
        }

        if (financial.filteredItems.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = "Nenhum item encontrado",
                    subtitle = "Altere os filtros acima ou faça uma nova compra no Modo Mercado."
                )
            }
        } else {
            items(financial.filteredItems, key = { it.id }) { item ->
                val diff = item.unitPrice - item.previousUnitPrice
                val pct = if (item.previousUnitPrice > 0.0) (diff / item.previousUnitPrice) * 100.0 else 0.0
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
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = item.productName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = astroColors.textPrimary
                            )
                            Text(
                                text = "Qtd: ${formatNumber(item.quantity)} • Pago: ${formatCurrency(item.unitPrice)} (antes: ${formatCurrency(item.previousUnitPrice)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = astroColors.textSecondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = formatCurrency(item.totalPrice),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = astroColors.textPrimary
                            )
                            when {
                                item.previousUnitPrice > 0.0 && diff > 0.005 -> StatusPill(
                                    text = "+${String.format("%.1f", pct)}%",
                                    color = astroColors.danger,
                                    backgroundColor = astroColors.dangerBg
                                )
                                item.previousUnitPrice > 0.0 && diff < -0.005 -> StatusPill(
                                    text = "-${String.format("%.1f", abs(pct))}%",
                                    color = astroColors.success,
                                    backgroundColor = astroColors.successBg
                                )
                                else -> StatusPill(
                                    text = "Igual",
                                    color = astroColors.textSecondary,
                                    backgroundColor = astroColors.pageBackground
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
