package com.example.ui.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.ui.theme.LocalAstroColors
import com.example.util.LOCATION_SUGGESTIONS
import com.example.util.UNIT_OPTIONS
import java.util.Locale

@Composable
fun ProductDialog(
    existingProduct: Product?,
    categories: List<Category>,
    onSave: (
        existing: Product?,
        name: String,
        sku: String?,
        barcode: String?,
        category: Category?,
        location: String,
        unit: String,
        quantity: Double,
        minQuantity: Double,
        unitPrice: Double,
        previousUnitPrice: Double,
        expirationDate: String?,
        notes: String?,
        onDone: () -> Unit
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val astroColors = LocalAstroColors.current
    var name by remember { mutableStateOf(existingProduct?.name.orEmpty()) }
    var barcode by remember { mutableStateOf(existingProduct?.barcode ?: existingProduct?.sku.orEmpty()) }
    var selectedCategory by remember {
        mutableStateOf(
            categories.firstOrNull { it.id == existingProduct?.categoryId } ?: categories.firstOrNull()
        )
    }
    var location by remember { mutableStateOf(existingProduct?.location ?: "Despensa") }
    var unit by remember { mutableStateOf(existingProduct?.unit ?: "un") }
    var quantityInput by remember {
        mutableStateOf(
            if (existingProduct != null) formatInput(existingProduct.quantity) else "1"
        )
    }
    var minQtyInput by remember {
        mutableStateOf(
            if (existingProduct != null) formatInput(existingProduct.minQuantity) else "1"
        )
    }
    var priceInput by remember {
        mutableStateOf(
            if (existingProduct != null && existingProduct.unitPrice > 0.0) {
                String.format(Locale.US, "%.2f", existingProduct.unitPrice)
            } else ""
        )
    }
    var expirationDate by remember { mutableStateOf(existingProduct?.expirationDate.orEmpty()) }
    var notes by remember { mutableStateOf(existingProduct?.notes.orEmpty()) }
    var showScanner by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(22.dp),
            color = astroColors.cardSurface,
            border = BorderStroke(1.dp, astroColors.border)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .heightIn(max = 640.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (existingProduct == null) "Novo Produto da Casa" else "Editar Produto",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = astroColors.textPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Fechar formulário",
                            tint = astroColors.textSecondary
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do produto *") },
                    placeholder = { Text("Ex: Arroz Tipo 1 (5kg)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Código de barras / EAN") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_barcode_input")
                    )
                    IconButton(
                        onClick = { showScanner = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("product_scan_barcode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCodeScanner,
                            contentDescription = "Escanear código de barras",
                            tint = astroColors.primary
                        )
                    }
                }

                Text(
                    text = "Categoria",
                    style = MaterialTheme.typography.labelMedium,
                    color = astroColors.textSecondary
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory?.id == cat.id,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.name) }
                        )
                    }
                }

                Text(
                    text = "Onde fica guardado",
                    style = MaterialTheme.typography.labelMedium,
                    color = astroColors.textSecondary
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LOCATION_SUGGESTIONS.forEach { loc ->
                        FilterChip(
                            selected = location.equals(loc, ignoreCase = true),
                            onClick = { location = loc },
                            label = { Text(loc) }
                        )
                    }
                }
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Local personalizado") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Unidade",
                    style = MaterialTheme.typography.labelMedium,
                    color = astroColors.textSecondary
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UNIT_OPTIONS.forEach { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { unit = u },
                            label = { Text(u) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { quantityInput = it },
                        label = { Text("Qtd atual") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_qty_input")
                    )
                    OutlinedTextField(
                        value = minQtyInput,
                        onValueChange = { minQtyInput = it },
                        label = { Text("Qtd mínima") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_min_qty_input")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text("Preço pago (R$)") },
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_price_input")
                    )
                    OutlinedTextField(
                        value = expirationDate,
                        onValueChange = { expirationDate = it },
                        label = { Text("Validade (AAAA-MM-DD)") },
                        placeholder = { Text("2026-12-31") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observação (opcional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val qty = quantityInput.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                            val minQty = minQtyInput.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 1.0
                            val price = priceInput.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                            val prevPrice = if (existingProduct != null && existingProduct.unitPrice > 0.0 && existingProduct.unitPrice != price) {
                                existingProduct.unitPrice
                            } else {
                                existingProduct?.previousUnitPrice ?: price
                            }
                            onSave(
                                existingProduct,
                                name,
                                existingProduct?.sku,
                                barcode.ifBlank { null },
                                selectedCategory,
                                location,
                                unit,
                                qty,
                                minQty,
                                price,
                                prevPrice,
                                expirationDate.ifBlank { null },
                                notes.ifBlank { null },
                                onDismiss
                            )
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("save_product_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = astroColors.primary)
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }

    if (showScanner) {
        BarcodeScannerDialog(
            onBarcodeDetected = { code ->
                barcode = code
                showScanner = false
            },
            onDismiss = { showScanner = false }
        )
    }
}

private fun formatInput(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.2f", value)
}
