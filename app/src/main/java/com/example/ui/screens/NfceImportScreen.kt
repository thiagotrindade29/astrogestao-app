package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.nfce.NfceParser
import com.example.data.nfce.RegionalPortal
import com.example.data.repository.AstroRepository
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.formatBrl
import com.example.ui.components.formatQty
import com.example.ui.theme.AstroTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.AstroUiState
import com.example.ui.viewmodel.NfceImportState
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    onXmlLoaded: (String) -> Unit,
    onWebViewExtracted: (rawJson: String, isAutoScan: Boolean) -> Unit,
    onToggleItem: (Int) -> Unit,
    onSelectItemCategory: (Int, Long) -> Unit,
    onConfirmImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val extended = AstroTheme.extended
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var webProgress by remember { mutableIntStateOf(0) }
    var showImageSourceChooser by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showAllStates by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    BackHandler(onBack = onBack)

    // Zero-permission Android Photo Picker for receipt photos / screenshots
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onProcessReceiptImage(uri)
        }
    }

    // Camera capture via FileProvider into app cacheDir (no storage permissions needed)
    val cameraCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            onProcessReceiptImage(uri)
        }
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
        if (granted) {
            launchCameraCapture()
        }
    }

    val xmlPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()?.let { xmlText ->
                onXmlLoaded(xmlText)
            }
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
        AlertDialog(
            onDismissRequest = { showImageSourceChooser = false },
            title = {
                Text(
                    text = "Foto ou Print do Cupom",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "O sistema detectará automaticamente o QR Code da SEFAZ ou fará leitura ótica (OCR) dos itens impressos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImageSourceChooser = false
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("nfce_pick_gallery_option"),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Escolher da Galeria / Print", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Selecione foto ou print do cupom fiscal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImageSourceChooser = false
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                    PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    launchCameraCapture()
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                            .testTag("nfce_take_photo_option"),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Tirar Foto Agora", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Fotografe o cupom fiscal de papel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImageSourceChooser = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    val selectedCount = nfceState.parsedItems.count { it.selected }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
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
                            text = "Importador de NF-e & Cupons Fiscais",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // 4 Tabs matching user's reference screenshot
        ScrollableTabRow(
            selectedTabIndex = nfceState.activeTab,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = nfceState.activeTab == 0,
                onClick = { onSelectTab(0) },
                icon = { Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp)) },
                text = { Text("Foto / Print do Cupom") },
                modifier = Modifier.testTag("nfce_tab_photo")
            )
            Tab(
                selected = nfceState.activeTab == 1,
                onClick = { onSelectTab(1) },
                icon = { Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp)) },
                text = { Text("Link do QR Code (SEFAZ)") },
                modifier = Modifier.testTag("nfce_tab_link")
            )
            Tab(
                selected = nfceState.activeTab == 2,
                onClick = { onSelectTab(2) },
                icon = { Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(18.dp)) },
                text = { Text("Arquivo XML da NF-e") },
                modifier = Modifier.testTag("nfce_tab_xml")
            )
            Tab(
                selected = nfceState.activeTab == 3,
                onClick = { onSelectTab(3) },
                icon = { Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp)) },
                text = { Text("Colar Texto do Cupom") },
                modifier = Modifier.testTag("nfce_tab_text")
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("nfce_import_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Importe itens de compras direto para o estoque via foto do cupom, link do QR Code da SEFAZ (todo o Brasil) ou arquivo XML.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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
                                text = nfceState.statusMessage ?: "Processando cupom fiscal...",
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

            when (nfceState.activeTab) {
                // TAB 0: Foto / Print do Cupom (OCR + QR Code auto-detect, matching screenshot!)
                0 -> {
                    item {
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
                                    .padding(horizontal = 20.dp, vertical = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(58.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.CameraAlt,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Tire uma Foto ou Selecione o Print do Cupom Fiscal",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "O sistema detectará automaticamente se há um QR Code da SEFAZ na foto para buscar os dados eletrônicos oficiais em qualquer estado do Brasil. Se não houver QR Code visível, faremos a leitura ótica inteligente (OCR) dos itens impressos!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = { showImageSourceChooser = true },
                                    enabled = !nfceState.isFetching,
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("nfce_choose_photo_button")
                                ) {
                                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Escolher Imagem / Tirar Foto")
                                }

                                Text(
                                    text = "Dica: Centralize bem os itens do cupom fiscal ou a área do QR Code para melhor leitura.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // TAB 1: Link do QR Code / Chave (Nationwide - All 27 UFs)
                1 -> {
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Estado (Detecção Automática — Todo o Brasil)",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Selecionado: ${nfceState.selectedPortal.label}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    TextButton(onClick = { showAllStates = !showAllStates }) {
                                        Text(if (showAllStates) "Ocultar UFs" else "Todas as UFs (27)")
                                        Icon(
                                            imageVector = if (showAllStates) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                            contentDescription = null
                                        )
                                    }
                                }

                                if (showAllStates) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        NfceParser.regionalPortals.forEach { portal ->
                                            FilterChip(
                                                selected = nfceState.selectedPortal.ufCode == portal.ufCode,
                                                onClick = {
                                                    onSelectPortal(portal)
                                                    showAllStates = false
                                                },
                                                label = { Text(portal.ufSigla) },
                                                modifier = Modifier.testTag("portal_chip_${portal.ufCode}")
                                            )
                                        }
                                    }
                                } else {
                                    // Quick row with Portal Nacional + selected + major UFs
                                    val quickPortals = remember(nfceState.selectedPortal) {
                                        val defaults = listOf("00", "42", "41", "43", "35", "31", "33", "29", "53")
                                        val ids = (listOf(nfceState.selectedPortal.ufCode) + defaults).distinct()
                                        ids.mapNotNull { id -> NfceParser.regionalPortals.firstOrNull { it.ufCode == id } }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        quickPortals.forEach { portal ->
                                            FilterChip(
                                                selected = nfceState.selectedPortal.ufCode == portal.ufCode,
                                                onClick = { onSelectPortal(portal) },
                                                label = { Text(portal.label) },
                                                modifier = Modifier.testTag("portal_chip_${portal.ufCode}")
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = nfceState.inputQuery,
                                    onValueChange = onInputChange,
                                    placeholder = { Text("Cole o link do QR Code de qualquer estado ou a chave de 44 dígitos…") },
                                    minLines = 2,
                                    maxLines = 4,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("nfce_query_input")
                                )

                                if (nfceState.detectedKey != null) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = MaterialTheme.shapes.extraSmall
                                    ) {
                                        Text(
                                            text = "Chave (${nfceState.selectedPortal.ufSigla}): ${nfceState.detectedKey}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { showQrScanner = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("nfce_scan_qr_button")
                                    ) {
                                        Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ler QR Code")
                                    }
                                    OutlinedButton(
                                        onClick = onConsultInput,
                                        enabled = !nfceState.isFetching,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("nfce_consult_button")
                                    ) {
                                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Consultar")
                                    }
                                }

                                OutlinedButton(
                                    onClick = onOpenPortal,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("nfce_open_portal_button")
                                ) {
                                    Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Abrir Portal SEFAZ (${nfceState.selectedPortal.ufSigla})")
                                }
                            }
                        }
                    }

                    if (nfceState.showWebPortal) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("nfce_webview_card"),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = nfceState.selectedPortal.label,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (nfceState.detectedKey != null) {
                                            OutlinedButton(
                                                onClick = {
                                                    webViewRef?.evaluateJavascript(
                                                        NfceParser.buildFillKeyJs(nfceState.detectedKey),
                                                        null
                                                    )
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.nfce_fill_key),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    }

                                    if (webProgress in 1..99) {
                                        LinearProgressIndicator(
                                            progress = { webProgress / 100f },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    AndroidView(
                                        factory = { ctx ->
                                            WebView(ctx).apply {
                                                val cm = CookieManager.getInstance()
                                                cm.setAcceptCookie(true)
                                                cm.setAcceptThirdPartyCookies(this, true)
                                                settings.javaScriptEnabled = true
                                                settings.domStorageEnabled = true
                                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                                settings.builtInZoomControls = true
                                                settings.displayZoomControls = false
                                                settings.loadWithOverviewMode = true
                                                settings.useWideViewPort = true
                                                settings.userAgentString =
                                                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                                                webViewClient = object : WebViewClient() {
                                                    override fun onPageFinished(view: WebView?, url: String?) {
                                                        super.onPageFinished(view, url)
                                                        nfceState.detectedKey?.let { key ->
                                                            view?.evaluateJavascript(NfceParser.buildFillKeyJs(key), null)
                                                        }
                                                        view?.evaluateJavascript(NfceParser.WEBVIEW_EXTRACTION_JS) { raw ->
                                                            if (!raw.isNullOrBlank()) {
                                                                onWebViewExtracted(raw, true)
                                                            }
                                                        }
                                                    }
                                                }
                                                webChromeClient = object : WebChromeClient() {
                                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                        webProgress = newProgress
                                                    }
                                                }
                                                loadUrl(nfceState.activePortalUrl)
                                                webViewRef = this
                                            }
                                        },
                                        update = { wv ->
                                            if (wv.url != nfceState.activePortalUrl && !nfceState.activePortalUrl.isBlank()) {
                                                wv.loadUrl(nfceState.activePortalUrl)
                                            }
                                            webViewRef = wv
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(400.dp)
                                            .clip(MaterialTheme.shapes.small)
                                    )

                                    Button(
                                        onClick = {
                                            webViewRef?.evaluateJavascript(NfceParser.WEBVIEW_EXTRACTION_JS) { raw ->
                                                onWebViewExtracted(raw.orEmpty(), false)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("nfce_extract_webview_button")
                                    ) {
                                        Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.nfce_extract_page))
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 2: Arquivo XML da NF-e
                2 -> {
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
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.UploadFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = "Importar Arquivo XML Oficial (NF-e / NFC-e)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Funciona para notas fiscais de qualquer estado brasileiro. Lê código EAN, descrição, unidade, quantidade e preço unitário diretamente do XML.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = { xmlPickerLauncher.launch(arrayOf("text/xml", "application/xml", "*/*")) },
                                    modifier = Modifier.testTag("nfce_import_xml_button")
                                ) {
                                    Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Selecionar Arquivo .XML")
                                }
                            }
                        }
                    }
                }

                // TAB 3: Colar Texto do Cupom
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
                                    text = "Cole os itens do cupom ou texto copiado",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedTextField(
                                    value = nfceState.rawReceiptTextInput,
                                    onValueChange = onRawTextChange,
                                    placeholder = {
                                        Text("Ex:\nARROZ TIO JOAO 5KG  1 un  24,90\nLEITE INTEGRAL 1L  6 un  4,89")
                                    },
                                    minLines = 5,
                                    maxLines = 10,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("nfce_paste_text_input")
                                )
                                Button(
                                    onClick = onParseRawText,
                                    enabled = nfceState.rawReceiptTextInput.isNotBlank(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("nfce_parse_text_button")
                                ) {
                                    Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Interpretar Itens do Texto")
                                }
                            }
                        }
                    }
                }
            }

            // Shared Parsed Items Review List
            if (nfceState.parsedItems.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, extended.positive.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = nfceState.storeName ?: "Itens Prontos para Importar",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$selectedCount de ${nfceState.parsedItems.size} selecionado(s) • Total: ${
                                        formatBrl(
                                            nfceState.parsedItems.filter { it.selected }.sumOf { it.totalPrice }
                                        )
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.positive
                                )
                            }
                            Button(
                                onClick = onConfirmImport,
                                enabled = selectedCount > 0,
                                modifier = Modifier.testTag("nfce_confirm_import_button")
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Importar ($selectedCount)")
                            }
                        }
                    }
                }

                itemsIndexed(nfceState.parsedItems) { index, item ->
                    val normalizedName = AstroRepository.normalizeProductName(item.name)
                    val existingMatch = uiState.products.firstOrNull { prod ->
                        val aliases = uiState.aliasesByProductId[prod.id].orEmpty()
                        (item.code.isNotBlank() && (
                            prod.code.equals(item.code, ignoreCase = true) ||
                                prod.uniqueCode.equals(item.code, ignoreCase = true) ||
                                aliases.any { it.externalCode.equals(item.code, ignoreCase = true) }
                            )) || AstroRepository.normalizeProductName(prod.name) == normalizedName
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, extended.borderSubtle)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.selected,
                                    onCheckedChange = { onToggleItem(index) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${formatQty(item.quantity)} ${item.unit} × ${formatBrl(item.unitPrice)} = ${formatBrl(item.totalPrice)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (existingMatch != null) {
                                            "Somará ao existente ${existingMatch.uniqueCode} (saldo: ${formatQty(existingMatch.quantity)} ${existingMatch.unit})"
                                        } else {
                                            "Será cadastrado com novo código único AST"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (existingMatch != null) extended.info else extended.positive
                                    )
                                }
                            }

                            if (existingMatch == null && uiState.categories.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    uiState.categories.forEach { cat ->
                                        FilterChip(
                                            selected = item.categoryId == cat.id,
                                            onClick = { onSelectItemCategory(index, cat.id) },
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
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
