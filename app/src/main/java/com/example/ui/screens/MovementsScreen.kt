package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.StockMovement
import com.example.ui.components.AstroCard
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalAstroColors
import com.example.util.formatNumber
import com.example.util.formatTimestamp

@Composable
fun MovementsScreen(
    movements: List<StockMovement>,
    modifier: Modifier = Modifier
) {
    val astroColors = LocalAstroColors.current
    var search by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("ALL") }

    val filtered = remember(movements, search, typeFilter) {
        val term = search.trim()
        movements.filter { m ->
            val matchesType = typeFilter == "ALL" || m.type == typeFilter
            val matchesSearch = term.isEmpty() ||
                m.productName.contains(term, ignoreCase = true) ||
                m.reason.orEmpty().contains(term, ignoreCase = true)
            matchesType && matchesSearch
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Buscar por produto ou motivo...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "Todas",
                    "IN" to "Entradas",
                    "OUT" to "Saídas",
                    "ADJUST" to "Ajustes"
                ).forEach { (code, label) ->
                    FilterChip(
                        selected = typeFilter == code,
                        onClick = { typeFilter = code },
                        label = { Text(label) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Outlined.History,
                    title = "Sem movimentações",
                    subtitle = "Entradas de compras e consumos em casa aparecem aqui."
                )
            }
        } else {
            items(filtered, key = { it.id }) { mov ->
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
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = mov.productName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = astroColors.textPrimary
                            )
                            Text(
                                text = mov.reason ?: "Movimentação de estoque",
                                style = MaterialTheme.typography.bodySmall,
                                color = astroColors.textSecondary
                            )
                            Text(
                                text = "Saldo: ${formatNumber(mov.previousQuantity)} → ${formatNumber(mov.newQuantity)} ${mov.productUnit} • ${formatTimestamp(mov.createdAt?.toDate())}",
                                style = MaterialTheme.typography.bodySmall,
                                color = astroColors.textMuted
                            )
                        }
                        StatusPill(
                            text = when {
                                isIn -> "+${formatNumber(mov.quantity)}"
                                isOut -> "-${formatNumber(mov.quantity)}"
                                else -> "=${formatNumber(mov.newQuantity)}"
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
