package com.example.data.nfce

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.max

data class ReceiptImageScanResult(
    val qrOrBarcodeRaw: String? = null,
    val ocrText: String = ""
)

object ReceiptImageProcessor {

    suspend fun scanReceiptImage(context: Context, imageUri: Uri): ReceiptImageScanResult {
        val image = InputImage.fromFilePath(context, imageUri)

        val qrRaw = detectBarcodeOrQr(image)
        val ocrText = detectTextOcr(image)

        return ReceiptImageScanResult(
            qrOrBarcodeRaw = qrRaw,
            ocrText = ocrText
        )
    }

    private suspend fun detectBarcodeOrQr(image: InputImage): String? =
        suspendCancellableCoroutine { cont ->
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
            val scanner = BarcodeScanning.getClient(options)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val prioritized = barcodes
                        .mapNotNull { it.rawValue?.trim()?.takeIf { v -> v.isNotBlank() } }
                        .sortedByDescending { value ->
                            when {
                                SefazPortals.sanitizeSefazUrl(value) != null -> 3
                                SefazPortals.extractAccessKey(value) != null -> 2
                                value.startsWith("http://", ignoreCase = true) ||
                                    value.startsWith("https://", ignoreCase = true) -> 1
                                else -> 0
                            }
                        }
                        .firstOrNull()
                    scanner.close()
                    if (cont.isActive) cont.resume(prioritized)
                }
                .addOnFailureListener {
                    scanner.close()
                    if (cont.isActive) cont.resume(null)
                }
        }

    private suspend fun detectTextOcr(image: InputImage): String =
        suspendCancellableCoroutine { cont ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val reconstructed = reconstructReceiptLines(visionText)
                    recognizer.close()
                    if (cont.isActive) cont.resume(reconstructed)
                }
                .addOnFailureListener {
                    recognizer.close()
                    if (cont.isActive) cont.resume("")
                }
        }

    /**
     * ML Kit agrupa blocos separados por colunas (ex: nomes à esquerda e valores à direita).
     * Reconstruímos cada linha física do cupom ordenando por coordenada Y e agrupando
     * trechos na mesma faixa horizontal da esquerda para a direita.
     */
    private fun reconstructReceiptLines(visionText: Text): String {
        val allLines = visionText.textBlocks.flatMap { it.lines }
        if (allLines.isEmpty()) return visionText.text.orEmpty()

        val positioned = allLines.mapNotNull { line ->
            val box = line.boundingBox ?: return@mapNotNull null
            val clean = line.text.trim()
            if (clean.isEmpty()) return@mapNotNull null
            Triple(box.centerY(), box.height().coerceAtLeast(12), box.left to clean)
        }.sortedBy { it.first }

        if (positioned.isEmpty()) return visionText.text.orEmpty()

        val rows = mutableListOf<MutableList<Pair<Int, String>>>()
        var currentAnchorY = positioned.first().first
        var currentTolerance = max(12, positioned.first().second / 2)

        for ((centerY, height, leftAndText) in positioned) {
            if (rows.isEmpty() || abs(centerY - currentAnchorY) > currentTolerance) {
                rows.add(mutableListOf(leftAndText))
                currentAnchorY = centerY
                currentTolerance = max(12, (height * 0.6f).toInt())
            } else {
                rows.last().add(leftAndText)
            }
        }

        return rows.joinToString("\n") { row ->
            row.sortedBy { it.first }.joinToString("   ") { it.second }
        }
    }
}
