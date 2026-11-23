package com.example.ui.modals

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Category
import com.example.data.model.ParsedReceiptData
import com.example.data.model.ParsedReceiptItem
import com.example.data.model.Product
import com.example.data.nfe.ReceiptParser
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalAstroColors
import com.example.util.formatCurrency
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@Composable
fun NfeImportDialog(
    existingProducts: List<Product>,
    categories: List<Category>,
    isSubmitting: Boolean,
    onConfirmImport: (
        storeName: String,
        notes: String,
        items: List<ParsedReceiptItem>,
        onDone: () -> Unit
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val astroColors = LocalAstroColors.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var isParsing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var sefazUrl by remember { mutableStateOf("") }
    var pastedText by remember { mutableStateOf("") }

    var parsedData by remember { mutableStateOf<ParsedReceiptData?>(null) }
    var editStoreName by remember { mutableStateOf("Supermercado") }
    var editNotes by remember { mutableStateOf("") }
    var editItems by remember { mutableStateOf<List<ParsedReceiptItem>>(emptyList()) }

    fun applyParsed(data: ParsedReceiptData) {
        parsedData = data
        editStoreName = data.storeName
        editNotes = data.notes
        editItems = data.items
        errorMessage = null
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isParsing = true
            errorMessage = null
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                val barcodeClient = BarcodeScanning.getClient()
                val barcodes = barcodeClient.process(inputImage).await()
                val qrUrl = barcodes.firstNotNullOfOrNull { it.rawValue }
                    ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }

                if (qrUrl != null) {
                    val urlResult = ReceiptParser.parseSefazUrl(qrUrl, existingProducts, categories)
                    urlResult.onSuccess { data ->
                        applyParsed(data)
                    }.onFailure {
                        // Fall back to OCR text recognition on the same image
                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        val visionText = recognizer.process(inputImage).await()
                        val ocrData = ReceiptParser.parseReceiptText(
                            text = visionText.text,
                            existingProducts = existingProducts,
                            categories = categories,
                            sourceLabel = "Foto / OCR"
                        )
                        if (ocrData.items.isEmpty()) {
                            errorMessage = "QR Code encontrado, mas não foi possível extrair os itens automaticamente. Tente usar o Texto Colado ou Arquivo XML."
                        } else {
                            applyParsed(ocrData)
                        }
                    }
                } else {
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    val visionText = recognizer.process(inputImage).await()
                    val ocrData = ReceiptParser.parseReceiptText(
                        text = visionText.text,
                        existingProducts = existingProducts,
                        categories = categories,
                        sourceLabel = "Foto / OCR"
                    )
                    if (ocrData.items.isEmpty()) {
                        errorMessage = "Não identificamos itens legíveis nessa imagem. Experimente colar o texto do cupom ou importar o XML."
                    } else {
                        applyParsed(ocrData)
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Erro ao ler foto do cupom."
            } finally {
                isParsing = false
            }
        }
    }

    val xmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isParsing = true
            errorMessage = null
            try {
                val content = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                    .orEmpty()
                if (content.isBlank()) {
                    errorMessage = "O arquivo selecionado está vazio."
                } else {
                    val data = ReceiptParser.parseNfeXml(content, existingProducts, categories)
                    if (data.items.isEmpty()) {
                        errorMessage = "Nenhum produto encontrado nesse XML de NF-e."
                    } else {
                        applyParsed(data)
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Arquivo XML inválido: ${e.localizedMessage}"
            } finally {
                isParsing = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            shape = RoundedCornerShape(22.dp),
            color = astroColors.cardSurface,
            border = BorderStroke(1.dp, astroColors.border)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .heightIn(max = 640.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = astroColors.primary
                        )
                        Text(
                            text = "Importar Cupom / NF-e",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = astroColors.textPrimary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("close_nfe_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Fechar importação",
                            tint = astroColors.textSecondary
                        )
                    }
                }

                if (parsedData == null) {
                    Text(
                        text = "Escolha como deseja importar os itens da sua nota fiscal antes de enviar para o estoque:",
                        style = MaterialTheme.typography.bodySmall,
                        color = astroColors.textSecondary
                    )

                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 0.dp
                    ) {
                        listOf("Foto / QR", "Link SEFAZ", "Arquivo XML", "Texto Colado").forEachIndexed { idx, label ->
                            Tab(
                                selected = selectedTab == idx,
                                onClick = {
                                    selectedTab = idx
                                    errorMessage = null
                                },
                                text = { Text(label) }
                            )
                        }
                    }

                    when (selectedTab) {
                        0 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Selecione uma foto do cupom fiscal ou QR Code da NF-e. O app tenta ler o QR Code primeiro e, se necessário, faz leitura de texto (OCR) automaticamente.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = astroColors.textMuted
                                )
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    enabled = !isParsing,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .testTag("pick_nfe_photo_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = astroColors.primary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PhotoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Selecionar Foto do Cupom")
                                }
                            }
                        }

                        1 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = sefazUrl,
                                    onValueChange = { sefazUrl = it },
                                    label = { Text("Link de consulta da NFC-e / SEFAZ") },
                                    placeholder = { Text("https://www.nfce.fazenda...") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("sefaz_url_input")
                                )
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isParsing = true
                                            errorMessage = null
                                            ReceiptParser.parseSefazUrl(sefazUrl, existingProducts, categories)
                                                .onSuccess { applyParsed(it) }
                                                .onFailure { errorMessage = it.message }
                                            isParsing = false
                                        }
                                    },
                                    enabled = !isParsing && sefazUrl.isNotBlank(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .testTag("fetch_sefaz_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = astroColors.primary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Link,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Buscar Itens da Nota")
                                }
                            }
                        }

                        2 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Selecione um arquivo .xml da Nota Fiscal Eletrônica para extrair produtos, quantidades e valores exatos.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = astroColors.textMuted
                                )
                                Button(
                                    onClick = {
                                        xmlLauncher.launch(arrayOf("text/xml", "application/xml", "*/*"))
                                    },
                                    enabled = !isParsing,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .testTag("pick_nfe_xml_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = astroColors.primary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Escolher Arquivo XML")
                                }
                            }
                        }

                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = pastedText,
                                    onValueChange = { pastedText = it },
                                    label = { Text("Cole o texto do cupom fiscal") },
                                    placeholder = { Text("Ex:\nArroz Tipo 1 2 UN x 28,90\nLeite Integral 4 L x 5,49") },
                                    minLines = 4,
                                    maxLines = 7,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("pasted_nfe_input")
                                )
                                Button(
                                    onClick = {
                                        val data = ReceiptParser.parseReceiptText(
                                            text = pastedText,
                                            existingProducts = existingProducts,
                                            categories = categories,
                                            sourceLabel = "Texto Colado"
                                        )
                                        if (data.items.isEmpty()) {
                                            errorMessage = "Não conseguimos identificar linhas de produto e preço neste texto."
                                        } else {
                                            applyParsed(data)
                                        }
                                    },
                                    enabled = !isParsing && pastedText.isNotBlank(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .testTag("parse_pasted_nfe_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = astroColors.primary)
                                ) {
                                    Text("Extrair Itens do Texto")
                                }
                            }
                        }
                    }

                    if (isParsing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Processando cupom...", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    if (errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = astroColors.dangerBg,
                            border = BorderStroke(1.dp, astroColors.danger.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                color = astroColors.danger,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                } else {
                    // Editable pre-stock review screen
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(
                            text = "${editItems.size} itens • ${parsedData?.sourceLabel.orEmpty()}",
                            color = astroColors.info,
                            backgroundColor = astroColors.infoBg
                        )
                        TextButton(onClick = { parsedData = null }) {
                            Text("Voltar")
                        }
                    }

                    OutlinedTextField(
                        value = editStoreName,
                        onValueChange = { editStoreName = it },
                        label = { Text("Mercado / Estabelecimento") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    editItems.forEachIndexed { idx, item ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = astroColors.pageBackground,
                            border = BorderStroke(1.dp, astroColors.border)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatusPill(
                                        text = if (item.alreadyInStock) "Já existe no estoque" else "Novo item",
                                        color = if (item.alreadyInStock) astroColors.success else astroColors.primary,
                                        backgroundColor = if (item.alreadyInStock) astroColors.successBg else astroColors.primary.copy(alpha = 0.14f)
                                    )
                                    IconButton(
                                        onClick = {
                                            editItems = editItems.filterNot { it.localId == item.localId }
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Remover item importado",
                                            tint = astroColors.danger
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = item.name,
                                    onValueChange = { v ->
                                        editItems = editItems.mapIndexed { i, it ->
                                            if (i == idx) it.copy(name = v) else it
                                        }
                                    },
                                    label = { Text("Produto") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = item.quantityInput,
                                        onValueChange = { v ->
                                            editItems = editItems.mapIndexed { i, it ->
                                                if (i == idx) it.copy(quantityInput = v) else it
                                            }
                                        },
                                        label = { Text("Qtd (${item.unit})") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = item.unitPriceInput,
                                        onValueChange = { v ->
                                            editItems = editItems.mapIndexed { i, it ->
                                                if (i == idx) it.copy(unitPriceInput = v) else it
                                            }
                                        },
                                        label = { Text("Preço un. (R$)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            val defaultCat = categories.firstOrNull()
                            editItems = editItems + ParsedReceiptItem(
                                localId = UUID.randomUUID().toString(),
                                name = "",
                                categoryId = defaultCat?.id,
                                categoryName = defaultCat?.name ?: "Alimentos Secos",
                                categoryColor = defaultCat?.color ?: "#3b82f6"
                            )
                        }
                    ) {
                        Text("+ Adicionar item manualmente")
                    }

                    val totalPreview = editItems.sumOf { it.totalPrice }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total do cupom:",
                            style = MaterialTheme.typography.titleSmall,
                            color = astroColors.textSecondary
                        )
                        Text(
                            text = formatCurrency(totalPreview),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = astroColors.primary
                        )
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
                            onClick = {
                                val valid = editItems.filter { it.name.isNotBlank() && it.parsedQuantity > 0.0 }
                                if (valid.isNotEmpty()) {
                                    onConfirmImport(editStoreName, editNotes, valid, onDismiss)
                                }
                            },
                            enabled = !isSubmitting && editItems.any { it.name.isNotBlank() && it.parsedQuantity > 0.0 },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("confirm_nfe_import_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = astroColors.primary)
                        ) {
                            Text(if (isSubmitting) "Salvando..." else "Enviar para o Estoque")
                        }
                    }
                }
            }
        }
    }
}
