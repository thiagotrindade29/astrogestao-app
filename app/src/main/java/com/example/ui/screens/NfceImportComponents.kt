package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.nfce.SefazPortals
import com.example.data.repository.ProductMatcher
import com.example.ui.components.formatBrl
import com.example.ui.components.formatQty
import com.example.ui.theme.AstroTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.AstroUiState
import com.example.ui.viewmodel.NfceImportState

internal fun openUrlInExternalBrowser(context: android.content.Context, url: String) {
    if (url.isBlank()) return
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

@Composable
internal fun NfceImageSourceDialog(
    onPickGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                        .clickable(onClick = onPickGallery)
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
                        .clickable(onClick = onTakePhoto)
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
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun NfcePortalWebViewDialog(
    nfceState: NfceImportState,
    onWebViewExtracted: (rawJson: String, isAutoScan: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var webProgress by remember { mutableIntStateOf(0) }
    var currentUrl by remember(nfceState.activePortalUrl) { mutableStateOf(nfceState.activePortalUrl) }
    var webErrorMessage by remember(nfceState.activePortalUrl) { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(10.dp)
                .testTag("nfce_webview_dialog"),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = nfceState.selectedPortal.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentUrl.ifBlank { nfceState.selectedPortal.url },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (nfceState.detectedKey != null) {
                        OutlinedButton(
                            onClick = {
                                webViewRef?.evaluateJavascript(
                                    SefazPortals.buildFillKeyJs(nfceState.detectedKey),
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
                    IconButton(
                        onClick = {
                            openUrlInExternalBrowser(
                                context = context,
                                url = currentUrl.ifBlank { nfceState.selectedPortal.url }
                            )
                        },
                        modifier = Modifier.testTag("nfce_header_chrome_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = "Abrir no Navegador"
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("nfce_close_webview_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }

                HorizontalDivider()

                if (webProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { webProgress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                val cm = CookieManager.getInstance()
                                cm.setAcceptCookie(true)
                                cm.setAcceptThirdPartyCookies(this, true)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.javaScriptCanOpenWindowsAutomatically = true
                                settings.setSupportMultipleWindows(false)
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.userAgentString =
                                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val reqUrl = request?.url?.toString().orEmpty()
                                        if (reqUrl.startsWith("http://", ignoreCase = true) ||
                                            reqUrl.startsWith("https://", ignoreCase = true)
                                        ) {
                                            return false
                                        }
                                        openUrlInExternalBrowser(ctx, reqUrl)
                                        return true
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        webErrorMessage = null
                                        if (!url.isNullOrBlank()) {
                                            currentUrl = url
                                        }
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        if (!url.isNullOrBlank()) {
                                            currentUrl = url
                                        }
                                        nfceState.detectedKey?.let { key ->
                                            view?.evaluateJavascript(SefazPortals.buildFillKeyJs(key), null)
                                        }
                                        view?.evaluateJavascript(SefazPortals.WEBVIEW_EXTRACTION_JS) { raw ->
                                            if (!raw.isNullOrBlank()) {
                                                onWebViewExtracted(raw, true)
                                            }
                                        }
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        super.onReceivedError(view, request, error)
                                        if (request?.isForMainFrame == true) {
                                            webErrorMessage = "Não foi possível abrir este portal dentro do app (${error?.description ?: "falha de conexão"}). Use \"Abrir no Chrome\" para visualizar a nota e depois cole o texto ou tire um print."
                                        }
                                    }

                                    override fun onReceivedSslError(
                                        view: WebView?,
                                        handler: SslErrorHandler?,
                                        error: SslError?
                                    ) {
                                        handler?.cancel()
                                        webErrorMessage = "O certificado digital deste portal estadual exige abertura pelo navegador do celular (Chrome). Toque em \"Abrir no Chrome\" abaixo para abrir a nota diretamente."
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
                            if (nfceState.activePortalUrl.isNotBlank() &&
                                wv.url != nfceState.activePortalUrl &&
                                currentUrl != nfceState.activePortalUrl
                            ) {
                                webErrorMessage = null
                                currentUrl = nfceState.activePortalUrl
                                wv.loadUrl(nfceState.activePortalUrl)
                            }
                            webViewRef = wv
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    webErrorMessage?.let { errorMsg ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Text(
                                        text = "Portal bloqueado no navegador interno",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = errorMsg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Button(
                                        onClick = {
                                            openUrlInExternalBrowser(
                                                context = context,
                                                url = currentUrl.ifBlank { nfceState.selectedPortal.url }
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("nfce_error_open_chrome_button")
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Abrir no Chrome / Navegador")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            webErrorMessage = null
                                            webViewRef?.reload()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Tentar Novamente")
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            openUrlInExternalBrowser(
                                context = context,
                                url = currentUrl.ifBlank { nfceState.selectedPortal.url }
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("nfce_open_browser_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Abrir no Chrome")
                    }
                    Button(
                        onClick = {
                            webViewRef?.evaluateJavascript(SefazPortals.WEBVIEW_EXTRACTION_JS) { raw ->
                                onWebViewExtracted(raw.orEmpty(), false)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("nfce_extract_webview_button")
                    ) {
                        Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Extrair Itens")
                    }
                }
            }
        }
    }
}

internal fun LazyListScope.nfceParsedItemsSection(
    uiState: AstroUiState,
    nfceState: NfceImportState,
    selectedCount: Int,
    onToggleItem: (Int) -> Unit,
    onSelectItemCategory: (Int, Long) -> Unit,
    onConfirmImport: () -> Unit
) {
    if (nfceState.parsedItems.isEmpty()) return

    item {
        val extended = AstroTheme.extended
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
                            formatBrl(nfceState.parsedItems.filter { it.selected }.sumOf { it.totalPrice })
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
        val extended = AstroTheme.extended
        val existingMatch = ProductMatcher.findCanonicalMatch(
            products = uiState.products,
            aliases = uiState.aliases,
            code = item.code,
            name = item.name
        )

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
