package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.camera.mlkit.vision.MlKitAnalyzer
import com.example.R
import com.example.ui.theme.JetBrainsMonoFamily
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

@Composable
fun BarcodeScannerDialog(
    onCodeDetected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequestedOnce by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var manualCode by remember { mutableStateOf("") }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionRequestedOnce = true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        }
    }

    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                cameraController.unbind()
                barcodeScanner.close()
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
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Leitor de Código de Barras",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasCameraPermission) {
                            IconButton(
                                onClick = {
                                    torchEnabled = !torchEnabled
                                    runCatching { cameraController.enableTorch(torchEnabled) }
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = if (torchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                                    contentDescription = if (torchEnabled) "Desligar lanterna" else "Ligar lanterna"
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("scanner_close_button")
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_cancel))
                        }
                    }
                }

                val scannerBackdrop = MaterialTheme.colorScheme.primary
                val scannerAccent = MaterialTheme.colorScheme.tertiary
                val scannerInk = MaterialTheme.colorScheme.onPrimary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(270.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission && cameraError == null) {
                        var hasEmitted by remember { mutableStateOf(false) }
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val executor = ContextCompat.getMainExecutor(ctx)
                                runCatching {
                                    cameraController.setImageAnalysisAnalyzer(
                                        executor,
                                        MlKitAnalyzer(
                                            listOf(barcodeScanner),
                                            CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                                            executor
                                        ) { result ->
                                            if (hasEmitted) return@MlKitAnalyzer
                                            val barcodes = result?.getValue(barcodeScanner)
                                            val detected = barcodes?.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                                            if (!detected.isNullOrBlank()) {
                                                hasEmitted = true
                                                onCodeDetected(detected.trim())
                                            }
                                        }
                                    )
                                    cameraController.bindToLifecycle(lifecycleOwner)
                                    previewView.controller = cameraController
                                }.onFailure { err ->
                                    cameraError = err.localizedMessage ?: "Câmera indisponível neste dispositivo."
                                }
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Target Reticle Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.76f)
                                .height(130.dp)
                                .border(
                                    BorderStroke(2.dp, scannerAccent),
                                    RoundedCornerShape(14.dp)
                                )
                        )
                        Surface(
                            color = scannerBackdrop.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = "Aponte para o código EAN ou QR Code",
                                style = MaterialTheme.typography.labelSmall,
                                color = scannerInk,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(38.dp)
                            )
                            Text(
                                text = cameraError
                                    ?: if (permissionRequestedOnce) {
                                        "Permissão da câmera não concedida. Autorize a câmera ou digite o código abaixo."
                                    } else {
                                        "Autorize o uso da câmera para bipar os produtos."
                                    },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            if (cameraError == null) {
                                OutlinedButton(
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                                ) {
                                    Text("Permitir Câmera")
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Ou digite o código de barras manualmente:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualCode,
                        onValueChange = { manualCode = it },
                        placeholder = { Text("Ex: 7891149103508") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (manualCode.isNotBlank()) {
                                    onCodeDetected(manualCode.trim())
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scanner_manual_code_input")
                    )
                    Button(
                        onClick = {
                            if (manualCode.isNotBlank()) {
                                onCodeDetected(manualCode.trim())
                            }
                        },
                        enabled = manualCode.isNotBlank(),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("scanner_manual_confirm_button")
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bipar")
                    }
                }
            }
        }
    }
}
