package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
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
import com.example.ui.components.LowStockBadge
import com.example.ui.components.daysUntil
import com.example.ui.components.formatQty
import com.example.ui.components.formatShortDateBr
import com.example.ui.theme.AstroTheme
import com.example.ui.viewmodel.AstroUiState

@Composable
internal fun RestockListCard(
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.section_restock_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (lowStock.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = extended.warningContainer
                            ) {
                                Text(
                                    text = "${lowStock.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = extended.warning,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
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
internal fun ExpiringSoonCard(
    uiState: AstroUiState,
    onSeeCatalog: () -> Unit = {},
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.section_expiring_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (expiring.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = extended.negativeContainer
                            ) {
                                Text(
                                    text = "${expiring.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = extended.negative,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = stringResource(R.string.section_expiring_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = onSeeCatalog,
                    modifier = Modifier.testTag("expiring_see_catalog_button")
                ) {
                    Text(stringResource(R.string.action_view_catalog))
                }
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
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Validade: ${formatShortDateBr(expiryMillis)} • Qtd: ${formatQty(product.quantity)} ${product.unit}",
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
