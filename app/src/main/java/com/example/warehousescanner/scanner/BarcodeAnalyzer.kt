// В файле: app/src/main/java/com/example/warehousescanner/scanner/BarcodeAnalyzer.kt
package com.example.warehousescanner.scanner

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val onBarcodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    // --- УЛУЧШЕННАЯ ЛОГИКА ОГРАНИЧЕНИЯ ---
    private var lastAnalyzedTimestamp = 0L
    private val throttleIntervalMs = 1000 // Анализируем не чаще, чем раз в секунду

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()

        // Если с последней попытки прошло меньше секунды, пропускаем этот кадр
        if (currentTime - lastAnalyzedTimestamp < throttleIntervalMs) {
            imageProxy.close() // Важно закрыть кадр, чтобы освободить буфер
            return
        }
        // Обновляем время последней попытки
        lastAnalyzedTimestamp = currentTime

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().build())

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        barcodes.firstOrNull()?.rawValue?.let { barcodeValue ->
                            onBarcodeScanned(barcodeValue)
                        }
                    }
                }
                .addOnFailureListener {
                    // Можно добавить логирование ошибки, если нужно
                }
                .addOnCompleteListener {
                    // Важно всегда закрывать imageProxy
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}