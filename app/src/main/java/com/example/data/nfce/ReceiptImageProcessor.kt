package com.example.data.nfce

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_ITF,
                    Barcode.FORMAT_ALL_FORMATS
                )
                .build()
            val scanner = BarcodeScanning.getClient(options)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val found = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue?.trim()
                    scanner.close()
                    if (cont.isActive) cont.resume(found)
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
                    recognizer.close()
                    if (cont.isActive) cont.resume(visionText.text.orEmpty())
                }
                .addOnFailureListener {
                    recognizer.close()
                    if (cont.isActive) cont.resume("")
                }
        }
}
