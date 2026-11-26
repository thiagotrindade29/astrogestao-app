package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.example.data.nfce.RegionalPortal
import com.example.data.nfce.SefazPortals
import com.example.data.repository.ProductMatcher
import com.example.ui.components.formatBrl
import com.example.ui.components.formatQty
import com.example.ui.theme.AstroTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.AstroUiState
import com.example.ui.viewmodel.NfceImportState
import kotlinx.coroutines.delay

internal fun openUrlInExternalBrowser(context: android.content.Context, url: String) {
    val safeUrl = SefazPortals.sanitizeSefazUrl(url)
        ?: url.trim().takeIf { SefazPortals.isAllowedWebViewNavigationUrl(it) }
        ?: return
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

private fun isProbablyEmulator(): Boolean {
    return Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.startsWith("unknown") ||
        Build.MODEL.contains("google_sdk") ||
        Build.MODEL.contains("Emulator") ||
        Build.MODEL.contains("Android SDK built for x86") ||
        Build.MANUFACTURER.contains("Genymotion") ||
        (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
        Build.PRODUCT == "google_sdk" ||
        Build.HARDWARE.contains("goldfish") ||
        Build.HARDWARE.contains("ranchu")
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
                text = "Enviar Foto do Cupom",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Lemos o QR Code ou a lista de produtos impressa automaticamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onTakePhoto)
                        .testTag("nfce_take_photo_option"),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Tirar Foto Agora", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Use a câmera no cupom de papel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
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
                            Text("Escolher da Galeria", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Selecione uma foto ou print salvo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
internal fun NfceStatePickerDialog(
    selectedPortal: RegionalPortal,
    onSelectPortal: (RegionalPortal) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filteredPortals = remember(search) {
        val q = search.trim().lowercase()
        if (q.isEmpty()) SefazPortals.allPortals
        else SefazPortals.allPortals.filter {
            it.label.lowercase().contains(q) || it.ufSigla.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Escolha o Estado da Nota",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Buscar estado (ex: SC, SP, PR)...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredPortals, key = { it.ufCode }) { portal ->
                        val isSelected = portal.ufCode == selectedPortal.ufCode
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectPortal(portal)
                                    onDismiss()
                                }
                                .testTag("portal_chip_${portal.ufCode}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = portal.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selecionado",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
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
    var retryAttempt by remember { mutableIntStateOf(0) }
    var rendererCrashed by remember { mutableStateOf(false) }
    var forceSoftwareLayer by remember { mutableStateOf(isProbablyEmulator()) }
    val resolvedFallbackUrl = remember(nfceState.selectedPortal, nfceState.detectedKey, nfceState.activePortalUrl) {
        SefazPortals.resolvePortalUrl(nfceState.selectedPortal, nfceState.detectedKey, nfceState.activePortalUrl)
    }
    var currentUrl by remember(resolvedFallbackUrl) { mutableStateOf(resolvedFallbackUrl) }
    var lastLoadedUrl by remember(resolvedFallbackUrl, retryAttempt) { mutableStateOf("") }
    var webErrorMessage by remember(resolvedFallbackUrl, retryAttempt) { mutableStateOf<String?>(null) }

    DisposableEffect(retryAttempt) {
        onDispose {
            webViewRef?.let { wv ->
                runCatching {
                    wv.stopLoading()
                    wv.loadUrl("about:blank")
                    wv.clearHistory()
                    (wv.parent as? ViewGroup)?.removeView(wv)
                    wv.removeAllViews()
                    wv.destroy()
                }
            }
            webViewRef = null
        }
    }

    LaunchedEffect(resolvedFallbackUrl, retryAttempt, rendererCrashed) {
        while (!rendererCrashed) {
            delay(1200L)
            if (webErrorMessage == null) {
                webViewRef?.evaluateJavascript(SefazPortals.WEBVIEW_EXTRACTION_JS) { raw ->
                    if (!raw.isNullOrBlank()) {
                        onWebViewExtracted(raw, true)
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))
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
                            text = "Consulta da Nota (${nfceState.selectedPortal.ufSigla})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Confirme na página abaixo que traremos os produtos.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (nfceState.detectedKey != null && !rendererCrashed) {
                        OutlinedButton(
                            onClick = {
                                webViewRef?.evaluateJavascript(
                                    SefazPortals.buildFillKeyJs(nfceState.detectedKey),
                                    null
                                )
                            },
                            enabled = webViewRef != null,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Preencher Chave",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
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
                    if (!rendererCrashed) {
                        key(retryAttempt) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        // Em aparelhos reais mantém aceleração por hardware (evita tela branca em CSS/CAPTCHA);
                                        // em emuladores ou após crash de rendernode usa camada de software.
                                        setLayerType(
                                            if (forceSoftwareLayer) View.LAYER_TYPE_SOFTWARE else View.LAYER_TYPE_NONE,
                                            null
                                        )
                                        runCatching {
                                            setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true)
                                        }
                                        val cm = CookieManager.getInstance()
                                        cm.setAcceptCookie(true)
                                        // Necessário para iframes de reCAPTCHA / hCaptcha e sessões cruzadas da SEFAZ
                                        cm.setAcceptThirdPartyCookies(this, true)
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.databaseEnabled = true
                                        settings.javaScriptCanOpenWindowsAutomatically = false
                                        settings.setSupportMultipleWindows(false)
                                        settings.allowFileAccess = false
                                        settings.allowContentAccess = false
                                        @Suppress("DEPRECATION")
                                        settings.allowFileAccessFromFileURLs = false
                                        @Suppress("DEPRECATION")
                                        settings.allowUniversalAccessFromFileURLs = false
                                        settings.safeBrowsingEnabled = false
                                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                        settings.builtInZoomControls = true
                                        settings.displayZoomControls = false
                                        settings.loadWithOverviewMode = true
                                        settings.useWideViewPort = true
                                        val defaultUa = settings.userAgentString.orEmpty()
                                        if (defaultUa.isNotBlank()) {
                                            settings.userAgentString = defaultUa.replace("; wv)", ")")
                                        }
                                        webViewClient = object : WebViewClient() {
                                            override fun shouldOverrideUrlLoading(
                                                view: WebView?,
                                                request: WebResourceRequest?
                                            ): Boolean {
                                                val reqUrl = request?.url?.toString().orEmpty()
                                                if (SefazPortals.isAllowedWebViewNavigationUrl(reqUrl)) {
                                                    return false
                                                }
                                                val upgraded = SefazPortals.upgradeGovHttpRedirect(reqUrl)
                                                if (upgraded != null && request?.isForMainFrame == true) {
                                                    view?.loadUrl(upgraded)
                                                }
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
                                                // Detecta página em branco retornada por bloqueio anti-bot/VPN da SEFAZ
                                                view?.postDelayed({
                                                    if (webErrorMessage == null) {
                                                        view.evaluateJavascript(
                                                            "(function(){var t=(document.body&&document.body.innerText||'').trim().length;var f=document.querySelectorAll('input,iframe,form,table').length;return t+'|'+f;})()"
                                                        ) { check ->
                                                            val cleaned = check?.trim('"', ' ') ?: ""
                                                            val parts = cleaned.split("|")
                                                            val textLen = parts.getOrNull(0)?.toIntOrNull() ?: -1
                                                            val domCount = parts.getOrNull(1)?.toIntOrNull() ?: -1
                                                            if (textLen == 0 && domCount == 0) {
                                                                webErrorMessage = "O portal da nota (${nfceState.selectedPortal.ufSigla}) abriu em branco. Abra no navegador do celular ou volte e tire uma foto do cupom."
                                                            }
                                                        }
                                                    }
                                                }, 1800L)
                                            }

                                            override fun onReceivedHttpError(
                                                view: WebView?,
                                                request: WebResourceRequest?,
                                                errorResponse: WebResourceResponse?
                                            ) {
                                                super.onReceivedHttpError(view, request, errorResponse)
                                                if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 0) >= 400) {
                                                    webErrorMessage = "O portal da nota (${nfceState.selectedPortal.ufSigla}) não pôde ser exibido aqui dentro. Abra no navegador do celular ou tire uma foto do cupom."
                                                }
                                            }

                                            override fun onReceivedError(
                                                view: WebView?,
                                                request: WebResourceRequest?,
                                                error: WebResourceError?
                                            ) {
                                                super.onReceivedError(view, request, error)
                                                if (request?.isForMainFrame == true) {
                                                    webErrorMessage = "Não foi possível carregar essa página aqui dentro. Abra no navegador do celular ou volte e tire uma foto do cupom."
                                                }
                                            }

                                            override fun onReceivedSslError(
                                                view: WebView?,
                                                handler: SslErrorHandler?,
                                                error: SslError?
                                            ) {
                                                handler?.cancel()
                                                webErrorMessage = "Não foi possível abrir essa página aqui dentro com segurança. Toque em \"Abrir no Navegador\" abaixo."
                                            }

                                            override fun onRenderProcessGone(
                                                view: WebView?,
                                                detail: RenderProcessGoneDetail?
                                            ): Boolean {
                                                runCatching {
                                                    (view?.parent as? ViewGroup)?.removeView(view)
                                                    view?.destroy()
                                                }
                                                webViewRef = null
                                                forceSoftwareLayer = true
                                                rendererCrashed = true
                                                webErrorMessage = "A página da nota precisou ser fechada neste aparelho. Toque em \"Abrir no Navegador\" ou use a foto do cupom."
                                                return true
                                            }
                                        }
                                        webChromeClient = object : WebChromeClient() {
                                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                webProgress = newProgress
                                            }
                                        }
                                        val initialSafeUrl = SefazPortals.sanitizeSefazUrl(resolvedFallbackUrl)
                                            ?: nfceState.selectedPortal.url
                                        lastLoadedUrl = initialSafeUrl
                                        loadUrl(initialSafeUrl)
                                        webViewRef = this
                                    }
                                },
                                update = { wv ->
                                    val targetSafeUrl = SefazPortals.sanitizeSefazUrl(resolvedFallbackUrl)
                                    if (!targetSafeUrl.isNullOrBlank() && targetSafeUrl != lastLoadedUrl) {
                                        webErrorMessage = null
                                        currentUrl = targetSafeUrl
                                        lastLoadedUrl = targetSafeUrl
                                        wv.loadUrl(targetSafeUrl)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

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
                                        text = "Não foi possível abrir a página aqui",
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
                                                url = currentUrl.ifBlank { resolvedFallbackUrl }
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("nfce_error_open_chrome_button")
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Abrir no Navegador")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            webErrorMessage = null
                                            if (rendererCrashed) {
                                                rendererCrashed = false
                                                retryAttempt++
                                            } else {
                                                webViewRef?.reload()
                                            }
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
                                url = currentUrl.ifBlank { resolvedFallbackUrl }
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("nfce_open_browser_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Abrir no Chrome", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = {
                            webViewRef?.evaluateJavascript(SefazPortals.WEBVIEW_EXTRACTION_JS) { raw ->
                                onWebViewExtracted(raw.orEmpty(), false)
                            }
                        },
                        enabled = !rendererCrashed && webViewRef != null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("nfce_extract_webview_button")
                    ) {
                        Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trazer Produtos", maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    onConfirmImport: () -> Unit,
    onDiscardImport: () -> Unit
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = nfceState.storeName ?: "Itens Prontos para Salvar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$selectedCount de ${nfceState.parsedItems.size} marcado(s) • Total: ${
                            formatBrl(nfceState.parsedItems.filter { it.selected }.sumOf { it.totalPrice })
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.positive
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDiscardImport,
                        enabled = !nfceState.isImporting,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("nfce_discard_import_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Excluir",
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = onConfirmImport,
                        enabled = selectedCount > 0 && !nfceState.isImporting,
                        modifier = Modifier
                            .weight(1.35f)
                            .testTag("nfce_confirm_import_button")
                    ) {
                        if (nfceState.isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Salvando...", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        } else {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Importar ($selectedCount)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
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
                                "Somará ao item ${existingMatch.uniqueCode} (atual: ${formatQty(existingMatch.quantity)} ${existingMatch.unit})"
                            } else {
                                "Novo item no estoque"
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
