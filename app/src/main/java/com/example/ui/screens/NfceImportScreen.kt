package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.ui.viewmodel.NfceErrorAction
import com.example.ui.viewmodel.NfceImportState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    val extended = AstroTheme.extended

    var showImageSourceChooser by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showStatePicker by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun dismissKeyboard() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    BackHandler {
        dismissKeyboard()
        onBack()
    }

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
    val digitsCount = remember(nfceState.inputQuery) {
        nfceState.inputQuery.count { it.isDigit() }
    }
    val hasAnyDraft = nfceState.parsedItems.isNotEmpty() ||
        nfceState.inputQuery.isNotBlank() ||
        nfceState.detectedKey != null ||
        nfceState.errorMessage != null

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
                        text = "Importar Nota Fiscal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        dismissKeyboard()
                        onBack()
                    },
                    modifier = Modifier.testTag("nfce_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
            },
            actions = {
                if (hasAnyDraft) {
                    IconButton(
                        onClick = {
                            dismissKeyboard()
                            onDiscardImport()
                        },
                        enabled = !nfceState.isImporting,
                        modifier = Modifier.testTag("nfce_topbar_discard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Limpar importação",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("nfce_import_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, extended.borderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Chave de Acesso da Nota",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Digite os 44 números impressos perto do código de barras do cupom.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedTextField(
                            value = nfceState.inputQuery,
                            onValueChange = onInputChange,
                            label = { Text("Chave de Acesso (44 números)") },
                            placeholder = { Text("Ex: 4226 1144 9078 5800...") },
                            isError = nfceState.errorMessage != null,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                            trailingIcon = {
                                if (nfceState.inputQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onInputChange("") },
                                        modifier = Modifier.testTag("nfce_clear_input_button")
                                    ) {
                                        Icon(Icons.Rounded.Clear, contentDescription = "Limpar campo")
                                    }
                                } else {
                                    IconButton(
                                        onClick = {
                                            clipboardManager.getText()?.text?.trim()?.takeIf { it.isNotBlank() }?.let {
                                                onInputChange(it)
                                            }
                                        },
                                        modifier = Modifier.testTag("nfce_paste_clipboard_button")
                                    ) {
                                        Icon(Icons.Rounded.ContentPaste, contentDescription = "Colar chave copiada")
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    dismissKeyboard()
                                    onConsultInput()
                                }
                            ),
                            minLines = 2,
                            maxLines = 3,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("nfce_query_input")
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (!nfceState.inputQuery.startsWith("http", ignoreCase = true)) {
                                LinearProgressIndicator(
                                    progress = { (digitsCount.coerceAtMost(44)) / 44f },
                                    color = if (nfceState.detectedKey != null) extended.positive else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (nfceState.detectedKey != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = extended.positive,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Chave completa (${nfceState.selectedPortal.ufSigla})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = extended.positive
                                        )
                                    }
                                } else if (nfceState.inputQuery.startsWith("http", ignoreCase = true)) {
                                    Text(
                                        text = "Link detectado (${nfceState.selectedPortal.ufSigla})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Text(
                                        text = "$digitsCount de 44 números",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (digitsCount > 44) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier.clickable {
                                        dismissKeyboard()
                                        showStatePicker = true
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Place,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = nfceState.selectedPortal.ufSigla,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                dismissKeyboard()
                                onConsultInput()
                            },
                            enabled = !nfceState.isFetching,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("nfce_consult_button")
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Buscar Produtos da Nota",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f))
                            Text(
                                text = "ou use a câmera",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    dismissKeyboard()
                                    showQrScanner = true
                                },
                                enabled = !nfceState.isFetching,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("nfce_scan_qr_button")
                            ) {
                                Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ler QR Code", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            OutlinedButton(
                                onClick = {
                                    dismissKeyboard()
                                    showImageSourceChooser = true
                                },
                                enabled = !nfceState.isFetching,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("nfce_choose_photo_button")
                            ) {
                                Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Foto do Cupom", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            if (nfceState.isFetching) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            Text(
                                text = nfceState.statusMessage ?: "Buscando os produtos da nota...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else if (!nfceState.errorMessage.isNullOrBlank()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("nfce_error_card"),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = nfceState.errorTitle ?: "Não foi possível continuar",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Text(
                                text = nfceState.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            when (nfceState.errorAction) {
                                NfceErrorAction.OPEN_BROWSER -> {
                                    OutlinedButton(
                                        onClick = {
                                            dismissKeyboard()
                                            openUrlInExternalBrowser(context, nfceState.activePortalUrl)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Abrir no Navegador")
                                    }
                                }
                                NfceErrorAction.TRY_PHOTO -> {
                                    OutlinedButton(
                                        onClick = {
                                            dismissKeyboard()
                                            showImageSourceChooser = true
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Tirar Foto do Cupom")
                                    }
                                }
                                NfceErrorAction.CLEAR_INPUT -> {
                                    OutlinedButton(
                                        onClick = { onInputChange("") },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Rounded.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Limpar e Digitar de Novo")
                                    }
                                }
                                NfceErrorAction.NONE -> Unit
                            }
                        }
                    }
                }
            } else if (!nfceState.statusMessage.isNullOrBlank()) {
                item {
                    val isPositive = nfceState.parsedItems.isNotEmpty()
                    Surface(
                        color = if (isPositive) {
                            extended.positive.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isPositive) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                                contentDescription = null,
                                tint = if (isPositive) extended.positive else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = nfceState.statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
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
                onConfirmImport = {
                    dismissKeyboard()
                    onConfirmImport()
                },
                onDiscardImport = {
                    dismissKeyboard()
                    onDiscardImport()
                }
            )

            if (nfceState.parsedItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = {
                                dismissKeyboard()
                                xmlPickerLauncher.launch(arrayOf("text/xml", "application/xml", "*/*"))
                            },
                            modifier = Modifier.testTag("nfce_import_xml_button")
                        ) {
                            Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tenho o arquivo XML da nota")
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
