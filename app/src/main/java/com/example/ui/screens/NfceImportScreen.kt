package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.nfce.RegionalPortal
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.theme.AstroTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.AstroUiState
import com.example.ui.viewmodel.NfceImportState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfceImportScreen(
    uiState: AstroUiState,
    nfceState: NfceImportState,
    onBack: () -> Unit,
    onSelectTab: (Int) -> Unit,
    onSelectPortal: (RegionalPortal) -> Unit,
    onInputChange: (String) -> Unit,
    onRawTextChange: (String) -> Unit,
    onParseRawText: () -> Unit,
    onProcessReceiptImage: (Uri) -> Unit,
    onConsultInput: () -> Unit,
    onOpenPortal: () -> Unit,
    onDismissWebPortal: () -> Unit,
    onXmlLoaded: (String) -> Unit,
    onWebViewExtracted: (rawJson: String, isAutoScan: Boolean) -> Unit,
    onToggleItem: (Int) -> Unit,
    onSelectItemCategory: (Int, Long) -> Unit,
    onConfirmImport: () -> Unit,
    onDiscardImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val extended = AstroTheme.extended
    var showImageSourceChooser by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showStatePicker by remember { mutableStateOf(false) }
    var showPasteText by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    BackHandler(onBack = onBack)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onProcessReceiptImage(uri)
    }

    val cameraCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) onProcessReceiptImage(uri)
    }

    fun launchCameraCapture() {
        val photoFile = File(context.cacheDir, "cupom_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        pendingCameraUri = uri
        cameraCaptureLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCameraCapture()
    }

    val xmlPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()?.let(onXmlLoaded)
        }
    }

    if (showQrScanner) {
        BarcodeScannerDialog(
            onCodeDetected = { detected ->
                showQrScanner = false
                onInputChange(detected)
                onConsultInput()
            },
            onDismiss = { showQrScanner = false }
        )
    }

    if (showImageSourceChooser) {
        NfceImageSourceDialog(
            onPickGallery = {
                showImageSourceChooser = false
                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onTakePhoto = {
                showImageSourceChooser = false
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) launchCameraCapture()
                else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onDismiss = { showImageSourceChooser = false }
        )
    }

    if (showStatePicker) {
        NfceStatePickerDialog(
            selectedPortal = nfceState.selectedPortal,
            onSelectPortal = onSelectPortal,
            onDismiss = { showStatePicker = false }
        )
    }

    if (nfceState.showWebPortal) {
        NfcePortalWebViewDialog(
            nfceState = nfceState,
            onWebViewExtracted = onWebViewExtracted,
            onDismiss = onDismissWebPortal
        )
    }

    val selectedCount = nfceState.parsedItems.count { it.selected }
    val selectedTab = nfceState.activeTab.coerceIn(0, 1)

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Importar Cupom Fiscal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("nfce_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
            },
            actions = {
                if (nfceState.parsedItems.isNotEmpty() ||
                    nfceState.inputQuery.isNotBlank() ||
                    nfceState.detectedKey != null ||
                    nfceState.rawReceiptTextInput.isNotBlank()
                ) {
                    IconButton(
                        onClick = onDiscardImport,
                        enabled = !nfceState.isImporting,
                        modifier = Modifier.testTag("nfce_topbar_discard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Excluir importação",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onSelectTab(0) },
                icon = { Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp)) },
                text = { Text("Câmera / Foto", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.testTag("nfce_tab_photo")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onSelectTab(1) },
                icon = { Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp)) },
                text = { Text("Chave / Arquivo", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.testTag("nfce_tab_link")
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("nfce_import_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (nfceState.isFetching) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            Text(
                                text = nfceState.statusMessage ?: "Lendo cupom fiscal...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else if (!nfceState.statusMessage.isNullOrBlank()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = nfceState.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> item {
                    val borderColor = extended.borderSubtle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            drawRoundRect(
                                color = borderColor,
                                cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                                )
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(60.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.QrCodeScanner,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Importe direto do seu cupom",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Aponte para o QR Code no fim do cupom (mais rápido) ou tire uma foto da lista de produtos.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = { showQrScanner = true },
                                enabled = !nfceState.isFetching,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("nfce_scan_qr_button")
                            ) {
                                Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Escanear QR Code", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { showImageSourceChooser = true },
                                enabled = !nfceState.isFetching,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("nfce_choose_photo_button")
                            ) {
                                Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tirar Foto ou Escolher Print")
                            }

                            Text(
                                text = "Dica: em local bem iluminado, o app lê os produtos da foto mesmo sem QR Code.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            if (nfceState.inputQuery.startsWith("http", ignoreCase = true) && nfceState.parsedItems.isEmpty()) {
                                OutlinedButton(
                                    onClick = onOpenPortal,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("nfce_photo_open_portal_button")
                                ) {
                                    Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Abrir Site da Nota (${nfceState.selectedPortal.ufSigla})")
                                }
                            }
                        }
                    }
                }

                else -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                Text(
                                    text = "Chave ou Link da Nota",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Cole o link do QR Code ou digite os 44 números impressos no cupom.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = nfceState.inputQuery,
                                    onValueChange = onInputChange,
                                    placeholder = { Text("Cole o link ou os 44 números...") },
                                    trailingIcon = if (nfceState.inputQuery.isNotEmpty()) {
                                        {
                                            IconButton(onClick = { onInputChange("") }) {
                                                Icon(Icons.Rounded.Clear, contentDescription = "Limpar")
                                            }
                                        }
                                    } else null,
                                    minLines = 2,
                                    maxLines = 4,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("nfce_query_input")
                                )

                                if (nfceState.detectedKey != null) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Chave detectada (${nfceState.selectedPortal.ufSigla}): ${nfceState.detectedKey}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showStatePicker = true }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Place,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Estado: ${nfceState.selectedPortal.label}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "Alterar",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Button(
                                    onClick = onConsultInput,
                                    enabled = !nfceState.isFetching,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("nfce_consult_button")
                                ) {
                                    Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Buscar Itens da Nota")
                                }

                                TextButton(
                                    onClick = onOpenPortal,
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .testTag("nfce_open_portal_button")
                                ) {
                                    Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Abrir site da SEFAZ (${nfceState.selectedPortal.ufSigla}) manualmente")
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, extended.borderSubtle)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            xmlPickerLauncher.launch(arrayOf("text/xml", "application/xml", "*/*"))
                                        }
                                        .padding(16.dp)
                                        .testTag("nfce_import_xml_button"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.UploadFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Importar arquivo .XML",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Se você baixou o arquivo da nota no celular ou e-mail",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                HorizontalDivider()

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showPasteText = !showPasteText }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentPaste,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Colar lista de produtos em texto",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Cole um texto copiado de outra página ou mensagem",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = if (showPasteText) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        contentDescription = if (showPasteText) "Recolher" else "Expandir"
                                    )
                                }

                                AnimatedVisibility(visible = showPasteText || nfceState.rawReceiptTextInput.isNotBlank()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = nfceState.rawReceiptTextInput,
                                            onValueChange = onRawTextChange,
                                            placeholder = {
                                                Text("Ex:\nARROZ 5KG  1 un  24,90\nLEITE 1L  6 un  4,89")
                                            },
                                            minLines = 4,
                                            maxLines = 8,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("nfce_paste_text_input")
                                        )
                                        Button(
                                            onClick = onParseRawText,
                                            enabled = nfceState.rawReceiptTextInput.isNotBlank(),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("nfce_parse_text_button")
                                        ) {
                                            Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Ler Itens do Texto")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            nfceParsedItemsSection(
                uiState = uiState,
                nfceState = nfceState,
                selectedCount = selectedCount,
                onToggleItem = onToggleItem,
                onSelectItemCategory = onSelectItemCategory,
                onConfirmImport = onConfirmImport,
                onDiscardImport = onDiscardImport
            )

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
