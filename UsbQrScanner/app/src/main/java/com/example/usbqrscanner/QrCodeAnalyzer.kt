package com.example.usbqrscanner

import android.os.SystemClock
import android.util.Log
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Feeds NV21 frames from an external USB UVC camera into Google ML Kit for QR code detection.
 */
class QrCodeAnalyzer(
    private val onQrDetected: (QrScanResult) -> Unit
) {
    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    private val isProcessing = AtomicBoolean(false)
    @Volatile
    var isPaused: Boolean = false

    @Volatile
    var rotationDegrees: Int = 0

    @Volatile
    private var lastAnalyzedElapsedMs: Long = 0L

    fun analyzeNv21Frame(frame: ByteBuffer, width: Int, height: Int) {
        if (isPaused || width <= 0 || height <= 0) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastAnalyzedElapsedMs < MIN_SCAN_INTERVAL_MS) return
        if (!isProcessing.compareAndSet(false, true)) return

        lastAnalyzedElapsedMs = now

        val nv21Bytes = try {
            val copy = ByteArray(frame.remaining())
            frame.get(copy)
            copy
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read NV21 frame buffer", e)
            isProcessing.set(false)
            return
        }

        val inputImage = try {
            InputImage.fromByteArray(
                nv21Bytes,
                width,
                height,
                rotationDegrees,
                InputImage.IMAGE_FORMAT_NV21
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to build InputImage from USB frame", e)
            isProcessing.set(false)
            return
        }

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val result = barcodes.firstNotNullOfOrNull { ScanResultFormatter.format(it) }
                if (result != null) {
                    onQrDetected(result)
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "ML Kit QR detection failed", e)
            }
            .addOnCompleteListener {
                isProcessing.set(false)
            }
    }

    fun close() {
        scanner.close()
    }

    companion object {
        private const val TAG = "QrCodeAnalyzer"
        private const val MIN_SCAN_INTERVAL_MS = 250L
    }
}
