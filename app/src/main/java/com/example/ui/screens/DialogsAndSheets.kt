package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CategoryEntity
import com.example.data.local.MovementType
import com.example.data.local.ProductEntity
import com.example.ui.components.daysUntil
import com.example.ui.components.formatQty

private val availableUnits = listOf("un", "kg", "g", "L", "ml", "pct", "cx")

@Composable
fun ProductEditorDialog(
    initialProduct: ProductEntity?,
    categories: List<CategoryEntity>,
    onSave: (ProductEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var code by remember { mutableStateOf(initialProduct?.code ?: "") }
    var selectedCategoryId by remember {
        mutableStateOf(initialProduct?.categoryId ?: categories.firstOrNull()?.id)
    }
    var unit by remember { mutableStateOf(initialProduct?.unit ?: "un") }
    var quantityText by remember {
        mutableStateOf(initialProduct?.quantity?.toString() ?: "1")
    }
    var minStockText by remember {
        mutableStateOf(initialProduct?.minStock?.toString() ?: "1")
    }
    var priceText by remember {
        mutableStateOf(initialProduct?.unitPrice?.takeIf { it > 0.0 }?.toString() ?: "")
    }
    var expiryDaysText by remember {
        mutableStateOf(
            initialProduct?.expiryDateMillis?.let { daysUntil(it).coerceAtLeast(0).toString() } ?: ""
        )
    }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialProduct == null) "Novo Produto" else "Editar Produto",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text(stringResource(R.string.label_name)) },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text(stringResource(R.string.error_required)) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(R.string.label_code)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(stringResource(R.string.label_category), style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(cat.colorHex))
                                )
                            },
                            label = { Text(cat.name) }
                        )
                    }
                }

                Text(stringResource(R.string.label_unit), style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableUnits.forEach { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { unit = u },
                            label = { Text(u) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text(stringResource(R.string.label_quantity)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_qty_input")
                    )
                    OutlinedTextField(
                        value = minStockText,
                        onValueChange = { minStockText = it },
                        label = { Text(stringResource(R.string.label_min_stock)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_min_input")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text(stringResource(R.string.label_unit_price)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_price_input")
                    )
                    OutlinedTextField(
                        value = expiryDaysText,
                        onValueChange = { expiryDaysText = it },
                        label = { Text(stringResource(R.string.label_expiry_days)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_expiry_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val qty = quantityText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    val min = minStockText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 1.0
                    val price = priceText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    val expiryMillis = expiryDaysText.trim().toLongOrNull()?.let { days ->
                        System.currentTimeMillis() + days * 86_400_000L
                    }
                    onSave(
                        ProductEntity(
                            id = initialProduct?.id ?: 0L,
                            uniqueCode = initialProduct?.uniqueCode ?: "",
                            code = code.trim(),
                            name = name.trim(),
                            categoryId = selectedCategoryId,
                            unit = unit,
                            quantity = qty,
                            minStock = min,
                            unitPrice = price,
                            lastPaidPrice = initialProduct?.lastPaidPrice,
                            expiryDateMillis = expiryMillis,
                            notes = initialProduct?.notes ?: ""
                        )
                    )
                },
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun MovementEditorDialog(
    products: List<ProductEntity>,
    preselectedProduct: ProductEntity?,
    preselectedType: MovementType,
    onConfirm: (productId: Long, type: MovementType, amountOrBalance: Double, unitPrice: Double?, reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedProduct by remember {
        mutableStateOf(preselectedProduct ?: products.firstOrNull())
    }
    var type by remember { mutableStateOf(preselectedType) }
    var amountText by remember {
        mutableStateOf(
            if (preselectedType == MovementType.AJUSTE && selectedProduct != null) {
                selectedProduct!!.quantity.toString()
            } else {
                "1"
            }
        )
    }
    var priceText by remember {
        mutableStateOf(selectedProduct?.unitPrice?.takeIf { it > 0.0 }?.toString() ?: "")
    }
    var reason by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Movimentação", style = MaterialTheme.typography.titleLarge) },
        text = {
            if (products.isEmpty()) {
                Text("Cadastre ao menos um produto antes de registrar movimentações.")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Produto", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        products.forEach { p ->
                            FilterChip(
                                selected = selectedProduct?.id == p.id,
                                onClick = {
                                    selectedProduct = p
                                    priceText = p.unitPrice.takeIf { it > 0.0 }?.toString() ?: ""
                                    if (type == MovementType.AJUSTE) {
                                        amountText = p.quantity.toString()
                                    }
                                },
                                label = { Text(p.name) }
                            )
                        }
                    }

                    selectedProduct?.let { prod ->
                        Text(
                            text = "Saldo atual: ${formatQty(prod.quantity)} ${prod.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MovementType.entries.forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t.label) },
                                modifier = Modifier.testTag("mv_type_${t.name.lowercase()}")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            amountError = false
                        },
                        label = {
                            Text(
                                if (type == MovementType.AJUSTE) "Novo saldo final"
                                else "Quantidade (${selectedProduct?.unit ?: "un"})"
                            )
                        },
                        isError = amountError,
                        supportingText = if (amountError) {
                            { Text(stringResource(R.string.error_invalid_number)) }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mv_amount_input")
                    )

                    if (type == MovementType.ENTRADA) {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text(stringResource(R.string.label_unit_price)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text(stringResource(R.string.label_reason)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mv_reason_input")
                    )
                }
            }
        },
        confirmButton = {
            if (products.isNotEmpty()) {
                Button(
                    onClick = {
                        val prod = selectedProduct ?: return@Button
                        val valNum = amountText.replace(",", ".").toDoubleOrNull()
                        if (valNum == null || (type != MovementType.AJUSTE && valNum <= 0.0) || valNum < 0.0) {
                            amountError = true
                            return@Button
                        }
                        val priceOverride = priceText.replace(",", ".").toDoubleOrNull()
                        onConfirm(prod.id, type, valNum, priceOverride, reason.trim())
                    },
                    modifier = Modifier.testTag("save_movement_button")
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
