package com.example.ui.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.example.data.model.Product
import com.example.ui.theme.LocalAstroColors
import com.example.util.formatNumber

@Composable
fun MovementDialog(
    product: Product,
    initialType: String,
    onConfirm: (type: String, quantity: Double, reason: String?, onDone: () -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val astroColors = LocalAstroColors.current
    var type by remember { mutableStateOf(initialType) }
    var quantityInput by remember { mutableStateOf("1") }
    var reason by remember { mutableStateOf("") }

    val parsedQty = quantityInput.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val projected = if (type == "IN") product.quantity + parsedQty else product.quantity - parsedQty
    val isValid = parsedQty > 0.0 && projected >= 0.0

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = astroColors.cardSurface,
            border = BorderStroke(1.dp, astroColors.border)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Movimentar Estoque",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = astroColors.textPrimary
                        )
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = astroColors.textSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Fechar movimentação",
                            tint = astroColors.textSecondary
                        )
                    }
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = type == "IN",
                        onClick = { type = "IN" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("+ Entrada")
                    }
                    SegmentedButton(
                        selected = type == "OUT",
                        onClick = { type = "OUT" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("- Saída / Uso")
                    }
                }

                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = { quantityInput = it },
                    label = { Text("Quantidade (${product.unit})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("movement_qty_input")
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (projected < 0.0) astroColors.dangerBg else astroColors.pageBackground,
                    border = BorderStroke(
                        1.dp,
                        if (projected < 0.0) astroColors.danger else astroColors.border
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Atual: ${formatNumber(product.quantity)} ${product.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = astroColors.textSecondary
                        )
                        Text(
                            text = if (projected < 0.0) {
                                "Saldo insuficiente"
                            } else {
                                "Novo saldo: ${formatNumber(projected)} ${product.unit}"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (projected < 0.0) astroColors.danger else astroColors.primary
                        )
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo (opcional)") },
                    placeholder = {
                        Text(if (type == "IN") "Ex: Compra da semana" else "Ex: Consumo em casa")
                    },
                    singleLine = true,
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(type, parsedQty, reason.ifBlank { null }, onDismiss)
                        },
                        enabled = isValid,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("confirm_movement_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "IN") astroColors.success else astroColors.primary
                        )
                    ) {
                        Text("Confirmar")
                    }
                }
            }
        }
    }
}
