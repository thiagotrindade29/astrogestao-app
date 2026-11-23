package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.ui.components.AstroCard
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalAstroColors
import com.example.util.PRESET_CATEGORY_COLORS
import com.example.util.parseHexColor

@Composable
fun CategoriesScreen(
    categories: List<Category>,
    products: List<Product>,
    onSaveCategory: (
        categoryId: String?,
        name: String,
        description: String?,
        color: String,
        onDone: () -> Unit
    ) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    val astroColors = LocalAstroColors.current
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Category?>(null) }

    val countsByCat = remember(products) {
        products.groupingBy { it.categoryId ?: "" }.eachCount()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("new_category_button"),
                colors = ButtonDefaults.buttonColors(containerColor = astroColors.primary)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nova Categoria")
            }
        }

        if (categories.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Outlined.Category,
                    title = "Nenhuma categoria criada",
                    subtitle = "Crie categorias como Alimentos, Geladeira, Limpeza ou Pet para organizar a casa."
                )
            }
        } else {
            items(categories, key = { it.id }) { cat ->
                val color = parseHexColor(cat.color)
                val count = countsByCat[cat.id] ?: 0
                AstroCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = astroColors.textPrimary
                                )
                                if (!cat.description.isNullOrBlank()) {
                                    Text(
                                        text = cat.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = astroColors.textMuted
                                    )
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusPill(
                                text = "$count itens",
                                color = color,
                                backgroundColor = color.copy(alpha = 0.14f)
                            )
                            IconButton(
                                onClick = { editingCategory = cat },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Editar categoria ${cat.name}",
                                    tint = astroColors.textSecondary
                                )
                            }
                            IconButton(
                                onClick = { pendingDelete = cat },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Excluir categoria ${cat.name}",
                                    tint = astroColors.danger
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog || editingCategory != null) {
        CategoryEditDialog(
            existing = editingCategory,
            onSave = { name, desc, color ->
                onSaveCategory(editingCategory?.id, name, desc, color) {
                    showCreateDialog = false
                    editingCategory = null
                }
            },
            onDismiss = {
                showCreateDialog = false
                editingCategory = null
            }
        )
    }

    pendingDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Excluir categoria") },
            text = { Text("Deseja remover a categoria \"${cat.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCategory(cat)
                        pendingDelete = null
                    }
                ) {
                    Text("Excluir", color = astroColors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun CategoryEditDialog(
    existing: Category?,
    onSave: (name: String, description: String?, color: String) -> Unit,
    onDismiss: () -> Unit
) {
    val astroColors = LocalAstroColors.current
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var selectedColor by remember {
        mutableStateOf(existing?.color ?: PRESET_CATEGORY_COLORS.first())
    }

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
                Text(
                    text = if (existing == null) "Nova Categoria" else "Editar Categoria",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = astroColors.textPrimary
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Cor da etiqueta",
                    style = MaterialTheme.typography.labelMedium,
                    color = astroColors.textSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PRESET_CATEGORY_COLORS.forEach { hex ->
                        val c = parseHexColor(hex)
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (isSelected) Modifier.border(3.dp, astroColors.textPrimary, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
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
                        onClick = { onSave(name, description.ifBlank { null }, selectedColor) },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.minimumInteractiveComponentSize(),
                        colors = ButtonDefaults.buttonColors(containerColor = astroColors.primary)
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}
